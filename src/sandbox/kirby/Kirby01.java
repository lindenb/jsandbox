package sandbox.kirby;

import java.awt.Color;
import java.util.List;

import sandbox.Logger;
import sandbox.SimpleGraphics;

public class Kirby01 extends AbstractKirbyLauncher {
	private static final Logger LOG = Logger.builder(Kirby01.class).build();
	
	private class Circle
		{
		Color color;
		float alpha;
		double cx;
		double cy;
		double r;
		void paint(SimpleGraphics g) {
			if(this.alpha<=0) return;
			g.setAlpha(this.alpha);
			g.setFill(this.color);
			g.circle(cx, cy, r);
			}
		}
	
	

	
	
	@Override
	protected void paint(final SimpleGraphics g) {
		
		}
	
	@Override
	public int doWork(final List<String> args) {
		return 0;
		}
	
	public static void main(final String[] args) {
		new Kirby01().instanceMainWithExit(args);
	}
}
