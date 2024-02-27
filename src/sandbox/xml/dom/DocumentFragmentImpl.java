package sandbox.xml.dom;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class DocumentFragmentImpl extends AbstractNode implements NodeList,DocumentFragment{
	private AbstractNode parentNode;
	private AbstractNode firstChild;
	DocumentFragmentImpl(DocumentImpl doc) {
		super(doc);
		}
	@Override
	protected void setParentNode(AbstractNode p) {
		this.parentNode=p;
		}
	
	
	@Override public AbstractNode getFirstChild() { return this.firstChild; }
	
	public boolean isEmpty() {
		return !hasChildNodes();
		}
	
	@Override
	public AbstractNode item(int index) {
		int n=0;
		for(AbstractNode c=this.getFirstChild();c!=null;c=c.getNextSibling()) {
			if(n==index) return c;
			}
		throw new ArrayIndexOutOfBoundsException(index);
		}
	
	@Override
	public final int getLength() {
		return getChildCount();
		}
	
	@Override
	public final short getNodeType() {
		return DocumentFragmentImpl.DOCUMENT_FRAGMENT_NODE;
		}
	
	@Override
	public Node cloneNode(boolean deep) {
		final DocumentFragment cp = getOwnerDocument().createDocumentFragment();
		if(deep) {
			for(Node c=this.getFirstChild();c!=null;c=c.getNextSibling()) {
				cp.appendChild(c.cloneNode(deep));
				}
			}
		return cp;
		}
	@Override
	public void sax(DefaultHandler handler) throws SAXException {
		throw new SAXException("Cannot run sax handler on DocumentFragment ");
		}
	
	@Override
	public void write(XMLStreamWriter w) throws XMLStreamException {
		throw new XMLStreamException("Cannot run XMLStreamWriter on DocumentFragment ");
		}
	
	
	
	@Override
	public final String getPath() {
		return "document-fragment()";
		}
	
	@Override
	public final AbstractNode getParentNode() {
		return null;
		}
	@Override
	public final boolean hasAttributes() {
		return false;
		}
	@Override
	public final String getNodeValue() throws DOMException {
		return null;
		}
	}
