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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;

import javax.swing.AbstractAction;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;

/**
 * Swing-Bases treeMap Viewer
 * @author Pierre Lindenbaum PhD
 *
 */
@SuppressWarnings("serial")
public class TreeMapViewer extends JFrame {
	private enum FilterType {None,FilterOut,FilterIn};
	private enum Orientation { VERTICAL, HORIZONTAL};
    private enum Direction { ASCENDING, DESCENDING}
    private static final Font THE_FONT=new Font("Courier", Font.PLAIN, 12);
    private static final  DecimalFormat decimalFormat = new DecimalFormat("#,###");
    
    private static class HistogramRow
    	{
    	final List<String> values;
    	final long weight;
    	HistogramRow(final List<String> values,final long weight) {
    		this.values=values;
    		this.weight=weight;
    		}
    	}
    
    
    private static class TreePack
		{
    	private final String label;
    	private final TreePack parent;
    	private final Map<String, TreePack> children=new HashMap<>();
    	private Rectangle2D bounds=new Rectangle2D.Double();
    	private long weight=1;
		public void setBounds(final Rectangle2D bounds) { this.bounds=bounds;}
		//public Rectangle2D getBounds() { return this.bounds;}
		TreePack(final String label,final TreePack parent)
			{
			this.label=label;
			this.parent=parent;
			}
		public long getWeight() { 
			return weight;
			}
		public int getDepth()
			{
			return this.parent==null?0:1+parent.getDepth();
			}
		
		private Rectangle2D grow(final double ratio,final Rectangle2D r)
			{
			final double w=r.getWidth()*ratio;
			final double h=r.getHeight()*ratio;
			return new Rectangle2D.Double(
					r.getX()+(r.getWidth()-w)/2.0,
					r.getY()+(r.getHeight()-h)/2.0,
					w,h
					);
			}
		
		private void print(final Graphics2D g2,final Rectangle2D box,final String text)
			{
			if(box.getHeight()<6 || box.getWidth()<6 || text.isEmpty()) return;
			g2.setFont(THE_FONT);
			g2.setColor(Color.BLACK);

	        
			 FontRenderContext frc = g2.getFontRenderContext();
	        Font font = g2.getFont().deriveFont(32f);
	        g2.setFont(font);
	        double sw = font.getStringBounds(text, frc).getWidth();
	        LineMetrics lm = font.getLineMetrics(text, frc);
	        double sh = lm.getAscent() + lm.getDescent();
	        
	        double xScale = (box.getWidth() )/sw;
	        double yScale = box.getHeight()/sh;
	        double x = box.getX() + xScale*(box.getWidth()  - xScale*sw)/2;
	        double y = box.getMaxY() - yScale*lm.getDescent();
	        AffineTransform at =
	                AffineTransform.getTranslateInstance(x, y);
	        if (xScale != yScale) {
	            if (xScale > yScale)
	                xScale = yScale;
	            else if (yScale > xScale)
	                yScale = xScale;
	        }
	        at.scale(xScale, yScale);
	        g2.setFont(font.deriveFont(at));
	        g2.drawString(text, 0, 0);
			}
		
		
		private void fillHistogramRow(
			final List<HistogramRow> histogramrows,
			final List<String> values
			)
			{
			values.add(this.label);
			if(this.children.isEmpty())
				{
				histogramrows.add(new HistogramRow(values,this.getWeight()));
				}
			else
				{
				for(final TreePack child:this.children.values())
					{
					child.fillHistogramRow(histogramrows,new ArrayList<>(values));
					}
				}
			}
		
		
		public void layout(final Graphics2D g,final Rectangle2D area) {
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
			//System.err.println(this.getBounds()+ " "+this.label+" "+getWeight());
			
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
		@Override
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
		public long getWeight() { 
			return Long.parseLong(values[values.length-1]);
			}
		}
	
	
	private abstract class AbstractDataRowTableModel
		extends AbstractTableModel {
		
		protected abstract List<DataRow> getDataRowList();
		
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
			return getDataRowList().get(rowIndex).values[columnIndex];
		}
		
		@Override
		public int getRowCount() {
			return getDataRowList().size();
		}
		
		@Override
		public int getColumnCount() {
			return columnHeaders.size();
			}
		};

	
	
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
	
	private final List<ColumnHeader> columnHeaders=new Vector<>();
	private final List<DataRow> dataRows=new Vector<>();
	private final List<DataRow> filteredDataRows=new Vector<>();
	private JTable filteredDataTable;
	private final JPanel drawingArea;
	private List<JComboBox<String>> selectColumns=new Vector<>();
	private final JCheckBox useHistogramCbox;
	
	private TreeMapViewer(final File input) throws IOException
		{
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		final JMenuBar menubar=new JMenuBar();
		final JMenu menu=new JMenu("File");
		menubar.add(menu);
		this.setJMenuBar(menubar);
		menu.add(new AbstractAction("Open") {
			@Override
			public void actionPerformed(ActionEvent e) {
				TreeMapViewer.newInstance(TreeMapViewer.this);
			}
		});
		menu.add(new AbstractAction("Quit") {
			@Override
			public void actionPerformed(ActionEvent e) {
				TreeMapViewer.this.setVisible(false);
				TreeMapViewer.this.dispose();
			}
		});
		
		
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
						long n=Long.parseLong(tokens[tokens.length-1]);
						if(n<1L) throw new IOException(
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
			final AbstractDataRowTableModel dataRowModel = new AbstractDataRowTableModel() {
				@Override
				protected List<DataRow> getDataRowList() {
					return TreeMapViewer.this.dataRows;
					}
				};
				
				
			
			this.setTitle(input.getName());
			final JPanel contentPane=new JPanel(new BorderLayout(5,5));
			contentPane.setBorder(new EmptyBorder(5,5,5,5));
			this.setContentPane(contentPane);
			final JTabbedPane tabbedPane=new JTabbedPane();
			
			final JTable dataTable=new JTable(dataRowModel);
			dataTable.setShowVerticalLines(false);
			dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			tabbedPane.addTab("Data", new JScrollPane(dataTable));
			
			
			final TableModel columnsModel=new AbstractTableModel() {
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
					final ColumnHeader header= columnHeaders.get(rowIndex);
					switch(columnIndex)
						{
						case 1: 
							{
							header.filterType = aValue==null?FilterType.None:FilterType.valueOf(String.valueOf(aValue)); 
							fireTableCellUpdated(rowIndex, columnIndex);
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
							fireTableCellUpdated(rowIndex, columnIndex);
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
					final ColumnHeader header= columnHeaders.get(rowIndex);
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
			final JTable colsTable=new JTable(columnsModel);
			colsTable.setShowVerticalLines(false);

			colsTable.getColumnModel().getColumn(1).setCellEditor(new FilterTypeCellEditor());
			dataTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			JPanel pane2=new JPanel(new BorderLayout(5,5));
			pane2.setBorder(new EmptyBorder(5,5,5,5));
			pane2.add(new JScrollPane(colsTable),BorderLayout.CENTER);
			
			JPanel pane3= new JPanel(new FlowLayout(FlowLayout.TRAILING));
			pane2.add(pane3,BorderLayout.NORTH);
			pane3.add(new JButton(new AbstractAction("Autofill"){
				@Override
				public void actionPerformed(final ActionEvent e) {				
					int rowIdx=colsTable.getSelectedRow();
					if(rowIdx<0) return;
					rowIdx = colsTable.convertRowIndexToModel(rowIdx);
					if(rowIdx<0) return;
					final Set<String> words=new LinkedHashSet<>();
					final ColumnHeader header = columnHeaders.get(rowIdx);
					for(int y=0;y<TreeMapViewer.this.dataRows.size();++y) {
						words.add(TreeMapViewer.this.dataRows.get(y).values[header.index]);
						}
					if(words.isEmpty()) return;
					columnsModel.setValueAt("("+String.join("|", words)+")",rowIdx,2);
					
					}
				}));
			
			
			tabbedPane.addTab("Columns", pane2);

			
			final AbstractDataRowTableModel filteredDataModel = new AbstractDataRowTableModel() {
					@Override
					protected List<DataRow> getDataRowList() {
					return TreeMapViewer.this.filteredDataRows;
					}	
				};
			this.filteredDataRows.addAll(dataRows);
			this.filteredDataTable=new JTable(filteredDataModel);
			this.filteredDataTable.setShowVerticalLines(false);
			this.filteredDataTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
			tabbedPane.addTab("Filtered Data", new JScrollPane(filteredDataTable));
			
			
			pane2=new JPanel(new BorderLayout(5,5));
			pane2.setBorder(new EmptyBorder(5,5,5,5));
			pane3=new JPanel(new FlowLayout());
			pane2.add(pane3,BorderLayout.NORTH);
			for(int x=0;x<4;++x)
				{
				
				Vector<String> items=new Vector<>();
				items.add("");
				for(int y=0;y+1< this.columnHeaders.size();++y) {
					items.add(this.columnHeaders.get(y).label);
				}
				final JComboBox<String> combo=new JComboBox<>(items);
				combo.addItemListener(new ItemListener() {
					@Override
					public void itemStateChanged(ItemEvent e) {
						drawingArea.repaint();
						
					}
					
				});
				pane3.add(combo);
				pane3.add(new JSeparator(SwingConstants.VERTICAL));
				this.selectColumns.add(combo);
				}
			
			pane3.add(this.useHistogramCbox=new JCheckBox("Histogram", false));
			
			
			
			this.drawingArea=new JPanel(null){
				@Override
				protected void paintComponent(Graphics g1) {
					if(!this.isVisible()) return;
					paintDrawingArea(Graphics2D.class.cast(g1));
					
					}
			};
			this.drawingArea.setOpaque(true);
			this.drawingArea.setBackground(Color.WHITE);
			pane2.add(drawingArea,BorderLayout.CENTER);
			tabbedPane.addTab("TreeMap",pane2);
			
			this.useHistogramCbox.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					drawingArea.repaint();
					
				}
			});
			
			
			
			pane2 = new JPanel(new FlowLayout(FlowLayout.LEADING));
			pane2.add(new JLabel("Author: Pierre Lindenbaum PhD"));
			contentPane.add(pane2,BorderLayout.SOUTH);
			
			contentPane.add(tabbedPane,BorderLayout.CENTER);
			
			
			
			
			Dimension dim= Toolkit.getDefaultToolkit().getScreenSize();
			setBounds(100, 100, dim.width-200, dim.height-200);
			
			} 
		finally
			{
			IOUtils.close(r);
			}
		}
	
	
	public void histogram(
			final TreePack root,
			final List<ColumnHeader> selectedHeaders,
			final Graphics2D g,final Rectangle2D area) {
		final List<HistogramRow> histogramrows=new ArrayList<>();
		for(final TreePack child:root.children.values())
			{
			child.fillHistogramRow(histogramrows,new ArrayList<>());
			}
		if(histogramrows.isEmpty())
			{
			histogramrows.add(new HistogramRow(Collections.singletonList(root.label),root.getWeight()));
			}
		
		double colwidth=area.getWidth()/(selectedHeaders.size()+1);
		double rowheight=area.getHeight()/(histogramrows.size()+1);
		double y=0;
		//draw table header
		for(int x=0;x<(selectedHeaders.size()+1);++x) {
			Rectangle2D r= new Rectangle2D.Double(x*colwidth,y,colwidth,rowheight);
			g.setColor(Color.LIGHT_GRAY);
			g.fill(r);
			g.setColor(Color.BLACK);
			g.draw(r);
			r=root.grow(0.9, r);
			root.print(g, r, (x<selectedHeaders.size()?selectedHeaders.get(x).label:
				"TOTAL "+decimalFormat.format(root.weight)));
			}
		y+=rowheight;
		int rowidx=0;
		for(final HistogramRow histrow: histogramrows)
			{
			final Color bckColor=(rowidx++%2==0?Color.WHITE:new Color(251,251,251));
			int x=0;
			for(x=0;x<histrow.values.size();++x) {
				Rectangle2D r= new Rectangle2D.Double(x*colwidth,y,colwidth,rowheight);
				g.setColor(bckColor);
				g.fill(r);
				g.setColor(Color.BLACK);
				g.draw(r);
				r=root.grow(0.9, r);
				root.print(g, r, histrow.values.get(x));
				}
			
			Rectangle2D r= new Rectangle2D.Double(x*colwidth,y,colwidth,rowheight);
			g.setColor(bckColor);
			g.fill(r);
			double barwidth = ((histrow.weight/(double)root.weight))*colwidth;
			r= new Rectangle2D.Double(x*colwidth,y,barwidth,rowheight);
			g.setColor(Color.RED);
			g.fill(r);
			
			
			r= new Rectangle2D.Double(x*colwidth,y,colwidth,rowheight);
			g.setColor(Color.BLACK);
			g.draw(r);
			r=root.grow(0.9, r);
			g.setColor(Color.DARK_GRAY);
			root.print(g, r, decimalFormat.format(histrow.weight)+" "+String.format("(%.2f%%)",((histrow.weight/(double)root.weight)*100.0)));
			y+=rowheight;
			}
		
		
		}

	
	private void paintDrawingArea(final Graphics2D g)
		{
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, drawingArea.getWidth(), drawingArea.getHeight());
		//System.err.println("DRAIWNG AREA:"+ drawingArea.getWidth()+" "+ drawingArea.getHeight());
		final List<ColumnHeader> selectedHeaders=new ArrayList<>(this.selectColumns.size());
		for(JComboBox<String> combox:this.selectColumns)
			{
			int idx= combox.getSelectedIndex();
			if(idx<1) continue;
			selectedHeaders.add(this.columnHeaders.get(idx-1));
			}
		
		final TreePack root=new TreePack("ALL",null);
		long count=0L;
		for(final DataRow row:this.filteredDataRows)
			{
			count+=row.getWeight();
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
		root.weight=count;
		final Rectangle2D area= new Rectangle2D.Double(0,0,drawingArea.getWidth(), drawingArea.getHeight());
		if(useHistogramCbox.isSelected())
			{
			histogram(root, selectedHeaders, g, area);
			}
		else
			{
			root.layout(g,area);
			}
		
		
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

private static double sum(final List<TreePack> items, int start, int end)
    {
    double sum=0;
    while(start<=end)//yes <=
    	{
    	sum+=items.get(start++).getWeight();
    	}
    return sum;
    }

private static List<TreePack> sortDescending(final List<TreePack> items)
    {
	final List<TreePack> L=new ArrayList<TreePack>(items);
    Collections.sort(L,comparator);
    return L;
    }

private static void layout(final List<TreePack> items, int start, int end, final Rectangle2D bounds)
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
        while (mid+1<end)//+1 added in Jan. 2017
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
        while (mid+1<end)//+1 added in Jan. 2017
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

private static double aspect(final double big,final double small,final double a,final double b)
{
    return (big*b)/(small*a/b);
}

private static double normAspect(final double big,final double small,final double a,final double b)
{
	final double x=aspect(big,small,a,b);
    if (x<1) return 1/x;
    return x;
}

private static void layoutBest(final List<TreePack> items, int start, int end, final Rectangle2D bounds)
    {
    sliceLayout(
    		items,start,end,bounds,
            bounds.getWidth()>bounds.getHeight() ? Orientation.HORIZONTAL : Orientation.VERTICAL,
            Direction.ASCENDING);
    }



private static void sliceLayout(final List<TreePack> items, int start, int end, final Rectangle2D bounds, Orientation orientation, Direction order)
    {
		final double total=sum(items, start, end);
        double a=0;
        final boolean vertical=orientation==Orientation.VERTICAL;
       
        for (int i=start; i<=end; i++)
        {
        	final Rectangle2D.Double r=new Rectangle2D.Double();
        	final double b=items.get(i).getWeight()/total;
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

	
	private static void newInstance(final Component parentComponent,final File f) {
		try {
			final TreeMapViewer app=new TreeMapViewer(f);
			app.setVisible(true);
		} catch (Exception e) {
			JOptionPane.showMessageDialog(parentComponent, e.getMessage(),"Cannot create view",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private static void newInstance(final Component parentComponent) {
		try {
			final JFileChooser jfc= new JFileChooser();
			if(jfc.showOpenDialog(parentComponent)!=JFileChooser.APPROVE_OPTION) return;
			newInstance(parentComponent,jfc.getSelectedFile());
		} catch (Exception e) {
			JOptionPane.showMessageDialog(parentComponent, e.getMessage(),"Cannot create view",JOptionPane.ERROR_MESSAGE);
		}
	}
	
	
	
	public static void main(final String[] args) throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() {
			@Override
			public void run() {
				if(args.length==0)
					{
					TreeMapViewer.newInstance(null);
					}
				else
					{
					for(String f:args) {
						TreeMapViewer.newInstance(null,new File(f));
						}
					}
				//newInstance(null,new File("/home/lindenb/jeter.tsv"));
			}
		});
	}
	
}
