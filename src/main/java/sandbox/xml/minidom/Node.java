package sandbox.xml.minidom;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import sandbox.lang.StringUtils;

public abstract class Node {
private static long ID_GENERATOR=0L;
private long node_id=ID_GENERATOR++;
protected Element parentNode=null;
protected Node prevSibling=null;
protected Node nextSibling=null;
private Map<String,Object> userData=null;
protected Node() {
	}

public final String getNodeId() {
	return "n"+this.node_id;
	}


public Map<String,Object>  getUserData() {
	if(this.userData==null) return Collections.emptyMap();
	return Collections.unmodifiableMap(this.userData);
	}

public Object getUserData(final String key) {
	if(this.userData==null) return null;
	return this.userData.get(key);
	}

public Object removeUserData(String key) {
	if(this.userData==null) return null;
	final Object o2  = this.userData.remove(key);
	if(this.userData.isEmpty()) this.userData=null;
	return o2;
	}


public Object setUserData(String key,Object o) {
	if(o==null) return removeUserData(key);
	if(this.userData==null) this.userData = createUserDataMap();
	return this.userData.put(key, o);
	}


/** create default map for this.userData */
protected  Map<String,Object> createUserDataMap() {
	return new HashMap<>();
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


public abstract void findDeep(final Consumer<Node> consumer);
public abstract Node clone(boolean deep);
public abstract boolean isText() ;
public abstract boolean isElement();
public Element asElement() {
	return Element.class.cast(this);
	}
public Text asText() {
	return Text.class.cast(this);
	}

public boolean isRoot() {
	return !hasParentNode();
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
/** same as org.w3c.Node */
public abstract int getNodeType();

public int getDepth() {
	return hasParentNode() ?1+getParentNode().getDepth():0;
	}
/** return true if there is at least one child */
public abstract boolean hasChildNodes();


public abstract String getPath();

public static String toString(Object o) {
	if(o instanceof org.w3c.dom.CharacterData) return toString(org.w3c.dom.CharacterData.class.cast(o).getData());
	return String.valueOf(o);
	}

public abstract boolean isEqualNode(final Node other);

@Override
public boolean equals(final Object obj) {
	if(obj==this) return true;
	if(obj==null || !(obj instanceof Node)) return false;
	return isEqualNode((Node)obj);
	}

public boolean isSameNode(final Node other) {
	return this==other;
	}
/** return true if Element contains only Text, or (Element+blank text) */
public abstract boolean isDataNode();
public void assertIsDataNode() {
	if(!isDataNode()) throw new IllegalStateException("not a data node:"+getPath());
	}

public abstract void setTextContent(Object o);
public abstract String getTextContent();



public abstract org.w3c.dom.Node toDOM(org.w3c.dom.Document owner);

public abstract void write(XMLStreamWriter w) throws XMLStreamException;
}
