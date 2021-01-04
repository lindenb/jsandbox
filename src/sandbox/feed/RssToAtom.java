package sandbox.feed;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import sandbox.io.IOUtils;


public class RssToAtom  implements Function<Document,Document> {

	private Document atomDoc = null;
	private String prefix="atom";
	private Predicate<Element> itemFilter = (E)->true;
	private Predicate<Element> atomFilter = E->true;
	
		
	public void setAtomFilter(Predicate<Element> filter)
		{
		this.atomFilter = filter;
		}
	
	public Predicate<Element> getAtomFilter()
		{
		return atomFilter;
		}
	
	
	private Stream<Node> stream(Node root) {
		final List<Node> L = new ArrayList<>();
		for(Node n=root.getFirstChild();n!=null;n=n.getNextSibling()) {
			L.add(n);
			}
		return L.stream();
		}

	
	private boolean hasName(final Element E,final String s) {
		return s.equals(E.getNodeName()) || s.equals(E.getLocalName());
		}
	
	private Element createElement(final String localName) {
		return this.atomDoc.createElementNS(Atom.NS, this.prefix +":"+localName);
		}
	private Text createText(final String txt) {
		return this.atomDoc.createTextNode(txt);
		}
	
	
	
	private Element matchItem(final Element fragment)
		{
		final Element entry = this.createElement("entry");
		// guid
		this.stream(fragment).
			filter(N->N.getNodeType()==Node.ELEMENT_NODE).
			map(N->Element.class.cast(N)).
			filter(E->hasName(E,"guid")).
			map(E->E.getTextContent()).
			map(S->{
				final Element id = createElement("id");
				id.appendChild(createText(S));
				return id;
				}).
			limit(1L).
			forEach(E->{entry.appendChild(E);});

		// pubDate
		this.stream(fragment).
			filter(N->N.getNodeType()==Node.ELEMENT_NODE).
			map(N->Element.class.cast(N)).
			filter(E->hasName(E,"pubDate")).
			map(E->E.getTextContent()).
			map(S->{
				final Element id = createElement("updated");
				id.appendChild(createText(S));
				return id;
				}).
			limit(1L).
			forEach(E->{entry.appendChild(E);});

		//enclosure
		this.stream(fragment).
			filter(N->N.getNodeType()==Node.ELEMENT_NODE).
			map(N->Element.class.cast(N)).
			filter(E->hasName(E,"enclosure") && E.hasAttribute("url") && E.getAttribute("type").startsWith("image/")).
			map(E->E.getAttribute("url")).
			filter(U->IOUtils.isURL(U)).
			findFirst().ifPresent(U-> {
				final Element E = createElement("content");
				E.setAttribute("type", "html");
				E.appendChild(createText("<span><img src=\"" + U+ "\"/></span>"));
				entry.appendChild(E);
				});
		
		// description
		this.stream(fragment).
			filter(N->N.getNodeType()==Node.ELEMENT_NODE).
			map(N->Element.class.cast(N)).
			filter(E->hasName(E,"description")).
			map(E->E.getTextContent()).
			map(S->{
				final Element id = createElement("subtitle");
				id.appendChild(createText(S));
				return id;
				}).
			limit(1L).
			forEach(E->{entry.appendChild(E);});
			
	
		
		return entry;

		}
	private DocumentFragment matchChannel(final Element channel)
		{
		final DocumentFragment frag = this.atomDoc.createDocumentFragment();
		
		// title
		this.stream(channel).
			filter(N->N.getNodeType()==Node.ELEMENT_NODE).
			map(N->Element.class.cast(N)).
			filter(E->hasName(E,"title")).
			map(E->E.getTextContent()).
			map(S->{
				final Element id = createElement("title");
				id.appendChild(createText(S));
				return id;
				}).
			limit(1L).
			forEach(E->{frag.appendChild(E);});

		// link
		this.stream(channel).
			filter(N->N.getNodeType()==Node.ELEMENT_NODE).
			map(N->Element.class.cast(N)).
			filter(E->hasName(E,"link")).
			map(E->E.getTextContent()).
			map(S->{
				final Element id = createElement("id");
				id.appendChild(createText(S));
				return id;
				}).
			limit(1L).
			forEach(E->{frag.appendChild(E);});

		final Element updated = createElement("updated");
		updated.appendChild(createText(""));
		frag.appendChild(updated);
		
		
		this.stream(channel).
			filter(N->N.getNodeType()==Node.ELEMENT_NODE).
			map(N->Element.class.cast(N)).
			filter(E->hasName(E,"item")).
			filter(E->itemFilter.test(E)).
			map(E->matchItem(E)).
			filter(N->N!=null).
			filter(E->this.atomFilter.test(E)).
			forEach(E->{frag.appendChild(E);})
			;
		
		
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

			final Element channel = this.stream(feedE).
				filter(N->N.getNodeType()==Node.ELEMENT_NODE).
				map(N->Element.class.cast(N)).
				filter(E->hasName(E,"channel")).
				findFirst().orElse(null);

			if(channel!=null) {
				feedE.appendChild(matchChannel(channel));
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
