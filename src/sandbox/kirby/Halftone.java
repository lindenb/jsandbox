package sandbox.kirby;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.DoubleSupplier;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.Parameter;

import sandbox.Logger;
import sandbox.SimpleGraphics;

public class Halftone extends AbstractKirbyLauncher {
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
			catch(final NumberFormatException err) {
				throw new IllegalArgumentException(err);
				}
			}
	}
	
	enum MarkShape {
		circle,square,random
	}
	
	
	@Parameter(names= {"-sx1","--sx1",},description="scale pattern dimension by factor 'sx1'")
	private double scaleFactor1 = 1.0;
	@Parameter(names= {"-sx2","--sx2",},description="scale mark size by factor 'sx2'",converter=ScaleDoubleSupplier.class)
	private DoubleSupplier scaleFactor2 = ()->1.0;
	@Parameter(names= {"-a","--alpha",},description="alpha ",converter=ScaleDoubleSupplier.class)
	private DoubleSupplier alphaSupplier = ()->1.0;
	@Parameter(names= {"-n","--pattern",},description="pattern name",required=true)
	private String patName = "";
	@Parameter(names= {"-l","--list",},description="list patterns and exit",help=true)
	private boolean list_patterns = false;
	@Parameter(names= {"-m","--mark",},description="markShape")
	private MarkShape markhape = MarkShape.circle;
	@DynamicParameter(names = "-D", description = "Dynamic parameters go here",hidden=true)
	private Map<String, String> dynParams = new HashMap<>();
	
	private interface Pattern {
		String getName();
		String getDescription();
		void paint(SimpleGraphics ctx);
		}
	
	private abstract class AbstractPattern implements Pattern {
		final String name;
		final String desc;
		AbstractPattern(final String name,final String desc){
			this.name = name;
			this.desc=desc;
			}
		@Override
		public String getName() {
			return this.name;
			}
		@Override
		public String getDescription() {
			return desc==null?this.name:this.desc;
			}

		}
	
	private class RandomMatrix extends AbstractPattern {
		RandomMatrix(final String name,final String desc)  {
			super(name,desc);
			}
		@Override
		public void paint(SimpleGraphics ctx) {
			double n = (long)ctx.getWidth()*(long)ctx.getHeight();
			n = n*(1.0/Double.valueOf(dynParams.getOrDefault("density",String.valueOf(1000.0))));
			while(n>0) {
				n--;
				double r = 1.0*scaleFactor2.getAsDouble();
				if(r<=0) continue;
				double cx = random.nextInt(ctx.getWidth());
				double cy = random.nextInt(ctx.getHeight());
				mark(ctx,cx,cy,r);
				}
			}
		}
	
	private class PatternMatrix extends AbstractPattern {
		final float array[][];
		PatternMatrix(final String name,final String desc,float array[][])  {
			super(name,desc);
			this.array=array;
			}
		PatternMatrix(final String name,final String desc,String...array)  {
			super(name,desc);
			this.array=new float[array.length][];
			for(int y=0;y< array.length;y++)
				{
				this.array[y]=new float[array[y].length()];
				for(int x=0;x< array[y].length();++x)
					{
					this.array[y][x]=Character.isWhitespace(array[y].charAt(x)) || array[y].charAt(x)=='.'?0f:1f;
					}
				}
			}
		public int getWidth() {
			return array[0].length;
			}
		public int getHeight() {
			return array.length;
			}
		
		@Override
		public void paint(final SimpleGraphics ctx) {
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
		
		patterns.add(new RandomMatrix("rnd0",null));
		
		
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
		
		return paint();
		}
	
	@Override
	protected void paint(SimpleGraphics g) {
		final Optional<Pattern> pat = this.patterns.stream().filter(P->this.patName.equals(P.getName())).findFirst();
		if(!pat.isPresent()) throw new IllegalStateException();
		pat.get().paint(g);
		}
	
	public static void main(final String[] args) {
		new Halftone().instanceMainWithExit(args);
	}
}
