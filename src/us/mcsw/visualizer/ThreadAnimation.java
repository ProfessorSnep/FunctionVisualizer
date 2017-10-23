package us.mcsw.visualizer;

public class ThreadAnimation extends Thread {

	VisCanvas canvas;

	public ThreadAnimation(VisCanvas canvas) {
		super("VisAnimThread");
		this.canvas = canvas;
	}

	// number of times to update the animation per second
	public static final double UPDATE_RATE = 15.0;

	@Override
	public void run() {
		// setup frame counting
		long lastTime = System.nanoTime();
		final double ns = 1000000000.0 / (double) UPDATE_RATE;
		double del = 0.0;
		// render and update loop
		while (canvas.isRunning()) {
			// find how many times we need to update before rendering
			long cur = System.nanoTime();
			del += (cur - lastTime) / ns;
			lastTime = cur;
			while (del >= 1) {
				canvas.update();
				del--;
			}
			canvas.render();
		}
	}

}
