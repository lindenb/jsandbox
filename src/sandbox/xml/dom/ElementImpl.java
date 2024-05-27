package sandbox.xml.dom;

import java.util.function.Predicate;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.TypeInfo;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import sandbox.StringUtils;

public class ElementImpl extends AbstractNamedNode implements org.w3c.dom.Element {
	protected NamedNodeMapImpl namedNodeMap = null;

	
	public ElementImpl(final DocumentImpl owner,final QName qName) {
		super(owner, qName);
		}



	@Override
	public final String getTagName() {
		return getNodeName();
		}
	
	@Override
	public final String getNodeValue() throws DOMException {
		return null;//spec
		}
	

	
	@Override
	public short getNodeType() {
		return ELEMENT_NODE;
		}
	
	protected AbstractNode adoptDoc(final DocumentImpl doc) {
		if(this.namedNodeMap!=null) {
			this.namedNodeMap.forEach(N->AttrImpl.class.cast(N).adoptDoc(doc));
			}
		return super.adoptDoc(doc);
		}
	
	
	@Override
	public Node cloneNode(boolean deep) {
		org.w3c.dom.Element cp;
		if(getNamespaceURI()==null) {
			cp = getOwnerDocument().createElement(getTagName());
			}
		else
			{
			cp = getOwnerDocument().createElementNS(getNamespaceURI(),getNodeName());
			}
		if(hasAttributes()) {
			final NamedNodeMap nm = this.getAttributes();
			for(int i=0;i< nm.getLength();i++) {
				final Attr att = (Attr)nm.item(i).cloneNode(deep);
				if(att.getNamespaceURI()==null) {
					cp.setAttributeNode(att);
					}
				else
					{
					cp.setAttributeNodeNS(att);
					}
				}
			}
		if(deep) {
			for(Node c= this.getFirstChild();c!=null;c=c.getNextSibling()) {
				cp.appendChild(c.cloneNode(deep));
				}
			}
		return cp;
		}
	
	
	@Override
	public void setTextContent(final String textContent) throws DOMException {
		this.removeAllChildNodes();
		this.appendChild(getOwnerDocument().createTextNode(textContent));
		}
	
	private boolean hasAttribute(final Predicate<Node> filter) {
		return namedNodeMap!=null && namedNodeMap.
				stream().
				anyMatch(filter);
		}
	
	@Override
	public boolean hasAttribute(String name) {
		return hasAttribute(createNodeMatcher(name));
		}

	@Override
	public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException {
		return hasAttribute(createNodeMatcher(namespaceURI, localName));
		}
	
	@Override
	public AttrImpl removeAttributeNode(Attr oldAttr) throws DOMException {
		if(namedNodeMap==null || oldAttr.getOwnerElement()!=this) throw new DOMException(DOMException.NOT_FOUND_ERR, "cannot find this attr");
		AttrImpl att;
		if(oldAttr.getNamespaceURI()!=null) {
			att=(AttrImpl)namedNodeMap.removeNamedItemNS(oldAttr.getNamespaceURI(), oldAttr.getLocalName());
			}
		else
			{
			att=(AttrImpl)namedNodeMap.removeNamedItem(oldAttr.getNodeName());
			}
		if(att==null) throw new DOMException(DOMException.NOT_FOUND_ERR, "not found");
		att.setOwnerElement(null);
		return att;
		}
	
	@Override
	public String getAttribute(final String name) {
		final Attr  n=getAttributeNode(name);
		return n==null?"":n.getValue();
		}
	@Override
	public AttrImpl getAttributeNode(final String name) {
		if(this.namedNodeMap==null) return null;
		return (AttrImpl) this.namedNodeMap.getNamedItem(name);
		}
	
	@Override
	public NamedNodeMapImpl getAttributes() {
		if(namedNodeMap==null) return NamedNodeMapImpl.getEmptyNamedNodeMap();
		return namedNodeMap;
		}
	
	@Override
	public NodeListImpl<ElementImpl> getElementsByTagName(String name) {
		return findAll(E->E.isElement() && name.equals(Element.class.cast(E).getTagName())).asElements();
		}
	
	@Override
	public NodeList getElementsByTagNameNS(String namespaceURI, String localName) throws DOMException {
		return findAll(E->E.isElement() && namespaceURI.equals(E.getNamespaceURI()) && localName.equals(E.getLocalName())).asElements();
		}
	
	
	@Override
	public void setAttribute(String name, String value) throws DOMException {
		final Attr att = getOwnerDocument().createAttribute(name);
		att.setValue(value);
		setAttributeNode(att);
		}
	@Override
	public Attr setAttributeNode(Attr newAttr) throws DOMException {
		if(newAttr.getOwnerDocument()!=this.getOwnerDocument()) throw new DOMException(DOMException.WRONG_DOCUMENT_ERR,"wrong doc");
		AttrImpl x = AttrImpl.class.cast(newAttr);
		Element owner = x.getOwnerElement();
		if(owner==this) return null;
		
		if(namedNodeMap==null) namedNodeMap=new NamedNodeMapImpl();
		Attr old= (Attr)namedNodeMap.getNamedItem(newAttr.getName());
		namedNodeMap.setNamedItem(newAttr);
		return old;
		}
	@Override
	public Attr setAttributeNodeNS(Attr newAttr) throws DOMException {
		if(newAttr.getOwnerDocument()!=this.getOwnerDocument()) throw new DOMException(DOMException.WRONG_DOCUMENT_ERR,"wrong doc");
		if(namedNodeMap==null) namedNodeMap=new NamedNodeMapImpl();
		Attr old= (Attr)namedNodeMap.getNamedItemNS(newAttr.getNamespaceURI(),newAttr.getName());
		namedNodeMap.setNamedItemNS(newAttr);
		return old;
		}
	
	
	@Override
	public void removeAttribute(String name) throws DOMException {
		if(namedNodeMap==null)return;
		if(namedNodeMap.stream().noneMatch(createNodeMatcher(name))) return;
		AttrImpl att = (AttrImpl)namedNodeMap.removeNamedItem(name);
		if(att!=null) att.unlink();
		att.setOwnerElement(null);
		}

	

	
	@Override
	public String getPath() {
		String s= getNodeName();
		
		if(getParentNode()!=null) {
			String indexstr="";
			int i=-1;
			for(AbstractNode n: getChildNodes()) {
				if(n==this) break;
				if(n.isElement() && n.asElement().hasQName(getQName())) {
					i++;
					}
				}
			if(i>=0) indexstr="["+(i+1)+"]";
			s= parentNode.getPath()+indexstr+"/"+s;
			}
		return s;
		}
	
	
	@Override
	public void sax(final DefaultHandler handler) throws SAXException {
		
		
		handler.startElement(getNamespaceURI(), getLocalName(), getNodeName(), new SAXAttributes(getAttributes()));
		for(AbstractNode c1=getFirstChild();c1!=null;c1=c1.getNextSibling()) {
			c1.sax(handler);
			}
		handler.endElement(getNamespaceURI(), getLocalName(), getNodeName());
		}
	
	private static class SAXAttributes implements Attributes {
		final NamedNodeMap nm;
		SAXAttributes(final NamedNodeMap nm) {
			this.nm = nm;
			}
		@Override
		public int getLength() {
			return this.nm.getLength();
			}

		public Attr get(int index) {
			return (Attr)this.nm.item(index);
			}
		
		@Override
		public String getURI(int index) {
			return get(index).getNamespaceURI();
			}

		@Override
		public String getLocalName(int index) {
			return get(index).getLocalName();
		}

		@Override
		public String getQName(int index) {
			return get(index).getName();
		}

		@Override
		public String getType(int index) {
			return "CDATA";
		}

		@Override
		public String getValue(int index) {
			return get(index).getValue();
			}

		private int findIndex(final Predicate<Node> filter) {
			for(int i=0;i< this.nm.getLength();i++) {
				if(filter.test(this.nm.item(i))) return i;
				}
			return -1;
		}
		
		@Override
		public int getIndex(final String uri, final String localName) {
			return findIndex(AbstractNode.createNodeMatcher(uri, localName));
		}

		@Override
		public int getIndex(final String qName) {
			return findIndex(N->qName.equals(N.getNodeName()));
		}

		@Override
		public String getType(String uri, String localName) {
			int i = getIndex(uri,localName);
			return  i==-1?null:getType(i);
		}

		@Override
		public String getType(String qName) {
			int i = getIndex(qName);
			return  i==-1?null:getType(i);
		}

		@Override
		public String getValue(String uri, String localName) {
			int i = getIndex(uri,localName);
			return  i==-1?null:getValue(i);
		}

		@Override
		public String getValue(String qName) {
			int i = getIndex(qName);
			return  i==-1?null:getValue(i);
			}
		
		}

	@Override
	public String getAttributeNS(String namespaceURI, String localName) throws DOMException {
		if(this.namedNodeMap==null) return "";
		return this.namedNodeMap.stream().
				filter(createNodeMatcher(namespaceURI, localName)).
				map(N->N.getNodeValue()).findFirst().
				orElse("");
		}

	@Override
	public void setAttributeNS(String namespaceURI, String qualifiedName, String value) throws DOMException {
		final AttrImpl att = getOwnerDocument().createAttributeNS(namespaceURI, qualifiedName);
		att.setValue(value);
		this.setAttributeNode(att);		
		}

	@Override
	public void removeAttributeNS(String namespaceURI, String localName) throws DOMException {
		if(this.namedNodeMap==null) return;
		if( this.namedNodeMap.stream().
				noneMatch(createNodeMatcher(namespaceURI, localName))) return;
				
		AttrImpl old =(AttrImpl) this.namedNodeMap.removeNamedItemNS(namespaceURI, localName);
		old.setOwnerElement(null);
		}

	@Override
	public AttrImpl getAttributeNodeNS(String namespaceURI, String localName) throws DOMException {
		if(this.namedNodeMap==null) return null;
		return this.namedNodeMap.stream().
				filter(createNodeMatcher(namespaceURI, localName)).
				map(N->AttrImpl.class.cast(N)).
				findFirst().
				orElse(null);
		}

	@Override
	public TypeInfo getSchemaTypeInfo() {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void setIdAttribute(String name, boolean isId) throws DOMException {
		setIdAttributeNode(getAttributeNode(name),isId);
	}

	@Override
	public void setIdAttributeNS(String namespaceURI, String localName, boolean isId) throws DOMException {
		setIdAttributeNode(getAttributeNodeNS(namespaceURI,localName),isId);
	}

	@Override
	public void setIdAttributeNode(Attr idAttr, boolean isId) throws DOMException {
		if(idAttr.getOwnerElement()!=this) throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "not owner element");
		AttrImpl.class.cast(this).setId(isId);
		}
	
	
	protected static String assertNonEmpty(final String s) {
		if(StringUtils.isBlank(s)) throw new IllegalArgumentException("empty string");
		return s;
	}
	
	@Override
	public void write(XMLStreamWriter w) throws XMLStreamException {
		
		
		
		if(!hasChildNodes()) {
			if(!hasNamespaceURI()) {
				w.writeEmptyElement(getTagName());
				}
			else if(!hasPrefix()) {
				w.writeEmptyElement(getNamespaceURI(), getLocalName());
				}
			else
				{
				w.writeEmptyElement(getPrefix(),getLocalName(), getNamespaceURI());
				}
			}
		else
			{
			if(!hasNamespaceURI()) {
				w.writeStartElement(getTagName());
				}
			else if(!hasPrefix()) {
				w.writeStartElement(getNamespaceURI(), getLocalName());
				}
			else
				{
				w.writeStartElement(getPrefix(),getLocalName(), getNamespaceURI());
				}
			}
				
		if(hasAttributes()) {
			for(Node a: getAttributes()) {
				AttrImpl.class.cast(a).write(w);
				}
			}
		
		if(hasChildNodes())
			{
			for(AbstractNode c= getFirstChild(); c!=null; c=c.getNextSibling()) {
				c.write(w);
				}
			w.writeEndElement();
			}
		
		}

	@Override
	public void write(XMLEventWriter w, XMLEventFactory factory) throws XMLStreamException {
		throw new UnsupportedOperationException();
		}

	@Override
	public AbstractNode removeChild(Node oldChild) throws DOMException {
		throw new UnsupportedOperationException();
		}
	}
