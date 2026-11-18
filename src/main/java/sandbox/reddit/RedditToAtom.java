package sandbox.reddit;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import sandbox.Logger;
import sandbox.feed.Atom;
import sandbox.html.TidyToDom;
import sandbox.lang.StringUtils;



public class RedditToAtom {
	private static final Logger LOG=Logger.builder(RedditToAtom.class).build();

	private Document document = null;
	private Element atomRoot = null;
	private Element titleElement=null;
	private XPath xpath=null;
	private boolean debug_flag=true;
	
	
	private void reset() {
		this.document=null;
		this.atomRoot = null;
		this.titleElement=null;
		this.xpath=null;
		}
	private Element element(final String s) {
		return this.document.createElementNS(Atom.NS, s);
	}
	private Text text(final String s) {
		return this.document.createTextNode(s);
	}
	private String sanitize(String url) {
		if(url.endsWith(".rss")) {
			url = url.substring(0,url.length()-4);
			}
		return url;
		}
	private void debugNode(String msg,Node n) {
		if(!debug_flag) return;
		try {
			System.err.println();
			System.err.println(msg);
			Transformer tr=TransformerFactory.newInstance().newTransformer();
			tr.setOutputProperty(OutputKeys.INDENT,"yes");
			tr.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"yes");
			tr.transform(new DOMSource(n), new StreamResult(System.err));
			System.err.println();
			}
		catch(Throwable err) {
			err.printStackTrace();
			}
		}
	
	private boolean isEntry(Element root) {
		if(!root.getNodeName().equalsIgnoreCase("div")) return false;
		if(!root.hasAttribute("class")) return false;
		final String clazz=root.getAttribute("class");
		if(Arrays.stream(clazz.split("\\s+")).noneMatch(S->S.equals("entry"))) return false;
		return true;
		}
	
	public void setDebug(boolean debug_flag) {
		this.debug_flag = debug_flag;
		}
	
	private Element toEntry(Element root) throws XPathExpressionException {
		debugNode("scanning entry",root);
		
		
		Element a=(Element)this.xpath.evaluate( ".//p[@class='title']//a",root,XPathConstants.NODE);
		if(a==null || !a.hasAttribute("href")) return null;
		
		debugNode("ok, got anchor",a);
		
		Element entry = element("entry");
		
			{
		
			
			
			String s = a.getTextContent();
			Element title=element("title");
			title.appendChild(text(s));
			entry.appendChild(title);
			
			final String href= a.getAttribute("href");
			if(StringUtils.isBlank(href)) return null;
			
			Element link=element("link");
			link.setAttribute("href",href);
			entry.appendChild(link);
			
			if(href.toLowerCase().matches(".*\\.(jpg|png|jpeg)") ) {
				Element content = element("content");
				content.setAttribute("type","html");
				Element img = this.document.createElement("img");
				img.setAttribute("src", href);
				content.appendChild(img);
				entry.appendChild(content);
				
				Element thumbnail = element("thumbnail");
				thumbnail.setAttribute("url", href);
				entry.appendChild(thumbnail);
				
				}
			}
		Attr att =root.getAttributeNode("data-timestamp");
		if(att!=null) {
			try
			{
				Instant t=Instant.ofEpochMilli(Long.parseLong(att.getValue()));
				
				String s= t.toString();;
				Element e = element("published");
				entry.appendChild(e);
				e.appendChild(text(s));
				e = element("updated");
				entry.appendChild(e);
				e.appendChild(text(s));
				}
			catch(Throwable err) {
				LOG.error(err);
				}
			}
		 att =root.getAttributeNode("id");
		if(att!=null) {
			Element e = element("id");
			entry.appendChild(e);
			e.appendChild(text(att.getValue()));
			}
		att=root.getAttributeNode("data-author");
		if(att!=null) {
			Element e = element("author");
			entry.appendChild(e);
			Element e2 = element("name");
			e.appendChild(e2);
			e2.appendChild(text(att.getValue()));
			}
		
		debugNode("entry is", entry);	
		
		
		return entry;
		}
	private void scan(Node root)  throws Exception {
		if(this.titleElement==null) {
			Element t=(Element)this.xpath.evaluate(".//head/title", root, XPathConstants.NODE);
			if(t!=null) {
				debugNode("GO TITLE", t);
				this.titleElement= element("title");
				this.atomRoot.appendChild(this.titleElement);
				this.titleElement.appendChild(text(t.getTextContent()));
			}
		}
		
		
		for(Node n=root.getFirstChild();n!=null;n=n.getNextSibling()) {
			if(n.getNodeType()!=Node.ELEMENT_NODE) continue;
			Element e =Element.class.cast(n);
			if(isEntry(e)) {
				Element E = toEntry(Element.class.cast(e.getParentNode()));
				if(E!=null) this.atomRoot.appendChild(E);
				
				}
			else
				{
				scan(e);
				}
			}
		}
	
	public Optional<Document> fetch(final CloseableHttpClient client, final String url) throws IOException {
		reset();
		final String url1 = sanitize(url);
		try(CloseableHttpResponse resp=client.execute(new HttpGet(url1))) {
		if(resp.getStatusLine().getStatusCode()!=200) {
			LOG.error("cannot fetch "+url1+" "+resp.getStatusLine());
			return Optional.empty();
			}
		
		//DocumentBuilder documentBuilder = dbf.newDocumentBuilder();
		final String html;
		try(ByteArrayOutputStream baos =new ByteArrayOutputStream() ) {
			resp.getEntity().writeTo(baos);
			html=new String(baos.toByteArray());
			System.err.println(url1);
			System.err.println(html);
			System.err.println();
			
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			this.xpath = XPathFactory.newInstance().newXPath();
			this.document = dbf.newDocumentBuilder().newDocument();
			this.atomRoot = element("feed");
			this.document.appendChild(this.atomRoot);
			Element id= element("id");
			this.atomRoot.appendChild(id);
			id.appendChild(text(StringUtils.md5(url1)));
			final TidyToDom htmlTidy = new TidyToDom();
			final DocumentFragment frag=htmlTidy.importString(html);
			scan(frag);
			
			if(this.titleElement==null) {
				this.titleElement= element("title");
				this.atomRoot.appendChild(this.titleElement);
				this.titleElement.appendChild(text(url1));
				}
			
			debugNode("At the end ",this.document);
			final Optional<Document> ret  = Optional.of(this.document);
			reset();
			return ret;
			}
		catch(final Throwable err) {
			LOG.error(err);
			return Optional.empty();
			}
		}
	
//		final TidyToDom htmlTidy = new TidyToDom();//
	//htmlTidy.importString(html);
	//return Optional.empty();
		}
	
public static boolean isRedditURL(final String url) {
	return url.startsWith("https://www.reddit.com/");
	}
}
