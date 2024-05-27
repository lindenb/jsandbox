package sandbox.xml.minidom;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.NodeList;

import sandbox.StringUtils;

public abstract class Node {
protected Element parentNode=null;
protected Node prevSibling=null;
protected Node nextSibling=null;
protected Node() {
	}

public void unlink() {
	if(this.parentNode==null) return;
	this.parentNode.asElement().removeChild(this);
	this.parentNode=null;
	}
protected QName toQName(org.w3c.dom.Node root) {
	String ns=root.getNamespaceURI();
	if(StringUtils.isBlank(ns)) {
		return new QName(root.getNodeName());
		}
	else
		{
		String prefix = root.getPrefix();
		if(StringUtils.isBlank(prefix)) return new QName(ns, root.getLocalName());
		return new QName(ns,root.getLocalName(),prefix);
		}
	}


public abstract void find(final Consumer<Node> consumer);

public abstract boolean isText() ;
public abstract boolean isElement();
public Element asElement() {
	return Element.class.cast(this);
	}
public Text asText() {
	return Text.class.cast(this);
	}
public boolean hasParentNode() {
	return getParentNode()!=null;
}
public Element getParentNode() {
	return parentNode;
	}
public Node getPrevSibling() {
	return prevSibling;
	}
public Node getNextSibling() {
	return nextSibling;
	}

public int getDepth() {
	return hasParentNode() ?1+getParentNode().getDepth():0;
	}

public static String toString(Object o) {
	return String.valueOf(o);
	}


public abstract void setTextContent(Object o);
public abstract String getTextContent();

public static Node importDOM(org.w3c.dom.Node root,final Function<org.w3c.dom.Element,Element> elementConverter) {
	switch(root.getNodeType()) {
		case org.w3c.dom.Node.ELEMENT_NODE:
			Element E= new Element( org.w3c.dom.Element.class.cast(root));
			if(root.hasChildNodes()) {
				NodeList L=root.getChildNodes();
				for(int i=0;i< L.getLength();i++) {
					Node n= Node.importDOM(L.item(i),elementConverter);
					if(n==null) continue;
					E.appendChild(n);
					}
				}
			return E;
		case org.w3c.dom.Node.TEXT_NODE:
		case org.w3c.dom.Node.CDATA_SECTION_NODE:
			return new Text(root.getTextContent());
		default: return null;
		}
	}
public static Node importDOM(org.w3c.dom.Node root) {
	return importDOM(root,ELT->new Element( org.w3c.dom.Element.class.cast(ELT)));
	}


public abstract void write(XMLStreamWriter w) throws XMLStreamException;
}
