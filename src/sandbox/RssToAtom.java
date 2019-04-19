package sandbox;

import java.io.File;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import com.beust.jcommander.ParametersDelegate;

public class RssToAtom extends Launcher
	{
	private static final Logger LOG=Logger.builder(RssToAtom.class).build();
	
	static final String ATOM="http://www.w3.org/2005/Atom";
	@ParametersDelegate
	private Factory factory = new Factory();
	
	public static class Factory implements Supplier<Function<Document,Document>>
		{
		
		private class Converter implements Function<Document,Document>
			{
			private Document atomDoc = null;
			private String prefix="atom";
			private Predicate<Element> itemFilter = (E)->true;
			private Predicate<Element> atomFilter = E->true;
			
			private boolean hasName(final Element E,final String s) {
				return s.equals(E.getNodeName()) || s.equals(E.getLocalName());
				}
			
			private Element createElement(final String localName) {
			return this.atomDoc.createElementNS(ATOM, this.prefix +":"+localName);
			}
			private Text createText(final String txt) {
			return this.atomDoc.createTextNode(txt);
			}
			
			private Element matchItem(final Element fragment)
				{
				final Element entry = this.createElement("entry");
				// guid
				XmlUtils.stream(fragment).
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
				XmlUtils.stream(fragment).
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
				XmlUtils.stream(fragment).
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
				XmlUtils.stream(fragment).
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
				XmlUtils.stream(channel).
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
				XmlUtils.stream(channel).
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
				
				
				XmlUtils.stream(channel).
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
					if(ATOM.equals(rss.getNamespaceURI()) && "feed".equals(rss.getLocalName())) {
						return rssDoc;
						}
					final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
					dbf.setNamespaceAware(true);
					final DocumentBuilder db = dbf.newDocumentBuilder();
					this.atomDoc = db.newDocument();

					final Element feedE = this.createElement("feed");
					this.atomDoc.appendChild(feedE);

					XmlUtils.elements(rss).stream().
						filter(E->hasName(E,"channel")).
						limit(1L).
						forEach(C->{
							feedE.appendChild(matchChannel(C));	
						});

					
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
		
		private Predicate<Element> atomFilter = E->true;
		
		public void setAtomFilter(Predicate<Element> filter)
			{
			this.atomFilter = filter;
			}
		
		public Predicate<Element> getAtomFilter()
			{
			return atomFilter;
			}
		
		@Override
		public Function<Document, Document> get()
			{
			final Converter converter = new Converter();
			converter.atomFilter = this.getAtomFilter();
			return converter;
			}
		}
	@Override
	public int doWork(final List<String> args)
		{
		try {
			final String input = oneFileOrNull(args);
			LOG.info("reading "+ input);
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document rss;
			if(input==null) {
				rss = db.parse(System.in);
				}
			else if(IOUtils.isURL(input)) {
				rss = db.parse(input);
				}
			else
				{
				rss = db.parse(new File(input));
				}
		    new XMLSerializer().serialize(
		    		this.factory.get().apply(rss),
		    		System.out
		    		);
		    return 0;
			}
		catch(Exception err)
			{
			err.printStackTrace();
			LOG.error(err);
			return -1;
			}
		
		}
	public static void main(final String[] args)
		{
		new RssToAtom().instanceMainWithExit(args);
		}
	}
