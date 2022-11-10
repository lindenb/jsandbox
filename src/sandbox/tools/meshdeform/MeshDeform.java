package sandbox.tools.meshdeform;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

// https://github.com/dhotson/springy/blob/master/springy.js
@SuppressWarnings("serial")
public class MeshDeform extends JFrame
	{
	private final JPanel drawingArea;
	private final List<Mesh> array = new ArrayList<>();
	private Mesh selectMesh = null;
	private int n_split = 10;
	
	private static class Vect {
		double x;
		double y;
		Vect(double x,double y) {
			this.x = x;
			this.y = y;
			}
		Vect add(Vect v) { return new Vect(this.x+v.x,this.y+v.y);}
		Vect substract(Vect v) { return new Vect(this.x-v.x,this.y-v.y);}
		Vect multiply(double f) { return new Vect(this.x*f,this.y*f);}
		Vect divide(double f) { return new Vect((f==0?0:this.x/f),(f==0?0:this.y/f));}
		double magnitude() { return Math.sqrt(this.x*this.x + this.y*this.y);}
		Vect normal() { return new Vect(-this.y, this.x);}
		Vect normalise() { return this.divide(this.magnitude());}
		}
	
	private class Mesh {
		int gridx;
		int gridy;
		double fx;
		double fy;
		double prevFx;
		double prevFy;
		Vect velocity = new Vect(0, 0); // velocity
		Vect acceleration = new Vect(0, 0); // acceleration
		public double getX() {
			return fx * drawingArea.getWidth();
			}
		public double getY() {
			return fy * drawingArea.getHeight();
			}
		public void applyForce(Vect force) {
			this.acceleration = this.acceleration.add(force);
			}
		
		
		public List<Mesh> getNeighbours() {
			final List<Mesh> L = new ArrayList<>();
			for(int x=-1;x<=1;++x) {
				for(int y=-1;y<=1;++y) {
					if(x==0 && y==0) continue;
					Mesh n = getMesh(this.gridx+x,this.gridy+y);
					if(n!=null) L.add(n);
					}
				}
			return L;
			}
		
		void recursive(double nx,double ny,double factor) {
			if(visitedFlag) return;
			visitedFlag=true;
			this.prevFx = this.fx;
			this.prevFy = this.fy;
			this.fx=nx;
			this.fy=ny;
			//visit neighbours
			for(int x=-1;x<=1;++x) {
				for(int y=-1;y<=1;++y) {
					if(x==0 && y==0) continue;
					Mesh n = getMesh(this.gridx+x,this.gridy+y);
					if(n==null || n.visitedFlag) continue;
					double prevDistance = Point2D.distance(this.prevFx, this.prevFy,n.prevFx,n.prevFy); 
					double newDistance = Point2D.distance(nx, ny,n.prevFx,n.prevFy);
					double f=factor;
					if(prevDistance>newDistance) {
						f=1.0-f;
						}
					
					double n2x = this.fx + (n.prevFx-this.fx)*f;
					double n2y = this.fy + (n.prevFy-this.fy)*f;
					n.fx = n2x;
					n.fy = n2y;
					//n.recursive(n2x,n2y,factor*0.999);
					}
				}
			
			}
		}
	
	
	
	MeshDeform() {
		this.drawingArea = new JPanel(null) {
			@Override
			protected void paintComponent(Graphics g)
				{
				paintDrawingArea(Graphics2D.class.cast(g),drawingArea.getWidth(),drawingArea.getHeight());
				}
			};
		this.drawingArea.setDoubleBuffered(true);
		this.drawingArea.setOpaque(true);
		final MouseAdapter mouseAdapter = new MouseAdapter()
			{
			@Override
			public void mousePressed(final MouseEvent e)
				{
				selectMesh = findMeshAt(e.getX(), e.getY());
				for(Mesh m:array) {
					m.prevFx = m.fx;
					m.prevFy = m.fy;
					}
				}
			@Override
			public void mouseDragged(final MouseEvent e)
				{
				if(selectMesh!=null) {
					for(Mesh m:array) {
						m.prevFx = m.fx;
						m.prevFy = m.fy;
						m.visitedFlag=false;
						}
					double nx = (e.getX()/(double)drawingArea.getWidth());
					double ny = (e.getY()/(double)drawingArea.getHeight());
					selectMesh.recursive(nx,ny,0.9);
					drawingArea.repaint();
					}
				}
			@Override
			public void mouseReleased(MouseEvent e)
				{
				if(selectMesh!=null) {
					drawingArea.repaint();
				}
				selectMesh=null;
				}
			};
		drawingArea.addMouseListener(mouseAdapter);
		drawingArea.addMouseMotionListener(mouseAdapter);
		this.addWindowListener(new WindowAdapter()
			{
			@Override
			public void windowOpened(WindowEvent e)
				{
				for(int x=0;x<= n_split;x++) {
						for(int y=0;y<=n_split;y++) {
							final Mesh mesh = new Mesh();
							mesh.gridx=x;
							mesh.gridy=y;
							mesh.fx=x*(1.0/(double)n_split);
							mesh.fy=y*(1.0/(double)n_split);
							mesh.prevFx = mesh.fx;
							mesh.prevFy = mesh.fy;
							array.add(mesh);
						}
					}
				removeWindowListener(this);
				}
			});
		this.setContentPane(this.drawingArea);
		}
	
	private Mesh getMesh(int x,int y) {
		int i =  y*(this.n_split+1)+x;
		return (i<0 || i>= this.array.size()?null:this.array.get(i));
	}
	
	private Mesh findMeshAt(final int x,final int y) {		
		return array.stream().
				filter(M->Point.distance(x, y, M.getX(), M.getY())<10).
				findFirst().
				orElse(null);
		}
	
	
	private void paintDrawingArea(final Graphics2D g,int width,int height) {
		g.setColor(Color.WHITE);
		g.fillRect(0,0,width,height);
		g.setColor(Color.BLACK);
		array.stream().forEach(M->{
			g.fill(new Ellipse2D.Double(M.getX()-1, M.getY()-1, 10, 10));
			});
		}
	
	private void applyCoulombsLaw(){
		for(Mesh n1: this.array) {
			for(Mesh n2: this.array) {
				if(n1==n2) continue;
				double dx  = n1.fx - n2.fx;
				double dy  = n1.fy - n2.fy;
				double distance = Point2D.distance(0, 0, dx, dy) + 0.001; // avoid massive forces at small distances (and divide by zero)
				//normalize
				dx = dx/distance;
				dy = dy/distance;
				
				// apply force to each end point
				n1.fx += dx; 
				n1.fy += dy;
			}

		}
	}
	
	private void applyHookesLaw() {
		for(Mesh n1: this.array) {
			
		}
	}
	
	public static void main(String[] args)
		{
		try {
			JFrame.setDefaultLookAndFeelDecorated(true);
			SwingUtilities.invokeAndWait(()->{
				final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
				final MeshDeform frame   = new MeshDeform();
				frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				frame.setBounds(50, 50, dim.width-100, dim.height-100);
				frame.setVisible(true);
				});
			}
		catch(Exception err) {
			err.printStackTrace();
			}

		}

	}
