package sandbox.tools.comicsbuilder;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Arrays;

import sandbox.swing.DrawingArea;

public class Test extends DrawingArea {
	
public interface Selectable {
}
	
public interface Drag extends Selectable {
	double getX();
	double getY();
	void moveTo(double x,double y);
	}
public class Rect implements Selectable {
	class MyDrag implements Drag {
		double x;
		double y;
		MyDrag(double x,double y) {
			this.x = x;
			this.y = y;
			}
		@Override
		public double getX() {
			return x;
			}
		@Override
		public double getY() {
			return y;
			}
		@Override
		public void moveTo(double x, double y) {
			this.x = x;
			this.y = y;
			}
		}
	private final MyDrag[] drags = new MyDrag[4];
	Rect(final Rectangle2D rect) {
		drags[0] = new MyDrag(rect.getX(), rect.getY());
		drags[1] = new MyDrag(rect.getMaxX(), rect.getY());
		drags[2] = new MyDrag(rect.getMaxX(), rect.getMaxY());
		drags[3] = new MyDrag(rect.getX(), rect.getMaxY());
		}
	public double getX() {
		return Arrays.stream(drags).mapToDouble(P->P.getX()).min().getAsDouble();
		}
	public double getY() {
		return Arrays.stream(drags).mapToDouble(P->P.getY()).min().getAsDouble();
		}
	public double getWidth() {
		double x0 = getX();
		double x1 = Arrays.stream(drags).mapToDouble(P->P.getX()).max().getAsDouble();
		return x1 - x0;
		}
	public double getHeight() {
		double y0 = getY();
		double y1 = Arrays.stream(drags).mapToDouble(P->P.getY()).max().getAsDouble();
		return y1 - y0;
		}
	}
public interface Layer {
	public boolean isVisible();
	public boolean isLocked();
	}
private class Selection extends  sandbox.swing.collections.ObservableList<Object> {
	
	}
private Selection selection = new Selection();

public Test() {
	final MouseAdapter m = new MouseAdapter() {
		Point mouseStart = null;
		Point mousePrev = null;
		@Override
		public void mousePressed(MouseEvent e) {
			mouseStart  = new Point(e.getX(),e.getY());
			mousePrev  = new Point(e.getX(),e.getY());
			}
		@Override
		public void mouseDragged(MouseEvent e) {
			int dx = e.getX() - mousePrev.x;
			int dy = e.getY() - mousePrev.y;
			
			
			mousePrev.x =  e.getX() ;
			mousePrev.y =  e.getY() ;
			}
		@Override
		public void mouseReleased(MouseEvent e) {
			
			}
		};
	addMouseListener(m);
	addMouseMotionListener(m);
	addMouseWheelListener(m);
	}
@Override
public void paintDrawingArea(Graphics2D g) {
	g.setColor(Color.WHITE);
	g.fillRect(0, 0, getWidth(), getHeight());
	paintLayers(g);
	}
public void paintLayers(Graphics2D g) {
	
	}
}
