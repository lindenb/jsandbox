package sandbox.xml.dom;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CommentImpl extends AbstractTextNode implements org.w3c.dom.Comment {
	CommentImpl(DocumentImpl owner,final String text) {
		super(owner, text);
		}
	
	@Override
	public final String getNodeName() {
		return "#comment"; // in spec
		}

	@Override
	public boolean equals(final Object obj) {
		if(obj == this) return true;
		if(obj==null ||  !(obj instanceof CommentImpl)) return false;
		final CommentImpl o = CommentImpl.class.cast(obj);
		return o.getData().equals(this.getData());
		}
	
	@Override
	public short getNodeType() {
		return COMMENT_NODE;
		}
	
	@Override
	public Node cloneNode(boolean deep) {
		return getOwnerDocument().createComment(this.getData());
		}
	
	
	@Override
	public void sax(final DefaultHandler handler) throws SAXException {
	}

	@Override
	public void write(XMLStreamWriter w) throws XMLStreamException {
		w.writeComment(getData());
		}
	@Override
	public void write(XMLEventWriter w, XMLEventFactory factory) throws XMLStreamException {
		w.add(factory.createComment(getData()));
		}
	
	@Override
	public String getPath() {
		String s= "comment()";
		if(getParentNode()!=null) {
			s=getParentNode().getPath()+"/"+s;
			}
		return s;
		}
	
	}
