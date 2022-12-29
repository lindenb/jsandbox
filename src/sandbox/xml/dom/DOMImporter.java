package sandbox.xml.dom;

import java.util.ArrayList;

import javax.xml.namespace.QName;

import org.w3c.dom.NamedNodeMap;

import sandbox.StringUtils;

public class DOMImporter {
public Node importNode(org.w3c.dom.Node n) {
	return importNode(null,n);
	}

protected QName toQName(final org.w3c.dom.Node n) {
	if(StringUtils.isBlank(n.getNamespaceURI())) {
		return new QName(n.getLocalName());
		}
	else if(StringUtils.isBlank(n.getPrefix())) {
		return new QName(n.getNamespaceURI(),n.getLocalName());
		}
	else
		{
		return new QName(n.getNamespaceURI(), n.getLocalName(), n.getPrefix());
		}
	}

public Node importNode(Element parent,org.w3c.dom.Node n) {
	switch(n.getNodeType()) {
	case org.w3c.dom.Node.DOCUMENT_NODE:
		return importNode(org.w3c.dom.Document.class.cast(n).getDocumentElement());
	case org.w3c.dom.Node.ATTRIBUTE_NODE:
		return new Attr(parent, toQName(n),org.w3c.dom.Attr.class.cast(n).getValue());
	case org.w3c.dom.Node.ELEMENT_NODE:
		Element e = new Element(parent,toQName(n));
		if(n.hasAttributes()) {
			final NamedNodeMap nnm = n.getAttributes();
			e._atts=new ArrayList<>(nnm.getLength());
			for(int i=0;i< nnm.getLength();i++) {
				e._atts.add(Attr.class.cast(importNode(e,nnm.item(i))));
				}
			}
	
		for(org.w3c.dom.Node c=n.getFirstChild();c!=null;c=c.getNextSibling()) {
			final Node n2 = importNode(e,c);
			if(n2==null) continue;
			e._children.add(n2);
			}
		return e;
	case org.w3c.dom.Node.TEXT_NODE:
		Text t = new Text(parent,org.w3c.dom.Text.class.cast(n).getData());
		return t;
	default:
		throw new IllegalArgumentException();
	}
	}
}
