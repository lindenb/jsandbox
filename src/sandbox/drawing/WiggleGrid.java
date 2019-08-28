package sandbox.drawing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

import javax.imageio.ImageIO;

import com.beust.jcommander.Parameter;

import sandbox.ColorParser;
import sandbox.ImageUtils;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.NoSplitter;
import sandbox.jcommander.DimensionConverter;
import sandbox.jcommander.DoubleParamSupplier;

public class WiggleGrid extends Launcher {
	private static final Logger LOG = Logger.builder(WiggleGrid.class).build();
	@Parameter(names= {"-D","--size","--dim","--dimension"},converter=DimensionConverter.StringConverter.class,description=DimensionConverter.OPT_DESC,required=true)
	protected Dimension dimIn = null;
	@Parameter(names= {"-b","--background","--paper"},description="background-color. "+ColorParser.OPT_DESC,converter=ColorParser.Converter.class,splitter=NoSplitter.class)
	protected Color bckg = null;
	@Parameter(names= {"-f","--foreground","--pen"},description="foreground-color. "+ColorParser.OPT_DESC,converter=ColorParser.Converter.class,splitter=NoSplitter.class)
	protected Color penColor = Color.DARK_GRAY;
	@Parameter(names= {"-o","--output",},description="output")
	protected File output = null;
	
	@Parameter(names= {"-p","--precision",},description="precision",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	protected DoubleSupplier precision = DoubleParamSupplier.createDefault(5);
	@Parameter(names= {"-d","--shift","--distance"},description="shift, distance between lines",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	protected DoubleSupplier distanceBetweenLines = DoubleParamSupplier.createDefault(5);
	@Parameter(names= {"-s","--stroke",},description="stroke width",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	protected DoubleSupplier strokeWidth = DoubleParamSupplier.createDefault(1);
	@Parameter(names= {"-n","--hand",},description="hand distance",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	protected DoubleSupplier handDistance = DoubleParamSupplier.createDefault(100);
	@Parameter(names= {"-a","--alpha",},description="alpha",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	protected DoubleSupplier alpha = DoubleParamSupplier.createDefault(1);
	@Parameter(names= {"-B","--blockSize",},description="block",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	protected DoubleSupplier blockSize = DoubleParamSupplier.createDefault(200);

	
	private final Random rnd=new Random(System.currentTimeMillis());
	
	private double delta() {
		return this.precision.getAsDouble() * rnd.nextDouble() * (rnd.nextBoolean()?-1.0:1.0);
		}

	
	@Override
	public int doWork(final List<String> args) {
		final ImageUtils imageUtils = ImageUtils.getInstance();
		try
			{
			final BufferedImage img= new BufferedImage(dimIn.width, dimIn.height, BufferedImage.TYPE_INT_RGB);
			final Graphics2D g=img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			
			if(bckg==null && this.output!=null && !this.output.getName().toString().toLowerCase().endsWith(".png")) {
				bckg = Color.WHITE;
				}
			
			if(bckg!=null) {
				g.setColor(bckg);
				g.fillRect(0, 0, img.getWidth(), img.getHeight());
				}
			
			g.setColor(penColor);
			
			
			Supplier<Stroke> strokeSupplier=()->
				new WiggleStroke(new BasicStroke((float)this.strokeWidth.getAsDouble())).
					setPrecision(precision.getAsDouble()).
					setHandDistance(handDistance.getAsDouble()).
					setDisableQuad(true)
					;
			Supplier<Composite> alphaSupplier=()->AlphaComposite.getInstance(
					AlphaComposite.SRC_OVER,
					Math.min(1f,Math.max(0f,(float)alpha.getAsDouble())));
				
			//strokeSupplier=()->g.getStroke();
			Consumer<Line2D> paint=(L)->{
				final double blck = Math.max(2,blockSize.getAsDouble()+delta());
				double x1 = L.getX1();
				double y1 = L.getY1();
				double x2 = L.getX2();
				double y2 = L.getY2();
				for(;;) {
					double d = Point.distance(x1, y1, x2, y2);

					g.setStroke(strokeSupplier.get());
					g.setComposite(alphaSupplier.get());
					
					if(d<blck) {
						g.draw(new Line2D.Double(x1, y1,x2,y2));
						break;
						}
					double nPt= d/blck;
					double dx=(x2-x1)/nPt;
					double dy=(y2-y1)/nPt;
					double x3 = x1+dx + delta();
					double y3 = y1+dy + delta();
					
					g.draw(new Line2D.Double(x1, y1, x3, y3));
					x1 =x3;
					y1 = y3;
					}
				};
			
			// vertical
			double y1 = delta();
			for(;;) {
				if( y1>=this.dimIn.height) break;
				double x1= delta();
				
				double x2 = img.getWidth()+delta();
				double y2 = y1 + delta();
				
				paint.accept(new Line2D.Double(x1,y1,x2,y2));
				y1= Math.max(y1,y2)+this.distanceBetweenLines.getAsDouble() + delta();
				}
			
			// horizontal
			double x1 = delta();
			for(;;) {
				if(x1>=this.dimIn.width) break;
				y1= delta();
				double y2 = img.getHeight()+delta();
				double x2 = x1 + delta();
				
				paint.accept(new Line2D.Double(x1,y1,x2,y2));
				x1= Math.max(x1,x2) + this.distanceBetweenLines.getAsDouble() + delta();
				}
			
			// diag1
			y1 =  delta()- dimIn.width;
			for(;;) {
				if( y1>=this.dimIn.height+dimIn.width) break;
				x1= delta();
				double x2 = img.getWidth()+delta();
				double y2 = y1 + dimIn.width + delta();
				
				paint.accept(new Line2D.Double(x1,y1,x2,y2));
				y1 += Math.max(1,this.distanceBetweenLines.getAsDouble() + delta());
				}
			// diag2
			y1 =  delta()- dimIn.width;
			for(;;) {
				if( y1>=this.dimIn.height+dimIn.width) break;
				x1= delta();
				double x2 = img.getWidth()+delta();
				double y2 = y1 - dimIn.width + delta();
				
				paint.accept(new Line2D.Double(x1,y1,x2,y2));
				y1 += Math.max(1,this.distanceBetweenLines.getAsDouble() + delta());
				}
			
			
			g.dispose();
			
			ImageIO.write(img, imageUtils.formatForFile(output.getName()), output);
			return 0;
			}
		catch(final Throwable err) {
			LOG.error(err);
			return -1;
			}
		}
	
	public static void main(final String[] args) {
		new WiggleGrid().instanceMainWithExit(args);
	}
}
