package sandbox.drawing;

import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.PathIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WiggleStroke implements Stroke {
	double flatness=2;
	double handDistance = 20;
	double precision = 5;
	private final Stroke delegate;
	private boolean disableQuad = false;
	
	public WiggleStroke(final Stroke delegate) {
		this.delegate  = delegate;
		}
	/** flatness for getPathIterator */
	public WiggleStroke setFlatness(double flatness) {
		this.flatness = flatness;
		return this;
		}
	
	public WiggleStroke setHandDistance(double handDistance) {
		this.handDistance = handDistance;
		return this;
		}
	
	public WiggleStroke setPrecision(double precision) {
		this.precision = precision;
		return this;
		}
	
	public WiggleStroke setDisableQuad(boolean disableQuad) {
		this.disableQuad = disableQuad;
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
	private double delta(final Random r) {
		return this.precision * r.nextDouble() * (r.nextBoolean()?-1.0:1.0);
		}
	
	protected void lineTo(
			final Random random,
			final GeneralPath path,
			double x1, double y1,
			double x2, double y2
			) {
		final List<Double> coords = new ArrayList<>(); 
		for(;;) {
			final double distance = Point.distance(x1, y1, x2, y2) +delta(random);
			double nPt = distance/ this.handDistance ;
			if(nPt<=1) {
				coords.add(x2);
				coords.add(y2);
				break;
				}
			
			double dx = (x2-x1)/nPt;
			double dy = (y2-y1)/nPt;
			double x3 = x1 + dx + delta(random) ;
			double y3 = y1 + dy + delta(random) ;
			coords.add(x3);
			coords.add(y3);
			x1 = x3;
			y1 = y3;
			}
		
		int i=0;
		while(i+1<coords.size()) {
			if(!this.disableQuad && i+4 < coords.size()) {
				path.quadTo(
						coords.get(i+0),
						coords.get(i+1),
						coords.get(i+2),
						coords.get(i+4)
						);
				i+=4;
			} else
			{
				path.lineTo(coords.get(i+0),coords.get(i+1));
				i+=2;
			}
			
		}
		
		}
	}
