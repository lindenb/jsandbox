package sandbox;

import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.beust.jcommander.Parameter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
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
	@Parameter(names={"-f","--force"},description="Force print only new items, discard the non-updated.")
	private boolean force_print_new_only = false;
	@Parameter(names={"-s","--seconds"},description="Sleep s seconds between each calls.")
	private int sleep_seconds = 5;
	@Parameter(names={"-d","--directory"},description="Cache directory. default: ${HOME}/.insta2atom ")
	private File cacheDirectory  = null;
	@Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private File cookieStoreFile  = null;
	@Parameter(names={"-g","--group"},description="group items per query")
	private boolean group_flag =false;

	
	private final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private CloseableHttpClient client = null;
	private final XPath xpath = XPathFactory.newInstance().newXPath();

	
	
	private static String getAttribute(final StartElement root,final String name) {
		final Attribute att = root.getAttributeByName(new QName(name));
		return att==null?"":att.getValue();
		}
	
	private static boolean hasAttribute(final StartElement root,final String name) {
		return root.getAttributeByName(new QName(name))!=null;
		}
		
		
	private String getUrl(final String queryName) {
		return "https://www.instagram.com"+
				(queryName.startsWith("/")?"":"/") + 
				queryName +
				(queryName.endsWith("/")?"":"/");
		}
		
	private String queryHtml(final String queryName) throws IOException {
		CloseableHttpResponse response = null;
		InputStream httpIn = null;
		try
			{
			response = InstagramToAtom.this.client.execute(new HttpGet(this.getUrl( queryName)));
			httpIn = response.getEntity().getContent();
			final String html = IOUtils.readStreamContent(httpIn);
			return html;
			}
		catch(final IOException err)
			{
			LOG.error(err);
			throw err;
			}
		finally
			{
			IOUtils.close(httpIn);
			IOUtils.close(response);
			}
		}
		
		private Integer parseCount(final JsonElement je)
			{
			if(je==null ||!je.isJsonObject()) return null;
			final JsonObject jObject = je.getAsJsonObject();
			if(!jObject.has("count")) return null;
			return jObject.get("count").getAsInt();
			}
		private String parseOwner(final JsonElement je)
			{
			if(je==null ||!je.isJsonObject()) return null;
			final JsonObject jObject = je.getAsJsonObject();
			if(!jObject.has("id")) return null;
			return jObject.get("id").getAsString();
			}
		
		
		private Element parseThumbail(final Document owner,final JsonObject jObject) {
			final Element th = owner.createElement("img");
			th.setAttribute("class", "thumbail");
			th.setAttribute("width",""+jObject.get("config_width").getAsInt());
			th.setAttribute("height",""+jObject.get("config_height").getAsInt());
			th.setAttribute("src", jObject.get("src").getAsString());
			return th;
			}
		
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
			n.setData("owner",parseOwner(jObject.get("owner")));
			if(jObject.has("thumbnail_resources")) {
				for(final JsonElement c: jObject.getAsJsonArray("thumbnail_resources"))
					{
					//n.div.appendChild(parseThumbail(owner,c.getAsJsonObject()));
					}
				}
			n.div.setAttribute("id", n.getId());
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
						if(kv.getKey().equals("edge_hashtag_to_top_posts")) continue;
						searchNodes(owner,nodes,kv.getKey(),kv.getValue());
						}
					}
				}
			}
	
	private Set<Image> query(final Document owner,final String queryName) throws Exception
		{
		final String windowSharedData = "window._sharedData";
		final String endObject="};";
		String html = queryHtml(queryName);
		int i= html.indexOf(windowSharedData);
		if(i==-1) {
			LOG.error("cannot find "+windowSharedData+" in "+getUrl(queryName));
			return null;
			}
		html=html.substring(i+windowSharedData.length());
		i= html.indexOf("{");
		if(i==-1)
			{
			LOG.error("cannot find '{' after "+windowSharedData+" in "+getUrl(queryName));
			return null;
			}
		html=html.substring(i);
		i = html.indexOf(endObject);
		if(i==-1)
			{
			LOG.error("cannot find  "+endObject+" in "+getUrl(queryName));
			return null;
			}
		html=html.substring(0, i+1);
		final JsonReader jsr = new JsonReader(new StringReader(html));
		jsr.setLenient(true);
		final JsonParser parser = new JsonParser();
		JsonElement root;
		try {
			root = parser.parse(jsr);
			}
		catch(final JsonSyntaxException err) {
			LOG.error(err);
			return null;
			}
		finally
			{
			jsr.close();
			}
		if(!root.isJsonObject())
			{
			LOG.error("root is not json object in "+getUrl(queryName));
			return null;
			}
		final Set<Image> nodes =  new TreeSet<>((I1,I2)->I1.compareTime(I2));
		searchNodes(owner,nodes,null,root.getAsJsonObject());
		nodes.removeIf(N->N.getSrc()==null && N.getSrc().isEmpty());
		if(nodes.isEmpty()) 
			{
			LOG.warning("No image found for "+queryName+".");
			}
		return nodes;
		}
	
	
		
	void writeAtom(
		final String query,
		Set<Image> images,	
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
			
	
			for(final Image image_url:images_to_print.stream().sorted((I1,I2)->I1.compareTime(I2)).collect(Collectors.toList())) {
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
			for(final Image image_url:images_to_print.stream().sorted((I1,I2)->I1.compareTime(I2)).collect(Collectors.toList())) {
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

	private class Image
		implements Comparable<Image>
		{
		final Element div;
		final Element img;
		Image(final Document dom,final Element div) {
			if(div==null)
				{
				this.div = dom.createElement("div");
				this.img = dom.createElement("img");
				this.div.appendChild(this.img);
				}
			else
				{
				this.div = div;
				try {
					this.img=(Element)xpath.evaluate("img", this.div, XPathConstants.NODE);
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
           w.writeAttribute("alt", this.getSrc());
           w.writeAttribute("src", this.getSrc());
           w.writeAttribute("width",getData("width"));
           w.writeAttribute("height",getData("height"));
           }
		
		public String getId() {
			return getData("id");
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
				return 0L;
				}
			}
		
		public int compareTime(final Image o) {
			final int i = Long.compare(
				o.getTimestampSec(),
				this.getTimestampSec()
				);
			return i!=0?i:compareTo(o);
		}
		
		
		@Override
		public int compareTo(final Image o)
			{
			return this.getSrc().compareTo(o.getSrc());
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
			return this.div.getAttribute(attName);
		}
		
		void setData(final String att,Object o)
			{
			final String attName="data-"+att;
			if(o==null)
				{
				this.div.removeAttribute(attName);
				}
			else
				{
				this.div.setAttribute("data-"+att, String.valueOf(o));
				}
			}
		
		void parseDimension(final JsonObject jObject)
			{
			int w = jObject.get("width").getAsInt();
			int h = jObject.get("height").getAsInt();
			this.setData("width",w);
			this.setData("height",h);
			}

		}
	
	private int query(final File htmlFile,XMLStreamWriter w) throws Exception
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
		Element body = (Element)xpath.evaluate("/html/body", dom, XPathConstants.NODE);
		
		final Set<Image> oldImages = new TreeSet<>((I1,I2)->I1.compareTime(I2));
		final NodeList nodeList = (NodeList)xpath.evaluate("div[img]", body, XPathConstants.NODESET);
		
		for(int x=0;x< nodeList.getLength();++x) {
			final Node c1= nodeList.item(x);
			if(c1.getNodeType()!=Node.ELEMENT_NODE) continue;
			final Element e1=Element.class.cast(c1);
			oldImages.add(new Image(dom,e1));
			e1.getParentNode().removeChild(e1);
			}
		
		final Set<Image> newImages = query(dom, qName);
		newImages.addAll(oldImages);
		final LinkedList<Image> list = new LinkedList<>(newImages);
		while(list.size()>100 )
			{
			list.removeLast();
			}
		LOG.info(list);
		for(final Image img:list) {
			body.appendChild(img.div);
		}
		
		final TransformerFactory xft=TransformerFactory.newInstance();
		final Transformer tr = xft.newTransformer();
		File tmpFile = File.createTempFile("tmp.", ".xml",htmlFile.getParentFile());
		tr.transform(new DOMSource(dom), new StreamResult(tmpFile));;
		tmpFile.renameTo(htmlFile);
		tmpFile.delete();
		return 0;
		}
	
	
	@Override
	public int doWork(final List<String> args) {
		
		XMLStreamWriter w = null;
		XMLStreamWriter writeCache = null;
		FileOutputStream writeCacheFileWriter = null;
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
			
			this.client = builder.build();
			
			final File tmpFile = File.createTempFile("insta2atom.",".tmp",getCacheDir());
			tmpFile.deleteOnExit();
			writeCacheFileWriter = new FileOutputStream(tmpFile);

			final XMLOutputFactory xof = XMLOutputFactory.newInstance();
			
			
			writeCache = xof.createXMLStreamWriter(writeCacheFileWriter,"UTF-8");
			writeCache.writeStartDocument("UTF-8", "1.0");
			writeCache.writeStartElement("cache");
			writeCache.writeAttribute("date",this.dateFormatter.format(new Date()));
			writeCache.writeCharacters("\n");
			
			
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
				}
				
			w.writeEndElement();
			w.writeEndDocument();
			w.flush();
			w.close();
			
			writeCache.writeEndElement();//cache
			writeCache.writeEndDocument();
			writeCache.flush();
			writeCache.close();
			writeCacheFileWriter.close();
			this.client.close();this.client=null;
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
			IOUtils.close(this.client);
			}
		
		}
	
	public static void main(final String[] args) {
		new InstagramToAtom().instanceMainWithExit(args);
		}

	
}
