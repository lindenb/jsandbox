/* 
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	May-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Compilation:
 *        ant geneticpainting
 * Usage:
 *        java -jar geneticpainting.jar 
 */
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
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.beust.jcommander.Parameter;

/**
 * GeneticPainting
 *
 */
public class GeneticPainting extends Launcher
	{
	protected static final Logger LOG=Logger.builder(GeneticPainting.class).build();
	private final int IMAGE_TYPE=BufferedImage.TYPE_INT_ARGB;
	private BufferedImage sourceImage=null;
	private Random random=new Random();
	@Parameter(names="-o",description="output file")
	private String fileout="_painting";
	@Parameter(names="-n5",description="shape min size")
	private int shape_min_size=10;
	@Parameter(names="-n6",description="shape max size")
	private int shape_max_size=20;
	@Parameter(names="-n0",description="scale image to this size")
	private int image_scaled_size=200;

	private char shape_type='c';
	private int n_threads=3;
	
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
		
		
		private int rnd255(int n)
			{
			n+= plusMinus(3);
			if(n<0) n=0;
			if(n>255) n=255;
			return n;
			}
		
		public void mute()
			{
			if(random.nextBoolean()) return;
 			red=rnd255(red);
			green=rnd255(green);
			blue=rnd255(blue);
			alpha=rnd255(alpha);
			if(alpha<0) alpha=1;
			}
		public void html(PrintWriter out)
			{
			
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
			this.radius= shape_min_size+random.nextInt(shape_max_size-shape_min_size);
			this.alpha=1+random.nextInt(254);
			
			int rgb1= sourceImage.getRGB(cx, cy);
			this.red = (rgb1 >> 16)&0xFF;
			this.green = (rgb1 >>8)&0xFF;
			this.blue = (rgb1 )&0xFF;
			
			}
		
		
		@Override
		public void html(PrintWriter out)
			{
			out.print(cx+ "," + cy + "," + radius + ","
					+ red + "," + green + "," + blue + ","+ (1.0-(alpha/255.0)));
			;
			}
		@Override
		public void mute()
			{
			super.mute();
			this.cx+=plusMinus(5);
			this.cy+=plusMinus(5);
			this.radius+=plusMinus(10);
			if(this.radius<=shape_min_size) this.radius=shape_min_size;
			if(this.cx-this.radius<0 || this.cx+this.radius> sourceImage.getWidth() ||
			   this.cy-this.radius<0 || this.cy+this.radius> sourceImage.getHeight())
				{
				this.cx=random.nextInt(sourceImage.getWidth());
				this.cy=random.nextInt(sourceImage.getHeight());
				}
			
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
			this.weight=shape_min_size+random.nextInt(shape_max_size-shape_min_size);
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
			if(this.weight<=0) this.weight=1;
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
			return "stroke:rgb("+red+","+green+","+blue+");stroke-opacity:"+(alpha/255.0)+";" +
					"stroke-linecap:butt;" +
					"stroke-linejoin:round;" +
					"fill:none;stroke-width:"+weight+"px;";
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
			int n=shape_min_size+random.nextInt(shape_max_size-shape_min_size);
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
						p.getX()+plusMinus(2),
						p.getY()+plusMinus(2)
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
							p3.getX()+plusMinus(3),
							p3.getY()+plusMinus(3)
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
		
		public Solution mute()
			{
			for(Figure f:this.shapes)
				{
				if(random.nextBoolean())
					{
					f.mute();
					}
				}
			if(random.nextBoolean() && this.shapes.size()< max_shape_per_solution)
				{
				this.shapes.add(random.nextInt(this.shapes.size()),makeFigure());
				}
			if(this.shapes.size()>1 && random.nextBoolean())
				{
				this.shapes.remove(random.nextInt(this.shapes.size()));
				}
			return this;
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
			if(i!=0) return i;
			i= shapes.size() - o.shapes.size();
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
		
		public void html(PrintWriter out)
			{
			long id=System.currentTimeMillis();
			out.print(
				"<html>\n"+
				"<head>\n"+
				"<script type=\"text/javascript\">\n"
				);
			
			out.print("var solution={width:"+
					sourceImage.getWidth()+",height:" +
					sourceImage.getHeight() +",shapes:["
					);
            for(int j=0;j< shapes.size();++j)
                    {
                    if(j>0) out.print(",");
                    shapes.get(j).html(out);
                    }
			 out.print("]};");
			
			out.print("function paint"+id+"(param)\n"+
			"\t{\n"+
			"\tparam.ctx=param.circle.getContext(\"2d\");\n"+
			"\tparam.ctx.fillStyle = \"rgb(255,255,255)\";\n"+
			"\tparam.ctx.fillRect (0,0, solution.width*param.scale,solution.height*param.scale);\n"+
			"\tfor(var i=0;i+7< param.array.length;i+=7)\n"+
			"\t\t{\n"+
			"\t\tvar dn=(param.maxRadius-param.n)*2.0*param.scale;\n"+
			"\t\tvar dx=dn*( param.array[i+0] % 2 ? -1 : 1) ;\n"+
			"\t\tvar dy=dn*( param.array[i+1] % 2 ? 1 : -1) ;\n"+
			"\t\tparam.ctx.beginPath();\n"+
			"\t\tparam.ctx.arc(\n"+
			"\t\t\tparam.array[i+0]*param.scale+dx,\n"+
			"\t\t\tparam.array[i+1]*param.scale+dy, \n"+
			"\t\t\t(param.n > param.array[i+2] ? param.array[i+2] : param.n)*param.scale,\n"+
			"\t\t\t0, Math.PI*2, true);\n"+
			"\t\tparam.ctx.closePath();\n"+
			"\t\tparam.ctx.fillStyle = \"rgb(\"+\n"+
			"\t\t\tparam.array[i+3]+\",\"+\n"+
			"\t\t\tparam.array[i+4] +\",\"+\n"+
			"\t\t\tparam.array[i+5] +\")\";\n"+
			"\t\tparam.ctx.globalAlpha= param.array[i+6];\n"+
			"\t\tparam.ctx.fill();\n"+
			"\t\t}\n"+
			"\tparam.ctx.fillStyle = \"rgb(0,0,0)\";\n"+
			"\t//param.ctx.drawRect(0,0, solution.width-1,solution.height-1);\n"+
			"\tif(param.n< param.maxRadius)\n"+
			"\t\t{\n"+
			"\t\tparam.n++;\n"+
			"\t\tsetTimeout(paint"+id+",param.time,param);\n"+
			"\t\t}\n"+
			"\t\n"+
			"\t}\n"+
			"function init"+id+"()\n"+
			"\t{\n"+
			"\tvar param={maxRadius:0,n:1,circle:null,ctx:null,array:[],time:50,scale:2.5};\n"+
			"\tparam.circle = document.getElementById(\"canvas"+id+"\");\n"+
			"\tif (!param.circle.getContext)return;\n"+
			"\tparam.circle.setAttribute(\"width\",solution.width*param.scale);\n"+
			"\tparam.circle.setAttribute(\"height\",solution.height*param.scale);\n"+
			"\tparam.ctx=param.circle.getContext(\"2d\");\n"+
			"\tparam.array= solution.shapes;\n"+
			"\t\n"+
			"\tfor(var i=0;i+7< param.array.length;i+=7)\n"+
			"\t\t{\n"+
			"\t\tif(param.maxRadius < param.array[i+2])\n"+
			"\t\t\t{\n"+
			"\t\t\tparam.maxRadius=param.array[i+2];\n"+
			"\t\t\t}\n"+
			"\t\t}\n"+
			"\tsetTimeout(paint"+id+",param.time,param);\n"+
			"\t}\n"+
			"</script>\n"+
			"</head>\n"+
			"<body onload=\"init"+id+"();\"><canvas id=\"canvas"+id+"\"/></body>\n"+
			"</html>");
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
	@Parameter(names="-n1",description="Max num shape per solution")
	private int max_shape_per_solution=3000;
	@Parameter(names="-n2",description="Min num shape per solution")
	private int min_shape_per_solution=100;
	
	private int plusMinus(int n)
		{
		return n-random.nextInt(2*n+1);
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
		if(random.nextBoolean())
			{
			Collections.sort(s.shapes,new java.util.Comparator<Figure>()
					{
					@Override
					public int compare(Figure o1, Figure o2) {
						double n= o1.area() - o2.area();
						return n==0?0:n<0?1:-1;
						}
					}
				);
			}
		return s;
		}
	
	private Solution mate(final Solution s1,final Solution s2)
		{
		
		
		Solution s=new Solution();
		boolean side1=random.nextBoolean();
		Solution sx= (side1?s1:s2);
		for(int i=0;i< Math.min(s1.shapes.size(),s2.shapes.size());++i)
			{
			sx= (side1?s1:s2);
			s.shapes.add((Figure)sx.shapes.get(i).clone());
			if(random.nextInt(5)<2) side1=!side1;
			}
		for(int i=s.shapes.size();i< sx.shapes.size();++i)
			{
			s.shapes.add((Figure)sx.shapes.get(i).clone());
			}
		
		
		while(s.shapes.size()>max_shape_per_solution)
			{
			s.shapes.remove(s.shapes.size()-1);
			}
		
		//s.mute();
		
		
		
		return s;
		}
	
	private class Fitness
		implements Callable<Long>
		{
		int rowStart;
		int rowEnd;
		BufferedImage tmpImage;
		
		Fitness()
			{
			
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
		
		@Override
		public Long call() throws Exception
			{
			long fitness=0L;
			for(int x=0;x< tmpImage.getWidth();++x )
				{
				for(int y=rowStart;y< rowEnd;++y )
					{
					int rgb1= sourceImage.getRGB(x, y);
					int rgb2= tmpImage.getRGB(x, y);
					fitness+=deltaRgb(rgb1, rgb2);
					}
				}
			return fitness;
			}
		}
	
	private int run() throws Exception
		{
		long now=System.currentTimeMillis();
		long n_generation=0;
		Solution best=null;
		BufferedImage tmpImage=new BufferedImage(
				this.sourceImage.getWidth(),
				this.sourceImage.getHeight(),
				IMAGE_TYPE
				);
		
		BufferedImage mosaicImage=new BufferedImage(
				this.sourceImage.getWidth(),
				this.sourceImage.getHeight(),
				this.sourceImage.getType()
				);
		
		int nThreads=this.n_threads;
		int rowsPerThreads=this.sourceImage.getHeight()/nThreads;
		if(rowsPerThreads<=0)
			{
			nThreads=1;
			rowsPerThreads=this.sourceImage.getHeight();
			}
		Fitness calls[]=new Fitness[nThreads];
		List<Future<Long>> futures=new ArrayList<Future<Long>>(nThreads);
		for(int i=0;i< calls.length;++i)
			{
			futures.add(null);
			Fitness f=new Fitness();
			f.rowStart=(i*rowsPerThreads);
			f.rowEnd=(i+1==calls.length?this.sourceImage.getHeight():(i+1)*rowsPerThreads);
			f.tmpImage=tmpImage;
			calls[i]=f;
			}

		
		for(;;)
			{
			++n_generation;
			List<Solution> parents=new ArrayList<Solution>();
			
			
			while(parents.size()< parent_count)
				{
				if(best==null || parents.isEmpty())
					{
					parents.add(makeSolution());
					}
				else if(parents.size()==1)
					{
					parents.add(Solution.class.cast(best.clone()));
					}
				else
					{
					parents.add(Solution.class.cast(best.clone()).mute());
					}
				}
			
			List<Solution> children=new ArrayList<Solution>(parents.size()*parents.size());
			
			
			
			for(int i=0;i< parents.size();++i)
				{
				for(int j=0;j< parents.size();++j)
					{
					if(i==j) continue;
					children.add(mate(parents.get(i),parents.get(j)));
					}
				}
				
			Graphics2D g3=mosaicImage.createGraphics();
			g3.setColor(Color.WHITE);
			g3.fillRect(0, 0, mosaicImage.getWidth(), mosaicImage.getHeight());
			int cols=0;
			int iconW=0;
			int iconH=0;
			if(!children.isEmpty())
				{
				cols=(int)Math.ceil(Math.sqrt(children.size()));
				int rows=(int)Math.ceil(children.size()/(double)cols);
				iconW=mosaicImage.getWidth()/cols;
				iconH=mosaicImage.getHeight()/rows;
				}
			int x=0;
			int y=0;
			for(Solution c:children)
				{
				c.fitness=0L;
				Graphics2D g2= tmpImage.createGraphics();
				g2.setStroke(new BasicStroke(1f));
				g2.setColor(Color.WHITE);
				g2.fillRect(0, 0, tmpImage.getWidth(), tmpImage.getHeight());
				c.paint(g2);
				g2.dispose();
				
				g3.drawImage(tmpImage,
						x*iconW,y*iconH,
						iconW,iconH,null
						);
				g3.setColor(Color.BLACK);
				g3.drawRect(x*iconW,y*iconH, iconW,iconH);
				
				x++;
				if(x==cols)
					{
					x=0;
					y++;
					}
				
				if(calls.length==1)
					{
					c.fitness=calls[0].call();
					}
				else
					{
					ExecutorService service=Executors.newFixedThreadPool(calls.length);
					for(int t=0;t< calls.length;++t)
						{
						Future<Long> rez=service.submit(calls[t]);
						futures.set(t, rez);
						}
					service.shutdown();
					while(!service.isTerminated())
						{
						//nothing
						}
					for(Future<Long> f: futures)
						{
						c.fitness+=f.get();
						}
					}
				}
			
			g3.dispose();
			
			
			Collections.sort(children);
			
			
			
			
			if( !children.isEmpty() &&
				(best==null || children.get(0).compareTo(best)<0)
				)
				{
				best=(Solution)children.get(0).clone();
				best.generation=n_generation;
				System.out.println("generation:"+n_generation+ " fitness:"+ best.fitness+" seconds:"+(System.currentTimeMillis()-now)/1000);
				now=System.currentTimeMillis();
				Graphics2D g2= tmpImage.createGraphics();
				g2.setStroke(new BasicStroke(1f));
				g2.setBackground(new Color(255,255,255,0));
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
				
				if(this.shape_type=='c')
					{
					PrintWriter o=new PrintWriter(new File(this.fileout+".html"));
					best.html(o);
					o.flush();
					o.close();
					}
				
				}
			else if(!children.isEmpty())
				{
				System.out.println("best(Generation="+n_generation+")="+children.get(0).fitness);
				ImageIO.write(mosaicImage, "PNG", new File(this.fileout+"-mosaic.png"));	
				}
			/*
			while(children.size()>parent_count)
				{
				children.remove(children.size()-1);
				}
			parents=children;*/ 
			}
		
		}
	
	private static int pow2(int a)
		{
		if(a<0) a=-a;
		return a*a;
		}
	@Override
	public int doWork(List<String> args) {
		
		if(args.size()!=1)
				{
				LOG.error("Illegal number of arguments");
				return -1;
				}
		try
			{
			final String filename=args.get(0);
			LOG.info("reading "+filename);
			this.sourceImage = ImageIO.read(new File(filename));
			if(this.sourceImage.getWidth()>image_scaled_size ||
				this.sourceImage.getHeight()>image_scaled_size ||
				this.sourceImage.getType()!=IMAGE_TYPE)
				{
				final int w,h;
				if(this.sourceImage.getWidth()<=image_scaled_size && this.sourceImage.getHeight()<=image_scaled_size) {
					w = this.sourceImage.getWidth();
					h= this.sourceImage.getHeight();
					}
				else if(this.sourceImage.getWidth()>this.sourceImage.getHeight() )
					{
					w=image_scaled_size;
					h=(int)(this.sourceImage.getHeight()*(w/(double)this.sourceImage.getWidth()));
					}
				else
					{
					h=image_scaled_size;
					w=(int)(this.sourceImage.getWidth()*(h/(double)this.sourceImage.getHeight()));
					}
				final BufferedImage img2=new BufferedImage(w, h, IMAGE_TYPE);
				Graphics2D g=img2.createGraphics();
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, w, h);
				g.drawImage(this.sourceImage, 0, 0,w,h,null);
				g.dispose();
				this.sourceImage=img2;
				}
				
			return this.run();
			}
		catch (final Exception e)
			{
			LOG.error(e);
			return -1;
			}
		}
	
	
	public static void main(final String[] args) {
		new GeneticPainting().instanceMainWithExit(args);
	}
	
}
