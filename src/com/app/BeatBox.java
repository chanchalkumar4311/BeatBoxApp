package com.app;

import javax.sound.midi.*;
import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.ArrayList;
import static javax.sound.midi.ShortMessage.*;
public class BeatBox {
	
	private ArrayList<JCheckBox> checkboxList;
	private Sequencer sequencer;
	private Sequence sequence;
	private Track track;
	JFileChooser fc = new JFileChooser();
	String[] instrumentNames = {"Bass Drum", "Closed Hi-Hat","Open Hi-Hat", "Acoustic Snare", 
			"Crash Cymbal", "Hand Clap","High Tom", "Hi Bongo",
			"Maracas", "Whistle", "Low Conga","Cowbell",
			"Vibraslap", "Low-mid Tom", "High Agogo","Open Hi Conga"};
	
	int[] instruments = {35, 42, 46, 38, 49, 39, 50, 60, 70, 72, 64, 56, 58, 47, 67, 63};
	
	public static void main(String[] args) {
		new BeatBox().buildGUI();
	}
	
	public void buildGUI() {
		
		JFrame frame = new JFrame("Cyber BeatBox");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setResizable(false);
		BorderLayout layout = new BorderLayout();
		JPanel background = new JPanel(layout);
		background.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		Box buttonBox = new Box(BoxLayout.Y_AXIS);
		JButton start = new JButton("Start");
		start.addActionListener(e -> buildTrackAndStart());
		buttonBox.add(start);
		JButton stop = new JButton("Stop");
		stop.addActionListener(e -> sequencer.stop());
		buttonBox.add(stop);
		JButton upTempo = new JButton("Tempo Up");
		upTempo.addActionListener(e -> changeTempo(1.03f));
		buttonBox.add(upTempo);
		JButton downTempo = new JButton("Tempo Down");
		downTempo.addActionListener(e -> changeTempo(0.97f));
		buttonBox.add(downTempo);
		JButton searlizeIt = new JButton("Save Track");
		searlizeIt.addActionListener(e -> writeToFile());
		buttonBox.add(searlizeIt);
		JButton restore = new JButton("Open Track");
		restore.addActionListener(e -> readFile());
		buttonBox.add(restore);
		Box nameBox = new Box(BoxLayout.Y_AXIS);
		for (String instrumentName : instrumentNames) {
			JLabel instrumentLabel = new JLabel(instrumentName);
			instrumentLabel.setBorder(BorderFactory.createEmptyBorder(4, 1, 4, 1));
			nameBox.add(instrumentLabel);
		}
		
		background.add(BorderLayout.EAST, buttonBox);
		background.add(BorderLayout.WEST, nameBox);
		frame.getContentPane().add(background);
		
		GridLayout grid = new GridLayout(16, 16);
		grid.setVgap(1);
		grid.setHgap(2);
		JPanel mainPanel = new JPanel(grid);
		background.add(BorderLayout.CENTER, mainPanel);
		checkboxList = new ArrayList<>();
		for (int i = 0; i < 256; i++) {
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			checkboxList.add(c);
			mainPanel.add(c);
		}
		
		setUpMidi();
		
		frame.setBounds(50, 50, 300, 300);
		frame.pack();
		frame.setVisible(true);
	}
	
	private void readFile() {
		System.out.println("Reading from the file...!");
		boolean[] checkboxState = null;
		int returnVal = fc.showOpenDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            System.out.println("Opening: " + file.getName() + ".");
            try (ObjectInputStream is =
    				new ObjectInputStream(new FileInputStream(file))) {
    			checkboxState = (boolean[]) is.readObject();
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
    		for (int i = 0; i < 256; i++) {
    			JCheckBox check = checkboxList.get(i);
    			check.setSelected(checkboxState[i]);
    		}
    		sequencer.stop();
    		buildTrackAndStart();
         
        } else {
            System.out.println("Open command cancelled by user.");
        }
	}

	private void writeToFile() {
		System.out.println("Writing to the file...!");
		boolean[] checkboxState = new boolean[256];
		int returnVal = fc.showSaveDialog(null);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fc.getSelectedFile();
			System.out.println("Saving: " + file.getName() + ".");
			for (int i = 0; i < 256; i++) {
				JCheckBox check = checkboxList.get(i);
				if (check.isSelected()) {
					checkboxState[i] = true;
				}
			}
			try (ObjectOutputStream os =
					new ObjectOutputStream(new FileOutputStream(file))) {
				os.writeObject(checkboxState);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}else {
			System.out.println("Save command cancelled by user.");
		}
	}

	private void setUpMidi() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ, 4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void buildTrackAndStart() {
		int[] trackList;
		sequence.deleteTrack(track);
		track = sequence.createTrack();
		for (int i = 0; i < 16; i++) {
			trackList = new int[16];
			int key = instruments[i];
			for (int j = 0; j < 16; j++) {
				JCheckBox jc = checkboxList.get(j + 16 * i);
				if (jc.isSelected()) {
					trackList[j] = key;
				} else {
					trackList[j] = 0;
				}
			}
			makeTracks(trackList);
			track.add(makeEvent(CONTROL_CHANGE, 1, 127, 0, 16));
		}
		track.add(makeEvent(PROGRAM_CHANGE, 9, 1, 0, 15));
		try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(Sequencer.LOOP_CONTINUOUSLY);
			sequencer.setTempoInBPM(120);
			sequencer.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void changeTempo(float tempoMultiplier) {
		float tempoFactor = sequencer.getTempoFactor();
		sequencer.setTempoFactor(tempoFactor * tempoMultiplier);
	}
	
	private void makeTracks(int[] list) {
		for (int i = 0; i < 16; i++) {
			int key = list[i];
			if (key != 0) {
				track.add(makeEvent(NOTE_ON, 9, key, 100, i));
				track.add(makeEvent(NOTE_OFF, 9, key, 100, i + 1));
			}
		}
	}
	
	public static MidiEvent makeEvent(int cmd, int chnl, int one, int two, int tick) {
		MidiEvent event = null;
		try {
			ShortMessage msg = new ShortMessage();
			msg.setMessage(cmd, chnl, one, two);
			event = new MidiEvent(msg, tick);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return event;
	}
}
