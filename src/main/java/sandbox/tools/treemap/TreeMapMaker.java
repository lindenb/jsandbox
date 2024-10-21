package sandbox.tools.treemap;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.geom.Rectangle2D;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.http.CookieStoreUtils;
import sandbox.io.IOUtils;
import sandbox.jcommander.DimensionConverter;
import sandbox.jcommander.NoSplitter;
import sandbox.net.HttpURLInputStreamProvider;
import sandbox.net.URLInputStreamProvider;
import sandbox.net.cache.DirectoryDataCache;
import sandbox.treemap.TreePack;
import sandbox.treemap.TreePacker;

public class TreeMapMaker extends Launcher
	{
	public static final String NODE_NAME="node";
	public static final String WEIGHT_ATTRIBUTE="weight";
	public static final String LABEL_ATTRIBUTE="label";
	protected static final Logger LOG=Logger.builder(TreeMapMaker.class).build();
	private static final  String SVG=  sandbox.svg.SVG.NS;
    private static int MARGIN=10;
    
    @Parameter(names={"-o","--output"},description="output name")
    private Path out = null; 
    @Parameter(names={"--size","--dimension"},description="Image output size",converter=DimensionConverter.StringConverter.class,splitter=NoSplitter.class)
    private Dimension viewRect = new Dimension(1000, 1000);
    @Parameter(names={"--title"},description="title")
    private String mainTitle = "TreeMap";
    @Parameter(names={"--cache"},description="cache images in that directory")
    private Path cacheDirectory = null;
	@Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private Path cookieStoreFile  = null;
	@Parameter(names={"--show-weight"},description="append score to label")
	private boolean show_scores_with_label =false;


    private DirectoryDataCache dataCache = null;
    private int ID_GENERATOR=0;
    
    private String format(double f) {
    	return String.format("%.2f",f);
    }
    
	private class Frame implements TreePack
		{
		private final int ID = ID_GENERATOR++;
		final Element domNode;
		private final List<Frame> children=new ArrayList<>();
		private final Frame parent;
		private Rectangle2D.Double nodeBounds = null;
		private Rectangle2D.Double childrenBounds = null;
		private Rectangle2D.Double titleBounds  = null;
		Frame(Frame parent,final Element domNode)
			{
			this.parent = parent;
			this.domNode = domNode;
			}
		
		
		@Override
		public double getWeight()
			{
			if(isLeaf()) {
				String s = getAttribute(WEIGHT_ATTRIBUTE, null);
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

		
		@Override
		public Rectangle2D getBounds() {
			return this.nodeBounds;
			}
		
		
		protected String getAttribute(final String name,String def)
			{
			final Attr att=this.domNode.getAttributeNode(name);
			return att==null?def:att.getValue();
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
			return getAttribute(LABEL_ATTRIBUTE,null);
			}
		
		public String getLabelAndWeight()
			{
			String s = getLabel();
			if(show_scores_with_label) {
					s=(s==null?"":s+" ");
					s+="("+getWeight()+")";
				}
			return s;
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
			if(!isLeaf()) {
				return null;
			}
			String s= getAttribute("image",null);
			if(StringUtils.isBlank(s)) s=getAttribute("img",null);
			return s;
			}
		
		
	@Override
	public void setBounds(Rectangle2D bounds) {
			this.nodeBounds = new Rectangle2D.Double(bounds.getX(),bounds.getY(),bounds.getWidth(),bounds.getHeight());
			}

		
		int getDepth()
			{
			return parent==null?0:1+parent.getDepth();
			}
		
		private void pack(final TreePacker packer) {
			
			
			this.childrenBounds = new Rectangle2D.Double(nodeBounds.getX(),nodeBounds.getY(),nodeBounds.getWidth(),nodeBounds.getHeight());

			 
			
			if(this.childrenBounds.width>4*MARGIN)
				{
				this.childrenBounds.x+=MARGIN;
				this.childrenBounds.width-=(MARGIN*2);
				}
			else
				{
				final double L=this.childrenBounds.getWidth()*0.05;
				this.childrenBounds.x+=L;
				this.childrenBounds.width-=(L*2.0);
				}
			if(this.childrenBounds.height>4*MARGIN)
				{
				this.childrenBounds.y+=MARGIN;
				this.childrenBounds.height-=(MARGIN*2);
				}
			else
				{
				final double L=this.childrenBounds.getHeight()*0.05;
				this.childrenBounds.y+=L;
				this.childrenBounds.height-=(L*2.0);
				}
			
			if(!StringUtils.isBlank(this.getLabelAndWeight()) && !isLeaf()) {
				   double fract = 0.9;
				   double h2 =this.nodeBounds.getHeight()*fract;
				  
				   this.titleBounds = new Rectangle2D.Double(
						   this.nodeBounds.x,
						   this.nodeBounds.y,
						   this.nodeBounds.width,
						   this.nodeBounds.getHeight() -  h2
						 );
				   this.childrenBounds.y = this.childrenBounds.getMaxY()-h2;
				   this.childrenBounds.height=h2;
				   }
		  packer.layout(this.children,this.childrenBounds);
		 
		   for(final Frame c:this.children)
			   {
			   c.pack(packer);
			   }
			}
		
		private void writeText(final XMLStreamWriter w,String label,final Rectangle2D area) throws Exception {
			
			final int label_length = label.length();
			final double fontSize;
			final boolean rotate = area.getHeight()> 1.5* area.getWidth();
			final double font_scale = 0.7;
			if(rotate) {
				fontSize = Math.min(area.getWidth(),area.getHeight()/label_length)*font_scale;
				}
			else
				{
				fontSize = Math.min(area.getWidth()/label_length,area.getHeight())*font_scale;
				}
			
			
			w.writeStartElement("text");
			w.writeAttribute("x",format(area.getCenterX()));
			w.writeAttribute("y",format(area.getCenterY() /* +fontSize/2.0 */));
			if(rotate) {
				w.writeAttribute("transform","rotate(90,"+format(area.getCenterX())+","+format(area.getCenterY())+")");
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
			
			
			 }
		
		private void svg(final XMLStreamWriter w)throws Exception
		   {
			final   String url= this.getUrl(); 
		   String selector=null;
		   final StringBuilder stylestr = new StringBuilder();
		   w.writeStartElement("g");
		   selector=this.getStyle("stroke",null);
		   if(selector!=null)  stylestr.append("stroke:").append(selector).append(";");
		   selector=this.getStyle("fill",null);
		   if(selector!=null)  {
			   stylestr.append("fill:").append(selector).append(";");
		   		}
		   else
			   	{
			   	float f =  (0.9f-(float)((this.ID/(float)ID_GENERATOR)*0.4f));
				final Color c=   new Color(f,f,f);
				stylestr.append("fill:").append("rgb("+c.getRed()+","+c.getGreen()+","+c.getBlue()+");");
			   	}
		   selector=this.getStyle("stroke-width",String.valueOf(Math.max(0.2,2/(this.getDepth()+1.0))));
		   stylestr.append("stroke-width").append(selector).append(";");
		   
			w.writeAttribute("style",stylestr.toString());
		   
		   if(!StringUtils.isBlank(url)) {
			   w.writeStartElement("a");
			   w.writeAttribute("href", escapeURL(url));
		   		}
		   
		   w.writeStartElement("rect");
		   w.writeAttribute("x",format(this.nodeBounds.getX()));
		   w.writeAttribute("y",format(this.nodeBounds.getY()));
		   w.writeAttribute("width",format(this.nodeBounds.getWidth()));
		   w.writeAttribute("height",format(this.nodeBounds.getHeight()));
		 
		   
		   w.writeStartElement("title");
		   w.writeCharacters(StringUtils.normalizeSpaces(""+this.getDescription()+" ("+getWeight()+")"));
		   w.writeEndElement();
		   	   
		   w.writeEndElement();//rect
		   if(isLeaf()) {
			   String imgPath=null;
			    try {
			    	if(!StringUtils.isBlank(getImage())) {
				    	imgPath=dataCache.getPath(new URL(getImage())).toString();
				    	}
			    	}
			    catch(final Throwable err) {
			    	LOG.warning(err);
			    	imgPath=null;
			    	}
			    if(!StringUtils.isBlank(url)) {
					   w.writeStartElement("a");
					   w.writeAttribute("href", escapeURL(url));
				   		}
				if(!StringUtils.isBlank(imgPath)) {
					w.writeComment(getImage());
					w.writeStartElement("image");
					w.writeAttribute("href",imgPath);
					w.writeAttribute("x",format(this.nodeBounds.getX()));
			   		w.writeAttribute("y",format(this.nodeBounds.getY()));
			   		w.writeAttribute("width",format(this.nodeBounds.getWidth()));
			   		w.writeAttribute("height",format(this.nodeBounds.getHeight()));
					w.writeAttribute("preserveAspectRatio",this.getStyle("preserveAspectRatio","xMidYMid slice"));
					w.writeEndElement();
					}
				if(!StringUtils.isBlank(getLabelAndWeight())) {
					 writeText(w, getLabelAndWeight(),this.nodeBounds);
					}

			   Optional<Dimension> imgDimOpt = Optional.empty();
			   String imgUrl = this.getImage();

			  
			   
			   if(imgDimOpt.isPresent()) {
				   w.writeEmptyElement("img");
				   w.writeAttribute("src", imgUrl);
				   //
				   
			 
			  if(!StringUtils.isBlank(url)) {
				   w.writeEndElement();//a
			   		} 
			   }
		   } else
			   	{
			    if(this.titleBounds!=null) {
			    	if(!StringUtils.isBlank(url)) {
						   w.writeStartElement("a");
						   w.writeAttribute("href", escapeURL(url));
					   		}
			    	 writeText(w, getLabelAndWeight(),this.titleBounds);
			    	 if(!StringUtils.isBlank(url)) {
						   w.writeEndElement();//a
					   		} 
			    	}
				for(Frame c:this.children) c.svg(w);   
			   	}
		   
		   
		  
		   w.writeEndElement();//g
		   }
		}
	
	
	private String escapeURL(final String url) {
		return url;
	}
    
	private Frame build(Frame parent,Element root) {
		final Frame f = new Frame(parent,root);
		for(Node c=root.getFirstChild();c!=null;c=c.getNextSibling()) {
			if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
			final Element E =Element.class.cast(c);
			if(c.getNodeName().equals(NODE_NAME)) {
				f.children.add(build(f,E));
				}
			}
		return f;
		}
    
    @Override
    public int doWork(final List<String> args) {
    	CloseableHttpClient client = null;
		try
			{
			final HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setUserAgent(IOUtils.getUserAgent());
			if(this.cookieStoreFile!=null) {
				final BasicCookieStore cookies = CookieStoreUtils.readTsv(this.cookieStoreFile);
				builder.setDefaultCookieStore(cookies);
				}
			client = builder.build();
			final URLInputStreamProvider urlInputStreamProvider = new HttpURLInputStreamProvider(client);
			this.dataCache = new DirectoryDataCache(this.cacheDirectory);
			DocumentBuilderFactory dbf= DocumentBuilderFactory.newInstance();
			DocumentBuilder db= dbf.newDocumentBuilder();
			
			final String input = oneFileOrNull(args);
			final Document dom;
			if(StringUtils.isBlank(input)) {
				dom = db.parse(System.in);
				} else
				{
				dom = db.parse(input);
				}
			
			final Frame root = build(null,dom.getDocumentElement());

						
			root.setBounds(new Rectangle2D.Double(0,0,this.viewRect.getWidth(),this.viewRect.getHeight()));
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
		finally {
			if(this.dataCache!=null) try{this.dataCache.close();} catch(final Throwable err2) {}
			if(client!=null) try{client.close();} catch(final Throwable err2) {}
			}
		}
	public static void main(final String[] args) {
		new TreeMapMaker().instanceMainWithExit(args);
		}
		
	}
