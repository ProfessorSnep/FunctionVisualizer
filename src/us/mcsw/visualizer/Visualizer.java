package us.mcsw.visualizer;

import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;

public class Visualizer {

	// number of pixels per 1-number increase of graph
	static final int scale = 25;

	// the size of the image to render on, should be the same as the canvas size
	static final int size = 800;

	// the function for the modified x axis
	static FunctionRunnable xaxis = new FunctionRunnable() {
		public double[] run(double x) {
			return new double[] { x * x };
		}
	};

	// the function for the modified y axis
	static FunctionRunnable yaxis = new FunctionRunnable() {
		public double[] run(double y) {
			return new double[] { y * y };
		}
	};

	// the function to graph
	static FunctionRunnable run = new FunctionRunnable() {
		public double[] run(double x) {
			return new double[] { x * x };
		}
	};

	static AlteredAxesFunction func = new AlteredAxesFunction(xaxis, yaxis, run);

	// number of ticks the animation should run for
	static int ticks = 5 * (int) ThreadAnimation.UPDATE_RATE;

	// animated graph for the x axis
	static AnimatedGraph anim_xaxis = new AnimatedGraph(new FunctionRunnable() {
		public double[] run(double x) {
			return new double[] { 0 };
		}
	}, func.xaxis_runnable, 10000, ticks);
	// animated graph for the y axis
	static AnimatedGraph anim_yaxis = new AnimatedGraph(new VerticalLineFunction(0), func.yaxis_runnable, 10000, ticks);
	// animated graph for the actual function
	static AnimatedGraph graph = new AnimatedGraph(run, func, 100000, ticks);

	// draw the graphs to the graphics object
	public static void draw(VisCanvas canvas, Graphics2D g, int ticks) {
		anim_xaxis.graphPoints(g, Color.gray);
		anim_yaxis.graphPoints(g, Color.darkGray);

		graph.graphPoints(g, graph.isRunning() ? Color.cyan : Color.green);
	}

	// update the current animation
	public static void update(VisCanvas canvas, int ticks) {
		// start the animation after some amount of ticks
		if (ticks > ThreadAnimation.UPDATE_RATE * 2) {
			if (graph.isRunning()) {
				anim_xaxis.advancePoints();
				anim_yaxis.advancePoints();
				graph.advancePoints();
			}
		}
	}

	/********************
	 * HELPER FUNCTIONS *
	 ********************
	 */

	// returns an ArrayList with size [steps] of the graphed points for the function
	public static ArrayList<Point> getPointsForFunction(FunctionRunnable func, int steps) {
		return getPointsForPiecewise(func, -(size / scale), (size / scale), steps);
	}

	// returns an ArrayList with size [steps] of the graphed points for the piecewise function
	public static ArrayList<Point> getPointsForPiecewise(PiecewiseRunnable func, double ti, double te, int steps) {
		ArrayList<Point> ret = new ArrayList<>();

		double t = ti;
		double incr = (te - ti) / (double) steps;
		for (int si = 0; si < steps; si++) {
			try {
				Point[] vals = func.runPiece(t);
				for (Point p : vals) {
					Point dr = p.toPixel();
					ret.add(dr);
				}
			} catch (ArithmeticException e) {
				e.printStackTrace();
			}
			t += incr;
		}

		return ret;
	}

	// draw the given function to the graphics object
	public static void drawFunction(Graphics2D g, FunctionRunnable func, int steps, Color color) {
		drawPiecewise(g, func, -(size / scale), (size / scale), steps, color);
	}

	// draw the given piecewise function, starting from t=[ti] and ending at t=[te]
	public static void drawPiecewise(Graphics2D g, PiecewiseRunnable func, double ti, double te, int steps,
			Color color) {
		g.setColor(color);
		for (Point p : getPointsForPiecewise(func, ti, te, steps)) {
			g.drawRect((int) p.x, (int) p.y, 1, 1);
		}
	}

	// basic piecewise function
	public static interface PiecewiseRunnable {
		public Point[] runPiece(double t);
	}

	// function class based on piecewise
	public static abstract class FunctionRunnable implements PiecewiseRunnable {

		public abstract double[] run(double x);

		public Point[] runPiece(double t) {
			ArrayList<Point> ret = new ArrayList<>();
			for (double d : run(t)) {
				ret.add(new Point(t, d));
			}
			return ret.toArray(new Point[0]);
		}
	}

	// piecewise function for a vertical line
	public static class VerticalLineFunction implements PiecewiseRunnable {

		double x = 0;

		public VerticalLineFunction(double x) {
			this.x = x;
		}

		@Override
		public Point[] runPiece(double t) {
			return new Point[] { new Point(x, t) };
		}

	}

	// function that takes in two other functions for each axis
	public static class AlteredAxesFunction implements PiecewiseRunnable {

		FunctionRunnable xaxis, yaxis, run;

		public PiecewiseRunnable xaxis_runnable, yaxis_runnable;

		public AlteredAxesFunction(FunctionRunnable xaxis, FunctionRunnable yaxis, FunctionRunnable run) {
			this.xaxis = xaxis;
			this.yaxis = yaxis;
			this.run = run;

			// turns the given x-axis function to a piecewise
			xaxis_runnable = new PiecewiseRunnable() {
				public Point[] runPiece(double t) {
					return new Point[] { new Point(t, xaxis.run(t)[0]) };
				}
			};

			// turns the given y-axis function to a piecewise
			yaxis_runnable = new PiecewiseRunnable() {
				public Point[] runPiece(double t) {
					return new Point[] { new Point(yaxis.run(t)[0], t) };
				}
			};
		}

		@Override
		public Point[] runPiece(double t) {
			double[] r = run.run(t);
			Point[] ret = new Point[r.length];
			for (int i = 0; i < r.length; i++) {
				// (x, y) => (x + yaxis(y), y + xaxis(x))
				ret[i] = new Point(yaxis.run(r[i])[0] + t, xaxis.run(t)[0] + r[i]);
			}
			return ret;
		}

		public void drawXAxis(Graphics2D g) {
			drawPiecewise(g, xaxis_runnable, -(size / scale), size / scale, 10000, Color.gray);
		}

		public void drawYAxis(Graphics2D g) {
			drawPiecewise(g, yaxis_runnable, -(size / scale), size / scale, 10000, Color.darkGray);
		}
	}

	// class for an animated graph of two functions
	public static class AnimatedGraph {

		ArrayList<MovingPoint> points = new ArrayList<>();

		PiecewiseRunnable begin, end;
		int steps, ticks;

		int ticksRemaining;

		// starts the graph at [begin], animates to [end] after [ticks] ticks
		public AnimatedGraph(PiecewiseRunnable begin, PiecewiseRunnable end, int steps, int ticks) {
			this.begin = begin;
			this.end = end;
			this.steps = steps;
			this.ticks = ticks;
			this.ticksRemaining = ticks;
			prepare();
		}

		// initialize the point arrays for both functions, transform them into MovingPoints
		private void prepare() {
			ArrayList<Point> initial = getPointsForPiecewise(begin, -(size / scale), size / scale, steps);
			ArrayList<Point> stop = getPointsForPiecewise(end, -(size / scale), size / scale, steps);

			for (int i = 0; i < initial.size(); i++) {
				Point p1 = initial.get(i);
				Point p2 = stop.get(i);
				MovingPoint mp = new MovingPoint(p1, p2, ticks);
				points.add(mp);
			}
		}

		// graph the current points
		public void graphPoints(Graphics2D g, Color color) {
			g.setColor(color);
			for (MovingPoint mp : points) {
				g.fillRect((int) mp.current.x, (int) mp.current.y, 1, 1);
			}
		}

		// move each point forward one step
		public void advancePoints() {
			for (MovingPoint mp : points) {
				mp.takeStep();
			}
			ticksRemaining--;
		}

		public boolean isRunning() {
			return ticksRemaining > 0;
		}

	}

	// basic point
	public static class Point {
		public double x, y;

		public Point(double x, double y) {
			this.x = x;
			this.y = y;
		}

		// turns a value-based point to its pixel location on the screen
		public Point toPixel() {
			double cx = this.x;
			double cy = this.y;
			cx *= scale;
			cy *= scale;
			cx += size / 2;
			cy += size / 2;
			cy = size - cy;
			return new Point(cx, cy);
		}

		// turns a pixel-based point to its true value position
		public Point toValue() {
			double cx = this.x;
			double cy = this.y;
			cy = size - cy;
			cx -= size / 2;
			cy -= size / 2;
			cx /= scale;
			cy /= scale;
			return new Point(cx, cy);
		}
	}

	// animated point
	public static class MovingPoint {
		public Point current;
		double dx, dy;

		// point starts at [begin] and moves toward [end] with each step
		public MovingPoint(Point begin, Point end, int numSteps) {
			this.current = begin;
			this.dx = end.x - begin.x;
			this.dy = end.y - begin.y;
			this.dx /= (double) numSteps;
			this.dy /= (double) numSteps;
		}

		public Point takeStep() {
			this.current.x += dx;
			this.current.y += dy;
			return this.current;
		}
	}

}
