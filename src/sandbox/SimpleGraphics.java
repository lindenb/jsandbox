package sandbox;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
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

import sandbox.IOUtils;

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
		public void rect(double x, double y, double width, double height) {
			try {
				this.w.writeEmptyElement("line");
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
			return openPathForRaster(path, width, height);
		}
}
