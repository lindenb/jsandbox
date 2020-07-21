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
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class MeshDeform extends JFrame
	{
	private final JPanel drawingArea;
	private final List<List<Mesh>> array = new ArrayList<>();
	private Mesh selectMesh = null;
	private class Mesh {
		int gridx;
		int gridy;
		double fx;
		double fy;
		public double getX() {
			return fx * drawingArea.getWidth();
			}
		public double getY() {
			return fy * drawingArea.getHeight();
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
		final MouseAdapter mouseAdapter = new MouseAdapter()
			{
			@Override
			public void mousePressed(MouseEvent e)
				{
				selectMesh = findMeshAt(e.getX(), e.getY());
				}
			@Override
			public void mouseDragged(MouseEvent e)
				{
				if(selectMesh!=null) {
					double nx = (e.getX()/(double)drawingArea.getWidth());
					double ny = (e.getY()/(double)drawingArea.getHeight());
					selectMesh.fx=nx;
					selectMesh.fy=ny;
					}
				}
			@Override
			public void mouseReleased(MouseEvent e)
				{
				selectMesh=null;
				}
			};
		this.addMouseListener(mouseAdapter);
		this.addMouseMotionListener(mouseAdapter);
		this.addWindowListener(new WindowAdapter()
			{
			@Override
			public void windowOpened(WindowEvent e)
				{
				for(int x=0;x<=10;x++) {
						List<Mesh> L = new ArrayList<>();
						for(int y=0;y<=10;y++) {
							final Mesh mesh = new Mesh();
							mesh.gridx=x;
							mesh.gridy=y;
							mesh.fx=x*(drawingArea.getWidth()/10.0);
							mesh.fy=y*(drawingArea.getHeight()/10.0);
							L.add(mesh);
						}
						array.add(L);
					}
				removeWindowFocusListener(this);
				}
			});
		}
	
	private Mesh findMeshAt(int x,int y) {
		return array.stream().flatMap(A->A.stream()).filter(M->Point.distance(x, y, M.getX(), M.getY())<2).findFirst().orElse(null);
	
		}
	
	
	private void paintDrawingArea(final Graphics2D g,int width,int height) {
		g.setColor(Color.WHITE);
		
		array.stream().flatMap(A->A.stream()).forEach(M->{
			g.fill(new Ellipse2D.Double(M.getX()-1, M.getY()-1, 2, 2));
			});
		
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
