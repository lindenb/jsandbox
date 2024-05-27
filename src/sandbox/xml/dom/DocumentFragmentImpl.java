package sandbox.xml.dom;


import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class DocumentFragmentImpl extends AbstractNode implements NodeList,DocumentFragment{

	DocumentFragmentImpl(DocumentImpl doc) {
		super(doc);
		}

	
	@Override
	public final AbstractNode getNextSibling() {
		return null;
		}
	@Override
	public final AbstractNode getPreviousSibling() {
		return null;
		}
	
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
		for(AbstractNode n=getFirstChild();n!=null;n=n.getNextSibling()) {
			n.write(w);
			}	
		}
	
	@Override
	public void write(XMLEventWriter w, XMLEventFactory factory) throws XMLStreamException {
		for(AbstractNode n=getFirstChild();n!=null;n=n.getNextSibling()) {
			n.write(w, factory);
			}
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
	@Override
	public AbstractNode removeChild(Node oldChild) throws DOMException {
		throw new UnsupportedOperationException();
		}
	}
