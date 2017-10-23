package us.mcsw.visualizer;

import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class VisCanvas extends Canvas {

	private static final long serialVersionUID = -3751853183360176775L;

	private static final int BUFFER_COUNT = 2;

	private BufferedImage image;
	private int pixels[];
	private Dimension dim;

	public VisCanvas() {
		this.dim = new Dimension(800, 800);
		setPreferredSize(dim);
		setSize(dim);

		// initialize the default image of the canvas
		image = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_RGB);
		pixels = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
	}

	int ticks = -1;
	ThreadAnimation renderer = null;

	public void update() {
		// update ticks and animation
		ticks++;
		Visualizer.update(this, ticks);
	}

	// NOTE: This render code was copy pasted from another project so there may
	// be extraneous code
	public void render() {
		if (!isValid())
			return;
		// create new buffer strategy
		BufferStrategy bs = getBufferStrategy();
		if (bs == null) {
			createBufferStrategy(BUFFER_COUNT);
			return;
		}

		// set all pixels to black
		for (int i = 0; i < pixels.length; i++)
			pixels[i] = 0x0;

		// get graphics object
		Graphics g = bs.getDrawGraphics();

		// setup image
		BufferedImage draw = new BufferedImage(dim.width, dim.height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D drawGraphics = draw.createGraphics();
		drawGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

		// render
		Visualizer.draw(this, drawGraphics, ticks);

		g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
		g.drawImage(draw, 0, 0, getWidth(), getHeight(), null);

		// clean up
		drawGraphics.dispose();
		g.dispose();
		bs.show();
	}

	public synchronized void startAnim() {
		// start the render thread
		ticks = 0;
		renderer = new ThreadAnimation(this);
		renderer.start();
	}

	public synchronized void stopAnim() {
		// stop the rendering
		ticks = -1;
		renderer = null;
	}

	public boolean isRunning() {
		return ticks >= 0;
	}

}
