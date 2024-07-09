package sandbox.graphics;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import sandbox.StringUtils;
import sandbox.functional.FunctionalMap;
import sandbox.svg.SVG;

public abstract class Canvas implements AutoCloseable {
	
	public abstract int getWidth();
	public abstract int getHeight();
	
	protected Canvas() {
		}

	public abstract Canvas line(double x1,double y1,double x2,double y2,FunctionalMap<String, Object> props);
	public abstract Canvas polygon(double[] x,double[] y,FunctionalMap<String, Object> props);
	public abstract Canvas circle(double cx,double cy,double r,FunctionalMap<String, Object> props);

	
	public Canvas rectangle(double x,double y, double width,double height,FunctionalMap<String, Object> props) {
		double[] ax = new double[] {x,x+width,x+width,x};
		double[] ay = new double[] {y,y,y+height,y+height};
		return polygon(ax,ay,props);
		}
	
	
	private static class Graphics2DCanvas extends Canvas {
		final File outputFile;
		final BufferedImage image;
		final Graphics2D g2d;
		Graphics2DCanvas(File outputFile,int width,int height,int imgType) {
			this.outputFile=outputFile;
			this.image = new BufferedImage(width, height, imgType);
			this.g2d = this.image.createGraphics();
			}
		protected BufferedImage getImage() {
			return image;
			}
		@Override
		public int getWidth() {
			return getImage().getWidth();
			}
		@Override
		public int getHeight() {
			return getImage().getHeight();
			}
		private Canvas shape(Shape shape,FunctionalMap<String, Object> props) {
			return this;
			}

		@Override
		public Canvas circle(double cx, double cy, double r, FunctionalMap<String, Object> props) {
			return shape(new java.awt.geom.Ellipse2D.Double(cx-r,cy-r,r*2,r*2),props);
			}
		
		@Override
		public Canvas line(double x1,double y1,double x2,double y2,FunctionalMap<String, Object> props) {
			return shape(new Line2D.Double(x1,y1,x2,y2),props);
			}

		@Override
		public Canvas polygon(double[] x, double[] y, FunctionalMap<String, Object> props) {
			final GeneralPath g =new GeneralPath();
			for(int i=0;i< x.length;++i) {
				if(i==0) {
					g.moveTo(x[i], y[i]);
					}
				else
					{
					g.lineTo(x[i], y[i]);
					}
				}
			g.closePath();
			return shape(g,props);
			}
		@Override
		public void close()  throws IOException {
			this.g2d.dispose();
			ImageIO.write(this.image, "", this.outputFile);
			}
		}
	
	private static class PSCanvas extends Canvas {
		final int width;
		final int height;
		final PrintWriter out;
		PSCanvas(int width,int height) {
			this.width=width;
			this.height=height;
			this.out=new PrintWriter(new StringWriter());
			this.out.println("%PS");
			}
		@Override
		public void close() throws IOException {
			this.out.println(" showpage");
			this.out.println("%EOF");
			this.out.flush();
			this.out.close();
			}
		@Override
		public int getWidth() {
			return this.width;
			}
		@Override
		public int getHeight() {
			return this.height;
			}
		String coord(double x,double y) {
			return String.valueOf(x)+" "+String.valueOf(y);
			}
		
		@Override
		public Canvas circle(double cx, double cy, double r, FunctionalMap<String, Object> props) {
			out.append(" newpath");
			out.append(" ");
			out.append(coord(cx,cy));
			out.append(" ");
			out.append(String.valueOf(r));
			out.append(" 0 360 arc");
			out.append(" closepath");
			return this;
			}
		
		@Override
		public Canvas line(double x1, double y1, double x2, double y2, FunctionalMap<String, Object> props) {
			return polygon(new double[] {x1,x2},new double[] {x1,y2},props);
			}
		@Override
		public Canvas polygon(double[] x, double[] y, FunctionalMap<String, Object> props) {
			out.append(" newpath");
			for(int i=0;i< x.length;++i) {
				out.append(" ");
				out.append(coord(x[i],y[i]));
				out.append(" ");
				out.append(i==0?"moveto":"lineto");
				}
			out.append(" closepath");
			return this;
			}
		
		}
	private static class DOMCanvas extends Canvas {
		final int width;
		final int height;
		XMLStreamWriter w;
		DOMCanvas(int width,int height) throws IOException {
			this.width=width;
			this.height=height;
			try {
				w.writeStartDocument("UTF-8", "1.0");
				w.writeStartElement(SVG.NS, "svg");
				w.writeAttribute("width", String.valueOf(width));
				w.writeAttribute("height", String.valueOf(height));
				w.writeStartElement(SVG.NS, "g");
				} catch(XMLStreamException err) {
					throw new IOException(err);
				}
			}
		
		@Override
		public void close() throws IOException {
			try {
				w.writeEndElement();
				w.writeEndElement();
				w.writeEndDocument();
				w.flush();
				}
			catch(XMLStreamException err) {
				throw new IOException(err);
				}
			}	
		
		@Override
		public int getWidth() {
			return this.width;
			}
		@Override
		public int getHeight() {
			return this.height;
			}

		private void beginWrap(FunctionalMap<String, Object> props) throws XMLStreamException {
			final String href = props.getOrDefault("href","").toString();
			if(!StringUtils.isBlank(href)) {
				w.writeStartElement(SVG.NS, "a");
				w.writeAttribute("href", href);
				}
			}
		private void inner(FunctionalMap<String, Object> props) throws XMLStreamException {
			final Object t = props.getOrDefault("title","");
			final String s = t==null?"":t.toString();
			if(!StringUtils.isBlank(s)) {
				w.writeStartElement(SVG.NS, "title");
				w.writeCharacters(s);
				w.writeEndElement();
				}
			}
		private Canvas endWrap(FunctionalMap<String, Object> props) throws XMLStreamException {
			final String href = props.getOrDefault("href","").toString();
			if(!StringUtils.isBlank(href)) {
				w.writeEndElement();
				}
			return this;
			}
		
		private String toString(Object o) {
			return o.toString();
			}
		
		private void style(FunctionalMap<String, Object> props) throws XMLStreamException {
			String css=props.
					minus("title","href").
					stream().
					map(KV->KV.getKey()+":"+KV.getValue()).
					collect(Collectors.joining(";"));
			if(!StringUtils.isBlank(css)) {
				w.writeAttribute("style", css);
				}
			}
		
		@Override
		public Canvas circle(double cx, double cy, double r, FunctionalMap<String, Object> props) {
			try {
				beginWrap(props);
				w.writeStartElement(SVG.NS, "circle");
				w.writeAttribute("cx", String.valueOf(cx));
				w.writeAttribute("cy", String.valueOf(cy));
				w.writeAttribute("r", String.valueOf(r));
				style(props);
				inner(props);
				w.writeEndElement();
				return endWrap(props);
				}
			catch(XMLStreamException err) {
				throw new RuntimeException(err);
				}
			}
		@Override
		public Canvas line(double x1, double y1, double x2, double y2, FunctionalMap<String, Object> props) {
			try {
				beginWrap(props);
				w.writeStartElement(SVG.NS, "line");
				w.writeAttribute("x1", String.valueOf(x1));
				w.writeAttribute("y1", String.valueOf(y1));
				w.writeAttribute("x2", String.valueOf(x2));
				w.writeAttribute("y2", String.valueOf(y2));
				inner(props.minus("fill"));
				w.writeEndElement();
				return endWrap(props);
				}
			catch(XMLStreamException err) {
				throw new RuntimeException(err);
				}
			}
		@Override
		public Canvas polygon(double[] x, double[] y, FunctionalMap<String, Object> props) {
			try {
				beginWrap(props);
				w.writeStartElement(SVG.NS, "shape");
				w.writeAttribute("p", String.valueOf(""));
				inner(props);
				w.writeEndElement();
				return endWrap(props);
				}
			catch(XMLStreamException err) {
				throw new RuntimeException(err);
				}
			}
		}
}
