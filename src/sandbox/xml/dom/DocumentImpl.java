package sandbox.xml.dom;

import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMException;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class DocumentImpl extends AbstractNode implements org.w3c.dom.Document  {
	public DocumentImpl() {
		super(null);
		}
	@Override
	public final short getNodeType() {
		return org.w3c.dom.Node.DOCUMENT_NODE;
		}
	@Override
	public Attr createAttribute(String name) throws DOMException {
		return new AttrImpl(this,createQName(null,name));
		}
	@Override
	public Attr createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException {
		return new AttrImpl(this,createQName(namespaceURI,qualifiedName));
		}
	@Override
	public Comment createComment(String data) {
		return new CommentImpl(this,data);
		}
	@Override
	public Text createTextNode(String data) {
		return new TextImpl(this,data);
		}
	@Override
	public Element createElement(String tagName) throws DOMException {
		return new ElementImpl(this,createQName(null,tagName));
		}
	@Override
	public Element createElementNS(String namespaceURI, String qualifiedName) throws DOMException {
		return new ElementImpl(this,createQName(namespaceURI,qualifiedName));
		}
	@Override
	public CDATASection createCDATASection(String data) throws DOMException {
		return new CDataSectionImpl(this, data);
		}
	@Override
	public DocumentFragment createDocumentFragment() {
		return new DocumentFragmentImpl(this);
		}
	}
