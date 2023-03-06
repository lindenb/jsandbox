package sandbox.xml.dom;



import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.TypeInfo;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class AttrImpl extends AbstractTerminalNode implements org.w3c.dom.Attr {
	private final QName qName;
	private ElementImpl owner = null;
	private String value="";
	AttrImpl(final DocumentImpl owner,final QName qName) {
		super(owner);
		this.qName = qName;
		}
	
	@Override
	public String getValue() {
		return this.value;
		}
	
	@Override
	public QName getQName() {
		return this.qName;
		}
	
	@Override
	public final String getNodeValue() throws DOMException {
		return getValue();
		}
	
	@Override
	public final short getNodeType() {
		return ATTRIBUTE_NODE;
		}
	
	@Override
	public Node cloneNode(boolean deep) {
		return getOwnerDocument().createAttribute(getQName(),getValue());
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
	public final boolean hasChildNodes() {
		return false;
		}
	
	@Override
	public void sax(DefaultHandler handler) throws SAXException {
		}
	
	@Override
	public final String getName() {
		return getNodeName();
		}
	@Override
	public boolean getSpecified() {
		throw new UnsupportedOperationException();
		}
	@Override
	public ElementImpl getOwnerElement() {
		return this.owner;
		}
	/* package */ void setOwnerElement(ElementImpl e) {
		this.owner = e;
		}
	
	public void write(XMLStreamWriter w) throws XMLStreamException {
		if(getNamespaceURI()==null) {
			w.writeAttribute(getLocalName(), getValue());
			}
		else if(getPrefix()==null)
			{
			w.writeAttribute(getNamespaceURI(),getLocalName(), getValue());
			}
		else
			{
			w.writeAttribute(getPrefix(),getNamespaceURI(),getLocalName(), getValue());
			}
		}
	
	@Override
	public String toString() {
		return "@"+getNodeName()+"="+getValue();
		}
	}
