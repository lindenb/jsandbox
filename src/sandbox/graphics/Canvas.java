package sandbox.graphics;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.imageio.ImageIO;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;


import sandbox.StringUtils;
import sandbox.io.IOUtils;
import sandbox.svg.SVG;
import sandbox.util.function.FunctionalMap;

/**
 * Multiple format drawing class
 *
 */
public abstract class Canvas implements AutoCloseable {
	
	public abstract int getWidth();
	public abstract int getHeight();
	
	protected Canvas() {
		}
	protected static int toInt(Object o) {
		Objects.requireNonNull(o, "object is null");
		if(o instanceof Number) {
			return Number.class.cast(o).intValue();
			}
		if(o instanceof String) {
			return Integer.parseInt(String.class.cast(o));
			}
		throw new IllegalArgumentException("cannot cast object "+o.getClass()+" as int");
		}
	
	public static Canvas open(Path output,FunctionalMap<String, Object> props) throws IOException {
		int width = toInt(props.getOrDefault("width", 500));
		int height = toInt(props.getOrDefault("height", 500));
		if(output==null) {
			return new PSCanvas(null, width, height);
			}
		final String extension  = IOUtils.getFileSuffix(output).toLowerCase();
		if(extension.equals(".png") || extension.equals(".jpg") || extension.equals(".jpeg")) {
			return new Graphics2DCanvas(output.toFile(), width, height, BufferedImage.TYPE_INT_RGB);
			}
		
		if(extension.equals(".svg") || output.toString().toLowerCase().endsWith(".svg.gz")) {
			return new SVGCanvas(output, width, height);
			}
		if(extension.equals(".ps")) {
			return new PSCanvas(output, width, height);
			}
		throw new IllegalArgumentException("unknown extension:"+output);
		}
	
	public abstract Canvas line(double x1,double y1,double x2,double y2,FunctionalMap<String, Object> props);
	public abstract Canvas polygon(double[] x,double[] y,FunctionalMap<String, Object> props);
	public abstract Canvas circle(double cx,double cy,double r,FunctionalMap<String, Object> props);
	public abstract Canvas draw(Shape shape,FunctionalMap<String, Object> props);

	
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
		public Canvas draw(Shape shape, FunctionalMap<String, Object> props) {
			return shape(shape,props);
			}
		@Override
		public void close()  throws IOException {
			this.g2d.dispose();
			if(this.outputFile==null) {
				ImageIO.write(this.image, "JPG",System.out);
				}
			else
				{
				final String format= this.outputFile.getName().toLowerCase().endsWith(".png")?"PNG":"JPG";
				ImageIO.write(this.image, format, this.outputFile);
				}
			}
		}
	
	private static class PSCanvas extends Canvas {
		final int width;
		final int height;
		final PrintWriter out;
		PSCanvas(Path outputOrNull,int width,int height) throws IOException{
			this.width=width;
			this.height=height;
			this.out= outputOrNull==null?
					new PrintWriter(System.out):
					new PrintWriter(Files.newBufferedWriter(outputOrNull))
					;
			this.out.println("%PS");
			this.out.println("%%BoundingBox: 0 0 "+width+" "+height);
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
		@Override
		public Canvas draw(Shape shape,FunctionalMap<String, Object> props) {
			float coords[]=new float[6];
			out.append(" newpath");
			PathIterator iter = shape.getPathIterator(null);
			while(!iter.isDone()) {
				switch(iter.currentSegment(coords))
				{
				case PathIterator.SEG_MOVETO:
					{
					out.append(" ");
					out.append(coord(coords[0],coords[1]));
					out.append(" moveto");
					break;
					}
				case PathIterator.SEG_LINETO:
					{
					out.append(" ");
					out.append(coord(coords[0],coords[1]));
					out.append(" lineto");
					break;
					}
				case PathIterator.SEG_QUADTO:
					{
					//TODO
					break;
					}
				case PathIterator.SEG_CUBICTO:
					{
					out.append(" ");
					out.append(coord(coords[0],coords[1]));
					out.append(" ");
					out.append(coord(coords[2],coords[3]));
					out.append(" ");
					out.append(coord(coords[4],coords[5]));
					out.append(" curveto");
					break;
					}
				case PathIterator.SEG_CLOSE:
					{
					out.append(" closepath");
					break;
					}
				}
			
			iter.next();
			}
			return this;
			}
		}
	private static class SVGCanvas extends Canvas {
		final int width;
		final int height;
		OutputStream outputStream;
		XMLStreamWriter w;
		SVGCanvas(final Path outputOrNull,int width,int height) throws IOException {
			this.width=width;
			this.height=height;
			try {
				final XMLOutputFactory xof = XMLOutputFactory.newFactory();
				this.outputStream = outputOrNull==null?
						System.out:
						IOUtils.openPathAsOutputStream(outputOrNull)
						;
				w=xof.createXMLStreamWriter(this.outputStream, "UTF-8");
				w.writeStartDocument("UTF-8", "1.0");
				w.writeStartElement("svg");
				w.writeDefaultNamespace(SVG.NS);
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
				w.writeEndElement();//g
				w.writeEndElement();//svg
				w.writeEndDocument();// document
				w.flush();
				outputStream.flush();
				outputStream.close();
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
				w.writeStartElement( "a");
				w.writeAttribute("href", href);
				}
			}
		private void inner(FunctionalMap<String, Object> props) throws XMLStreamException {
			final Object t = props.getOrDefault("title","");
			final String s = t==null?"":toString(t);
			if(!StringUtils.isBlank(s)) {
				w.writeStartElement("title");
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
		public Canvas rectangle(double x, double y, double width, double height, FunctionalMap<String, Object> props) {
			try {
				beginWrap(props);
				w.writeStartElement("rect");
				w.writeAttribute("x", String.valueOf(x));
				w.writeAttribute("x", String.valueOf(y));
				w.writeAttribute("width", String.valueOf(width));
				w.writeAttribute("height", String.valueOf(height));
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
		public Canvas circle(double cx, double cy, double r, FunctionalMap<String, Object> props) {
			try {
				beginWrap(props);
				w.writeStartElement("circle");
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
				w.writeStartElement("line");
				w.writeAttribute("x1", String.valueOf(x1));
				w.writeAttribute("y1", String.valueOf(y1));
				w.writeAttribute("x2", String.valueOf(x2));
				w.writeAttribute("y2", String.valueOf(y2));
				style(props);
				inner(props.minus("fill"));
				w.writeEndElement();
				return endWrap(props);
				}
			catch(XMLStreamException err) {
				throw new RuntimeException(err);
				}
			}
		@Override
		public Canvas polygon(final double[] x, final double[] y, FunctionalMap<String, Object> props) {
			try {
				beginWrap(props);
				w.writeStartElement("polygon");
				w.writeAttribute("points",
						IntStream.rangeClosed(0, x.length-1).
							mapToObj(IDX->String.valueOf(x[IDX])+" "+
						    String.valueOf(y[IDX])).collect(Collectors.joining(" "))
						);
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
		public Canvas draw(Shape shape, FunctionalMap<String, Object> props) {
			try {
				final StringBuilder path = new StringBuilder();
				double tab[] = new double[6];
				PathIterator pathiterator = shape.getPathIterator(null);

				while(!pathiterator.isDone())
					{
						int currSegmentType= pathiterator.currentSegment(tab);
						switch(currSegmentType) {
						case PathIterator.SEG_MOVETO: {
							path.append( "M " + (tab[0]) + " " + (tab[1]) + " ");
							break;
						}
						case PathIterator.SEG_LINETO: {
							path.append( "L " + (tab[0]) + " " + (tab[1]) + " ");
							break;
						}
						case PathIterator.SEG_CLOSE: {
							path.append( "Z ");
							break;
						}
						case PathIterator.SEG_QUADTO: {
							path.append( "Q " + (tab[0]) + " " + (tab[1]));
							path.append( " "  + (tab[2]) + " " + (tab[3]));
							path.append( " ");
							break;
						}
						case PathIterator.SEG_CUBICTO: {
							path.append( "C " + (tab[0]) + " " + (tab[1]));
							path.append( " "  + (tab[2]) + " " + (tab[3]));
							path.append( " "  + (tab[4]) + " " + (tab[5]));
							path.append( " ");
							break;
						}
						default:
						{
							throw new IllegalStateException("Cannot handled "+currSegmentType);
						}
						}
						pathiterator.next();
					}
				beginWrap(props);
				w.writeStartElement("path");
				w.writeAttribute("d",  path.toString());
				style(props);
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
