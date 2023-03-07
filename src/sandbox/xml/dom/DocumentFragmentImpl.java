package sandbox.xml.dom;

import java.util.List;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class DocumentFragmentImpl extends AbstractNode implements DocumentFragment{
	DocumentFragmentImpl(DocumentImpl doc) {
		super(doc);
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
