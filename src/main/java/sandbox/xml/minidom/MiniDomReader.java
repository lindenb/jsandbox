package sandbox.xml.minidom;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Path;
import java.util.function.Function;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import sandbox.io.IOUtils;
import sandbox.io.RuntimeIOException;

public class MiniDomReader {
	private Function<StartElement,Element> elementCreator= (SE)->new Element(SE);
	public final Text createText(final Characters chars) {
		return createText(chars.getData());
		}
	public Text createText(final String chars) {
		return new Text(chars);
		}
	
	public MiniDomReader setElementCreator(Function<StartElement, Element> elementCreator) {
		this.elementCreator = elementCreator;
		return this;
		}
	
	public Element createElement(StartElement se) {
		return this.elementCreator.apply(se);
		}
	
	public Element createElement(org.w3c.dom.Element E) {
		return new Element(E);
		}
	
	protected XMLInputFactory newXMLInputFactory() {
		return XMLInputFactory.newFactory();
		}
	
	public Element parseFile(File filename) throws IOException {
		return parsePath(filename.toPath());
		}
	
	public Element parsePath(Path filename) throws IOException {
		try(Reader is=IOUtils.openPathAsReader(filename)) {
			return parseReader(is);
			}
		}
	
	public Element parseInputStream(InputStream is)  throws IOException {
		try {
			final XMLEventReader r=newXMLInputFactory().createXMLEventReader(is);
			final Element root= parse(r);
			r.close();
			return root;
			}
		catch(XMLStreamException err) {
			throw new IOException(err);
			}
		}
	
	public Element parseReader(Reader reader) throws IOException {
		try {
			XMLEventReader r=newXMLInputFactory().createXMLEventReader(reader);
			Element root= parse(r);
			r.close();
			return root;
			}
		catch(XMLStreamException err) {
			throw new IOException(err);
			}		
		}

	public Element parse(XMLEventReader r) {
		Element root=null;
		try {
			while(r.hasNext()) {
				final XMLEvent evt=r.nextEvent();
				if(evt.isStartElement()) {
					if(root!=null) throw new IllegalStateException("duplicate element root");
					root = createElement(evt.asStartElement());
					parse(root,r);
					}
				else if(evt.isEndDocument()) {
					break;
					}
				}
			return root;
			}
		catch(XMLStreamException err) {
			throw new RuntimeIOException(err);
			}
		}
	
	private void parse(final Element root,XMLEventReader r) throws XMLStreamException {
		while(r.hasNext()) {
			final XMLEvent evt=r.nextEvent();
			if(evt.isStartElement()) {
				final Element child = createElement(evt.asStartElement());
				if(child!=null && accept(root,child)) root.appendChild(child);
				parse(child,r);
				}
			else if(evt.isEndElement()) {
				return;
				}
			else if(evt.isCharacters()) {
				final Text child = createText(evt.asCharacters());
				if(child!=null && accept(root,child)) root.appendChild(child);
				}
			}
		}
	protected boolean accept(Element root,Node n) {
		return true;
		}
	
	public Node importDOM(org.w3c.dom.Node n) {
		if(n.getNodeType()==org.w3c.dom.Node.DOCUMENT_NODE) {
			return importDOM(org.w3c.dom.Document.class.cast(n).getDocumentElement());
			}
		else if(n.getNodeType()==org.w3c.dom.Node.TEXT_NODE ||n.getNodeType()==org.w3c.dom.Node.CDATA_SECTION_NODE ) {
			return createText(org.w3c.dom.CharacterData.class.cast(n).getData());
			}
		else if(n.getNodeType()==org.w3c.dom.Node.ELEMENT_NODE ) {
			Element E= createElement(org.w3c.dom.Element.class.cast(n));
			for(org.w3c.dom.Node c=n.getFirstChild();c!=null;c=c.getNextSibling() ) {
				Node c2 = importDOM(c);
				if(c2!=null &&  accept(E,c2)) E.appendChild(c2);
				}
 			return E;
			}
		return null;
		}
	
	
}
