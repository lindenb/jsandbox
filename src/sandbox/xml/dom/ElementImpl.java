package sandbox.xml.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class Element extends NamedNode {
	/* private */ List<Attr> _atts = null;
	/* no private */ final List<Node> _children = new ArrayList<>();
	public Element(final Element parent,final QName qName) {
		super(parent, qName);
		}

	public Element(final QName qName) {
		this(null, qName);
		}
	
	
	@Override
	public final boolean isElement() {
		return true;
		}
	
	@Override
	public boolean isAttribute() {
		return false;
		}
	
	public boolean hasAttributes() {
		return _atts!=null && !_atts.isEmpty();
		}
	public List<Attr> getAttributes() {
		if(!hasAttributes()) return Collections.emptyList();
		return Collections.unmodifiableList(this._atts);
		}
	public boolean hasAttribute(String s) {
		if(!hasAttributes()) return false;
		return getAttributes().stream().anyMatch(N->N.getNodeName().equals(s));
		}
	
	
	public Optional<String> getAttribute(final QName qName) {
		if(!hasAttributes()) return Optional.empty();
		return getAttributes().stream().
				filter(N->hasQName(qName)).
				map(T->T.getValue()).findFirst();
		}
	
	public Optional<String> getAttribute(String s) {
		if(!hasAttributes()) return Optional.empty();
		return getAttributes().stream().
				filter(N->N.getNodeName().equals(s)).
				map(T->T.getValue()).findFirst();
		}
	
	public String getAttribute(String s,String def) {
		return getAttribute(s).orElse(def);
		}

	public Attr getAttributeNodeNS(final String namespaceURI,final String localName) {
		if(!hasAttributes()) return  null;
		return getAttributes().stream().
				filter(N->N.isA(namespaceURI,localName)).
				findFirst().orElse(null);
		}
	public Optional<String> getAttributeNS(final String namespaceURI,final String localName) {
		final Attr att = getAttributeNodeNS(namespaceURI,localName);
		if(att==null) return Optional.empty();
		return Optional.of(att.getValue());
		}
	
	@Override
	public String getPath() {
		String s= getNodeName();
		
		if(getParentNode()!=null) {
			String indexstr="";
			int i=-1;
			for(Node n:getParentNode().getChildrenAsList()) {
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
	public List<Node> getChildrenAsList() {
		return Collections.unmodifiableList(this._children);
		}
	
	@Override
	public void sax(final DefaultHandler handler) throws SAXException {
		handler.startElement(getPath(), getPath(), getPath(), null);
		for(Node n:getChildrenAsList()) {
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
		for(Attr att:getAttributes()) {
			//TODO
			}
		doc.createElement(getNodeName());
		for(Node n:getChildrenAsList()) {
			E.appendChild(n.toDOM(doc));
			}
		return E;
		}
	
	
	
	}
