package sandbox.xml.stream;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import sandbox.StringUtils;
import sandbox.io.RuntimeIOException;

public class XmlStreamWriter {
	private final XMLStreamWriter delegate;
	private Runnable runAtEnd = null;
	public XmlStreamWriter(XMLStreamWriter delegate) {
		this(delegate,null);
		}
	private XmlStreamWriter(XMLStreamWriter delegate, Runnable runAtEnd) {
		this.delegate = delegate;
		this.runAtEnd=runAtEnd;
		}
	
	public XMLStreamWriter getDelegate() {
		return delegate;
		}

	private static RuntimeIOException wrap(XMLStreamException err)		
		{
		throw new RuntimeIOException(err);
		}
	
	private static QName toQName(Node n) {
		String ns = n.getNamespaceURI();
		if(StringUtils.isBlank(ns) || XMLConstants.NULL_NS_URI.equals(ns)) {
			return new QName(n.getNodeName());
			}
		if(StringUtils.isBlank(n.getPrefix())) {
			return new QName(n.getNamespaceURI(),n.getLocalName());
			}
		return new QName(n.getNamespaceURI(),n.getLocalName(),n.getPrefix());
		}
	

	
	public XmlStreamWriter writeNamespace(String prefix,String ns) {
		try {
			getDelegate().writeNamespace(prefix, ns);;
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}
	
	public XmlStreamWriter writeDefaultNamespace(String ns) {
		try {
			getDelegate().writeDefaultNamespace(ns);
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}
	
	public XmlStreamWriter writeStartDocument(String encoding,String version) {
		try {
			getDelegate().writeStartDocument(encoding, version);
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}
	
	public XmlStreamWriter writeStartDocument(String version) {
		try {
			getDelegate().writeStartDocument(version);
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}

	public XmlStreamWriter writeStartDocument() {
		try {
			getDelegate().writeStartDocument();
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}
	
	
	
	public XmlStreamWriter writeEndDocument() {
		try {
			getDelegate().writeEndDocument();
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}
	
	public XmlStreamWriter writeEmptyElement(String localName) {
		try {
			getDelegate().writeEmptyElement(localName);
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}
	
	public XmlStreamWriter writeStartElement(final QName qName){
		final String ns= qName.getNamespaceURI();
		if(StringUtils.isBlank(ns) || XMLConstants.NULL_NS_URI.equals(ns)) {
			return writeStartElement(qName.getLocalPart());
			}
		else if(StringUtils.isBlank(qName.getPrefix())) {
			return writeStartElement(ns, qName.getLocalPart());
			}
		else
			{
			return writeStartElement(qName.getPrefix(),qName.getLocalPart(),ns);
			}
		}

	
	
	public XmlStreamWriter writeStartElement(String localName) {
		try {
			getDelegate().writeStartElement(localName);
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}
	
	public XmlStreamWriter writeStartElement(String namespaceUri,String localName) {
		try {
			getDelegate().writeStartElement(namespaceUri,localName);
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}
	
	public XmlStreamWriter writeStartElement(String prefix, String localName,String namespaceUri) {
		try {
			getDelegate().writeStartElement(prefix,localName,namespaceUri);
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}
	
	public XmlStreamWriter writeEndElement() {
		try {
			getDelegate().writeEndElement();
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}
	
	public XmlStreamWriter writeAttributes(Map<String,?> atts) {
		for(String key:atts.keySet()) {
			writeAttribute(key,atts.get(key));
			}
		return this;
		}
	
	
	public XmlStreamWriter writeAttributes(
			String localName1,Object value1,
			String localName2,Object value2,
			String localName3,Object value3,
			String localName4,Object value4
			) {
		writeAttributes(
				localName1,value1,
				localName2,value2,
				localName3,value3
				);
		return writeAttribute(localName4,value4);
		}

	
	public XmlStreamWriter writeAttributes(
			String localName1,Object value1,
			String localName2,Object value2,
			String localName3,Object value3
			) {
		writeAttributes(
				localName1,value1,
				localName2,value2
				);
		return writeAttribute(localName3,value3);
		}
	
	public XmlStreamWriter writeAttributes(
			String localName1,Object value1,
			String localName2,Object value2
			) {
		writeAttribute(localName1,value1);
		return writeAttribute(localName2,value2);
		}
	
	public XmlStreamWriter writeAttribute(String localName,Object value) {
		try {
			getDelegate().writeAttribute(localName,convertObjectToText(value));
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}
	
	public XmlStreamWriter writeAttribute(String namespaceUri,String localName,Object value) {
		try {
			getDelegate().writeAttribute(namespaceUri, localName,convertObjectToText(value));
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}

	public XmlStreamWriter writeAttribute(String prefix,String namespaceUri,String localName,Object value) {
		try {
			getDelegate().writeAttribute(prefix,namespaceUri, localName,convertObjectToText(value));
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}

	
	public XmlStreamWriter writeAttribute(QName qName,Object value) {
		final String ns= qName.getNamespaceURI();
		if(StringUtils.isBlank(ns) || XMLConstants.NULL_NS_URI.equals(ns)) {
			return writeAttribute(qName.getLocalPart(),value);
			}
		else if(StringUtils.isBlank(qName.getPrefix())) {
			return writeAttribute(ns, qName.getLocalPart(),value);
			}
		else
			{
			return writeAttribute(qName.getPrefix(),ns,qName.getLocalPart(),value);
			}
		}
		
	public XmlStreamWriter writeDOM(NodeList n) {
		for(int i=0;i< n.getLength();i++) {
			writeDOM(n.item(i));
			}
		return this;
		}
	public XmlStreamWriter writeDOM(Node n) {
		if(n==null) return this;
		switch(n.getNodeType()) {
			case Node.DOCUMENT_NODE:
				writeStartDocument();
				for(Node c=n.getFirstChild();c!=null;c=c.getNextSibling()) {
					writeDOM(c);
					}
				return writeEndDocument();
			case Node.DOCUMENT_FRAGMENT_NODE:
				for(Node c=n.getFirstChild();c!=null;c=c.getNextSibling()) {
					writeDOM(c);
					}
				return this;
			case Node.ELEMENT_NODE:
				if(n.hasChildNodes()) {
					writeStartElement(toQName(n));
					if(n.hasAttributes()) {
						final NamedNodeMap nm=n.getAttributes();
						for(int i=0;i< nm.getLength();i++) {
							writeDOM(nm.item(i));
							}
						}
					for(Node c=n.getFirstChild();c!=null;c=c.getNextSibling()) {
						writeDOM(c);
						}
					writeEndElement();
					}
				else
					{
					writeEmptyElement(toQName(n));
					if(n.hasAttributes()) {
						final  NamedNodeMap nm=n.getAttributes();
						for(int i=0;i< nm.getLength();i++) {
							writeDOM(nm.item(i));
							}
						}
					}
				return this;
			case Node.ATTRIBUTE_NODE: return writeAttribute(toQName(n), Attr.class.cast(n).getValue());
			case Node.TEXT_NODE: return this.writeCharacters(Text.class.cast(n).getData());
			case Node.CDATA_SECTION_NODE: return this.writeCData(CDATASection.class.cast(n).getData());
			case Node.COMMENT_NODE: return this.writeCData(Comment.class.cast(n).getData());
			default: throw new IllegalStateException("TODO");
			}
		}
	
	public XmlStreamWriter writeEmptyElement(final QName qName){
		final String ns= qName.getNamespaceURI();
		if(StringUtils.isBlank(ns) || XMLConstants.NULL_NS_URI.equals(ns)) {
			return writeEmptyElement(qName.getLocalPart());
			}
		else if(StringUtils.isBlank(qName.getPrefix())) {
			return writeEmptyElement(ns, qName.getLocalPart());
			}
		else
			{
			return writeEmptyElement(qName.getPrefix(),qName.getLocalPart(),ns);
			}
		}

	
	public XmlStreamWriter writeEmptyElement(String namespaceURI, String localName){
		try {
			getDelegate().writeEmptyElement(namespaceURI,localName);
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}

	public XmlStreamWriter writeEmptyElement(String prefix, String localName, String namespaceURI)  {
		try {
			getDelegate().writeEmptyElement(prefix, localName, namespaceURI);
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}

	public XmlStreamWriter writeCharacters(Object text)  {
		try {
			getDelegate().writeCharacters(convertObjectToText(text));
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}
	
	public XmlStreamWriter writeCData(Object text)  {
		try {
			getDelegate().writeCData(convertObjectToText(text));
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}
	
	public XmlStreamWriter writeComment(Object text)  {
		try {
			getDelegate().writeComment(convertObjectToText(text));
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}
	
	protected String convertObjectToText(Object o) {
		return String.valueOf(o);
		}


	
	public XmlStreamWriter writeCharacters(char[] text, int start, int len) {
		try {
			getDelegate().writeCharacters(text, start, len);
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}
		}
	
	public XmlStreamWriter flush() {
		try {
			getDelegate().flush();
			return this;
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}		
		}

	
	public void close() {
		try {
			flush();
			getDelegate().close();
			if(runAtEnd!=null) {
				runAtEnd.run();
				runAtEnd=null;
				}
			}
		catch(XMLStreamException err ) {
			throw wrap(err);
			}		
		}
	public static XmlStreamWriter of(final Writer w) {
		final  XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
		XMLStreamWriter xw;
		try {
			xw = xmlfactory.createXMLStreamWriter(w);
			return new XmlStreamWriter(xw);
		} catch (XMLStreamException err) {
			throw wrap(err);
			}
		}
	public static XmlStreamWriter of(final Path path) {
		try {
			final Writer w= Files.newBufferedWriter(path);
			final XmlStreamWriter xw = of(w);
			xw.runAtEnd=()->{try{w.close();}catch(Throwable err) {}};
			return xw;
		} catch (IOException err) {
			throw new RuntimeIOException(err);
			}
		}

	}
