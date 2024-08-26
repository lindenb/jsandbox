package sandbox.tools.svgshow;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import sandbox.Launcher;
import sandbox.svg.SVGIcon;
import sandbox.tools.central.ProgramDescriptor;

public class SVGShow extends Launcher {
	private class ScaleIcon implements Icon {
		private final Icon delegate;
		ScaleIcon(Icon delegate) {
			this.delegate=delegate;
			}
		@Override
		public int getIconHeight() {
			return iconSize;
			}
		@Override
		public int getIconWidth() {
			return iconSize;
			}
		@Override
		public void paintIcon(Component c, Graphics g1, int x, int y) {
			double dx= iconSize/(double)delegate.getIconWidth();
			double dy= iconSize/(double)delegate.getIconHeight();
			double df= Math.max(dx, dy);
			Graphics2D g=Graphics2D.class.cast(g1);
			AffineTransform tr = g.getTransform();
			AffineTransform tr2 = new AffineTransform(tr);
			tr2.translate(x, y);
			tr2.scale(df, df);
			g.setTransform(tr2);
			
			delegate.paintIcon(c, g, 0, 0);
			
			g.setTransform(tr);
			}
		}
	
	private int iconSize=96;
	
	@Override
	public int doWork(List<String> args) {
		try {
			Icon icon=new SVGIcon(Paths.get(this.oneAndOnlyOneFile(args)));
			icon = new ScaleIcon(icon);
			JPanel pane=new JPanel(new BorderLayout(5,5));
			JScrollPane scroll=new JScrollPane(new JLabel(icon));
			scroll.setPreferredSize(new Dimension(icon.getIconWidth()+1,icon.getIconWidth()+1));
			pane.add(scroll);
			
			JOptionPane.showMessageDialog(null, pane);
			}
		catch(Exception err) {
			err.printStackTrace();
			return -1;
			}
		return 0;
		}
	
	public static ProgramDescriptor getProgramDescriptor() {
		return new ProgramDescriptor() {
			@Override
			public String getName() {
				return SVGShow.class.getSimpleName().toLowerCase();
				}
			};
		}
	
	public static void main(String[] args) {
		new SVGShow().instanceMain(args);
	}

}
