package sandbox.xml.dom;


import javax.xml.namespace.QName;

import sandbox.StringUtils;

public abstract class NamedNode extends Node {
	private final QName qName;
	protected NamedNode(final Element parent,final QName qName) {
		super(parent);
		this.qName = qName;
		}
	
	@Override
	public int hashCode() {
		return this.getQName().hashCode();
		}
	
	@Override
	public final boolean isText() {
		return false;
		}
	
	public boolean isA(String namespaceURI, String localName) {
		return hasNamespaceURI(namespaceURI) && hasLocalName(localName);
		}
	
	public boolean hasNamespaceURI() {
		return !StringUtils.isBlank(getQName().getNamespaceURI());
		}
	
	public boolean hasNamespaceURI(final Element other) {
		return other!=null && hasNamespaceURI(other.getNamespaceURI());
		}
	
	public boolean hasNamespaceURI(final String ns) {
		return ns.equals(getNamespaceURI());
		}
	public boolean hasLocalName(final String ns) {
		return ns.equals(getLocalName());
		}
	public boolean hasNodeName(final String ns) {
		return ns.equals(getNodeName());
		}
	public QName getQName() {
		return this.qName;
		}
	public boolean hasQName(final QName qName) {
		return getQName().equals(qName);
		}
	
	public String getNamespaceURI() {
		return getQName().getNamespaceURI();
		}
	public String getLocalName() {
		return getQName().getLocalPart();
		}
	
	public String getPrefix() {
		return getQName().getPrefix();
	}
	
	public String getNodeName() {
		return StringUtils.isBlank(getPrefix())?getLocalName():getPrefix()+":"+getLocalName();
		}
	
	
}
