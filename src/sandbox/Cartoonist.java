import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Paint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.geom.GeneralPath;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.border.EmptyBorder;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.lindenb.awt.ColorUtils;
import org.lindenb.io.PreferredDirectory;
import org.lindenb.lang.ThrowablePane;
import org.lindenb.swing.SwingUtils;
import org.lindenb.swing.layout.InputLayout;



/**
 * Cartoonist
 * @author pierre
 *
 */
@SuppressWarnings("serial")
public class Cartoonist
	extends JFrame
	{
	private JPanel drawingArea;
	private JScrollPane scrollPane;
	private double zoom=1.0;
	private Figure selected=null;
	private Drag dragSelected=null;
	private Point mousePrev=null;
	private JMenu menuImageSource;
	private DefaultModel model;
	
	/**
	 * Graphic Context
	 */
	private class GC
		{
		Graphics2D g;
		}
	
	private static class BorderStyle
		{
		float lineHeight=1;
		Color color=Color.BLACK;
		float opacity=1f;
		
		void paint(GC gc,Shape shape)
			{
			if(this.color==null || lineHeight==0) return;
			Stroke oldstroke=gc.g.getStroke();
			Composite oldComposite = gc.g.getComposite();
			gc.g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,this.opacity));
			gc.g.setColor(this.color);
			gc.g.setStroke(new BasicStroke(this.lineHeight));
			gc.g.draw(shape);
			gc.g.setComposite(oldComposite);
			gc.g.setStroke(oldstroke);
			}
		
		void write(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeStartElement("border");
			
			w.writeStartElement("height");
			w.writeCharacters(String.valueOf(lineHeight));
			w.writeEndElement();
			
			w.writeStartElement("color");
			w.writeCharacters(ColorUtils.toRGB(color));
			w.writeEndElement();
			
			w.writeStartElement("opacity");
			w.writeCharacters(String.valueOf(opacity));
			w.writeEndElement();
			
			w.writeEndElement();
			}
		
		void read(StartElement e,XMLEventReader reader)
			throws XMLStreamException,IOException
			{
			while(reader.hasNext())
				{
				XMLEvent evt=reader.nextEvent();
				
				if(evt.isStartElement())
					{
					String localName=evt.asStartElement().getName().getLocalPart();
					if(localName.equals("height"))
						{
						this.lineHeight=Float.parseFloat(reader.getElementText());
						}
					else if(localName.equals("color"))
						{
						this.color= ColorUtils.parseColor(reader.getElementText());
						}
					else if(localName.equals("opacity"))
						{
						this.opacity= Float.parseFloat(reader.getElementText());
						}
					}
				else if(evt.isEndElement())
					{
					String localName=evt.asEndElement().getName().getLocalPart();
					if(localName.equals("border")) break;
					}
				}
			}
		
		}
	
	private static class ColorButton
		extends JButton
		{
		private Color color;
		ColorButton()
			{
			this(Color.BLACK);
			}
		ColorButton(Color color)
			{
			super();
			this.color=color;
			this.addActionListener(new ActionListener()
				{
				@Override
				public void actionPerformed(ActionEvent e)
					{
					JColorChooser chooser=new JColorChooser(getSelectedColor());
					if(JOptionPane.showConfirmDialog(ColorButton.this, chooser,"Choose Color",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,null)!=JOptionPane.OK_OPTION)
						{
						return;
						}
					ColorButton.this.color=chooser.getColor();
					}
				});
			makeIcon();
			}
		Color getSelectedColor()
			{
			return this.color;
			}
		
		private void makeIcon()
			{
			this.setIcon(new Icon()
				{
				@Override
				public void paintIcon(Component c, Graphics g, int x, int y)
					{
					g.setColor(getSelectedColor());
					g.fillRect(x, y, getIconWidth(), getIconHeight());
					}
				
				@Override
				public int getIconWidth()
					{
					return 32;
					}
				
				@Override
				public int getIconHeight()
					{
					return 32;
					}
				});
			}
		}
	
	
	/**
	 * ImageInstanceEditor
	 *
	 */
	class ImageInstanceEditor
		extends JDialog
		{
		/**
		 * 
		 * HotSpot
		 *
		 */
		private class HotSpot
			{
			private double x;
			private double y;
			
			HotSpot(double x,double y)
				{
				this.x=x;
				this.y=y;
				}
			
			private int getIndex()
				{
				for(int i=0;i< getEditor().hotspots.size();++i)
					{
					if(getEditor().hotspots.get(i)==this) return i;
					}
				return -1;
				}
			public double getX()
				{
				return this.x*getEditor().zoom;
				}
			public double getY()
				{
				return this.y*getEditor().zoom;
				}
			
			public ImageInstanceEditor getEditor()
				{
				return ImageInstanceEditor.this;
				}
			@Override
			public String toString()
				{
				return "("+getX()+","+getY()+")";
				}
			}
		private List<HotSpot> hotspots=new Vector<HotSpot>();
		private HotSpot selectedSpot=null;
		private double zoom=1f;
		private JPanel	paintArea;
		private ImageSource	source;
		private Point mousePrev=null;
		private Image scaledImage=null;
		private ImageInstance instance;
		private JScrollPane scrollPane=null;
		private int exitStatus=JOptionPane.CANCEL_OPTION;
		
		public ImageInstanceEditor(ImageSource	source)
			{
			this(source,null);
			}
		
		public ImageInstanceEditor(ImageInstance instance)
			{
			this(instance.source,instance);
			}
			
		private ImageInstanceEditor(
				ImageSource	source,
				ImageInstance instance
				)
			{
			super(Cartoonist.this,true);
			setTitle("Instance Editor");
			setModal(true);
			this.source=source;
			this.scaledImage=this.source.image;
			this.instance=instance;
			if(this.instance==null)
				{
				int w=this.source.image.getWidth(null);
				int h=this.source.image.getHeight(null);
				this.instance=new ImageInstance();
				this.instance.source=source;
				this.hotspots.add(new HotSpot(0,0));
				this.hotspots.add(new HotSpot(w,0));
				this.hotspots.add(new HotSpot(w,h));
				this.hotspots.add(new HotSpot(0,h));
				}
			else
				{
				for(Point2D pt: instance.polygon)
					{
					this.hotspots.add(new HotSpot(pt.getX(),pt.getY()));
					}
				}
			
			JMenuBar bar=new JMenuBar();
			this.setJMenuBar(bar);
			JMenu menu=new JMenu("File");
			bar.add(menu);
			
			JPanel panel=new JPanel(new BorderLayout());
			panel.setBorder(new EmptyBorder(5, 5, 5, 5));
			this.setContentPane(panel);
			
			JToolBar toolBar=new JToolBar();
			panel.add(toolBar,BorderLayout.NORTH);
			AbstractAction action=new AbstractAction("[+]")
					{
					@Override
					public void actionPerformed(ActionEvent e)
						{
						ImageInstanceEditor.this.doZoom(ImageInstanceEditor.this.zoom*1.05f);
						}
				};
			toolBar.add(action);
			action=new AbstractAction("[-]")
				{
				@Override
				public void actionPerformed(ActionEvent e)
					{
					ImageInstanceEditor.this.doZoom(ImageInstanceEditor.this.zoom*0.95f);
					}
				};
			toolBar.add(action);
			action=new AbstractAction("View")
				{
				@Override
				public void actionPerformed(ActionEvent e)
					{
					Shape shape=getPath(1f);
					Rectangle bounds=shape.getBounds();
					BufferedImage img=new BufferedImage(bounds.width, bounds.height, BufferedImage.TYPE_INT_RGB);
					Graphics2D g=(Graphics2D)img.getGraphics();
					g.setColor(Color.WHITE);
					g.fillRect(0, 0, bounds.width, bounds.height);
					AffineTransform tr2=AffineTransform.getTranslateInstance(-bounds.x,-bounds.y);
					Shape shape2=tr2.createTransformedShape(shape);
					Shape oldclip=g.getClip();
					g.setClip(shape2);
					g.drawImage(ImageInstanceEditor.this.source.image,-bounds.x,-bounds.y,null);
					g.setClip(oldclip);
					g.setColor(Color.BLACK);
					g.draw(shape2);
					
					JOptionPane.showMessageDialog(null, new JLabel(new ImageIcon(img)));
					}
				};
			toolBar.add(action);
				
			this.paintArea=new JPanel(null,true)
				{
				@Override
				protected void paintComponent(Graphics g1)
					{
					super.paintComponent(g1);
					paintArea(Graphics2D.class.cast(g1));
					}
				};
			this.paintArea.setOpaque(true);
			this.paintArea.setBackground(Color.BLACK);
			MouseAdapter mouse=new MouseAdapter()
				{
				@Override
				public void mousePressed(MouseEvent e)
					{
					selectedSpot= findHotSpotAt(e.getX(),e.getY());
					if(selectedSpot!=null && e.isControlDown())
						{
						int n=selectedSpot.getIndex();
						selectedSpot=new HotSpot(selectedSpot.x, selectedSpot.y);
						hotspots.add(n, selectedSpot);
						}
					mousePrev=null;
					}
				@Override
				public void mouseDragged(MouseEvent e)
					{
					if(selectedSpot==null ) return;	
					if(mousePrev!=null)
						{
						double dx=(e.getX()-mousePrev.getX())/zoom;
						double dy=(e.getY()-mousePrev.getY())/zoom;
						selectedSpot.x=trim(selectedSpot.x+dx,ImageInstanceEditor.this.source.image.getWidth(paintArea));
						selectedSpot.y=trim(selectedSpot.y+dy,ImageInstanceEditor.this.source.image.getHeight(paintArea));
						paintArea.repaint();
						}
					mousePrev=new Point(e.getX(),e.getY());	
					}
				
				@Override
				public void mouseReleased(MouseEvent e)
					{
					selectedSpot=null;
					mousePrev=null;
					paintArea.repaint();
					}
				};
			this.paintArea.addMouseListener(mouse);
			this.paintArea.addMouseMotionListener(mouse);
			this.scrollPane=new JScrollPane(this.paintArea);
			Dimension dim=new Dimension(
				this.scaledImage.getWidth(null),
				this.scaledImage.getHeight(null)
				);
			this.paintArea.setSize(dim);
			this.paintArea.setPreferredSize(dim);
			this.paintArea.setMinimumSize(dim);
			panel.add(this.scrollPane,BorderLayout.CENTER);
			
			JPanel bot=new JPanel(new FlowLayout(FlowLayout.TRAILING));
			panel.add(bot,BorderLayout.SOUTH);
			action=new AbstractAction("OK")
				{
				@Override
				public void actionPerformed(ActionEvent e)
					{
					ImageInstanceEditor.this.exitStatus=JOptionPane.OK_OPTION;
					ImageInstanceEditor.this.setVisible(false);
					ImageInstanceEditor.this.dispose();
					ImageInstanceEditor.this.instance.polygon.clear();
					for(HotSpot h: ImageInstanceEditor.this.hotspots)
						{
						ImageInstanceEditor.this.instance.polygon.add(
							new Point2D.Double(h.x,h.y)
							);
						}
					}
				};
			bot.add(new JButton(action));
			
			action=new AbstractAction("Cancel")
				{
				@Override
				public void actionPerformed(ActionEvent e)
					{
					ImageInstanceEditor.this.exitStatus=JOptionPane.CANCEL_OPTION;
					ImageInstanceEditor.this.setVisible(false);
					ImageInstanceEditor.this.dispose();
					}
				};
			bot.add(new JButton(action));
			}
		
		private void doZoom(double newzoom)
			{
			if(this.zoom==newzoom) return;
			this.zoom=newzoom;
			
			Dimension d=new Dimension(
					(int)(this.source.image.getWidth(this.paintArea)*newzoom),
					(int)(this.source.image.getHeight(this.paintArea)*newzoom)
					);
			
			this.scaledImage=this.source.image.getScaledInstance(
				d.width,d.height,0);
			
			this.paintArea.setSize(d);
			this.paintArea.setPreferredSize(d);

			this.paintArea.revalidate();
			this.scrollPane.revalidate();
			this.paintArea.repaint();
			}
		
		
		private double trim(double v,int max)
			{
			return Math.max(Math.min(v, max), 0);
			}
		
		private void paintArea(Graphics2D g)
			{
			
			g.drawImage(
				this.scaledImage,
				0,
				0,
				this.paintArea);
			Stroke stroke=g.getStroke();
			g.setStroke(new BasicStroke(2f));
			g.setXORMode(this.paintArea.getBackground());
			g.setColor(Color.RED);
			g.draw(getPath(this.zoom));
			g.setPaintMode();
			for(HotSpot spot:this.hotspots)
				{
				g.setColor(spot==selectedSpot?Color.BLUE:Color.RED); 
				g.drawRect((int)spot.getX()-3, (int)spot.getY()-3, 7, 7);
				}
			g.setStroke(stroke);
			}
		
		private HotSpot findHotSpotAt(int x,int y)
			{
			for(HotSpot h:this.hotspots)
				{
				if(Point2D.distance(x, y, h.getX(), h.getY())<5)
					{
					return h;
					}
				}
			return null;
			}
		
		private Shape getPath(double scaled)
			{
			GeneralPath path=new GeneralPath();
			for(int i=0;i<this.hotspots.size();++i)
				{
				HotSpot spot=this.hotspots.get(i);
				if(i==0)
					{
					path.moveTo(spot.x*scaled, spot.y*scaled);
					}
				else
					{
					path.lineTo(spot.x*scaled, spot.y*scaled);
					}
				}
			path.closePath();
			return path;
			}
		
		}
	
	/**
	 * Drawable
	 */
	static abstract class Drawable
		{
		int id=0;
		public int getId()
			{
			return id;
			}
		abstract void write(XMLStreamWriter w) throws XMLStreamException;
		}
	/**
	 * ImageInstance
	 */
	static class TextBox
		extends Drawable
		{
		private Rectangle2D.Double viewRect=new Rectangle2D.Double();
		private String text="";
		
		
		public double getWidth()
			{
			return this.viewRect.getWidth();
			}
		
		public double getHeight()
			{
			return this.viewRect.getHeight();
			}
		
		public double getX()
			{
			return this.viewRect.getX();
			}
		
		public double getY()
			{
			return this.viewRect.getY();
			}
		
		void read(StartElement e,XMLEventReader reader)
		throws XMLStreamException,IOException
			{
			this.id=Integer.parseInt(e.getAttributeByName(new QName("id")).getValue());
			
			while(reader.hasNext())
				{
				XMLEvent evt=reader.nextEvent();
				
				if(evt.isStartElement())
					{
					String localName=evt.asStartElement().getName().getLocalPart();
					if(localName.equals("text"))
						{
						this.text=reader.getElementText();
						}
					else if(localName.equals("x"))
						{
						this.viewRect.x= Double.parseDouble(reader.getElementText());
						}
					else if(localName.equals("y"))
						{
						this.viewRect.y= Double.parseDouble(reader.getElementText());
						}
					else if(localName.equals("width"))
						{
						this.viewRect.width= Double.parseDouble(reader.getElementText());
						}
					else if(localName.equals("height"))
						{
						this.viewRect.height= Double.parseDouble(reader.getElementText());
						}
					}
				else if(evt.isEndElement())
					{
					String localName=evt.asEndElement().getName().getLocalPart();
					if(localName.equals("TextBox")) break;
					}
				}
			}
		
		@Override
		void write(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeStartElement("TextBox");
			w.writeAttribute("id", String.valueOf(getId()));
			
			w.writeStartElement("text");
			w.writeCharacters(this.text);
			w.writeEndElement();
			
			w.writeStartElement("x");
			w.writeCharacters(String.valueOf(getX()));
			w.writeEndElement();
			
			w.writeStartElement("y");
			w.writeCharacters(String.valueOf(getY()));
			w.writeEndElement();
			
			w.writeStartElement("width");
			w.writeCharacters(String.valueOf(getWidth()));
			w.writeEndElement();
			
			w.writeStartElement("height");
			w.writeCharacters(String.valueOf(getHeight()));
			w.writeEndElement();

			
			w.writeEndElement();
			}
		}
	/**
	 * ImageInstance
	 */
	static class ImageInstance
		extends Drawable
		{
		private BorderStyle border=new BorderStyle();
		int imageSourceId=-1;
		ImageSource source=null;
		List<Point2D> polygon=new ArrayList<Point2D>();
		private Rectangle2D.Double viewRect=new Rectangle2D.Double();
		
		ImageInstance()
			{
			}
		
		private Shape getShape()
			{
			GeneralPath path=new GeneralPath();
			for(int i=0;i<this.polygon.size();++i)
				{
				Point2D spot=this.polygon.get(i);
				if(i==0)
					{
					path.moveTo(spot.getX(), spot.getY());
					}
				else
					{
					path.lineTo(spot.getX(), spot.getY());
					}
				}
			path.closePath();
			return path;
			}
		
		public ImageSource getImageSource()
			{
			return source;
			}
		
		
		
		
		
		public double getWidth()
			{
			return this.viewRect.getWidth();
			}
		
		public double getHeight()
			{
			return this.viewRect.getHeight();
			}
		
		public double getX()
			{
			return this.viewRect.getX();
			}
		
		public double getY()
			{
			return this.viewRect.getY();
			}
		
		public double getWHRatio()
			{
			return getWidth()/getHeight();
			}
		
		@Override
		void write(XMLStreamWriter w)
		throws XMLStreamException
			{
			w.writeStartElement("ImageInstance");
			w.writeAttribute("id", String.valueOf(getId()));
			w.writeAttribute("source", String.valueOf(getImageSource().getId()));
			
			w.writeStartElement("points");
			w.writeAttribute("width", String.valueOf(getShape().getBounds2D().getWidth()));
			w.writeAttribute("height", String.valueOf(getShape().getBounds2D().getHeight()));
			for(int i=0;i< polygon.size();++i)
				{
				if(i>0) w.writeCharacters(" ");
				w.writeCharacters(String.valueOf(polygon.get(i).getX()));
				w.writeCharacters(",");
				w.writeCharacters(String.valueOf(polygon.get(i).getY()));
				}
			w.writeEndElement();
			
			w.writeStartElement("x");
			w.writeCharacters(String.valueOf(getX()));
			w.writeEndElement();
			
			w.writeStartElement("y");
			w.writeCharacters(String.valueOf(getY()));
			w.writeEndElement();
			
			w.writeStartElement("width");
			w.writeCharacters(String.valueOf(getWidth()));
			w.writeEndElement();
			
			w.writeStartElement("height");
			w.writeCharacters(String.valueOf(getHeight()));
			w.writeEndElement();
			
			this.border.write(w);
			
			w.writeEndElement();
			}
	
		void read(StartElement e,XMLEventReader reader)
			throws XMLStreamException,IOException
			{
			this.id=Integer.parseInt(e.getAttributeByName(new QName("id")).getValue());
			this.imageSourceId= Integer.parseInt(e.getAttributeByName(new QName("source")).getValue());
			
			while(reader.hasNext())
				{
				XMLEvent evt=reader.nextEvent();
				
				if(evt.isStartElement())
					{
					String localName=evt.asStartElement().getName().getLocalPart();
					if(localName.equals("points"))
						{
						for(String s:reader.getElementText().split("[ \t\r]+"))
							{
							int n=s.indexOf(",");
							polygon.add(new Point2D.Double(
								Double.parseDouble(s.substring(0, n)),
								Double.parseDouble(s.substring(n+1))
								));
							}
						}
					else if(localName.equals("x"))
						{
						this.viewRect.x= Double.parseDouble(reader.getElementText());
						}
					else if(localName.equals("y"))
						{
						this.viewRect.y= Double.parseDouble(reader.getElementText());
						}
					else if(localName.equals("width"))
						{
						this.viewRect.width= Double.parseDouble(reader.getElementText());
						}
					else if(localName.equals("height"))
						{
						this.viewRect.height= Double.parseDouble(reader.getElementText());
						}
					else if(localName.equals("border"))
						{
						this.border.read(e, reader);
						}
					}
				else if(evt.isEndElement())
					{
					String localName=evt.asEndElement().getName().getLocalPart();
					if(localName.equals("ImageInstance")) break;
					}
				}
			}
		
		}
	
	/**
	 * Image Source
	 *
	 */
	static class ImageSource
		{
		int id;
		Image image;
		Icon icon64;
		URL imageUrl;
		URL imagePage;
		int width=-1;
		int height=-1;
		
		
		public Image getImage()
			{
			return image;
			}
		
		public URL getImageUrl()
			{
			return imageUrl;
			}
		public URL getImagePage()
			{
			return imagePage;
			}
		
		public int getWidth()
			{
			return this.width;
			}
		
		public int getHeight()
			{
			return this.width;
			}
		public int getId()
			{
			return id;
			}
		
		void write(XMLStreamWriter w)
			throws XMLStreamException
			{
			w.writeStartElement("ImageSource");
			w.writeAttribute("id", String.valueOf(getId()));
			
			w.writeStartElement("url");
			w.writeCharacters(String.valueOf(getImageUrl()));
			w.writeEndElement();
			
			if(getImagePage()!=null)
				{
				w.writeStartElement("pageSrc");
				w.writeCharacters(String.valueOf(getImagePage()));
				w.writeEndElement();
				}
			w.writeStartElement("height");
			w.writeCharacters(String.valueOf(getHeight()));
			w.writeEndElement();
			
			w.writeStartElement("width");
			w.writeCharacters(String.valueOf(getWidth()));
			w.writeEndElement();
			
			w.writeEndElement();
			}
		
		void read(StartElement e,XMLEventReader reader)
			throws XMLStreamException,IOException
			{
			this.id=Integer.parseInt(e.getAttributeByName(new QName("id")).getValue());
			while(reader.hasNext())
				{
				XMLEvent evt=reader.nextEvent();
				
				if(evt.isStartElement())
					{
					String localName=evt.asStartElement().getName().getLocalPart();
					if(localName.equals("url"))
						{
						this.imageUrl=new URL(reader.getElementText());
						}
					else if(localName.equals("pageSrc"))
						{
						this.imagePage=new URL(reader.getElementText());
						}
					else if(localName.equals("width"))
						{
						this.width=Integer.parseInt(reader.getElementText());
						}
					else if(localName.equals("height"))
						{
						this.height=Integer.parseInt(reader.getElementText());
						}
					}
				else if(evt.isEndElement())
					{
					String localName=evt.asEndElement().getName().getLocalPart();
					if(localName.equals("ImageSource")) break;
					}
				}
			}
		}
	
	
	/**
	 * Model
	 */
	static class Model
		{
		private List<Figure> figures=new Vector<Figure>();
		private Model parent=null;
		private Model getParent()
			{
			return this.parent;
			}
		
		public Model()
			{
			}
		
		public List<Figure> getFigures()
			{
			return this.figures;
			}
		public int getIndexOf(Figure f)
			{
			for(int i=0;i< this.figures.size();++i)
				{
				if(this.figures.get(i)==f) return i;
				}
			return -1;
			}
		protected Figure findFigureAt(int x,int y)
			{
			for(int i=this.getFigures().size()-1;
				i>=0;--i)
				{
				Figure t=this.getFigures().get(i);
				if(t.contains(x, y)) return t;
				}
			return null;
			}
		
		protected Drag findDragAt(int x,int y)
			{
			for(int i=this.getFigures().size()-1;
				i>=0;--i)
				{
				Figure t=this.getFigures().get(i);
				for(Drag d: t.getDrags())
					{
					if(Point2D.distance(x,y,d.getX(),d.getY())<5)
						{
						return d;
						}
					}
				}
			return null;
			}
		
		void paint(GC gc)
			{
			for(Figure thumb: this.figures)
				{
				thumb.paint(gc);
				}
			}
		}
	
	/**
	 * 
	 * DefaultModel
	 *
	 */
	static class DefaultModel extends Model
		{
		private File fileSaveAs=null;
		private boolean documentModified=false;
		private List<ImageSource> imageSources=new Vector<ImageSource>();
		private List<Drawable> drawables=new Vector<Drawable>();
		
		public DefaultModel()
			{
			}
		public String getTitle()
			{
			return fileSaveAs==null?"Untitled Document":fileSaveAs.getName();
			}
		
		void read(File file) throws IOException,XMLStreamException
			{
			XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
			xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
			xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
			xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
			XMLEventReader reader= xmlInputFactory.createXMLEventReader(new FileReader(file));
			while(reader.hasNext())
				{
				XMLEvent evt=reader.nextEvent();
				if(evt.isStartElement())
					{
					StartElement e=evt.asStartElement();
					String localName=e.getName().getLocalPart();
					if(localName.equals("Cartoon")) continue;
					else if(localName.equals("ImageSource"))
						{
						ImageSource imgsrc=new ImageSource();
						imgsrc.read(e,reader);
						this.imageSources.add(imgsrc);
						}
					else if(localName.equals("ImageInstance"))
						{
						ImageInstance instance=new ImageInstance();
						instance.read(e,reader);
						this.drawables.add(instance);
						}
					else if(localName.equals("TextBox"))
						{
						TextBox box=new TextBox();
						box.read(e,reader);
						this.drawables.add(box);
						}
					}
				else if(evt.isEndElement())
					{
					
					}
				}
			reader.close();
			
			Component observer=new JPanel();
			MediaTracker mediaTracker=new MediaTracker(observer);
		
			for(ImageSource img: imageSources)
				{
				img.image=Toolkit.getDefaultToolkit().getImage(img.getImageUrl());
				mediaTracker.addImage(img.image,img.id);
				for(Drawable drawable: drawables)
					{
					if(!(drawable instanceof ImageInstance)) continue;
					ImageInstance instance=ImageInstance.class.cast(drawable);
					if(instance.imageSourceId==img.id)
						{
						instance.source=img;
						}
					}
				}
			
			for(Drawable drawable: drawables)
				{
				if(!(drawable instanceof ImageInstance)) continue;
				ImageInstance instance=ImageInstance.class.cast(drawable);
				if(instance.source==null) throw new IOException("cannot find image-id");
				}
			
			try
				{
				mediaTracker.waitForAll();
				}
			catch(InterruptedException err)
				{
				throw new IOException(err);
				}
			for(ImageSource img: imageSources)
				{
				img.width=img.image.getWidth(observer);
				img.height=img.image.getHeight(observer);
				img.icon64= new ImageIcon(img.image.getScaledInstance(64, 64, 0));
				}
			this.fileSaveAs=file;
			this.documentModified=false;
			}
		
		void write(XMLStreamWriter w)
			throws XMLStreamException
			{
			w.writeStartElement("Cartoon");
			for(ImageSource img: imageSources) img.write(w);
			for(Drawable d: this.drawables) d.write(w);
			w.writeEndElement();
			}
		
		void write(File file)
			throws XMLStreamException,IOException
			{
			XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
			XMLStreamWriter w= xmlfactory.createXMLStreamWriter(new BufferedWriter(new FileWriter(file)));
			w.writeStartDocument("UTF-8","1.0");
			write(w);
			w.writeEndDocument();
			w.flush();
			w.close();
			}
		
		public int newId()
			{
			int id=1;
			boolean found=false;
			while(!found)
				{
				found=true;
				for(Drawable img: this.drawables)
					{
					if(img.id==id)
						{
						id++;
						found=false;
						break;
						}
					}
				}
			return id;
			}
		
		}	
	
	/**
	 * 
	 * ImageSourceEditorDialog
	 *
	 */
	private class ImageSourceEditorDialog
		extends JPanel
		{
		private ImageIcon imageIcon;
		private Rectangle viewRect;
		private ImageSourceEditorDialog(URL url)
			{
			super(new BorderLayout());
			Image img= Toolkit.getDefaultToolkit().getImage(url);
			JLabel label=new JLabel(this.imageIcon=new ImageIcon(img))
				{
				protected void paintComponent(Graphics g)
						{
						
						};
				};
			
			
			JScrollPane scroll=new JScrollPane(label);
			scroll.setPreferredSize(new Dimension(400,400));
			add(scroll,BorderLayout.CENTER);
			JPanel bottom=new JPanel(new FlowLayout());
			add(scroll,BorderLayout.SOUTH);
			JTextField tfImageURL=new JTextField();
			AbstractAction action=new AbstractAction("Update URL")
				{
				@Override
				public void actionPerformed(ActionEvent e)
					{
					}
				};
			tfImageURL.addActionListener(action);
			JButton button=new JButton(action);
			}
		}
	
	/**
	 * Drag
	 */
	abstract class Drag
		{
		public abstract double getX(); 
		public abstract double getY();
		public abstract Figure getFigure();
		public abstract void moveTo(int x,int y);
		public Shape getShape()
			{
			return new Rectangle2D.Double(getX()-3,getY()-3,7,7);
			}
		}
	
	
	
	/**
	 * 
	 * Figure
	 *
	 */
	private abstract class Figure
		{
		//private Rectangle2D.Double bounds=null;
		private Drag drags[];
		private Double ratiowh=null;
		
		/** My drag */
		private abstract class MyDrag
			extends Drag
				{
				@Override
				public Figure getFigure()
					{
					return Figure.this;
					}
				}

		/**
		 * TopLeftDrag
		 */
		private class TopLeftDrag
		extends MyDrag
			{
			@Override
			public double getX()
				{
				return getFigure().getViewRect().getX();
				}
			@Override
			public double getY()
				{
				return getFigure().getViewRect().getY();
				}
			@Override
			public void moveTo(int x, int y)
				{
				double dx=(x-getX())/zoom;
				double dy=(y-getY())/zoom;
				if(getFigure().getRatioWH()!=null)
					{
					dy=dx/getFigure().getRatioWH();
					} 
				Rectangle2D bounds= getFigure().getSrcBounds();
				
				if( bounds.getWidth()-dx>0 &&
					bounds.getHeight()-dy>0)
					{
					getFigure().setSrcBounds(
						bounds.getX()+dx,
						bounds.getY()+dy,
						bounds.getWidth()-dx,
						bounds.getHeight()-dy
						);
					}
				}
			}
		
		/** BottomLeftDrag */
		private class BottomLeftDrag
			extends MyDrag
			{
			@Override
			public double getX()
				{
				return getFigure().getViewRect().getX();
				}
			@Override
			public double getY()
				{
				return getFigure().getViewRect().getMaxY();
				}
			@Override
			public void moveTo(int x, int y)
				{
				double dx=(x-getX())/zoom;
				double dy=(y-getY())/zoom;
				if(getFigure().getRatioWH()!=null)
					{
					dy=-dx/getFigure().getRatioWH();
					}
				Rectangle2D bounds= getFigure().getSrcBounds();
				
				if( bounds.getWidth()-dx>0 &&
					bounds.getHeight()+dy>0)
					{
					getFigure().setSrcBounds(
						bounds.getX()+dx,
						bounds.getY(),
						bounds.getWidth()-dx,
						bounds.getHeight()+dy
						);
					}
				}
			}
		
		/** TopRightDrag */
		private class TopRightDrag
			extends MyDrag
			{
			@Override
			public double getX()
				{
				return getFigure().getViewRect().getMaxX();
				}
			@Override
			public double getY()
				{
				return getFigure().getViewRect().getY();
				}
			@Override
			public void moveTo(int x, int y)
				{
				double dx=(x-getX())/zoom;
				double dy=(y-getY())/zoom;
				
				if(getFigure().getRatioWH()!=null)
					{
					dy=-dx/getFigure().getRatioWH();
					}
				
				Rectangle2D bounds= getFigure().getSrcBounds();
				
				if( bounds.getWidth()+dx>0 &&
					bounds.getHeight()-dy>0)
					{
					setSrcBounds(
							bounds.getX(),	
							bounds.getY()+dy,
							bounds.getWidth()+dx,
							bounds.getHeight()-dy
							);
					}
				}
			}		
		
		/** BottomRightDrag */
		private class BottomRightDrag
			extends MyDrag
			{
			@Override
			public double getX()
				{
				return getFigure().getViewRect().getMaxX();
				}
			@Override
			public double getY()
				{
				return getFigure().getViewRect().getMaxY();
				}
			@Override
			public void moveTo(int x, int y)
				{
				double dx=(x-getX())/zoom;
				double dy=(y-getY())/zoom;
				
				if(getFigure().getRatioWH()!=null)
					{
					dy=dx/getFigure().getRatioWH();
					}
				
				Rectangle2D bounds= getFigure().getSrcBounds();
				
				if( bounds.getWidth()+dx>0 &&
					bounds.getHeight()+dy>0)
					{
					setSrcBounds(
							bounds.getX(),	
							bounds.getY(),
							bounds.getWidth()+dx,
							bounds.getHeight()+dy
						);
					}
				}
			}
		
		protected Figure()
			{
			this.drags=new Drag[]{
					new TopLeftDrag(),
					new BottomLeftDrag(),
					new BottomRightDrag(),
					new TopRightDrag()
					};
			}
		
		public void fillPopup(JPopupMenu menu)
			{
			AbstractAction action=new AbstractAction("Lock")
				{
				@Override
				public void actionPerformed(ActionEvent e)
					{
					
					}
				};
			
			menu.add(action);
			action=new AbstractAction("Move Back")
				{
				@Override
				public void actionPerformed(ActionEvent e)
					{
					int index= getModel().getIndexOf(Figure.this);
					if(index<=0 && getModel().getFigures().size()<2) return;	
					getModel().getFigures().remove(index);
					getModel().getFigures().add(0, Figure.this);
					drawingArea.repaint();
					}
				};
			menu.add(action);

			action=new AbstractAction("Move Backward")
				{
				@Override
				public void actionPerformed(ActionEvent e)
					{
					int index= getModel().getIndexOf(Figure.this);
					if(index<=0) return;
					Figure f2= getModel().getFigures().get(index-1);
					getModel().getFigures().set(index, f2);
					getModel().getFigures().set(index-1,Figure.this);
					drawingArea.repaint();
					}
				};
			menu.add(action);			
			
			action=new AbstractAction("Move Forward")
				{
				@Override
				public void actionPerformed(ActionEvent e)
					{
					int index= getModel().getIndexOf(Figure.this);
					if(index+1< getModel().getFigures().size()) return;
					Figure f2= getModel().getFigures().get(index+1);
					getModel().getFigures().set(index, f2);
					getModel().getFigures().set(index+1,Figure.this);
					drawingArea.repaint();
					}
				};
			menu.add(action);
			
			action=new AbstractAction("Move Front")
				{
				@Override
				public void actionPerformed(ActionEvent e)
					{
					int index= getModel().getIndexOf(Figure.this);
					if(getModel().getFigures().size()<2) return;	
					getModel().getFigures().remove(index);
					getModel().getFigures().add(Figure.this);
					drawingArea.repaint();
					}
				};
			menu.add(action);
			}
		
		public abstract Drawable getDrawable();
		
		public Double getRatioWH()
			{
			return this.ratiowh;
			}
		
		public Drag[] getDrags()
			{
			return this.drags;
			}
		
		
		public double getX()
			{
			return (getSrcBounds().getX()*Cartoonist.this.zoom);
			}
		
		public double getY()
			{
			return (getSrcBounds().getY()*Cartoonist.this.zoom);
			}
		
		public double getWidth()
			{
			return (getSrcBounds().getWidth()*Cartoonist.this.zoom);
			}
		
		public double getHeight()
			{
			return (getSrcBounds().getHeight()*Cartoonist.this.zoom);
			}
		
		public Rectangle2D getViewRect()
			{
			return new Rectangle2D.Double(
				getX(),getY(),
				getWidth(),
				getHeight()
				);
			}
		
		public boolean contains(int x,int y)
			{
			return getViewRect().contains(x,y);
			}
		
		public abstract void setSrcBounds(double x,double y,double width,double height);
		public abstract Rectangle2D getSrcBounds();
			
			
		public abstract void paint(GC gc);
		
		}
	
	class DefaultFigure
		extends Figure
		{
		private Rectangle2D.Double rect;
		DefaultFigure(Rectangle2D.Double rect)
			{
			this.rect=rect;
			}
		
		@Override
		public Rectangle2D getSrcBounds()
			{
			return this.rect;
			}
		
		@Override
		public void setSrcBounds(double x,double y,double width,double height)
			{
			this.rect.x=x;
			this.rect.y=y;
			this.rect.width=width;
			this.rect.height=height;
			}
		@Override
		public void paint(GC gc)
			{
			Rectangle2D r=getViewRect();
			gc.g.setColor(Color.WHITE);
			gc.g.fill(r);
			gc.g.setColor(Color.BLACK);
			gc.g.draw(r);
			}
		
		@Override
		public Drawable getDrawable()
			{
			return null;
			}
		
		}
	
	 /**
	  * ImageInstanceFigure
	  */
	 class TextBoxFigure
		extends Figure
		{
		private TextBox tbox;
		TextBoxFigure(TextBox tbox)
			{
			this.tbox=tbox;
			}
		@Override
		public Drawable getDrawable()
			{
			return this.tbox;
			}
		
		@Override
		public Rectangle2D getSrcBounds()
			{
			return new Rectangle2D.Double(
				this.tbox.getX(),
				this.tbox.getY(),
				this.tbox.getWidth(),
				this.tbox.getHeight()
				);
			}
		
		@Override
		public void setSrcBounds(double x, double y, double width, double height)
			{
			tbox.viewRect.x=x;
			tbox.viewRect.y=y;
			tbox.viewRect.width=width;
			tbox.viewRect.height=height;
			}
		
		@Override
		public void paint(GC gc)
			{
			Rectangle2D r=getViewRect();
			gc.g.setColor(Color.WHITE);
			gc.g.fill(r);
			gc.g.setColor(Color.BLUE);
			gc.g.draw(r);
			gc.g.drawString(tbox.text, (int)r.getX()+1, (int)r.getY()+12);
			
			
			}
		}
	
	 /**
	  * ImageInstanceFigure
	  */
	 class ImageInstanceFigure
		extends Figure
		{
		private ImageInstance imageInstance;
		ImageInstanceFigure(ImageInstance imageInstance)
			{
			this.imageInstance=imageInstance;
			}
		
		public ImageInstance getImageInstance()
			{
			return imageInstance;
			}
		
		@Override
		public Double getRatioWH()
			{
			return getImageInstance().getWHRatio();
			}
		
		@Override
		public Rectangle2D getSrcBounds()
			{
			return getImageInstance().viewRect;
			}
		@Override
		public void setSrcBounds(double x, double y, double width, double height)
			{
			imageInstance.viewRect.x=x;
			imageInstance.viewRect.y=y;
			imageInstance.viewRect.width=width;
			imageInstance.viewRect.height=height;
			}
		
		public Shape getViewShape()
			{
			Shape shape=getImageInstance().getShape();
			Rectangle2D bounds=shape.getBounds2D();
			
			double ratioW=((imageInstance.getWidth()/bounds.getWidth()));
			double ratioH=((imageInstance.getHeight()/bounds.getHeight()));
		
			AffineTransform transforms[]=
				{
				AffineTransform.getScaleInstance(zoom, zoom),
				AffineTransform.getTranslateInstance(imageInstance.getX(),imageInstance.getY()),
				AffineTransform.getScaleInstance(ratioW, ratioH),
				AffineTransform.getTranslateInstance(-bounds.getX(),-bounds.getY())
				};
			
			
			
			AffineTransform tr=new AffineTransform();
			for(int i=0;i< transforms.length;++i)
				{
				tr.concatenate(transforms[i]);
				}
			
			return tr.createTransformedShape(shape);
			}
		
		@Override
		public void paint(GC gc)
			{
			Rectangle2D r=getViewRect();
			gc.g.setXORMode(drawingArea.getBackground());
			gc.g.setColor(Color.BLUE);
			gc.g.draw(r);
			gc.g.setPaintMode();
			
			Rectangle2D srcRec=getImageInstance().getShape().getBounds2D();
			Shape viewShape=getViewShape();
			
			Shape oldClip= gc.g.getClip();
			Area newClip= new Area(viewShape);
			newClip.intersect(new Area(oldClip));
			gc.g.setClip(newClip);
			gc.g.drawImage(
				getImageInstance().getImageSource().image,
				(int)r.getX(),
				(int)r.getY(),
				(int)r.getMaxX(),
				(int)r.getMaxY(),
				(int)srcRec.getX(),
				(int)srcRec.getY(),
				(int)srcRec.getMaxX(),
				(int)srcRec.getMaxY(),
				null
				);
			//gc.g.fill(r);
			
			gc.g.setClip(oldClip);
			
			getImageInstance().border.paint(gc, viewShape);
			
			}
		
		@Override
		public Drawable getDrawable()
			{
			return getImageInstance();
			}
		}
	
	/**
	 * Constructor
	 * @param model
	 */
	public Cartoonist(DefaultModel model)
		{
		super("Cartoonist");
		this.model=model;
		for(Drawable d: model.drawables)
			{
			if(d instanceof ImageInstance)
				{
				ImageInstance is=ImageInstance.class.cast(d);
				model.getFigures().add(new ImageInstanceFigure(is));
				}
			else if(d instanceof TextBox)
				{
				TextBox is=TextBox.class.cast(d);
				model.getFigures().add(new TextBoxFigure(is));
				}
			else
				{
				throw new UnsupportedOperationException(d.getClass().getName());
				}
			}
		
		
		this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter()
			{
			@Override
			public void windowOpened(WindowEvent e)
				{
				reloadImageSrcMenu();
				adjustSize();
				}
			@Override
			public void windowClosing(WindowEvent e)
				{
				doMenuQuit();
				}
			});
		
		JPanel contentPane=new JPanel(new BorderLayout());
		setContentPane(contentPane);
		this.drawingArea=new JPanel(null,false)
			{
			@Override
			protected void paintComponent(Graphics g)
				{
				super.paintComponent(g);
				GC gc=new GC();
				gc.g=Graphics2D.class.cast(g);
				paintDrawingArea(gc);
				}
			};
		this.drawingArea.setOpaque(true);
		this.drawingArea.setBackground(Color.LIGHT_GRAY);
		this.scrollPane=new JScrollPane(this.drawingArea);
		contentPane.add(this.scrollPane,BorderLayout.CENTER);
		contentPane.setBorder(new EmptyBorder(5,5,5,5));
		MouseAdapter mouse=new MouseAdapter()
			{
			@Override
			public void mouseClicked(MouseEvent e)
				{
				if(e.getClickCount()>1)
					{
					Figure t= getModel().findFigureAt(e.getX(), e.getY());
					if(t!=null) editFigure(t);
					}
				}
			
			@Override
			public void mousePressed(MouseEvent e)
				{
				dragSelected= getModel().findDragAt(e.getX(), e.getY());
				if(dragSelected!=null)
					{
					selected= dragSelected.getFigure();
					}
				else
					{
					selected= getModel().findFigureAt(e.getX(), e.getY());
					}
				if(e.isPopupTrigger())
					{
					JPopupMenu pop=new JPopupMenu();
					selected.fillPopup(pop);
					pop.show(e.getComponent(), e.getX(), e.getY());
					}
				mousePrev=null;
				}
			
			@Override
			public void mouseDragged(MouseEvent e)
				{
				if(selected==null) return;
				if(mousePrev!=null)
					{
					if(dragSelected!=null)
						{
						dragSelected.moveTo(e.getX(), e.getY());
						}
					else
						{
						double dx=e.getX()-mousePrev.x;
						double dy=e.getY()-mousePrev.y;
						dx/=zoom;
						dy/=zoom;
						Rectangle2D rect=selected.getSrcBounds();
						selected.setSrcBounds(
							rect.getX()+dx, rect.getY()+dy,
							rect.getWidth(), rect.getHeight()
							);
						}
					drawingArea.repaint();
					getModel().documentModified=true;
					}
				mousePrev=new Point(e.getX(), e.getY());
				}
			
			@Override
			public void mouseReleased(MouseEvent e)
				{
				if(mousePrev!=null)
					{
					adjustSize();
					mousePrev=null;
					}
				}
			};
		this.drawingArea.addMouseListener(mouse);
		this.drawingArea.addMouseMotionListener(mouse);
		
		JToolBar toolbar=new JToolBar();
		contentPane.add(toolbar,BorderLayout.NORTH);
		JMenuBar bar=new JMenuBar();
		this.setJMenuBar(bar);
		JMenu menu=new JMenu("File");
		bar.add(menu);
		AbstractAction action=new AbstractAction("Quit")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				doMenuQuit();
				}
			};
		menu.add(action);
		
		action=new AbstractAction("Save As...")
			{
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doMenuSaveAs();
				getModel().documentModified=false;
				}
			};
		menu.add(action);
		
		action=new AbstractAction("Save")
			{
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doMenuSave(getModel().fileSaveAs);
				getModel().documentModified=false;
				}
			};
		menu.add(action);
		
		action=new AbstractAction("Open")
			{
			@Override
			public void actionPerformed(ActionEvent arg0) {
				doMenuOpen();
				}
			};
		menu.add(action);
		toolbar.add(action);
		
		JMenu subMenu=new JMenu("Export");
		menu.add(subMenu);
		
		
		action=new AbstractAction("As SVG")
			{
			@Override
			public void actionPerformed(ActionEvent ae)
				{
				doMenuExportAsSVG();
				}
			};
		subMenu.add(action);
		action=new AbstractAction("As HTML")
			{
			@Override
			public void actionPerformed(ActionEvent ae)
				{
				doMenuExportAsHTML();
				}
			};
		subMenu.add(action);
		
		
		
		menu=new JMenu("Images Source");
		this.menuImageSource=menu;
		bar.add(menu);
		
		menu=new JMenu("Icons");
		bar.add(menu);
		action=new AbstractAction("New")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				doMenuCreateNew();
				}
			};
		menu.add(action);
		toolbar.add(action);
		action=new AbstractAction("[+]")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				setZoom(zoom*1.2);
				}
			};
		menu.add(action);
		toolbar.add(action);
		action=new AbstractAction("[-]")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				setZoom(zoom*0.8);
				}
			};
		menu.add(action);
		toolbar.add(action);
		
		action=new AbstractAction("Add Image Source")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				doMenuAddImageSource();
				}
			};
		menu.add(action);
		toolbar.add(action);
		
		action=new AbstractAction("TextBox")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				doMenuAddTextBox();
				}
			};
		menu.add(action);
		toolbar.add(action);
		}
	
	private void doMenuCreateNew()
		{
		int midX= drawingArea.getWidth()/2;
		int midY= drawingArea.getHeight()/2;
		
		Figure figure=new DefaultFigure(new Rectangle2D.Double(100,100,50,50));
		Rectangle r=drawingArea.getVisibleRect();
		
		//figure.bounds.x=(int)r.getCenterX()-midX;
		//figure.bounds.y=(int)r.getCenterY()-midY;
		getModel().getFigures().add(figure);
		getModel().documentModified=true;
		adjustSize();
		}
	
	private void setZoom(double value)
		{
		this.zoom=value;
		adjustSize();
		}
	
	private void editFigure(Figure figure)
		{
		JButton button=new JButton();
		button.addActionListener(new AbstractAction()
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				try{
				ImageSourceEditorDialog dlg=new ImageSourceEditorDialog(new URL("http://biostar.stackexchange.com/theme/image/theme.logo.1d7249"));
				JOptionPane.showConfirmDialog(null, dlg);
				} catch (Exception err)
					{
				
					}
				}
			});
		
		
		
		JPanel pane=new JPanel(new BorderLayout(5,5));
		Dimension d=new Dimension(64,64);
		button.setPreferredSize(d);
		pane.add(button,BorderLayout.WEST);
		JPanel pane2=new JPanel(new InputLayout());
		pane.add(pane2,BorderLayout.CENTER);
		pane2.setPreferredSize(new Dimension(300,100));
		
		JLabel label=new JLabel("Title");
		pane2.add(label);
		JTextField tfTitle=new JTextField(10);
		pane2.add(tfTitle);
		label.setLabelFor(tfTitle);
		
		label=new JLabel("Year");
		pane2.add(label);
		JTextField tfYear=new JTextField(10);
		pane2.add(tfYear);
		label.setLabelFor(tfYear);
		
		label=new JLabel("Year");
		pane2.add(label);
		JTextField tfCountry=new JTextField(10);
		pane2.add(tfCountry);
		label.setLabelFor(tfCountry);
		
		if(JOptionPane.showConfirmDialog(this, pane,"Edit",JOptionPane.OK_CANCEL_OPTION)!=JOptionPane.OK_OPTION)
			{
			return;
			}
		
		}

	private static File assureExtension(File f,String suffix)
		{
		if(f==null) return null;
		String s=f.getName();
		if(!suffix.startsWith(".")) suffix="."+suffix;
		if(s.toLowerCase().endsWith(suffix.toLowerCase())) return f;
		return new File(f.getParentFile(),s+suffix);
		}	
	
	private boolean doMenuSaveAs()
		{
		File f=askFileSaveAs(".svg");
		if(f==null) return false;
		boolean b=doMenuSave(f);
		if(b)
			{
			getModel().fileSaveAs=f;
			}
		return b;
		}
	
	public DefaultModel getModel()
		{
		return model;
		}
	
	private boolean doMenuSave(File filename)
		{
		if(filename==null)
			{
			return doMenuSaveAs();
			}
		try {
			save(filename);
			JOptionPane.showMessageDialog(this, "Saved as "+filename);
			return true;
			}
		catch (Exception e)
			{
			JOptionPane.showMessageDialog(this, String.valueOf(e.getMessage()));
			return false;
			}
		}

	private File askFileSaveAs(String extension)
		{
		JFileChooser chooser=new JFileChooser(PreferredDirectory.getPreferredDirectory());
		if(chooser.showSaveDialog(this)!=JFileChooser.APPROVE_OPTION) return null;
		File selFile=chooser.getSelectedFile();
		if(selFile==null) return null;
		selFile=assureExtension(selFile,extension);
		if( selFile.exists() &&
			JOptionPane.showConfirmDialog(this, "File exists. Overwrite ?", "Overwitre",
					JOptionPane.WARNING_MESSAGE,
					JOptionPane.OK_CANCEL_OPTION, 
					null)!=JOptionPane.OK_OPTION)
			{
			return null;
			}
		PreferredDirectory.setPreferredDirectory(selFile);
		return selFile;
		}
	
	private void save(File file)
		throws IOException,XMLStreamException
		{
		getModel().write(file);
		}
	
	private void adjustSize()
		{
		/*int minX=0;
		int minY=0;
		for(Thumb thumb: this.thumbs)
			{
			Rectangle2D r= thumb.getShape();
			d.width=Math.max(d.width, (int)r.getMaxX());
			d.height=Math.max(d.height, (int)r.getMaxY());
			minX=Math.max(minX,(int)r.getMinX());
			minY=Math.max(minY,(int)r.getMinY());
			}
		d.width-=minX;
		d.height-=minY;*/
		Dimension d= this.drawingArea.getSize();
		this.drawingArea.setPreferredSize(d);
		this.drawingArea.setMinimumSize(d);
		this.drawingArea.setSize(d);
		this.drawingArea.revalidate();
		this.drawingArea.repaint();
		}
	
	private void paintDrawingArea(GC gc)
		{
		getModel().paint(gc);
		if(selected!=null)
			{
			for(Drag drag:selected.getDrags())
				{
				gc.g.setColor(drag==dragSelected?Color.BLUE:Color.RED);
				gc.g.drawOval((int)drag.getX()-3, (int)drag.getY()-3, 7, 7);
				}
			}
		}
	
	private void doMenuQuit()
		{
		if(getModel().documentModified)
			{
			int opt;
			String choices[]={"Quit Anyway","Save & Close","Cancel"};
			if((opt=JOptionPane.showOptionDialog(this,
					"Save document "+getModel().getTitle(),
					"Save ?",
					JOptionPane.DEFAULT_OPTION,
					JOptionPane.WARNING_MESSAGE,
					null, choices, choices[2]))==JOptionPane.CLOSED_OPTION)
				{
				return;
				}
			if(opt==2) return;
			if(opt==1)
				{
				if(!doMenuSave(getModel().fileSaveAs)) return;
				}
			}
		this.setVisible(false);
		this.dispose();
		}
	
	private void doMenuOpen()
		{
		JFileChooser chooser=new JFileChooser();
		if(chooser.showOpenDialog(this)!=JFileChooser.APPROVE_OPTION)
			{
			return;
			}
		File f=chooser.getSelectedFile();
		try
			{
			DefaultModel model=new DefaultModel();
			model.read(f);
			Cartoonist frame=new Cartoonist(model);
			frame.setTitle(f.getName());
			SwingUtils.center(frame,100);
			frame.setVisible(true);
			}
		catch(Exception err)
			{
			ThrowablePane.show(this, err);
			}
		}
	
	
	private void doMenuAddImageSource()
		{
		JPanel pane=new JPanel(new InputLayout());
		JLabel label;
		
		
		label=new JLabel("URL:",JLabel.RIGHT);
		JTextField tfURL=new JTextField(20);
		label.setLabelFor(tfURL);
		pane.add(label);
		pane.add(tfURL);
		
		label=new JLabel("Source URL:",JLabel.RIGHT);
		JTextField tfSource=new JTextField(20);
		label.setLabelFor(tfSource);
		pane.add(label);
		pane.add(tfSource);
		
		label=new JLabel("Title",JLabel.RIGHT);
		JTextField tfTitle=new JTextField(20);
		label.setLabelFor(tfTitle);
		pane.add(label);
		pane.add(tfTitle);
		
		pane.setPreferredSize(new Dimension(400,50));
		URL url=null;
		URL urlSrc=null;
		String title=null;
		
		while(url==null)
			{
			if(JOptionPane.showConfirmDialog(this, pane,"URL",JOptionPane.PLAIN_MESSAGE,JOptionPane.OK_CANCEL_OPTION,null)!=JOptionPane.OK_OPTION)
				{
				return;
				}
			try
				{
				url=new URL(tfURL.getText().trim());
				}
			catch (Exception e)
				{
				JOptionPane.showMessageDialog(null, "Bad URL");
				continue;
				}
			try
				{
				urlSrc=new URL(tfSource.getText().trim());
				}
			catch (Exception e)
				{
				url=null;
				JOptionPane.showMessageDialog(null, "Bad URL");
				continue;
				}
			title=tfTitle.getText().trim();
			if(title.isEmpty())
				{
				title=tfURL.getText().trim();
				}
			}
		
		ImageSource src=new ImageSource();
		
		
		try
			{
			src.image=ImageIO.read(url);
			src.imageUrl=url;
			src.imagePage=urlSrc;
			src.width=src.image.getWidth(null);
			src.height=src.image.getHeight(null);
			src.id= model.imageSources.size()+1;
			src.icon64=new ImageIcon(src.image.getScaledInstance(64, 64, 0));
			}
		catch (IOException e)
			{
			JOptionPane.showMessageDialog(this, String.valueOf(e.getMessage()));
			return;
			}
		finally
			{
			
			}
		if(JOptionPane.showConfirmDialog(null, new JLabel(src.icon64),"Image",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE,null)!=JOptionPane.OK_OPTION)
			{
			return;
			}
		
		
		boolean found=false;
		src.id=1;
		//search a new Id
		while(!found)
			{
			found=true;
			for(ImageSource img: getModel().imageSources)
				{
				if(src.id==img.id)
					{
					src.id++;
					found=false;
					break;
					}
				}
			}
		
		getModel().imageSources.add(src);
		getModel().documentModified=true;
		reloadImageSrcMenu();
		}
	
	
	private void doMenuAddTextBox()
		{
		TextBox tbox=new TextBox();
		tbox.id=getModel().newId();
		tbox.text="Hello World";
		tbox.viewRect.x=5;
		tbox.viewRect.y=5;
		tbox.viewRect.width=150;
		tbox.viewRect.height=50;
		getModel().drawables.add(tbox);
		getModel().getFigures().add(new TextBoxFigure(tbox));
		getModel().documentModified=true;
		reloadImageSrcMenu();
		drawingArea.repaint();
		}
	
	private void reloadImageSrcMenu()
		{
		this.menuImageSource.removeAll();
		for(ImageSource is: getModel().imageSources)
			{
			AbstractAction action=new AbstractAction()
				{
				@Override
				public void actionPerformed(ActionEvent e)
					{
					ImageSource is=ImageSource.class.cast(getValue("image.src"));
					createImageInstance(is);
					}
				};
			action.putValue("image.src", is);
			action.putValue(AbstractAction.SMALL_ICON, is.icon64);
			this.menuImageSource.add(action);
			}
		}
	
	private void createImageInstance(ImageSource is)
		{
		ImageInstanceEditor ed=new ImageInstanceEditor(is);
		SwingUtils.center(ed,150);
		ed.setVisible(true);
		if(ed.exitStatus!=JOptionPane.OK_OPTION) return;
		ed.instance.id=getModel().newId();
		Rectangle2D rx =ed.instance.getShape().getBounds2D();
		ed.instance.viewRect=new Rectangle2D.Double(10,10,rx.getWidth(),rx.getHeight());
		getModel().drawables.add(ed.instance);
		getModel().getFigures().add(new ImageInstanceFigure(ed.instance));
		getModel().documentModified=true;
		adjustSize();
		}
	
	private void doMenuExportAsSVG()
		{
		File file=askFileSaveAs(".svg");
		if(file==null) return;
		}
	
	private void doMenuExportAsHTML()
		{
		File file=askFileSaveAs(".html");
		if(file==null) return;
		}
	
	
	/**
	 * main
	 */
	public static void main(String args[])
		{
		try
			{
			int optind=0;
			JFrame.setDefaultLookAndFeelDecorated(true);
			JDialog.setDefaultLookAndFeelDecorated(true);
			if(optind==args.length)
				{
				Cartoonist f=new Cartoonist(new DefaultModel());
				SwingUtils.center(f,100);
				SwingUtils.show(f);
				}
			else
				{
				while(optind<args.length)
					{
					File fileIn=new File(args[optind++]);
					DefaultModel m=new DefaultModel();
					m.read(fileIn);
					m.fileSaveAs=fileIn;
					m.documentModified=false;
					Cartoonist f=new Cartoonist(m);
					SwingUtils.center(f,100);
					SwingUtils.show(f);
					}
				}
			}
		catch (Exception e)
			{
			e.printStackTrace();
			}
		}
	}