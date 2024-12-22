package sandbox.feed;

import java.util.Arrays;
import java.util.function.Function;
import java.util.function.Predicate;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import sandbox.Logger;
import sandbox.html.TidyToDom;
import sandbox.io.IOUtils;
import sandbox.lang.StringUtils;


public class RssToAtom  implements Function<Document,Document> {
	private static final Logger LOG =Logger.builder(RssToAtom.class).build();

	private Document atomDoc = null;
	private Predicate<Element> itemFilter = (E)->true;
	private Predicate<Element> atomFilter = E->true;
	private final TidyToDom tidy2dom = new TidyToDom();
		
	public void setAtomFilter(Predicate<Element> filter)
		{
		this.atomFilter = filter;
		}
	
	public Predicate<Element> getAtomFilter()
		{
		return atomFilter;
		}
	
	
	private boolean hasName(final Element E,final String s) {
		return s.equals(E.getNodeName()) || s.equals(E.getLocalName());
		}
	
	private Element createElement(final String localName) {
		return this.atomDoc.createElementNS(Atom.NS, localName);
		}
	private Text createText(final String txt) {
		return this.atomDoc.createTextNode(txt);
		}
	
	private String findImage(Node root) {
		for(Node c1=root.getFirstChild();c1!=null;c1=c1.getNextSibling()) {
			if(c1.getNodeType()!=Node.ELEMENT_NODE) continue;
			final Element e1 = Element.class.cast(c1);
			if(e1.getTagName().equals("img") ) {
				if(e1.hasAttribute("srcset")) {
					String u = Arrays.stream(e1.getAttribute("srcset").split("[ ,]")).
							filter(S->IOUtils.isURL(S)).
							findFirst().
							orElse(null);
					if(u!=null) return u;
					}
				if(e1.hasAttribute("src")) return e1.getAttribute("src");
				}
			String u = findImage(c1);
			if(u!=null) return u;
			}
		return null;
	}
	
	private Element matchItem(final Element root)
		{
		final Element entry = this.createElement("entry");
		String img = null;
		for(Node c1=root.getFirstChild();c1!=null;c1=c1.getNextSibling()) {
			if(c1.getNodeType()!=Node.ELEMENT_NODE) continue;
			final Element E1=Element.class.cast(c1);
			if(hasName(E1,"description")) {
				final String s = E1.getTextContent();
				if(StringUtils.isBlank(s)) {
					continue;
				}
				final DocumentFragment f = this.tidy2dom.importString("<html><body>"+s+"</body></html>", this.atomDoc);
				if(f==null || !f.hasChildNodes()) {
					continue;
					}
				if(img==null) img = findImage(f);
				final Element e = createElement("content");
				entry.appendChild(e);
				e.setAttribute("type", "html");
				e.appendChild(f);
				}
			else if(hasName(E1,"guid")) {
				final String s = E1.getTextContent();
				if(StringUtils.isBlank(s)) continue;
				final Element e = createElement("id");
				e.appendChild(createText(s));
				entry.appendChild(e);
				
				final Element e2 = createElement("link");
				e2.setAttribute("href", s);
				entry.appendChild(e2);
				}
			else if(hasName(E1,"pubDate")) {
				final String s = E1.getTextContent();
				if(StringUtils.isBlank(s)) continue;
				final Element e = createElement("updated");
				e.appendChild(createText(s));
				entry.appendChild(e);
				}
			else if(hasName(E1,"enclosure")&& E1.hasAttribute("url") && E1.getAttribute("type").startsWith("image/")) {
				String u = E1.getAttribute("url");
				if(!IOUtils.isURL(u)) continue;
				final Element E = createElement("content");
				E.setAttribute("type", "html");
				E.appendChild(createText("<span><img src=\"" + u+ "\"/></span>"));
				entry.appendChild(E);
				}
			}
		
		if(img!=null) {
			final Element E2 = createElement("thumbnail");
			E2.setAttribute("url", img);
			entry.appendChild(E2);
			}
		return entry;

		}
	private DocumentFragment matchChannel(final Element channel)
		{
		final DocumentFragment frag = this.atomDoc.createDocumentFragment();
		
		
		for(Node c1=channel.getFirstChild();c1!=null;c1=c1.getNextSibling()) {
			if(c1.getNodeType()!=Node.ELEMENT_NODE) continue;
			final Element E1=Element.class.cast(c1);
			if(hasName(E1,"title")) {
				final String s = E1.getTextContent();
				if(StringUtils.isBlank(s)) continue;
				final Element e = createElement("title");
				e.appendChild(createText(s));
				frag.appendChild(e);
				}
			else if(hasName(E1,"link")) {
				final String s = E1.getTextContent();
				if(StringUtils.isBlank(s)) continue;
				final Element e = createElement("id");
				e.appendChild(createText(s));
				frag.appendChild(e);
				}
			}
		
		
		final Element updated = createElement("updated");
		updated.appendChild(createText(""));
		frag.appendChild(updated);
		
		for(Node c1=channel.getFirstChild();c1!=null;c1=c1.getNextSibling()) {
			if(c1.getNodeType()!=Node.ELEMENT_NODE) continue;
			final Element E1=Element.class.cast(c1);
			if(hasName(E1,"item")) {
				if(!itemFilter.test(E1)) continue;
				final Element e = matchItem(E1);
				if(e==null) continue;
				frag.appendChild(e);
				}
			}
		
		return frag;
		}
	
	@Override
	public Document apply(final Document rssDoc)
		{
		try
			{
			final Element rss = rssDoc.getDocumentElement();
			if(Atom.NS.equals(rss.getNamespaceURI()) && "feed".equals(rss.getLocalName())) {
				return rssDoc;
				}
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			this.atomDoc = db.newDocument();

			final Element feedE = this.createElement("feed");
			this.atomDoc.appendChild(feedE);

			final Element rssRoot = rssDoc.getDocumentElement();
			if(rssRoot==null) {
				return this.atomDoc;
				}
			
			for(Node c1=rssRoot.getFirstChild();c1!=null;c1=c1.getNextSibling()) {
				if(c1.getNodeType()!=Node.ELEMENT_NODE) continue;
				final Element E1=Element.class.cast(c1);
				if(!hasName(E1,"channel")) continue;
				feedE.appendChild(matchChannel(E1));
				break;
				}
			
			final Document dom = this.atomDoc;
			this.atomDoc = null;
			return dom;
			}
		catch (final Exception err)
			{
			throw new RuntimeException(err);
			}
		finally
			{
			this.atomDoc = null;
			}
		}
	
}
