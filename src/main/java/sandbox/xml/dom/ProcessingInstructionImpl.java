package sandbox.xml.dom;

import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Node;
import org.w3c.dom.ProcessingInstruction;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class ProcessingInstructionImpl extends AbstractCharacterNode implements ProcessingInstruction {
	private final String target;
	ProcessingInstructionImpl(DocumentImpl owner,final String target,final String text) {
		super(owner,text);
		this.target = target;
		}
	@Override
	public final String getNodeName() {
		return getTarget(); //see spec
		}
	
	@Override
	public final short getNodeType() {
		return PROCESSING_INSTRUCTION_NODE;
		}

	@Override
	public Node cloneNode(boolean deep) {
		return getOwnerDocument().createProcessingInstruction(getTarget(), getData());
		}

	@Override
	public final String getTarget() {
		return this.target;
		}

	@Override
	public String getPath() {
		String s= getTarget()+"()";
		if(hasParent()) {
			s= parentNode.getPath()+"/"+s;
			}
		return s;
		}

	@Override
	public void sax(DefaultHandler handler) throws SAXException {
		handler.processingInstruction(getTarget(), getData());
	}
	public void write(XMLStreamWriter w) throws XMLStreamException {
		w.writeProcessingInstruction(getTarget(), getData());
		}
	
	@Override
	public void write(XMLEventWriter w, XMLEventFactory factory) throws XMLStreamException {
		w.add(factory.createProcessingInstruction(getTarget(), getData()));
	}
	
}
