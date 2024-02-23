package sandbox.xml.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.DOMException;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import sandbox.StringUtils;

public abstract class AbstractNode implements org.w3c.dom.Node {
	private /*final no, node can be 'adopted' */ DocumentImpl ownerDoc;
	private AbstractNode parentNode = null;
	private AbstractNode firstChild = null;
	private AbstractNode lastChild = null;
	private AbstractNode nextSibling = null;
	private AbstractNode prevSibling = null;
	private Map<String,Object> userProperties  = null;
	protected AbstractNode(final DocumentImpl ownerDoc) {
		this.ownerDoc = ownerDoc;
		}
	
	/** called by Document.adoptNode */
	protected AbstractNode adoptDoc(final DocumentImpl doc) {
		this.ownerDoc=doc;
		for(AbstractNode c=getFirstChild();c!=null;c=c.getNextSibling()) {
			c.adoptDoc(doc);
			}
		return this;
		}
	
	/** return true for text, cdata, comment */
	public boolean isTerminal() {
		switch(this.getNodeType()) {
			case TEXT_NODE: return true;
			case COMMENT_NODE: return true;
			case CDATA_SECTION_NODE: return true;
			case ATTRIBUTE_NODE: return true;
			case ENTITY_REFERENCE_NODE: return true;
			case PROCESSING_INSTRUCTION_NODE: return true;
			default:return false;
			}
		}

	public boolean hasParent() {
		return getParentNode()!=null;
		}
	
	@Override
	public AbstractNode getParentNode() {
		return this.parentNode;
		}
	
	/** remove this node from it's parent if any
	 * @return this
	 */
	public AbstractNode unlink() {
		if(getParentNode()!=null) getParentNode().removeChild(this);
		return this;
		}
	/** 
	 * remove all child nodes
	 * @return this
	 */
	public AbstractNode removeAllChildNodes() {
		if(hasChildNodes()) {
			AbstractNode n1 = this.firstChild;
			while(n1!=null) {
				final AbstractNode n2 = n1.getNextSibling();
				n1.nextSibling = null;
				n1.prevSibling = null;
				n1.parentNode = null;
				n1 = n2;
				}
			this.firstChild = null;
			this.lastChild = null;
			}
		return this;
		}
	
	public boolean isRoot() {
		return this.parentNode==null;
		}
	@Override
	public /*final$*/ DocumentImpl getOwnerDocument() {
		return ownerDoc;
		}
	
	@Override
	public AbstractNode getPreviousSibling() {
		return this.prevSibling;
		}
	@Override
	public AbstractNode getNextSibling() {
		return this.nextSibling;
		}
	@Override
	public AbstractNode getFirstChild() {
		return this.firstChild;
		}

	@Override
	public AbstractNode getLastChild() {
		return this.lastChild;
		}
	
	@Override
	public NamedNodeMapImpl getAttributes() {
		return NamedNodeMapImpl.getEmptyNamedNodeMap();
		}
	
	@Override
	public String getBaseURI() {
		return null;
		}
	
	public QName getQName() {
		return null;
		}
	
	public boolean hasQName(final QName qName) {
		final QName qN = this.getQName();
		return qN==null?false:qN.equals(qName);
		}
	
	public boolean isA(String namespaceURI, String localName) {
		return hasNamespaceURI(namespaceURI) && hasLocalName(localName);
		}
	
	public boolean hasNamespaceURI() {
		return !getNamespaceURI().equals(XMLConstants.NULL_NS_URI);
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
	
	@Override
	public String getNamespaceURI() {
		final QName qN = this.getQName();
		return qN==null?XMLConstants.NULL_NS_URI:qN.getNamespaceURI();
		}
	@Override
	public String getLocalName() {
		final QName qN = this.getQName();
		return qN==null?null:qN.getLocalPart();
		}
	
	
	public boolean hasPrefix() {
		return !StringUtils.isBlank(getPrefix());
		}

	
	@Override
	public String getPrefix() {
		final QName qN = this.getQName();
		return qN==null?XMLConstants.DEFAULT_NS_PREFIX:qN.getPrefix();
		}
	
	public String getNodeName() {
		return StringUtils.isBlank(getPrefix())?getLocalName():getPrefix()+":"+getLocalName();
		}
	
	@Override
	public abstract String getNodeValue() throws DOMException;
	

	public Object getFeature(String feature, String version) {
		throw new UnsupportedOperationException();
		}
	
	@Override
	public void setNodeValue(String nodeValue) throws DOMException {
		throw new UnsupportedOperationException();
		}

	@Override
	public void setPrefix(String prefix) throws DOMException {
		throw new UnsupportedOperationException();
		}
	
	
	@Override
	public void setTextContent(String textContent) throws DOMException {
		throw new DOMException(DOMException.INVALID_MODIFICATION_ERR, "Cannot set text");
		}
	
	@Override
	public boolean isDefaultNamespace(String namespaceURI) {
		throw new UnsupportedOperationException();
		}
	
	@Override
	public boolean isEqualNode(Node arg) {
		throw new UnsupportedOperationException();
		}
	
	@Override
	public boolean isSameNode(Node arg) {
		throw new UnsupportedOperationException();
		}
	@Override
	public boolean isSupported(String feature, String version) {
		return false;
		}
	
	@Override
	public boolean hasAttributes() {
		final NamedNodeMap m=getAttributes();
		return m!=null && m.getLength()>0;
		}
	
	
	@Override
	public NodeListImpl<AbstractNode> getChildNodes() {
		if(!hasChildNodes()) {
			return NodeListImpl.emptyNodeList();
			}
		return getChildNodes(N->true);
		}
	
	@Override
	public boolean hasChildNodes() {
		return !isTerminal() && getFirstChild()!=null;
		}
	@Override
	public Object setUserData(String key, Object data, UserDataHandler handler) {
		if(this.userProperties==null) {
			this.userProperties = new HashMap<>();
			}
		return this.userProperties.put(key, data);
		}
	
	
	@Override
	public Object getUserData(String key) {
		if(this.userProperties==null) return null;
		return this.userProperties.get(key);
		}
	
	
	@Override
	public String lookupNamespaceURI(String prefix) {
		throw new UnsupportedOperationException();
		}
	
	@Override
	public String lookupPrefix(String namespaceURI) {
		throw new UnsupportedOperationException();
		}
	
	@Override
	public AbstractNode appendChild(Node newChild) throws DOMException {
		if(newChild.getOwnerDocument()!=this.getOwnerDocument()) {
			throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, "Not the same document");
			}
		if(newChild==this) {
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "this==child");
			}
		final List<AbstractNode> nodes;
		if(newChild.getNodeType()==DOCUMENT_FRAGMENT_NODE) {
			nodes = new ArrayList<>(DocumentFragmentImpl.class.cast(newChild).getChildNodes());
			}
		else
			{
			nodes = Collections.singletonList(AbstractNode.class.cast(newChild));
			}
		AbstractNode last = null;
		for(final AbstractNode n : nodes ) {
			n.unlink();
			if(firstChild==null) {
				this.firstChild=n;
				this.lastChild=n;
				}
			else
				{
				this.lastChild.nextSibling = n;
				n.prevSibling  = this.lastChild;
				this.lastChild = n;
				}
			n.parentNode = this;
			last = n;
			}
		return last;
		}
	
	@Override
	public AbstractNode removeChild(Node oldChild) throws DOMException {
		if(oldChild==null) return null;
		if(oldChild.getParentNode()!=this) {
			throw new DOMException(DOMException.NOT_FOUND_ERR, "Not child of this node");
			}
		final AbstractNode n = AbstractNode.class.cast(oldChild);
		
		final AbstractNode prev = (AbstractNode)n.getPreviousSibling();
		final AbstractNode next = (AbstractNode)n.getNextSibling();
		if(prev!=null) prev.nextSibling = next;
		if(next!=null) next.nextSibling = prev;
		if(this.firstChild==n) this.firstChild=next;
		if(this.lastChild==n) this.lastChild=prev;
		n.prevSibling = null;
		n.nextSibling = null;
		n.parentNode = null;
		return n;
		}
	
	@Override
	public AbstractNode insertBefore(final Node newChild, final Node refChild) throws DOMException {
		if(newChild==null) return null;
		if(refChild==null) {
			return appendChild(newChild);
			}
		if(refChild!=null && refChild.getParentNode()!=this) {
			throw new DOMException(DOMException.NOT_FOUND_ERR, "refchild is not child of this node");
			}
		if(newChild==refChild) {
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "newChild==refChild");
			}
		if(newChild.getNodeType()==Node.DOCUMENT_FRAGMENT_NODE) {
			final List<Node> L = new ArrayList<>();
			for(Node n1= newChild.getFirstChild();n1!=null;n1=n1.getNextSibling()) {
				L.add(n1);
				}
			AbstractNode last=null;
			for(Node n1:L) {
				last =  this.insertBefore(n1,refChild);
				}
			return last;
			}
		
		if(newChild.getOwnerDocument()!=this.getOwnerDocument()) {
			throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, "Not the same document");
			}
		if(newChild.getNodeType()==DOCUMENT_NODE) {
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot doc to node");
			}
		if(newChild==this) {
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot add to itself");
			}
		if(isTerminal()) {
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot append child to termnal");
			}
		if(!(newChild instanceof AbstractNode)) {
			throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Cannot append child that is not instance of AbstractNode");
			}
		final AbstractNode n = AbstractNode.class.cast(newChild);
		final AbstractNode next = AbstractNode.class.cast(newChild);
		n.unlink();
		if(this.firstChild==next) firstChild=n;
		n.nextSibling = next;
		n.prevSibling = next.prevSibling;
		next.prevSibling = n;
		n.parentNode = this;
		return n;
		}

	@Override
	public void normalize() {
		if(!hasChildNodes()) return;
		AbstractNode c1= getFirstChild();
		while(c1!=null) {
			AbstractNode c2 = c1.getNextSibling();
			if(c1.isText() && c2!=null && c2.isText()) {
				Text.class.cast(c1).appendData(Text.class.cast(c2).getData());
				c2.unlink();
				}
			else
				{
				c1.normalize();
				c1=c1.getNextSibling();
				}
			}
		}
	
	@Override
	public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
		if(newChild==null) return null;
		
		if(oldChild.getParentNode()!=this) {
			throw new DOMException(DOMException.NOT_FOUND_ERR, "refchild is not child of this node");
			}
		if(newChild==oldChild) {
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "newChild==refChild");
			}
		final List<Node> L;
		if(newChild.getNodeType()==Node.DOCUMENT_FRAGMENT_NODE) {
			L = new ArrayList<>();
			for(Node n1= newChild.getFirstChild();n1!=null;n1=n1.getNextSibling()) {
				L.add(n1);
				}
			}
		else
			{
			L = Collections.singletonList(newChild);
			}
		
		if(newChild.getOwnerDocument()!=this.getOwnerDocument()) {
			throw new DOMException(DOMException.WRONG_DOCUMENT_ERR, "Not the same document");
			}
		if(newChild.getNodeType()==DOCUMENT_NODE) {
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot doc to node");
			}
		if(newChild==this) {
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot add to itself");
			}
		if(isTerminal()) {
			throw new DOMException(DOMException.HIERARCHY_REQUEST_ERR, "Cannot append child to termnal");
			}
		if(!(newChild instanceof AbstractNode)) {
			throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Cannot append child that is not instance of AbstractNode");
			}
		
		AbstractNode prev= (AbstractNode)oldChild.getPreviousSibling();
		AbstractNode next= (AbstractNode)oldChild.getNextSibling();

		if(newChild.getNodeType()==DOCUMENT_FRAGMENT_NODE) {
			
			}
		else
			{
			}
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR, "Cannot append child that is not instance of AbstractNode");

		
		}
	
	@Override
	public short compareDocumentPosition(Node other) throws DOMException {
		throw new UnsupportedOperationException();
		}
	
	public AbstractNode getRoot() {
		org.w3c.dom.Node r=this;
		for(;;) {
			final org.w3c.dom.Node p = r.getParentNode();
			if(p==null) return AbstractNode.class.cast(r);
			r = p;
			}
		}
	
	public int getChildCount() {
		return getChildCount(N->true);
		}

	public int getChildCount(Predicate<AbstractNode> predicate) {
		if(!hasChildNodes()) {
			return 0;
			}
		else
			{
			int n = 0;
			for(AbstractNode c = this.getFirstChild();c!=null;c=c.getNextSibling()) {
				if(predicate==null || predicate.test(c)) ++n;
				}
			return n;
			}
		}
	
	
	public NodeListImpl<AbstractNode> getChildNodes(Predicate<AbstractNode> predicate) {
		if(!hasChildNodes()) {
			return NodeListImpl.emptyNodeList();
			}
		else
			{
			final List<AbstractNode> L= new ArrayList<AbstractNode>();
			for(AbstractNode c = this.getFirstChild();c!=null;c=c.getNextSibling()) {
				if(predicate==null || predicate.test(c)) L.add(c);
				}
			return new NodeListImpl<>(L);
			}
		}
	
	
	public final boolean isText() {
		return getNodeType() == org.w3c.dom.Node.TEXT_NODE;
		}
	public final boolean isElement() {
		return getNodeType() == org.w3c.dom.Node.ELEMENT_NODE;
		}
	public final boolean isComment() {
		return getNodeType() == org.w3c.dom.Node.COMMENT_NODE;
		}
	public final boolean isDocument() {
		return getNodeType() == org.w3c.dom.Node.DOCUMENT_NODE;
		}
	public final boolean isAttribute() {
		return getNodeType() == org.w3c.dom.Node.ATTRIBUTE_NODE;
		}
	
	public ElementImpl asElement() {
		if(!isElement()) throw new IllegalStateException("not an element");
		return ElementImpl.class.cast(this);
		}
	public TextImpl asText() {
		if(!isText()) throw new IllegalStateException("not a text");
		return TextImpl.class.cast(this);
		}
	
	@Override
	public String getTextContent() throws DOMException {
		final StringBuilder sb=new StringBuilder();
		return sb.toString();
		}

	private static List<AbstractNode> elements(AbstractNode  root,Predicate<AbstractNode> predicate,final List<AbstractNode> array) {
		if(predicate.test(root)) array.add(root);
		if(root.hasChildNodes()) {
			for(Node c=root.getFirstChild();c!=null;c=c.getNextSibling()) {
				elements(AbstractNode.class.cast(c),predicate,array);
				}
			}
		return array;
		}
	
	public  NodeListImpl<AbstractNode> findAll(final Predicate<AbstractNode> predicate) {
		return new NodeListImpl<>(elements(this,predicate,new ArrayList<>()));
		}
	
	
	
	
	public abstract String getPath();
	
	public abstract void sax(final DefaultHandler handler) throws SAXException;
	
	
	
	public static QName createQName(String namespaceUri,String qName) {
		if(namespaceUri==null) {
			return new QName(qName);
			}
		else
			{
			final int i = qName.indexOf(':');
			if(i!=-1)
				{
				return new QName(
					namespaceUri,
					qName.substring(0,i),
					qName.substring(i+1)
					);
				}
			else
				{
				return new QName(
					namespaceUri,
					qName
					);
				}
			}
		}
	static Predicate<Node> createNodeMatcher(String name) {
		if(StringUtils.isBlank(name)) throw new IllegalArgumentException("empty name");
		return N->name.equals(N.getNodeName());
		}
	
	static Predicate<Node> createNodeMatcher(final String namespaceUri,final String localName) {
		if(StringUtils.isBlank(namespaceUri)) throw new IllegalArgumentException("empty NS");
		if(StringUtils.isBlank(localName)) throw new IllegalArgumentException("empty localName");
		return N->namespaceUri.equals(N.getNamespaceURI()) &&
				localName.equals(N.getLocalName());
		}
	static Predicate<Node> createNodeMatcher(final QName qName) {
		if(qName==null) throw new IllegalArgumentException("qName is null");
		if(!StringUtils.isBlank(qName.getNamespaceURI())) {
			return createNodeMatcher(qName.getNamespaceURI(),qName.getLocalPart());
			}
		else
			{
			return createNodeMatcher(qName.getLocalPart());
			}
		}
	static QName toQName(final org.w3c.dom.Node n) {
		if(StringUtils.isBlank(n.getNamespaceURI())) {
			return new QName(n.getLocalName());
			}
		else if(StringUtils.isBlank(n.getPrefix())) {
			return new QName(n.getNamespaceURI(),n.getLocalName());
			}
		else
			{
			return new QName(n.getNamespaceURI(), n.getLocalName(), n.getPrefix());
			}
		}

	public abstract void write(XMLStreamWriter w) throws XMLStreamException;
	
	/*
	public void print() {
		final TransformerFactory trf = TransformerFactory.newInstance();
		final Transformer tr = trf.newTransformer();
		final org.w3c.dom.Document doc = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().newDocument();
		tr.transform(new DOMSource(toDOM(doc)), new );
		}*/
 	
	}
