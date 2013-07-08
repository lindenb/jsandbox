package sandbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Comparator;
import java.util.Map;
import java.util.Collections;
import java.util.Properties;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleBindings;
import javax.swing.JOptionPane;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Attr;


import org.scribe.builder.*;
import org.scribe.builder.api.*;
import org.scribe.model.*;
import org.scribe.oauth.*;


// http://www.flickr.com/services/api/flickr.photos.search.html
/* 
<?xml version="1.0" encoding="utf-8" ?>
<rsp stat="ok">
    <photo id="7154235545" owner="36500748@N04" secret="cb0ee4c0ed" server="7085" farm="8" title="_descabelada" ispublic="1" isfriend="0" isfamily="0" license="0" tags="pictures city red brazil urban music woman girl brasil canon photo model girlfriend exposure foto photographer sãopaulo mulher modelo vermelho namorada sp ibirapuera garota urbano música rockandroll exposição oca metrópole canonef50mmf14usm parquedoibirapuera cenaurbana letsrock canonef50mm cidadesbrasileiras clicksp cityofsaopaulo yourcountry abnermerchan lineimarani" ownername=".merchan" url_q="http://farm8.staticflickr.com/7085/7154235545_cb0ee4c0ed_q.jpg" height_q="150" width_q="150">
      <description>Exposição Let's Rock
Oca - Parque Ibirapuera
São Paulo - SP

&lt;b&gt;&amp;gt;&amp;gt;&lt;/b&gt; &lt;a href=&quot;http://www.flickr.com/photos/abnermerchan/&quot; target=&quot;blank&quot;&gt;Flickr&lt;/a&gt; | &lt;a href=&quot;http://www.gettyimages.com/Search/Search.aspx?assettype=image&amp;amp;artist=Abner+Merchan&quot; target=&quot;blank&quot; rel=&quot;nofollow&quot;&gt;Getty Images&lt;/a&gt; | &lt;a href=&quot;http://500px.com/abnermerchan&quot; target=&quot;blank&quot; rel=&quot;nofollow&quot;&gt;500px&lt;/a&gt; | &lt;a href=&quot;http://twitter.com/abnermerchan&quot; target=&quot;blank&quot; rel=&quot;nofollow&quot;&gt;Twitter&lt;/a&gt; | &lt;a href=&quot;http://www.facebook.com/abnermerchan&quot; target=&quot;blank&quot; rel=&quot;nofollow&quot;&gt;Facebook&lt;/a&gt; | &lt;a href=&quot;http://pinterest.com/abnermerchan/&quot; target=&quot;blank&quot; rel=&quot;nofollow&quot;&gt;Pinterest&lt;/a&gt;

&lt;i&gt;Copyright © 2012, Abner Merchan - Todos os direitos reservados.&lt;/i&gt;</description>
    </photo>
 
 * 
 */
public class FlickrRss
	{
	private static Logger LOG=Logger.getLogger("flickr.search");
	private static final String BASE_REST="http://api.flickr.com/services/rest/";
	private enum Format { html,atom};
	private Format format=Format.html;
	private boolean printMime=false;
	private TreeSet<Photo> photos=new TreeSet<FlickrRss.Photo>();
	private int max_count=-1;
	private int start_index=0;
	private ScriptEngine jsEngine;
	private String dateStart=null;
	private String dateEnd=null;
	private boolean enableGroup=true;
	private boolean sort_on_views=false;
	private Properties flickrProperties=new Properties();
	private OAuthService service;
	private org.scribe.model.Token accessToken;
	private boolean use_priority=true;
	
	public class Photo implements Comparable<Photo>
		{
		Long id=null;
		String owner=null;
		String ownername=null;
		String secret=null;
		String _title=null;
		String license=null;
		String description=null;
		String server=null;
		String farm=null;
		String tags=null;
		Long dateupload=null;
		String date_taken="";//e.g: 2012-06-26 19:06:42
		int o_width;
		int o_height;
		int views=0;
		int priority=1;
		
		public int getPriority()
			{
			return (use_priority?priority:1);
			}
		
		public String[] getTags()
			{
			return (this.tags==null?"":this.tags).split("[ \t]+");
			}
		public String getOwner()
			{
			return this.owner==null?"":this.owner;
			}
		
		public String getOwnerName()
			{
			return this.ownername==null?getOwner():this.ownername;
			}
		
		public int getWidth()
			{
			if(this.o_width>this.o_height)
				{
				return 240;
				}
			else
				{
				return (int)((240.0/o_height)*o_width);
				}
			}
		public int getHeight()
			{
			if(this.o_width>this.o_height)
				{
				return (int)((240.0/o_width)*o_height);
				}
			else
				{
				return 240;
				}
			}
		public String getDescription()
			{
			return description==null?"":description.toLowerCase();
			}
		public String getTitle()
			{
			return _title==null?"":_title.toLowerCase();
			}
		
		public String getLicense()
			{
			return license==null?"":this.license;
			}
		
		public String getPageURL()
			{
			return "http://www.flickr.com/photos/"+this.owner+"/"+id+"/";
			}
		
		public String getPhotoURL()
			{
			return "http://farm"+farm+".staticflickr.com/"+server+"/"+id+"_"+secret+"_"+"m"+".jpg";
			}
		@Override
		public String toString()
			{
			return "PHOTO ID."+this.id+" title:"+getTitle();
			}
		
		@Override
		public int hashCode() {
			return id.hashCode();
			}
		
		@Override
		public int compareTo(Photo o)
			{
			if(this==o) return 0;
			Photo other=Photo.class.cast(o);
			if(other.id.equals(this.id)) return 0;
			int i=o.getPriority()-this.getPriority();
			if(i!=0) return i;
			return other.id.compareTo(this.id);
			}
		
		@Override
		public boolean equals(Object obj)
			{
			return Photo.class.cast(obj).id.equals(this.id);
			}
		
		void writeAtom(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeStartElement("entry");
			writeSimple(w,"title",this._title);
			w.writeEmptyElement("link");
			w.writeAttribute("href", this.getPageURL());
			w.writeStartElement("author");
			writeSimple(w,"name",this.owner);
			w.writeEndElement();
			w.writeEndElement();

			}
		void writeHtml(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeStartElement("div");
			w.writeAttribute("style","margin:10px;background-color:lightgray;");
			w.writeStartElement("a");
			w.writeAttribute("title", this.getTitle());
			w.writeAttribute("href", this.getPageURL());
			w.writeEmptyElement("img");
			w.writeAttribute("src", this.getPhotoURL());
			w.writeAttribute("style","max-width:240px;");
			if(getWidth()>0 && getHeight()>0)
				{
				w.writeAttribute("width",String.valueOf(this.getWidth()));
				w.writeAttribute("height",String.valueOf(this.getHeight()));
				}
			else
				{
				w.writeAttribute("height","240");
				}
			w.writeEndElement();//a
			
			w.writeEmptyElement("br");
			w.writeCharacters(this._title.length()>20?this._title.substring(0,17)+"...":this._title);
			w.writeEmptyElement("br");
			w.writeCharacters("by "+getOwnerName());
			w.writeEndElement();
			}
		}
	
	private FlickrRss() throws Exception
		{
		ScriptEngineManager mgr = new ScriptEngineManager();
		this.jsEngine = mgr.getEngineByName("JavaScript");
		}
	
	
	protected Photo parsePhoto(StartElement start,XMLEventReader r) throws IOException,XMLStreamException
		{
		Photo p=new Photo();
		Attribute att=start.getAttributeByName(new QName("id"));
		if(att!=null) p.id=new Long(att.getValue());
		att=start.getAttributeByName(new QName("secret"));
		if(att!=null) p.secret=att.getValue();
		att=start.getAttributeByName(new QName("owner"));
		if(att!=null) p.owner=att.getValue();
		att=start.getAttributeByName(new QName("ownername"));
		if(att!=null) p.ownername=att.getValue();

		att=start.getAttributeByName(new QName("title"));
		if(att!=null) p._title=att.getValue();
		att=start.getAttributeByName(new QName("license"));
		if(att!=null) p.license=att.getValue();
		att=start.getAttributeByName(new QName("server"));
		if(att!=null) p.server=att.getValue();
		att=start.getAttributeByName(new QName("farm"));
		if(att!=null) p.farm=att.getValue();
		att=start.getAttributeByName(new QName("license"));
		if(att!=null) p.license=att.getValue();
		att=start.getAttributeByName(new QName("tags"));
		if(att!=null) p.tags=att.getValue();
		att=start.getAttributeByName(new QName("dateupload"));
		if(att!=null) p.dateupload= new Long(att.getValue());
		att=start.getAttributeByName(new QName("datetaken"));
		if(att!=null) p.date_taken= att.getValue();
		att=start.getAttributeByName(new QName("o_width"));
		if(att!=null) p.o_width= Integer.parseInt(att.getValue());
		att=start.getAttributeByName(new QName("o_height"));
		if(att!=null) p.o_height= Integer.parseInt(att.getValue());
		att=start.getAttributeByName(new QName("views"));
		if(att!=null) p.views= Integer.parseInt(att.getValue());
		
		while(r.hasNext())
			{
			XMLEvent evt=r.nextEvent();
			if(evt.isStartElement())
				{
				StartElement e=evt.asStartElement();
				String name=e.getName().getLocalPart();
				if(name.equals("description"))
					{
					p.description=r.getElementText();
					}
				}
			else if(evt.isEndElement())
				{
				EndElement e=evt.asEndElement();
				String name=e.getName().getLocalPart();
				if(name.equals("photo")) break;
				}
			}
		return p;
		}
	
	
	protected List<Photo> parseUrl(OAuthRequest request)throws IOException,XMLStreamException
		{
		service.signRequest(accessToken, request);
		Response response = request.send();


		List<Photo> L=new ArrayList<FlickrRss.Photo>();
		
		InputStream  in=response.getStream();
		if(in==null) return L;
		XMLInputFactory xmlInputFactory = XMLInputFactory.newInstance();
		xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
		XMLEventReader r= xmlInputFactory.createXMLEventReader(in);	
	
		while(r.hasNext())
			{
			XMLEvent evt=r.nextEvent();
			if(evt.isStartElement())
				{
				StartElement e=evt.asStartElement();
				String name=e.getName().getLocalPart();
				if(name.equals("photo"))
					{
					Photo photo=parsePhoto(e, r);
					L.add(photo);
					}
				else if(name.equals("err"))
					{
					System.err.println("ERROR:"+r.getElementText());
					System.exit(-1);
					}

				}
			}
		r.close();
		in.close();
		return L;
		}
	
	private static void writeSimple(XMLStreamWriter w,String tag,String content) throws XMLStreamException
		{
		w.writeStartElement(tag);
		w.writeCharacters(content);
		w.writeEndElement();
		}
	
	private void dump() throws XMLStreamException,IOException
		{
		
		XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
		XMLStreamWriter w= xmlfactory.createXMLStreamWriter(System.out,"UTF-8");
		
		if(printMime)
			{
			switch(this.format)
				{
				case atom:System.out.print("Content-type: application/atom+xml\n\n");break;
				default:System.out.print("Content-type: text/html\n\n");break;
				}
			}
		switch(this.format)
			{
			case atom:
				{
				w.writeStartDocument("UTF-8","1.0");
				w.writeStartElement("feed");
				w.writeAttribute("xmlns","http://www.w3.org/2005/Atom");
				writeSimple(w,"title","flickr");
				writeSimple(w,"updated",new SimpleDateFormat("yyyy-MM-dd'T'h:m:ssZ").format(new Date()));
				writeSimple(w,"subtitle","flickr");
				writeSimple(w,"generator",getClass().getName());
				
				for(Photo photo: this.photos)
					{
					photo.writeAtom(w);
					}
				w.writeEndElement();
				w.writeEndDocument();
				break;
				}
			case html:
				{
				final int num_cols=4;
				int x=0;
				w.writeStartElement("html");
				w.writeStartElement("body");
				w.writeStartElement("table");
				List<Photo> sorted=new ArrayList<Photo>(this.photos);
				if(this.sort_on_views)
					{
					Collections.sort(sorted,new Comparator<Photo>()
						{
						@Override
						public int compare(Photo a, Photo b)
							{
							int i=b.views-a.views;//inverse
							if(i!=0) return i;
							return a.compareTo(b);
							} 
						});
					}
				
				for(Photo photo: sorted)
					{
					if(x==0) w.writeStartElement("tr");
					w.writeStartElement("td");
					photo.writeHtml(w);
					w.writeEndElement();
					++x;
					if(x==num_cols)
						{
						w.writeEndElement();
						x=0;
						}
					}
				if(x!=0) w.writeEndElement();//tr
				w.writeEndElement();//table
				w.writeEndElement();
				w.writeEndElement();
				break;
				}
			}
				
		w.flush();
		w.close();
		System.out.println();
		}
	
	private Map<String,String> getArguments(
			Node root,
			Map<String,String> args)
		{
		args.put("api_key",this.flickrProperties.getProperty("api_key",""));
		
		args.put("extras","o_dims,license,description,owner_name,icon_server,tags,date_upload,views");
		if(root==null) return args;
		for(Node c2=root.getFirstChild();c2!=null;c2=c2.getNextSibling())
			{
			if(c2.getNodeType()!=Node.ELEMENT_NODE) continue;
			if("arg".equals(c2.getNodeName()))
					{
					String attName=Element.class.cast(c2).getAttribute("name");
					if(attName.isEmpty() || args.containsKey(attName)) continue;
					String attValue=c2.getTextContent();
					args.put(attName, attValue);
					}
			}
		Node parent=root.getParentNode();
		return getArguments(parent,args);
		}
	
	private String getScript(Node root,String js)
		{
		if(root==null) return js;
		for(Node c2=root.getFirstChild();c2!=null;c2=c2.getNextSibling())
			{
			if(c2.getNodeType()!=Node.ELEMENT_NODE) continue;
			if(!("script".equals(c2.getNodeName()))) continue;
			js=c2.getTextContent()+"\n"+js;
			}
		Node parent=root.getParentNode();
		return getScript(parent,js);
		}
	
	private void recursive(Node root)throws IOException,XMLStreamException
		{
		for(Node c1=root.getFirstChild();
			c1!=null;
			c1=c1.getNextSibling())
			{
			if("div".equals(c1.getNodeName()))
				{
				recursive(c1);
				continue;
				}
			else if ( "query".equals(c1.getNodeName()))
				{
				OAuthRequest request = new OAuthRequest(Verb.POST, BASE_REST);
				
				Map<String,String> args=getArguments(c1, new HashMap<String,String>());
				String script=getScript(c1,"");
				//System.err.println("script:"+script);
				for(String attName:args.keySet())
					{
					String attValue=args.get(attName);
					LOG.info(attName+":"+attValue);
					request.addBodyParameter(attName,attValue);
					}
				if(dateStart!=null)
					{
					request.addBodyParameter("min_upload_date",dateStart);
					}
				if(dateEnd!=null)
					{
					request.addBodyParameter("max_upload_date",dateEnd);
					}
			
				SimpleBindings bind=new SimpleBindings();
				
				List<Photo> L=new ArrayList<Photo>();
				
				if(enableGroup || "flickr.groups.pools.getPhotos".equals(args.get("method"))==false )
					{
					L=parseUrl(request);
					}
				Node att=c1.getAttributes().getNamedItem("priority");
				if(att!=null && this.use_priority)
					{
					int priority=Integer.parseInt(Attr.class.cast(att).getValue());
					for(Photo p:L) p.priority+=priority;
					}
				if(!script.isEmpty())
					{
					for(Photo p:L)
						{
						if(p==null || p.id==null) continue;
						bind.put("photo", p);
						try
							{
							Object ret=null;
							if((ret=jsEngine.eval(script,bind)).equals(Boolean.TRUE))
								{
								this.photos.add(p);
								}
							else
								{
								}
							}
						catch (Throwable e)
							{
							System.err.println("Error with photo "+p.id);
							e.printStackTrace();
							break;
							}
						}
					}
				else
					{
					this.photos.addAll(L);
					}
				if(this.max_count!=-1)
					{
					while(!this.photos.isEmpty() &&
						  this.photos.size()> (this.start_index+this.max_count) )
						{
						Photo last=this.photos.last();
						this.photos.remove(last);
						}
					}
				}
			}	
		}

	
	private void run(File config) throws IOException,XMLStreamException
		{
	
		try {
		    this.service = new ServiceBuilder()
		        .provider(FlickrApi.class)
		        .apiKey(this.flickrProperties.getProperty("api_key",""))
		        .apiSecret(this.flickrProperties.getProperty("api_secret",""))
		        .build();
	    
		    org.scribe.model.Token requestToken = service.getRequestToken();
		    LOG.info("got request token");
		    
		    this.accessToken=null;
		    boolean update_token_file=true;
		    File prefs=new File(System.getProperty("user.home"),".flickr_api.tokens.txt");
		    if(prefs.exists())
		    	{
		    	BufferedReader in=new BufferedReader(new FileReader(prefs));
		    	String token=in.readLine();
		    	String secret=in.readLine();
		    	String raw=in.readLine();
		    	in.close();
		    	if(token!=null && secret!=null)
		    		{
		    		this.accessToken=new org.scribe.model.Token(token,secret,raw);
		    		}
		    	update_token_file=false;
		    	}
		    
		    if(this.accessToken==null || accessToken.isEmpty())
		    	{
		    	System.err.println("GO TO URL:\n"+service.getAuthorizationUrl(requestToken));
		    	System.err.println();
		    	Verifier verifier = new Verifier(JOptionPane.showInputDialog("Token?"));
		    	this.accessToken = service.getAccessToken(requestToken, verifier);
		    	}
		   
		    
			DocumentBuilderFactory factory=DocumentBuilderFactory.newInstance();
			factory.setNamespaceAware(false);
			DocumentBuilder builder=factory.newDocumentBuilder();
			Document dom=builder.parse(config);
			Element root=dom.getDocumentElement();
			recursive(root);
			//remove head
			LOG.info("remove head "+this.photos.size());
			for(int i=0;i<this.start_index && !this.photos.isEmpty();++i)
				{
				Iterator<Photo> iter=this.photos.iterator();
				iter.next();
				iter.remove();
				}
			LOG.info("dump "+this.photos.size());
			dump();
			
			if(update_token_file)
				{
				PrintWriter pw=new PrintWriter(prefs);
				pw.println(this.accessToken.getToken());
				pw.println(this.accessToken.getSecret());
				pw.println(this.accessToken.getRawResponse());
				pw.flush();
				pw.close();
				}
			LOG.info("Done.");
			System.exit(0);
			}
		catch (Exception e) 
			{
			e.printStackTrace();
			}
		}
	
	public static void main(String[] args) throws Exception
		{
		FlickrRss app=new FlickrRss();
		int optind=0;
		while(optind< args.length)
			{
			if(args[optind].equals("-h") ||
			   args[optind].equals("-help") ||
			   args[optind].equals("--help"))
				{
				System.err.println("Options:");
				System.err.println(" -h help; This screen.");
				System.err.println(" -proxyHost <host>.");
				System.err.println(" -proxyPort <port>.");
				System.err.println(" -n <count>.");
				System.err.println(" -s <start0>.");
				System.err.println(" --date-start <YYYYMMDD>.");
				System.err.println(" --date-end <YYYYMMDD>.");
				System.err.println(" --html --atom ");
				System.err.println(" --mime ");
				System.err.println(" --no-group ");
				System.err.println(" --sort-views ");
				System.err.println(" -p do not use priority");
				return;
				}
			else if(args[optind].equals("-p"))
				{
				app.use_priority=false;
				}
			else if(args[optind].equals("--sort-views"))
				{
				app.sort_on_views=true;
				}
			else if(args[optind].equals("--no-group"))
				{
				app.enableGroup=false;
				}
			else if(args[optind].equals("--date-start"))
				{
				app.dateStart=args[++optind];
				}
			else if(args[optind].equals("--date-end"))
				{
				app.dateEnd=args[++optind];
				}
			else if(args[optind].equals("--date"))
				{
				app.dateStart=args[++optind];
				app.dateEnd=app.dateStart+" 23:59:59";
				app.dateStart+=" 00:00:01";
				}
			else if(args[optind].equals("-proxyHost"))
				{
				System.setProperty("http.proxyHost", args[++optind]);
				}
			else if(args[optind].equals("-proxyPort"))
				{
				System.setProperty("http.proxyPort", args[++optind]);
				}
			else if(args[optind].equals("-n") && optind+1< args.length)
				{
				app.max_count=Integer.parseInt(args[++optind]);
				}
			else if(args[optind].equals("-s") && optind+1< args.length)
				{
				app.start_index=Integer.parseInt(args[++optind]);
				}	
			else if(args[optind].equals("-L") && optind+1< args.length)
				{
				LOG.setLevel(Level.parse(args[++optind]));
				}
			else if(args[optind].equals("--atom"))
				{
				app.format=Format.atom;
				}
			else if(args[optind].equals("--html"))
				{
				app.format=Format.html;
				}
			else if(args[optind].equals("--mime"))
				{
				app.printMime=true;
				}
			else if(args[optind].equals("--"))
				{
				optind++;
				break;
				}
			else if(args[optind].startsWith("-"))
				{
				System.err.println("Unknown option "+args[optind]);
				return;
				}
			else 
				{
				break;
				}
			++optind;
			}
		
		FileInputStream isf=new FileInputStream(new File(System.getProperty("user.home"), ".flickr_api.xml"));
		app.flickrProperties.loadFromXML(isf);
		isf.close();
		
		if(optind+1!=args.length)
			{
			System.err.println("Illegal number of arguments.");
			return;
			}
		String filename=args[optind++];
		app.run(new File(filename));
		LOG.info("Done");
		}
}
