package sandbox.ig;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpHost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.beust.jcommander.Parameter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import sandbox.IOUtils;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.http.CookieStoreUtils;
/**
```
$ crontab -e

0 0-23 * * * /home/lindenb/bin/insta2atom.sh

$ cat /home/lindenb/bin/insta2atom.sh

```bash
#!/bin/bash

java -jar ${HOME}/src/jsandbox/dist/insta2atom.jar |\
	xmllint --format --output  "${HOME}/public_html/feed/instagram.xml" -
```


 */
public class InstagramToAtom extends Launcher {
	private static final Logger LOG = Logger.builder(InstagramToAtom.class).build();
	private int ID_GENERATOR=0;
	@Parameter(names={"-t","--tumb-size"},description="Thumb size.")
	private int thumb_size =256;
	@Parameter(names={"-H","--hours"},description="print items published less than 'x' hours. negative: don't use.")
	private int filter_last_xx_hours = -1;
	@Parameter(names={"-s","--seconds"},description="Sleep s seconds between each calls.")
	private int sleep_seconds = 5;
	@Parameter(names={"-d","--directory"},description="Cache directory. default: ${HOME}/.insta2atom ")
	private File cacheDirectory  = null;
	@Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private File cookieStoreFile  = null;
	@Parameter(names={"-g","--group"},description="group items per query")
	private boolean group_flag =false;
	@Parameter(names={"-N","--max-items"},description="max-items")
	private int max_images_per_qery = 50;
	@Parameter(names={"-new","--new"},description="Only show new items since last call of this tool")
	private boolean what_is_new_flag = false;

	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private InstagramJsonScraper scraper = null;
	private final XPath xpath = XPathFactory.newInstance().newXPath();

	
	
		
	private String getUrl(final String queryName) {
		return "https://www.instagram.com"+
				(queryName.startsWith("/")?"":"/") + 
				queryName +
				(queryName.endsWith("/")?"":"/");
		}
		
		
		private Integer parseCount(final JsonElement je)
			{
			if(je==null ||!je.isJsonObject()) return null;
			final JsonObject jObject = je.getAsJsonObject();
			if(!jObject.has("count")) return null;
			return jObject.get("count").getAsInt();
			}
		private void parseOwner(final Image img,final JsonElement je)
			{
			if(je==null ||!je.isJsonObject()) return ;
			final JsonObject jObject = je.getAsJsonObject();
			if(jObject.has("id")) {
				img.setData("owner",jObject.get("id").getAsString());
				}
			}
		
		/*
		private Element parseThumbail(final Document owner,final JsonObject jObject) {
			final Element th = owner.createElement("img");
			th.setAttribute("class", "thumbail");
			th.setAttribute("width",""+jObject.get("config_width").getAsInt());
			th.setAttribute("height",""+jObject.get("config_height").getAsInt());
			th.setAttribute("src", jObject.get("src").getAsString());
			return th;
			}*/
		
		private Image parseNode(final Document owner,final JsonObject jObject)
			{
			final Image n = new Image(owner,null);
			n.parseDimension(jObject.get("dimensions").getAsJsonObject());
			n.setData("display_url", jObject.get("display_url").getAsString());
			n.setData("edge_liked_by",parseCount(jObject.get("edge_liked_by")));
			n.setData("shortcode", jObject.get("shortcode").getAsString());
			n.setData("taken_at_timestamp",jObject.get("taken_at_timestamp").getAsString());
			n.setData("thumbnail_src",jObject.get("thumbnail_src").getAsString());
			n.setData("id", jObject.get("id").getAsString());
			parseOwner(n,jObject.get("owner"));
			
			/*if(jObject.has("thumbnail_resources")) {
				for(final JsonElement c: jObject.getAsJsonArray("thumbnail_resources"))
					{
					n.div.appendChild(parseThumbail(owner,c.getAsJsonObject()));
					}
				}*/
			n.span.setAttribute("id", n.getId());
			n.anchor.setAttribute("target", "_blank");
			n.anchor.setAttribute("href", n.getPageHref());
			n.anchor.setAttribute("title",
					dateFormatter.format(new Date(n.getTimestampSec()*1000L))+
					" liked by : "+n.getLikedBy()+" new:"+n.is_new
					);
			n.img.setAttribute("src", n.getData("thumbnail_src"));
			n.img.setAttribute("alt", n.getData("thumbnail_src"));
			n.img.setAttribute("width", ""+InstagramToAtom.this.thumb_size);
			return n;
			}
		
		
		private void searchNodes(final Document owner,final Set<Image> nodes,final String key,final JsonElement elt)
			{
			if(elt.isJsonArray())
				{
				final JsonArray jArray = elt.getAsJsonArray();
				for(final JsonElement item:jArray) {
					searchNodes(owner,nodes,key,item);
					}
				}
			else if(elt.isJsonObject())
				{
				final JsonObject jObject = elt.getAsJsonObject();
				if("node".equals(key) && jObject.has("display_url"))
					{
					final Image n = parseNode(owner,jObject);
					if(n!=null) nodes.add(n);
					}
				else
					{
					for(final Map.Entry<String, JsonElement> kv: jObject.entrySet())
						{
						//if(kv.getKey().equals("edge_hashtag_to_top_posts")) continue;
						searchNodes(owner,nodes,kv.getKey(),kv.getValue());
						}
					}
				}
			}
	
	private Set<Image> query(final Document owner,final String queryName) throws Exception
		{
		Optional<JsonObject> jsonObj = scraper.apply(queryName);
		final Set<Image> nodes =  new TreeSet<>();
		if(!jsonObj.isPresent()) return nodes;
		JsonObject root= jsonObj.get();
		searchNodes(owner,nodes,null,root.getAsJsonObject());
		nodes.removeIf(N->N.getSrc()==null && N.getSrc().isEmpty());
		if(nodes.isEmpty())
			{
			LOG.warning("No image found for "+queryName+"."+root);
			}
		return nodes;
		}

	void writeAtom(
		final String query,
		final Set<Image> images,
		final XMLStreamWriter w) throws XMLStreamException {
		if(InstagramToAtom.this.group_flag)
			{
			writeGroupedImages(query,w,images);
			}
		else
			{
			writeSoloImages(query,w,images);
			}
		}
		
		void writeGroupedImages(
			final String query,
			final XMLStreamWriter w,Set<Image> images_to_print) throws XMLStreamException {
			w.writeStartElement("entry");
			
			w.writeStartElement("title");
				w.writeCharacters(query);
			w.writeEndElement();

			w.writeStartElement("id");
			w.writeCharacters(md5(query));
			w.writeEndElement();

			
			w.writeEmptyElement("link");
			w.writeAttribute("href", this.getUrl(query)+"?m="+md5(query));
			
			w.writeStartElement("updated");
				w.writeCharacters(InstagramToAtom.this.dateFormatter.format(new Date()));
			w.writeEndElement();
			
			w.writeStartElement("author");
				w.writeStartElement("name");
					w.writeCharacters(query);
				w.writeEndElement();
			w.writeEndElement();
			
			w.writeStartElement("content"); 
			
			w.writeAttribute("type","xhtml");
			w.writeStartElement("div");
			w.writeDefaultNamespace("http://www.w3.org/1999/xhtml");
			w.writeStartElement("p");  
			
	
			for(final Image image_url:images_to_print.stream().sorted().collect(Collectors.toList())) {
				w.writeStartElement("a");
				w.writeAttribute("id",String.valueOf(++ID_GENERATOR));
				w.writeAttribute("target", "_blank");
				w.writeAttribute("href", image_url.getShortCode().isEmpty()?this.getUrl(query):image_url.getPageHref());
				image_url.write(w);
				w.writeEndElement();//a
				}
			w.writeEndElement();//p
			w.writeEndElement();//div

			w.writeEndElement();//content
			
			w.writeEndElement();//entry
		}
		
		void writeSoloImages(
			final String query,	
			final XMLStreamWriter w,Set<Image> images_to_print) throws XMLStreamException {
			for(final Image image_url:images_to_print.stream().sorted().collect(Collectors.toList())) {
				w.writeStartElement("entry");
				
				w.writeStartElement("title");
					w.writeCharacters(query);
				w.writeEndElement();

				w.writeStartElement("id");
				w.writeCharacters(md5(image_url.getSrc()));
				w.writeEndElement();

				
				w.writeEmptyElement("link");
				w.writeAttribute("href", 
						image_url.getShortCode().isEmpty()?
						getUrl(query)+"?m="+md5(image_url.getSrc()):
						image_url.getPageHref()
						);
				
				w.writeStartElement("updated");
					w.writeCharacters(InstagramToAtom.this.dateFormatter.format(new Date()));
				w.writeEndElement();
				
				w.writeStartElement("author");
					w.writeStartElement("name");
						w.writeCharacters(query);
					w.writeEndElement();
				w.writeEndElement();
				
				w.writeStartElement("content"); 
				w.writeAttribute("type","xhtml");
				
				w.writeStartElement("div");
				w.writeDefaultNamespace("http://www.w3.org/1999/xhtml");
				w.writeStartElement("p");  
				
				w.writeStartElement("a");
				w.writeAttribute("id",String.valueOf(++ID_GENERATOR));
				w.writeAttribute("target", "_blank");
				w.writeAttribute("href", image_url.getShortCode().isEmpty()?this.getUrl(query)+"?m="+md5(image_url.getSrc()):image_url.getPageHref());
				image_url.write(w);
				w.writeEndElement();//a
				
				w.writeEndElement();//p
				w.writeEndElement();//div
				w.writeEndElement();//content
				
				w.writeEndElement();//entry
				}
			}
		
		
	
	private String md5(final String s) {
		 MessageDigest md;
		 try {
			 md = MessageDigest.getInstance("MD5");
		 } catch (final Exception err) {
			throw new RuntimeException(err);
		 	}
		md.update(s.getBytes());
		return new BigInteger(1,md.digest()).toString(16);
		}
	
	private File getCacheDir() {
		if(this.cacheDirectory==null) {
			final File userDir = new File(System.getProperty("user.home","."));
			this.cacheDirectory =  new File(userDir,".insta2atom");
			}
		return this.cacheDirectory;
		}

	private List<File> getQueryFiles() {
		final File dir= this.getCacheDir();
		if(dir==null ||!dir.exists() || !dir.isDirectory() ) {
			return Collections.emptyList();
			}
		return Arrays.asList(dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(final File f) {
				return f!=null && f.canRead() && f.isFile() &&
						f.getName().endsWith(".html");
			}
		}));
		}

	private void convertIgFilesToHtml()
		{
		final File dir= this.getCacheDir();
		if(dir==null ||!dir.exists() || !dir.isDirectory() ) return;
		final File igFiles[]=dir.listFiles(new FileFilter() {
			@Override
			public boolean accept(final File f) {
				return f!=null && f.canRead() && f.isFile() &&
						f.getName().endsWith(".ig");
			}
			});
		if(igFiles==null ||igFiles.length==0) return;
		for(final File igFile:igFiles)
				{
				PrintWriter pw = null;
				try
					{
					final String name = igFile.getName();
					final File htmlFile = new File(dir,name.substring(0,name.length()-3)+".html");
					if(htmlFile.exists()) continue;
					final String query= IOUtils.slurp(igFile).trim();
					if(query.isEmpty()) continue;
					LOG.info("converting "+igFile+" to "+htmlFile+" "+query);
					pw=  new PrintWriter(htmlFile);
					pw.println("<html><head><title>"+query+"</title></head><body></body></html>");
					pw.flush();
					pw.close();
					pw=null;
					igFile.delete();
					}
				catch(IOException err)
					{
					LOG.warning(err);
					}
				finally
					{
					if(pw!=null) pw.close();	
					}
				}
		}
	
	private class Image
		implements Comparable<Image>
		{
		final Element span;
		final Element anchor;
		final Element img;
		final boolean is_new;
		Image(final Document dom,final Element span) {
			if(span==null)
				{
				this.span = dom.createElement("span");
				this.img = dom.createElement("img");
				this.anchor = dom.createElement("a");
				this.anchor.setAttribute("target","_blank");

				this.span.appendChild(this.anchor);
				this.anchor.appendChild(this.img);
				this.is_new = true;
				}
			else
				{
				this.span = span;
				this.is_new = false;
				try {
					this.anchor=(Element)xpath.evaluate("a", this.span, XPathConstants.NODE);
					this.img=(Element)xpath.evaluate("img", this.anchor, XPathConstants.NODE);
					} catch(XPathExpressionException err) {
					throw new RuntimeException(err);
					}
				}
			}
		
		@Override
       public boolean equals(final Object obj)
               {
               if(obj==this) return true;
               if(obj==null || !(this instanceof Image)) return false;
               final Image other=Image.class.cast(obj);
               if (compareTo(other)==0)return true;
               if(this.getId()!=null && other.getId()!=null && this.getId().equals(other.getId())) return true;
               if(this.getShortCode()!=null && other.getShortCode()!=null &&
            		   	getShortCode().equals(other.getShortCode())) return true;
               return false;
               }

		
		void write(final XMLStreamWriter w) throws XMLStreamException
          {
           w.writeEmptyElement("img");
           w.writeAttribute("id",getShortCode());
           w.writeAttribute("alt", "liked by : "+ this.getLikedBy()+" new:"+this.is_new);
           w.writeAttribute("src", this.getSrc());
           w.writeAttribute("width",String.valueOf(InstagramToAtom.this.thumb_size));
           
         
           //w.writeAttribute("height",getData("height"));
           }
		
		public String getId() {
			return getData("id");
		}

		public int getLikedBy() {
			try {
				return Integer.parseInt(getData("edge_liked_by"));
			} catch(Throwable err) {
				LOG.error(err);
				return 0;
			}
		}

		public String getSrc() {
			return getData("thumbnail_src");
		}
		
		String getShortCode(){
			return getData("shortcode");
		}
		
		long getTimestampSec()
			{
			try {
				return Long.parseLong(getData("taken_at_timestamp"));
				}
			catch(final NumberFormatException err)
				{
				LOG.warning("bad taken_at_timestamp");
				return 0L;
				}
			}
		
		public int compareTime(final Image o) {
			final int i = Long.compare(
				o.getTimestampSec(),
				this.getTimestampSec()
				);
			return i!=0?i:this.getShortCode().compareTo(o.getShortCode());
		}
		
		
		@Override
		public int compareTo(final Image o)
			{
			if(o==this) return 0;
			if(this.getShortCode().equals(o.getShortCode())) return 0;
			return this.compareTime(o);
			}
		@Override
		public int hashCode()
			{
			return this.getSrc().hashCode();
			}
		
		public String getPageHref() {
			return "https://www.instagram.com/p/"+getData("shortcode")+"/";
			}

		public String getData(String att) {
			final String attName="data-"+att;
			return this.span.getAttribute(attName);
		}
		
		void setData(final String att,Object o)
			{
			final String attName="data-"+att;
			if(o==null)
				{
				this.span.removeAttribute(attName);
				}
			else
				{
				this.span.setAttribute("data-"+att, String.valueOf(o));
				}
			}
		
		void parseDimension(final JsonObject jObject)
			{
			int w = jObject.get("width").getAsInt();
			int h = jObject.get("height").getAsInt();
			this.setData("width",w);
			this.setData("height",h);
			}
		boolean isLessThanXXHours() {
			if(filter_last_xx_hours<=0) return true;
			final Calendar cal = Calendar.getInstance();
			// remove next line if you're always using the current time.
			cal.setTime(new Date());
			cal.add(Calendar.HOUR, -1 * filter_last_xx_hours);
			final Date xxxHourBack = cal.getTime();
			final Date imgDate = new Date(getTimestampSec()*1000);
			return imgDate.compareTo(xxxHourBack) >= 0;
			}
		}
	
	private int query(final File htmlFile,XMLStreamWriter watom) throws Exception
		{
		final DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
		final DocumentBuilder db;
		final Document dom;
		try {
			db = dbf.newDocumentBuilder();
			}
		catch(final Exception err) {
			LOG.error(err);
			return -1;
			}
		try
			{
			dom = db.parse(htmlFile);
			}
		catch(final Exception err) {
			LOG.error(err);
			return -1;
			}
		final Element root= dom.getDocumentElement();
		if(!root.getNodeName().equals("html")) {
			LOG.warning("root is not <html>");
			return -1;
		}
		
		
		final String qName=(String)xpath.evaluate("/html/head/title/text()",dom, XPathConstants.STRING);
		if(qName==null || qName.isEmpty()) return -1;
		LOG.info("query is "+qName+" "+htmlFile);
		final Element body = (Element)xpath.evaluate("/html/body", dom, XPathConstants.NODE);
		
		final Set<Image> oldImages = new TreeSet<>();

		//remove text
		final NodeList txtList = (NodeList)xpath.evaluate("span/text()", body, XPathConstants.NODESET);
		
		for(int x=0;x< txtList.getLength();++x) {
			final Node c1= txtList.item(x);
			c1.getParentNode().removeChild(c1);
			}
		// get imaes
		final NodeList nodeList = (NodeList)xpath.evaluate("span[a/img]", body, XPathConstants.NODESET);
		
		for(int x=0;x< nodeList.getLength();++x) {
			final Node c1= nodeList.item(x);
			final Element e1=Element.class.cast(c1);
			oldImages.add(new Image(dom,e1));
			e1.getParentNode().removeChild(e1);
			}
		
		final Set<Image> newImages ;
		try {
		    newImages = query(dom, qName);
		    }
		catch(final Exception err) {
			return -1;
			}
		
		if(newImages==null) return -1;
		newImages.addAll(oldImages);
		final LinkedList<Image> list = new LinkedList<>(newImages);
		while(list.size()>this.max_images_per_qery )
			{
			list.removeLast();
			}
		
		for(final Image img:list) {
			body.appendChild(img.span);
		}
		
		
		final Set<Image> atomizableImages = new TreeSet<>(list.
				stream().
				filter(I->I.isLessThanXXHours()).
				filter(I->!this.what_is_new_flag || I.is_new).
				collect(Collectors.toSet()));
		if(!atomizableImages.isEmpty()) {
			LOG.info("saving "+atomizableImages.size()+" images");
			writeAtom(qName, atomizableImages, watom);
		} 
		
		final TransformerFactory xft=TransformerFactory.newInstance();
		final Transformer tr = xft.newTransformer();
		tr.setOutputProperty(OutputKeys.METHOD,"xml");
		tr.setOutputProperty(OutputKeys.INDENT,"yes");
		final File tmpFile = File.createTempFile("tmp.", ".html",htmlFile.getParentFile());
		tmpFile.deleteOnExit();
		tr.transform(new DOMSource(dom), new StreamResult(tmpFile));;
		tmpFile.renameTo(htmlFile);
		tmpFile.delete();
		return 0;
		}
	
	
	@Override
	public int doWork(final List<String> args) {
		
		XMLStreamWriter w = null;
		try
			{			
			final HttpClientBuilder builder = HttpClientBuilder.create();
			final String proxyH = System.getProperty("http.proxyHost");
			final String proxyP = System.getProperty("http.proxyPort");
			if(proxyH!=null && proxyP!=null && 
					!proxyH.trim().isEmpty() && 
					!proxyP.trim().isEmpty())
				{
				builder.setProxy(new HttpHost(proxyH, Integer.parseInt(proxyP)));
				}
			builder.setUserAgent(IOUtils.getUserAgent());
			
			if(this.cookieStoreFile!=null) {
				final BasicCookieStore cookies = CookieStoreUtils.readTsv(this.cookieStoreFile);
				builder.setDefaultCookieStore(cookies);
			}
			
			this.scraper = new InstagramJsonScraper(builder.build());
			

			convertIgFilesToHtml();
			
			final XMLOutputFactory xof = XMLOutputFactory.newInstance();
			
			
			
			
			w = xof.createXMLStreamWriter(System.out, "UTF-8");
			w.writeStartDocument("UTF-8", "1.0");
			w.writeStartElement("feed");
			w.writeAttribute("xmlns", "http://www.w3.org/2005/Atom");
			
			w.writeStartElement("title");
			w.writeCharacters(getClass().getSimpleName());
			w.writeEndElement();
			
			w.writeStartElement("updated");
			w.writeCharacters(this.dateFormatter.format(new Date()));
			w.writeEndElement();
			
			
			for(final File queryFile : getQueryFiles())
				{
				LOG.info("query file "+queryFile);
				query(queryFile,w);
				Thread.sleep(this.sleep_seconds*1000);
				}
				
			w.writeEndElement();
			w.writeEndDocument();
			w.flush();
			w.close();
			
			this.scraper.close();this.scraper=null;
			return 0;
			}
		catch(final Exception err)
			{
			LOG.error(err);
			return -1;
			}
		finally
			{
			IOUtils.close(w);
			IOUtils.close(this.scraper);
			}
		
		}
	
	public static void main(final String[] args) {
		new InstagramToAtom().instanceMainWithExit(args);
		}

	
}
