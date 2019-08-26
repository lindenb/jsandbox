package sandbox.drawing;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import sandbox.ImageUtils;

@SuppressWarnings("serial")
public class DrawingGrid extends JFrame {
	private BufferedImage srcImage;
	private final JPanel drawingArea;
	private final SpinnerNumberModel ticksSpinner;
	private final JCheckBox rotateCbox;
	private File imageFile;
	private Point mouseStart=null;
	private Point mousePrev=null;
	DrawingGrid(BufferedImage srcImage,final File imageFile) {
		this.srcImage=srcImage;
		this.imageFile = imageFile;
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
	JPanel contentPane =new JPanel(new BorderLayout(1,1));	
	this.setContentPane(contentPane);
	contentPane.setBorder(BorderFactory.createEmptyBorder());
	
	JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT));
	top.setBorder(BorderFactory.createEmptyBorder());
	top.add(new JLabel("Ticks:",JLabel.RIGHT));
	top.add(new JSpinner(ticksSpinner=new SpinnerNumberModel(8, 2, 30,2)));
	top.add(rotateCbox=new JCheckBox("Rotation",true));
	JButton but;
	top.add(but=new JButton("Prev"));
	but.addActionListener(E->switchImage(-1));
	top.add(but=new JButton("Next"));
	but.addActionListener(E->switchImage(1));
	
	/*top.add(but=new JButton("[+]"));
	but.addActionListener(E->zoomImage(1.1));
	top.add(but=new JButton("[-]"));
	but.addActionListener(E->zoomImage(0.9));
	*/
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
	this.drawingArea.setBorder(BorderFactory.createEmptyBorder());
	contentPane.add(this.drawingArea,BorderLayout.CENTER);
	
	
	final MouseAdapter mouse= new MouseAdapter() {
		public void mousePressed(MouseEvent e) 
			{
			mouseStart = new Point(e.getX(),e.getY());
			e.getComponent().setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
			}
		public void mouseDragged(MouseEvent e) {
			Graphics2D g=(Graphics2D)e.getComponent().getGraphics();
			g.setXORMode(g.getBackground());
			for(int side=0;side<2;++side) {
				if(mousePrev!=null) {
					g.fillRect(mouseStart.x,mouseStart.y,
							Math.abs(mouseStart.x-mousePrev.x),
							Math.abs(mouseStart.y-mousePrev.y)
							);
					}
				mousePrev=new Point(e.getX(),e.getY());
				}
			g.setPaintMode();
			}
		public void mouseReleased(MouseEvent e){
			if(mousePrev==null) return;
			e.getComponent().setCursor(Cursor.getDefaultCursor());
			AffineTransform tr= getTransform();
			if(tr==null) return;
			final Rectangle bounds =tr.createTransformedShape(new Rectangle(0,0,
					DrawingGrid.this.srcImage.getWidth(),
					DrawingGrid.this.srcImage.getHeight())
					).getBounds();
			final Rectangle b2 = bounds.intersection(new Rectangle(
					mouseStart.x,
					mouseStart.y,
					Math.abs(mouseStart.x-mousePrev.x),
					Math.abs(mouseStart.y-mousePrev.y)
					));
			if(b2!=null) {
				try {
				final AffineTransform tr2 = tr.createInverse();
				final Rectangle b3 = tr2.createTransformedShape(b2).getBounds();
				DrawingGrid.this.srcImage=DrawingGrid.this.srcImage.getSubimage(b3.x, b3.y, b3.width, b3.height);
				} catch(Throwable err) {
					return;
				}
				}
			drawingArea.repaint();
			mouseStart=null;
			mousePrev=null;
			
			}
		};
	this.drawingArea.addMouseListener(mouse);
	this.drawingArea.addMouseMotionListener(mouse);

	
	ticksSpinner.addChangeListener(E->drawingArea.repaint());
	rotateCbox.addActionListener(E->drawingArea.repaint());
	}
	
	private boolean isPortrait(final int w,final int h) {
		return h>w;
	}
	
	private AffineTransform getTransform() {
		int panelWidth = this.drawingArea.getWidth();
		int panelHeight = this.drawingArea.getHeight();
		int imgWidth = this.srcImage.getWidth();
		int imgHeight = this.srcImage.getHeight();
		if(imgWidth==0 || imgWidth==0 || panelWidth==0 || panelHeight==0) return null;
		AffineTransform tr= new AffineTransform();
		
		if(this.rotateCbox.isSelected() && isPortrait(panelWidth, panelHeight) != isPortrait(imgWidth,imgHeight)) {
			tr.concatenate(AffineTransform.getTranslateInstance(imgHeight, 0));
			tr.concatenate(AffineTransform.getRotateInstance(Math.PI/2));
			int tmp = imgHeight;
			imgHeight = imgWidth;
			imgWidth = tmp;
		}
		
		if(imgWidth!= panelWidth || imgHeight!=panelHeight) {
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
		return tr;
		}
	
	private void paintDrawingArea(final Graphics2D g)
		{
		AffineTransform tr=getTransform();
		if(tr==null) return;
		final Composite oldComposite = g.getComposite();
		//g.setComposite(AlphaComposite.getInstance(AlphaComposite.XOR));
		g.drawImage(this.srcImage,tr,null);
		g.setComposite(oldComposite);
		
		g.setXORMode(Color.YELLOW);
		final Stroke stroke=g.getStroke();
		int num1 = this.ticksSpinner.getNumber().intValue();
		int num2 = num1/2;
		
		final Rectangle bounds =tr.createTransformedShape(new Rectangle(0,0,this.srcImage.getWidth(),this.srcImage.getHeight())).getBounds();
		
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
			
			
			int x= bounds.x + (int)(i*(bounds.width/(double)num1));
			int y= bounds.y + (int)(i*(bounds.height/(double)num1));
			g.drawLine(x, bounds.y, x, bounds.y+bounds.height);
			g.drawLine(bounds.x , y, bounds.x+bounds.width, y);
			}
		g.setPaintMode();
		g.setStroke(stroke);
		
		}

	private void switchImage(int dx) {
		final ImageUtils utils = ImageUtils.getInstance();
		final File dir=this.imageFile.getParentFile();
		if(dir==null || !dir.isDirectory()) return;
		final File list[]= dir.listFiles(F->F.canRead() && F.isFile() && utils.hasImageSuffix(F));
		if(list==null || list.length<=1) return;
		final List<File> files = Arrays.stream(list).sorted((A,B)->A.getName().compareTo(B.getName())).collect(Collectors.toList());
		int idx=0;
		for(idx=0;idx< files.size();idx++) {
			if(files.get(idx).getName().equals(this.imageFile.getName())) {
				break;
				}
			}
		if(idx>=files.size()) {
			return;
		}
		idx +=dx;
		if(idx<0) idx=files.size()-1;
		if(idx>=files.size()) idx=0;

		try {
			final File f2 = files.get(idx);
			final BufferedImage img2=ImageIO.read(f2);
			this.imageFile=f2;
			this.srcImage=img2;
			this.setTitle(f2.getName());
			this.drawingArea.repaint();
			}
		catch(final IOException err)
			{
			err.printStackTrace();
			}
		}
	
	private void doMenuClose()
		{
		this.setVisible(false);
		this.dispose();
		}
	
	public static void main(final String[] args) {
		if(args.length!=1) return;
		try {
		final File baseFile = new File(args[0]);
		final File srcFile;
		
		if(baseFile.isDirectory()) {
			final ImageUtils iu = ImageUtils.getInstance();
			final File array[]=baseFile.listFiles(F->F.isFile() && F.canRead() && iu.hasImageSuffix(F));
			if(array==null || array.length==0) {
				System.err.println("No Image found in "+baseFile);
				System.exit(-1);
				}
			srcFile = array[0];
			}
		else
			{
			srcFile = baseFile;
			}
		
		final BufferedImage srcImage = ImageIO.read(srcFile);

		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
		final DrawingGrid f=new DrawingGrid(srcImage,srcFile);
		f.setTitle(srcFile.getName());
		SwingUtilities.invokeAndWait(()->f.setVisible(true));
		} catch(final Throwable err) {
			err.printStackTrace();
		}
	}

}
