package sandbox.xml.dom;

import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Attr extends NamedNode {
	final String value;
	Attr(Element e,final QName qName,String value) {
		super(e,qName);
		this.value = value;
		}
	public String getValue() {
		return this.value;
		}
	@Override
	public int hashCode() {
		return super.hashCode()*31 + getValue().hashCode();
		}
	
	@Override
	public String getPath() {
		String s= "@"+getNodeName();
		if(getParentNode()!=null) {
			s=getParentNode().getPath()+"/"+s;
			}
		return s;
		}
	@Override
	public List<Node> getChildrenAsList() {
		return Collections.emptyList();
		}
	@Override
	public final boolean isElement() {
		return false;
		}
	@Override
	public final boolean isAttribute() {
		return true;
		}
	@Override
	public org.w3c.dom.Node toDOM(org.w3c.dom.Document doc) {
		org.w3c.dom.Attr att;
		if(hasNamespaceURI()) {
			att =  doc.createAttributeNS(getNamespaceURI(), getNodeName());
			}
		else
			{
			att= doc.createAttribute(getNodeName());
			}
		att.setNodeValue(getValue());
		return att;
		}
	@Override
	public void sax(DefaultHandler handler) throws SAXException {
		
		}

	}
