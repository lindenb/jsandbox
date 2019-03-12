package sandbox;

import java.awt.Color;
import java.awt.Dimension;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.function.DoubleSupplier;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;

import sandbox.DimensionConverter;

public class Halftone extends Launcher {
	private static final Logger LOG = Logger.builder(Halftone.class).build();
	
	private static class ScaleDoubleSupplier implements IStringConverter<DoubleSupplier>{
		@Override
		public DoubleSupplier convert(String s) {
			if(s.equals("random") || s.equals("random()")) {
				return ()->Math.random();
			}
			
			if(s.startsWith("random(") && s.endsWith(")")) {
				s = s.substring(7,s.length()-1);
				try {
					int slash = s.indexOf(",");
					if(slash!=-1) {
						final double d1= Double.parseDouble(s.substring(0,slash));
						final double d2= Double.parseDouble(s.substring(slash+1));
						return ()->d1+Math.random()*(d2-d1);
						}
					else
						{
						final double d1= Double.parseDouble(s);
						return ()-> Math.random()*d1;
						}
					}
				catch(NumberFormatException err) {
					throw new IllegalArgumentException(err);
					}
				
				}
			try {
				final double d= Double.parseDouble(s);
				return ()->d;
				}
			catch(NumberFormatException err) {
				throw new IllegalArgumentException(err);
				}
			}
	}
	
	enum MarkShape {
		circle,square,random
	}
	
	@Parameter(names= {"-d","--size","--dim","--dimension"},converter=DimensionConverter.StringConverter.class,description=DimensionConverter.OPT_DESC,required=true)
	private Dimension dimIn = null;
	@Parameter(names= {"-sx1","--sx1",},description="scale pattern dimension by factor 'sx1'")
	private double scaleFactor1 = 1.0;
	@Parameter(names= {"-sx2","--sx2",},description="scale mark size by factor 'sx2'",converter=ScaleDoubleSupplier.class)
	private DoubleSupplier scaleFactor2 = ()->1.0;
	@Parameter(names= {"-a","--alpha",},description="alpha ",converter=ScaleDoubleSupplier.class)
	private DoubleSupplier alphaSupplier = ()->1.0;
	@Parameter(names= {"-o","--output",},description="output")
	private Path output = null;
	@Parameter(names= {"-n","--pattern",},description="pattern name",required=true)
	private String patName = "";
	@Parameter(names= {"-l","--list",},description="list patterns and exit",help=true)
	private boolean list_patterns = false;
	@Parameter(names= {"-b","--background",},description="trace white background")
	private boolean trace_background = false;
	@Parameter(names= {"-m","--mark",},description="markShape")
	private MarkShape markhape = MarkShape.circle;

	
	private interface Pattern {
		int getWidth();
		int getHeight();
		String getName();
		String getDescription();
		void paint(SimpleGraphics ctx);
		}
	private class PatternMatrix implements Pattern {
		final float array[][];
		final String name;
		final String desc;
		PatternMatrix(final String name,final String desc,float array[][])  {
			this.array=array;
			this.name = name;
			this.desc=desc;
			}
		PatternMatrix(final String name,final String desc,String...array)  {
			this.array=new float[array.length][];
			for(int y=0;y< array.length;y++)
				{
				this.array[y]=new float[array[y].length()];
				for(int x=0;x< array[y].length();++x)
					{
					this.array[y][x]=Character.isWhitespace(array[y].charAt(x)) || array[y].charAt(x)=='.'?0f:1f;
					}
				}
			this.name = name;
			this.desc=desc;
			}
		@Override
		public String getName() {
			return this.name;
			}
		@Override
		public String getDescription() {
			return desc;
			}
		@Override
		public int getWidth() {
			return array[0].length;
			}
		@Override
		public int getHeight() {
			return array.length;
			}
		
		private void mark(SimpleGraphics ctx,double cx,double cy,double cr)
			{
			float a = (float)alphaSupplier.getAsDouble();
			if(a<0f) a=0f;
			if(a>1f) a=1f;
			
			ctx.setAlpha(a);
			
			MarkShape x = Halftone.this.markhape;
			if(x.equals(MarkShape.random))
				{
				switch(random.nextInt(2))
					{
					case 0: x= MarkShape.circle;break;
					default: x= MarkShape.square;break;
					}
				}
			
			switch(x)
				{
				case circle: ctx.circle(cx, cy, cr); break;
				case square: ctx.rect(cx-cr, cy-cr, cr*2,cr*2); break;
				default:break;
				}
			
			}
		
		@Override
		public void paint(SimpleGraphics ctx) {
			double y =0;
			while(y<ctx.getHeight()) {
				double x =0;
				while(x<ctx.getWidth()) {
					for(int j=0;j< this.getHeight();j++)
						{
						for(int i=0;i< this.getWidth();i++)
							{
							double r = this.array[j][i]*scaleFactor2.getAsDouble();
							if(r<=0) continue;
							double x1 = x + (i+0.5)*scaleFactor1; 
							double x2 = y + (j+0.5)*scaleFactor1;
							mark(ctx,x1,x2,r);
							}
						}
					x+= this.getWidth()*scaleFactor1;
					}
				y+= this.getHeight()*scaleFactor1;
				}
			}
		}
	
	
	private final List<Pattern> patterns = new ArrayList<>();
	private final Random random = new Random(System.currentTimeMillis());
	
	@Override
	public int doWork(final List<String> args) {
		// fill patterns
		
		PatternMatrix m = new PatternMatrix("g1", "g1",
				"X       ",
				"        ",
				"        ",
				"        ",
				"        ",
				"        ",
				"        ",
				"        "
				);
		patterns.add(m);
		
		m = new PatternMatrix("g50", "g50",
				"X X X X ",
				" X X X X",
				"X X X X ",
				" X X X X",
				"X X X X ",
				" X X X X",
				"X X X X ",
				" X X X X"
				);
		patterns.add(m);
		
		if(list_patterns) {
			patterns.forEach(P->{
				System.out.print(P.getName());
				System.out.print("\t");
				System.out.print(P.getDescription());
				System.out.println();
			});
			return 0;
		}
		
		final Optional<Pattern> pat = this.patterns.stream().filter(P->this.patName.equals(P.getName())).findFirst();
		
		if(!pat.isPresent()) {
			LOG.error("Cannot find pattern name "+this.patName);
			return -1;
		}
		
		SimpleGraphics g = null;
		try {
			g = SimpleGraphics.openPath(this.output,this.dimIn.width,this.dimIn.height);
			if(trace_background) {
				g.setFill(Color.WHITE);
				g.rect(0, 0, g.getWidth(), g.getHeight());
				}
			g.setFill(Color.BLACK);
			pat.get().paint(g);
			g.close();
			g=null;
			return 0;
			}
		catch(final Throwable err) {
			LOG.error(err);
			return -1;
			}
		finally
			{
			IOUtils.close(g);
			}
		}
	
	public static void main(String[] args) {
		new Halftone().instanceMainWithExit(args);
	}
}
