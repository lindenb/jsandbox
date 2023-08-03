package sandbox.xml;

public class XMLException extends RuntimeException {
private static final long serialVersionUID = 1L;

public XMLException(final String message) {
	super(message);
	}
public XMLException(org.w3c.dom.Node node, final String message) {
	this(XmlUtils.getNodePath(node)+" : "+String.valueOf(message));
	}
public XMLException(org.w3c.dom.Node node) {
	this(node,"Error");
	}
}
