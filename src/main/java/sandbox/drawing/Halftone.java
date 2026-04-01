package sandbox.drawing;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.util.function.DoubleSupplier;

import com.beust.jcommander.Parameter;

import sandbox.Logger;
import sandbox.jcommander.DoubleParamSupplier;
import sandbox.jcommander.NoSplitter;

public class Halftone extends AbstractDrawingProgram {
	@SuppressWarnings("unused")
	private static final Logger LOG = Logger.builder(Halftone.class).build();
	
	@Parameter(names= {"-x","-y","-s","--shift","--dx"},description="distance between points",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier distance1 = DoubleParamSupplier.createDefault(20);
	@Parameter(names= {"-p","--precision",},description="precision",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier precision = DoubleParamSupplier.createDefault(0);
	@Parameter(names= {"-r","radius",},description="radius",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier radius = DoubleParamSupplier.createDefault(5);
	@Parameter(names= {"-t","--alpha",},description="alpha",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier alpha = DoubleParamSupplier.createRandomBetween(0.8,1);
	@Parameter(names= {"-gray","--gray"},description="gray value",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier grayValue =DoubleParamSupplier.createRandomBetween(0,0.2);
	@Parameter(names= {"-m","--mark","--shape"},description="shape")
	private ShapeFactory markhape = ShapeFactory.square;
	@Parameter(names= {"-rotate"},description="roate shape by x degree",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	private DoubleSupplier rotate = DoubleParamSupplier.createDefault(0);

	
	
	@Override
	protected int paint(BufferedImage img, Graphics2D g) {
		double y=0;
		int row=0;
		while(y<= img.getHeight())
			{
			double x = (row%2==0?0:this.distance1.getAsDouble()/2.0);
			while(x<= img.getWidth())
				{
				final double radius = this.radius.getAsDouble();
				final double radian =rotate.getAsDouble();
				final Shape shape = this.markhape.create(
						x + this.precision.getAsDouble(),
						y + this.precision.getAsDouble(),
						radius + this.precision.getAsDouble(),
						Math.toDegrees(radian)
						);
				
				float a = (float)alpha.getAsDouble();
				if(a<0f) a=0f;
				if(a>1f) a=1f;
				g.setComposite(AlphaComposite.getInstance(
						AlphaComposite.SRC_OVER,
						Math.min(1f,Math.max(0f,a))));

				
				float gray = (float)Math.min(1,Math.max(0.0,grayValue.getAsDouble()));
				g.setColor(new Color(gray,gray,gray));

				g.fill(shape);
				x+=this.distance1.getAsDouble();
				}
			row++;
			y+=this.distance1.getAsDouble();
			}
			
		return 0;
		}
	
	public static void main(final String[] args) {
		new Halftone().instanceMainWithExit(args);
	}
}
