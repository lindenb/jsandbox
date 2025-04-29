package sandbox.tools.chalkboard;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Rectangle2D;
import java.io.File;
import java.io.PrintWriter;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.svg.SVG;
import sandbox.tools.central.ProgramDescriptor;

@SuppressWarnings("serial")
public class ChalkBoard extends Launcher  {
	@Parameter(names="-o",description="save directory")
	private File outputDir = null;
	@Parameter(names="--prefix",description="file prefix")
	private String prefix = "slideshow.";


	private static class MyStyle {
		String className;
		boolean for_text=false;
		Color color = Color.BLACK;
		int lineSize = 1;
		@Override
		public boolean equals(Object obj)
			{
			MyStyle other= MyStyle.class.cast(obj);
			if(this.for_text!=other.for_text) return false;
			if(this.lineSize!=other.lineSize) return false;
			if(!this.color.equals(other.color)) return false;
			return true;
			}
		@Override
		public int hashCode()
			{
			return (color.hashCode()*31 +Integer.hashCode(lineSize))*31 + (for_text?0:1);
			}
		void apply(final Graphics2D g) {
			g.setColor(this.color);
			if(this.for_text) {
				g.setFont(new Font("courier",Font.BOLD,this.lineSize));
				}
			else
				{
				g.setStroke(new BasicStroke(lineSize,BasicStroke.CAP_ROUND,BasicStroke.JOIN_ROUND));
				}
			}
		void write(final XMLStreamWriter w) throws XMLStreamException {
			final StringBuilder sb=new StringBuilder(".").
					append(className).
					append("{");
			sb.append("opacity:0.95;fill:none;stroke-linecap:butt;stroke-linejoin:round;stroke:rgb(").
				append(color.getRed()).
				append(",").
				append(color.getGreen()).
				append(",").
				append(color.getBlue()).
				append(");");
			if(for_text) {
				sb.append("font-height:").append(this.lineSize).append(";");
				}else
				{
				sb.append("stroke-width:").append(this.lineSize).append(";");
				}
			
			sb.append("}\n");
			w.writeCharacters(sb.toString());
			}
		}
	private static abstract class MyShape {
		MyStyle style;
		boolean isText() { return false;}
		boolean isPolyline() { return false;}
		abstract void paint(final Graphics2D g);
		abstract void write(final XMLStreamWriter w) throws XMLStreamException;
		abstract void translate(int dx,int dy);
		}
	
	private static class MyText extends MyShape {
		Point point;
		StringBuilder sb= new StringBuilder();
		@Override
		boolean isText() {
			return true;
			}
		@Override
		void paint(Graphics2D g) {
			style.apply(g);
			g.drawString(sb.toString(), point.x, point.y);
			}
		@Override void write(final XMLStreamWriter w) throws XMLStreamException {
			w.writeStartElement("x");
			w.writeAttribute("class", style.className);
			w.writeAttribute("x", String.valueOf(point.getX()));
			w.writeAttribute("y", String.valueOf(point.getY()));
			w.writeCharacters(sb.toString());
			w.writeEndElement();
			}
		@Override
		void translate(int dx,int dy) {
			point.x+=dx;
			point.y+=dy;
			}
		}
	
	
	private static class MyPolyLine extends MyShape {
		final List<Point> points = new ArrayList<>();
		@Override
		boolean isPolyline() {
			return true;
			}
		
		private Shape toShape() {
			final GeneralPath gp = new GeneralPath();
			
			for(int i=0;i< points.size();i++) {
				Point pt = points.get(i);
				if(i==0) {
					gp.moveTo(pt.x, pt.y);
					}
				else
					{
					gp.lineTo(pt.x, pt.y);
					}
				}
			return gp;
			}
		
		@Override void translate(int dx,int dy) {
			for(final Point p:this.points) {
				p.x+=dx;
				p.y+=dy;
			}
		}
		
		@Override
		void paint(final Graphics2D g) {
			style.apply(g);
			g.draw(toShape());
			}
		@Override
		void write(final XMLStreamWriter w) throws XMLStreamException {
			w.writeEmptyElement("polyline");
			w.writeAttribute("class", style.className);
			w.writeAttribute("points",
				this.points.stream().map(P->String.valueOf(P.x)+","+P.y).
					collect(Collectors.joining(" "))
				);
			}
		}
	private static class Slide  extends AbstractList<MyShape> {
		String title="";
		Dimension dimension;
		final List<MyStyle> styles = new ArrayList<>();
		final List<MyShape> shapes = new ArrayList<>();
		Slide(Dimension d) {
			this.dimension = d;
			}
		
		@Override
		public int size() {
			return this.shapes.size();
			}
		@Override
		public MyShape get(int index) {
			return this.shapes.get(index);
			}
		public MyShape getLast() {
			return isEmpty()?
					null:
					this.shapes.get(this.size()-1);
			}
		
		public MyStyle getSyle(MyStyle st) {
			int i= this.styles.indexOf(st);
			if(i!=-1) return this.styles.get(i);
			st.className = "s"+styles.size();
			this.styles.add(st);
			return st;
			}
		
		void paint(final Graphics2D g) {
			final Stroke stroke = g.getStroke();
			final Font font = g.getFont();
			g.setColor(Color.LIGHT_GRAY);
			g.drawString(this.title, 1, 15);
			for(MyShape shape:this.shapes) {
				shape.paint(g);
				}
			g.setFont(font);
			g.setStroke(stroke);
			}
		void write(final XMLStreamWriter w) throws XMLStreamException {
			w.writeStartElement("svg");
			w.writeDefaultNamespace(SVG.NS);
			w.writeAttribute("width", String.valueOf(this.dimension.width+1));
			w.writeAttribute("height", String.valueOf(this.dimension.height+1));
			w.writeStartElement("style");
			w.writeCharacters(".bckg {fill:white;stroke:darkgray;}");
			for(MyStyle style:this.styles) {
				style.write(w);
				}
			w.writeEndElement();//style
			w.writeStartElement("title");
			w.writeCharacters(this.title);
			w.writeEndElement();//title
			
			
			w.writeStartElement("g");
			w.writeEmptyElement("rect");
			w.writeAttribute("class", "bckg");
			w.writeAttribute("x", "0");
			w.writeAttribute("y", "0");
			w.writeAttribute("width", String.valueOf(this.dimension.width));
			w.writeAttribute("height", String.valueOf(this.dimension.width));
			

			for(MyShape shape:this.shapes) {
				shape.write(w);
				}
			w.writeEndElement();//g
			w.writeEndElement();//svg
			}
		}
	
	private static class SlideShow  {
		final List<Slide> slides = new ArrayList<>();
		SlideShow() {
			
			}
		
		}
	
	
	private class XFrame extends JFrame {
		private JPanel drawingArea;
		private SlideShow slideShow = new SlideShow();
		private int slide_index=0;
		private final String prefix;
		private File saveDir = null;
		private Point lastClick = null;
		
		private abstract class AbstractSelectorPane extends JPanel {
		AbstractSelectorPane() {
				setOpaque(true);
				setBackground(Color.WHITE);
				setPreferredSize(new Dimension(200,30));
				final MouseAdapter mouse= new MouseAdapter()
					{
					@Override
					public void mouseClicked(MouseEvent e)
						{
						doMouseClicked(e);
						}
					@Override
					public void mouseMoved(MouseEvent e)
						{
						doMouseClicked(e);
						}
					};
				this.addMouseListener(mouse);
				this.addMouseMotionListener(mouse);
				}
			@Override
			protected void paintComponent(Graphics g) {
				g.setColor(Color.WHITE);
				g.fillRect(0,0,this.getWidth(),this.getHeight());
				paintDrawingArea(Graphics2D.class.cast(g));
				}
			abstract void paintDrawingArea(Graphics2D g);
			abstract void doMouseClicked(MouseEvent e);
			}
		
		private class SelectColorPane extends AbstractSelectorPane {
			double fraction=0.0;
			Color getColor(float f) {
				return Color.getHSBColor(f, 0.9f, 0.9f);
				}
			Color getValue() {
				return getColor((float)fraction);
				}
			void paintDrawingArea(Graphics2D g) {
				final int width = this.getWidth();
				if(width<2) return;
				double w=1.0/width;
				for(int i=0;i< width;i++) {
					g.setColor(getColor(i/(float)width));
					g.fill(new Rectangle2D.Double(i, 2, w, this.getHeight()-4));
					}
				int x = (int)(fraction*width);
				g.setXORMode(this.getBackground());
				g.setColor(Color.BLACK);
				g.drawLine(x, 0, x, this.getHeight());
				g.setPaintMode();
				}
			@Override
			void doMouseClicked(MouseEvent e)
				{
				this.fraction = e.getX()/(double)this.getWidth();
				this.repaint();
				}
			}
		private SelectColorPane selectColorPane;

		private class SelectPenSize extends AbstractSelectorPane {
			double fraction=1.0;
			int getPenSize(double f) {
				return (int)(fraction*this.getHeight());
				}
			int getValue() {
				return getPenSize(fraction);
				}
			void paintDrawingArea(Graphics2D g) {
				g.setColor(Color.BLACK);
				GeneralPath p = new GeneralPath();
				p.moveTo(0, this.getHeight()/2.0);
				p.lineTo(this.getWidth(), 0);
				p.lineTo(this.getWidth(), this.getHeight());
				p.closePath();
				g.fill(p);
				
				int x = (int)(fraction * this.getWidth());
				g.setXORMode(this.getBackground());
				g.setColor(Color.BLACK);
				g.drawLine(x, 0, x, this.getHeight());
				g.setPaintMode();
				}
			@Override
			void doMouseClicked(MouseEvent e)
				{
				this.fraction = e.getX()/(double)this.getWidth();
				this.repaint();
				}
			}
		private SelectPenSize selectPenSize;
		XFrame(final File outDir,final String prefix) {
			super(ChalkBoard.class.getSimpleName());
			setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
			this.saveDir = outDir;
			this.prefix = prefix;
			final JPanel content = new JPanel(new BorderLayout());
			setContentPane(content);
			final JPanel top = new JPanel(new FlowLayout(FlowLayout.LEADING));
			content.add(top,BorderLayout.NORTH);
			top.add(this.selectColorPane=new SelectColorPane());
			top.add(this.selectPenSize=new SelectPenSize());
			top.add(new JButton(new AbstractAction("<") {
				@Override
				public void actionPerformed(ActionEvent e)
					{
					if(slide_index>0)  slide_index--;
					drawingArea.repaint();
					}
				}));
			top.add(new JButton(new AbstractAction(">") {
				@Override
				public void actionPerformed(ActionEvent e)
					{
					if(slide_index+1<slideShow.slides.size())  slide_index++;
					drawingArea.repaint();
					}
				}));
			top.add(new JButton(new AbstractAction("+") {
				@Override
				public void actionPerformed(ActionEvent e)
					{
					pushSlide();
					}
				}));
			
			top.add(new JButton(new AbstractAction("x") {
				@Override
				public void actionPerformed(ActionEvent e)
					{
					final Slide slide= slideShow.slides.get(slide_index);
					if(slide.shapes.isEmpty()) return;
					slide.shapes.remove(slide.shapes.size()-1);
					drawingArea.repaint();
					}
				}));
			
			final AbstractAction actionSave = new AbstractAction("Save") {
				@Override
				public void actionPerformed(ActionEvent e)
					{
					doMenuSave(saveDir);
					}
				};
			top.add(new JButton(actionSave));
			
			this.drawingArea=new JPanel(null) {
				@Override
				protected void paintComponent(Graphics g)
					{
					paintDrawingArea(Graphics2D.class.cast(g));
					}
				};
			this.drawingArea.setBackground(Color.WHITE);
			this.drawingArea.setOpaque(true);
		
			addWindowListener(new WindowAdapter()
				{
				@Override
				public void windowClosing(WindowEvent e)
					{
					if(saveDir==null) {
						doMenuSaveAs();
					} else
						{
						doMenuSave(saveDir);
						}
					setVisible(false);
					dispose();
					}
				@Override
				public void windowOpened(WindowEvent e) {
					pushSlide();
					drawingArea.requestFocus();
					drawingArea.repaint();
					}
				@Override
				public void windowClosed(WindowEvent e)
					{
					doMenuQuit();
					}
				});
			
			
			content.add(this.drawingArea, BorderLayout.CENTER);
			final MouseAdapter mouse = new MouseAdapter()
				{
				final List<Point> points =new ArrayList<>();
				MyStyle style = null;
				@Override
				public void mousePressed(MouseEvent e)
					{
					style = getCurrentStyle();
					style.for_text = false;
					points.clear();
					points.add(e.getPoint());
					drawingArea.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
					}
				@Override
				public void mouseDragged(MouseEvent e)
					{
					if(points.size()>0) {
						final Graphics2D g = (Graphics2D)drawingArea.getGraphics();
						final Point p=points.get(points.size()-1);
						style.apply(g);
						g.drawLine(p.x, p.y,e.getX(),e.getY());
						points.add(e.getPoint());
						}
					}
				@Override
				public void mouseReleased(MouseEvent e)
					{
					drawingArea.setCursor(Cursor.getDefaultCursor());
					if(points.size()<2) return;
					final Slide slide = slideShow.slides.get(slide_index);
					MyPolyLine shape = new MyPolyLine();
					shape.style = slide.getSyle(this.style);
					shape.points.addAll(points);
					slide.shapes.add(shape);
					drawingArea.repaint();
					style=null;
					points.clear();
					}
				@Override
				public void mouseClicked(MouseEvent e) {
					XFrame.this.lastClick = new Point(e.getX(),e.getY());
					}
				};
			this.drawingArea.addMouseListener(mouse);
			this.drawingArea.addMouseMotionListener(mouse);
			
			final KeyListener keyListener = new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					switch(e.getKeyCode())
						{
						case KeyEvent.VK_UP:
						case KeyEvent.VK_KP_UP: translate(0,-1,e.isControlDown());return;
						case KeyEvent.VK_DOWN:
						case KeyEvent.VK_KP_DOWN: translate(0,1,e.isControlDown());return;
						case KeyEvent.VK_LEFT:
						case KeyEvent.VK_KP_LEFT: translate(-1,0,e.isControlDown());return;
						case KeyEvent.VK_RIGHT:
						case KeyEvent.VK_KP_RIGHT: translate(1,0,e.isControlDown());return;
						}
					if(!Character.isISOControl(e.getKeyChar()) || e.getKeyChar()=='\n') {
						appendCharOrCreateText(e.getKeyChar());
					}
					
					
					if(e.isControlDown()) {
						switch(e.getKeyCode()) {
							case KeyEvent.VK_S: {
								doMenuSave(saveDir);
								break;
								}
							case KeyEvent.VK_Z: {
								final Slide slide= slideShow.slides.get(slide_index);
								if(slide.shapes.isEmpty()) return;
								slide.shapes.remove(slide.shapes.size()-1);
								drawingArea.repaint();
								break;
								}
							case KeyEvent.VK_Q:{
								doMenuSave(saveDir);
								doMenuQuit();
								break;
								}
							}
						return;
						}
					
					}	
				};
			
			drawingArea.addKeyListener(keyListener);
			drawingArea.setFocusable(true);
			}
		
		private void translate(int dx,int dy,boolean accelerator) {
			final Slide slide =  this.slideShow.slides.get(this.slide_index);
			if(slide.shapes.isEmpty()) return;
			int x=accelerator?10:1;
			slide.shapes.get(slide.shapes.size()-1).translate(
				dx*x,
				dy*x
				);
			drawingArea.repaint();
			}
		
		private void appendCharOrCreateText(char c) {
			System.err.println("c="+c+" "+lastClick);
			if(this.lastClick==null) return;
			if(c=='\n') {
				this.lastClick.y+= this.selectPenSize.getValue()+1;
				return;
				}
		
			final Slide slide =  this.slideShow.slides.get(this.slide_index);
			MyText text = null;
			
			if(!slide.isEmpty()) {
				final MyShape shape=slide.getLast();
				if(shape.isText()) {
					MyText atext = MyText.class.cast(shape);
					if(atext.point.equals(this.lastClick)) {
						text = atext;
						}
					}
				}
			if(text==null) {
				final MyStyle style = getCurrentStyle();
				style.for_text = true;
				text = new MyText();
				text.point = new Point(this.lastClick);
				text.style = slide.getSyle(style);
				slide.shapes.add(text);
				}
			text.sb.append(c);
			drawingArea.repaint();
			}
		
		private void paintDrawingArea(final Graphics2D g) {
			g.setColor(Color.WHITE);
			g.fillRect(0,0,drawingArea.getWidth(),drawingArea.getHeight());
			if(slide_index<0 || slide_index>=this.slideShow.slides.size()) {
				return;
				}
			final Slide slide =  this.slideShow.slides.get(this.slide_index);
			slide.paint(g);
			}
		private void doMenuSave(File dir) {
			if(dir==null) {
				doMenuSaveAs();
				return;
				}
			try {
				final XMLOutputFactory xof = XMLOutputFactory.newFactory();
				for(int i=0;i< slideShow.slides.size();i++) {
					final File f = new File(dir, String.format("%s%03d.svg",this.prefix, (i+1)));
					try(PrintWriter w = new PrintWriter(f,"UTF-8")) {
						final XMLStreamWriter wc=xof.createXMLStreamWriter(w);
						wc.writeStartDocument("UTF-8","1.0");
						slideShow.slides.get(i).write(wc);
						wc.writeEndDocument();
						wc.flush();
						w.flush();
						}
					}
				final File f = new File(dir,this.prefix+"index.html");
				try(PrintWriter w = new PrintWriter(f,"UTF-8")) {
					final XMLStreamWriter wc=xof.createXMLStreamWriter(w);
					wc.writeStartElement("html");
					wc.writeStartElement("body");
					wc.writeStartElement("div");
					for(int i=0;i< slideShow.slides.size();i++) {
						wc.writeStartElement("div");
						slideShow.slides.get(i).write(wc);
						wc.writeEndElement();//div
						}
					wc.writeEndElement();//div
					wc.writeEndElement();//body
					wc.writeEndElement();//html
					wc.flush();					
					w.flush();
					}
				this.saveDir = dir;
				}
			catch(final Throwable err) {
				err.printStackTrace();
				JOptionPane.showMessageDialog(this, err.getMessage());
				}
			}
		private boolean doMenuSaveAs() {
			final JFileChooser fc = new JFileChooser();
			fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			if(fc.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) {
				return false;
				}
			File d=fc.getSelectedFile();
			if(d==null) return false;
			doMenuSave(d);
			return true;
			}
		private void doMenuQuit() {
			this.setVisible(false);
			this.dispose();
			}
		
		private void pushSlide() {
			final Slide s = new Slide(new Dimension(this.drawingArea.getSize()));
			
			this.slideShow.slides.add(s);
			this.slide_index= this.slideShow.slides.size()-1;
			s.title="Slide " + this.slideShow.slides.size();
			this.drawingArea.repaint();
			}
		private MyStyle getCurrentStyle() {
			MyStyle st = new MyStyle();
			st.for_text = false;
			st.className = "x";
			st.color = this.selectColorPane.getValue();
			st.lineSize = this.selectPenSize.getValue();
			return st;
			}
		}
	
	@Override
	public int doWork(List<String> args)
		{
		final XFrame f=new XFrame(this.outputDir,this.prefix);
		final Dimension d=Toolkit.getDefaultToolkit().getScreenSize();
		SwingUtilities.invokeLater(()->{
			f.setBounds(30, 30, d.width-60, d.height-60);
			f.setVisible(true);
			});
		return 0;
		}
	
	public static ProgramDescriptor getProgramDescriptor() {
    	return new ProgramDescriptor() {
    		@Override
    		public String getName() {
    			return "chalkboard";
    			}
    		};
    	}
	
	public static void main(String[] args)
		{
		new ChalkBoard().instanceMain(args);
		}
	}
