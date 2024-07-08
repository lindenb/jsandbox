package sandbox.graphics;

import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.GeneralPath;
import java.awt.image.BufferedImage;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

import javax.xml.namespace.QName;

import sandbox.svg.SVG;
import sandbox.xml.minidom.Element;

public abstract class Canvas implements AutoCloseable {
	public enum Key {
		fill,
		stroke,
		title,
		href
		};
	public abstract int getWidth();
	public abstract int getHeight();
	
	public abstract void polygon(double[] x,double[] y,Map<Key, Object> props);

	
	public void rectangle(double x,double y, double width,double height,Map<Key, Object> props) {
		double[] ax = new double[] {x,x+width,x+width,x};
		double[] ay = new double[] {y,y,y+height,y+height};
		polygon(ax,ay,props);
		}
	
	public static Canvas createGraphics2DCanvas(int width,int height,int imgType) {
		return new Graphics2DCanvas(width,height,imgType);
		}
	@Override
	public void close()  {
		
		}
	
	private static class Graphics2DCanvas extends Canvas {
		final BufferedImage image;
		final Graphics2D g2d;
		Graphics2DCanvas(int width,int height,int imgType) {
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
		private void shape(Shape shape,Map<Key, Object> props) {
			
			}
		@Override
		public void polygon(double[] x, double[] y, Map<Key, Object> props) {
			GeneralPath g =new GeneralPath();
			for(int i=0;i< x.length;++i) {
				if(i==0) {
					g.moveTo(x[i], y[i]);
					}
				else
					{
					g.lineTo(x[i], y[i]);
					}
				}
			shape(g,props);
			}
		@Override
		public void close() {
			this.g2d.dispose();
			
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
			}
		protected PrintWriter getWriter() {
			return out;
			}
		@Override
		public int getWidth() {
			return this.width;
			}
		@Override
		public int getHeight() {
			return this.height;
			}
		String coord(double v) {
			return String.valueOf(v);
			}
		@Override
		public void polygon(double[] x, double[] y, Map<Key, Object> props) {
			// TODO Auto-generated method stub
			
			}
		}
	private static class DOMCanvas extends Canvas {
		final int width;
		final int height;
		Element svgdoc;
		Element svgRoot;
		DOMCanvas(int width,int height) {
			this.width=width;
			this.height=height;
			
			}
		
		@Override
		public int getWidth() {
			return this.width;
			}
		@Override
		public int getHeight() {
			return this.height;
			}

		private Element wrap(Element e,Map<Key, Object> props) {
			return e;
			}
		private QName qName(final String lcl) {
			return new QName(SVG.NS, lcl);
			}
		@Override
		public void polygon(double[] x, double[] y, Map<Key, Object> props) {
			Element e=new Element(qName("shape"));
			e.setAttribute("p", e);
			e=wrap(e,props);
			}
		}
}
