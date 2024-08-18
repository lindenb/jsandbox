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
	public Text createText(Characters chars) {
		return new Text(chars.getData());
		}
	
	
	public MiniDomReader setElementCreator(Function<StartElement, Element> elementCreator) {
		this.elementCreator = elementCreator;
		return this;
		}
	
	public Element createElement(StartElement se) {
		return this.elementCreator.apply(se);
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
	
}
