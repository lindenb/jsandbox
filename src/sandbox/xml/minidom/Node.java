package sandbox.xml.minidom;

import java.util.function.Consumer;
import java.util.function.Function;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;


import sandbox.StringUtils;

public abstract class Node {
protected Element parentNode=null;
protected Node prevSibling=null;
protected Node nextSibling=null;
protected Node() {
	}


public Node unlink() {
	if(this.parentNode!=null) {
		this.parentNode.asElement().removeChild(this);
		this.parentNode=null;
		}
	return this;
	}

protected static QName toQName(org.w3c.dom.Node root) {
	String ns=root.getNamespaceURI();
	if(StringUtils.isBlank(ns)) {
		return new QName(root.getNodeName());
		}
	else
		{
		final String prefix = root.getPrefix();
		if(StringUtils.isBlank(prefix)) return new QName(ns, root.getLocalName());
		return new QName(ns,root.getLocalName(),prefix);
		}
	}


public abstract void find(final Consumer<Node> consumer);
public abstract Node clone(boolean deep);
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

public abstract String getPath();

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
				for(org.w3c.dom.Node x=root.getFirstChild();x!=null;x=x.getNextSibling()) {
					final Node n= Node.importDOM(x,elementConverter);
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

public abstract org.w3c.dom.Node toDOM(org.w3c.dom.Document owner);

public abstract void write(XMLStreamWriter w) throws XMLStreamException;
}
