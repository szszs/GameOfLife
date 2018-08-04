package main.GOL;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileNameExtensionFilter;

@SuppressWarnings("serial")
public class MainFrame extends JFrame{
	public GridPanel gridPanel;
	public MainMenuBar mainMenuBar;
	public ToolBar toolBar;
	private Container c;
	
	private final String gameIcon = "images/app.png";
	
	// game rules
	private Set<Integer> bornRules = new HashSet<Integer>();
	private Set<Integer> surviveRules = new HashSet<Integer>();
	private final int[] initialBornRules = new int[] {3};
	private final int[] initialSurviveRules = new int[] {2,3};
	
	private final String MOVE_CAM_UP = "move cam up";
	private final String MOVE_CAM_DOWN = "move cam down";
	private final String MOVE_CAM_RIGHT = "move cam right";
	private final String MOVE_CAM_LEFT = "move cam left";
	private final String ZOOM_OUT = "zoom out";
	private final String ZOOM_IN = "zoom in";
	private final String PAUSE_UNPAUSE = "pause unpause";
	private final String STEP_FRAME = "step frame";
	private final String RESET = "reset";
	
	private final Font font = new Font("Helvetica", Font.PLAIN, 20);
	private final String helpMessage = ""
			+ "Controls:\n"
			+ "Create alive cell: left click\n"
			+ "Kill cell: right click\n"
			+ "Pause/Unpause: SPACEBAR (also on toolbar)\n"
			+ "Increment generation: ENTER (also on toolbar)\n"
			+ "Clear grid: BACKSPACE (also on edit menu)\n"
			+ "Zoom in/out: CTRL+/- or use mouse wheel\n"
			+ "Pan screen: CTRL+click mouse and drag\n";
	
	private final FileNameExtensionFilter[] fileTypes = new FileNameExtensionFilter[] {
		new FileNameExtensionFilter("Run Length Encoded (*.rle)", "rle"),
		new FileNameExtensionFilter("Life Sim (*.lifesim)", "lifesim"),
		new FileNameExtensionFilter("Plaintext (*.cells)", "cells")
	};
	
	public MainFrame(String title) {
		super(title);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
	}
	
	public void startWindow() {
		// set up initial rules
		for (int surviveRule:initialSurviveRules) {
			surviveRules.add(surviveRule);
		}
		
		for (int bornRule:initialBornRules) {
			bornRules.add(bornRule);
		}
		
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {
			e.printStackTrace();
		}
		UIManager.put("Menu.font", font);
		UIManager.put("MenuItem.font", font);
		UIManager.put("OptionPane.messageFont", font);
		UIManager.put("ColorChooser.font", font);
		
		setLayout(new BorderLayout());
		c = getContentPane();
		c.setBackground(Color.GREEN);
		
		/// create grid panel
		gridPanel = new GridPanel(20, 20, 15, 20*20, 20*15, 0, 0);
		add(gridPanel, BorderLayout.CENTER);
		
		
		this.setMinimumSize(new Dimension(gridPanel.gridPixelWidth, gridPanel.gridPixelHeight));
		
		/// create menu bar
		mainMenuBar = new MainMenuBar();
		setJMenuBar(mainMenuBar);
		
		/// create tool bar
		toolBar = new ToolBar();
		add(toolBar, BorderLayout.LINE_START);
		
		
		//// add main key bindings and listeners
		/// camera move listeners (WASD + arrow keys)
		gridPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("UP"), MOVE_CAM_UP);
		gridPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("W"), MOVE_CAM_UP);
		gridPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("DOWN"), MOVE_CAM_DOWN);
		gridPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("S"), MOVE_CAM_DOWN);
		gridPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("RIGHT"), MOVE_CAM_RIGHT);
		gridPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("D"), MOVE_CAM_RIGHT);
		gridPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("LEFT"), MOVE_CAM_LEFT);
		gridPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("A"), MOVE_CAM_LEFT);
		
		gridPanel.getActionMap().put(MOVE_CAM_UP, new MoveCamAction(0, -10));
		gridPanel.getActionMap().put(MOVE_CAM_DOWN, new MoveCamAction(0, 10));
		gridPanel.getActionMap().put(MOVE_CAM_RIGHT, new MoveCamAction(10, 0));
		gridPanel.getActionMap().put(MOVE_CAM_LEFT, new MoveCamAction(-10, 0));
		
		/// zoom listeners (CTRL+(=/-))
		gridPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), ZOOM_OUT);
		gridPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), ZOOM_IN);
		
		gridPanel.getActionMap().put(ZOOM_OUT, new ZoomAction(false));
		gridPanel.getActionMap().put(ZOOM_IN, new ZoomAction(true));
		
		// pause and time listeners
		gridPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke((char) KeyEvent.VK_SPACE), PAUSE_UNPAUSE);
		gridPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke((char) KeyEvent.VK_ENTER), STEP_FRAME);
		
		gridPanel.getActionMap().put(PAUSE_UNPAUSE, new PauseAction());
		gridPanel.getActionMap().put(STEP_FRAME, new StepFrameAction());
		
		// reset listener
		gridPanel.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke((char) KeyEvent.VK_BACK_SPACE), RESET);
		gridPanel.getActionMap().put(RESET, new ResetAction());
		
		// add window resize listener
		addComponentListener(new ComponentResize());
		
		setIconImage(new ImageIcon(gameIcon).getImage());
		
		pack();
		// open window
		setVisible(true);
	}
	
	private static List<Point> getNeighbourCoords(Point coordinate){
		List<Point> neighbours = new ArrayList<Point>();
		
		int x = coordinate.x;
		int y = coordinate.y;
		
		neighbours.add(new Point(x-1, y-1));
		neighbours.add(new Point(x-1, y+1));
		neighbours.add(new Point(x-1, y));
		neighbours.add(new Point(x+1, y-1));
		neighbours.add(new Point(x+1, y+1));
		neighbours.add(new Point(x+1, y));
		neighbours.add(new Point(x, y+1));
		neighbours.add(new Point(x, y-1));
		
		return neighbours;
	}
	
	private int getAliveNeighbours(List<Point> neighbourCoords) {
		int alive=0;
		for (Point neighbourCoord:neighbourCoords) {
			if (gridPanel.grid.containsKey(neighbourCoord)) {
				alive += 1;
			}
		}
		
		return alive;
	}
	
	private void updateNeighbours(Map<Point, Integer> reproducedCoordinates, List<Point> addCoordinates, List<Point> neighbourCoords) {
		for (Point neighbourCoord:neighbourCoords) {
			if (!gridPanel.grid.containsKey(neighbourCoord)) {
				int supportCount = reproducedCoordinates.containsKey(neighbourCoord) ? reproducedCoordinates.get(neighbourCoord) : 0;
				reproducedCoordinates.put(neighbourCoord, supportCount+1);
			}
		}
	}
	
	// moves time by one step
	public synchronized ArrayList<ArrayList<Point>> prepareUpdateGrid() {
		ArrayList<Point> addCoordinates = new ArrayList<Point>();
		ArrayList<Point> delCoordinates = new ArrayList<Point>();
		Map<Point, Integer> reproducedCoordinates = new HashMap<Point, Integer>();
		
		for (Point point:gridPanel.grid.keySet()) {
			List<Point> neighbourCoords = getNeighbourCoords(point);
			int alive = getAliveNeighbours(neighbourCoords);
			if (!surviveRules.contains(alive))
				delCoordinates.add(point);
			
			updateNeighbours(reproducedCoordinates, addCoordinates, neighbourCoords);
				
		}
		
		// check cells which come alive
		Iterator<Entry<Point, Integer>> it = reproducedCoordinates.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Point, Integer> pair = (Map.Entry<Point, Integer>)it.next();
			if (bornRules.contains(pair.getValue())) {
				addCoordinates.add((Point)pair.getKey());
			}
		}
		
		ArrayList<ArrayList<Point>> prepareUpdateLists = new ArrayList<ArrayList<Point>>();
		prepareUpdateLists.add(addCoordinates);
		prepareUpdateLists.add(delCoordinates);
		
		return prepareUpdateLists;
	}
	
	public synchronized void updateGrid(ArrayList<ArrayList<Point>> prepareUpdateLists) {
		/// update grid
		gridPanel.gridLock.writeLock().lock();
		try {
			// increment age
			gridPanel.grid.replaceAll((point, age) -> age+1);
			
			// add and delete coordinates
			for (Point addCoordinate:prepareUpdateLists.get(0)) {
				gridPanel.grid.put(addCoordinate, 0);
			}
			
			for (Point delCoordinate:prepareUpdateLists.get(1)) {
				gridPanel.grid.remove(delCoordinate);
			}
			gridPanel.steps += 1;
		} finally {
			gridPanel.gridLock.writeLock().unlock();
		}
	}
	
	public void updatePause(boolean paused) {
		gridPanel.pause = paused;
		gridPanel.repaint();
		toolBar.updatePauseIcon(paused);
	}
	
	public Point[] getGridBounds() {
		Point[] bounds = new Point[2];
		
		Set<Integer> xCoords = new HashSet<Integer>();
		Set<Integer> yCoords = new HashSet<Integer>();
		for (Point coordinate:gridPanel.grid.keySet()) {
			xCoords.add(coordinate.x);
			yCoords.add(coordinate.y);
		}
		
		int topLeftX = Collections.min(xCoords);
		int topLeftY = Collections.min(yCoords);
		
		int bottomRightX = Collections.max(xCoords);
		int bottomRightY = Collections.max(yCoords);
		
		bounds[0] = new Point(topLeftX, topLeftY);
		bounds[1] = new Point(bottomRightX, bottomRightY);
		
		return bounds;
	}
	
	public void saveFile(File saveFile, FileNameExtensionFilter selectedFileType) {
		String extension = '.' + selectedFileType.getExtensions()[0];
		
		String path = saveFile.getAbsolutePath();
		if (!path.endsWith(extension))
			path += extension;
		
		saveFile = new File(path);
		
		boolean writeFile = true;
		
		if (saveFile.exists()) {
		    int reply = JOptionPane.showConfirmDialog(null, "File already exists, overwrite?", "Overwrite?", JOptionPane.YES_NO_OPTION, JOptionPane.PLAIN_MESSAGE);
	        if (reply != JOptionPane.YES_OPTION) {
	        	writeFile = false;
	        }
		}
		
		if (writeFile) {
			List<String> lines = new ArrayList<String>();
			
			// convert grid to text
			
			if (extension.equals(".lifesim")) {
				// add top left corner
				lines.add(String.format("%d %d", gridPanel.topLeftX, gridPanel.topLeftY));
				// add living coordinates
				for (Point point:gridPanel.grid.keySet()) {
					lines.add(String.format("%d %d", point.x, point.y));
				}
			}
			
			else if (extension.equals(".cells")) {
				Point[] gridBounds = getGridBounds();
				Point topLeftCoords = gridBounds[0];
				Point bottomRightCoords = gridBounds[1];
				
				for(int y=topLeftCoords.y;y<=bottomRightCoords.y;y++) {
					String line = "";
					for(int x=topLeftCoords.x;x<=bottomRightCoords.x;x++) {
						if(gridPanel.grid.containsKey(new Point(x, y))) {
							line += "O";
						}
						else {
							line += ".";
						}
					}
					lines.add(line);
				}
			}
			
			else if (extension.equals(".rle")) {
				Point[] gridBounds = getGridBounds();
				Point topLeftCoords = gridBounds[0];
				Point bottomRightCoords = gridBounds[1];
				
				int width = bottomRightCoords.x - topLeftCoords.x + 1;
				int height = bottomRightCoords.y - topLeftCoords.y + 1;
				
				lines.add(String.format("x = %d, y = %d", width, height));
				
				String rawLines = "";
				int emptyLinesTrack = 0;
				for(int y=topLeftCoords.y;y<=bottomRightCoords.y;y++) {
					String line = "";
					boolean emptyLine = true;
					List<Boolean> lineData = new ArrayList<Boolean>();
					for(int x=topLeftCoords.x;x<=bottomRightCoords.x;x++) {
						boolean alive = gridPanel.grid.containsKey(new Point(x, y));
						lineData.add(alive);
						if (alive)
							emptyLine = false;
					}
					
					if (emptyLine) {
						emptyLinesTrack++;
					}
					else {
						if (emptyLinesTrack > 0) {
							rawLines += Integer.toString(emptyLinesTrack) + "$";
							emptyLinesTrack = 0;
						}
						try {
							boolean recentState = lineData.get(0);
							int numOfState = 0;
							for(boolean alive:lineData) {
								if (alive == recentState) {
									numOfState++;
								}
								else {
									// add states
									String stateString = recentState?"o":"b";
									if (numOfState > 1) {
										line += Integer.toString(numOfState) + stateString;
									}
									else {
										line += stateString;
									}
									recentState = alive;
									numOfState = 1;
								}
							}
							// add final state
							if (recentState) {
								if (numOfState > 1) {
									line += Integer.toString(numOfState) + "o";
								}
								else {
									line += "o";
								}
							}
							
							line += "$";
							rawLines += line;
						} catch (IndexOutOfBoundsException e) {}
					}
				}
				rawLines = rawLines.substring(0, rawLines.length()-1) + "!";
				for (int i=0; i<=rawLines.length(); i+=70) {
					lines.add(rawLines.substring(i, Math.min(i+70, rawLines.length())));
				}
			}
			
			try {
				Files.write(saveFile.toPath(), lines);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void loadFile(File loadFile, FileNameExtensionFilter selectedFileType) {
		try {
			Scanner sc = new Scanner(loadFile);
			String extension = '.' + selectedFileType.getExtensions()[0];
			
			updatePause(true);
			gridPanel.resetGrid();
			
			// load grid
			if (extension.equals(".lifesim")) {
				// get top left corner
				gridPanel.topLeftX = sc.nextInt();
				gridPanel.topLeftY = sc.nextInt();
				
				while(sc.hasNextInt()) {
					int x = sc.nextInt();
					int y = sc.nextInt();
					gridPanel.grid.put(new Point(x, y), 0);
				}
			}
			else if (extension.equals(".cells")) {
				gridPanel.topLeftX = 0;
				gridPanel.topLeftY = 0;
				
				int y=0;
				
				while(sc.hasNextLine()) {
					String line = sc.nextLine();
					if (!line.startsWith("!")) {
						int x=0;
						for(char cell:line.toCharArray()) {
							if (cell == 'O') {
								gridPanel.grid.put(new Point(x, y), 0);
							}
							x++;
						}
						y++;
					}
				}
			}
			else if (extension.equals(".rle")) {
				int xLen=0;
				int yLen=0;
				while(sc.hasNextLine()) {
					String line = sc.nextLine();
					
					// find starting line
					if (line.startsWith("x")) {
						Scanner lineScanner = new Scanner(line);
						lineScanner.useDelimiter("\\D+");
						xLen = lineScanner.nextInt();
						yLen = lineScanner.nextInt();
						lineScanner.close();
						break;
					}
				}
				
				gridPanel.topLeftX = xLen / 2 - gridPanel.viewableWidth / 2;
				gridPanel.topLeftY = yLen / 2 - gridPanel.viewableHeight / 2;
				
				int y = 0;
				sc.useDelimiter("\\$");
				while(sc.hasNext()) {
					// process new line
					String line = sc.next();
					Matcher lineMatcher = Pattern.compile("\\d+|b|o|!").matcher(line);
					
					int x = 0;
					boolean emptyLine = false;
					
					while (lineMatcher.find()) {
						String tileType = lineMatcher.group();
						int amount = 1;
						
						try {
							// check if tileType is really an integer
							amount = Integer.parseInt(tileType);
							if (lineMatcher.find())
								tileType = lineMatcher.group();
							else {
								// increment new line
								y += amount;
								emptyLine = true;
								break;
							}
						} catch (NumberFormatException e) {}
						
						if (tileType.equals("o")) {
							for (int i=0;i<amount;i++) {
								gridPanel.grid.put(new Point(x+i, y), 0);
							}
						}
						else if (tileType.equals("!")) {
							break;
						}
						
						x += amount;
					}
					
					if (!emptyLine)
						y += 1;
				}
			}
			sc.close();

			gridPanel.redrawMap();
			
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			System.out.println("error opening file");
		}
	}
	
	private class ToolBar extends JToolBar{
		private JButton pauseButton;
		private JButton incrementButton;
		private JToggleButton gridButton;
		private JButton speedUpButton;
		private JButton slowDownButton;
		
		private final static String pauseIcon = "images/pause.png";
		private final static String playIcon = "images/play.png";
		private final static String nextStepIcon = "images/next_step.png";
		private final static String gridOnIcon = "images/grid_on.png";
		private final static String gridOffIcon = "images/grid_off.png";
		private final static String speedUpIcon = "images/speed_up.png";
		private final static String slowDownIcon = "images/slow_down.png";
		
		public ToolBar() {
			super(null, JToolBar.VERTICAL);
			setFloatable(false);
			
			pauseButton = new JButton(new ImageIcon(playIcon));
			pauseButton.addActionListener(new PauseAction());
			add(pauseButton);
			
			incrementButton = new JButton(new ImageIcon(nextStepIcon));
			incrementButton.addActionListener(new StepFrameAction());
			add(incrementButton);
			
			speedUpButton = new JButton(new ImageIcon(speedUpIcon));
			slowDownButton = new JButton(new ImageIcon(slowDownIcon));
			speedUpButton.addActionListener(new SpeedUpAction());
			slowDownButton.addActionListener(new SlowDownAction());
			add(speedUpButton);
			add(slowDownButton);
			
			gridButton = new JToggleButton(new ImageIcon(gridOffIcon));
			gridButton.addActionListener(new GridToggleAction());
			add(gridButton);
		}
		
		public void updateGridButton(boolean gridOn) {
			if (gridOn)
				gridButton.setIcon(new ImageIcon(gridOnIcon));
			else
				gridButton.setIcon(new ImageIcon(gridOffIcon));
		}
		
		public void updatePauseIcon(boolean paused) {
			if (paused)
				pauseButton.setIcon(new ImageIcon(playIcon));
			else
				pauseButton.setIcon(new ImageIcon(pauseIcon));
		}
	}
	
	private class MainMenuBar extends JMenuBar{
		private JMenu fileBar;
		private FileMenuListener fileMenuListener;
		private JMenuItem saveFile;
		private JMenuItem loadFile;
		
		private JMenu editBar;
		private EditMenuListener editMenuListener;
		private JMenuItem clearGrid;
		private JMenuItem changeRule;
		private JMenu preferences;
		private PreferencesMenuListener preferencesMenuListener;
		private JMenuItem changeColors;
		
		private JMenu helpBar;
		private HelpMenuListener helpMenuListener;
		private JMenuItem controls;
		
		public MainMenuBar() {
			setLayout(new FlowLayout(FlowLayout.LEFT));
			
			/// file bar
			fileBar = new JMenu("File");
			fileMenuListener = new FileMenuListener();
			saveFile = new JMenuItem("Save file");
			loadFile = new JMenuItem("Load file");
			saveFile.addActionListener(fileMenuListener);
			loadFile.addActionListener(fileMenuListener);
			fileBar.add(saveFile);
			fileBar.add(loadFile);
			add(fileBar);
			
			/// edit bar
			editBar = new JMenu("Edit");
			editMenuListener = new EditMenuListener();
			clearGrid = new JMenuItem("Clear grid (BACKSPACE)");
			clearGrid.addActionListener(new ResetAction());
			editBar.add(clearGrid);
			changeRule = new JMenuItem("Change game rules");
			changeRule.addActionListener(editMenuListener);
			editBar.add(changeRule);
			preferences = new JMenu("Preferences");
			preferencesMenuListener = new PreferencesMenuListener();
			changeColors = new JMenuItem("Colors");
			changeColors.addActionListener(preferencesMenuListener);
			preferences.add(changeColors);
			editBar.add(preferences);
			add(editBar);
			
			/// help bar (last)
			helpBar = new JMenu("Help");
			helpMenuListener = new HelpMenuListener();
			controls = new JMenuItem("Controls");
			controls.addActionListener(helpMenuListener);
			helpBar.add(controls);
			add(helpBar);
		}
		
		public class FileMenuListener implements ActionListener {

			@Override
			public void actionPerformed(ActionEvent e) {
				updatePause(true);
				if (e.getSource() == saveFile) {
					// check if grid is empty
					if (gridPanel.grid.isEmpty()) {
						JOptionPane.showMessageDialog(null, "Error: the grid is empty!", "Error", JOptionPane.PLAIN_MESSAGE);
						return;
					}
					JFileChooser fileChooser = new JFileChooser();
					for (FileNameExtensionFilter fileType:fileTypes) {
						fileChooser.addChoosableFileFilter(fileType);
					}
					fileChooser.setAcceptAllFileFilterUsed(false);
					if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
						File file = fileChooser.getSelectedFile();
						saveFile(file, (FileNameExtensionFilter)fileChooser.getFileFilter());
					}
				}
				else if (e.getSource() == loadFile) {
					JFileChooser fileChooser = new JFileChooser();
					for (FileNameExtensionFilter fileType:fileTypes) {
						fileChooser.addChoosableFileFilter(fileType);
					}
					fileChooser.setAcceptAllFileFilterUsed(false);
					if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
					  File file = fileChooser.getSelectedFile();
					  loadFile(file, (FileNameExtensionFilter)fileChooser.getFileFilter());
					}
				}
			}
		}
		
		public class EditMenuListener implements ActionListener{
			@Override
			public void actionPerformed(ActionEvent e) {
				updatePause(true);
				if (e.getSource() == changeRule) {
					JPanel changeRulePanel = new JPanel();
					changeRulePanel.setLayout(new GridLayout(4, 1));
					changeRulePanel.setFont(font);
					
					JLabel bornLabel = new JLabel("Select number of neighbours for cell to be born:");
					JLabel surviveLabel = new JLabel("Select number of neighbours for cell to survive:");
					JPanel bornOptionsPanel = new JPanel(new FlowLayout());
					JPanel surviveOptionsPanel = new JPanel(new FlowLayout());
					JCheckBox[] bornOptions = new JCheckBox[9];
					JCheckBox[] surviveOptions = new JCheckBox[9];
					
					bornLabel.setFont(font);
					surviveLabel.setFont(font);
					for (int i=0; i<9; i++) {
						bornOptions[i] = new JCheckBox(Integer.toString(i));
						bornOptions[i].setFont(font);
						if (bornRules.contains(i)) {
							bornOptions[i].doClick();
						}
						bornOptionsPanel.add(bornOptions[i]);
						
						surviveOptions[i] = new JCheckBox(Integer.toString(i));
						surviveOptions[i].setFont(font);
						if (surviveRules.contains(i)) {
							surviveOptions[i].doClick();
						}
						surviveOptionsPanel.add(surviveOptions[i]);
					}
					
					changeRulePanel.add(bornLabel);
					changeRulePanel.add(bornOptionsPanel);
					changeRulePanel.add(surviveLabel);
					changeRulePanel.add(surviveOptionsPanel);
					
					int result = JOptionPane.showConfirmDialog(null, changeRulePanel, "Enter rule:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
					
					if (result == JOptionPane.OK_OPTION) {
						// update rules
						bornRules.clear();
						surviveRules.clear();
						
						for (int i=0; i<9; i++) {
							if (bornOptions[i].isSelected()) {
								bornRules.add(i);
							}
							if (surviveOptions[i].isSelected()) {
								surviveRules.add(i);
							}
						}
					}
				}
			}
		}
		
		public class PreferencesMenuListener implements ActionListener{
			private JPanel colorChangePanel;
			private JButton changeGridColor;
			private JButton changeYoungColor;
			private JButton changeOldColor;
			private JPanel gridPreview;
			private JPanel youngPreview;
			private JPanel oldPreview;
			private Color newGridColor;
			private Color newYoungColor;
			private Color newOldColor;
			private Color oldGridColor;
			private Color oldYoungColor;
			private Color oldOldColor;
			
			private ColorChangeListener colorChangeListener;
			
			private JSlider ageSlider;
			private AgeSliderListener ageSliderListener;
			private JPanel ageChangerInfo;
			private JLabel ageInfo;
			private JTextField ageChanger;
			private AgeChangerListener ageChangerListener;
			private int oldAge;
			private int newAge;
			
			@Override
			public void actionPerformed(ActionEvent e) {
				if (e.getSource() == changeColors) {
					updatePause(true);
					// prepare old and new preferences
					oldGridColor = Tile.gridColor;
					oldYoungColor = Tile.youngColor;
					oldOldColor = Tile.oldColor;
					oldAge = Tile.maxAge;
					
					newGridColor = Tile.gridColor;
					newYoungColor = Tile.youngColor;
					newOldColor = Tile.oldColor;
					newAge = Tile.maxAge;
					
					colorChangePanel = new JPanel();
					colorChangePanel.setLayout(new GridLayout(4, 2));
					colorChangePanel.setFont(font);
					changeGridColor = new JButton("Change Grid Color");
					changeYoungColor = new JButton("Change Young Cell Color");
					changeOldColor = new JButton("Change Old Cell Color");
					gridPreview = new JPanel();
					youngPreview = new JPanel();
					oldPreview = new JPanel();
					gridPreview.setBackground(newGridColor);
					youngPreview.setBackground(newYoungColor);
					oldPreview.setBackground(newOldColor);
					changeGridColor.setFont(font);
					changeYoungColor.setFont(font);
					changeOldColor.setFont(font);
					
					// make slider
					ageSlider = new JSlider(JSlider.HORIZONTAL, 0, newAge*2, newAge);
					ageSlider.setMajorTickSpacing(newAge/2);
					ageSlider.setMinorTickSpacing(newAge/10);
					ageSlider.setPaintTicks(true);
					ageSlider.setPaintLabels(true);
					
					ageSliderListener = new AgeSliderListener();
					ageSlider.addChangeListener(ageSliderListener);
					
					// make age changer
					ageChangerInfo = new JPanel(new BorderLayout());
					ageInfo = new JLabel(" Max age: ");
					ageChanger = new JTextField();
					ageChanger.setText(Integer.toString(newAge));
					ageChangerInfo.add(ageInfo, BorderLayout.WEST);
					ageChangerInfo.add(ageChanger, BorderLayout.CENTER);
					
					ageChangerListener = new AgeChangerListener();
					ageChanger.getDocument().addDocumentListener(ageChangerListener);
					
					// add components
					colorChangePanel.add(changeGridColor);
					colorChangePanel.add(ageSlider);
					colorChangePanel.add(gridPreview);
					colorChangePanel.add(ageChangerInfo);
					colorChangePanel.add(changeYoungColor);
					colorChangePanel.add(changeOldColor);
					colorChangePanel.add(youngPreview);
					colorChangePanel.add(oldPreview);
					
					// add listeners
					colorChangeListener = new ColorChangeListener();
					changeGridColor.addActionListener(colorChangeListener);
					changeYoungColor.addActionListener(colorChangeListener);
					changeOldColor.addActionListener(colorChangeListener);
					int result = JOptionPane.showConfirmDialog(null, colorChangePanel, "Change color:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
					if (result == JOptionPane.OK_OPTION) {
						Tile.gridColor = newGridColor;
						Tile.youngColor = newYoungColor;
						Tile.oldColor = newOldColor;
						Tile.maxAge = newAge;
					}
					else {
						Tile.gridColor = oldGridColor;
						Tile.youngColor = oldYoungColor;
						Tile.oldColor = oldOldColor;
						Tile.maxAge = oldAge;
					}
					
					gridPanel.updateTileColors();
					gridPanel.repaint();
				}
			}
			
			private class ColorChangeListener implements ActionListener {

				@Override
				public void actionPerformed(ActionEvent e) {
					JColorChooser chooseNewColor = new JColorChooser();
					chooseNewColor.setFont(font);
					if (e.getSource() == changeGridColor) {
						chooseNewColor.setColor(newGridColor);
						int result = JOptionPane.showConfirmDialog(null, chooseNewColor, "Select Grid Color:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
						if (result == JOptionPane.OK_OPTION) {
							newGridColor = chooseNewColor.getColor();
							gridPreview.setBackground(newGridColor);
						}
					}
					else if (e.getSource() == changeYoungColor) {
						chooseNewColor.setColor(newYoungColor);
						int result = JOptionPane.showConfirmDialog(null, chooseNewColor, "Select Young Cell Color:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
						if (result == JOptionPane.OK_OPTION) {
							newYoungColor = chooseNewColor.getColor();
							youngPreview.setBackground(newYoungColor);
						}
					}
					else if (e.getSource() == changeOldColor) {
						chooseNewColor.setColor(newOldColor);
						int result = JOptionPane.showConfirmDialog(null, chooseNewColor, "Select Old Cell Color:", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
						if (result == JOptionPane.OK_OPTION) {
							newOldColor = chooseNewColor.getColor();
							oldPreview.setBackground(newOldColor);
						}
					}
					
					Tile.gridColor = newGridColor;
					Tile.youngColor = newYoungColor;
					Tile.oldColor = newOldColor;
					
					gridPanel.updateTileColors();
					gridPanel.repaint();
				}
			}
			
			private class AgeSliderListener implements ChangeListener{
				@Override
				public void stateChanged(ChangeEvent e) {
					if (!ageSlider.getValueIsAdjusting()) {
						int age = ageSlider.getValue();
						newAge = age;
						ageChanger.getDocument().removeDocumentListener(ageChangerListener);
						ageChanger.setText(Integer.toString(newAge));
						ageChanger.getDocument().addDocumentListener(ageChangerListener);
						Tile.maxAge = newAge;
						
						gridPanel.updateTileColors();
						gridPanel.repaint();
					}
				}
			}
			
			private class AgeChangerListener implements DocumentListener{

				@Override
				public void changedUpdate(DocumentEvent e) {
				}

				@Override
				public void insertUpdate(DocumentEvent e) {
					updateSlider();
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					updateSlider();
				}
				
				private void updateSlider() {
					try {
						int possibleNewAge = Integer.parseInt(ageChanger.getText());
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								if (possibleNewAge > ageSlider.getMaximum()) {
									ageSlider.setMaximum(possibleNewAge);
									ageSlider.setLabelTable(null);
									ageSlider.setMajorTickSpacing(possibleNewAge/2);
									ageSlider.setMinorTickSpacing(possibleNewAge/10);
								}
								ageSlider.setValue(possibleNewAge);
							}
						});
					}
					catch (NumberFormatException e1){}
				}
			}
		}
		
		public class HelpMenuListener implements ActionListener {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				updatePause(true);
				if (e.getSource() == controls) {
					JOptionPane.showMessageDialog(null, helpMessage, "Controls", JOptionPane.PLAIN_MESSAGE);
				}
			}
		}
	}
	
	private class MoveCamAction extends AbstractAction{
		private final int dx;
		private final int dy;
		
		public MoveCamAction(int dx, int dy) {
			this.dx = dx;
			this.dy = dy;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			gridPanel.dragScreen(dx, dy);
		}
	}
	
	private class ZoomAction extends AbstractAction{
		private final boolean zoomIN;
		
		public ZoomAction(boolean zoomIN) {
			this.zoomIN = zoomIN;
		}
		
		@Override
		public void actionPerformed(ActionEvent e) {
			double dzoomFactor;
			if (zoomIN) {
				dzoomFactor = 0.05;
			}
			else {
				dzoomFactor = -0.05;
			}
			gridPanel.zoomRedraw(dzoomFactor);
		}
	}
	
	private class PauseAction extends AbstractAction{
		@Override
		public void actionPerformed(ActionEvent e) {
			updatePause(!gridPanel.pause);
		}
	}
	
	private class StepFrameAction extends AbstractAction{
		@Override
		public void actionPerformed(ActionEvent e) {
			updateGrid(prepareUpdateGrid());
			gridPanel.repaint();
			gridPanel.redrawMap();
		}
	}
	
	private class ResetAction extends AbstractAction{
		@Override
		public void actionPerformed(ActionEvent e) {
			updatePause(true);
			gridPanel.topLeftX = 0;
			gridPanel.topLeftY = 0;
			gridPanel.resetGrid();
			gridPanel.repaint();
		}
	}
	
	private class GridToggleAction extends AbstractAction{

		@Override
		public void actionPerformed(ActionEvent e) {
			gridPanel.gridOn = !gridPanel.gridOn;
			toolBar.updateGridButton(gridPanel.gridOn);
			gridPanel.repaint();
		}
		
	}
	
	private class SpeedUpAction extends AbstractAction{

		@Override
		public void actionPerformed(ActionEvent e) {
			gridPanel.msDelay -= 10;
			if (gridPanel.msDelay < gridPanel.minDelay) {
				gridPanel.msDelay = gridPanel.minDelay;
			}
			gridPanel.repaint();
		}
		
	}
	
	private class SlowDownAction extends AbstractAction{
		
		@Override
		public void actionPerformed(ActionEvent e) {
			gridPanel.msDelay += 10;
			gridPanel.repaint();
		}
	}
	
	private class ComponentResize extends ComponentAdapter{
		@Override
		public void componentResized(ComponentEvent e) {
			gridPanel.resizedRedraw(c.getWidth()-toolBar.getWidth(), c.getHeight(), toolBar.getWidth(), 0);
		}
	}
}