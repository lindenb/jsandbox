package sandbox.tools.comicsbuilder;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

import sandbox.xml.XmlUtils;

public class AbstractNode {
	protected Element element;
	AbstractNode(Element element) {
		this.element = element;
		}
	AbstractNode() {
		this(null);
	}
	
	Element getElement() {
		return this.element;
	}
	
	String getRequiredAttribute(final String key) {
		final Attr att = getElement().getAttributeNode(key);
		if(att==null)  throw new IllegalArgumentException("No @"+key+" defined under "+ XmlUtils.getNodePath(getElement()));
		return att.getValue();
		}
}
