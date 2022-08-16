package sandbox.tools.drawclip;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.io.IOUtils;
import sandbox.svg.SVG;

@SuppressWarnings("serial")
public class DrawClip extends Launcher {
	private static final sandbox.Logger LOG = sandbox.Logger.builder(DrawClip.class).build();
    @Parameter(names={"-o","--ouput"},description=OUTPUT_OR_STANDOUT,required = true)
	private File outpuFile  = null;

	private class XFrame extends JFrame {

	private final double HOTSPOT_RADIUS=5;
	private BufferedImage srcImage;
	private final JPanel drawingArea;
	private final JCheckBox rotateCbox;
	private Object imageFileOrUrl;
	private final List<Point2D> hotSpots = new ArrayList<>();
	private AffineTransform transform;
	private AffineTransform inverseTransform;
	
	
	
	XFrame(final Object imageFileOrUrl,BufferedImage srcImage) {
		this.srcImage=srcImage;
		this.imageFileOrUrl = imageFileOrUrl;
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		
		this.hotSpots.add(new Point2D.Double(0,0));
		this.hotSpots.add(new Point2D.Double(srcImage.getWidth(),0));
		this.hotSpots.add(new Point2D.Double(srcImage.getWidth(),srcImage.getHeight()));
		this.hotSpots.add(new Point2D.Double(0,srcImage.getHeight()));
		
		
		this.addWindowListener(new WindowAdapter()
			{
			@Override
			public void windowOpened(WindowEvent e)
				{
				final Dimension d=Toolkit.getDefaultToolkit().getScreenSize();
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
	top.add(rotateCbox=new JCheckBox("Rotate",true));
	
	top.add(new JButton(new AbstractAction("Save...") {			
			@Override
			public void actionPerformed(ActionEvent e) {
				saveAs(outpuFile);
			}
		}));
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
	this.drawingArea.addComponentListener(new ComponentAdapter() {
		void reset() {
			transform=null;
			inverseTransform=null;
			}
		@Override
		public void componentResized(ComponentEvent e) {
			reset();
			}
		@Override
		public void componentShown(ComponentEvent e) {
			reset();
			}
		});
	contentPane.add(this.drawingArea,BorderLayout.CENTER);
	
	
	final MouseAdapter mouse= new MouseAdapter() {
		private Point mouseStart=null;
		private Point mousePrev=null;
		private int selIndex=-1;
		public void mousePressed(MouseEvent e) 
			{
			this.mouseStart = new Point(e.getX(),e.getY());
			this.mousePrev = null;
			this.selIndex = getHotSpotIndexAt(this.mouseStart);
			if(this.selIndex==-1 && !e.isControlDown()) {
				int best= -1;
				double best_distance=0;
				for(int i=0;i<hotSpots.size();i++) {
					final Point2D p1 = modelToScreen(hotSpots.get(i));
					final Point2D p2 = modelToScreen(hotSpots.get(i+1< hotSpots.size()?i+1:0));
					final Line2D line = new Line2D.Double(p1, p2);
					final double distance =  line.ptLineDist(this.mouseStart);
					if(distance > HOTSPOT_RADIUS) continue;
					if(best==-1 || best_distance > distance) {
						best =i;
						best_distance = distance;
						}
					}
				if(best!=-1) {
					final Point2D ptModel =screenToModel(this.mouseStart);
					hotSpots.add(best+1,ptModel);
					this.selIndex=best+1;
					}
				}
			lines(this.mouseStart);
			e.getComponent().setCursor(Cursor.getPredefinedCursor(this.selIndex==-1?Cursor.DEFAULT_CURSOR:Cursor.MOVE_CURSOR));
			}
		
		void lines(Point p) {
			if(this.selIndex==-1 || p==null) return;
			int i1 = this.selIndex-1<0?hotSpots.size()-1:this.selIndex-1;
			int i3 = this.selIndex+1>=hotSpots.size()?0:this.selIndex+1;
			Point2D p1 = modelToScreen(i1);
			Point2D p3 = modelToScreen(i3);
			Graphics2D g = (Graphics2D)drawingArea.getGraphics();
			g.setXORMode(drawingArea.getBackground());
			g.draw(new Line2D.Double(p1, p));
			g.draw(new Line2D.Double(p3, p));
			}
		
		public void mouseDragged(final MouseEvent e) {
			if(this.selIndex!=-1) {
				lines(this.mousePrev);
				this.mousePrev = e.getPoint();
				hotSpots.set(selIndex, screenToModel(e.getPoint()));
				lines(this.mousePrev);
				}
			}
		public void mouseReleased(final MouseEvent e){
			if(e.isControlDown() && hotSpots.size()>2 && this.selIndex!=-1) {
				hotSpots.remove(this.selIndex);
				}
			drawingArea.repaint();
			mouseStart=null;
			mousePrev=null;
			selIndex=-1;
			}
		};
	this.drawingArea.addMouseListener(mouse);
	this.drawingArea.addMouseMotionListener(mouse);

	
	rotateCbox.addActionListener(E->drawingArea.repaint());
	}
	
	private boolean isPortrait(final int w,final int h) {
		return h>w;
	}
	
	private int getHotSpotIndexAt(Point ptScreen) {
		final AffineTransform tr = getTransform();
		final Point2D p2 = new Point2D.Double();
		int best= -1;
		double best_d = -1;
		for(int i=0;i< hotSpots.size();i++) {
			final Point2D p3 = tr.transform(hotSpots.get(i),p2);
			final double dist = p3.distance(ptScreen);
			if(dist> HOTSPOT_RADIUS || (best!=-1 && dist > best_d)) continue;
			best = i;
			best_d = dist;
			}
		return best;
		}
	
	private Point2D modelToScreen(int idx) {
		return modelToScreen(hotSpots.get(idx));
		}

	private Point2D screenToModel(Point2D p) {
		return getInverseTransform().transform(p, null);
		}
	private Point2D modelToScreen(Point2D p) {
		return getTransform().transform(p, null);
		}

	
	private AffineTransform getInverseTransform() {
		if(this.inverseTransform!=null) return this.inverseTransform;
			try {
				this.inverseTransform = getTransform().createInverse();
				return this.inverseTransform;
				}
			catch(Throwable err) {
				throw new RuntimeException(err);
			}
		}
	
	private AffineTransform getTransform() {
		if(this.transform!=null) return transform;
		int panelWidth = this.drawingArea.getWidth();
		int panelHeight = this.drawingArea.getHeight();
		int imgWidth = this.srcImage.getWidth();
		int imgHeight = this.srcImage.getHeight();
		if(imgWidth==0 || imgWidth==0 || panelWidth==0 || panelHeight==0) {
			this.transform= new AffineTransform();
			}
		AffineTransform tr= new AffineTransform();
		
		if(this.rotateCbox.isSelected() && isPortrait(panelWidth, panelHeight) != isPortrait(imgWidth,imgHeight)) {
			tr.concatenate(AffineTransform.getTranslateInstance(imgHeight, 0));
			tr.concatenate(AffineTransform.getRotateInstance(Math.PI/2));
			int tmp = imgHeight;
			imgHeight = imgWidth;
			imgWidth = tmp;
		}
		
		if(imgWidth!= panelWidth || imgHeight!=panelHeight) {
			final double r1 = (double)panelWidth/imgWidth;
			final double r2 = (double)panelHeight/imgHeight;
			final double r = Math.min(r1, r2);
			final AffineTransform tr2 = AffineTransform.getScaleInstance(r, r);
			tr2.concatenate(tr);
			tr=tr2;
			imgWidth = (int)(imgWidth*r);
			imgHeight =(int)(imgHeight*r);
			}
		
		final int dx=(panelWidth-imgWidth)/2;
		final int dy=(panelHeight-imgHeight)/2;
		final AffineTransform tr3 = AffineTransform.getTranslateInstance(dx,dy);
				;
		tr3.concatenate(tr);
		tr=tr3;
		this.transform = tr;
		return this.transform;
		}
	
	private Shape getHotSpotsToShape() {
		final GeneralPath path = new GeneralPath();
		for(int i=0;i< hotSpots.size();i++) {
			final Point2D p = hotSpots.get(i);
			if(i==0) {
				path.moveTo(p.getX(),p.getY());
				}
			else {
				path.lineTo(p.getX(),p.getY());
				}
			}
		path.closePath();
		return path;
		}
	
	private void paintDrawingArea(final Graphics2D g)
		{
		AffineTransform tr=getTransform();
		if(tr==null) return;
		g.drawImage(this.srcImage,tr,null);
		
		
		if(!hotSpots.isEmpty()) {
			final Shape path = tr.createTransformedShape(getHotSpotsToShape());
			g.setXORMode(Color.YELLOW);
			g.fill(path);
			
			for(int i=0;i< hotSpots.size();i++) {
				final Point2D p = modelToScreen(hotSpots.get(i));
				g.fillOval(
						(int)(p.getX()-HOTSPOT_RADIUS),
						(int)(p.getY()-HOTSPOT_RADIUS),
						(int)(HOTSPOT_RADIUS*2),
						(int)(HOTSPOT_RADIUS*2)
						);
					}
			g.setPaintMode();
			}
		}

	
	private void doMenuClose()
		{
		this.setVisible(false);
		this.dispose();
		}
	private void saveAs(File f) {
		if(f==null) return;
		LOG.info("save as \""+f+"\"");
		try {
			XMLOutputFactory xof = XMLOutputFactory.newFactory();
				try(FileWriter pw = new FileWriter(f)) {
				final XMLStreamWriter w = xof.createXMLStreamWriter(pw);
				w.writeStartDocument("1.0");
				w.writeStartElement("svg");
				w.writeDefaultNamespace(SVG.NS);
				Shape shape = getHotSpotsToShape();
				Rectangle2D bounds=shape.getBounds2D();
				
				w.writeAttribute("width",String.valueOf(bounds.getWidth()));
				w.writeAttribute("height",String.valueOf(bounds.getHeight()));
				
				w.writeStartElement("defs");
				
				
				// clip
				w.writeStartElement("clipPath");
				w.writeAttribute("id", "clip1");
				w.writeStartElement("path");
				StringBuilder sb = new StringBuilder();
				sb.append("M " +hotSpots.get(0).getX()+","+hotSpots.get(0).getY());
				for(int i=1;i<hotSpots.size();i++) {
					sb.append(" L " +hotSpots.get(i).getX()+","+hotSpots.get(i).getY());
					}
				sb.append(" z");
				w.writeAttribute("d", sb.toString());
				w.writeAttribute("style", "fill:black");
				w.writeEndElement();
				w.writeEndElement();
				
				// image
				w.writeStartElement("g");
				w.writeAttribute("id", "img1");
				w.writeAttribute("transform", "translate("+(-bounds.getMinX())+","+(-bounds.getMinY()+")"));
				w.writeStartElement("image");
				w.writeAttribute("href", String.valueOf(this.imageFileOrUrl));
				w.writeAttribute("width", String.valueOf(this.srcImage.getWidth()));
				w.writeAttribute("height", String.valueOf(this.srcImage.getHeight()));
				w.writeAttribute("clip-path","url(#clip1)");
				w.writeEndElement();
				w.writeEndElement();
	
				
				w.writeEndElement();//defs
	
				w.writeStartElement("g");
				w.writeEmptyElement("use");
				w.writeAttribute("x", String.valueOf(0));
				w.writeAttribute("y", String.valueOf(0));
				w.writeAttribute("href", "#img1");
				
				w.writeEndElement();//g
				
				w.writeEndElement();//svg
				w.writeEndDocument();//doc
				w.flush();
				w.close();
				}
			}
		catch(Exception err) {
			LOG.error(err);
			}
		}
	}
	@Override
	public int doWork(List<String> args) {
		try {
		final String filename  = super.oneAndOnlyOneFile(args);
		final BufferedImage srcImage;
		final Object imageFileOrUrl;
		if(IOUtils.isURL(filename)) {
			final URL url = new URL(filename);
			srcImage = ImageIO.read(url);
			imageFileOrUrl = url;
			}
		else
			{
			final File baseFile = new File(filename);
			srcImage = ImageIO.read(baseFile);
			imageFileOrUrl = baseFile;
			}
		LOG.info(imageFileOrUrl);

		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
		final DrawClip.XFrame f=new DrawClip.XFrame(imageFileOrUrl,srcImage);
		f.setTitle(filename);
		SwingUtilities.invokeAndWait(()->f.setVisible(true));
		} catch(final Throwable err) {
			err.printStackTrace();
		}
		return 0;
	}

	public static void main(String[] args) {
		new DrawClip().instanceMain(args);
	}
}
