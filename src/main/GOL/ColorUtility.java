package main.GOL;

import java.awt.Color;

public final class ColorUtility {
	static Color getGradient(Color c1, Color c2, float p) {
		int R = (int) (c1.getRed() * p + c2.getRed() * (1.0 - p));
		int G = (int) (c1.getGreen() * p + c2.getGreen() * (1.0 - p));
		int B = (int) (c1.getBlue() * p + c2.getBlue() * (1.0 - p));
		
		return new Color(R, G, B);
	}
}
