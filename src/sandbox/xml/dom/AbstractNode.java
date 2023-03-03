package sandbox.xml.dom;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.namespace.QName;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.UserDataHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public abstract class AbstractNode implements org.w3c.dom.Node {
	private final DocumentImpl ownerDoc;
	private AbstractNode parentNode;
	private AbstractNode firstChild = null;
	private AbstractNode lastChild = null;
	private AbstractNode nextSibling = null;
	private AbstractNode prevSibling = null;
	private Map<String,Object> userProperties  = null;
	protected AbstractNode(final DocumentImpl ownerDoc) {
		this.ownerDoc = ownerDoc;
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
	public boolean isRoot() {
		return this.parentNode==null;
		}
	@Override
	public final Document getOwnerDocument() {
		return ownerDoc;
		}
	
	@Override
	public Node getPreviousSibling() {
		return this.prevSibling;
		}
	@Override
	public Node getNextSibling() {
		return this.nextSibling;
		}
	@Override
	public Node getFirstChild() {
		return this.firstChild;
		}

	@Override
	public Node getLastChild() {
		return this.lastChild;
		}
	
	@Override
	public NamedNodeMap getAttributes() {
		return null;
		}
	
	@Override
	public String getBaseURI() {
		return null;
		}
	
	@Override
	public String getNodeName() {
		return null;
		}
	
	@Override
	public String getNodeValue() throws DOMException {
		return null;
		}
	
	@Override
	public String getNamespaceURI() {
		return null;
		}
	
	@Override
	public String getLocalName() {
		return null;
		}
	
	

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
	public String getPrefix() {
		return null;
		}
	
	@Override
	public void setTextContent(String textContent) throws DOMException {
		throw new UnsupportedOperationException();
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
	public NodeList getChildNodes() {
		final NodeListImpl L = new NodeListImpl();
		Node c = this.getFirstChild();
		while(c!=null) {
			L.add(c);
			c = c.getNextSibling();
			}
		return L;
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
	public Node appendChild(Node newChild) throws DOMException {
			return insertBefore(newChild,null);
			}
	
	@Override
	public Node removeChild(Node oldChild) throws DOMException {
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
		n.parentNode = null;
		return n;
		}
	
	@Override
	public Node insertBefore(final Node newChild, final Node refChild) throws DOMException {
		if(newChild==null) return null;
		
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
			for(Node n1:L) {
				this.appendChild(n1);
				}
			return newChild;
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
		if(newChild.getParentNode()!=null) {
			newChild.getParentNode().removeChild(newChild);
			}
		final AbstractNode n = AbstractNode.class.cast(newChild);
		if(refChild!=null) {
			final AbstractNode ref = AbstractNode.class.cast(refChild);
			final AbstractNode prev = (AbstractNode)ref.getPreviousSibling();
			if(prev!=null) {
				prev.nextSibling = n;
				}
			else
				{
				this.firstChild = n;
				}
			ref.prevSibling = n;
			n.nextSibling = ref;
			}
		else
			{
			if(firstChild==null) {
				firstChild = n;
				lastChild = n;
				}
			else
				{
				lastChild.nextSibling = n;
				this.lastChild=n;
				}
			}
		n.parentNode = this;
		return newChild;
		}

	@Override
	public void normalize() {
		}
	
	@Override
	public Node replaceChild(Node newChild, Node oldChild) throws DOMException {
		throw new UnsupportedOperationException();
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
	
	public List<AbstractNode> getPrecedingSiblingAsList() {
		if(!hasParent()) return Collections.emptyList();
		final List<AbstractNode> L = new ArrayList<>();
		for(AbstractNode n: getParentNode().getChildrenAsList()) {
			if(n==this) break;
			L.add(n);
			}
		return L;
		}
	public List<AbstractNode> getFollowingSiblingAsList() {
		if(!hasParent()) return Collections.emptyList();
		final List<AbstractNode> L = new ArrayList<>();
		int state=0;
		for(AbstractNode n: getParentNode().getChildrenAsList()) {
			if(n==this) {
				state=1;
				}
			else if(state==1) {
				L.add(n);
				}
			}
		return L;
		}
	
	public abstract List<AbstractNode> getChildrenAsList();
	
	public boolean hasChild() {
		return !getChildrenAsList().isEmpty();
		}
	
	public Stream<AbstractNode> getChildren() {
		return getChildren(N->true);
		}
	public Stream<AbstractNode> getChildren(final Predicate<AbstractNode> filter) {
		return getChildrenAsList().stream().filter(filter);
		}
	public List<AbstractNode> getChildrenAsList(final Predicate<AbstractNode> filter) {
		return getChildren(filter).collect(Collectors.toList());
		}
	

	public Stream<ElementImpl> getElements(final Predicate<ElementImpl> filter) {
		return getChildren(N->N.isElement()).
				map(C->C.asElement()).
				filter(filter)
				;
		}
	public Stream<ElementImpl> getElements() {
		return getElements(E->true);
		}
	
	public List<ElementImpl> getElementsAsList(final Predicate<ElementImpl> filter) {
		return getElements(filter).collect(Collectors.toList());
		}
	public List<ElementImpl> getElementsAsList() {
		return getElementsAsList(E->true);
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
	
	


	
	
	public abstract String getPath();
	
	public abstract void sax(final DefaultHandler handler) throws SAXException;
	
	public void consume(final Consumer<AbstractNode> consumer) {
		consumer.accept(this);
		for(AbstractNode n:getChildrenAsList()) {
			n.consume(consumer);
			}
		}
	
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
	
	/*
	public void print() {
		final TransformerFactory trf = TransformerFactory.newInstance();
		final Transformer tr = trf.newTransformer();
		final org.w3c.dom.Document doc = DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().newDocument();
		tr.transform(new DOMSource(toDOM(doc)), new );
		}*/
 	
	}
