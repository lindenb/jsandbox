package sandbox.xml.minidom;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;

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

public Element(final QName qName) {
	this.qName = qName;
	}
public Element(final String tag) {
	this(new QName(tag));
	}

public Element(final QName qName,Object content) {
	this(qName);
	if(content!=null) setTextContent(content);
	}

public Element(final String tag,final Object content) {
	this(new QName(tag),content);
	}

public Element(final org.w3c.dom.Element root) {
	this.qName = toQName(root);
	if(root.hasAttributes()) {
		final NamedNodeMap atts=root.getAttributes();
		for(int i=0;i< atts.getLength();i++) {
			org.w3c.dom.Node named=atts.item(i);
			setAttribute(toQName(named), named.getNodeValue());
			}
		}
	}

public Element(final StartElement startElement) {
	this.qName=startElement.getName();
	for(Iterator<?> iter=startElement.getAttributes();iter.hasNext();) {
		setAttribute((Attribute)iter.next());
		}
	}

public boolean hasLocalName(final String lclName) {
	return getLocalName().equals(lclName);
	}

/** check element has given localName and return this */
public Element assertHasLocalName(final String lclName) {
	if(!hasLocalName(lclName)) throw new IllegalArgumentException("Expected <"+lclName+"> but got <"+getLocalName()+"> in "+this.getPath());
	return this;
	}

public boolean isA(final String ns,final String lclName) {
	return hasNamespaceURI(ns) && hasLocalName(lclName);
	}

@Override
public int getNodeType() {
	return org.w3c.dom.Node.ELEMENT_NODE;
	}


public QName getQName() {
	return qName;
	}

/** return prefix:localName or tagName */
public String getQualifiedName() {
	return (hasPrefix()?getPrefix()+":":"")+getLocalName();
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
@Override
public boolean hasChildNodes() {
	return getFirstChild()!=null;
	}

public String getPrefix() {
	return getQName().getPrefix();
	}
public boolean hasPrefix() {
	return !StringUtils.isBlank(getPrefix());
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

public Optional<Element> findFirstChildElement(final Predicate<Element> predicate) {
	for(Node c=getFirstChild();c!=null;c=c.getNextSibling()) {
		if(!c.isElement()) continue;
		if(!predicate.test(c.asElement())) continue;
		return Optional.of(c.asElement());
		}
	return Optional.empty();
	}


public Optional<Element> findFirstChildElement(final String lclName) {
	return findFirstChildElement(E->E.hasLocalName(lclName));
	}


public Element getRoot() {
	return hasParentNode()?getParentNode().getRoot():this;
	}

public List<Node> getAllNodes() {
	final List<Node> all = new ArrayList<>();
	find(N->all.add(N));
	return all;
	}

public Stream<Node> stream() {
	return StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(iterator(), 
            Spliterator.ORDERED),
            false
            );
	}

/** returns all child Elements */
public Stream<Element> elements() {
	return stream().filter(N->N.isElement()).map(E->E.asElement());
	}

/** return all child Element as List */
public List<Element> getChildElements() {
	return elements().collect(Collectors.toList());
	}

/** count child Element  */
public int countChildElements() {
	return (int)elements().count();
	}


/** return true if there is one Element as child */
public boolean hasChildElement() {
	for(Node n1=getFirstChild();n1!=null;n1=n1.getNextSibling()) {
		if(n1.isElement()) return true;
		}
	return false;
	}



public Node getFirstChild() {
	return firstChild;
	}

public Node getLastChild() {
	return lastChild;
	}

public boolean hasAttributes() {
	return !this.attributes.isEmpty();
}

public void setAttribute(Attribute att) {
	this.attributes.put(att.getName(), att.getValue());
	}

public void setAttribute(final QName qName,final Object value) {
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

public boolean hasAttribute(String ns,String lclName) {
	return hasAttribute(new QName(ns,lclName));
}

public Optional<String> getAttribute(final QName q) {
	return Optional.ofNullable(this.attributes.getOrDefault(q, null));
}
public Optional<String> getAttribute(String q) {
	return getAttribute(new QName(q));
}

public OptionalDouble getDoubleAttribute(String tag) {
	return getDoubleAttribute(new QName(tag));
	}

public OptionalDouble getDoubleAttribute(QName q) {
	Optional<String> opt= getAttribute(q);
	return opt.isPresent()?OptionalDouble.of(Double.valueOf(opt.get())):OptionalDouble.empty();
	}

public OptionalInt getIntAttribute(String tag) {
	return getIntAttribute(new QName(tag));
	}

public OptionalInt getIntAttribute(QName q) {
	Optional<String> opt= getAttribute(q);
	return opt.isPresent()?OptionalInt.of(Integer.valueOf(opt.get())):OptionalInt.empty();
	}


public Map<QName,String> getAttributes() {
	return this.attributes;
}

public void normalize() {
	for(Node c=getFirstChild();c!=null;c=c.getNextSibling()) {
		if(c.isText()) {
		for(;;) {
			Node next = c.getNextSibling();
			if ( next==null ||!next.isText()) break;
	        this.asText().appendData(next.getTextContent());
	        removeChild( next );
			}
		}
	}
}


public void insertBefore(final Node newNode, final Node referenceNode) {
	if(referenceNode==null) {
		appendChild(newNode);
		return;
		}
	if(newNode.isSameNode(referenceNode)) throw new IllegalArgumentException();
	assertNotAncestor(referenceNode);
	for(Node c=getFirstChild();c!=null;c=c.getNextSibling()) {
		if(c.isSameNode(referenceNode)) {
			newNode.unlink();
			if(referenceNode.prevSibling!=null) 
				{
				referenceNode.prevSibling.nextSibling = newNode;
				newNode.prevSibling =referenceNode.prevSibling;
				}
			else
				{
				this.firstChild = newNode;
				}
			newNode.nextSibling=referenceNode;
			referenceNode.prevSibling=newNode;
			newNode.parentNode=this;
			return;
			}
		}
	throw new IllegalArgumentException("node "+referenceNode.getPath()+" is not a child of "+this.getPath());
	}

public void removeChild(final Node c) {
	if(c==null) return;
	stream().filter(N->N.isSameNode(c)).findFirst().orElseThrow(()->new IllegalArgumentException("node "+c.getPath()+" is not a child of "+this.getPath()));
	
	if(c.prevSibling!=null) {
		c.prevSibling.nextSibling = c.nextSibling; 
		}
	if(c.nextSibling!=null) {
		c.nextSibling.prevSibling = c.prevSibling; 
		}
	if(this.firstChild==c) {
		this.firstChild=c.nextSibling;
		}
	if(this.lastChild==c) {
		this.lastChild=c.prevSibling;
		}
	c.parentNode=null;
	c.nextSibling=null;
	c.prevSibling=null;
	}

private void assertNotAncestor(final Node n) {
	Element p = getParentNode();
	while(p!=null) {
		if(p.isSameNode(n)) throw new IllegalArgumentException(n.getPath()+" is an ancestor of "+this.getPath());
		p = p.getParentNode();
		}
	}

public void appendChild(Node n) {
	if(n==null) return;
	assertNotAncestor(n);
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
	if(!hasChildNodes()) return "";
	final StringBuilder sb = new StringBuilder();
	for(Node n1=getFirstChild();n1!=null;n1=n1.getNextSibling()) {
		sb.append(n1.getTextContent());
		}
	return sb.toString();
	}

@Override
public void write(final XMLStreamWriter w) throws XMLStreamException {
	if(!hasChildNodes()) {
		if(!hasNamespaceURI()) {
			w.writeEmptyElement(getLocalName());
			}
		else if(!hasPrefix()) {
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
		else if(!hasPrefix()) {
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
	
@Override
public Node clone(boolean deep) {
	final Element cp = new Element(this.getQName());
	cp.attributes.putAll(this.attributes);
	for(Node c= getFirstChild(); c!=null && deep; c=c.getNextSibling()) {
		cp.appendChild(c.clone(deep));
		}
	return cp;
	}

@Override
public String getPath() {
	String s=getQualifiedName();
	if(hasParentNode()) {
		int i=0;
		for(Node c=getParentNode().getFirstChild();c!=null;c=c.getNextSibling()) {
			if(!c.isElement()) continue;
			if(c.asElement().getQName().equals(this.getQName())) {
				i++;
				if(c==this) {
					s+="["+i+"]";
					break;
					}
				}
			}
		}
	else
		{
		s="/"+s;
		}
	return s;
	}

@Override
public org.w3c.dom.Element toDOM(org.w3c.dom.Document owner) {
	org.w3c.dom.Element E;
	if(hasNamespaceURI()) {
		E = owner.createElementNS(getNamespaceURI(), getQualifiedName());
		}
	else
		{
		E = owner.createElement(getLocalName());
		}
	for(QName key: getAttributes().keySet()) {
		final String value = getAttribute(key).get();
		if(StringUtils.isBlank(key.getNamespaceURI())) {
			E.setAttribute(key.getLocalPart(), value);
			}
		else
			{
			E.setAttributeNS(key.getNamespaceURI(), (StringUtils.isBlank(key.getPrefix())?"":key.getPrefix()+":")+key.getLocalPart(), value);
			}
		}
	for(Node c= getFirstChild();c!=null;c=c.getNextSibling()) {
		E.appendChild(c.toDOM(owner));
		}

	return E;
	}
@Override
public int hashCode() {
	int i= this.attributes.hashCode();
	for(Node c= getFirstChild();c!=null;c=c.getNextSibling()) {
		i=i%31 + c.hashCode();
		}
	return i;
	}

@Override
public boolean isEqualNode(final Node other) {
	if(other==null || !other.isElement()) return false;
	if(isSameNode(other)) return true;
	Element o=other.asElement();
	if(!getQName().equals(o.getQName())) return false;
	if(this.getAttributes().size()!=o.getAttributes().size()) return false;
	for(QName k:this.getAttributes().keySet()) {
		if(!o.hasAttribute(k)) return false;
		if(!this.getAttribute(k).get().equals(o.getAttribute(k).get())) return false;
		}
	Node c1=this.getFirstChild();
	Node c2=o.getFirstChild();
	for(;;) {
		if(c1==null && c2==null) return true; 
		if(c1==null || c2==null) return false;
		if(!c1.isEqualNode(c2)) return false;
		c1=c1.getNextSibling();
		c2=c2.getNextSibling();
		}
	}


@Override
public final boolean isDataNode() {
	boolean has_element=false;
	boolean has_non_ws = false;
	for(Node n1=this.getFirstChild();n1!=null;n1=n1.getNextSibling()) {
		if(n1.isText()&& n1.asText().isBlank()) has_non_ws=true;
		if(n1.isElement()) has_element=true;
		if(has_non_ws && has_element) return false;
		if(!n1.isDataNode()) return false;
		}
	return true;
	}

/**
 * @rerturn true if Element contains NO element as child and one or more Text element
 */
public boolean isElementWithTextOnly() {
	boolean has_text=false;
	for(Node n1=this.getFirstChild();n1!=null;n1=n1.getNextSibling()) {
		if(n1.isText()) has_text=true;
		if(n1.isElement()) return false;
		}
	return has_text;
	}

public static Element importDOM(org.w3c.dom.Element e,boolean deep) {
	final Element root=new Element(e);
	if(deep) {
		for(org.w3c.dom.Node c1=e.getFirstChild();c1!=null;c1=c1.getNextSibling()) {
			switch(c1.getNodeType()) {
				case org.w3c.dom.Node.TEXT_NODE:
				case org.w3c.dom.Node.CDATA_SECTION_NODE:
					root.appendChild(Text.importDOM(org.w3c.dom.CharacterData.class.cast(c1)));
					break;
				case org.w3c.dom.Node.ELEMENT_NODE:
					root.appendChild(Element.importDOM(org.w3c.dom.Element.class.cast(c1),deep));
					break;
				default: throw new IllegalArgumentException("cannot import nodetype="+c1.getNodeType());
				}
			}
		}
	return root;
	}

@Override
public String toString() {
	return "<"+getQualifiedName()+"/>";
	}
}
