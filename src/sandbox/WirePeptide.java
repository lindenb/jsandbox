/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	March-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * Reference:
 *   http://biostar.stackexchange.com/questions/6172/drawing-protein-picture
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Motivation:
 * 	draws a peptide
 * Compilation & Run:
 *        cd jsandbox
 *        ant wirepeptide
 *        java -jar dist/wirepeptide.jar
 */
package sandbox;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.JColorChooser;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;

@SuppressWarnings("serial")
public class WirePeptide
	extends JFrame
	{
	private static final int RADIUS=20;
	private static final int SCREEN_WIDTH=900;
	private static final int SCREEN_HEIGHT=700;
	/** initial peptide */
	private String peptide;
	/** change fill color */
	private AbstractAction fillAction;
	/** change stroke color */
	private AbstractAction strokeAction;
	/** aa to fill color */
	private Map<Character, Color> aa2fill=new HashMap<Character, Color>();
	/** aa to stroke color */
	private Map<Character, Color> aa2stroke=new HashMap<Character, Color>();
	/** prev mouse position */
	private Point prevMouse=null;
	/** amino acid selected */
	private Node selected=null;
	/** drawing area */
	private JPanel drawingArea;
	/** root node */
	private Node root;

	/** an amino acid */
	private class Node
		{
		char aa='A';
		double cx;
		double cy;
		Node next=null;
		Node prev=null;
		Shape getShape()
			{
			return new Ellipse2D.Double(cx-RADIUS,cy-RADIUS,RADIUS*2,RADIUS*2);
			}


		private void dragPrev(double x0,double y0)
			{
			double d= Point2D.distance(cx, cy, x0, y0);
			cx=x0+(RADIUS*2)*(cx-x0)/d;
			cy=y0+(RADIUS*2)*(cy-y0)/d;
			if(prev!=null) prev.dragPrev(cx,cy);
			}

		private void dragNext(double x0,double y0)
			{
			double d= Point2D.distance(cx, cy, x0, y0);
			cx=x0+(RADIUS*2)*(cx-x0)/d;
			cy=y0+(RADIUS*2)*(cy-y0)/d;
			if(next!=null) next.dragNext(cx,cy);
			}

		void dragTo(double x,double y)
			{
			cx=x;
			cy=y;
			if(prev!=null) prev.dragPrev(cx,cy);
			if(next!=null) next.dragNext(cx,cy);
			}

		private Color getFill()
			{
			return WirePeptide.this.getFill(this.aa);
			}

		private Color getStroke()
			{
			return  WirePeptide.this.getStroke(this.aa);
			}



		private void paint(Graphics2D g)
			{
			Stroke old=g.getStroke();
			Shape s=getShape();

			g.setColor(getFill());
			g.fill(s);

			g.setStroke(new BasicStroke(this==selected?3f:1f));

			g.setColor(getStroke());
			g.draw(s);

			g.drawString(String.valueOf(this.aa), (int)cx-RADIUS/4, (int)cy+RADIUS/4);
			g.setStroke(old);
			}
		}





	private WirePeptide(String peptide)
		{
		super("WirePeptide : Pierre Lindenbaum.");
		this.peptide=peptide;
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		JPanel main=new JPanel(new BorderLayout(10,10));
		main.setBorder(new EmptyBorder(10, 10, 10, 10));
		this.drawingArea=new JPanel(null)
			{
			@Override
			protected void paintComponent(Graphics g) {
				super.paintComponent(g);
				paintDrawingArea(Graphics2D.class.cast(g));
				}
			};
		main.add(this.drawingArea,BorderLayout.CENTER);
		this.drawingArea.setOpaque(true);
		this.drawingArea.setBackground(Color.LIGHT_GRAY);
		MouseAdapter m=new MouseAdapter()
			{
			@Override
			public void mousePressed(MouseEvent e)
				{
				prevMouse=null;
				selected=null;
				Node n=root;
				while(n!=null)
					{
					if(n.getShape().contains(e.getX(),e.getY()))
						{
						selected=n;
						break;
						}
					n=n.next;
					}
				strokeAction.setEnabled(selected!=null);
				fillAction.setEnabled(selected!=null);
				if(selected!=null && e.isPopupTrigger())
					{
					JPopupMenu pop=new JPopupMenu();
					pop.add(fillAction);
					pop.add(strokeAction);
					pop.show(drawingArea, e.getX(), e.getY());
					}
				}
			@Override
			public void mouseDragged(MouseEvent e)
				{
				if(selected==null) return;
				if(prevMouse!=null)
					{
					selected.dragTo(
						selected.cx+e.getX()-prevMouse.getX(),
						selected.cy+e.getY()-prevMouse.getY()
						);

					}
				prevMouse=new Point(e.getX(),e.getY());
				drawingArea.repaint();
				}
			};
		this.drawingArea.addMouseListener(m);
		this.drawingArea.addMouseMotionListener(m);
		setContentPane(main);

		List<Node> nodes=new ArrayList<Node>();
		for(int i=0;i< peptide.length();++i)
			{
			Node n=new Node();
			n.aa=Character.toUpperCase(peptide.charAt(i));
			n.cx=1+RADIUS+(RADIUS*2.0)*i;
			n.cy=1+RADIUS;
			if(i>0)
				{
				nodes.get(i-1).next=n;
				n.prev=nodes.get(i-1);
				}
			nodes.add(n);
			}
		this.root=nodes.get(0);
		JMenuBar bar=new JMenuBar();
		setJMenuBar(bar);
		JMenu menu=new JMenu("File");
		bar.add(menu);
		menu.add(new AbstractAction("New...")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				askPeptide(WirePeptide.this.drawingArea);
				}
			});
		menu.add(new AbstractAction("Save as PNG...")
			{
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser=new JFileChooser();
				if(chooser.showSaveDialog(WirePeptide.this)!=JFileChooser.APPROVE_OPTION) return;
				File sel=chooser.getSelectedFile();
				if(!sel.exists() || JOptionPane.showConfirmDialog(WirePeptide.this,
					"File exists. Overwrite ?","Overwrite ?",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE,
					null)!=JOptionPane.OK_OPTION)
					{
					return;
					}
				try {
					toPNG(sel);
					}
				catch (Exception e2)
					{
					JOptionPane.showMessageDialog(WirePeptide.this, e2.getMessage());
					}
				}
			});

		menu.add(new AbstractAction("Save as Canvas...")
			{
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser=new JFileChooser();
				if(chooser.showSaveDialog(WirePeptide.this)!=JFileChooser.APPROVE_OPTION) return;
				File sel=chooser.getSelectedFile();
				if(!sel.exists() || JOptionPane.showConfirmDialog(WirePeptide.this,
					"File exists. Overwrite ?","Overwrite ?",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE,
					null)!=JOptionPane.OK_OPTION)
					{
					return;
					}
				try {
					PrintWriter p=new PrintWriter(sel);
					toCanvas(p);
					p.flush();
					p.close();
					}
				catch (Exception e2)
					{
					JOptionPane.showMessageDialog(WirePeptide.this, e2.getMessage());
					}
				}
			});

		menu.add(new AbstractAction("Save as SVG...")
			{
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser=new JFileChooser();
				if(chooser.showSaveDialog(WirePeptide.this)!=JFileChooser.APPROVE_OPTION) return;
				File sel=chooser.getSelectedFile();
				if(!sel.exists() || JOptionPane.showConfirmDialog(WirePeptide.this,
					"File exists. Overwrite ?","Overwrite ?",
					JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.WARNING_MESSAGE,
					null)!=JOptionPane.OK_OPTION)
					{
					return;
					}
				try {
					PrintWriter p=new PrintWriter(sel,"UTF-8");
					toSVG(p);
					p.flush();
					p.close();
					}
				catch (Exception e2)
					{
					JOptionPane.showMessageDialog(WirePeptide.this, e2.getMessage());
					}
				}
			});

	    menu.add(new JSeparator());
		menu.add(new AbstractAction("Quit")
			{
			@Override
			public void actionPerformed(ActionEvent e) {
				WirePeptide.this.setVisible(false);
				WirePeptide.this.dispose();
				}
			});
		menu=new JMenu("Protein");
		bar.add(menu);
		menu.add(new AbstractAction("SVG")
			{
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					StringWriter str=new StringWriter();
					PrintWriter p=new PrintWriter(str);
					toSVG(p);
					p.flush();
					JTextArea area=new JTextArea(str.toString(), 40, 80);
					JOptionPane.showMessageDialog(WirePeptide.this, new JScrollPane(area));
					}
				catch (Exception e2)
					{
					JOptionPane.showMessageDialog(WirePeptide.this, e2.getMessage());
					}
				}
			});
		menu.add(new AbstractAction("Canvas")
			{
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					StringWriter str=new StringWriter();
					PrintWriter p=new PrintWriter(str);
					toCanvas(p);
					p.flush();
					JTextArea area=new JTextArea(str.toString(), 40, 80);
					JOptionPane.showMessageDialog(WirePeptide.this, new JScrollPane(area));
					}
				catch (Exception e2)
					{
					JOptionPane.showMessageDialog(WirePeptide.this, e2.getMessage());
					}
				}
			});

		fillAction=new AbstractAction("Fill color...")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				if(selected==null) return;
				JColorChooser chooser=new JColorChooser(selected.getFill());
				if(JOptionPane.showConfirmDialog(WirePeptide.this.drawingArea,chooser,
					String.valueOf(this.getValue(NAME)),
					JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,null)
					!=JOptionPane.OK_OPTION)
					{
					return;
					}
				aa2fill.put(selected.aa, chooser.getColor());
				drawingArea.repaint();
				}
			};
		fillAction.setEnabled(false);
		strokeAction=new AbstractAction("Stroke color...")
			{
			@Override
			public void actionPerformed(ActionEvent e)
				{
				if(selected==null) return;
				JColorChooser chooser=new JColorChooser(selected.getStroke());
				if(JOptionPane.showConfirmDialog(WirePeptide.this.drawingArea,chooser,
					String.valueOf(this.getValue(NAME)),
					JOptionPane.OK_CANCEL_OPTION,JOptionPane.QUESTION_MESSAGE,null)
					!=JOptionPane.OK_OPTION)
					{
					return;
					}
				aa2stroke.put(selected.aa, chooser.getColor());
				drawingArea.repaint();
				}
			};
		strokeAction.setEnabled(false);
		menu.add(this.fillAction);
		menu.add(this.strokeAction);

		this.aa2fill.put('H', Color.BLUE);
		this.aa2fill.put('K', Color.BLUE);
		this.aa2fill.put('R', Color.BLUE);
		this.aa2fill.put('D', Color.RED);
		this.aa2fill.put('E', Color.RED);
		this.aa2fill.put('S', Color.GREEN);
		this.aa2fill.put('T', Color.GREEN);
		this.aa2fill.put('N', Color.GREEN);
		this.aa2fill.put('Q', Color.GREEN);
		this.aa2fill.put('A', Color.WHITE);
		this.aa2fill.put('V', Color.WHITE);
		this.aa2fill.put('L', Color.WHITE);
		this.aa2fill.put('I', Color.WHITE);
		this.aa2fill.put('M', Color.WHITE);
		this.aa2fill.put('F', Color.MAGENTA);
		this.aa2fill.put('W', Color.MAGENTA);
		this.aa2fill.put('Y', Color.MAGENTA);
		this.aa2fill.put('P', Color.LIGHT_GRAY);
		this.aa2fill.put('G', Color.LIGHT_GRAY);
		this.aa2fill.put('C', Color.YELLOW);
		this.aa2fill.put('B', Color.GRAY);
		this.aa2fill.put('Z', Color.GRAY);
		this.aa2fill.put('X', Color.GRAY);
		this.aa2fill.put('-', Color.GRAY);
		}


	static private String toRgb(Color c)
		{
		return "rgb("+c.getRed()+","+c.getGreen()+","+c.getBlue()+")";
		}

	private Color getFill(char aa)
		{
		Color c= aa2fill.get(aa);
		if(c==null) c=Color.WHITE;
		return c;
		}
	private Color getStroke(char aa)
		{
		Color c= aa2stroke.get(aa);
		if(c==null) c=Color.BLACK;
		return c;
		}

	private void paintDrawingArea(Graphics2D g)
		{
		g.setFont(new Font("Courier",Font.PLAIN,RADIUS));
		Node n=root;
		while(n!=null)
			{
			n.paint(g);
			n=n.next;
			}
		}

	private void toPNG(File file) throws IOException
		{
		int minx=Integer.MAX_VALUE;
		int miny=Integer.MAX_VALUE;
		int maxx=Integer.MIN_VALUE;
		int maxy=Integer.MIN_VALUE;
		Node n=root;
		while(n!=null)
			{
			Rectangle shape=n.getShape().getBounds();
			minx=Math.min(minx,(int)shape.getMinX());
			miny=Math.min(miny,(int)shape.getMinY());

			maxx=Math.max(maxx,(int)shape.getMaxX());
			maxy=Math.max(maxy,(int)shape.getMaxY());

			n=n.next;
			}
		minx--;
		maxx++;
		miny--;
		maxy++;
		BufferedImage img=new BufferedImage((maxx-minx), (maxy-miny), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g= img.createGraphics();
		g.translate(-minx, -miny);
		paintDrawingArea(g);
		ImageIO.write(img,"PNG",file);
		}

	private void toCanvas(PrintWriter out)
		{
		int minx=Integer.MAX_VALUE;
		int miny=Integer.MAX_VALUE;
		int maxx=Integer.MIN_VALUE;
		int maxy=Integer.MIN_VALUE;
		Node n=root;
		while(n!=null)
			{
			Rectangle shape=n.getShape().getBounds();
			minx=Math.min(minx,(int)shape.getMinX());
			miny=Math.min(miny,(int)shape.getMinY());

			maxx=Math.max(maxx,(int)shape.getMaxX());
			maxy=Math.max(maxy,(int)shape.getMaxY());

			n=n.next;
			}
		minx--;
		maxx++;
		miny--;
		maxy++;
		long id=System.currentTimeMillis();
		out.print("<html><head><script type='application/javascript'>function draw"+id+"(){");
		out.print("var canvas = document.getElementById('node"+id+"');if(canvas==null) return;");
		out.print("var ctx = canvas.getContext('2d'); if(ctx==null) return;");
		out.print("ctx.font = '"+RADIUS+"px Courier';");

		n=root;
		while(n!=null)
			{
			out.print("ctx.strokeStyle=\""+toRgb(n.getStroke())+"\";");
			out.print("ctx.fillStyle=\""+toRgb(n.getFill())+"\";");
			out.print("ctx.beginPath();ctx.arc("+(n.cx-minx)+","+(n.cy-miny)+","+RADIUS+",0,Math.PI*2,true);ctx.closePath();");
			out.print("ctx.fill();ctx.stroke();");
			out.print("ctx.fillStyle=\""+toRgb(n.getStroke())+"\";");
			out.print("ctx.beginPath();ctx.fillText('"+n.aa+"',"+(n.cx-minx-RADIUS/4)+","+(n.cy-miny+RADIUS/4)+");ctx.closePath();");
			n=n.next;
			}
		out.print("}</script></head>" +
			"<body onload='draw"+id+"();'><canvas id=\"node"+id+"\" width=\""+
				(maxx-minx)+"\" "+ " height=\""+(maxy-miny)+"\" />" +
			"</body>" +
			"</html>");
		out.flush();
		}


	private void toSVG(PrintWriter out)
		{
		Set<Character> aacids=new HashSet<Character>();
		int minx=Integer.MAX_VALUE;
		int miny=Integer.MAX_VALUE;
		int maxx=Integer.MIN_VALUE;
		int maxy=Integer.MIN_VALUE;
		Node n=root;
		while(n!=null)
			{
			aacids.add(n.aa);
			Rectangle shape=n.getShape().getBounds();
			minx=Math.min(minx,(int)shape.getMinX());
			miny=Math.min(miny,(int)shape.getMinY());

			maxx=Math.max(maxx,(int)shape.getMaxX());
			maxy=Math.max(maxy,(int)shape.getMaxY());

			n=n.next;
			}
		minx--;
		maxx++;
		miny--;
		maxy++;

		out.printf("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n"+
				      "<svg "+
				      " xmlns:svg=\"http://www.w3.org/2000/svg\" "+
				      " xmlns=\"http://www.w3.org/2000/svg\" "+
				      " xmlns:xlink='http://www.w3.org/1999/xlink' "+
				      " width=\""+(maxx-minx)+"\" "+
				      " height=\""+(maxy-miny)+"\" "+
				      " style=\"stroke-width:1px;stroke:black;font-size:"+(int)RADIUS+"px;font-family=courier;\" "+
				      " version=\"1.0\">"
				      );
		out.printf("<title>"+this.peptide+"</title>");
		out.printf("<description>"+this.peptide+". Made with "+getClass().getSimpleName()+" Pierre Lindenbaum 2011 http://plindenbaum.blogspot.com</description>");
		out.printf("<defs>");
		for(Character aa:aacids)
			{
			out.print("<g id=\"AA_"+aa+"\">");
		    out.printf("<circle r=\""+RADIUS+"\" cx='0' cy='0' "+
		    		"style=\"fill:"+
		    		toRgb(getFill(aa))+
		    		";stroke:"+
		    		toRgb(getStroke(aa))+
		    		";\"/>");
		    out.print("<text x=\"0\" y=\""+(RADIUS/4.0)+"\" text-anchor='middle'>"+aa+"</text>");
		    out.print("</g>");
			}
		out.printf("</defs>");
		n=root;
		int index=0;
		while(n!=null)
			{
			++index;
	        out.print("<use id=\""+n.aa+index+"\" title=\""+n.aa+"("+index+")\" xlink:href=\"#AA_"+n.aa+"\" " +
	        		"x=\""+ (n.cx-minx)+"\" "+
	        		"y=\""+ (n.cy-miny)+ "\" "+
	        		"/>");
	        n=n.next;
			}
	    out.print("</svg>\n");
		}



	private static void askPeptide(JComponent owner)
		{
		Dimension dim=new Dimension(SCREEN_WIDTH,SCREEN_HEIGHT);
		Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
		String peptide="";
		while(peptide.isEmpty())
			{
			peptide=JOptionPane.showInputDialog(owner,"Enter the sequence of the protein");
			if(peptide==null) return;
			peptide=peptide.replaceAll("[^A-Za-z\\-]", "");
			}

		try {
			JFrame.setDefaultLookAndFeelDecorated(true);
			final WirePeptide f=new WirePeptide(peptide);
			f.setBounds(
					(screen.width-dim.width)/2,
					(screen.height-dim.height)/2,
					dim.width,
					dim.height
					);
			SwingUtilities.invokeAndWait(new Runnable()
				{
				@Override
				public void run() {
					f.setVisible(true);
				}
				});
			}
		catch (Exception e)
			{
			JOptionPane.showMessageDialog(owner,String.valueOf(e.getMessage()));
			}
		}



	public static void main(String[] args) throws Exception
		{
		Dimension dim=new Dimension(SCREEN_WIDTH,SCREEN_HEIGHT);
		Dimension screen=Toolkit.getDefaultToolkit().getScreenSize();
		JFrame.setDefaultLookAndFeelDecorated(true);
		JDialog.setDefaultLookAndFeelDecorated(true);
		if(args.length>1)
			{
			System.err.println("Too many arguments");
			return;
			}
		if(args.length==1)
			{
			String peptide=args[0].replaceAll("[^A-Za-z\\-]", "");
			if(!peptide.isEmpty())
				{
				final WirePeptide f=new WirePeptide(peptide);
				f.setBounds(
					(screen.width-dim.width)/2,
					(screen.height-dim.height)/2,
					dim.width,
					dim.height
					);

				SwingUtilities.invokeAndWait(new Runnable()
					{
					@Override
					public void run() {
						f.setVisible(true);
						}
					});
				return;
				}
			}
		askPeptide(null);
		}
	}
