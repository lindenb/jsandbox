package sandbox.xml.dom;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.DOMException;
import org.w3c.dom.Node;
import org.w3c.dom.TypeInfo;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
/**
 * implementation of org.w3c.dom.Attr
 */
public class AttrImpl extends AbstractTerminalNode implements org.w3c.dom.Attr {
	private final QName qName;
	private ElementImpl owner = null;
	private String value="";
	private boolean is_id;
	
	private static TypeInfo DEFAULT_TYPE_INFO = new TypeInfo() {
		@Override
		public String getTypeName() {
			return null;
			}
		@Override
		public String getTypeNamespace() {
			return null;
			}
		@Override
		public boolean isDerivedFrom(String typeNamespaceArg, String typeNameArg, int derivationMethod) {
			return false;
			}
		};
	
	AttrImpl(final DocumentImpl owner,final QName qName) {
		super(owner);
		this.qName = qName;
		this.is_id = false;
		}
	
	
	@Override
	public final AbstractNode getParentNode() {
		return null;
		}
	
	@Override
	public AttrImpl unlink() {
		if(getOwnerElement()!=null) {
			getOwnerElement().removeAttributeNode(this);
			setOwnerElement(null);
			}
		return this;
		}
	@Override
	public String getValue() {
		return this.value;
		}
	
	void setId(boolean b)  {
		this.is_id = b;
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
		return this.is_id;
		}
	@Override
	public TypeInfo getSchemaTypeInfo() {
		return DEFAULT_TYPE_INFO;
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
	public final void setNodeValue(final String nodeValue) throws DOMException {
		this.setValue(value);
		}
	
	@Override
	public int hashCode() {
		return super.hashCode()*31 + getValue().hashCode();
		}
	
	@Override
	public String getPath() {
		String s= "@"+getNodeName();
		if(getOwnerElement()!=null) {
			s=getOwnerElement().getPath()+"/"+s;
			}
		return s;
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
		if(!hasNamespaceURI()) {
			w.writeAttribute(getLocalName(), getValue());
			}
		else if(!hasPrefix())
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


	@Override
	public void write(XMLEventWriter w, XMLEventFactory factory) throws XMLStreamException {
		// TODO Auto-generated method stub
		
		}
	}
