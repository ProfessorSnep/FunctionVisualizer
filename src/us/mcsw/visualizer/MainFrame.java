package us.mcsw.visualizer;

import javax.swing.JFrame;

public class MainFrame extends JFrame {

	private static final long serialVersionUID = 1364457883486500374L;
	
	VisCanvas canvas = null;
	
	public MainFrame() {
		// Initialize the JFrame components and values
		setTitle("Visualizer");
		// create the canvas object
		this.canvas = new VisCanvas();
		add(canvas);
		pack();
		setLocationRelativeTo(null);
		setResizable(false);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		// start playing the animation
		canvas.startAnim();
	}
	
	public static void main(String[] args) {
		// Create the JFrame and make it visible
		MainFrame frame = new MainFrame();
		frame.setVisible(true);
	}

}
