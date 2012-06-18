package sandbox;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;

import java.util.Collections;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class TreeMapMaker
	{
	private final String SVG="http://www.w3.org/2000/svg";
	private final String XLINK="http://www.w3.org/1999/xlink";
	private enum Orientation { VERTICAL, HORIZONTAL};
    private enum Direction { ASCENDING, DESCENDING}
    private static final  String NODE_NAME="node";
    private Dimension viewRect=new Dimension(500,500);
    private Graphics2D gc;
    private static int MARGIN=10;
   
    
	static abstract class Frame
		implements Comparable<Frame>
		{
		Element node;
		Frame parent=null;
		Rectangle2D.Double bounds=new Rectangle2D.Double();
		Frame(Element node)
			{
			this.node=node;
			}
		protected String getAttribute(String name,String def)
			{
			Attr att=node.getAttributeNode(name);
			if(att==null) return def;
			String s=att.getValue().trim();
			return s.isEmpty()?null:s;
			}
		
		protected String getStyle(String sel,String def)
			{
			String val=null;
			String style=getAttribute("style",null);
			if(style!=null)
				{
				for(String s:style.split("[;]"))
					{
					int i=s.indexOf(':');
					if(i==-1) continue;
					if(!s.substring(0, i).equals(sel)) continue;
					val=s.substring(i+1);
					break;
					}
				}
			if(val==null)
				{
				return parent==null?def:parent.getStyle(sel, def);
				}
			return val;
			}
		
		public String getLabel()
			{
			return getAttribute("label",null);
			}
		public String getDescription()
			{
			return getAttribute("description",getLabel());
			}
		public String getUrl()
			{
			return getAttribute("url",null);
			}
		
		@Override
		public int compareTo(Frame o)
			{
			double d1= this.getWeight();
            double d2= o.getWeight();
            return (d2==d1 ? 0 : (d1<d2?1:-1));//inverse
			}
		
		private String getFontFamily()
			{
			return getStyle("font-family","Courier");
			}
		Rectangle2D.Double getInnerRect()
			{
			Rectangle2D.Double r=new Rectangle2D.Double();
			r.setRect(this.bounds);
			if(parent==null) return r;
			if(r.width>4*MARGIN)
				{
				r.x+=MARGIN;
				r.width-=(MARGIN*2);
				}
			else
				{
				double L=r.getWidth()*0.05;
				r.x+=L;
				r.width-=(L*2.0);
				}
			if(r.height>4*MARGIN)
				{
				r.y+=MARGIN;
				r.height-=(MARGIN*2);
				}
			else
				{
				double L=r.getHeight()*0.05;
				r.y+=L;
				r.height-=(L*2.0);
				}
			
			return r;
			}
		int getDepth()
			{
			return parent==null?0:1+parent.getDepth();
			}
		public abstract double getWeight();
		public abstract boolean isLeaf();
		
		}
	
	static class Leaf extends Frame
		{
		Leaf(Element root) { super(root);}
		@Override
		public double getWeight()
			{
			String s=getAttribute("weight", "1.0");
			return Double.parseDouble(s);
			}
		@Override
		public boolean isLeaf()
			{
			return true;
			}
		}
	
	static class Branch extends Frame
		{
		List<Frame> children=new ArrayList<Frame>();
		 Branch(Element root) { super(root);}
		@Override
		public double getWeight()
			{
			double w=0.0;
			for(Frame f:children)
				{
				w+=f.getWeight();
				}
			return w;
			}
		@Override
		public boolean isLeaf()
			{
			return false;
			}
		
		}
	
	 
	private double sum(List<Frame> items, int start, int end)
		    {
		    double sum=0;
		    while(start<=end)//yes <=
	        	{
	        	sum+=items.get(start++).getWeight();
	        	}
		    return sum;
		    }
	
	private void layoutBest(List<Frame> items, int start, int end, final Rectangle2D.Double bounds)
	    {
	    sliceLayout(
	    		items,start,end,bounds,
	            bounds.width>bounds.height ? Orientation.HORIZONTAL : Orientation.VERTICAL,
	            Direction.ASCENDING);
	    }

	/*
	private void layoutBest(List<Frame> items, int start, int end, Rectangle2D.Double bounds, Direction order)
	    {
	    sliceLayout(items,start,end,bounds,
	                    bounds.width>bounds.height ?  Orientation.HORIZONTAL :  Orientation.VERTICAL, order);
	    }

	private void layout(List<Frame> items, Rectangle2D.Double bounds, Orientation orientation)
	    {
	    sliceLayout(items,0,items.size()-1,bounds,orientation);
	    }*/
	
	    
	private List<Frame> sortDescending(List<Frame> items)
        {
        List<Frame> L=new ArrayList<Frame>(items);
        Collections.sort(L);
        return L;
        }
	
	private void layout(Frame f,final Rectangle2D.Double bounds)
		{
		if(!f.isLeaf())
			{
			Branch b=(Branch)f;
			Rectangle2D.Double bbox=b.getInnerRect();
			layout(b.children,bbox);
			for(Frame c:b.children)
				{
				layout(c,c.bounds);
				}
			}
		}
	
	private void layout(List<Frame> items,final Rectangle2D.Double bounds)
    	{
        layout(sortDescending(items),0,items.size()-1,bounds);
    	}
    
	private void layout(List<Frame> items, int start, int end, final Rectangle2D.Double bounds)
    {
        if (start>end) return;
            
        if (end-start<2)
        {
            layoutBest(items,start,end,bounds);
            return;
        }
        
        double x=bounds.x, y=bounds.y, w=bounds.width, h=bounds.height;
        
        double total=sum(items, start, end);
        int mid=start;
        double a=items.get(start).getWeight()/total;
        double b=a;
        
        if (w<h)
        {
            // height/width
            while (mid<=end)
            {
                double aspect=normAspect(h,w,a,b);
                double q=items.get(mid).getWeight()/total;
                if (normAspect(h,w,a,b+q)>aspect) break;
                mid++;
                b+=q;
            }
            layoutBest(items,start,mid,new Rectangle2D.Double(x,y,w,h*b));
            layout(items,mid+1,end,new Rectangle2D.Double(x,y+h*b,w,h*(1-b)));
        }
        else
        {
            // width/height
            while (mid<=end)
            {
                double aspect=normAspect(w,h,a,b);
                double q=items.get(mid).getWeight()/total;
                if (normAspect(w,h,a,b+q)>aspect) break;
                mid++;
                b+=q;
            }
           layoutBest(items,start,mid,new Rectangle2D.Double(x,y,w*b,h));
           layout(items,mid+1,end,new Rectangle2D.Double(x+w*b,y,w*(1-b),h));
        }
        
    }
    
    private double aspect(double big, double small, double a, double b)
    {
        return (big*b)/(small*a/b);
    }
    
    private double normAspect(double big, double small, double a, double b)
    {
        double x=aspect(big,small,a,b);
        if (x<1) return 1/x;
        return x;
    }


    
    /*
    private  void sliceLayout(List<Frame> items, int start, int end, Rectangle2D.Double bounds, Orientation orientation)
        {
            sliceLayout(items,start,end,bounds,orientation,Direction.ASCENDING);
        }*/
        
    private  void sliceLayout(List<Frame> items, int start, int end, final Rectangle2D.Double bounds, Orientation orientation, Direction order)
        {
            double total=sum(items, start, end);
            double a=0;
            boolean vertical=orientation==Orientation.VERTICAL;
           
            for (int i=start; i<=end; i++)
            {
            	Rectangle2D.Double r=new Rectangle2D.Double();
                double b=items.get(i).getWeight()/total;
                if (vertical)
                {
                    r.x=bounds.x;
                    r.width=bounds.width;
                    if (order==Direction.ASCENDING)
                        r.y=bounds.y+bounds.height*a;
                    else
                        r.y=bounds.y+bounds.height*(1-a-b);
                    r.height=bounds.height*b;
                }
                else
                {
                    if (order==Direction.ASCENDING)
                        r.x=bounds.x+bounds.width*a;
                    else
                        r.x=bounds.x+bounds.width*(1-a-b);
                    r.width=bounds.width*b;
                    r.y=bounds.y;
                    r.height=bounds.height;
                }
     
                items.get(i).bounds.setRect(r);
                a+=b;
            }
        }
    
   private String path(Shape shape)
	   {
	   StringBuilder path=new StringBuilder();
		double tab[] = new double[6];
		PathIterator pathiterator = shape.getPathIterator(null);
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
		return path.toString();
	   }
        
   private TreeMapMaker()
	   {
	   /* create small image */
		BufferedImage img=new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		/* get graphics from this image */
		this.gc=Graphics2D.class.cast(img.getGraphics());
	   }
   private Frame parse(Element root)
	   {
	   if(!root.getNodeName().equals(NODE_NAME))
		   {
		   throw new RuntimeException("Expected node <"+NODE_NAME+"> but got <"+root.getNodeName()+">");
		   }
	   boolean hasChildren=false;
	   Frame frame=null;
	   for(Node n=root.getFirstChild();n!=null;n=n.getNextSibling())
		   {
		   if(	n.getNodeType()!=Node.ELEMENT_NODE ||
				!n.getNodeName().equals(NODE_NAME)) continue;
		   hasChildren=true;
		   break;
		   }
	   if(hasChildren)
		   {
		   frame=new Branch(root);
		   }
	   else
		   {
		   Leaf leaf=new Leaf(root);
		   frame=leaf;
		   }
	   if(hasChildren)
		   {
		   for(Node n=root.getFirstChild();n!=null;n=n.getNextSibling())
			   {
			   if(	n.getNodeType()!=Node.ELEMENT_NODE ||
					!n.getNodeName().equals(NODE_NAME)) continue;
			   Frame c=parse(Element.class.cast(n));
			   c.parent=frame;
			   Branch.class.cast(frame).children.add(c);
			   }
		   }
	  
	   return frame;
	   }
   
   private String fitText(
		   String text,
		   Font font,
		   Rectangle2D.Double bounds)
	   {
	   FontRenderContext frc = this.gc.getFontRenderContext();
	   TextLayout textLayout=new TextLayout(text, font, frc);
		
	    Shape shape=textLayout.getOutline(null);
	   
	    if(bounds.width<bounds.height)
	    	{
	    	AffineTransform rotate=AffineTransform.getRotateInstance(
					Math.PI/2.0
					);
			shape=rotate.createTransformedShape(shape);
	    	}
	    Rectangle2D textBBox=shape.getBounds2D();
	    if(textBBox.getX()!=0 || textBBox.getY()!=0)
	    	{
	    	shape=AffineTransform.getTranslateInstance(
	    			-textBBox.getX(),-textBBox.getY()
					).createTransformedShape(shape);
	    	textBBox=shape.getBounds2D();
	    	}
	    double ratio=Math.max(
	    		textBBox.getWidth()/bounds.width,
	    		textBBox.getHeight()/bounds.height
	    		);
	    shape=AffineTransform.getScaleInstance(1/ratio,1/ratio)
   			.createTransformedShape(shape);
	   	textBBox=shape.getBounds2D();
			
	   	shape=AffineTransform.getTranslateInstance(
	   			bounds.x+(bounds.width-textBBox.getWidth())/2.0,
	   			bounds.y+(bounds.height-textBBox.getHeight())/2.0
					).createTransformedShape(shape);
	   return path(shape);
	   }
   
   private void svg(XMLStreamWriter w,Frame f)throws Exception
	   {
	   String textPath=null;
	   String selector=null;
	   Font font=new Font(f.getFontFamily(),Font.BOLD,1+(int)Math.max(f.bounds.width,f.bounds.height));
	   w.writeStartElement("svg","g",SVG);
	   selector=f.getStyle("stroke",null);
	   if(selector!=null)  w.writeAttribute("stroke",selector);
	   selector=f.getStyle("fill",null);
	   if(selector!=null)  w.writeAttribute("fill",selector);
	   selector=f.getStyle("stroke-width",String.valueOf(Math.max(0.2,2/(f.getDepth()+1.0))));
	   w.writeAttribute("stroke-width",selector);
	   
	   
	   w.writeEmptyElement("svg", "rect", SVG);
	   if(f.getDescription()!=null) w.writeAttribute("title",f.getDescription());
	  
	   w.writeAttribute("x",String.valueOf(f.bounds.getX()));
	   w.writeAttribute("y",String.valueOf(f.bounds.getY()));
	   w.writeAttribute("width",String.valueOf(f.bounds.getWidth()));
	   w.writeAttribute("height",String.valueOf(f.bounds.getHeight()));
	 
	   if(!f.isLeaf())
		   {
		   Branch branch=(Branch)f;
		   for(Frame c:branch.children)
			   {
			   svg(w,c);
			   }
		   if(f.getLabel()!=null)
			   {
			   Rectangle2D.Double bbox=f.getInnerRect();
			   if(bbox.getMaxY()+6 < f.bounds.getMaxY())
				   {
				   bbox=new Rectangle2D.Double(
						   f.bounds.x,bbox.getMaxY()+2,
						   f.bounds.width,
						   (f.bounds.getMaxY()-bbox.getMaxY())-4
						   );
				   textPath=fitText(f.getLabel(), font, bbox);
				   }
			   else if(bbox.getMaxX()+6 < f.bounds.getMaxX())
				   {
				   bbox=new Rectangle2D.Double(bbox.getMaxX(),f.bounds.y,(f.bounds.getMaxX()-bbox.getMaxX()),f.bounds.height);
				   textPath=fitText(f.getLabel(), font, bbox);
				   }
			   }
		   
		   }
	   else
		   {
		   if(f.getLabel()!=null)
			   {
			   textPath=fitText(f.getLabel(), font, f.getInnerRect());
			   }
		   }
	   
	   if(textPath!=null)
		   {
		   w.writeEmptyElement("svg", "path", SVG);
		   w.writeAttribute("d",textPath);
		   selector=f.getStyle("font-stroke",null);
		   if(selector!=null) w.writeAttribute("font-stroke","none");
		   selector=f.getStyle("font-fill",null);
		   if(selector!=null) w.writeAttribute("font-fill",selector);
		   }
	   
	   w.writeEndElement();//g
	   }
   
    private void svg(Frame f) throws Exception
    	{
		XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
		XMLStreamWriter w= xmlfactory.createXMLStreamWriter(System.out,"UTF-8");
		w.writeStartDocument("UTF-8","1.0");
		w.writeStartElement("svg","svg",SVG);
		w.writeAttribute("style", "fill:none;stroke:black;stroke-width:0.5px;");
		w.writeAttribute("xmlns", XMLConstants.XML_NS_URI, "svg", SVG);
		w.writeAttribute("xmlns", XMLConstants.XML_NS_URI, "xlink", XLINK);
		w.writeAttribute("width",String.valueOf(this.viewRect.getWidth()));
		w.writeAttribute("height",String.valueOf(this.viewRect.getHeight()));
		w.writeStartElement("svg","title",SVG);
		w.writeCharacters("made with TreeMapMaker (c) Pierre Lindenbaum");
		w.writeEndElement();
		svg(w,f);
		w.writeEndElement();//svg
		w.writeEndDocument();
		w.flush();
    	}
    public static void main(String[] args)
    	{
		try
			{
			TreeMapMaker app=new TreeMapMaker();
			int optind=0;
			while(optind< args.length)
				{
				if(args[optind].equals("-h") ||
				   args[optind].equals("-help") ||
				   args[optind].equals("--help"))
					{
					System.err.println("");
					System.err.println("Options:");
					System.err.println(" -h help; This screen.");
					return;
					}
				else if(args[optind].equals("-W"))
					{
					app.viewRect.width=Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-H"))
					{
					app.viewRect.height=Integer.parseInt(args[++optind]);
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
			if(optind+1!=args.length)
				{
				System.err.println("Illegal number of arguments.");
				return;
				}
						
			DocumentBuilderFactory f=DocumentBuilderFactory.newInstance();
			f.setCoalescing(true);
			f.setNamespaceAware(false);
			f.setValidating(false);
			f.setExpandEntityReferences(true);
			f.setIgnoringComments(false);
			f.setIgnoringElementContentWhitespace(true);
			Document dom=f.newDocumentBuilder().parse(new File(args[optind++]));
			Frame root=app.parse(dom.getDocumentElement());
			Rectangle2D.Double rect=new Rectangle2D.Double(0,0,
					app.viewRect.getWidth(),
					app.viewRect.getHeight()
					);
			root.bounds.setRect(rect);
			app.layout(root, rect);
			app.svg(root);
			} 
		catch(Throwable err)
			{
			err.printStackTrace();
			}
		
		}
		
		
	}
