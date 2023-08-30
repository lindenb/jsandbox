package sandbox.tools.rdftemplate;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.jena.expr.RDFExprParser;
import sandbox.xml.DefaultNamespaceContext;
import sandbox.xml.XMLException;
import sandbox.xml.XmlUtils;

public class RDFTemplate extends Launcher {
private static final Logger LOG = Logger.builder(RDFTemplate.class).build();
private static final String NS="https://github.com/lindenb/jsandboox/rdftemplate#";
@Parameter(names= {"-o","--out"},description=OUTPUT_OR_STANDOUT)
private Path outPath = null;

private XPath xpath;

private static class State {
	State parent=null;
	String encoding;
	Document dom;
	Model model;
	XMLStreamWriter w;
	Object current;
	State() {
		
	}
	State(State cp) {
		this.parent=cp;
		this.encoding = cp.encoding;
		this.dom = cp.dom;
		this.model = cp.model;
		this.w= cp.w;
		this.current = cp.current;
		}
	public State setCurrent(Object current) {
		this.current = current;
		return this;
		}
	}

void process(State state,Node node) throws XMLStreamException,XPathException {
	switch(node.getNodeType()) {
		case Node.DOCUMENT_NODE: {
			final Document doc = (Document)node;
			final Element root = doc.getDocumentElement();
			if(root==null) throw new XMLException("no root");
			if(XmlUtils.isA(root, NS, "template")) throw new XMLException(root,"not a <template>");
			final NodeList L=(NodeList)this.xpath.evaluate("t:main", root,XPathConstants.NODESET);
			if(L.getLength()!=1) throw new XMLException(root,"expected one <main> but got "+L.getLength());
			final Element mainE = (Element)L.item(0);
			state.w.writeStartDocument(state.encoding,"1.0");
			process(new State(state),mainE);
			state.w.writeEndDocument();
			break;
			}
		case Node.ELEMENT_NODE:
			{
			Element E = (Element)node;
			if(NS.equals(E.getNamespaceURI())) {
				String lclName  = E.getLocalName();
				if(lclName.equals("comment")) {
					
					}
				else if(lclName.equals("for-each")) {
					Attr att = E.getAttributeNode("select");
					if(att==null) throw new XMLStreamException("@select missing "+XmlUtils.getNodePath(E));
					ExtendedIterator<?> iter=RDFExprParser.iterator(new RDFExprParser.Context().setModel(state.model).setCurrent(state.current), att.getValue());
					while(iter.hasNext()) {
						final Object current = iter.next();
						for(Node c=E.getFirstChild();c!=null;c=c.getNextSibling()) {
							process(new State(state).setCurrent(current),c);
							}
						}
					iter.close();
					}
				else 
					{
					throw new XMLStreamException("unknown element "+lclName+". "+XmlUtils.getNodePath(E));
					}
				}
			else
				{
				if(E.hasChildNodes()) {
					XmlUtils.writeStartElement(state.w,E);
					}
				else
					{
					XmlUtils.writeEmptyElement(state.w,E);
					}
				final NodeList L=(NodeList)this.xpath.evaluate("t:attribute", E,XPathConstants.NODESET);
				for(int i=0;i< L.getLength();i++) {
					final Element A = (Element)L.item(i);
					
					}
				
				if(E.hasAttributes()) {
					final NamedNodeMap atts=E.getAttributes();
					for(int i=0;i< atts.getLength();i++) {
						if(NS.equals(atts.item(i).getNamespaceURI())) continue;
						XmlUtils.writeAttribute(state.w,(Attr)atts.item(i));
						}
					}
				
				if(E.hasChildNodes()) {
					for(Node c=E.getFirstChild();c!=null;c=c.getNextSibling()) {
						process(new State(state),c);
						}
					state.w.writeEndElement();
					}
				}
			}
		case Node.COMMENT_NODE:{
			break;
			}
		case Node.TEXT_NODE:{
			state.w.writeCharacters(Text.class.cast(node).getData());
			break;
			}
		case Node.PROCESSING_INSTRUCTION_NODE:{
			state.w.writeProcessingInstruction(ProcessingInstruction.class.cast(node).getData());
			break;
			}
		}
	}

@Override
public int doWork(List<String> args) {
	try {
		if(args.size()==2) {
			
			final XPathFactory xpf = XPathFactory.newInstance();
			this.xpath = xpf.newXPath();
			this.xpath.setNamespaceContext(new DefaultNamespaceContext().put("t", NS));
			
			final State state = new State();
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			state.dom  = db.parse(args.get(0));
			state.model = ModelFactory.createDefaultModel() ;
			state.model.read(args.get(1),"RDF/XML");
			
			XMLOutputFactory xof = XMLOutputFactory.newFactory();
			try(OutputStream out  = super.openPathAsOuputStream(outPath)) {
				state.encoding = state.dom.getInputEncoding();
				state.encoding=StringUtils.isBlank(state.encoding)?"UTF-8":state.encoding;
				final XMLStreamWriter w = xof.createXMLStreamWriter(out,state.encoding);
				process(state,state.dom);
				w.close();
				w.flush();
				out.flush();
				}
			return 0;
			}
		else
			{
			LOG.equals("illegal number of arguments");
			return -1;
			}
		}
	catch(Throwable err) {
		LOG.error(err);
		return -1;
		}
	}
public static void main(String[] args) {
	new RDFTemplate().instanceMainWithExit(args);
	}
}
