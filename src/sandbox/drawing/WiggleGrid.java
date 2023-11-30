package sandbox.drawing;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;


import com.beust.jcommander.Parameter;

import sandbox.ImageUtils;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.colors.NamedColors;
import sandbox.colors.parser.ColorParser;
import sandbox.jcommander.DimensionConverter;
import sandbox.jcommander.DoubleParamSupplier;
import sandbox.jcommander.NoSplitter;

public class WiggleGrid extends Launcher {
	private static final Logger LOG = Logger.builder(WiggleGrid.class).build();
	@Parameter(names= {"-D","--size","--dim","--dimension"},converter=DimensionConverter.StringConverter.class,description=DimensionConverter.OPT_DESC,required=true)
	protected Dimension dimIn = null;
	@Parameter(names= {"-b","--background","--paper"},description="background-color. "+ColorParser.OPT_DESC,converter=ColorParser.Converter.class,splitter=NoSplitter.class)
	protected sandbox.colors.Color bckg = NamedColors.getInstance().findByName("white").get();
	@Parameter(names= {"-o","--output",},description="output")
	protected Path output = null;
	
	@Parameter(names= {"-p","--precision",},description="precision",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	protected DoubleSupplier precision = DoubleParamSupplier.createDefault(5);
	@Parameter(names= {"-d","--shift","--distance"},description="shift, distance between lines",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	protected DoubleSupplier distanceBetweenLines = DoubleParamSupplier.createDefault(5);
	@Parameter(names= {"-s","--stroke",},description="stroke width",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	protected DoubleSupplier strokeWidth = DoubleParamSupplier.createDefault(1);
	@Parameter(names= {"-n","--hand",},description="hand distance",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	protected DoubleSupplier handDistance = DoubleParamSupplier.createDefault(100);
	@Parameter(names= {"-t","--alpha",},description="alpha",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	protected DoubleSupplier alpha = DoubleParamSupplier.createDefault(1);
	@Parameter(names= {"-B","--blockSize",},description="block",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	protected DoubleSupplier blockSize = DoubleParamSupplier.createDefault(200);
	@Parameter(names= {"-a","--angles"},description="angles in degree")
	protected String anglesStr="0,90,45,-45";
	@Parameter(names= {"-ai","--angle-precision"},description="angle precision in degree",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	protected DoubleSupplier anglePrecisionDeg =DoubleParamSupplier.createRandomBetween(1,2);
	@Parameter(names= {"-gray","--gray"},description="gray value",converter=DoubleParamSupplier.class,splitter=NoSplitter.class)
	protected DoubleSupplier grayValue =DoubleParamSupplier.createRandomBetween(0,0.1);
	@Parameter(names= {"-hatching","--hatching"},description="hatching mode")
	protected boolean hatching = false;

	
	private final Random rnd=new Random(System.currentTimeMillis());
	
	private double delta() {
		return this.precision.getAsDouble() * rnd.nextDouble() * (rnd.nextBoolean()?-1.0:1.0);
		}

	
	@Override
	public int doWork(final List<String> args) {
		if(!args.isEmpty()) {
			LOG.error("illagel number of arguments "+String.join(",",args));
			return -1;
			}
		final ImageUtils imageUtils = ImageUtils.getInstance();
		try
			{
			final BufferedImage img= new BufferedImage(dimIn.width, dimIn.height, BufferedImage.TYPE_INT_RGB);
			final Graphics2D g=img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(bckg.toAWT());
			g.fillRect(0, 0, img.getWidth(), img.getHeight());
				
			
			
			
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

					float gray = Math.max(0,Math.min(1f,(float)grayValue.getAsDouble()));
					g.setColor(new java.awt.Color(gray,gray,gray));
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
			
			final double centerX = dimIn.getWidth()/2.0;
			final double centerY = dimIn.getHeight()/2.0;
	
			for(double radian : Arrays.stream(this.anglesStr.split("[,; ]")).
					filter(S->!StringUtils.isBlank(S)).
					mapToDouble(S->Double.parseDouble(S)).
					map(Math::toRadians).
					toArray())
				{
				
				final double maxD= Point.distance(0, 0, dimIn.getWidth(), dimIn.getHeight());
				double y1 = centerY - maxD + delta();
				for(;;) {
					if( y1>= centerY + maxD) break;
					final AffineTransform oldtr= g.getTransform();
					final AffineTransform tr= new AffineTransform();
					tr.concatenate(AffineTransform.getTranslateInstance(
							centerX ,
							centerY
							));
					tr.concatenate(AffineTransform.getRotateInstance(radian + (rnd.nextBoolean()?-1:1)*Math.toRadians(this.anglePrecisionDeg.getAsDouble())));
					g.setTransform(tr);
				
					
					double x1 = centerX - maxD + delta();
					double x2 = centerX + maxD + delta();
					double y2 = y1 + delta();
					double y_next= Math.max(y1,y2)+this.distanceBetweenLines.getAsDouble() + delta() ;
					if(hatching) {
						while(x1 < x2) {
							paint.accept(new Line2D.Double(x1,y1,x1+delta(),y_next));
							x1+=Math.max(0.1,this.precision.getAsDouble());
							}
						}
					else
						{
						paint.accept(new Line2D.Double(x1,y1,x2,y2));
						}
					y1= y_next;
					
					g.setTransform(oldtr);
					}
				}
			
			g.dispose();
			
			imageUtils.saveToPathOrStandout(img, output);
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
