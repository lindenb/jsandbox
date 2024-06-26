package sandbox.xml.minidom;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

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

public abstract boolean isEqualNode(final Node other);

@Override
public boolean equals(final Object obj) {
	if(obj==this) return true;
	if(obj==null || !(obj instanceof Node)) return false;
	return isEqualNode((Node)obj);
	}

public boolean isSameNode(Node other) {
	return this==other;
	}

public abstract void setTextContent(Object o);
public abstract String getTextContent();



public abstract org.w3c.dom.Node toDOM(org.w3c.dom.Document owner);

public abstract void write(XMLStreamWriter w) throws XMLStreamException;
}
