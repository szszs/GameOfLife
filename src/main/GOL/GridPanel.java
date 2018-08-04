package main.GOL;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class GridPanel extends JPanel{
	// width and height of viewable grid in tiles, excluding the outer border
	public int viewableHeight;
	public int viewableWidth;
	
	// width and height of window in pixels, including visible borders
	public int gridPixelWidth;
	public int gridPixelHeight;
	
	// size of one square tile
	private final int baseTileSize;
	private int tileSize;
	
	// upper left border coordinates of screen
	public int topLeftX;
	public int topLeftY;
	
	// relative mouse coordinates
	private int mouseX;
	private int mouseY;
	
	// color and font
	private static final Color highlightColor = new Color (0.5f, 0.5f, 0.5f, 0.5f);
	private static final Color BORDER_COLOR = Color.DARK_GRAY;
	private static final Color textColor = Color.WHITE;
	private static final Color infoBackgroundColor = new Color(0f, 0f, 0f, 0.5f);
	private static final Font textFont = new Font("Helvetica", Font.PLAIN, 20);
	
	/// border lengths
	// actual pixel offsets
	private int leftSideOffset=10;
	private int topSideOffset=10;
	
	// zoom factor
	private double zoomFactor=1.0;
	// zoom constants
	private static final double ZOOM_FACTOR_MAX=4.0;
	private static final double ZOOM_FACTOR_MIN=0.1;
	private static final double ZOOM_WHEEL_SPEED=0.15;
	
	// info
	public int steps;
	
	// viewable and actual grid
	private Tile[][] gridViewable;
	public Map<Point, Integer> grid = new ConcurrentHashMap<Point, Integer>();
	
	// actual grid lock
	ReadWriteLock gridLock = new ReentrantReadWriteLock();
	
	// control
	public boolean pause = true;
	public boolean gridOn = false;
	public int msDelay = 100;
	public final int minDelay = 10;
	
	public GridPanel(int newtileSize, int viewableHeight, int viewableWidth, int gridPixelWidth, int gridPixelHeight, int topLeftX, int topLeftY) {
		// setup panel
		super(null);
		setLayout(null);
		
		this.baseTileSize = newtileSize;
		this.tileSize = newtileSize;
		this.viewableHeight = viewableHeight;
		this.viewableWidth = viewableWidth;
		this.gridPixelWidth = gridPixelWidth;
		this.gridPixelHeight = gridPixelHeight;
		this.topLeftX = topLeftX;
		this.topLeftY = topLeftY;
		
		resetGrid();
		
		// add mouse listeners
		addMouseListener(new MouseGridListener());
		addMouseMotionListener(new MouseMovementGridListener());
		addMouseWheelListener(new MouseWheelGridListener());
	}
	
	@Override
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		
		removeAll();
		// paint background
		g2.setColor(Tile.gridColor);
		g2.fillRect(0, 0, gridPixelWidth, gridPixelHeight);
		// paint alive tiles
		for (int i=0; i<viewableHeight+2; i++) {
			for (int j=0; j<viewableWidth+2; j++) {
				Tile tile = gridViewable[i][j];
				if (tile.age != -1) {
					g2.setColor(tile.getBackgroundColor());
					g2.fillRect(-(tileSize-leftSideOffset)+j*tileSize, -(tileSize-topSideOffset)+i*tileSize, tileSize, tileSize);
				}
				if (gridOn) {
					g2.setColor(BORDER_COLOR);
					g2.drawRect(-(tileSize-leftSideOffset)+j*tileSize, -(tileSize-topSideOffset)+i*tileSize, tileSize, tileSize);
				}
			}
		}
		
		// paint highlight
		int relativeX = (mouseX+tileSize-leftSideOffset) / (tileSize);
		int relativeY = (mouseY+tileSize-topSideOffset) / (tileSize);
		
		Tile highlightedTile = gridViewable[relativeY][relativeX];
		
		g2.setColor(highlightColor);
		g2.fillRect(-(tileSize-leftSideOffset)+relativeX*tileSize, -(tileSize-topSideOffset)+relativeY*tileSize, tileSize, tileSize);
		
		// paint info
		FontMetrics fm = getFontMetrics(textFont);
		int fmVert = fm.getAscent();
		
		String firstLine = String.format("Generations: %d", steps);
		if (pause)
			firstLine += " (PAUSED)";
		String secondLine = String.format("Delay: %d ms", msDelay);
		
		String thirdLine = String.format("(%d, %d)", relativeX+topLeftX, relativeY+topLeftY);
		if (highlightedTile.age != -1)
			thirdLine += String.format(" Age: %d", highlightedTile.age);
		
		int infoBackgroundWidth = Math.max(fm.stringWidth(firstLine), Math.max(fm.stringWidth(secondLine), fm.stringWidth(thirdLine)));
		g2.setColor(infoBackgroundColor);
		g2.fillRect(0, 0, infoBackgroundWidth+3, fmVert*3+5);
		
		g2.setColor(textColor);
		g2.setFont(textFont);
		
		g2.drawString(firstLine, 0, fmVert);
		g2.drawString(secondLine, 0, fmVert*2);
		g2.drawString(thirdLine, 0, fmVert*3);
	}
	
	public synchronized void resetGrid() {
		steps = 0;
		gridViewable = new Tile[viewableHeight+2][viewableWidth+2];
		
		gridLock.writeLock().lock();
		
		try {
			grid = new ConcurrentHashMap<Point, Integer>();
		} finally {
			gridLock.writeLock().unlock();
		}
		
		// create tiles
		for (int i=0; i<viewableHeight+2; i++) {
			for (int j=0; j<viewableWidth+2; j++) {
				Tile newTile = createTile(topLeftX+j, topLeftY+i, -1);
				gridViewable[i][j] = newTile;
			}
		}
	}
	
	// this method doesn't clear the screen, make sure to do that first. also doesn't repaint the map
	private synchronized void remakeViewableTiles() {
		gridViewable = new Tile[viewableHeight+2][viewableWidth+2];
		// create buttons
		for (int i=0; i<viewableHeight+2; i++) {
			for(int j=0; j<viewableWidth+2; j++) {
				Point coordinate = new Point(j+topLeftX, i+topLeftY);
				int age = grid.containsKey(coordinate)?grid.get(coordinate):-1;
				Tile newTile = createTile(topLeftX+j, topLeftY+i, age);
				gridViewable[i][j] = newTile;
			}
		}
	}
	
	// updates the tiles. does not resize or move grid.
	public synchronized void redrawMap() {
		for (int i=0; i<viewableHeight+2; i++) {
			for(int j=0; j<viewableWidth+2; j++) {
				Tile checkTile = gridViewable[i][j];
				// check if coordinate is marked
				Point coordinate = new Point(topLeftX+j, topLeftY+i);
				int age = grid.containsKey(coordinate)?grid.get(coordinate):-1;
				checkTile.updateMark(age);
				
				checkTile.changeCoordinates(topLeftX+j, topLeftY+i);
			}
		}
	}
	
	public void updateTileColors() {
		for (int i=0; i<viewableHeight+2; i++) {
			for (int j=0; j<viewableWidth+2; j++) {
				Tile checkTile = gridViewable[i][j];
				checkTile.updateColor();
			}
		}
	}
	
	// grid must be initialized with tiles first!
	public void moveMap(int topLeftX, int topLeftY) {
		this.topLeftX = topLeftX;
		this.topLeftY = topLeftY;
		
		redrawMap();
		repaint();
	}
	
	private Tile createTile(int xcoord, int ycoord, int age) {
		Tile newTile = new Tile(xcoord, ycoord);
		newTile.updateMark(age);
		
		return newTile;
	}
	
	public boolean markGrid(int x, int y) {
		if (grid.put(new Point(x, y), 0) == null)
			return true;
		return false;
	}
	
	public boolean unmarkGrid(int x, int y) {
		if (grid.remove(new Point(x, y)) == null)
				return false;
		return true;
	}
	
	// returns if tile has been marked
	public boolean switchGrid(int x, int y) {
		if (markGrid(x, y))
			return true;
		else {
			unmarkGrid(x, y);
			return false;
		}
	}
	
	public void clickGrid(int clickedMouseX, int clickedMouseY, boolean mark) {
		int relativeX = (clickedMouseX+tileSize-leftSideOffset) / (tileSize);
		int relativeY = (clickedMouseY+tileSize-topSideOffset) / (tileSize);
		int x = relativeX + topLeftX;
		int y = relativeY + topLeftY;
		
		Tile tile = gridViewable[relativeY][relativeX];
		if (mark) {
			if (markGrid(x, y))
				tile.updateMark(0);
		}
		else {
			if (unmarkGrid(x, y))
				tile.updateMark(-1);
		}
		
		repaint();
	}
	
	// this function moves the offset and if the offset is out of range, it calls
	// the movescreen function as well.
	public void dragScreen(int dx, int dy) {
		// reverse directions; moving "right" is decreasing horizontal offset on left side
		// these refer to changes in top left offset
		dx *= -1;
		dy *= -1;
		
		int numTileMoveX=0;
		int numTileMoveY=0;
		
		if (dx > 0) {
			// moving left
			numTileMoveX -= (dx+leftSideOffset)/tileSize;
			leftSideOffset = (dx+leftSideOffset)%tileSize;
		}
		else {
			// moving right
			numTileMoveX += -(dx+leftSideOffset)/tileSize;
			leftSideOffset = (dx+leftSideOffset)%tileSize;
			if (leftSideOffset < 0) {
				leftSideOffset += tileSize;
				numTileMoveX += 1;
			}
		}
		
		if (dy > 0) {
			// moving up
			numTileMoveY -= (dy+topSideOffset)/tileSize;
			topSideOffset = (dy+topSideOffset)%tileSize;
		}
		else {
			// moving down
			numTileMoveY += -(dy+topSideOffset)/tileSize;
			topSideOffset = (dy+topSideOffset)%tileSize;
			if (topSideOffset < 0) {
				topSideOffset += tileSize;
				numTileMoveY += 1;
			}
		}
		
		if ((numTileMoveX != 0)||(numTileMoveY != 0))
			moveScreen(numTileMoveX, numTileMoveY);
		else
			repaint();
	}
	
	// this function moves the grid and preserves the old offset
	public void moveScreen(int dx, int dy) {
		topLeftX += dx; topLeftY += dy;
		this.removeAll();
		remakeViewableTiles();
		repaint();
	}
	
	public void zoomRedraw(double dzoomFactor) {
		zoomFactor += dzoomFactor;
		
		if (zoomFactor > ZOOM_FACTOR_MAX)
			zoomFactor = ZOOM_FACTOR_MAX;
		if (zoomFactor < ZOOM_FACTOR_MIN)
			zoomFactor = ZOOM_FACTOR_MIN;
		
		// calculate new tile size for the transform
		int oldSize = tileSize;
		int newSize = (int) ((float) baseTileSize*zoomFactor);
		
		int oldOffsetX = leftSideOffset;
		int oldOffsetY = topSideOffset;
		
		// location of cursor relative to tile proportions, before zoom
		float proportionX = (float)((mouseX-oldOffsetX)%oldSize)/oldSize;
		float proportionY = (float)((mouseY-oldOffsetY)%oldSize)/oldSize;
		
		// equals (mouseX/Y-newOffsetX/Y)%newSize, aka the new number of pixels the mouse should be
		// after zoom from the border of tile
		int solveThisX = Math.round(proportionX*newSize);
		int solveThisY = Math.round(proportionY*newSize);
		
		int newOffsetX = (mouseX-newSize-solveThisX)%newSize;
		int newOffsetY = (mouseY-newSize-solveThisY)%newSize;
		
		if (newOffsetX < 0)
			newOffsetX = newSize+newOffsetX;
		if (newOffsetY < 0)
			newOffsetY = newSize+newOffsetY;
		
		leftSideOffset = newOffsetX;
		topSideOffset = newOffsetY;
		
		int oldTileRelativeX = (mouseX+(oldSize-oldOffsetX)) / (oldSize);
		int oldTileRelativeY = (mouseY+(oldSize-oldOffsetY)) / (oldSize);
		
		int newTileRelativeX = (mouseX+(newSize-newOffsetX)) / (newSize);
		int newTileRelativeY = (mouseY+(newSize-newOffsetY)) / (newSize);
		
		// update tile size and viewable width/height based on zoom
		viewableWidth = gridPixelWidth/newSize;
		viewableHeight = gridPixelHeight/newSize;
		
		topLeftX = oldTileRelativeX - newTileRelativeX + topLeftX;
		topLeftY = oldTileRelativeY - newTileRelativeY + topLeftY;
		
		tileSize = newSize;
		
		// remake the viewable grid and redraw
		removeAll();
		remakeViewableTiles();
		repaint();
	}
	
	// resizes and redraws the grid by moving bottom right corner bounds
	public void resizedRedraw(int gridPixelWidth, int gridPixelHeight, int x, int y) {
		this.gridPixelWidth = gridPixelWidth;
		this.gridPixelHeight = gridPixelHeight;
		
		setBounds(x, y, gridPixelWidth, gridPixelHeight);
		if ((gridPixelWidth / tileSize) != viewableWidth) {
			this.removeAll();
			viewableWidth = gridPixelWidth / tileSize;
			remakeViewableTiles();
			repaint();
		}
		if ((gridPixelHeight / tileSize) != viewableHeight) {
			this.removeAll();
			viewableHeight = gridPixelHeight / tileSize;
			remakeViewableTiles();
			repaint();
		}
	}
	
	private class MouseWheelGridListener implements MouseWheelListener{

		@Override
		public void mouseWheelMoved(MouseWheelEvent e) {
			int step = e.getWheelRotation();
			zoomRedraw(-step*ZOOM_WHEEL_SPEED);
		}
		
	}
	
	private class MouseMovementGridListener implements MouseMotionListener{

		@Override
		public void mouseDragged(MouseEvent e) {
			int draggedMouseX = e.getX();
			int draggedMouseY = e.getY();
			if (draggedMouseX >= 0 && draggedMouseX <= gridPixelWidth && draggedMouseY >= 0 && draggedMouseY <= gridPixelHeight) {
				if (e.isControlDown()) {
					dragScreen(mouseX-draggedMouseX, mouseY-draggedMouseY);
				}
				else {
					if (SwingUtilities.isLeftMouseButton(e)) {
						List<Point> coordinates = GeometryUtility.getLine(mouseX, mouseY, draggedMouseX, draggedMouseY);
						for (Point coordinate:coordinates) {
							clickGrid(coordinate.x, coordinate.y, true);
						}
					}
					else if (SwingUtilities.isRightMouseButton(e)) {
						List<Point> coordinates = GeometryUtility.getLine(mouseX, mouseY, draggedMouseX, draggedMouseY);
						for (Point coordinate:coordinates) {
							clickGrid(coordinate.x, coordinate.y, false);
						}
					}
				}
				mouseX = draggedMouseX;
				mouseY = draggedMouseY;
			}
		}

		@Override
		public void mouseMoved(MouseEvent e) {
			int oldMouseX = mouseX;
			int oldMouseY = mouseY;
			
			if (e.getX() > gridPixelWidth)
				mouseX = gridPixelWidth;
			else if (e.getX() < 0)
				mouseX = 0;
			else
				mouseX = e.getX();
			
			if (e.getY() > gridPixelHeight)
				mouseY = gridPixelHeight;
			else if (e.getY() < 0)
				mouseY = 0;
			else
				mouseY = e.getY();
			
			if ((mouseX != oldMouseX)||(mouseY != oldMouseY)) {
				// repaint to reshow highlight
				repaint();
			}
		}
		
	}

	private class MouseGridListener implements MouseListener{

		@Override
		public void mouseClicked(MouseEvent e) {
			// TODO Auto-generated method stub
		}

		@Override
		public void mouseEntered(MouseEvent e) {
			// TODO Auto-generated method stub
			requestFocus();
			
		}

		@Override
		public void mouseExited(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void mousePressed(MouseEvent e) {
			requestFocus();
			
			if (!e.isControlDown()) {
				if (SwingUtilities.isLeftMouseButton(e)) {
					clickGrid(e.getX(), e.getY(), true);
				}
				else if (SwingUtilities.isRightMouseButton(e)) {
					clickGrid(e.getX(), e.getY(), false);
				}
			}
		}

		@Override
		public void mouseReleased(MouseEvent e) {
			// TODO Auto-generated method stub
			
		}
		
	}
}