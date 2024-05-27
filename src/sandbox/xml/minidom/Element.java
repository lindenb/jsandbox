package sandbox.xml.minidom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.NamedNodeMap;

import sandbox.StringUtils;
import sandbox.iterator.AbstractIterator;



public class Element extends  Node implements Iterable<Node> {
protected Node firstChild=null;
protected Node lastChild=null;
protected final Map<QName,String> attributes = new HashMap<>();
protected QName qName;
public Element() {
	}

public Element(QName qName) {
	this.qName = qName;
	}
public Element(String tag) {
	this(new QName(tag));
	}

public Element(QName qName,Object content) {
	this(qName);
	if(content!=null) setTextContent(content);
	}

public Element(String tag,Object content) {
	this(new QName(tag),content);
	}

public Element(org.w3c.dom.Element root) {
	this.qName = toQName(root);
	if(root.hasAttributes()) {
		final NamedNodeMap atts=root.getAttributes();
		for(int i=0;i< atts.getLength();i++) {
			org.w3c.dom.Node named=atts.item(i);
			setAttribute(toQName(named), named.getNodeValue());
			}
		}
	}

public QName getQName() {
	return qName;
	}

public String getLocalName() {
	return getQName().getLocalPart();
	}

public boolean hasNamespaceURI(String ns) {
	return getNamespaceURI().equals(ns);
	}

public boolean hasNamespaceURI() {
	return !StringUtils.isBlank(getNamespaceURI());
	}

public String getNamespaceURI() {
	return getQName().getNamespaceURI();
	}
public boolean hasChildNodes() {
	return getFirstChild()!=null;
	}

public String getPrefix() {
	return getQName().getPrefix();
}

@Override
public Iterator<Node> iterator() {
	return new AbstractIterator<Node>() {
		Node n=getFirstChild();
		@Override
		protected Node advance() {
			final Node x = n;
			if(n!=null) n=n.getNextSibling();
			return x;
			}
		};
	}

@Override
public void find(final Consumer<Node> consumer) {
	consumer.accept(this);
	for(Node c=getFirstChild();c!=null;c=c.getNextSibling()) {
		c.find(consumer);
		}
	}

public Element getRoot() {
	return hasParentNode()?getParentNode().getRoot():this;
	}

public Stream<Node> stream() {
	return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(iterator(), 
            Spliterator.ORDERED),
            false
            );
	}



public Node getFirstChild() {
	return firstChild;
	}

public boolean hasAttributes() {
	return !this.attributes.isEmpty();
}

public void setAttribute(QName qName,Object value) {
	if(value==null) {
		this.attributes.remove(qName);
		return;
		}
	this.attributes.put(qName, Node.toString(value));
	}
public void setAttribute(String lclName,Object value) {
	setAttribute(new QName(lclName),value);
	}

public boolean hasAttribute(String s) {
	return hasAttribute(new QName(s));
}
public boolean hasAttribute(QName qN) {
	return this.attributes.containsKey(qN);
}
public Optional<String> getAttribute(QName q) {
	return Optional.ofNullable(this.attributes.getOrDefault(q, null));
}
public Optional<String> getAttribute(String q) {
	return getAttribute(new QName(q));
}

public Map<QName,String> getAttributes() {
	return this.attributes;
}

public void removeChild(final Node c) {
	Node rm=null;
	if(this.firstChild==c) {
		rm=this.firstChild;
		this.firstChild=c.nextSibling;
		}
	if(this.lastChild==c) {
		rm=this.lastChild;
		this.lastChild=c.prevSibling;
		}
	if(rm==null) {
		rm = stream().filter(N->N==c).findFirst().get();
		}
	
	if(rm.prevSibling!=null) {
		rm.prevSibling.nextSibling = rm.nextSibling; 
		}
	if(rm.nextSibling!=null) {
		rm.nextSibling.prevSibling = rm.prevSibling; 
		}
		
	rm.parentNode=null;
	rm.nextSibling=null;
	rm.prevSibling=null;
	}


public void appendChild(Node n) {
	n.unlink();
	if(this.firstChild==null) {
		this.firstChild=n;
		this.lastChild=n;
		}
	else
		{
		this.lastChild.nextSibling=n;
		n.prevSibling=this.lastChild;
		this.lastChild=n;
		}
	n.parentNode=this;
	}

public void removeAllChildren() {
	while(getFirstChild()!=null) {
		this.removeChild(this.getFirstChild());
	}
}

@Override
public final boolean isText() {
	return false;
	}
@Override
public final boolean isElement() {
	return true;
	}

@Override
public void setTextContent(Object o) {
	this.removeAllChildren();
	if(o==null) return;
	this.appendChild(new Text(o));
	}

@Override
public String getTextContent() {
	final StringBuilder sb = new StringBuilder();
	for(Node n1=getFirstChild();n1!=null;n1=n1.getNextSibling()) {
		sb.append(n1.getTextContent());
		}
	return sb.toString();
	}

@Override
	public void write(XMLStreamWriter w) throws XMLStreamException {
		if(!hasChildNodes()) {
			if(!hasNamespaceURI()) {
				w.writeEmptyElement(getLocalName());
				}
			else if(StringUtils.isBlank(getPrefix())) {
				w.writeEmptyElement(getNamespaceURI(), getLocalName());
				}
			else
				{
				w.writeEmptyElement(getPrefix(),getLocalName(), getNamespaceURI());
				}
			}
		else
			{
			if(!hasNamespaceURI()) {
				w.writeStartElement(getLocalName());
				}
			else if(StringUtils.isBlank(getPrefix())) {
				w.writeStartElement(getNamespaceURI(), getLocalName());
				}
			else
				{
				w.writeStartElement(getPrefix(),getLocalName(), getNamespaceURI());
				}
			}
			
		for(QName key: this.attributes.keySet()) {
			if(StringUtils.isBlank(key.getNamespaceURI())) {
				w.writeStartElement(getLocalName());
				}
			else if(StringUtils.isBlank(key.getPrefix())) {
				w.writeStartElement(getNamespaceURI(), getLocalName());
				}
			else
				{
				w.writeStartElement(key.getPrefix(),key.getLocalPart(), key.getNamespaceURI());
				}
			}
			
		
		if(hasChildNodes())
			{
			for(Node c= getFirstChild(); c!=null; c=c.getNextSibling()) {
				c.write(w);
				}
			w.writeEndElement();
			}
		}
	}
