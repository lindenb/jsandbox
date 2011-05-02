package sandbox;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

/**
 * GeneticPainting
 *
 */
public class GeneticPainting
	{
	private BufferedImage sourceImage=null;
	private Random random=new Random();
	private String fileout="_painting";
	private int shape_size=20;
	private char shape_type='c';
	
	private abstract class Figure implements Cloneable
		{
		int red;
		int green;
		int blue;
		int alpha;
		
		String getSvgStyle()
			{
			return "fill:rgb("+red+","+green+","+blue+");fill-opacity:"+(alpha/255.0)+";";
			}
		public abstract void paint(Graphics2D g);
		
		public void mute()
			{
			red=rnd255(red);
			green=rnd255(green);
			blue=rnd255(blue);
			alpha=rnd255(alpha);
			if(alpha<0) alpha=1;
			}
		public abstract Object clone() ;
		public abstract double area();
		public abstract void xml(XMLStreamWriter w) throws XMLStreamException;
		}
	
	
	private class Circle extends Figure
		{
		int cx;
		int cy;
		int radius;
		Circle()
			{
			this.cx=random.nextInt(sourceImage.getWidth());
			this.cy=random.nextInt(sourceImage.getHeight());
			this.radius=1+random.nextInt(shape_size);
			this.alpha=1+random.nextInt(254);
			int rgb1= sourceImage.getRGB(cx, cy);
			this.red = (rgb1 >> 16)&0xFF;
			this.green = (rgb1 >>8)&0xFF;
			this.blue = (rgb1 )&0xFF;
			}
		
		@Override
		public void mute()
			{
			this.cx+=plusMinus(1);
			this.cy+=plusMinus(1);
			this.radius+=plusMinus(1);
			if(this.radius<=0) this.radius=1;
			}
		public void paint(Graphics2D g)
			{
			g.setColor(new Color(red, green, blue, alpha));
			g.fill(new Ellipse2D.Double(
					cx-radius,
					cy-radius,
					radius*2,
					radius*2
				));
			}
		@Override
		public Object clone() 
			{
			Circle cp=new Circle();
			cp.alpha=alpha;
			cp.cx=cx;
			cp.cy=cy;
			cp.radius=radius;
			cp.red=red;
			cp.green=green;
			cp.blue=blue;
			return cp;
			}
		@Override
		public double area() {
			return radius;
			}
		
		@Override
		public void xml(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeEmptyElement("circle");
			w.writeAttribute("cx", String.valueOf(cx));
			w.writeAttribute("cy", String.valueOf(cy));
			w.writeAttribute("r", String.valueOf(radius));
			w.writeAttribute("style", getSvgStyle()+"stroke:none;");
			}
	}
	
	
	private class Line extends Figure
		{
		int x1,x2,y1,y2;
		int weight;
		Line()
			{
			this.x1=random.nextInt(sourceImage.getWidth());
			this.y1=random.nextInt(sourceImage.getHeight());
			this.x2=random.nextInt(sourceImage.getWidth());
			this.y2=random.nextInt(sourceImage.getHeight());
			this.weight=1+random.nextInt(shape_size);
			this.alpha=1+random.nextInt(254);
			int rgb1= sourceImage.getRGB((x1+x2)/2,(y1+y2)/2);
			this.red = (rgb1 >> 16)&0xFF;
			this.green = (rgb1 >>8)&0xFF;
			this.blue = (rgb1 )&0xFF;
			}
		
		@Override
		public void mute()
			{
			super.mute();
			this.x1+=plusMinus(3);
			this.y1+=plusMinus(3);
			this.x2+=plusMinus(3);
			this.y2+=plusMinus(3);
			this.weight+=plusMinus(3);
			}
		public void paint(Graphics2D g)
			{
			g.setColor(new Color(red, green, blue, alpha));
			g.setStroke(new BasicStroke(this.weight, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));
			g.draw(new Line2D.Double(
					x1,y1,x2,y2
				));
			}
		@Override
		public Object clone() 
			{
			Line cp=new Line();
			cp.alpha=alpha;
			cp.x1=x1;
			cp.y2=y1;
			cp.x2=x2;
			cp.y1=y2;
			cp.weight=weight;
			cp.red=red;
			cp.green=green;
			cp.blue=blue;
			return cp;
			}
		@Override
		public double area() {
			return weight;
			}
		
		@Override
		String getSvgStyle() {
			return "stroke:rgb("+red+","+green+","+blue+");stroke-opacity:"+(alpha/255.0)+";stroke-linecap:butt;fill:none;stroke-width:"+weight+"px;";
			}
		
		@Override
		public void xml(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeEmptyElement("line");
			w.writeAttribute("x1", String.valueOf(x1));
			w.writeAttribute("x2", String.valueOf(x2));
			w.writeAttribute("y1", String.valueOf(y1));
			w.writeAttribute("y2", String.valueOf(y2));
			w.writeAttribute("style", getSvgStyle());
			}
		}
	
	
	private class Poly extends Figure
		{
		private List<Point2D.Double> points=new ArrayList<Point2D.Double>();
		Poly()
			{
			int n=1+random.nextInt(shape_size);
			this.alpha=1+random.nextInt(254);
			
			Point2D.Double p1=new Point2D.Double(
				random.nextInt(sourceImage.getWidth()),
				random.nextInt(sourceImage.getHeight())
				);
			Point2D.Double p2=new Point2D.Double(
				p1.getX()+n,
				p1.getY()
				);
			Point2D.Double p3=new Point2D.Double(
				p1.getX()+n,
				p1.getY()+n
				);
			Point2D.Double p4=new Point2D.Double(
				p1.getX(),
				p1.getY()+n
				);
			
			int rgb1= 0;
			
			if(	p3.getX()< sourceImage.getWidth() &&
				p3.getY()< sourceImage.getHeight())
				{
				rgb1=sourceImage.getRGB(
					(int)((p1.getX()+p3.getX())/2.0),
					(int)((p1.getY()+p3.getY())/2.0)
					);
				}
			else
				{
				rgb1=sourceImage.getRGB((int)p1.getX(),(int)p1.getY());
				}
			this.red = (rgb1 >> 16)&0xFF;
			this.green = (rgb1 >>8)&0xFF;
			this.blue = (rgb1 )&0xFF;
			this.points.add(p1);
			this.points.add(p2);
			this.points.add(p3);
			this.points.add(p4);
			
			}
		Poly(int capacity)
			{
			
			}
		@Override
		public void mute()
			{
			super.mute();
			switch(random.nextInt(20))
				{
				case 0:
					{
					int n=random.nextInt(this.points.size());
					Point2D.Double p=this.points.get(n);
					p.setLocation(
						p.getX()+plusMinus(1),
						p.getY()+plusMinus(1)
						);
					break;
					}
				case 1:
					{
					if(this.points.size()>5)
						{
						int n=random.nextInt(this.points.size());
						this.points.remove(n);
						}
					break;
					}
				case 2:
					{
					int n1=random.nextInt(this.points.size());
					int n2=n1+1;
					if(n2==this.points.size()) n2=0;
					Point2D.Double p1=this.points.get(n1);
					Point2D.Double p2=this.points.get(n2);
					Point2D.Double p3=new Point2D.Double(
							(p1.getX()+p2.getX())/2.0,
							(p1.getY()+p2.getY())/2.0
							);
					p3.setLocation(
							p3.getX()+plusMinus(2),
							p3.getY()+plusMinus(2)
							);
					this.points.add(n1+1,p3);
					break;
					}
				default:break;
				}
				
			}
		private GeneralPath getPath()
			{
			GeneralPath path=new GeneralPath(GeneralPath.WIND_NON_ZERO);
			for(int i=0;i<points.size();++i)
				{
				Point2D.Double p=this.points.get(i);
				if(i==0)
					{
					path.moveTo(p.getX(),p.getY());
					}
				else
					{
					path.lineTo(p.getX(),p.getY());
					}
				}
			path.closePath();
			return path;
			}
		public void paint(Graphics2D g)
			{
			g.setColor(new Color(red, green, blue, alpha));
			g.fill(getPath());
			}
		
		@Override
		public Object clone() 
			{
			Poly cp=new Poly(this.points.size());
			cp.alpha=alpha;
			cp.red=red;
			cp.green=green;
			cp.blue=blue;
			
			for(Point2D.Double d:this.points)
				{
				cp.points.add(new Point2D.Double(d.getX(),d.getY()));
				}
			return cp;
			}
		
		@Override
		public double area() {
			Rectangle r= getPath().getBounds();
			return  r.getWidth() * r.getHeight();
			}
		@Override
		public void xml(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeEmptyElement("path");
			StringBuilder b=new StringBuilder();
			for(int i=0;i<points.size();++i)
				{
				b.append(i==0?"M":" L");
				b.append(points.get(i).getX());
				b.append(" ");
				b.append(points.get(i).getY());
				}
			b.append(" Z");
			w.writeAttribute("d", b.toString());
			w.writeAttribute("style",getSvgStyle()+"fill-rule:nonzero;");
			}
		}
	
	private class Solution implements Comparable<Solution>,Cloneable
		{
		long generation=0;
		Long fitness=null;
		List<Figure> shapes=new ArrayList<Figure>();
		
		public void mute()
			{
			for(Figure f:this.shapes) f.mute();
			if(random.nextBoolean())
				{
				this.shapes.add(random.nextInt(this.shapes.size()),makeFigure());
				}
			if(this.shapes.size()>1 && random.nextBoolean())
				{
				this.shapes.remove(random.nextInt(this.shapes.size()));
				}
			}
		
		public void paint(Graphics2D g)
			{
			for(Figure f: this.shapes)
				{
				f.paint(g);
				}
			}
		@Override
		public int compareTo(Solution o) {
			int i= fitness.compareTo(o.fitness);
			return i;
			}
		
		@Override
		public Object clone()
			{
			Solution s=new Solution();
			s.fitness=this.fitness;
			for(int i=0;i< this.shapes.size();++i)
				{
				s.shapes.add((Figure)(this.shapes.get(i).clone()));
				}
			return s;
			}
		
		public void xml(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeStartDocument("UTF-8", "1.0");
			w.writeStartElement("svg");
			w.writeAttribute("xmlns", "http://www.w3.org/2000/svg");
			w.writeAttribute("width", String.valueOf(sourceImage.getWidth()));
			w.writeAttribute("height", String.valueOf(sourceImage.getHeight()));
			w.writeAttribute("style", "stroke:none;");
			w.writeStartElement("title");
			w.writeCharacters("Fitness:"+fitness+" Generation:"+generation);
			w.writeEndElement();
			
			w.writeEmptyElement("rect");
			w.writeAttribute("style", "fill:none;stroke:black;");
			w.writeAttribute("x", "0");
			w.writeAttribute("y", "0");
			w.writeAttribute("width", String.valueOf(sourceImage.getWidth()-1));
			w.writeAttribute("height", String.valueOf(sourceImage.getHeight()-1));
			
			w.writeStartElement("g");
			for(Figure f:this.shapes)
				{
				f.xml(w);
				}
			w.writeEndElement();
			
			w.writeEmptyElement("rect");
			w.writeAttribute("style", "fill:none;stroke:black;");
			w.writeAttribute("x", "0");
			w.writeAttribute("y", "0");
			w.writeAttribute("width", String.valueOf(sourceImage.getWidth()-1));
			w.writeAttribute("height", String.valueOf(sourceImage.getHeight()-1));
			
			w.writeEndElement();
			}
		}
	
	private int parent_count=5;
	private int max_shape_per_solution=3000;
	private int min_shape_per_solution=100;
	
	private int plusMinus(int n)
		{
		return n-random.nextInt(2*n+1);
		}
	
	private int rnd255(int n)
		{
		n+= plusMinus(3);
		if(n<0) n=0;
		if(n>255) n=255;
		return n;
		}
	
	
	
	private Figure makeFigure()
		{
		switch(shape_type)
			{
			case 'l':case 'L': return new Line();
			case 'p': return new Poly();
			default: return new Circle();
			}
		}
	
	private Solution makeSolution()
		{	
		Solution s=new Solution();
		int n=min_shape_per_solution+ random.nextInt(max_shape_per_solution-min_shape_per_solution);
		while(s.shapes.size()<n)
			{
			s.shapes.add(makeFigure());
			}
		return s;
		}
	
	private Solution mate(Solution s1, Solution s2)
		{
		if(random.nextBoolean())
			{
			Solution s3=s1; s1=s2; s2=s3;
			}
		Solution p1= (Solution)s1.clone();
		p1.mute();
		Solution p2= (Solution)s2.clone();
		p2.mute();
		Solution s=new Solution();
		int index=random.nextInt(p1.shapes.size());
		for(int i=0;i<index && i< p1.shapes.size();++i)
			{
			s.shapes.add(p1.shapes.get(i));
			}
		for(int i=index ; i< p2.shapes.size();++i)
			{
			s.shapes.add(p2.shapes.get(i));
			}
		
		
		if(random.nextBoolean())
			{
			s.shapes.remove(random.nextInt(s.shapes.size()));
			}
		
		while(s.shapes.size()>max_shape_per_solution)
			{
			s.shapes.remove(s.shapes.size()-1);
			}
		
		if(random.nextBoolean())
			{
			s.shapes.add(random.nextInt(s.shapes.size()), makeFigure());
			}
		
		if(random.nextBoolean())
			{
			Collections.sort(s.shapes,new java.util.Comparator<Figure>()
					{
					@Override
					public int compare(Figure o1, Figure o2) {
						double n= o1.area() - o2.area();
						return n==0?0:n<0?-1:1;
						}
					}
				);
			}
		
		return s;
		}
	
	private void run() throws IOException,XMLStreamException
		{
		long now=System.currentTimeMillis();
		long n_generation=0;
		Solution best=null;
		BufferedImage tmpImage=new BufferedImage(
				this.sourceImage.getWidth(),
				this.sourceImage.getHeight(),
				BufferedImage.TYPE_INT_ARGB
				);
		List<Solution> parents=new ArrayList<Solution>();
		while(parents.size()< parent_count)
			{
			parents.add(makeSolution());
			}
		
		for(;;)
			{
			++n_generation;
			List<Solution> children=new ArrayList<Solution>(parents.size()*parents.size());
			for(int i=0;i< parents.size();++i)
				{
				for(int j=0;j< parents.size();++j)
					{
					if(i==j) continue;
					children.add(mate(parents.get(i),parents.get(j)));
					}
				}
			
			for(Solution c:children)
				{
				c.fitness=0L;
				Graphics2D g2= tmpImage.createGraphics();
				g2.clearRect(0, 0, tmpImage.getWidth(), tmpImage.getHeight());
				c.paint(g2);
				for(int x=0;x< tmpImage.getWidth();++x )
					{
					for(int y=0;y< tmpImage.getHeight();++y )
						{
						int rgb1= this.sourceImage.getRGB(x, y);
						int rgb2= tmpImage.getRGB(x, y);
						c.fitness+=deltaRgb(rgb1, rgb2);
						}
					}
				g2.dispose();
				
				}
			if(best!=null)
				{
				Solution sol=(Solution)best.clone();
				sol.clone();
				children.add(sol);
				}
			Collections.sort(children);
			
			if(best==null || (!children.isEmpty() && children.get(0).fitness<best.fitness))
				{
				best=children.get(0);
				best.generation=n_generation;
				System.out.println("generation:"+n_generation+ " fitness:"+ best.fitness+" seconds:"+(System.currentTimeMillis()-now)/1000);
				now=System.currentTimeMillis();
				Graphics2D g2= tmpImage.createGraphics();
				g2.clearRect(0, 0, tmpImage.getWidth(), tmpImage.getHeight());
				best.paint(g2);
				g2.dispose();
				ImageIO.write(tmpImage, "PNG", new File(this.fileout+".png"));
				
				XMLOutputFactory factory=XMLOutputFactory.newFactory();
				FileOutputStream fout=new FileOutputStream(this.fileout+".svg");
				XMLStreamWriter w=factory.createXMLStreamWriter(fout, "UTF-8");
				best.xml(w);
				fout.flush();
				fout.close();
				
				}
			
			while(children.size()>parent_count)
				{
				children.remove(children.size()-1);
				}
			parents=children; 
			}
		
		}
	
	private static int pow2(int a)
		{
		if(a<0) a=-a;
		return a*a;
		}
	
	private int deltaRgb(int rgb1,int rgb2)
		{
		int r1 = (rgb1)&0xFF;
		int g1 = (rgb1>>8)&0xFF;
		int b1 = (rgb1>>16)&0xFF;
		int a1 = (rgb1>>24)&0xFF;
	
		int r2 = (rgb2)&0xFF;
		int g2 = (rgb2>>8)&0xFF;
		int b2 = (rgb2>>16)&0xFF;
		int a2 = (rgb2>>24)&0xFF;
		
		return 	pow2(r1-r2)+
				pow2(g1-g2)+
				pow2(b1-b2)+
				pow2(a1-a2)
				;
		}
	
	public static void main(String[] args)
		{
		GeneticPainting app=new GeneticPainting();
		try {
			int optind=0;
			while(optind<args.length)
				{
				if(args[optind].equals("-h"))
					{
					System.out.println(" -o fileprefix "+app.fileout);
					System.out.println(" -n1 parents count");
					System.out.println(" -n2 min shapes per solution");
					System.out.println(" -n3 max shapes per solution");
					System.out.println(" -n4 shape-size per solution");
					System.out.println(" -t 'shape-type' c:circle l:line");
					return;
					}
				else if(args[optind].equals("-t"))
					{
					app.shape_type= args[++optind].toLowerCase().charAt(0);
					}
				else if(args[optind].equals("-n1"))
					{
					app.parent_count=Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-n2"))
					{
					app.min_shape_per_solution=Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-n3"))
					{
					app.max_shape_per_solution=Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-n4"))
					{
					app.shape_size = Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-o"))
					{
					app.fileout=new String(args[++optind]);
					}
				else if(args[optind].equals("--"))
					{
					optind++;
					break;
					}
				else if(args[optind].startsWith("-"))
					{
					System.err.println("Unnown option: "+args[optind]);
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
				System.err.println("Illegal number of arguments");
				return;
				}
			app.sourceImage=ImageIO.read(new File(args[optind]));
			app.run();
			}
		catch (Exception e)
			{
			e.printStackTrace();
			}
		}
}
