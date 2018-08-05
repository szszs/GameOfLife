package main.GOL;

import java.awt.EventQueue;
import java.awt.Point;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;

public class GameOfLife{
	public static void main(String[] args) {
		MainFrame mainFrame = new MainFrame("Game Of Life");
		
		try {
			EventQueue.invokeAndWait(new Runnable() {
				public void run() {
					mainFrame.startWindow();
				}
			});
		} catch (InvocationTargetException e1) {
			e1.printStackTrace();
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		new Thread ( new Runnable () {
			@Override
			public void run() {
				while (true) {
					try {
						Thread.sleep(mainFrame.gridPanel.msDelay);
					} catch (InterruptedException e) {
						break;
					}
					if (!mainFrame.gridPanel.pause) {
						ArrayList<ArrayList<Point>> prepareUpdate = mainFrame.prepareUpdateGrid();
						if (!mainFrame.gridPanel.pause) {
							mainFrame.updateGrid(prepareUpdate);
							EventQueue.invokeLater(new Runnable() {
								public void run() {
									mainFrame.gridPanel.redrawMap();
									mainFrame.gridPanel.repaint();
								}
							});
						}
					}
				}
			}
		}).start();
	}
}
