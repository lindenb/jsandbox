package sandbox.xml.dom;


import javax.xml.namespace.QName;

import sandbox.StringUtils;

public abstract class NamedNode extends AbstractNode {
	private final String name;
	private final String qName;
	private final String namespareUri;
	protected NamedNode(final DocumentImpl owner,final String name) {
		super(owner);
		this.name = name;
		this.qName = null;
		this.namespareUri= null;
		}
	protected NamedNode(final DocumentImpl owner,final String namespaceUri,final String qName) {
		super(owner);
		this.name = null;
		this.qName = qName;
		this.namespareUri= namespaceUri;
		}
	
	@Override
	public int hashCode() {
		return this.getQName().hashCode();
		}
	
	public QName getQName() {
		return null;
		}
	
	
	public boolean isA(String namespaceURI, String localName) {
		return hasNamespaceURI(namespaceURI) && hasLocalName(localName);
		}
	
	public boolean hasNamespaceURI() {
		return !StringUtils.isBlank(getQName().getNamespaceURI());
		}
	
	public boolean hasNamespaceURI(final ElementImpl other) {
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
