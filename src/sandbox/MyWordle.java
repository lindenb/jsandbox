package sandbox;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.io.InputStreamReader;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import com.beust.jcommander.Parameter;


public class MyWordle extends Launcher
	{
	private final static Logger LOG=Logger.builder(MyWordle.class).build();

	public static class Word
		{
		private String fontFamily=null;
		private int weight;
		private String text="";
		private Shape shape;
		private Rectangle2D bounds;
		private Color fill=null;
		private Color stroke=null;
		private float lineHeight=1.0f;
		private String title=null;
		private String url=null;
		private String format=null;
		
		public Word(String text,int weight)
			{
			this.text=text;
			this.weight=weight;
			if(this.weight<=0) throw new IllegalArgumentException("bad weight "+weight);
			}
		
		public String getText()
			{
			return text;
			}
		
		public int getWeight()
			{
			return weight;
			}
		
		public void setFontFamily(String fontFamily)
			{
			this.fontFamily = fontFamily;
			}
		
		public String getFontFamily()
			{
			return fontFamily;
			}
		
		public void setFill(Color fill)
			{
			this.fill = fill;
			}
		
		public Color getFill()
			{
			return fill;
			}
		
		public void setStroke(Color stroke)
			{
			this.stroke = stroke;
			}
		
		public Color getStroke()
			{
			return stroke;
			}
		
		public float getLineHeight()
			{
			return lineHeight;
			}
		
		public void setLineHeight(float lineHeight)
			{
			this.lineHeight = lineHeight;
			}
		
		public void setTitle(String title)
			{
			this.title = title;
			}
		
		public String getTitle()
			{
			return title;
			}
		
		public void setUrl(String url)
			{
			this.url = url;
			}
		
		public String getUrl()
			{
			return url;
			}
		
		}
	
	@Parameter(names={"-o","-out","--out"},description="output file")
	private File fileout=null;
	@Parameter(names={"--font-family"},description="font")
	private String fontFamily="Dialog";

	private int biggestSize=72;
	private int smallestSize=10;
	private List<Word> words=new ArrayList<Word>();
	private Random rand=new Random();
	private Rectangle2D imageSize=null;
	private Color fill=Color.YELLOW;
	private Color stroke=Color.RED;
	private double dRadius=10.0;
	private int dDeg=10;
	private boolean useArea=false;
	private int doSortType=-1;
	@Parameter(names={"-w"},description="output width")
	private Integer outputWidth=null;
	@Parameter(names={"-f"},description="allow rotate")
	private boolean allowRotate=false;
	
	public MyWordle()
		{
		}
	
	
	public void doLayout()
		{
		this.imageSize=new Rectangle2D.Double(0, 0, 0, 0);
		if(this.words.isEmpty()) return;
		/** sort from biggest to lowest */
		
		switch(doSortType)
			{
			case 1:
				{
				Collections.sort(this.words,new Comparator<Word>()
		            {
		            @Override
		            public int compare(Word w1, Word w2)
		                {
		                return (int)w2.getWeight()-(int)w1.getWeight();
		                }
		            });
				break;
				}
			case 2:
				{
				Collections.sort(this.words,new Comparator<Word>()
		            {
		            @Override
		            public int compare(Word w1, Word w2)
		                {
		                return (int)w1.getWeight()-(int)w2.getWeight();
		                }
		            });
				break;
				}
			case 3:
				{
				Collections.sort(this.words,new Comparator<Word>()
		            {
		            @Override
		            public int compare(Word w1, Word w2)
		                {
		                return w1.getText().compareToIgnoreCase(w2.getText());
		                }
		            });
				break;
				}
			default:
				{
				Collections.shuffle(this.words,this.rand);
				break;
				}
			}
		Word first=this.words.get(0);
		double high = -Double.MAX_VALUE;
		double low = Double.MAX_VALUE;
		for(Word w:this.words)
			{
			high= Math.max(high, w.getWeight());
			low= Math.min(low, w.getWeight());
			}
		
		
		/* create small image */
		BufferedImage img=new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
	    /* get graphics from this image */
		Graphics2D g=Graphics2D.class.cast(img.getGraphics());
	    FontRenderContext frc = g.getFontRenderContext();
	    
	    
		for(Word w:this.words)
			{
			String ff=w.getFontFamily();
			if(ff==null) ff=this.fontFamily;
			int fontSize=(int)(((w.getWeight()-low)/(high-low))*(this.biggestSize-this.smallestSize))+this.smallestSize;
			Font font=new Font(ff,Font.BOLD,fontSize);
			System.err.println("fontsize:"+fontSize);
			TextLayout textLayout=new TextLayout(w.getText(), font, frc);
			Shape shape=textLayout.getOutline(null);
			if(this.allowRotate && this.rand.nextBoolean())
				{
				AffineTransform rotate=AffineTransform.getRotateInstance(
						Math.PI/2.0
						);
				shape=rotate.createTransformedShape(shape);
				}
			Rectangle2D bounds= shape.getBounds2D();
			AffineTransform centerTr=AffineTransform.getTranslateInstance(-bounds.getCenterX(),-bounds.getCenterY());
			w.shape= centerTr.createTransformedShape(shape);
			w.bounds=w.shape.getBounds2D();
			}
		g.dispose();
		
		//first point
		Point2D.Double center=new Point2D.Double(0,0);
		
		for(int i=1;i< this.words.size();++i)
			{
			Word current=this.words.get(i);
			
			//calculate current center
			center.x=0;
			center.y=0;
			double totalWeight=0.0;
			for(int prev=0;prev< i;++prev)
				{
				Word wPrev=this.words.get(prev);
				center.x+= (wPrev.bounds.getCenterX())*wPrev.getWeight();
				center.y+= (wPrev.bounds.getCenterY())*wPrev.getWeight();
				totalWeight+=wPrev.getWeight();
				}
			center.x/=(totalWeight);
			center.y/=(totalWeight);
			
			//TODO
			Shape shaveH=current.shape;
			Rectangle2D bounds=current.bounds;
			
			
			
			boolean done=false;
			double radius=0.5*Math.min(
					first.bounds.getWidth(),
					first.bounds.getHeight());
			
			while(!done)
				{
				System.err.println(""+i+"/"+words.size()+" rad:"+radius);
				int startDeg=rand.nextInt(360);
				//loop over spiral
				int prev_x=-1;
				int prev_y=-1;
				for(int deg=startDeg;deg<startDeg+360;deg+=dDeg)
					{
					double rad=((double)deg/Math.PI)*180.0;
					int cx=(int)(center.x+radius*Math.cos(rad));
					int cy=(int)(center.y+radius*Math.sin(rad));
					if(prev_x==cx && prev_y==cy) continue;
					prev_x=cx;
					prev_y=cy;
					
					AffineTransform moveTo=AffineTransform.getTranslateInstance(cx,cy);
					Shape candidate=moveTo.createTransformedShape(current.shape);
					Area area1=null;
					Rectangle2D bound1=null;
					if(useArea)
						{
						area1=new Area(candidate);
						}
					else
						{
						bound1=new Rectangle2D.Double(
								current.bounds.getX()+cx,
								current.bounds.getY()+cy,
								current.bounds.getWidth(),
								current.bounds.getHeight()
								);
						}
					//any collision ?
					int prev=0;
					for(prev=0;prev< i;++prev)
						{
						if(useArea)
							{
							Area area2=new Area(this.words.get(prev).shape);
							area2.intersect(area1);
							if(!area2.isEmpty()) break;
							}
						else
							{
							if(bound1.intersects(this.words.get(prev).bounds))
								{
								break;
								}
							}
						}
					//no collision: we're done
					if(prev==i)
						{
						current.shape=candidate;
						current.bounds=candidate.getBounds2D();
						done=true;
						break;
						}
					}
				radius+=this.dRadius;
				}
			}
		
		double minx=Integer.MAX_VALUE;
		double miny=Integer.MAX_VALUE;
		double maxx=-Integer.MAX_VALUE;
		double maxy=-Integer.MAX_VALUE;
		for(Word w:words)
			{
			minx=Math.min(minx, w.bounds.getMinX()+1);
			miny=Math.min(miny, w.bounds.getMinY()+1);
			maxx=Math.max(maxx, w.bounds.getMaxX()+1);
			maxy=Math.max(maxy, w.bounds.getMaxY()+1);
			}
		AffineTransform shiftTr=AffineTransform.getTranslateInstance(-minx, -miny);
		for(Word w:words)
			{
			w.shape=shiftTr.createTransformedShape(w.shape);
			w.bounds=w.shape.getBounds2D();
			}
		this.imageSize=new Rectangle2D.Double(0,0,maxx-minx,maxy-miny);
		}
	
	
	private void random()
		{
		for(int i=0;i< 500;++i)
			{
			int n=1+rand.nextInt(20);
			String text="";
			while(text.length()<n)
				{
				text+=(char)(('A')+rand.nextInt(25));
				}
			Word w=new Word(text,rand.nextInt(100050)+10);
			w.setUrl("http://www.google.com");
			Color c=new Color(rand.nextInt(100),rand.nextInt(100),rand.nextInt(100));
			w.setFill(c);
			c=new Color(rand.nextInt(100),rand.nextInt(100),rand.nextInt(100));
			w.setStroke(c);
			w.setTitle(""+i);
			w.setLineHeight(1+2*rand.nextFloat());
			w.setFontFamily(rand.nextBoolean()?"Helvetica":"Courier");
			words.add(w);
			
			}
		}
	
	public void add(Word word)
		{
		this.words.add(word);
		}
	
	public void saveAsPNG(File file)
		throws IOException
		{
		AffineTransform scale=new AffineTransform();
		Dimension dim=new Dimension(
			(int)this.imageSize.getWidth(),
			(int)this.imageSize.getHeight()	
			);
		
		if(this.outputWidth!=null)
			{
			double ratio=this.outputWidth/dim.getWidth();
			dim.width=this.outputWidth;
			dim.height=(int)(dim.getHeight()*ratio);
			scale=AffineTransform.getScaleInstance(ratio, ratio);
			}
		
		BufferedImage img=new BufferedImage(
			dim.width,
			dim.height,
			BufferedImage.TYPE_INT_ARGB
			);
		
		Graphics2D g=(Graphics2D)img.getGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setTransform(scale);
		for(Word w:this.words)
			{
			Color c=w.getFill();
			if(c==null) c=this.fill;
			if(c!=null)
				{
				g.setColor(c);
				g.fill(w.shape);
				}
			
			c=w.getStroke();
			if(c==null) c=this.stroke;
			if(c!=null)
				{
				Stroke old=g.getStroke();
				g.setStroke(new BasicStroke(
						w.getLineHeight(),
						BasicStroke.CAP_BUTT,
						BasicStroke.JOIN_ROUND
						));
				g.setColor(c);
				g.draw(w.shape);
				g.setStroke(old);
				}
			}
		
		g.dispose();
		ImageIO.write(img, "png", file);
		}
	
	public void saveAsSVG(File file)
	throws IOException,XMLStreamException
		{
		final String SVG="http://www.w3.org/2000/svg";
		final String XLINK="http://www.w3.org/1999/xlink";
		XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
		FileOutputStream fout=new FileOutputStream(file);
		XMLStreamWriter w= xmlfactory.createXMLStreamWriter( fout,"UTF-8");
		w.writeStartDocument("UTF-8","1.0");
		w.writeStartElement("svg","svg",SVG);
		w.writeAttribute("xmlns", XMLConstants.XML_NS_URI, "svg", SVG);
		w.writeAttribute("xmlns", XMLConstants.XML_NS_URI, "xlink", XLINK);
		w.writeAttribute("width",String.valueOf(this.imageSize.getWidth()));
		w.writeAttribute("height",String.valueOf(this.imageSize.getHeight()));
		w.writeStartElement("svg","title",SVG);
		w.writeCharacters("made with MyWordle (c) Pierre Lindenbaum");
		w.writeEndElement();
		
		for(Word word:this.words)
			{

			if(word.getUrl()!=null)
				{
				w.writeStartElement("svg","a",SVG);
				w.writeAttribute("xlink",XLINK,"href",word.getUrl());
				}
			
			w.writeEmptyElement("svg","path",SVG);
			
			
			if(word.getTitle()!=null)
				{
				w.writeAttribute("title", word.getTitle());
				}
			
			String style="";
			Color c=word.getFill();
			if(c==null) c=this.fill;
			style+="fill:"+(c==null?"none":toRGB(c))+";";
			c=word.getStroke();
			if(c==null) c=this.stroke;
			style+="stroke:"+(c==null?"none":toRGB(c))+";";
			style+="stroke-width:"+word.getLineHeight()+";";
			w.writeAttribute("style", style);

			StringBuilder path=new StringBuilder();
			double tab[] = new double[6];
			PathIterator pathiterator = word.shape.getPathIterator(null);
			while(!pathiterator.isDone())
				{
				int currSegmentType= pathiterator.currentSegment(tab);
				switch(currSegmentType)
					{
					case PathIterator.SEG_MOVETO:
						{
						path.append( "M " + (tab[0]) + " " + (tab[1]) + " ");
						break;
						}
					case PathIterator.SEG_LINETO:
						{
						path.append( "L " + (tab[0]) + " " + (tab[1]) + " ");
						break;
						}
					case PathIterator.SEG_CLOSE:
						{
						path.append( "Z ");
						break;
						}
					case PathIterator.SEG_QUADTO:
						{
						path.append( "Q " + (tab[0]) + " " + (tab[1]));
						path.append( " "  + (tab[2]) + " " + (tab[3]));
						path.append( " ");
						break;
						}
					case PathIterator.SEG_CUBICTO:
						{
						path.append( "C " + (tab[0]) + " " + (tab[1]));
						path.append( " "  + (tab[2]) + " " + (tab[3]));
						path.append( " "  + (tab[4]) + " " + (tab[5]));
						path.append( " ");
						break;
						}
					default:
						{
						System.err.println("Cannot handled "+currSegmentType);
						break;
						}
					}
				pathiterator.next();
				}
			w.writeAttribute("d", path.toString());
			
		
			if(word.getUrl()!=null)
				{
				w.writeEndElement();
				}
			}
		
		
		w.writeEndDocument();
		w.flush();
		w.close();
		fout.flush();
		fout.close();
		}
	
	public void saveAsPostscript(File file)
	throws IOException
		{
		
		PrintWriter out= new PrintWriter(file);
		out.println("%!PS-Adobe-2.0");
		out.println("%%%BoundingBox: 0 0 "+(int)this.imageSize.getWidth()+" "+(int)this.imageSize.getHeight());
		out.println("%%EndComments");

		
		for(Word word:this.words)
			{
			
			out.print(""+word.getLineHeight()+" setlinewidth");
			
			out.print(" newpath");
			
			double mPenX=0;
			double mPenY=0;
			StringBuilder path=new StringBuilder();
			double tab[] = new double[6];
			PathIterator pathiterator = word.shape.getPathIterator(null);
			while(!pathiterator.isDone())
				{
				int currSegmentType= pathiterator.currentSegment(tab);
				for(int i=1;i< tab.length;i+=2)
					{
					tab[i]=this.imageSize.getHeight()-tab[i];
					}
				switch(currSegmentType)
					{
					case PathIterator.SEG_MOVETO:
						{
						out.print(' ');
						out.print(tab[0]);out.print(' '); out.print(tab[1]);
						out.print(" moveto");
						break;
						}
					case PathIterator.SEG_LINETO:
						{
						out.print(' ');
						out.print(tab[0]);out.print(' '); out.print(tab[1]);
						out.print(" lineto");
						mPenX=tab[0];
						mPenY=tab[1];
						break;
						}
					case PathIterator.SEG_CLOSE:
						{
						out.print(" closepath");
						break;
						}
					case PathIterator.SEG_QUADTO:
						{
						double lastX = mPenX;
						double lastY = mPenY;
						double c1x = lastX + (tab[0] - lastX) * 2 / 3;
						double c1y = lastY + (tab[1] - lastY) * 2 / 3;
						double c2x = tab[2] - (tab[2] - tab[0]) * 2/ 3;
						double c2y = tab[3] - (tab[3] - tab[1]) * 2/ 3;
						out.print(' ');
						out.print(c1x);out.print(' '); out.print(c1y);
						out.print(' ');
						out.print(c2x);out.print(' '); out.print(c2y);
						out.print(' ');
						out.print(tab[2]);out.print(' '); out.print(tab[3]);
						out.print(" curveto");
						mPenX = tab[2];
						mPenY = tab[3];
						break;
						}
					case PathIterator.SEG_CUBICTO:
						{
						out.print(' ');
						out.print(tab[0]);out.print(' '); out.print(tab[1]);
						out.print(' ');
						out.print(tab[2]);out.print(' '); out.print(tab[3]);
						out.print(' ');
						out.print(tab[4]);out.print(' '); out.print(tab[5]);
						out.print(" curveto ");
						mPenX = tab[4];
						mPenY = tab[5];
						break;
						}
					default:
						{
						System.err.println("Cannot handled "+currSegmentType);
						break;
						}
					}
				pathiterator.next();
				}
			
			if(word.getFill()!=null)
				{
				Color c=word.getStroke();
				out.print(c.getRed()/255.0);
				out.print(' ');
				out.print(c.getGreen()/255.0);
				out.print(' ');
				out.print(c.getBlue()/255.0);
				out.print(' ');
				out.print(" setrgbcolor fill");
				}
			
			if(word.getStroke()!=null)
				{
				Color c=word.getStroke();
				out.print(c.getRed()/255.0);
				out.print(' ');
				out.print(c.getGreen()/255.0);
				out.print(' ');
				out.print(c.getBlue()/255.0);
				out.print(' ');
				out.print(" setrgbcolor stroke");
				}
			}
		
		out.println(" showpage");
		out.flush();
		out.close();
		}
	



	
	
	private static String toRGB(Color c)
		{
		return "rgb("+c.getRed()+","+c.getGreen()+","+c.getBlue()+")";
		}
	
	
	public void setAllowRotate(boolean allowRotate)
		{
		this.allowRotate = allowRotate;
		}
	
	public void setBiggestSize(int biggestSize)
		{
		this.biggestSize = biggestSize;
		}
	
	public void setSmallestSize(int smallestSize)
		{
		this.smallestSize = smallestSize;
		}
	
	public void setSortType(int doSortType)
		{
		this.doSortType = doSortType;
		}
	
	public void setUseArea(boolean useArea)
		{
		this.useArea = useArea;
		}
			
	
	void read(BufferedReader r)
		{
		
		}
	
	@Override
	public int doWork(final List<String> args) {
		if(this.fileout==null) {
			LOG.error("file out missing");
			return -1;
		}
		try
			{
			MyWordle app=new MyWordle();
			String format=null;
			File fileOut=null;
			int optind=0;
			
			
			
			
	      	if(args.isEmpty())
	                {
	                this.read(new BufferedReader(new InputStreamReader(System.in)));
	                }
	        else
	                {
	               for(String filename:args)
	                        {
	                      	java.io.BufferedReader r= new BufferedReader(new FileReader(filename));
	                        this.read(r);
	                        r.close();
	                        }
	                }
			
			
			this.doLayout();
			
			if(fileOut.getName().toLowerCase().endsWith(".svg") || (format!=null && format.equalsIgnoreCase("svg")))
				{
				this.saveAsSVG(fileOut);
				}
			else if(fileOut.getName().toLowerCase().endsWith(".png") || (format!=null && format.equalsIgnoreCase("png")))
				{
				this.saveAsPNG(fileOut);
				}
			else if(fileOut.getName().toLowerCase().endsWith("ps") || (format!=null && format.equalsIgnoreCase("ps")))
				{
				this.saveAsPostscript(fileOut);
				}
			else
				{
				LOG.error("undefined format");
				return -1;
				}
			return 0;
			} 
		catch(Throwable err)
			{
			LOG.error(err);
			return -1;
			}
		}
	}
