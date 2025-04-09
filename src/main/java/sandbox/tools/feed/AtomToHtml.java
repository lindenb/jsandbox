package sandbox.tools.feed;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.protocol.HttpContext;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.date.DateParser;
import sandbox.feed.RssToAtom;
import sandbox.html.TidyToDom;
import sandbox.http.CookieStoreUtils;
import sandbox.io.IOUtils;
import sandbox.jcommander.DurationConverter;
import sandbox.jcommander.NoSplitter;
import sandbox.lang.StringUtils;
import sandbox.lang.StringWrapper;
import sandbox.tools.central.ProgramDescriptor;
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

	private static class FeedSource extends StringWrapper {
		FeedSource(String url) {
			super(url);
			}
		}
	
	private class Feed {
		String url;
		String title;
		List<FeedItem> items = new ArrayList<>();
	}
	
	private class FeedItem{
		String url;
		String date;
		String title;
		Set<String> userNames=new HashSet<>();
		String imgUrl;
	}
	
	private DocumentBuilder documentBuilder;
	private final DateParser dateParser = new DateParser();
	private final Set<String> imgSrcSet = new HashSet<>();
	private final Set<String> excludeUsers = new HashSet<>();
	
	
	private String findImageInHtml(Node root) {
		if(root==null) return null;
		for(Node c1= root.getFirstChild();c1!=null;c1=c1.getNextSibling()) {
			if(c1.getNodeType()==Node.ELEMENT_NODE) {
				final Element e1 = Element.class.cast(c1);
				if(e1.getNodeName().equals("img") && e1.hasAttribute("src")) {
					return e1.getAttribute("src");
					}
				}
			String s = findImageInHtml(c1);
			if(s!=null) return s;
			}
		return null;
		}
	
	private boolean isDateOk(final String date) {
		if(this.since==null || StringUtils.isBlank(date)) return true;
		final Optional<Date> optDate = this.dateParser.apply(date);
		if(optDate.isPresent())  {
			final Date today = new Date();
			final long diff = today.getTime() - optDate.get().getTime();
			if( diff > since.toMillis() ) {
				return false;
				}
			}
		else
			{
			LOG.debug("cannot parse date: "+date);
			}
		return true;
		}
	
	private boolean isImageUrlOk(final String img) {
		if(StringUtils.isBlank(img)) return false;
		if(no_gif && (img.endsWith(".gif") || img.contains(".gif?"))) {
			return false;
		}
		return true;
		}	
	
	
	private FeedItem parseAtomEntry(Node root) {
		String title = null;
		String updated = null;
		String url = null;
		String img = null;
		String categoryLabel=null;
		final Set<String> userNames = new HashSet<>();
		String license = null;
		for(Node c1= root.getFirstChild();c1!=null;c1=c1.getNextSibling()) {
			if(c1.getNodeType()!=Node.ELEMENT_NODE) continue;
			final Element e1 = Element.class.cast(c1);
			final String localName1= e1.getLocalName();
			if(localName1.equals("link") && e1.hasAttribute("href") &&
				!e1.getAttribute("rel").equals("enclosure") && 
				!e1.getAttribute("rel").equals("license") && 
				!e1.getAttribute("type").startsWith("image/")) {
				url = e1.getAttribute("href");
				}
			else if(localName1.equals("link") && e1.hasAttribute("href") && 
					e1.getAttribute("rel").equals("enclosure") && 
					e1.getAttribute("type").startsWith("image/")) {
				img = e1.getAttribute("href");
				}
			else if(localName1.equals("link") &&
					e1.hasAttribute("href") && 
					e1.getAttribute("rel").equals("license") ) {
				license = e1.getAttribute("href");
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
						userNames.add(e2.getTextContent());
						}
					else if(localName2.equals("uri")) {
						userNames.add(e2.getTextContent());
						}
					else if(localName2.equals("nsid") && "flickr".equals(e2.getPrefix())) {
						userNames.add(e2.getTextContent());
						}
					}
				}
			}
		
		if(img==null) {
			for(Node c1= root.getFirstChild();c1!=null;c1=c1.getNextSibling()) {
				if(c1.getNodeType()!=Node.ELEMENT_NODE) continue;
				final Element e1 = Element.class.cast(c1);
				
				final String localName1= e1.getLocalName();
				if(!localName1.equals("content")) continue;
				if(!e1.getAttribute("type").equals("html")) continue;
				final Node frag=new sandbox.html.TidyToDom().importString(e1.getTextContent(),root.getOwnerDocument());
				img = findImageInHtml(frag);
				}
			}
				
		if(this.excludeUsers.stream().anyMatch(S->userNames.contains(S))) {
			return null;
			}
		
		if(!StringUtils.isBlank(categoryLabel) &&
			!StringUtils.isBlank(title) &&
			StringUtils.md5(categoryLabel).equals("cc2e71a2513d6d4ad6d7b903eea11526") &&
			!title.contains("[OC]")
			) {
			return null;
			}
		
		if(!isDateOk(updated)) {
			return null;
			}
		
		if(StringUtils.isBlank(url)) return null;
		if(!isImageUrlOk(img)) return null;
		if(!StringUtils.isBlank(title) && !StringUtils.isBlank(license)) {
			title = title+" "+license;
		}
		
		if(this.imgSrcSet.contains(img)) return null;
		this.imgSrcSet.add(img);
		final FeedItem retE = new FeedItem();
		retE.userNames.addAll(userNames);
		retE.url = url;
		retE.title = title;
		retE.imgUrl = img;
		retE.date = updated;
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
		
	
	private Optional<Document> parseUrl(CloseableHttpClient client,final FeedSource feedSrc) throws Exception {
		final String url = feedSrc.toString();
		if(IOUtils.isURL(url)) {
			try(CloseableHttpResponse resp=client.execute(new HttpGet(url))) {
				if(resp.getStatusLine().getStatusCode()!=200) {
					LOG.error("cannot fetch "+url+" "+resp.getStatusLine());
					return Optional.empty();
					}
				try(InputStream in = resp.getEntity().getContent()) {
					return Optional.of(this.documentBuilder.parse(in));
					}
				catch(final Throwable err) {
					LOG.error(err);
					return Optional.empty();
					}
				}
			}
		else
			{
			try {
				return Optional.of(this.documentBuilder.parse(new File(url)));
				}
			catch(final Throwable err) {
				LOG.error(err);
				return Optional.empty();
				}
			}
		}
	
	private void getOggForHtml(final CloseableHttpClient client,final Feed feed , final Node root) {
		if(root==null) {
			return;
			}
		if(root.getNodeType()==Node.DOCUMENT_NODE) {
			getOggForHtml(client,feed,Document.class.cast(root).getDocumentElement());
			return;
			}
		final Element E1 = Element.class.cast(root);
		if(E1.getNodeName().equalsIgnoreCase("html"))
			{
			for(Node c=E1.getFirstChild();c!=null;c=c.getNextSibling()) {
				if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
				getOggForHtml(client,feed,c);
				}
			return;
			}
		else if(E1.getNodeName().equalsIgnoreCase("head")) {
			final List<Map.Entry<String,String> > properties = new ArrayList<>();
			for(Node c=E1.getFirstChild();c!=null;c=c.getNextSibling()) {
				if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
				final Element E2 = Element.class.cast(c);
				if(!E2.getNodeName().equals("meta")) continue;
				if( E2.getAttribute("property").startsWith("og:")) {
					properties.add(new AbstractMap.SimpleEntry<String,String>(
						E2.getAttribute("property"),
						E2.getAttribute("content")
						));
					}
				}
			for(Map.Entry<String, String> kv1 : properties) {
				if(!kv1.getKey().equals("og:image")) continue;
				final FeedItem retE = new FeedItem();
				retE.imgUrl= kv1.getValue();
				if(!isImageUrlOk(retE.imgUrl)) continue;
				retE.title= properties.stream().
						filter(KV->KV.getKey().equals("og:description")).
						map(KV->KV.getValue()).findFirst().
						orElse("");
				feed.items.add(retE);
				}
			}
		else
			{
			}
		}
	
	private Optional<Feed> getOggForHtml(final CloseableHttpClient client,final Feed feed,final String itemSrc) {
		if(!IOUtils.isURL(itemSrc)) return Optional.empty();
		final TidyToDom toDom = new TidyToDom();
		
		try(CloseableHttpResponse resp=client.execute(new HttpGet(itemSrc))) {
			if(resp.getStatusLine().getStatusCode()!=200) {
				LOG.error("cannot fetch "+feed.url+" != "+itemSrc+" "+resp.getStatusLine());
				}
			try(InputStream in = resp.getEntity().getContent()) {
				final Document dom= toDom.read(in);
				getOggForHtml(client,feed,dom);
				if(feed.items.isEmpty()) return  Optional.empty();
				return  Optional.of(feed);
				}
			catch(final Throwable err) {
				LOG.error(err);
				return Optional.empty();
				}
			}
		catch(IOException err) {
			LOG.error(err);
			return Optional.empty();
			}
		}
		
	
	private  Optional<Feed> rssByHtmlPage(final CloseableHttpClient client,final Document rss,String url) {
		if(!IOUtils.isURL(url)) return Optional.empty();
		final Feed feed = new Feed();
		feed.url = url;
		feed.title = url;
		NodeList nl=rss.getElementsByTagName("title");
		for(int i=0;i< nl.getLength();i++) {
			Node item = nl.item(i);
			if(item.getNodeType()!=Node.ELEMENT_NODE) continue;
			if(item.getParentNode()==null ||item.getParentNode().getNodeType()!=Node.ELEMENT_NODE ) continue;
			if(!item.getParentNode().getNodeName().equals("channel")) continue;
			feed.title= item.getTextContent();
			break;
			}
		
		nl=rss.getElementsByTagName("item");
		for(int i=0;i< nl.getLength();i++) {
			Node item = nl.item(i);
			
			String pubDate = null;
			String link = null;
			for(Node c=item.getFirstChild();c!=null;c=c.getNextSibling()) {
				if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
				Element e1=Element.class.cast(c);
				if(e1.getNodeName().equals("pubDate")) {
					pubDate  = e1.getTextContent();
					}
				else if(e1.getNodeName().equals("link")) {
					link = e1.getTextContent();
					}
				}			
			if(IOUtils.isURL(link)) {
				if(!isDateOk(pubDate))  continue;
				getOggForHtml(client,feed,link);
				for(FeedItem fi:feed.items) {
					if(fi.date==null) fi.date=pubDate;
					if(fi.url==null) fi.url=link;
					}
				}
			}
		if(feed.items.isEmpty()) return Optional.empty();
		return Optional.of(feed);
		}
	
	private Node feedItemToHtml(final Document document,final FeedItem feed) {
		final Element a  = document.createElement("a");
		a.setAttribute("href",feed.url);
		a.setAttribute("target","_blank");
		final Element imgE = document.createElement("img");
		if(width>0) imgE.setAttribute("width", String.valueOf(width));
		a.appendChild(imgE);
		if(!StringUtils.isBlank(feed.title)) imgE.setAttribute("alt", feed.title);
		if(!StringUtils.isBlank(feed.title)) a.setAttribute("title", feed.title + (feed.userNames.isEmpty()?"":" \""+feed.userNames.iterator().next()+"\"")+(StringUtils.isBlank(feed.date)?"":" "+feed.date));
		imgE.setAttribute("src", feed.imgUrl);
		return a;
		}
	
	private DocumentFragment feedToHtml(final Document document,final Feed feed) {
		final DocumentFragment fragment = document.createDocumentFragment();
		if(feed==null || feed.items.isEmpty()) return fragment;
		final Element dt =document.createElement("dt");
		fragment.appendChild(dt);
		if(!StringUtils.isBlank(feed.title)) dt.appendChild(document.createTextNode(feed.title));
		final Element dd= document.createElement("dd");
		fragment.appendChild(dd);
		for(FeedItem entry:feed.items) {
			dd.appendChild(feedItemToHtml(document,entry));
			}
		return fragment;
		}
	
	private Optional<Feed> parseFeed(CloseableHttpClient client,final FeedSource feedSrc) throws Exception {
		final RssToAtom rss2atom = new RssToAtom();
		final Optional<Document> rss;
		try {
			rss = parseUrl(client, feedSrc);
			}
		catch(Throwable err) {
			LOG.error(err);
			return Optional.empty();
			}
		if(!rss.isPresent()) return  Optional.empty();
		
		if(feedSrc.getString().startsWith("https://bsky.app/")) {
			return rssByHtmlPage(client, rss.get(),feedSrc.getString());
			}
		else
			{
			final Document atom = rss2atom.apply(rss.get());
			if(atom==null) return  Optional.empty();
			
			final Element feed= atom.getDocumentElement();
			if(feed==null) return Optional.empty();
			final Feed feeds  = new Feed();
			feeds.url= feedSrc.getString();
			feeds.title = feeds.url;
			
			for(Node c1=feed==null?null:feed.getFirstChild();c1!=null;c1=c1.getNextSibling()) {
				if(c1.getNodeType()!=Node.ELEMENT_NODE) continue;
				final Element e1= Element.class.cast(c1);
				if(e1.getLocalName().equals("title")) {
					feeds.title = e1.getTextContent();
					}
				else if(e1.getLocalName().equals("entry")) {
					final FeedItem entryE = parseAtomEntry(e1);
					if(entryE!=null) {
						feeds.items.add(entryE);
						}
					}
				}			
			if(feeds.items.isEmpty()) return Optional.empty();
			return Optional.of(feeds);
			}
		}
	
	@Override
	public int doWork(final List<String> args) {
		try {
			final List<FeedSource> feedSources =  IOUtils.unroll(args).stream().
					map(S->S.trim()).
					filter(S->!StringUtils.isBlank(S)).
					map(S->new FeedSource(S)).
					collect(Collectors.toList());
			
			if(this.xUserPath!=null) {
				Files.lines(this.xUserPath).
					map(S->S.trim()).
					filter(S->!StringUtils.isBlank(S)).
					forEach(S->this.excludeUsers.add(S));
				}
		
			final HttpClientBuilder builder = HttpClientBuilder.create();
			builder.setUserAgent(IOUtils.getUserAgent());
			builder.setRetryHandler(new DefaultHttpRequestRetryHandler() {
				@Override
				public boolean retryRequest(IOException ex, int executionCount, HttpContext context) {
					LOG.warning("cannot retry fetch url."+ex.getMessage());
					return false;
					}
				});
			if(this.cookieStoreFile!=null) {
				final BasicCookieStore cookies = CookieStoreUtils.readTsv(this.cookieStoreFile);
				builder.setDefaultCookieStore(cookies);
				}
			
			
			final List<Feed> all_feeds=  new ArrayList<>();
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			this.documentBuilder = dbf.newDocumentBuilder();

			try(CloseableHttpClient client = builder.build()) {
				for(final FeedSource feedSrc :feedSources) {
					Optional<Feed> n = parseFeed(client, feedSrc);
					if(n.isPresent()) all_feeds.add(n.get());
				}
			
			final Document document = this.documentBuilder.newDocument();
			final 	Element html = document.createElement("html");
			document.appendChild(html);
			final Element body = document.createElement("body");
			html.appendChild(body);
			final Element dl = document.createElement("dl");
			body.appendChild(dl);
			final Set<String> seen=new HashSet<>();
			for(Feed feed: all_feeds) {
				feed.items.removeIf(F->seen.contains(F.imgUrl));
				if(feed.items.isEmpty()) continue;
				seen.addAll(feed.items.stream().map(F->F.imgUrl).collect(Collectors.toSet()));
				dl.appendChild(feedToHtml(document,feed));
				}
			
			for(Feed feed: all_feeds) {
				for(FeedItem item : feed.items) {
					saveImage(item.imgUrl, client);
					}
				}			
			final XMLSerializer write =new XMLSerializer();
			write.serialize(document,this.output);
			}
		} catch (final Throwable err) {
			LOG.error(err);
			return -1;
		}
		return 0;
		}
	
	
    public static ProgramDescriptor getProgramDescriptor() {
    	return new ProgramDescriptor() {
    		@Override
    		public String getName() {
    			return "atom2html";
    			}
    		};
    	}
    
	public static void main(final String[] args) {
		new AtomToHtml().instanceMainWithExit(args);

	}

}
