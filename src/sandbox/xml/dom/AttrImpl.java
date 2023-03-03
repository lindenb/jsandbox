package sandbox.xml.dom;

import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.TypeInfo;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class AttrImpl extends NamedNode implements org.w3c.dom.Attr {
	private Element owner = null;
	private String value="";
	AttrImpl(final DocumentImpl owner,final QName qName) {
		super(owner,qName);
		}
	public String getValue() {
		return this.value;
		}
	
	@Override
	public final short getNodeType() {
		return ATTRIBUTE_NODE;
		}
	
	@Override
	public Node cloneNode(boolean deep) {
		Attr att = getOwnerDocument().createAttribute(getName());
		
		att.setValue(this.value);
		return att;
		}
	
	@Override
	public boolean isId() {
		throw new UnsupportedOperationException();
		}
	@Override
	public TypeInfo getSchemaTypeInfo() {
		throw new UnsupportedOperationException();
		}
	
	@Override
	public final String getTextContent() throws DOMException {
		return getValue();
		}
	
	@Override
	public void setValue(String value) {
		this.value = value;
		}
	@Override
	public final void setNodeValue(String nodeValue) throws DOMException {
		this.setValue(value);
		}
	
	@Override
	public boolean hasAttributes() {
		return false;
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
	public List<AbstractNode> getChildrenAsList() {
		return Collections.emptyList();
		}
	
	@Override
	public void sax(DefaultHandler handler) throws SAXException {
		
		}
	@Override
	public String getName() {
		throw new UnsupportedOperationException();
		}
	@Override
	public boolean getSpecified() {
		throw new UnsupportedOperationException();
		}
	@Override
	public Element getOwnerElement() {
		return this.owner;
		}

	}
