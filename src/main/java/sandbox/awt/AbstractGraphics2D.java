package sandbox.awt;

import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.RenderingHints.Key;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.Map;

public abstract class AbstractGraphics2D extends Graphics2D {
	private final BufferedImage onePix;
	private final Graphics2D delegate;

	
	public AbstractGraphics2D() {
		this.onePix = new BufferedImage(1, 1, BufferedImage.TYPE_INT_RGB);
		this.delegate = onePix.createGraphics();
		}
	
	public Graphics2D getDelegate() {
		return this.delegate;
		}
	
	@Override
	public void draw(Shape shape) {
		paintShape(shape,false);
	}

	@Override
	public boolean drawImage(Image img, AffineTransform xform, ImageObserver obs) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void drawImage(BufferedImage img, BufferedImageOp op, int x, int y) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void drawRenderedImage(RenderedImage img, AffineTransform xform) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public void drawRenderableImage(RenderableImage img, AffineTransform xform) {
		throw new UnsupportedOperationException();		
	}

	@Override
	public void drawString(String str, int x, int y) {
		drawString(str,(float)x,(float)y);
	}

	@Override
	public void drawString(String str, float x, float y) {
		FontRenderContext frc = this.getFontRenderContext();
	    TextLayout tl = new TextLayout(str, getFont(), frc);
	    draw(tl.getOutline(null));
	}

	@Override
	public void drawString(AttributedCharacterIterator iterator, int x, int y) {
		drawString(iterator,(float)x,(float)y);
		
	}

	@Override
	public void drawString(AttributedCharacterIterator iterator, float x, float y) {
		
	}

	@Override
	public void drawGlyphVector(GlyphVector g, float x, float y) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void fill(Shape s) {
		paintShape(s,true);
	}

	@Override
	public boolean hit(Rectangle rect, Shape s, boolean onStroke) {
		throw new UnsupportedOperationException();
	}

	@Override
	public GraphicsConfiguration getDeviceConfiguration() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setComposite(final Composite comp) {
		getDelegate().setComposite(comp);
	}

	@Override
	public void setPaint(Paint paint) {
		getDelegate().setPaint(paint);
	}

	@Override
	public void setStroke(Stroke s) {
		getDelegate().setStroke(s);
	}

	@Override
	public void setRenderingHint(Key hintKey, Object hintValue) {
		getDelegate().setRenderingHint(hintKey, hintValue);
		
	}

	@Override
	public Object getRenderingHint(Key hintKey) {
		return this.getDelegate().getRenderingHint(hintKey);
	}

	@Override
	public void setRenderingHints(Map<?, ?> hints) {
		getDelegate().setRenderingHints(hints);
	}

	@Override
	public void addRenderingHints(Map<?, ?> hints) {
		getDelegate().addRenderingHints(hints);	
	}

	@Override
	public RenderingHints getRenderingHints() {
		return getDelegate().getRenderingHints();
	}

	@Override
	public void translate(int x, int y) {
		translate((double)x,(double)y);
	}

	@Override
	public void translate(double tx, double ty) {
		getDelegate().translate(tx,ty);	
	}

	@Override
	public void rotate(double theta) {
		getDelegate().rotate(theta);
	}

	@Override
	public void rotate(double theta, double x, double y) {
		getDelegate().rotate(theta,x,y);
	}

	@Override
	public void scale(double sx, double sy) {
		getDelegate().scale(sx,sy);
		}

	@Override
	public void shear(double shx, double shy) {
		getDelegate().shear(shx,shy);
	}

	@Override
	public void transform(AffineTransform Tx) {
		getDelegate().transform(Tx);					
	}

	@Override
	public void setTransform(AffineTransform Tx) {
		getDelegate().setTransform(Tx);
	}

	@Override
	public AffineTransform getTransform() {
		return getDelegate().getTransform();
	}

	@Override
	public Paint getPaint() {
		return getDelegate().getPaint();
	}

	@Override
	public Composite getComposite() {
		return getDelegate().getComposite();
	}

	@Override
	public void setBackground(Color color) {
		getDelegate().setBackground(color);
	}

	@Override
	public Color getBackground() {
		return getDelegate().getBackground();
	}

	@Override
	public Stroke getStroke() {
		return getDelegate().getStroke();
	}

	@Override
	public void clip(Shape s) {
		getDelegate().clip(s);
		
	}

	@Override
	public FontRenderContext getFontRenderContext() {
		return getDelegate().getFontRenderContext();
	}

	@Override
	public Graphics create() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Color getColor() {
		return getDelegate().getColor();
	}

	@Override
	public void setColor(Color c) {
		getDelegate().setColor(c);
	}

	@Override
	public void setPaintMode() {
		getDelegate().setPaintMode();
	}

	@Override
	public void setXORMode(Color c1) {
		getDelegate().setXORMode(c1);
	}

	@Override
	public Font getFont() {
		return getDelegate().getFont();
	}

	@Override
	public void setFont(Font font) {
		getDelegate().setFont(font);
	}

	@Override
	public FontMetrics getFontMetrics(Font f) {
		return getDelegate().getFontMetrics(f);
	}

	@Override
	public Rectangle getClipBounds() {
		return getDelegate().getClipBounds();
	}

	@Override
	public final void clipRect(int x, int y, int width, int height) {
		clip(new Rectangle(x,y,width,height));
		
	}

	@Override
	public void setClip(int x, int y, int width, int height) {
		clip(new Rectangle(x,y,width,height));		
	}

	@Override
	public Shape getClip() {
		return getDelegate().getClip();
	}

	@Override
	public void setClip(Shape clip) {
		getDelegate().setClip(clip);
	}

	@Override
	public void copyArea(int x, int y, int width, int height, int dx, int dy) {
		throw new UnsupportedOperationException();
	}

	@Override
	public final void drawLine(int x1, int y1, int x2, int y2) {
        draw(new Line2D.Double(x1, y1, x2, y2));
	}

	@Override
	public final void fillRect(int x, int y, int width, int height) {
        fill(new Rectangle(x, y, width, height));		
	}

	@Override
	public void clearRect(int x, int y, int width, int height) {
		final Paint paint = this.getPaint();
        this.setColor(this.getBackground());
        fillRect(x, y, width, height);
        this.setPaint(paint);		
	}

	@Override
	public final void drawRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        draw(new RoundRectangle2D.Double(x,y,width,height,arcWidth, arcHeight));
	}

	@Override
	public final void fillRoundRect(int x, int y, int width, int height, int arcWidth, int arcHeight) {
        fill(new RoundRectangle2D.Double(x,y,width,height,arcWidth, arcHeight));
	}

	@Override
	public final void drawOval(int x, int y, int width, int height) {
        draw(new Ellipse2D.Float(x, y, width, height));		
	}

	@Override
	public final void fillOval(int x, int y, int width, int height) {
        fill(new Ellipse2D.Float(x, y, width, height));
        }

	@Override
	public final void drawArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        draw(new Arc2D.Float(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN));		
	}

	@Override
	public final void fillArc(int x, int y, int width, int height, int startAngle, int arcAngle) {
        fill(new Arc2D.Float(x, y, width, height, startAngle, arcAngle, Arc2D.OPEN));		
	}

	@Override
	public final void drawPolyline(int[] xPoints, int[] yPoints, int nPoints) {
		if(nPoints<1) return;
		final GeneralPath path=new GeneralPath();
		path.moveTo(xPoints[0], yPoints[0]);
		for(int i=1;i<nPoints;i++) {
			path.lineTo(xPoints[i], yPoints[i]);
			}
		draw(path);
	}

	@Override
	public final void drawPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        draw(new Polygon(xPoints, yPoints, nPoints));		
	}

	@Override
	public final void fillPolygon(int[] xPoints, int[] yPoints, int nPoints) {
        fill(new Polygon(xPoints, yPoints, nPoints));
	}

	@Override
	public final boolean drawImage(Image img, int x, int y, ImageObserver observer) {
		return drawImage(img,x,y,img.getWidth(observer),img.getHeight(observer),observer);
	}

	@Override
	public final boolean drawImage(Image img, int x, int y, int width, int height, ImageObserver observer) {
		return drawImage(img,x,y,width,height,getBackground(),observer);
	}

	@Override
	public boolean drawImage(Image img, int x, int y, Color bgcolor, ImageObserver observer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean drawImage(Image img, int x, int y, int width, int height, Color bgcolor, ImageObserver observer) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
			ImageObserver observer) {
		return drawImage(img,dx1,dy1,dx2,dy2,sx1,sy1,sx2,sy2,getBackground(),observer);
	}

	@Override
	public boolean drawImage(Image img, int dx1, int dy1, int dx2, int dy2, int sx1, int sy1, int sx2, int sy2,
			Color bgcolor, ImageObserver observer) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void dispose() {
	}
	
	private void paintShape(Shape shape,boolean filled) {
		paintTransformedShape(getTransform().createTransformedShape(shape), filled);
		}
	protected abstract void paintTransformedShape(Shape shape,boolean filled);	
}
