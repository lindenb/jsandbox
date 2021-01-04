package sandbox.tools.feed;

import java.io.File;
import java.io.InputStream;
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
import sandbox.XMLSerializer;
import sandbox.date.DateParser;
import sandbox.feed.RssToAtom;
import sandbox.http.CookieStoreUtils;
import sandbox.io.IOUtils;
import sandbox.jcommander.DurationConverter;
import sandbox.jcommander.NoSplitter;

public class AtomToHtml extends Launcher {
	private static final Logger LOG=Logger.builder(AtomToHtml.class).build();

	@Parameter(names= {"-o","--out"},description=OUTPUT_OR_STANDOUT)
	private Path output = null;
	@Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private Path cookieStoreFile  = null;
	@Parameter(names={"--since"},description=DurationConverter.OPT_DESC,converter=DurationConverter.class,splitter=NoSplitter.class)
	private Duration since=null;

	
	private DocumentBuilder documentBuilder;
	private Document document = null;
	private final DateParser dateParser = new DateParser();
	private final Set<String> imgSrcSet = new HashSet<>();
	
	private Node parseEntry(Node root) {
		String title = null;
		String updated = null;
		String url = null;
		String img = null;
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
				}
			else if(localName1.equals("thumbnail") && e1.hasAttribute("url")) {
				img = e1.getAttribute("url");
				}
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
		if(this.imgSrcSet.contains(img)) return null;
		this.imgSrcSet.add(img);
		Element retE = this.document.createElement("span");
		Element a  = this.document.createElement("a");
		retE.appendChild(a);
		a.setAttribute("href",url);
		a.setAttribute("target","_blank");
		Element imgE = this.document.createElement("img");
		a.appendChild(imgE);
		if(!StringUtils.isBlank(title)) imgE.setAttribute("alt", title);
		if(!StringUtils.isBlank(title)) a.setAttribute("title", title + (StringUtils.isBlank(updated)?"":" "+updated));
		imgE.setAttribute("src", img);
		return retE;
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
				Node entryE = parseEntry(e1);
				if(entryE!=null) {
					entries.add(entryE);
					}
				}
			}
		if(entries.isEmpty()) return null;
		Node retE = this.document.createDocumentFragment();
		Element dt = this.document.createElement("dt");
		retE.appendChild(dt);
		if(!StringUtils.isBlank(title)) retE.appendChild(document.createTextNode(title));
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
			final HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setUserAgent(IOUtils.getUserAgent());
			if(this.cookieStoreFile!=null) {
				final BasicCookieStore cookies = CookieStoreUtils.readTsv(this.cookieStoreFile);
				builder.setDefaultCookieStore(cookies);
				}
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newDefaultInstance();
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
