package sandbox;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import sandbox.io.IOUtils;

public abstract class SimpleGraphics implements Closeable {

	public class Style
		{
		private Color _stroke=Color.BLACK;
		private Color _fill=Color.BLACK;
		private float _alpha=1.f;
		
		boolean hasStroke() { return getStroke()!=null;}
		Color getStroke() { return _stroke;}
		boolean hasFill() { return getFill()!=null;}
		Color getFill() { return _fill;}
		float getAlpha() { return this._alpha;}
		
		Style() {
			}
		Style(final Style src)  {
			this._fill = src._fill;
			this._stroke = src._stroke;
			this._alpha = src._alpha;
			}
		Style stroke(Color c) { this._stroke = c; return this;}
		Style fill(Color c) { this._fill = c; return this;}
		Style alpha(float c) { this._alpha = c; return this;}

		}
		
	protected Stack<Style> styleStack = new Stack<>();
	protected final int width ;
	protected final int height;
	
	protected SimpleGraphics(final int width,final int height) {
		this.styleStack.add(new Style());
		this.width = width;
		this.height = height;
		}
	public int getWidth() {
		return width;
	}
	public int getHeight() {
		return height;
	}
	
	protected Style getStyle() {
		return this.styleStack.peek();
	}
	
	public abstract void circle(double cx,double cy,double r);
	public abstract void line(double x1,double y1,double x2,double y2);
	public abstract void rect(double x,double y,double width,double height);
	public abstract void polygon(double x[],double y[]);

	public void setFill(Color c) { getStyle().fill(c);}
	public void setStroke(Color c) { getStyle().stroke(c);}
	public void setAlpha(float a) { getStyle().alpha(a);}
	
	private static class SimpleGraphics2D
	extends SimpleGraphics
		{
		final Path  outputFile;
		final BufferedImage img;
		final Graphics2D g;
		SimpleGraphics2D(final Path outputFile,int width,int height)
			{
			super(width,height);
			this.outputFile = outputFile;
			img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
			this.g= img.createGraphics();
			}
		
		private void shape(final Shape shape)
			{
			float alpha = getStyle().getAlpha();
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
			if(getStyle().hasFill())
				{
				g.setColor(getStyle().getFill());
				g.fill(shape);
				}
			if(getStyle().hasStroke())
				{
				g.setColor(getStyle().getStroke());
				g.draw(shape);
				}
			g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1f));
			}
		
		@Override
		public void circle(double cx,double cy,double r)
			{
			this.shape(new Ellipse2D.Double(cx-r, cy-r, r*2, r*2));
			}
		@Override
		public void line(double x1, double y1, double x2, double y2) {
			this.shape(new Line2D.Double(x1,y2,x2,y2));
			}
		@Override
		public void rect(double x, double y, double width, double height) {
			this.shape(new Rectangle2D.Double(x,y,width,height));
			}
		
		@Override
		public void polygon(double[] x, double[] y) {
			if(x.length<2 || y.length<2 || x.length!=y.length) return;
			GeneralPath path = new GeneralPath(GeneralPath.WIND_EVEN_ODD);
			path.moveTo(x[0], y[0]);
			for(int i=1;i< x.length;++i) {
				path.lineTo(x[i], y[i]);
				}
			path.closePath();
			this.shape(path);
			}
		
		
		@Override
		public void close() {
			try {
			String formatName="JPG";
			OutputStream output = this.outputFile==null?System.out:Files.newOutputStream(outputFile);
			if(this.outputFile!=null && this.outputFile.getFileName().toString().toLowerCase().endsWith(".png")) {
				formatName="PNG";
				}
			ImageIO.write(this.img, formatName, output);
			output.close();
			} catch(Exception err)
				{
					
				}
			}
		}
	
	private static class SVGSimpleGraphics
		extends SimpleGraphics
		{
		private Writer writer;
		private XMLStreamWriter w;
		
		SVGSimpleGraphics(Writer writer,XMLStreamWriter w,int width,int height)
			throws XMLStreamException
			{
			super(width,height);
			this.writer=writer;
			this.w=w;
			this.w.writeStartDocument("UTF-8", "1.0");
			this.w.writeStartElement("svg");
			this.w.writeDefaultNamespace("http://www.w3.org/2000/svg");
			this.w.writeAttribute("width",String.valueOf(this.width));
			this.w.writeAttribute("height",String.valueOf(this.height));
			
			this.w.writeStartElement("g");
			}
		
		private String toRGB(final Color c) {
			return "rgb("+c.getRed()+","+c.getGreen()+","+c.getBlue()+")";
		}
		
		private void writeStyle() throws XMLStreamException {
			final Map<String,String> sb=new HashMap<String, String>();
			Style st = getStyle();
			sb.put("stroke",st.hasStroke()?toRGB(st.getStroke()):"none");
			sb.put("fill",st.hasFill()?toRGB(st.getFill()):"none");
		
			final float alpha = getStyle().getAlpha();
			if(alpha!=1f) sb.put("opacity",String.valueOf(alpha));
			
			this.w.writeAttribute("style",
				sb.entrySet().
				stream().
				map(KV->KV.getKey()+":"+KV.getValue()).
				collect(Collectors.joining(";"))
				);
			}
		
		@Override
		public void circle(double cx, double cy, double r) {
			try {
				if(r<=0) return;
				this.w.writeEmptyElement("circle");
				this.w.writeAttribute("cx", String.valueOf(cx));
				this.w.writeAttribute("cy", String.valueOf(cy));
				this.w.writeAttribute("r", String.valueOf(r));
				writeStyle();
				} 
			catch(XMLStreamException err) {
				
				}
			}
		@Override
		public void line(double x1, double y1, double x2, double y2) {
			try {
				this.w.writeEmptyElement("line");
				this.w.writeAttribute("x1", String.valueOf(x1));
				this.w.writeAttribute("y1", String.valueOf(y1));
				this.w.writeAttribute("x2", String.valueOf(x2));
				this.w.writeAttribute("y2", String.valueOf(x2));
				writeStyle();
				} 
			catch(XMLStreamException err) {
				
				}
			}
		
		@Override
		public void polygon(double[] x, double[] y) {
			if(x.length<2 || y.length<2 || x.length!=y.length) return;
			try {
				this.w.writeEmptyElement("polygon");
				final StringBuilder sb=new StringBuilder();
				for(int i=0;i< x.length;++i) {
					if(i>0) sb.append(" ");
					sb.append(x[i]).append(",").append(y[i]);
					}
				
				this.w.writeAttribute("points",sb.toString());
				writeStyle();
				} 
			catch(XMLStreamException err) {
				
				}
			}
		
		@Override
		public void rect(double x, double y, double width, double height) {
			try {
				this.w.writeEmptyElement("rect");
				this.w.writeAttribute("x", String.valueOf(x));
				this.w.writeAttribute("y", String.valueOf(y));
				this.w.writeAttribute("width", String.valueOf(width));
				this.w.writeAttribute("height", String.valueOf(height));
				writeStyle();
				} 
			catch(XMLStreamException err) {
				
				}
			}
		
		@Override
		public void close(){
			try {
				this.w.writeEndElement();//g
				this.w.writeEndElement();//svg
				this.w.writeEndDocument();
				this.w.flush();
				this.w.close();
				this.writer.close();
				}
			catch(Exception err) {
				
				}
			}
		}
	
	private static class SimplePostscript
	extends SimpleGraphics
		{
		PrintWriter pw;
		
		SimplePostscript(Path p,int width,int height) throws IOException {
			super(width,height);
			this.pw = (p==null?new PrintWriter(System.out):new PrintWriter(Files.newBufferedWriter(p)));
			this.pw.println("%!PS-Adobe-3.0 EPSF-3.0");
			this.pw.println("%%Creator: "+SimpleGraphics.class.getName());
			this.pw.println("%%BoundingBox: 0 0 " + toInch(width)+" "+toInch(height));
			this.pw.println("%%Pages: 1");
			this.pw.println("%%Page: 1 1");
			}
		
		private double flipY(double y) {
			return this.getHeight()-y;
			}
		private double toInch(double v) {
			return v/72.0;
			}
		private boolean before(int side) {
			Style st= getStyle();
			Color c = (side==0?st.getFill():st.getStroke());
			if(c==null)return false;
			return true;
			}
		@Override
		public void line(double x1, double y1, double x2, double y2) {
			if(!before(1)) return;
			pw.print(toInch(x1));
			pw.print(" ");
			pw.print(toInch(flipY(y1)));
			pw.print(" ");
			pw.print(toInch(x2));
			pw.print(" ");
			pw.print(toInch(flipY(y2)));
			pw.print(" stroke ");
			}
		
		@Override
		public void rect(double x, double y, double width, double height) {
			if(width<=0 || height<=0) return;
			double xv[]=new double[] {x,x+width,x+width,x};
			double yv[]=new double[] {y,y,y+height,y+height};
			polygon(xv, yv);
			}
		
		@Override
		public void circle(double cx, double cy, double r) {
			for(int i=0;i<2;i++) {
				if(!before(i)) continue;
				pw.print(toInch(cx));
				pw.print(" ");
				pw.print(toInch(flipY(cy)));
				pw.print(" ");
				pw.print(toInch(r));
				pw.print(" 0 360 arc ");
				pw.print(i==0?" fill ":" stroke ");
				}
			}
		
		@Override
		public void polygon(double[] x, double[] y) {
			if(x.length<2 || y.length<2 || x.length!=y.length) return;
			for(int i=0;i<2;i++) {
				if(!before(i)) continue;
				for(int j=0;j< x.length;++j) {
					pw.print(toInch(x[j]));
					pw.print(" ");
					pw.print(toInch(flipY(y[j])));
					pw.print(j==0?" moveto ":" lineto ");
					}
				pw.print(" closepath ");
				pw.print(i==0?" fill ":" stroke ");
				}
			
			
		}
		
		@Override
		public void close() throws IOException {
			this.pw.println("showpage");
			this.pw.println("%EOF");
			pw.flush();
			pw.close();
			}
		}
	public static SimpleGraphics openPathForPostScript(Path path,int width,int height) throws IOException {
		return new SimplePostscript(path, width, height);
		}
	public static SimpleGraphics openPathForRaster(Path path,int width,int height) throws IOException {
		return new SimpleGraphics2D(path, width, height);
		}
	
	public static SimpleGraphics openPathForSvg(Path path,int width,int height) throws IOException
		{
		try {
			final Writer w = IOUtils.openPathAsWriter(path);
			XMLOutputFactory xof = XMLOutputFactory.newFactory();
			XMLStreamWriter out= xof.createXMLStreamWriter(w);
			SVGSimpleGraphics g = new SVGSimpleGraphics(w,out,width,height);
			return g;
			}
		catch(final XMLStreamException err) {
			throw new IOException(err);
			}
		}
	public static SimpleGraphics openPath(Path path,int width,int height) throws IOException
		{
			String suffix=path==null?"":path.getFileName().toString();
			if(suffix.endsWith(".svg") || suffix.endsWith(".svg.gz"))
				{
				return openPathForSvg(path, width, height);
				}
			if(suffix.endsWith(".ps") || suffix.endsWith(".eps"))
				{
				return openPathForPostScript(path, width, height);
				}
			return openPathForRaster(path, width, height);
		}
}
