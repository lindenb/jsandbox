package sandbox.xml.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ElementImpl extends NamedNode implements org.w3c.dom.Element {
	protected NamedNodeMapImpl namedNodeMap = null;
	/* no private */ final List<AbstractNode> _children = new ArrayList<>();
	public ElementImpl(final DocumentImpl owner,final QName qName) {
		super(owner, qName);
		}

	@Override
	public String getAttribute(String name) {
		final Attr  n=getAttributeNode(name);
		return n==null?"":n.getValue();
		}
	@Override
	public Attr getAttributeNode(String name) {
		if(this.namedNodeMap==null) return null;
		return (Attr) this.namedNodeMap.getNamedItem(name);
		}
	
	@Override
	public NamedNodeMap getAttributes() {
		if(namedNodeMap==null) namedNodeMap=new NamedNodeMapImpl();
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

	
	
	
	public String getAttribute(String s,String def) {
		return getAttribute(s).orElse(def);
		}

	public AttrImpl getAttributeNodeNS(final String namespaceURI,final String localName) {
		if(!hasAttributes()) return  null;
		return getAttributes().stream().
				filter(N->N.isA(namespaceURI,localName)).
				findFirst().orElse(null);
		}
	public Optional<String> getAttributeNS(final String namespaceURI,final String localName) {
		final AttrImpl att = getAttributeNodeNS(namespaceURI,localName);
		if(att==null) return Optional.empty();
		return Optional.of(att.getValue());
		}
	
	@Override
	public String getPath() {
		String s= getNodeName();
		
		if(getParentNode()!=null) {
			String indexstr="";
			int i=-1;
			for(AbstractNode n:getParentNode().getChildrenAsList()) {
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
	public List<AbstractNode> getChildrenAsList() {
		return Collections.unmodifiableList(this._children);
		}
	
	@Override
	public void sax(final DefaultHandler handler) throws SAXException {
		handler.startElement(getPath(), getPath(), getPath(), null);
		for(AbstractNode n:getChildrenAsList()) {
			n.sax(handler);
			}
		handler.endElement(getPath(), getPath(), getPath());
		}

	@Override
	public org.w3c.dom.Node toDOM(org.w3c.dom.Document doc) {
		org.w3c.dom.Element E;
		if(hasNamespaceURI()) {
			E = doc.createElementNS(getNamespaceURI(), getNodeName());
			}
		else
			{
			E = doc.createElement(getNodeName());
			}
		for(AttrImpl att:getAttributes()) {
			//TODO
			}
		doc.createElement(getNodeName());
		for(AbstractNode n:getChildrenAsList()) {
			E.appendChild(n.toDOM(doc));
			}
		return E;
		}
	
	
	
	}
