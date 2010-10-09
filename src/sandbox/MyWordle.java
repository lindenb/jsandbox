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

public class MyWordle
	{
	
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
	private int biggestSize=72;
	private int smallestSize=10;
	private List<Word> words=new ArrayList<Word>();
	private String fontFamily="Dialog";
	private Random rand=new Random();
	private Rectangle2D imageSize=null;
	private Color fill=Color.YELLOW;
	private Color stroke=Color.RED;
	private double dRadius=10.0;
	private int dDeg=10;
	private boolean useArea=false;
	private int doSortType=-1;
	private Integer outputWidth=null;
	private boolean allowRotate=false;
	
	private MyWordle()
		{
		}
	
	
	private void layout()
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
			default:break;
			}
		Word first=this.words.get(0);
		double high = first.getWeight();
		double low = this.words.get( this.words.size()-1).getWeight();
		
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
			System.err.println(fontSize);
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
	
	private static String toRGB(Color c)
		{
		return "rgb("+c.getRed()+","+c.getGreen()+","+c.getBlue()+")";
		}
	
	
	private void read(BufferedReader in)throws IOException
		{
	
		}
	
	public static void main(String[] args)
		{
		try
			{
			MyWordle app=new MyWordle();
			String format=null;
			File fileOut=null;
			int optind=0;
			
			format="png";
			fileOut=new File("/home/pierre/jeter.png");
			
			while(optind< args.length)
				{
				if(args[optind].equals("-h") ||
				   args[optind].equals("-help") ||
				   args[optind].equals("--help"))
					{
					System.err.println("Options:");
					System.err.println(" -h help; This screen.");
					return;
					}
				else if(args[optind].equals("-font-family"))
					{
					app.fontFamily=args[++optind];
					}
				else if(args[optind].equals("-o"))
					{
					fileOut=new File(args[++optind]);
					}
				else if(args[optind].equals("-f"))
					{
					format=args[++optind];
					}
				else if(args[optind].equals("-w"))
					{
					app.outputWidth=Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-r"))
					{
					app.allowRotate=true;
					}
				else if(args[optind].equals("--"))
					{
					optind++;
					break;
					}
				else if(args[optind].startsWith("-"))
					{
					System.err.println("Unknown option "+args[optind]);
					return;
					}
				else 
					{
					break;
					}
				++optind;
				}
			
			if(fileOut==null)
				{
				System.err.println("file missing");
				return;
				}
			
			if(format==null)
				{
				System.err.println("format missing");
				return;
				}
			
	      	if(optind==args.length)
	                {
	                app.read(new BufferedReader(new InputStreamReader(System.in)));
	                }
	        else
	                {
	                while(optind< args.length)
	                        {
	                        String filename=args[optind++];
	                      	java.io.BufferedReader r= new BufferedReader(new FileReader(filename));
	                        app.read(r);
	                        r.close();
	                        }
	                }
			
			app.random();
			app.layout();
			app.outputWidth=500;
			app.doSortType=3;
			
			if(format.equalsIgnoreCase("svg"))
				{
				app.saveAsSVG(fileOut);
				}
			else if(format.equalsIgnoreCase("png"))
				{
				app.saveAsPNG(fileOut);
				}
			else
				{
				System.err.println("unknown format");
				}
			} 
		catch(Throwable err)
			{
			err.printStackTrace();
			}
		}
	}
