package sandbox.tools.svgshow;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.nio.file.Paths;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import sandbox.Launcher;
import sandbox.svg.SVGIcon;
import sandbox.tools.central.ProgramDescriptor;

public class SVGShow extends Launcher {

	@Override
	public int doWork(List<String> args) {
		try {
			SVGIcon icon=new SVGIcon(Paths.get(this.oneAndOnlyOneFile(args)));
			JPanel pane=new JPanel(new BorderLayout(5,5));
			JScrollPane scroll=new JScrollPane(new JLabel(icon));
			scroll.setPreferredSize(new Dimension(500,500));
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
