package main.GOL;

import java.awt.EventQueue;
import java.lang.reflect.InvocationTargetException;

public class GameOfLife{
	public static void main(String[] args) {
		MainFrame mainFrame = new MainFrame("GOL");
		
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
						e.printStackTrace();
					}
					if (!mainFrame.gridPanel.pause) {
						EventQueue.invokeLater(new Runnable() {
							public void run() {
								mainFrame.updateGrid();
							}
						});
					}
				}
			}
		}).start();
	}
}
