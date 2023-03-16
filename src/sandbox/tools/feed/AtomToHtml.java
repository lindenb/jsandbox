package sandbox.tools.feed;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.date.DateParser;
import sandbox.feed.RssToAtom;
import sandbox.http.CookieStoreUtils;
import sandbox.io.IOUtils;
import sandbox.jcommander.DurationConverter;
import sandbox.jcommander.NoSplitter;
import sandbox.xml.XMLSerializer;

public class AtomToHtml extends Launcher {
	private static final Logger LOG=Logger.builder(AtomToHtml.class).build();

	@Parameter(names= {"-o","--out"},description=OUTPUT_OR_STANDOUT)
	private Path output = null;
	@Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private Path cookieStoreFile  = null;
	@Parameter(names={"--since"},description=DurationConverter.OPT_DESC,converter=DurationConverter.class,splitter=NoSplitter.class)
	private Duration since=null;
	@Parameter(names={"--width"},description="Image width. Ignore if lower than 1.")
	private int width = -1;
	@Parameter(names={"--exclude-users"},description="Exclude users")
	private Path xUserPath = null;
	@Parameter(names={"--directory"},description="Save images in that directory")
	private Path saveDirectory = null;
	@Parameter(names={"--no-gif"},description="remove images with .gif suffix")
	private boolean no_gif = false;

	
	private DocumentBuilder documentBuilder;
	private Document document = null;
	private final DateParser dateParser = new DateParser();
	private final Set<String> imgSrcSet = new HashSet<>();
	private final Set<String> excludeUsers = new HashSet<>();
	
	private Node parseEntry(Node root,final CloseableHttpClient client) {
		String title = null;
		String updated = null;
		String url = null;
		String img = null;
		String categoryLabel=null;
		String userName =null;
		for(Node c1= root.getFirstChild();c1!=null;c1=c1.getNextSibling()) {
			if(c1.getNodeType()!=Node.ELEMENT_NODE) continue;
			final Element e1 = Element.class.cast(c1);
			final String localName1= e1.getLocalName();
			if(localName1.equals("link") && e1.hasAttribute("href")) {
				url = e1.getAttribute("href");
				}
			else if(localName1.equals("updated")) {
				updated = e1.getTextContent();
				}
			else if(localName1.equals("title")) {
				title = e1.getTextContent();
				if(title.toLowerCase().contains("[m]") || title.toLowerCase().contains("(m)")) return null;
				}
			else if(localName1.equals("thumbnail") && e1.hasAttribute("url")) {
				img = e1.getAttribute("url");
				}
			else if(localName1.equals("category") && e1.hasAttribute("label")) {
				categoryLabel = e1.getAttribute("label");
				}
			else if(localName1.equals("author")) {
				for(Node c2= e1.getFirstChild();c2!=null;c2=c2.getNextSibling()) {
					if(c2.getNodeType()!=Node.ELEMENT_NODE) continue;
					final Element e2 = Element.class.cast(c2);
					final String localName2 = e2.getLocalName();
					if(localName2.equals("name")) {
						userName = e2.getTextContent();
						}
					}
				}
			}
		if(userName!=null && this.excludeUsers.contains(userName)) {
			return null;
			}
		
		if(!StringUtils.isBlank(categoryLabel) &&
			!StringUtils.isBlank(title) &&
			StringUtils.md5(categoryLabel).equals("cc2e71a2513d6d4ad6d7b903eea11526") &&
			!title.contains("[OC]")
			) {
			return null;
			}
		
		if(!StringUtils.isBlank(updated) && this.since!=null) {
			final Optional<Date> optDate = this.dateParser.apply(updated);
			if(optDate.isPresent())  {
				final Date today = new Date();
				final long diff = today.getTime() - optDate.get().getTime();
				if( diff > since.toMillis() ) {
					return null;
					}
				}
			}
		
		if(StringUtils.isBlank(url)) return null;
		if(StringUtils.isBlank(img)) return null;
		if(no_gif && (img.endsWith(".gif") || img.contains(".gif?"))) {
			return null;
		}
		
		
		if(this.imgSrcSet.contains(img)) return null;
		this.imgSrcSet.add(img);
		final Element retE = this.document.createElement("span");
		final Element a  = this.document.createElement("a");
		retE.appendChild(a);
		a.setAttribute("href",url);
		a.setAttribute("target","_blank");
		final Element imgE = this.document.createElement("img");
		if(width>0) imgE.setAttribute("width", String.valueOf(width));
		a.appendChild(imgE);
		if(!StringUtils.isBlank(title)) imgE.setAttribute("alt", title);
		if(!StringUtils.isBlank(title)) a.setAttribute("title", title + (StringUtils.isBlank(userName)?"":" \""+userName+"\"")+(StringUtils.isBlank(updated)?"":" "+updated));
		imgE.setAttribute("src", img);
		saveImage(img,client);
		return retE;
		}
	
	private void saveImage(final String img,CloseableHttpClient client) {
		if(saveDirectory==null) return;
		if(!Files.exists(saveDirectory)) return;
		final String md5 = StringUtils.md5(img);
		final Path saveFile = saveDirectory.resolve(md5);
		if(Files.exists(saveFile)) return;
		
		try {
			try(CloseableHttpResponse resp=client.execute(new HttpGet(img))) {
				if(resp.getStatusLine().getStatusCode()!=200) {
					LOG.error("cannot fetch "+img+" "+resp.getStatusLine());
					}
				else {
					try(InputStream in = resp.getEntity().getContent()) {
						IOUtils.copyTo(in, saveFile);
						}
					catch(final Throwable err) {
						LOG.error(err);
						}
					}
				}
			} catch(IOException err) {
				LOG.error(err);
			}
			
	}
		
	
	private Document parseUrl(CloseableHttpClient client,final String url) throws Exception {
		if(IOUtils.isURL(url)) {
			try(CloseableHttpResponse resp=client.execute(new HttpGet(url))) {
				if(resp.getStatusLine().getStatusCode()!=200) {
					LOG.error("cannot fetch "+url+" "+resp.getStatusLine());
					return null;
					}
				try(InputStream in = resp.getEntity().getContent()) {
					return this.documentBuilder.parse(in);
					}
				catch(final Throwable err) {
					LOG.error(err);
					return null;
					}
				}
			}
		else
			{
			try {
				return this.documentBuilder.parse(new File(url));
				}
			catch(final Throwable err) {
				LOG.error(err);
				return null;
				}
			}
		}
	
	private Node parseFeed(CloseableHttpClient client,final String url) throws Exception {
		final RssToAtom rss2atom = new RssToAtom();
		final Document rss = parseUrl(client, url);
		if(rss==null) return null;
		final Document atom = rss2atom.apply(rss);
		if(atom==null) return null;
		
		Element feed= atom.getDocumentElement();
		if(feed==null) return null;
		String title = null;
		List<Node> entries= new ArrayList<>();
		for(Node c1=feed==null?null:feed.getFirstChild();c1!=null;c1=c1.getNextSibling()) {
			if(c1.getNodeType()!=Node.ELEMENT_NODE) continue;
			final Element e1= Element.class.cast(c1);
			if(e1.getLocalName().equals("title")) {
				title = e1.getTextContent();
				}
			else if(e1.getLocalName().equals("entry")) {
				Node entryE = parseEntry(e1,client);
				if(entryE!=null) {
					entries.add(entryE);
					}
				}
			}
		if(entries.isEmpty()) return null;
		Node retE = this.document.createDocumentFragment();
		Element dt = this.document.createElement("dt");
		retE.appendChild(dt);
		if(!StringUtils.isBlank(title)) dt.appendChild(document.createTextNode(title));
		Element dd= this.document.createElement("dd");
		retE.appendChild(dd);
		for(Node entry:entries) {
			dd.appendChild(entry);
			}
		return retE;
		
			
		}
	
	@Override
	public int doWork(final List<String> args) {
		try {
			if(this.xUserPath!=null) {
				Files.lines(this.xUserPath).
					map(S->S.trim()).
					filter(S->!StringUtils.isBlank(S)).
					forEach(S->this.excludeUsers.add(S));
				}
		
			final HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setUserAgent(IOUtils.getUserAgent());
			if(this.cookieStoreFile!=null) {
				final BasicCookieStore cookies = CookieStoreUtils.readTsv(this.cookieStoreFile);
				builder.setDefaultCookieStore(cookies);
				}
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			this.documentBuilder = dbf.newDocumentBuilder();
			this.document = this.documentBuilder.newDocument();
			Element html = this.document.createElement("html");
			this.document.appendChild(html);
			Element body = this.document.createElement("body");
			html.appendChild(body);
			Element dl = this.document.createElement("dl");
			body.appendChild(dl);
			try(CloseableHttpClient client = builder.build()) {
				for(final String url:args) {
					Node n = parseFeed(client, url);
					if(n!=null) dl.appendChild(n);
				}
				
			XMLSerializer write =new XMLSerializer();
			write.serialize(this.document,this.output);
			}
		} catch (final Throwable err) {
			LOG.error(err);
			return -1;
		}
		return 0;
		}
	
	public static void main(final String[] args) {
		new AtomToHtml().instanceMainWithExit(args);

	}

}
