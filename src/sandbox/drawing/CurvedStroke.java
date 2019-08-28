package sandbox.drawing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.Arrays;
import java.util.Random;
import java.util.function.DoubleUnaryOperator;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

public class CurvedStroke implements Stroke {
	private double flatness=2;
	private final Stroke delegate;
	private DoubleUnaryOperator quadDistance = D->Math.min(D/10.0,5);
	public CurvedStroke(final Stroke delegate) {
		this.delegate  = delegate;
		}
	
	/** flatness for getPathIterator */
	public CurvedStroke setFlatness(double flatness) {
		this.flatness = flatness;
		return this;
		}
		
	@Override
	public Shape createStrokedShape(final Shape p) {
		return createStrokedShape(p,null);
		}

	public Shape createStrokedShape(final Shape p,final AffineTransform tr) {
		final Point pt = p.getBounds().getLocation();
		final Random random = new Random(pt.x*31 + pt.y);
		final double coords[] = new double[6];
		final GeneralPath path = new GeneralPath(GeneralPath.WIND_NON_ZERO);
		final PathIterator iter= p.getPathIterator(tr, flatness);
		double prev_x = 0;
		double prev_y = 0;
		while(!iter.isDone()) {
			final int t=iter.currentSegment(coords);
			switch(t) {
				case PathIterator.SEG_LINETO:
					lineTo(random,path,prev_x,prev_y,coords[0], coords[1]);
					//path.lineTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_MOVETO:
					path.moveTo(coords[0], coords[1]);
					break;
				case PathIterator.SEG_CLOSE:
					path.closePath();
					break;
				default:
					break;
				}
			prev_x = coords[0];
			prev_y = coords[1];
			iter.next();
			}
		if(delegate!=null) return delegate.createStrokedShape(path);
		return path;
		}
	
	protected void lineTo(
			final Random random,
			final GeneralPath path,
			double x1, double y1,
			double x2, double y2
			) {
		final double distance = Point.distance(x1, y1, x2, y2);
		if(distance<=0) {
			path.lineTo(x2, y2);
			return;
			}
		double radian = Math.asin((x2-x1)/distance);
		if(Double.isNaN(radian)) {
			path.lineTo(x2, y2);
			return;
			}
		final double dx = (x2-x1)/distance;
		final double dy = (y2-y1)/distance; 

		if(random.nextBoolean() || dx!=0) {		
			final double mid= distance * random.nextDouble();
			
			double pivot_x= x1 + dx*mid;
			double pivot_y= y1 + dy*mid;
	
			//radian += Math.PI/2.0;
			double qd = quadDistance.applyAsDouble(distance);
	
			if(random.nextBoolean()) radian+=Math.PI;
			double x3= pivot_x + Math.cos(radian)*qd;
			double y3= pivot_y + Math.sin(radian)*qd;
			
			
			
			path.quadTo(x3, y3, x2, y2);
			}
		else
			{
			double mids[]= new double[2];
			Point2D.Double pivots[]= new Point2D.Double[2];
			mids[0]=distance * random.nextDouble();
			mids[1]=distance * random.nextDouble();
			Arrays.sort(mids);
			
			for(int i=0;i< 2;i++) {
				double pivot_x= x1 + dx*mids[i];
				double pivot_y= y1 + dy*mids[i];
		
				//radian += Math.PI/2.0;
				double qd = quadDistance.applyAsDouble(distance);
		
				radian+=Math.PI;
				double x3= pivot_x + Math.cos(radian)*qd;
				double y3= pivot_y + Math.sin(radian)*qd;
				pivots[i]=new Point2D.Double(x3, y3);
				}
			path.curveTo(pivots[0].x,pivots[0].y,pivots[1].x,pivots[1].y,x2,y2);
			}
		}
	
public static void main(String[] args) {
	JPanel pane = new JPanel(null) {
		@Override
		protected void paintComponent(Graphics g1) {
			super.paintComponent(g1);
			
			Graphics2D g= Graphics2D.class.cast(g1);
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setStroke(new CurvedStroke(g.getStroke()));
			
			for(int x=0;x< 500;x+=25) g.drawRect(x/50,x, 50+x, 50);
			}
		};
	pane.setOpaque(true);
	pane.setBackground(Color.WHITE);
	pane.setPreferredSize(new Dimension(600, 600));
	JOptionPane.showMessageDialog(null, pane);
	}
	
	
}
