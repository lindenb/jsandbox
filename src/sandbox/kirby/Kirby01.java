package sandbox.kirby;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.beust.jcommander.DynamicParameter;

import sandbox.AbstractIterator;
import sandbox.Logger;
import sandbox.SimpleGraphics;

public class Kirby01 extends AbstractKirbyLauncher {
	private static final Logger LOG = Logger.builder(Kirby01.class).build();
	@DynamicParameter(names = "-D", description = "Dynamic parameters go here",hidden=true)
	private Map<String, String> dynParams = new HashMap<>();

	private final Random random = new Random();
	
	private class Circle
		{
		Color color;
		float alpha = 1f;
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
	
	
	Iterator<Circle> circleIterator;
	
	class DefaultCircleIterator extends AbstractIterator<Circle>
		{
		int num=0;
		@Override
		protected Circle advance() {
			if(num>=1000) return null;
			num++;
			final Circle c= new Circle();
			c.color=Color.BLACK;
			c.cx = random.nextDouble()* dimIn.width;
			c.cy = random.nextDouble()* dimIn.height;
			c.r = random.nextDouble()* 50;
			return c;
			}
		}
	
	@Override
	protected void paint(final SimpleGraphics g) {
		while(circleIterator.hasNext()) {
			final Circle c = circleIterator.next();
			c.paint(g);
			}
		}
	
	@Override
	public int doWork(final List<String> args) {
		
		circleIterator=new DefaultCircleIterator();
		paint();
		return 0;
		}
	
	public static void main(final String[] args) {
		new Kirby01().instanceMainWithExit(args);
	}
}
