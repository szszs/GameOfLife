package main.GOL;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

public final class GeometryUtility {
	static final List<Point> getLine(int x0, int y0, int x1, int y1) {
		List<Point> coordinates = new ArrayList<Point>();
		int dx = Math.abs(x1-x0);
		int dy = Math.abs(y1-y0);
		
		int sx = x0 < x1 ? 1 : -1;
		int sy = y0 < y1 ? 1 : -1;
		
		int err = dx-dy;
		int e2;
		
		while (true) {
			coordinates.add(new Point(x0, y0));
			
			if (x0 == x1 && y0 == y1)
				break;
			
			e2 = 2*err;
			
			if (e2 > -dy) {
				err -= dy;
				x0 += sx;
			}
			
			if (e2 < dx) {
				err += dx;
				y0 += sy;
			}
		}
		
		return coordinates;
	}
}
