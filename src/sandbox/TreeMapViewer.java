package sandbox;


import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.font.FontRenderContext;
import java.awt.font.LineMetrics;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.AbstractCellEditor;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;



@SuppressWarnings("serial")
public class TreeMapViewer extends JFrame {
	private enum FilterType {None,FilterOut,FilterIn};
	private enum Orientation { VERTICAL, HORIZONTAL};
    private enum Direction { ASCENDING, DESCENDING}
    private static final Font THE_FONT=new Font("Courier", Font.PLAIN, 12);
    private   static final  DecimalFormat decimalFormat = new DecimalFormat("#,###");
    private static class TreePack
		{
    	private final String label;
    	private final TreePack parent;
    	private final Map<String, TreePack> children=new HashMap<>();
    	private Rectangle2D bounds=new Rectangle2D.Double();
    	private int weight=1;
		public void setBounds(Rectangle2D bounds) { this.bounds=bounds;}
		public Rectangle2D getBounds() { return this.bounds;}
		TreePack(final String label,final TreePack parent)
			{
			this.label=label;
			this.parent=parent;
			}
		public int getWeight() { 
			return weight;
			}
		public int getDepth()
			{
			return this.parent==null?0:1+parent.getDepth();
			}
		
		private Rectangle2D grow(double ratio,Rectangle2D r)
			{
			double w=r.getWidth()*ratio;
			double h=r.getHeight()*ratio;
			return new Rectangle2D.Double(
					r.getX()+(r.getWidth()-w)/2.0,
					r.getY()+(r.getHeight()-h)/2.0,
					w,h
					);
			}
		
		private void print(Graphics2D g2,Rectangle2D box,String text)
			{
			if(box.getHeight()<6 || box.getWidth()<6) return;
			if(text.isEmpty()) text=".";
			g2.setFont(THE_FONT);
	        FontRenderContext frc = g2.getFontRenderContext();
	        double mScale;
	        
		    Font font = g2.getFont().deriveFont(32f);
		       
	        double sw = font.getStringBounds(text, frc).getWidth();
	        LineMetrics lm = font.getLineMetrics(text, frc);
		    double sh = lm.getAscent() + lm.getDescent();
		    
		    if( sw> box.getWidth() || sh>box.getHeight())
		        	{
		    		mScale  = Math.min(
		    				box.getWidth()/sw,
		    				box.getHeight()/sh
		    				);
		        	}
		    else
		        	{
			    	mScale  = Math.max(
		    				sw/box.getWidth(),
		    				sh/box.getHeight()
		    				);
		        	}
		     
            
            //double mScale=Math.max(xScale,yScale);
            
            double x = box.getX() + mScale*(box.getWidth() - mScale*sw)/2;
            double y = box.getMaxY() - mScale*lm.getDescent();
            AffineTransform at =
                AffineTransform.getScaleInstance(mScale,mScale);
            g2.setFont(font.deriveFont(at));
            g2.drawString(text, (int)x,(int)y);
	            
            g2.drawLine((int)box.getX(),(int)box.getY(),(int)x,(int) y);
	        
		}
		
		public void layout(Graphics2D g,final Rectangle2D area) {
			
			
			g.setColor(getDepth()%2==0?Color.LIGHT_GRAY:Color.WHITE);
			g.fill(area);
			g.setColor(Color.BLACK);
			/*
			g.draw(new Line2D.Double(this.getBounds().getX(), this.getBounds().getY(),
					this.getBounds().getMaxX(), this.getBounds().getMaxY()
					));
			g.draw(new Line2D.Double(this.getBounds().getMaxX(), this.getBounds().getY(),
					this.getBounds().getX(), this.getBounds().getMaxY()
					));*/
			g.draw(area);
			
			if(this.children.isEmpty())
				{
				Rectangle2D rup = grow(0.9,new Rectangle2D.Double(
						area.getX(),
						area.getY(),
						area.getWidth(),
						area.getHeight()/2.0
						));
				print(g,rup,this.label);
				Rectangle2D rdown = grow(0.9,new Rectangle2D.Double(
						area.getX(),
						area.getCenterY(),
						area.getWidth(),
						area.getHeight()/2.0
						));
				print(g,rdown,decimalFormat.format(getWeight()));
				}
			else if(this.parent==null)
				{
				TreeMapViewer.layout(new ArrayList<>(this.children.values()), area);
				for(final TreePack child:this.children.values())
					{
					child.layout(g,child.bounds);
					}
				}
			else
				{

				double h1=area.getHeight()*0.1;
				Rectangle2D rup = grow(0.9,new Rectangle2D.Double(
						area.getX(),
						area.getY(),
						area.getWidth(),
						h1
						));
				print(g,rup,this.label+" ("+decimalFormat.format(getWeight())+")");
				Rectangle2D rdown = grow(0.9,new Rectangle2D.Double(
						area.getX(),
						area.getY()+h1,
						area.getWidth(),
						area.getHeight()-h1
						));
				TreeMapViewer.layout(new ArrayList<>(this.children.values()), rdown);
				
				for(final TreePack child:this.children.values())
					{
					child.layout(g,child.bounds);
					}
				}
			System.err.println(this.getBounds()+ " "+this.label+" "+getWeight());
			/*
			g.setColor(Color.BLACK);
			g.draw(new Line2D.Double(area.getX(), area.getY(),
					area.getMaxX(), area.getMaxY()
					));
			g.draw(new Line2D.Double(area.getMaxX(), area.getY(),
					area.getX(),area.getMaxY()
					));
			*/
			}
		}

	private static final Comparator<TreePack> comparator=new Comparator<TreePack>()
		{
		public int compare(TreePack pack1, TreePack pack2)
				{
				double w1=pack1.getWeight();
				double w2=pack2.getWeight();
				if(w1<w2) return 1;
				if(w1>w2) return -1;
				return 0;
				}
		};

	
	private static class ColumnHeader
		{
		private final String label;
		FilterType filterType=FilterType.None;
		private Pattern regex;
		final int index;
		public ColumnHeader(final String label,int index) 
			{
			this.label= label;
			this.regex=null;
			this.index=index;
			}
		

		}
	
	
	
	
	private static class DataRow
		{
		final String values[];
		DataRow(final String values[])
			{
			this.values = values;
			}
		public int getWeight() { 
			return Integer.parseInt(values[values.length-1]);
			}
		}
	
	public class FilterTypeCellEditor extends AbstractCellEditor implements TableCellEditor {

	    private  JComboBox<FilterType> jComboBox = new JComboBox<>();
	    boolean cellEditingStopped = false;

	    @Override
	    public Object getCellEditorValue() {
	        return jComboBox.getSelectedItem();
	    }

	    @Override
	    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
	    	FilterType filter;
	    	try {
	    		filter=value==null?FilterType.None:FilterType.valueOf(value.toString());
	    	}catch(Exception err)
	    		{
	    		filter=FilterType.None;
	    		}

	      
	        jComboBox = new JComboBox<FilterType>(FilterType.values());
	        jComboBox.setSelectedItem(filter);
	       
	        jComboBox.addItemListener(new ItemListener() {
	            @Override
	            public void itemStateChanged(ItemEvent e) {
	                if (e.getStateChange() == ItemEvent.SELECTED) {
	                    fireEditingStopped();
	                }
	            }
	        });
	        
	        
	        jComboBox.addPopupMenuListener(new PopupMenuListener() {

	            @Override
	            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
	                cellEditingStopped = false;
	            }

	            @Override
	            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
	                cellEditingStopped = true;
	                fireEditingCanceled();
	            }

	            @Override
	            public void popupMenuCanceled(PopupMenuEvent e) {

	            }
	        });
	        return jComboBox;
	    }
	}
	
	final TableModel dataModel;
	final List<ColumnHeader> columnHeaders=new ArrayList<>();
	final List<DataRow> dataRows=new ArrayList<>();
	final List<DataRow> filteredDataRows=new ArrayList<>();
	private JTable filteredDataTable;
	private final JPanel drawingArea;
	private List<JComboBox<String>> selectColumns=new ArrayList<>();
	
	private TreeMapViewer(final File input) throws IOException
		{
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		BufferedReader r=null;
		try {
			r= new BufferedReader(new FileReader(input));
			int nLine=0;
			final Pattern tab=Pattern.compile("[\t]");
			String line;
			while((line=r.readLine())!=null)
				{
				nLine++;
				final String tokens[]=tab.split(line);
				if(nLine==1)
					{	
					for(int i=0;i< tokens.length;++i)
						{
						columnHeaders.add(new ColumnHeader(tokens[i],i));
						}
					}
				else
					{
					if(tokens.length<2 || tokens.length!=columnHeaders.size())
						{
						r.close();
						throw new IOException(
							"Bad number of column in "+line
							);
						}
					try
						{
						int n=Integer.parseInt(tokens[tokens.length-1]);
						if(n<1) throw new IOException(
								"Bad Count of last column in "+line
								);
						}
					catch(NumberFormatException err)
						{
						throw new IOException(
								"Bad Count of last column in "+line,
								err
								);
						}
					dataRows.add(new DataRow(tokens));
					}
				}
			this.dataModel = new AbstractTableModel() {
				@Override
				public Class<?> getColumnClass(int columnIndex) {
					return String.class;
					}
				@Override
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					return false;
					}
				@Override
				public String getColumnName(int column) {
					return columnHeaders.get(column).label;
					}
				@Override
				public Object getValueAt(int rowIndex, int columnIndex) {
					return dataRows.get(rowIndex).values[columnIndex];
				}
				
				@Override
				public int getRowCount() {
					return dataRows.size();
				}
				
				@Override
				public int getColumnCount() {
					return columnHeaders.size();
					}
				};
			
			setTitle(input.getName());
			JPanel contentPane=new JPanel(new BorderLayout());	
			this.setContentPane(contentPane);
			JTabbedPane tabbedPane=new JTabbedPane();
			
			JTable dataTable=new JTable(this.dataModel);
			dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			tabbedPane.addTab("Data", new JScrollPane(dataTable));
			
			
			TableModel columnsModel=new AbstractTableModel() {
				@Override
				public boolean isCellEditable(int rowIndex, int columnIndex) {
					
					switch(columnIndex)
						{
						case 1: return true;
						case 2: return true;
						default: return false;
						}
					}
				@Override
				public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
					ColumnHeader header= columnHeaders.get(rowIndex);
					switch(columnIndex)
						{
						case 1: 
							{
							header.filterType = aValue==null?FilterType.None:FilterType.valueOf(String.valueOf(aValue)); 
							reloadFilteredDataTable();
							break;
							}
						case 2: {
							if(aValue==null || aValue.toString().trim().isEmpty())
								{
								header.regex=null;
								}
							try {
								Pattern pat=Pattern.compile(aValue.toString());
								header.regex=pat;
								}
							catch(Exception err)
								{
								header.regex=null;
								}
							reloadFilteredDataTable();
							break;
							}
						default: break;
						}
					}
				@Override
				public String getColumnName(int column) {
						switch(column)
						{
						case 0: return "Label";
						case 1: return "Filter";
						case 2: return "Regex";
						default: return null;
						}					
					}
				@Override
				public Class<?> getColumnClass(int columnIndex) {
					switch(columnIndex)
						{
						case 1: return FilterType.class;
						default: return String.class;
						}
					}
				@Override
				public Object getValueAt(int rowIndex, int columnIndex) {
					ColumnHeader header= columnHeaders.get(rowIndex);
					switch(columnIndex)
						{
						case 0: return header.label;
						case 1: return header.filterType;
						case 2: return header.regex;
						default: return null;
						}
				}
				
				@Override
				public int getRowCount() {
					return columnHeaders.size()-1;
				}
				
				@Override
				public int getColumnCount() {
					return 3;
					}
			};
			
			columnsModel.addTableModelListener(new TableModelListener() {
				@Override
				public void tableChanged(TableModelEvent e) {
					reloadFilteredDataTable();
					
				}
			});
			JTable colsTable=new JTable(columnsModel);
			
			colsTable.getColumnModel().getColumn(1).setCellEditor(new FilterTypeCellEditor());
			dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			JPanel pane2=new JPanel(new BorderLayout());
			pane2.add(new JScrollPane(colsTable),BorderLayout.CENTER);
			tabbedPane.addTab("Columns", pane2);

			
			AbstractTableModel filteredDataModel = new AbstractTableModel() {
					@Override
					public Class<?> getColumnClass(int columnIndex) {
						return String.class;
						}
					@Override
					public boolean isCellEditable(int rowIndex, int columnIndex) {
						return false;
						}
					@Override
					public String getColumnName(int column) {
						return columnHeaders.get(column).label;
						}
					@Override
					public Object getValueAt(int rowIndex, int columnIndex) {
						return filteredDataRows.get(rowIndex).values[columnIndex];
					}
					
					@Override
					public int getRowCount() {
						return filteredDataRows.size();
					}
					
					@Override
					public int getColumnCount() {
						return columnHeaders.size();
						}
					};
			this.filteredDataRows.addAll(dataRows);
			this.filteredDataTable=new JTable(filteredDataModel);
			dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
			tabbedPane.addTab("Filtered Data", new JScrollPane(filteredDataTable));
			
			
			pane2=new JPanel(new BorderLayout());
			JPanel pane3=new JPanel(new FlowLayout());
			pane2.add(pane3,BorderLayout.NORTH);
			for(int x=0;x<4;++x)
				{
				if(x>0) pane3.add(new JSeparator(SwingConstants.VERTICAL));
				Vector<String> items=new Vector<>();
				items.add("");
				for(ColumnHeader h:this.columnHeaders) items.add(h.label);
				JComboBox<String> combo=new JComboBox<>(items);
				combo.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						drawingArea.repaint();
						
					}
					
				});
				pane3.add(combo);
				this.selectColumns.add(combo);
				}
			this.drawingArea=new JPanel(null){
				@Override
				protected void paintComponent(Graphics g1) {
					paintDrawingArea(Graphics2D.class.cast(g1));
					
					}
			};
			drawingArea.setOpaque(true);
			drawingArea.setBackground(Color.WHITE);
			pane2.add(drawingArea,BorderLayout.CENTER);
			tabbedPane.addTab("TreeMap",pane2);
			
			
			contentPane.add(tabbedPane,BorderLayout.CENTER);
			
			
			
			
			Dimension dim= Toolkit.getDefaultToolkit().getScreenSize();
			setBounds(100, 100, dim.width-200, dim.height-200);
			
			} 
		finally
			{
			IOUtils.close(r);
			}
		}
	
	private void paintDrawingArea(Graphics2D g)
		{
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

		g.setColor(Color.WHITE);
		g.fillRect(0, 0, drawingArea.getWidth(), drawingArea.getHeight());
		System.err.println("DRAIWNG AREA:"+ drawingArea.getWidth()+" "+ drawingArea.getHeight());
		List<ColumnHeader> selectedHeaders=new ArrayList<>(this.selectColumns.size());
		for(JComboBox<String> combox:this.selectColumns)
			{
			int idx= combox.getSelectedIndex();
			if(idx<1) continue;
			selectedHeaders.add(this.columnHeaders.get(idx-1));
			}
		int total=0;
		final TreePack root=new TreePack("ALL",null);
		
		for(final DataRow row:this.filteredDataRows)
			{
			TreePack curr=root;
			for(int x=0;x< selectedHeaders.size();++x)
				{
				final String token=row.values[selectedHeaders.get(x).index];
				TreePack newpack= curr.children.get(token);
				if(newpack==null)
					{
					newpack = new TreePack(token,curr);
					curr.children.put(token,newpack);
					}
				newpack.weight+=row.getWeight();
				curr = newpack;
				}
			}
		root.layout(g,new Rectangle2D.Double(0,0,drawingArea.getWidth(), drawingArea.getHeight()));
		}
	
	private void reloadFilteredDataTable() {
		List<DataRow> filtered=new ArrayList<>(dataRows.size());
		for(DataRow row:dataRows) {
			boolean keep=true;
			for(ColumnHeader header:this.columnHeaders)
				{
				if(header.regex==null) continue;
				if(header.filterType==FilterType.None) continue;
				boolean match=header.regex.matcher(row.values[header.index]).matches();
				if((!match && header.filterType==FilterType.FilterIn) || (match && header.filterType==FilterType.FilterOut))
					{
					keep=false;
					break;
					}
				}
			
			if(keep) filtered.add(row);
		}
		this.filteredDataRows.clear();
		this.filteredDataRows.addAll(filtered);
		AbstractTableModel.class.cast(this.filteredDataTable.getModel()).fireTableDataChanged();
		drawingArea.repaint();
	}
	
	
private static void layout(List<TreePack> items,final Rectangle2D bounds)
	{
    layout(sortDescending(items),0,items.size()-1,bounds);
	}

private static double sum(List<TreePack> items, int start, int end)
    {
    double sum=0;
    while(start<=end)//yes <=
    	{
    	sum+=items.get(start++).getWeight();
    	}
    return sum;
    }

private static List<TreePack> sortDescending(List<TreePack> items)
    {
    List<TreePack> L=new ArrayList<TreePack>(items);
    Collections.sort(L,comparator);
    return L;
    }

private static void layout(List<TreePack> items, int start, int end,
		final Rectangle2D bounds)
{
    if (start>end) return;
        
    if (end-start<2)
    {
        layoutBest(items,start,end,bounds);
        return;
    }
    
    double x=bounds.getX(), y=bounds.getY(), w=bounds.getWidth(), h=bounds.getHeight();
    
    double total=sum(items, start, end);
    int mid=start;
    double a=items.get(start).getWeight()/total;
    double b=a;
    
    if (w<h)
    {
        // height/width
        while (mid<=end)
        {
            double aspect=normAspect(h,w,a,b);
            double q=items.get(mid).getWeight()/total;
            if (normAspect(h,w,a,b+q)>aspect) break;
            mid++;
            b+=q;
        }
        layoutBest(items,start,mid,new Rectangle2D.Double(x,y,w,h*b));
        layout(items,mid+1,end,new Rectangle2D.Double(x,y+h*b,w,h*(1-b)));
    }
    else
    {
        // width/height
        while (mid<=end)
        {
            double aspect=normAspect(w,h,a,b);
            double q=items.get(mid).getWeight()/total;
            if (normAspect(w,h,a,b+q)>aspect) break;
            mid++;
            b+=q;
        }
       layoutBest(items,start,mid,new Rectangle2D.Double(x,y,w*b,h));
       layout(items,mid+1,end,new Rectangle2D.Double(x+w*b,y,w*(1-b),h));
    }
    
}

private static double aspect(double big, double small, double a, double b)
{
    return (big*b)/(small*a/b);
}

private static double normAspect(double big, double small, double a, double b)
{
    double x=aspect(big,small,a,b);
    if (x<1) return 1/x;
    return x;
}

private static void layoutBest(List<TreePack> items, int start, int end, final Rectangle2D bounds)
    {
    sliceLayout(
    		items,start,end,bounds,
            bounds.getWidth()>bounds.getHeight() ? Orientation.HORIZONTAL : Orientation.VERTICAL,
            Direction.ASCENDING);
    }



private static void sliceLayout(List<TreePack> items, int start, int end, final Rectangle2D bounds, Orientation orientation, Direction order)
    {
        double total=sum(items, start, end);
        double a=0;
        boolean vertical=orientation==Orientation.VERTICAL;
       
        for (int i=start; i<=end; i++)
        {
        	Rectangle2D.Double r=new Rectangle2D.Double();
            double b=items.get(i).getWeight()/total;
            if (vertical)
            {
                r.x=bounds.getX();
                r.width=bounds.getWidth();
                if (order==Direction.ASCENDING)
                    r.y=bounds.getY()+bounds.getHeight()*a;
                else
                    r.y=bounds.getY()+bounds.getHeight()*(1-a-b);
                r.height=bounds.getHeight()*b;
            }
            else
            {
                if (order==Direction.ASCENDING)
                    r.x=bounds.getX()+bounds.getWidth()*a;
                else
                    r.x=bounds.getX()+bounds.getWidth()*(1-a-b);
                r.width=bounds.getWidth()*b;
                r.y=bounds.getY();
                r.height=bounds.getHeight();
            }
 
            items.get(i).setBounds(r);
            a+=b;
        }
    }

	
	private static void newInstance(Component parentComponent,File f) {
		try {
			final TreeMapViewer app=new TreeMapViewer(f);
			app.setVisible(true);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(parentComponent, e.getMessage(),"Cannot create view",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private static void newInstance(Component parentComponent) {
		try {
			final JFileChooser jfc= new JFileChooser();
			if(jfc.showOpenDialog(parentComponent)!=JFileChooser.APPROVE_OPTION) return;
			newInstance(parentComponent,jfc.getSelectedFile());
		} catch (Exception e) {
			JOptionPane.showMessageDialog(parentComponent, e.getMessage(),"Cannot create view",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
	
	public static void main(String[] args) throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				//TreeMapViewer.newInstance(null);
				newInstance(null,new File("/home/lindenb/jeter.tsv"));
			}
		});
	}
	
}
