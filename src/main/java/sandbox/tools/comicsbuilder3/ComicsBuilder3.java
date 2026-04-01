package sandbox.tools.comicsbuilder3;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;

import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

public class ComicsBuilder3 {
	private static abstract class Tool {
		abstract String getName();
		@Override
		public String toString() {
			return  getName();
			}
		}
	private static class ToolCase extends Tool {
		@Override
		String getName() {
			return "case";
			}
		}
	private static class ToolCase2 extends Tool {
		@Override
		String getName() {
			return "cas2e";
			}
		}
	private static class XFrame extends JFrame {
		private JPanel drawingArea;
		public XFrame() {
			setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			final JPanel pane=new JPanel(new BorderLayout());
			this.setContentPane(pane);
			JPanel top=new JPanel(new FlowLayout());
			top.add(new JLabel("Tool:"));
			final JComboBox<Tool> jcomboTool = new JComboBox<Tool>(new Tool[] {
					new ToolCase(),
					new ToolCase2()
					});
			jcomboTool.addItemListener(AE->System.err.println("Changed"+AE.getItem()));
			top.add(jcomboTool);
			pane.add(top,BorderLayout.NORTH);
			this.drawingArea=new JPanel(null) {
				@Override
				protected void paintComponent(Graphics g) {
					paintDrawingArea(Graphics2D.class.cast(g));
					}
				};
			pane.add(this.drawingArea,BorderLayout.CENTER);
			}
		private void paintDrawingArea(Graphics2D g) {
			g.setColor(Color.WHITE);
			g.fillRect(0, 0, this.drawingArea.getWidth(), this.drawingArea.getHeight());
		}
	}
	
	
	public static void main(String[] args) {
		final XFrame f=new XFrame();
		try {
			SwingUtilities.invokeAndWait(()->{
				Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
				f.setBounds(50, 50, d.width-100, d.height-100);
				f.setVisible(true);
			});
			}
		catch(Throwable err) {
			err.printStackTrace();
		}
	}

}
