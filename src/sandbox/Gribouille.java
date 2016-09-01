package sandbox;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public abstract class Gribouille extends AbstractApplication 
	{
	private static class GrayImageMap
		{
		private final Dimension dim;
		private final java.awt.image.DataBufferByte dataBuffer;
		GrayImageMap(final Dimension dim, final java.awt.image.DataBufferByte dataBuffer) {
			this.dim = dim;
			this.dataBuffer = dataBuffer;
			}
		float get(int x,int y) {
			return this.dataBuffer.getElem(dim.width*y+x)/255f;
			}
		}
	
	private class ProbabilityFunction
		{
		final int array[];
		ProbabilityFunction(int array[]) {
			this.array=array;
			Arrays.sort(this.array);
			int m = Integer.MAX_VALUE;
			int M = Integer.MIN_VALUE;
			
			}
		public int nextInt(int max) {
			double v = Gribouille.this.rand.nextDouble();
			return 0;
			}
		}
	
	private class Random2D
		{
		final int indexesx[];
		final int indexesy[];
		Random2D(final GrayImageMap gm) {
			this.indexesx=new int[gm.dim.width];
			this.indexesy=new int[gm.dim.height];
			float probay[] = new float[gm.dim.height];
			float probax[] = new float[gm.dim.width];
			for(int x=0;x< gm.dim.width;++x) {
				for(int y=0;y< gm.dim.height;++y) {
					float v = gm.get(x,y)/255f;
					probax[x]+=v;
					probay[y]+=v;
					}
				}
			}
	
		public double get(int x,int y) {
			double px=0;
			//x = (int)((x/Gribouille.this.imgDimension.getWidth())*dim.getWidth());
			//y = (int)((y/Gribouille.this.imgDimension.getHeight())*dim.getHeight());
			return -1;
			}
		}
	
	private class MinMax
		{
		double m,M;
		
		MinMax(double m,double M) {
			this.m = m;
			this.M = M;
		}
		
		void parse(final String s) {
			int slash = s.indexOf('/');
			if(slash==-1) slash=s.lastIndexOf('x');
			if(slash==-1) slash=s.lastIndexOf('X');
			if(slash==-1) slash=s.lastIndexOf(',');
			if(slash==-1) throw new IllegalArgumentException("bad min/max "+s);
			
			if(slash>0) this.m = Double.parseDouble(s.substring(0,slash));
			if(slash+1<s.length()) this.M= Double.parseDouble(s.substring(slash+1));
			}
		
		double min() { return Math.min(m, M);}
		double max() { return Math.max(m, M);}
		double distance() { return this.max()-this.min();}
		double rnd(final Random r) { return this.min()+ r.nextDouble()*this.distance();}
		}
	
	
	protected Random rand=new Random();
	protected File outputFile = null;
	protected Dimension imgDimension=new Dimension(100,100);
	protected BufferedImage image = null;
	
	protected Gribouille() {
		}
	
	
	protected GrayImageMap loadGrayMap(final File grayMapFile)  {
		LOG.info("Read gray map "+ grayMapFile);
		try {
			BufferedImage grayImage = ImageIO.read(grayMapFile);
			LOG.info("Grap type "+ grayImage.getType());
			if(grayImage.getType()!=BufferedImage.TYPE_BYTE_GRAY)
				{
				LOG.info("Converting to gray type "+ grayMapFile);
				BufferedImage grayImage2 = new BufferedImage(
						grayImage.getWidth(),
						grayImage.getHeight(),  
					    BufferedImage.TYPE_BYTE_GRAY
					    );  
				Graphics g = grayImage2.getGraphics();  
				g.drawImage(grayImage, 0, 0, null);  
				g.dispose();  
				grayImage = grayImage2;
				grayImage2=null;
				g=null;
				}
			final Raster raster = grayImage.getRaster();
			LOG.info("raster "+raster.getWidth()+"/"+raster.getHeight());
			final DataBuffer dataBuffer = raster.getDataBuffer();
			if(!(dataBuffer instanceof java.awt.image.DataBufferByte))
				{
				throw new IOException("Not a  java.awt.image.DataBufferByte:  "+grayMapFile);
				}
			return new GrayImageMap(
					new Dimension(raster.getWidth(), raster.getHeight()),
					java.awt.image.DataBufferByte.class.cast(dataBuffer)
					);
			} 
		catch(IOException err) {
			throw new RuntimeException(err);
			}
		}
	
	@Override
	protected void fillOptions(Options options) {
		options.addOption(Option.builder("seed").longOpt("seed").hasArg(true).desc("(long) random seed generator").build());
		options.addOption(Option.builder("dim").longOpt("dimension").hasArg(true).desc("output dimension. Can be either a numberXnumber expression or an existing image file.").build());
		options.addOption(Option.builder("o").longOpt("output").hasArg(true).desc("output file").build());
		super.fillOptions(options);
		}
	
	
	
	@Override
	protected Status decodeOptions(final CommandLine cmd)
		{
		if(cmd.hasOption("seed")) {
			this.rand = new Random(Long.parseLong(cmd.getOptionValue("seed")));
		}
		if(cmd.hasOption("o")) {
			this.outputFile = new File(cmd.getOptionValue("o"));
		}
		if(cmd.hasOption("dim")) {
			String dimStr = cmd.getOptionValue("dim");
			if(dimStr.toLowerCase().matches("\\d+x\\d+")) {
				int x_symbol = dimStr.toLowerCase().indexOf("x");
				this.imgDimension = new Dimension(
						Integer.parseInt(dimStr.substring(0,x_symbol)),
						Integer.parseInt(dimStr.substring(1+x_symbol))
						);
			} else {
				final File f= new File(dimStr);
				if(!f.exists() || !f.isFile()) {
					throw new IllegalArgumentException("not an existing file: "+f);
				}
				try(ImageInputStream in = ImageIO.createImageInputStream(f)){
				    final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
				    if (readers.hasNext()) {
				        ImageReader reader = readers.next();
				        try {
				            reader.setInput(in);
				            this.imgDimension =  new Dimension(reader.getWidth(0), reader.getHeight(0));
				        } finally {
				            reader.dispose();
				        }
				    }
				} 			
			catch(IOException err) {
				throw new IllegalArgumentException(err);
				}
			}
		}	
	return super.decodeOptions(cmd);
	}
	
	protected abstract void paint(Graphics2D g);
	
	@Override
	protected int execute(final CommandLine cmd)
		{
		if(this.imgDimension==null) {
			LOG.severe("undefined dimension.");
			return -1;
		}
		if(this.outputFile==null) {
			LOG.severe("undefined output.");
			return -1;
		}
		try {
			/* create image */
			this.image = new BufferedImage(this.imgDimension.width, this.imgDimension.height, BufferedImage.TYPE_INT_ARGB);
			Graphics2D g = this.image.createGraphics();
			paint(g);
			g.dispose();
			
			String format = "png";
			ImageIO.write(this.image, format, this.outputFile);
			return 0;
		} catch(Exception err) {
			err.printStackTrace();
			return -1;
		} finally {
			this.image = null;
		}
		}
	
	protected Color gray(double g,double alpha) {
		return new Color((float)g,(float)g,(float)g,(float)alpha);
	}
	
	protected Color black(double alpha) {
		return this.gray(0, alpha);
	}

	protected List<Point2D> wiggle(double x0,double y0,  double x1,double y1) {
		List<Point2D> L=new ArrayList<>();
		L.add(new Point2D.Double(x0, y0));
		L.add(new Point2D.Double(x1, y1));
		return L;
	}
	
	private static class X01 extends Gribouille
		{
		private MinMax alpha=new MinMax(0.5,1.0);
		private MinMax radius=new MinMax(0.5,50.0);
		private MinMax len=new MinMax(80,100);
		private double proba = 0.001;
		private boolean use_log=false;
		@Override
		protected void fillOptions(Options options) {
			options.addOption(Option.builder("alpha").longOpt("alpha").hasArg(true).desc("min/max alpha").build());
			options.addOption(Option.builder("radius").longOpt("radius").hasArg(true).desc("min/max radius").build());
			options.addOption(Option.builder("p").longOpt("proba").hasArg(true).desc("Propability").build());
			options.addOption(Option.builder("log").hasArg(false).desc("use log scale drawing").build());
			super.fillOptions(options);
			}
		@Override
		protected Status decodeOptions(CommandLine cmd) {
			if(cmd.hasOption("alpha")) {
				this.alpha.parse(cmd.getOptionValue("alpha"));
			}
			if(cmd.hasOption("radius")) {
				this.radius.parse(cmd.getOptionValue("radius"));
			}
			if(cmd.hasOption("p")) {
				this.proba =Double.parseDouble(cmd.getOptionValue("p"));
			}
			
			if(cmd.hasOption("log")) {
				this.use_log = true;
			}
			return super.decodeOptions(cmd);
			}
		
		@Override
		protected void paint(final Graphics2D g) {
			double angle = this.rand.nextDouble()*Math.PI*2.0;
			long occurences = (long)(((imgDimension.width)*(imgDimension.height))*this.proba);
			
	  		double x0 = rand.nextInt( this.image.getWidth() ) ;
	  		double y0 = rand.nextInt( this.image.getHeight() ) ;
	  		double coords[]=new double[6];
	  		GeneralPath gp  =new GeneralPath();
	  		gp.moveTo(x0, y0);
	  		
			int count=0;
			while(occurences>0) {
				angle += this.rand.nextDouble()*(Math.PI/10.0);
				double L = this.len.rnd(this.rand);
				double x1 = x0 + Math.cos(angle)*L;
				double y1 = y0 + Math.sin(angle)*L;
				
				coords[count++]=x1;
				coords[count++]=y1;
				++count;
				x0=x1;
				y0=y1;
				
				
				if(count==6)
					{
					g.setColor(this.black(this.alpha.rnd(this.rand)));
					
					gp.curveTo(
							coords[0],coords[1],
							coords[2],coords[3],
							coords[4],coords[5]
							);
					g.draw(gp);
	
					gp =new GeneralPath();
					
					
					if(x0<0 || x0>=imgDimension.width || y0<0 || y0>=imgDimension.height)
						{
						angle= rand.nextDouble()*Math.PI*2.0;
				  		x0 = rand.nextInt( this.image.getWidth() ) ;
				  		y0 = rand.nextInt( this.image.getHeight() ) ;
						}
	
					
			  		gp.moveTo(x0, y0);
			  		count=0;
					}
				
				occurences--;
				}
			}
		
		}

	
	private static class X02 extends Gribouille
		{
		private MinMax gray=new MinMax(0.0,0.3);
		private MinMax alpha=new MinMax(0.5,1.0);
		private MinMax radius=new MinMax(0.5,50.0);
		private MinMax len=new MinMax(80,100);
		private double proba = 0.001;
		@Override
		protected void fillOptions(Options options) {
			options.addOption(Option.builder("alpha").longOpt("alpha").hasArg(true).desc("min/max alpha").build());
			options.addOption(Option.builder("radius").longOpt("radius").hasArg(true).desc("min/max radius").build());
			options.addOption(Option.builder("p").longOpt("proba").hasArg(true).desc("Propability").build());
			super.fillOptions(options);
			}
		@Override
		protected Status decodeOptions(CommandLine cmd) {
			if(cmd.hasOption("alpha")) {
				this.alpha.parse(cmd.getOptionValue("alpha"));
			}
			if(cmd.hasOption("radius")) {
				this.radius.parse(cmd.getOptionValue("radius"));
			}
			if(cmd.hasOption("p")) {
				this.proba =Double.parseDouble(cmd.getOptionValue("p"));
			}
			
			
			return super.decodeOptions(cmd);
			}
		
		@Override
		protected void paint(final Graphics2D g) {
			double angle = this.rand.nextDouble()*Math.PI*2.0;
			long occurences = (long)(((imgDimension.width)*(imgDimension.height))*this.proba);
			
	  		double x0 = rand.nextInt( this.image.getWidth() ) ;
	  		double y0 = rand.nextInt( this.image.getHeight() ) ;
	  		double coords[]=new double[6];
	  		GeneralPath gp  =new GeneralPath();
	  		gp.moveTo(x0, y0);
	  		
			int count=0;
			for(long i=0;i< occurences;++i)
				{
		  		double gr = this.gray.rnd(this.rand) ;
			    double a = this.alpha.rnd(this.rand) ;
		  		double cx = rand.nextInt( this.image.getWidth() ) ;
		  		double cy = rand.nextInt( this.image.getHeight() ) ;
		  		double r = this.radius.rnd(this.rand) ;
				g.setColor(gray(gr, a));
				g.fill(new java.awt.geom.Ellipse2D.Double((cx-r), (cy-r),(r*2),(r*2)));
				}
		}
		}
	
	
	private static class Hatching01 extends Gribouille
		{
		private MinMax gray=new MinMax(0.0,0.3);
		private MinMax alpha=new MinMax(0.5,1.0);
		private MinMax radius=new MinMax(0.5,50.0);
		private MinMax len=new MinMax(80,100);
		private double proba = 0.001;
		@Override
		protected void fillOptions(Options options) {
			options.addOption(Option.builder("alpha").longOpt("alpha").hasArg(true).desc("min/max alpha").build());
			options.addOption(Option.builder("radius").longOpt("radius").hasArg(true).desc("min/max radius").build());
			options.addOption(Option.builder("p").longOpt("proba").hasArg(true).desc("Propability").build());
			super.fillOptions(options);
			}
		@Override
		protected Status decodeOptions(CommandLine cmd) {
			if(cmd.hasOption("alpha")) {
				this.alpha.parse(cmd.getOptionValue("alpha"));
			}
			if(cmd.hasOption("radius")) {
				this.radius.parse(cmd.getOptionValue("radius"));
			}
			if(cmd.hasOption("p")) {
				this.proba =Double.parseDouble(cmd.getOptionValue("p"));
			}
			
			
			return super.decodeOptions(cmd);
			}
		
		@Override
		protected void paint(final Graphics2D g) {
			//double angle = this.rand.nextDouble()*Math.PI*2.0;
			//long occurences = (long)(((imgDimension.width)*(imgDimension.height))*this.proba);
			int dx=5;
			for(int side=0;side<2;++side)
				{
				int p0 =- this.rand.nextInt(2*dx) -dx;
				while(p0 < (side==0 ?  this.image.getWidth(): this.image.getHeight()) )
					{
					List<Point2D> pts ;
					if( side==0)
						{
						pts = this.wiggle(p0, 0, p0, this.image.getWidth());
						}
					else
						{
						pts =this.wiggle(0,p0, this.image.getHeight(),p0);
						}
					
					//ctx->line_width(lwidth.rnd(&rand));
					g.setColor(black(this.alpha.rnd(this.rand)));
					
					GeneralPath gp=new GeneralPath();
					int i=0;
					while(i < pts.size())
						{
						if(i==0)
							{
							gp.moveTo(pts.get(i).getX(),pts.get(i).getY());
							i++;
							}
						else if(i+2< pts.size())
							{
							gp.curveTo(
								pts.get(i).getX(),pts.get(i).getY(),
								pts.get(i+1).getX(),pts.get(i+1).getY(),
								pts.get(i+2).getX(),pts.get(i+2).getY()
								);
							
							i+=3;
							}
						else
							{
							gp.lineTo(pts.get(i).getX(),pts.get(i).getY());
							i++;
							}
						}
					g.draw(gp);
					
					p0 += ( 1 + rand.nextInt(dx)) ;
					}
				}

		}
		}
	
	private interface PointPlotter
		{
		public void paint(Graphics2D g,double x, double y);
		}
	
	private abstract static class AbstractDot extends Gribouille
		{
		protected MinMax alpha=new MinMax(0.5,1.0);
		protected MinMax radius=new MinMax(0.5,50.0);
		protected double proba = 0.001;
		
		protected Point2D randomPoint() {
			return new Point2D.Double(
				this.rand.nextInt(this.imgDimension.width),	
				this.rand.nextInt(this.imgDimension.height)	
				);
			}
		
		@Override
		protected void fillOptions(Options options) {
			options.addOption(Option.builder("alpha").longOpt("alpha").hasArg(true).desc("min/max alpha").build());
			options.addOption(Option.builder("radius").longOpt("radius").hasArg(true).desc("min/max radius").build());
			options.addOption(Option.builder("p").longOpt("proba").hasArg(true).desc("Propability").build());
			super.fillOptions(options);
			}
		@Override
		protected Status decodeOptions(CommandLine cmd) {
			if(cmd.hasOption("alpha")) {
				this.alpha.parse(cmd.getOptionValue("alpha"));
			}
			if(cmd.hasOption("radius")) {
				this.radius.parse(cmd.getOptionValue("radius"));
			}
			if(cmd.hasOption("p")) {
				this.proba =Double.parseDouble(cmd.getOptionValue("p"));
			}
			
			return super.decodeOptions(cmd);
			}
		@Override
		protected void paint(final Graphics2D g) {
			final PointPlotter plotter = getPointPlotter();
			long occurences = (long)(((imgDimension.width)*(imgDimension.height))*this.proba);
			while(occurences>0) {
				occurences--;
		  		final Point2D p = randomPoint();
				if(p==null) continue;
				plotter.paint(g, p.getX(), p.getY());
				}
			}
		protected abstract PointPlotter getPointPlotter();
		}
	
	
	
	
	private static class Kirby01 extends Gribouille
		{
		private MinMax alpha=new MinMax(0.5,1.0);
		private MinMax radius=new MinMax(0.5,50.0);
		private double proba = 0.001;
		@Override
		protected void fillOptions(Options options) {
			options.addOption(Option.builder("alpha").longOpt("alpha").hasArg(true).desc("min/max alpha").build());
			options.addOption(Option.builder("radius").longOpt("radius").hasArg(true).desc("min/max radius").build());
			options.addOption(Option.builder("p").longOpt("proba").hasArg(true).desc("Propability").build());
			super.fillOptions(options);
			}
		@Override
		protected Status decodeOptions(CommandLine cmd) {
			if(cmd.hasOption("alpha")) {
				this.alpha.parse(cmd.getOptionValue("alpha"));
			}
			if(cmd.hasOption("radius")) {
				this.radius.parse(cmd.getOptionValue("radius"));
			}
			if(cmd.hasOption("p")) {
				this.proba =Double.parseDouble(cmd.getOptionValue("p"));
			}
			
			return super.decodeOptions(cmd);
			}
		
		@Override
		protected void paint(final Graphics2D g) {
			long occurences = (long)(((imgDimension.width)*(imgDimension.height))*this.proba);
			while(occurences>0) {
		  		double cy= rand.nextInt( this.image.getHeight() ) ;
				double cx ;
				double r;
				double a;
				
					cx = rand.nextInt( this.image.getWidth() ) ;
					r = this.radius.rnd(this.rand);
					a = this.alpha.rnd(this.rand);
					
				
				Color c = this.black(a);
				g.setColor(c);
				g.fill(new java.awt.geom.Ellipse2D.Double((cx-r), (cy-r),(r*2),(r*2)));
				
				occurences--;
				}
			}
		
		}
	
	private abstract class  PointPlotter01 implements PointPlotter
		{
		public PointPlotter01() {
			
			}
		public abstract double getRadius();
		public abstract Color getColor();
		
		@Override
		public void paint(Graphics2D g, double cx, double cy) {
			final double r = this.getRadius();
			g.setColor(getColor());
			g.fill(new java.awt.geom.Ellipse2D.Double((cx-r), (cy-r),(r*2),(r*2)));

			}
		}
	
	/**
	 * Dot01
	 */
	private static class Dot01 extends AbstractDot {
		@Override
		protected PointPlotter getPointPlotter() {
			return new PointPlotter01() {
				@Override
				public double getRadius() {
					return Dot01.this.radius.rnd(Dot01.this.rand);
					}
				@Override
				public java.awt.Color getColor() {
					final double a = Dot01.this.alpha.rnd(Dot01.this.rand);
					return Dot01.this.black(a);
					}
				};
			}
		}
	
	/**
	 * 
	 */
	private static class Engine01 extends Gribouille {
	private GrayImageMap grayMap=null;

	@Override
	protected void fillOptions(Options options) {
	options.addOption(Option.builder("gm").longOpt("graymap").hasArg(true).desc("Gray map image file. Used for probability").build());
		super.fillOptions(options);
		}
	@Override
	protected Status decodeOptions(final CommandLine cmd) {
		if(!cmd.hasOption("gm")) {
			LOG.severe("gray map missing");
			return Status.EXIT_ERROR;
		} else {
			this.grayMap = loadGrayMap(new File(cmd.getOptionValue("gm")));
		}
		return super.decodeOptions(cmd);
		}
	@Override
	protected void paint(final Graphics2D g)
		{
		// TODO Auto-generated method stub
		
		}
	}
	
	@Override
	protected void usage()
		{
		super.usage();
		printAvailableTools(System.out);
		}
	
	
	private static void printAvailableTools(PrintStream out) {
	out.println("Available engines: ");
	out.println("  eng01  loads a graymap, plot cross-point.");
	out.println("  dot01  random points.");
	}
	
	public static void main(String[] args) {
		Gribouille app = null;
		if(args.length==0) {
			System.err.println("Illegal number of arguments");
			System.exit(-1);
		} else if(args[0].equals("kirby01")) {
			app = new Kirby01();
		} else if(args[0].equals("x01")) {
			app = new X01();
		}else if(args[0].equals("x02")) {
			app = new X02();
		} else if(args[0].equals("h1")) {
			app = new Hatching01();
		}
		 else if(args[0].equals("eng01")) {
			app = new Engine01();
		}
		 else if(args[0].equals("dot01")) {
			app = new Dot01();
		}
		if(app!=null) {
			app.instanceMainWithExit( Arrays.copyOfRange(args, 1, args.length));
			}
		else
			{
			System.err.println("Illegal sub program");
			printAvailableTools(System.err);
			System.exit(-1);
			}
		}
	
}
