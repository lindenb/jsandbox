package sandbox.drawing;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.geom.Arc2D;
import java.awt.image.BufferedImage;
import java.util.Random;
import java.util.function.DoubleSupplier;

import com.beust.jcommander.Parameter;

import sandbox.Logger;
import sandbox.jcommander.DoubleParamSupplier;
import sandbox.jcommander.NoSplitter;

public class RandomDots01 extends AbstractDrawingProgram {
	
	private static final Logger LOG = Logger.builder(RandomDots01.class).build();
	
	@Parameter(names= {"-r","--radius",},description="radius",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier radius = DoubleParamSupplier.createDefault(10);
	@Parameter(names= {"-t","--alpha",},description="alpha",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier alpha = DoubleParamSupplier.createRandomBetween(0.8,1);
	@Parameter(names= {"-gray","--gray"},description="gray value",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier grayValue =DoubleParamSupplier.createRandomBetween(0,0.2);
	@Parameter(names= {"-N","--num"},description="Number of dot . Integer: absolute count. FLoat: fraction of area")
	private String countStr ="0.0001";
	@Parameter(names= {"-s","--shape"},description="shape")
	private ShapeFactory shapeFactory = ShapeFactory.circle;


	
	private final Random rnd=new Random(System.currentTimeMillis());

	@Override
	protected int paint(BufferedImage img, Graphics2D g) {
		long n_dots=1000;
		try {
			n_dots= Long.parseLong(countStr);
			}
		catch(NumberFormatException err) {
			try {
				double fract = Double.parseDouble(countStr);
				if(fract < 0 || fract > 1.0) {
					LOG.error("Bad fraction: "+ countStr);
					return -1;
					}
				n_dots= (long)(fract*((long)img.getWidth()*(long)img.getHeight()));
				}
			catch(NumberFormatException err2) {
				LOG.error("Bad numer of dots: "+ countStr);
				return -1;
				}
			}
		
		
		final Composite oldComposite = g.getComposite();
		while(n_dots > 0L) {
			double cx = rnd.nextInt(img.getWidth());
			double cy = rnd.nextInt(img.getHeight());
			double cr = this.radius.getAsDouble();
			float gray = (float)Math.min(1,Math.max(0.0,grayValue.getAsDouble()));
			g.setColor(new Color(gray,gray,gray));
			g.setComposite(AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER,
					Math.min(1f,Math.max(0f,(float)alpha.getAsDouble()))));
			g.fill(this.shapeFactory.create(cx, cy, cr));
			
			n_dots--;
		}
		g.setComposite(oldComposite);
		return 0;
	}
	
	public static void main(final String[] args) {
		new RandomDots01().instanceMainWithExit(args);
	}
}
