package sandbox.xml.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.helpers.DefaultHandler;

public class ElementImpl extends AbstractNamedNode implements org.w3c.dom.Element {
	protected NamedNodeMapImpl namedNodeMap = null;
	public ElementImpl(final DocumentImpl owner,final QName qName) {
		super(owner, qName);
		}

	@Override
	public short getNodeType() {
		return ELEMENT_NODE;
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
				Attr att = (Attr)nm.item(i).cloneNode(deep);
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
	
	@Override
	public boolean hasAttribute(String name) {
		return namedNodeMap!=null && namedNodeMap.getNamedItem(name)!=null;
		}

	@Override
	public boolean hasAttributeNS(String namespaceURI, String localName) throws DOMException {
		return namedNodeMap!=null && namedNodeMap.getNamedItemNS(namespaceURI,localName)!=null;
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
	public Attr getAttributeNode(String name) {
		if(this.namedNodeMap==null) return null;
		return (Attr) this.namedNodeMap.getNamedItem(name);
		}
	
	@Override
	public NamedNodeMapImpl getAttributes() {
		if(namedNodeMap==null) return NamedNodeMapImpl.getEmptyNamedNodeMap();
		return namedNodeMap;
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
		
		if(owner!=null) {
			
			}
		
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
final
	
	
	
	public String getAttribute(String s,String def) {
		return getAttribute(s).orElse(def);
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
			s=getParentNode().getPath()+indexstr+"/"+s;
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

		@Override
		public int getIndex(String uri, String localName) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int getIndex(String qName) {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public String getType(String uri, String localName) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getType(String qName) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getValue(String uri, String localName) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getValue(String qName) {
			// TODO Auto-generated method stub
			return null;
		}
		
		}
	
	}
