package sandbox.drawing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Random;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import com.beust.jcommander.Parameter;

import sandbox.Logger;
import sandbox.NoSplitter;
import sandbox.StringUtils;
import sandbox.jcommander.DoubleParamSupplier;

public class Hatching01 extends AbstractDrawingProgram {
	
	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.builder(Hatching01.class).build();
	
	@Parameter(names= {"-p","--precision",},description="precision",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier precision = DoubleParamSupplier.createDefault(1);
	@Parameter(names= {"-d","--shift","--distance"},description="shift, distance between lines",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier distanceBetweenLines = DoubleParamSupplier.createRandomBetween(-3, 3);
	@Parameter(names= {"-n","--ticks",},description="ticks distance",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier ticksDistance = DoubleParamSupplier.createDefault(5);
	@Parameter(names= {"-T","--ticks-height",},description="ticks height",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier ticksHeight = DoubleParamSupplier.createDefault(15);
	@Parameter(names= {"-t","--alpha",},description="alpha",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier alpha = DoubleParamSupplier.createRandomBetween(0.8,1);
	@Parameter(names= {"-ti","--ticks-increase",},description="ticks height increase by factor each line",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier ticksIncrease = DoubleParamSupplier.createDefault(1);
	@Parameter(names= {"-a","--angles"},description="angles in degree")
	private String anglesStr="0";
	@Parameter(names= {"-ai","--angle-precision"},description="angle precision in degree",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier anglePrecisionDeg =DoubleParamSupplier.createRandomBetween(1,2);
	@Parameter(names= {"-gray","--gray"},description="gray value",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier grayValue =DoubleParamSupplier.createRandomBetween(0,0.2);
	@Parameter(names= {"-s","--stroke",},description="stroke width",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	protected DoubleSupplier strokeWidth = DoubleParamSupplier.createRandomBetween(0.8,1.2);

	
	private final Random rnd=new Random(System.currentTimeMillis());
	
	private double delta() {
		return this.precision.getAsDouble() * rnd.nextDouble() * (rnd.nextBoolean()?-1.0:1.0);
		}

	private double averageAt(double y_max_array[],double xreal) {
		if(xreal<=0) return y_max_array[0];
		if(xreal>=y_max_array.length) return y_max_array[y_max_array.length-1];
		int x1=(int)Math.floor(xreal);
		int x2=(int)Math.min(Math.ceil(xreal),y_max_array.length);
		double sum=0;
		double n=0;
		while(x1 <= x2) {
			sum+=y_max_array[x1];
			x1++;
			n++;
			}
		return sum/n;
		}
	
	@Override
protected void paint(BufferedImage img, Graphics2D g) {
	
	Supplier<Composite> alphaSupplier=()->AlphaComposite.getInstance(
			AlphaComposite.SRC_OVER,
			Math.min(1f,Math.max(0f,(float)alpha.getAsDouble())));
		
	
	final double centerX = img.getWidth()/2.0;
	final double centerY = img.getHeight()/2.0;
	
	for(double radian : Arrays.stream(this.anglesStr.split("[,; ]")).
		filter(S->!StringUtils.isBlank(S)).
		mapToDouble(S->Double.parseDouble(S)).
		map(Math::toRadians).
		toArray())
		{
		final AffineTransform oldtr= g.getTransform();
		final AffineTransform tr= new AffineTransform();
		tr.concatenate(AffineTransform.getTranslateInstance(
				centerX ,
				centerY
				));
		tr.concatenate(AffineTransform.getRotateInstance(radian + (rnd.nextBoolean()?-1:1)*Math.toRadians(this.anglePrecisionDeg.getAsDouble())));
		g.setTransform(tr);
		
		final double maxD= Point.distance(0, 0, img.getWidth(), img.getHeight());
		double y_max_array[]=new double[(int)maxD];
		final int min_x_int= (int)(centerX - maxD);
		
		/* initialize ymax array */
		for(int x=0;x< y_max_array.length;x++) {
			y_max_array[x]= centerY - maxD + delta();
			}
		
		double hatchHeight = ticksHeight.getAsDouble();
		
		for(;;) {
			final double y_max_next[]=Arrays.copyOf(y_max_array, y_max_array.length);
		
			double x1 = centerX - maxD + delta();
			int prev_x_int=0;
			double y_min=Double.MAX_VALUE;
			
			while(x1 < centerX + maxD) {
				double y1 = averageAt(y_max_array, x1) + this.distanceBetweenLines.getAsDouble()+ delta();
				double x2 = x1+delta();
				double y2 = y1 + hatchHeight + delta();
				y_min= Math.min(y_min,y2);
				
				int xi = (int)x2-min_x_int;
				while(prev_x_int <= xi) {
					if(prev_x_int>=0 && prev_x_int<y_max_next.length) {
						y_max_next[prev_x_int]=y2;
						}
					prev_x_int++;
					}
				
				final float gray = Math.max(0,Math.min(1f,(float)grayValue.getAsDouble()));
				g.setColor(new Color(gray,gray,gray));
				g.setComposite(alphaSupplier.get());
				g.setStroke(new CurvedStroke(new BasicStroke((float)this.strokeWidth.getAsDouble())));
				g.draw(new Line2D.Double(x1, y1, x2, y2));
				
				x1 += this.ticksDistance.getAsDouble();;
				}
			
			if(y_min> centerY+maxD) break;
			
			/* fill remaining */
			while(prev_x_int <y_max_next.length) {
				if(prev_x_int>0 && prev_x_int<y_max_next.length) {
					y_max_next[prev_x_int]=y_max_next[prev_x_int-1];
					}
				prev_x_int++;
				}
			
			y_max_array= y_max_next;
			hatchHeight*= Math.max(1.0,ticksIncrease.getAsDouble());
			}
		
		
		g.setTransform(oldtr);
		}
	}
	
	public static void main(final String[] args) {
		new Hatching01().instanceMainWithExit(args);
	}
}
