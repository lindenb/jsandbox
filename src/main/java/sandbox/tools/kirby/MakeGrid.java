package sandbox.tools.kirby;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.beust.jcommander.Parameter;

import sandbox.ImageUtils;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.jcommander.DimensionConverter;
import sandbox.jcommander.NoSplitter;

public class MakeGrid extends Launcher
	{
	protected static final Logger LOG=Logger.builder(MakeGrid.class).build();
    
    @Parameter(names={"-o","--output"},description="output name")
    private Path out = null; 
    @Parameter(names={"--size","--dimension"},description="Image output size",converter=DimensionConverter.StringConverter.class,splitter=NoSplitter.class)
    private Dimension viewRect = new Dimension(1000, 1000);
    @Parameter(names={"--dx","-x"},description="square width")
    private double dx=10;
    @Parameter(names={"--dy","-y"},description="square height. if negative: use same value as dx.")
    private double dy=-1;
    @Parameter(names={"--precision"},description="Point will be randomly set to +/- precision.")
    private double precision= 0.0;
    @Parameter(names={"--stroke-width"},description="stroke width")
    private double stroke_width= 0.5;



    private final Random random = new Random(System.currentTimeMillis());
    private final ImageUtils imageUtils = ImageUtils.getInstance();
    
	private final void drawPath(final Graphics2D g,final List<Point2D> points) {
		g.setColor(Color.BLACK);
		final Stroke oldStroke = g.getStroke();
		g.setStroke(new BasicStroke((float)this.stroke_width,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
		for(int i=0;i+1<points.size();i++) {
			final Line2D line = new Line2D.Double(points.get(i),points.get(i+1));
			g.draw(line);
			}
		g.setStroke(oldStroke);
		}
	
    private Point2D makePoint(double x,double y) {
    	double ddx = (random.nextGaussian() * this.precision)*(random.nextBoolean()?1:-1);
    	double ddy = (random.nextGaussian() * this.precision)*(random.nextBoolean()?1:-1);
    	return new Point2D.Double(x+ddx,y+ddy);
    	}
    @Override
    public int doWork(final List<String> args) {
		try
			{
			if(!args.isEmpty()) {
				LOG.error("Illegal number of arguments");
				return -1;
				}
			if(dx<=0) {
				LOG.error("Bad dx");
				return -1;
				}
			if(dy<=0) dy=dx;
			
			final BufferedImage img = new BufferedImage(
					this.viewRect.width,
					this.viewRect.height,
					BufferedImage.TYPE_INT_RGB
					);
			
			final Graphics2D g = this.imageUtils.createGraphics(img);
			g.setColor(Color.WHITE);
			g.fillRect(0,0,this.viewRect.width,this.viewRect.height);
			
			final List<Point2D> point1s = new ArrayList<>();
			double x=0;
			while(x<= this.viewRect.width) {
				point1s.clear();
				double y=0;
				while(y<= this.viewRect.height) {
					point1s.add(makePoint(x,y));
					y+=dy;	
					}
				drawPath(g, point1s);
				x+=dx;
				}
			
			double y=0;
			while(y<= this.viewRect.height) {
				x=0;
				point1s.clear();
				while(x<= this.viewRect.width) {
					point1s.add(makePoint(x,y));				
					x+=dx;
					}
				drawPath(g, point1s);
				y+=dy;	
				}	
			
			this.imageUtils.saveToPathOrStandout(img, this.out);
			return 0;
			} 
		catch(final Throwable err)
			{
			LOG.error(err);
			return -1;
			}
		}
	public static void main(final String[] args) {
		new MakeGrid().instanceMainWithExit(args);
		}
		
	}
