package sandbox.drawing;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

@SuppressWarnings("serial")
public class DrawingGrid extends JFrame {
	final BufferedImage srcImage;
	final JPanel drawingArea;
	final SpinnerNumberModel ticksSpinner;
	final JCheckBox rotateCbox;
	DrawingGrid(BufferedImage srcImage) {
		this.srcImage=srcImage;
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter()
			{
			@Override
			public void windowOpened(WindowEvent e)
				{
				Dimension d=Toolkit.getDefaultToolkit().getScreenSize();
				e.getWindow().setBounds(50, 50, d.width-100, d.height-100);
				}
			
			@Override
			public void windowClosing(WindowEvent e)
				{
				doMenuClose();
				}
			});
	JPanel contentPane =new JPanel(new BorderLayout(5,5));	
	this.setContentPane(contentPane);
	
	JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
	top.add(new JLabel("Ticks:",JLabel.RIGHT));
	top.add(new JSpinner(ticksSpinner=new SpinnerNumberModel(8, 2, 30,2)));
	top.add(rotateCbox=new JCheckBox("Rotation",true));
	contentPane.add(top,BorderLayout.NORTH);
	
	this.drawingArea = new JPanel(null) {
		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			paintDrawingArea(Graphics2D.class.cast(g));
			}
		};
	this.drawingArea.setOpaque(true);
	this.drawingArea.setBackground(Color.LIGHT_GRAY);
	contentPane.add(this.drawingArea,BorderLayout.CENTER);
	
	ticksSpinner.addChangeListener(E->drawingArea.repaint());
	rotateCbox.addActionListener(E->drawingArea.repaint());
	}
	
	private boolean isPortrait(final int w,final int h) {
		return h>w;
	}
	
	private void paintDrawingArea(Graphics2D g)
		{
		int panelWidth = this.drawingArea.getWidth();
		int panelHeight = this.drawingArea.getHeight();
		int imgWidth = this.srcImage.getWidth();
		int imgHeight = this.srcImage.getHeight();
		if(imgWidth==0 || imgWidth==0 || panelWidth==0 || panelHeight==0) return;
		AffineTransform tr= new AffineTransform();
		
		if(this.rotateCbox.isSelected() && isPortrait(panelWidth, panelHeight) != isPortrait(imgWidth,imgHeight)) {
			tr.concatenate(AffineTransform.getTranslateInstance(imgHeight, 0));
			tr.concatenate(AffineTransform.getRotateInstance(Math.PI/2));
			int tmp = imgHeight;
			imgHeight = imgWidth;
			imgWidth = tmp;
		}
		
		if(imgWidth> panelWidth || imgHeight>panelHeight) {
			double r1 = (double)panelWidth/imgWidth;
			double r2 = (double)panelHeight/imgHeight;
			double r = Math.min(r1, r2);
			AffineTransform tr2 = AffineTransform.getScaleInstance(r, r);
			tr2.concatenate(tr);
			tr=tr2;
			imgWidth = (int)(imgWidth*r);
			imgHeight =(int)(imgHeight*r);
			}
		
		int dx=(panelWidth-imgWidth)/2;
		int dy=(panelHeight-imgHeight)/2;
		AffineTransform tr3 = AffineTransform.getTranslateInstance(dx,dy);
				;
		tr3.concatenate(tr);
		tr=tr3;

		
		g.drawImage(this.srcImage,tr,null);
		
		g.setXORMode(Color.YELLOW);
		final Stroke stroke=g.getStroke();
		int num1 = this.ticksSpinner.getNumber().intValue();
		int num2 = num1/2;
		for(int i=1;i< num1;i++) {
			if(i%num2==0) {
				g.setStroke(new BasicStroke(1.5f));
				g.setColor(Color.RED);
				}
			else
				{
				g.setStroke(new BasicStroke(0.5f));
				g.setColor(Color.BLUE);
				}
			int x= dx+ (int)(i*(imgWidth/(double)num1));
			int y= dy+ (int)(i*(imgHeight/(double)num1));
			g.drawLine(x, dy, x, dy+imgHeight);
			g.drawLine(dx, y, dx+imgWidth, y);
			}
		g.setPaintMode();
		g.setStroke(stroke);
		}

	
	private void doMenuClose()
		{
		this.setVisible(false);
		this.dispose();
		}
	
	public static void main(String[] args) {
		if(args.length!=1) return;
		try {
		final File imageFile = new File(args[0]);
		final BufferedImage srcImage = ImageIO.read(imageFile);
		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
		final DrawingGrid f=new DrawingGrid(srcImage);
		f.setTitle(imageFile.getName());
		SwingUtilities.invokeAndWait(()->f.setVisible(true));
		} catch(final Throwable err) {
			err.printStackTrace();
		}
	}

}
