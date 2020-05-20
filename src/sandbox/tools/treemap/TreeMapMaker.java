package sandbox.tools.treemap;

import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import com.beust.jcommander.Parameter;

import sandbox.IOUtils;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.jcommander.DimensionConverter;
import sandbox.jcommander.NoSplitter;
import sandbox.treemap.TreePack;
import sandbox.treemap.TreePacker;

public class TreeMapMaker extends Launcher
	{
	protected static final Logger LOG=Logger.builder(TreeMapMaker.class).build();
	private static final  String SVG=  sandbox.svg.SVG.NS;
    private static int MARGIN=10;
    
    @Parameter(names={"-o","--output"},description="output name")
    private Path out = null; 
    @Parameter(names={"--size","--dimension"},description="Image output size",converter=DimensionConverter.StringConverter.class,splitter=NoSplitter.class)
    private Dimension viewRect = new Dimension(1000, 1000);
    @Parameter(names={"--title"},description="title")
    private String mainTitle = "TreeMap";

    
    private String format(double f) {
    	return String.format("%.2f",f);
    }
    
	private class Frame implements TreePack
		{
		private final List<Frame> children=new ArrayList<>();
		private Frame parent=null;
		private Map<String,String> properties; 
		private Rectangle2D bounds=new Rectangle2D.Double();
		Frame(final Map<String,String> props)
			{
			this.properties = new HashMap<>(props);
			}
		public boolean hasAttribute(final String id) {
			return !StringUtils.isBlank(getAttribute(id,""));
			}
		public String getParentId() {
			String s = getAttribute("parent", null);
			if(StringUtils.isBlank(s)) s= getAttribute("parent-id", null);
			if(StringUtils.isBlank(s)) s= getAttribute("parent_id", null);
			return s;
			}
		String getId() { return getAttribute("id", null);}
		
		@Override
		public double getWeight()
			{
			if(isLeaf()) {
				String s = getAttribute("weight", null);
				if(StringUtils.isBlank(s)) s = getAttribute("score", null);
				if(StringUtils.isBlank(s)) return 1.0;
				double score = Double.parseDouble(s);
				if(score<=0) throw new IllegalArgumentException("score shoud be > 0 "+this.toString());
				return score;
				}
			else
				{
				return this.children.stream().mapToDouble(X->X.getWeight()).sum();
				}
			}
		
		public boolean isLeaf()
			{
			return children.isEmpty();
			}

		boolean isRoot() {
		return this.parent == null;
		}
		
		@Override
		public Rectangle2D getBounds() {
			return this.bounds;
			}
		@Override
		public void setBounds(final Rectangle2D bounds) {
			this.bounds = new Rectangle2D.Double(
					bounds.getX(),bounds.getY(),bounds.getWidth(),bounds.getHeight());
			}
		
		protected String getAttribute(final String name,String def)
			{
			return this.properties.getOrDefault(name,(def==null?"":def));
			}
		
		protected String getStyle(final String sel,final String def)
			{
			String val=null;
			String style=getAttribute(sel,null);
			if(!StringUtils.isBlank(style)) return style;
			
			style=getAttribute("style",null);
			if(!StringUtils.isBlank(style))
				{
				for(String s:style.split("[;]"))
					{
					int i=s.indexOf(':');
					if(i==-1) continue;
					if(!s.substring(0, i).equals(sel)) continue;
					val=s.substring(i+1);
					break;
					}
				}
			if(val==null)
				{
				return parent==null?def:parent.getStyle(sel, def);
				}
			return val;
			}
		
		public String getLabel()
			{
			return getAttribute("label",null);
			}
		public String getDescription()
			{
			return getAttribute("description",getLabel());
			}
		public String getUrl()
			{
			String s= getAttribute("url",null);
			if(StringUtils.isBlank(s)) s=getAttribute("href",null);
			return s;
			}
		public String getImage()
			{
			if(!isLeaf()) return null;
			String s= getAttribute("image",null);
			if(s==null) s=getAttribute("img",null);
			return s;
			}
		
		
		Rectangle2D getChildrenRect() {
			return getRects()[0];
		}
		
		private Rectangle2D[] getRects()
			{
			Rectangle2D.Double r=new Rectangle2D.Double();
			r.setRect(this.bounds);
			if(parent==null) return new Rectangle2D[]{r,null};
			
			
			if(r.width>4*MARGIN)
				{
				r.x+=MARGIN;
				r.width-=(MARGIN*2);
				}
			else
				{
				double L=r.getWidth()*0.05;
				r.x+=L;
				r.width-=(L*2.0);
				}
			if(r.height>4*MARGIN)
				{
				r.y+=MARGIN;
				r.height-=(MARGIN*2);
				}
			else
				{
				double L=r.getHeight()*0.05;
				r.y+=L;
				r.height-=(L*2.0);
				}
			
			Rectangle2D.Double r2= null;;
			if(StringUtils.isBlank(this.getLabel())) {
				   if(r.getMaxY()+6 < this.bounds.getMaxY())
					   {
					   r2=new Rectangle2D.Double(
							   this.bounds.getX(),
							   r.getMaxY()+2,
							   this.bounds.getWidth(),
							   (this.bounds.getMaxY()-r.getMaxY())-4
							   );
					   }
				   else if(r.getMaxX()+6 < this.bounds.getMaxX())
					   {
					   r2=new Rectangle2D.Double(
							  r.getMaxX(),
							  this.bounds.getY(),
							  (this.bounds.getMaxX()-r.getMaxX()),
							  this.bounds.getHeight()
							  );
					   }
				   }
			
			return new Rectangle2D[]{r,r2};
			}
		
		int getDepth()
			{
			return parent==null?0:1+parent.getDepth();
			}
		
		private void pack(final TreePacker packer) {
		   packer.layout(this.children,this.getChildrenRect());
		   for(final Frame c:this.children)
			   {
			   c.pack(packer);
			   }
			}
		
		private void svg(final XMLStreamWriter w)throws Exception
		   {
		   String url= this.getUrl(); 
		   String selector=null;
		   
		   w.writeStartElement("g");
		   selector=this.getStyle("stroke",null);
		   if(selector!=null)  w.writeAttribute("stroke",selector);
		   selector=this.getStyle("fill",null);
		   if(selector!=null)  w.writeAttribute("fill",selector);
		   selector=this.getStyle("stroke-width",String.valueOf(Math.max(0.2,2/(this.getDepth()+1.0))));
		   w.writeAttribute("stroke-width",selector);
		   
		   if(!StringUtils.isBlank(url)) {
			   w.writeStartElement("a");
			   w.writeAttribute("href", url);
		   		}
		   
		   w.writeStartElement("rect");
		   w.writeAttribute("fill",getStyle("fill",isRoot()?"none":"gray"));
		   w.writeAttribute("x",format(this.bounds.getX()));
		   w.writeAttribute("y",format(this.bounds.getY()));
		   w.writeAttribute("width",format(this.bounds.getWidth()));
		   w.writeAttribute("height",format(this.bounds.getHeight()));
		 
		   
		   w.writeStartElement("title");
		   w.writeCharacters(StringUtils.normalizeSpaces(""+this.getDescription()+" ("+getWeight()+")"));
		   w.writeEndElement();
		   	   
		   w.writeEndElement();//rect
		   if(isLeaf()) {
				if(!StringUtils.isBlank(getImage())) {
					w.writeStartElement("image");
					w.writeAttribute("href",getImage());
					w.writeAttribute("x",format(this.bounds.getX()));
			   		w.writeAttribute("y",format(this.bounds.getY()));
			   		w.writeAttribute("width",format(this.bounds.getWidth()));
			   		w.writeAttribute("height",format(this.bounds.getHeight()));
					w.writeAttribute("preserveAspectRatio",this.getStyle("preserveAspectRatio","xMidYMid slice"));
					w.writeEndElement();
					}
				if(!StringUtils.isBlank(getLabel())) {
					String label = this.getLabel();
					final int label_length = label.length();
					final double fontSize;
					final boolean rotate = this.bounds.getHeight()> 1.5* this.bounds.getWidth();
					
					if(rotate) {
						fontSize = Math.min(this.bounds.getWidth(),this.bounds.getHeight()/label_length);
						}
					else
						{
						fontSize = Math.min(this.bounds.getWidth()/label_length,this.bounds.getHeight());
						}
					
					if(!StringUtils.isBlank(url)) {
					   w.writeStartElement("a");
					   w.writeAttribute("href", url);
				   		}
					
					w.writeStartElement("text");
					w.writeAttribute("x",format(this.bounds.getCenterX()));
					w.writeAttribute("y",format(this.bounds.getCenterY() /* +fontSize/2.0 */));
					if(rotate) {
						w.writeAttribute("transform","rotate(90,"+format(this.bounds.getCenterX())+","+format(this.bounds.getCenterY())+")");
						}
					if(!StringUtils.isBlank(getImage())) {
						w.writeAttribute("fill-opacity","0.7");
						}
					w.writeAttribute("text-anchor",getStyle("text-anchor", "middle"));
					w.writeAttribute("dominant-baseline","central");
					w.writeAttribute("fill",getStyle("text-fill", "blue"));
					w.writeAttribute("font-size",getStyle("font-size", format(fontSize)));
					
					String style = getStyle("font-family",null);
					if(!StringUtils.isBlank(style)) w.writeAttribute("font-family",style);
					//w.writeStartElement("textPath");
					//w.writeAttribute("href","#"+path_id);
					//w.writeAttribute("method","stretch");
					//w.writeAttribute("lengthAdjust","spacingAndGlyphs");
					w.writeCharacters(label);
					//w.writeEndElement();//textPath
					w.writeEndElement();//mtext
					
					 if(!StringUtils.isBlank(url)) {
					    w.writeEndElement();
				   		}
					}

			   Optional<Dimension> imgDimOpt = Optional.empty();
			   String imgUrl = this.getImage();

			   
			   
			   if(imgDimOpt.isPresent()) {
				   w.writeEmptyElement("img");
				   w.writeAttribute("src", imgUrl);
				   //
			   }
		   } else
			   	{
				for(Frame c:this.children) c.svg(w);   
			   	}
		   
		   
		   if(!StringUtils.isBlank(url)) {
			   w.writeEndElement();//a
		   		} 
		   w.writeEndElement();//g
		   }
		}
	
	
	
    
   
    
    @Override
    public int doWork(final List<String> args) {
		try
			{
			final Frame root = new  Frame(Collections.emptyMap());
			root.properties.put("id", "default");
			final Map<String, Frame> id2frame = new HashMap<>();
			id2frame.put(root.getId(),root);
			Map<String, String> item = new HashMap<>();
			try(BufferedReader br = super.openBufferedReader(args)) {
				for(;;) {
					String line = br.readLine();
					if(line==null || StringUtils.isBlank(line)) {
						if(!item.isEmpty()) {
							final Frame frame = new Frame(item);
							if(!StringUtils.isBlank(frame.getId())) {
								if(id2frame.containsKey(frame.getId())) {
									LOG.error("duplicate id "+frame.getId());
									return -1;
									}
								id2frame.put(frame.getId(),frame);
								}
							Frame theParent = root;
							if(!StringUtils.isBlank(frame.getParentId())) {
								theParent = id2frame.get( frame.getParentId());
								if(theParent==null) {
									LOG.error("unknown parent id  '"+ frame.getParentId()+"'");
									return -1;
									}
								}
							frame.parent = theParent;
							theParent.children.add(frame);
							}
						
						if(line==null) break;
						item.clear();
						continue;
						}
					int delim = line.indexOf(':');
					if(delim<=0) delim = line.indexOf("=");
					if(delim<=0) delim = line.indexOf(" ");
					if(delim<=0) {
						LOG.warning("No delimiter in "+line);
						continue;
						}
					final String left = line.substring(0,delim).trim().toLowerCase();
					if(item.containsKey(left)) {
						LOG.error("Duplicate key "+line +" in "+item);
						return -1;
						}
					final String right = line.substring(delim+1).trim();
					if(StringUtils.isBlank(left)) continue;
					if(StringUtils.isBlank(right)) continue;
					item.put(left, right);
					}
				}
			
			final  Rectangle2D drawingArea= new Rectangle2D.Double(0,0,this.viewRect.getWidth(),this.viewRect.getHeight());
			root.bounds.setRect(drawingArea);
			root.pack(new TreePacker());
			LOG.debug(root.children.size());
			try(OutputStream os = IOUtils.openPathAsOutputStream(this.out)) {
				XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
				XMLStreamWriter w= xmlfactory.createXMLStreamWriter(os,"UTF-8");
				w.writeStartDocument("UTF-8","1.0");
				w.writeStartElement("svg");
				w.writeAttribute("style", "dominant-baseline:central;fill:none;stroke:darkgray;stroke-width:0.5px;");
				w.writeAttribute("xmlns",SVG);
				w.writeAttribute("width",format(this.viewRect.getWidth()+1));
				w.writeAttribute("height",format(this.viewRect.getHeight()+1));
				w.writeStartElement("title");
				w.writeCharacters(this.mainTitle);
				w.writeEndElement();
				
				w.writeStartElement("defs");
				
				w.writeEndElement();//defs
				
				
				root.svg(w);
				w.writeEndElement();//svg
				w.writeEndDocument();
				w.flush();
		    	}
			return 0;
			} 
		catch(final Throwable err)
			{
			LOG.error(err);
			return -1;
			}
		}
	public static void main(final String[] args) {
		new TreeMapMaker().instanceMainWithExit(args);
		}
		
	}
