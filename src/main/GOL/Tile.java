package main.GOL;

import java.awt.Color;

public class Tile {
	private int x;
	private int y;
	public int age;
	private Color color;
	public static Color gridColor = Color.BLACK;
	public static Color youngColor = new Color(204, 102, 0);
	public static Color oldColor = new Color(100, 0, 0);
	public int maxAge=100;
	
	public Tile(int x, int y) {
		changeCoordinates(x, y);
	}
	
	public void setBackground(Color color) {
		this.color = color;
	}
	
	public Color getBackgroundColor() {
		return this.color;
	}
	
	// -1 for dead
	public void updateMark(int age) {
		this.age = age;
		this.updateColor();
	}
	
	public void updateColor() {
		if (age == -1)
			setBackground(gridColor);
		else if (age >= maxAge)
			setBackground(oldColor);
		else {
			float pToMin = (float)1.0-(float)age/maxAge;
			setBackground(ColorUtility.getGradient(youngColor, oldColor, pToMin));
		}
	}
	
	public void changeCoordinates(int x, int y) {
		this.x = x; this.y = y;
	}
	
	public int getX() {
		return x;
	}
	
	public int getY() {
		return y;
	}
}
