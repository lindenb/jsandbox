package sandbox.xml.dom;



import javax.xml.namespace.QName;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.DOMConfiguration;
import org.w3c.dom.DOMException;
import org.w3c.dom.DOMImplementation;
import org.w3c.dom.DocumentType;
import org.w3c.dom.Element;
import org.w3c.dom.EntityReference;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
/**
 * Implementation of  org.w3c.dom.Document
 */
public class DocumentImpl extends AbstractNode implements org.w3c.dom.Document  {
	private String xmlEncoding="UTF-8";
	private String xmlVersion  = "1.0";
	private String documentUri  = null;
	private boolean strictErrorChecking =false;
	private boolean standalone =true;
	public DocumentImpl() {
		super(null);
		}
	@Override
	public final DocumentImpl getOwnerDocument() {
		return this;
		}
	@Override
	public final String getNodeName() {
		return "#document"; // in spec
		}
	
	@Override
	public final String getNodeValue() throws DOMException {
		return null;
		}
	
	@Override
	public final short getNodeType() {
		return org.w3c.dom.Node.DOCUMENT_NODE;
		}
	
	public AttrImpl createAttribute(final QName qname) throws DOMException {
		return new AttrImpl(this,qname);
		}
	
	
	public AttrImpl createAttribute(final QName qname,final Object value) throws DOMException {
		final AttrImpl att= createAttribute(qname);
		att.setValue(String.valueOf(value));
		return att;
		}
	
	@Override
	public AttrImpl createAttribute(final String name) throws DOMException {
		return createAttribute(createQName(null,name));
		}
	@Override
	public AttrImpl createAttributeNS(String namespaceURI, String qualifiedName) throws DOMException {
		return createAttribute(createQName(namespaceURI,qualifiedName));
		}
	@Override
	public CommentImpl createComment(String data) {
		return new CommentImpl(this,data);
		}

	public TextImpl createLiteral(Object data) {
		return createTextNode(String.valueOf(data));
		}
	
	@Override
	public TextImpl createTextNode(String data) {
		return new TextImpl(this,data);
		}
	
	public ElementImpl createElement(QName qName) throws DOMException {
		return new ElementImpl(this,qName);
		}
	
	@Override
	public ElementImpl createElement(String tagName) throws DOMException {
		return createElement(createQName(null,tagName));
		}
	@Override
	public ElementImpl createElementNS(String namespaceURI, String qualifiedName) throws DOMException {
		return createElement(createQName(namespaceURI,qualifiedName));
		}
	
	@Override
	public CDataSectionImpl createCDATASection(String data) throws DOMException {
		return new CDataSectionImpl(this, data);
		}
	@Override
	public DocumentFragmentImpl createDocumentFragment() {
		return new DocumentFragmentImpl(this);
		}
	
	@Override
	public NodeListImpl<AbstractNode> getElementsByTagName(final String tagname) {
		return findAll(N->N.isElement() && tagname.equals(N.getNodeName()));
		}
	
	@Override
	public NodeListImpl<AbstractNode> getElementsByTagNameNS(final String namespaceURI, final String localName) {
		return findAll(N->N.isElement() && localName.equals(N.getLocalName()) && namespaceURI.equals(N.getNamespaceURI()));
		}
	@Override
	public ElementImpl getElementById(final String elementId) {
		return findAll( N->N.isElement() && N.hasAttributes() && 
				N.getAttributes().stream().
				map(T->Attr.class.cast(T)).
				anyMatch(A->A.isId() && elementId.equals(A.getValue()))).
			asElements().
			stream().
			findFirst().
			orElse(null);
		}
	@Override
	public Node cloneNode(boolean deep) {
		final DocumentImpl doc = new DocumentImpl();
		if(deep && hasChildNodes()) {
			for(Node c=getFirstChild();c!=null;c=c.getNextSibling()) {
				doc.appendChild(c.cloneNode(deep));
				}
			}
		return doc;
		}
	@Override
	public DocumentType getDoctype() {
		throw new UnsupportedOperationException();
		}
	@Override
	public DOMImplementation getImplementation() {
		throw new UnsupportedOperationException();
		}
	@Override
	public Element getDocumentElement() {
		for(Node c=getFirstChild();c!=null;c=c.getNextSibling()) {
			if(c.getNodeType()==ELEMENT_NODE) return Element.class.cast(c);
			}
		return null;
		}
	
	@Override
	public ProcessingInstructionImpl createProcessingInstruction(String target, String data) throws DOMException {
		return new ProcessingInstructionImpl(this, target, data);
	}
	@Override
	public EntityReference createEntityReference(String name) throws DOMException {
		throw new UnsupportedOperationException();
	}
	@Override
	public Node importNode(Node source, boolean deep) throws DOMException {

		switch(source.getNodeType()) {
		case TEXT_NODE: return createTextNode(Text.class.cast(source).getData());
		case CDATA_SECTION_NODE: return createCDATASection(CDATASection.class.cast(source).getData());
		case COMMENT_NODE: return createComment(Comment.class.cast(source).getData());
		case PROCESSING_INSTRUCTION_NODE: return createProcessingInstruction(
				ProcessingInstruction.class.cast(source).getTarget(),
				ProcessingInstruction.class.cast(source).getData()
				);
		case ELEMENT_NODE:
			Element E1 = Element.class.cast(source);
			Element E2 = null;
			if(E1.getNamespaceURI()!=null) {
				E2 = createElementNS(E1.getNamespaceURI(), E1.getNodeName());
				}
			else
				{
				E2 = createElement(E1.getNodeName());
				}
			if(E1.hasAttributes()) {
				final NamedNodeMap nnmp = E2.getAttributes();
				for(int i=0;i< nnmp.getLength();i++) {
					Attr att1 = (Attr)nnmp.item(i);
					if(att1.getNamespaceURI()!=null) {
						E2.setAttributeNodeNS((Attr)this.importNode(att1,deep));
						}
					else
						{
						E2.setAttributeNode((Attr)this.importNode(att1,deep));
						}
					}
				}
			if(E1.hasChildNodes() && deep) {
				for(Node c=E1.getFirstChild();c!=null;c=c.getNextSibling()) {
					E2.appendChild(this.importNode(c,deep));
					}
				}
			return E2
			;
		case ATTRIBUTE_NODE:
			final Attr A1 = Attr.class.cast(source);
			Attr A2 = null;
			if(A1.getNamespaceURI()!=null) {
				A2 = createAttributeNS(A1.getNamespaceURI(), A1.getNodeName());
				}
			else
				{
				A2 = createAttribute(A1.getNodeName());
				}
			A2.setValue(A1.getValue());
			return A2;
		default:break;
		}
	throw new UnsupportedOperationException();
		}
	@Override
	public String getInputEncoding() {
		throw new UnsupportedOperationException();
	}
	@Override
	public String getXmlEncoding() {
		return this.xmlEncoding;
	}
	@Override
	public boolean getXmlStandalone() {
		return this.standalone;
	}
	@Override
	public void setXmlStandalone(boolean xmlStandalone) throws DOMException {
		this.standalone = xmlStandalone;
	}
	@Override
	public String getXmlVersion() {
		return this.xmlVersion;
	}
	@Override
	public void setXmlVersion(String xmlVersion) throws DOMException {
		this.xmlVersion = xmlVersion;
		
	}
	@Override
	public boolean getStrictErrorChecking() {
		return this.strictErrorChecking;
	}
	@Override
	public void setStrictErrorChecking(boolean strictErrorChecking) {
		this.strictErrorChecking = strictErrorChecking;
		
	}
	@Override
	public String getDocumentURI() {
		return this.documentUri;
	}
	@Override
	public void setDocumentURI(String documentURI) {
		this.documentUri = documentURI;
	}
	@Override
	public Node adoptNode(Node source) throws DOMException {
		if(source.getNodeType()==DOCUMENT_NODE) return null;
		if(source.getOwnerDocument()==this) return source;
		if(!(source instanceof AbstractNode)) return null;
		return AbstractNode.class.cast(source).adoptDoc(this);
		}
	
	@Override
	public DOMConfiguration getDomConfig() {
		throw new UnsupportedOperationException();
	}
	@Override
	public void normalizeDocument() {
		this.normalize();
		}
	@Override
	public Node renameNode(Node n, String namespaceURI, String qualifiedName) throws DOMException {
		throw new DOMException(DOMException.NOT_SUPPORTED_ERR,"cannot rename node");
	}
	
	
	@Override
	public final String getPath() {
		return "/";
		}
	
	@Override
	public void sax(DefaultHandler handler) throws SAXException {
		handler.startDocument();
		//TODO
		handler.endDocument();
		}
	
	}
