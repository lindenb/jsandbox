package sandbox.tools.rdftemplate;

import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathException;
import javax.xml.xpath.XPathFactory;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.util.iterator.ExtendedIterator;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.io.IOUtils;
import sandbox.jena.expr.RDFExprParser;
import sandbox.xml.DefaultNamespaceContext;
import sandbox.xml.XMLException;
import sandbox.xml.XmlUtils;

public class RDFTemplate extends Launcher {
private static final Logger LOG = Logger.builder(RDFTemplate.class).build();
private static final String NS="https://github.com/lindenb/jsandbox/rdftemplate#";
@Parameter(names= {"-o","--out"},description=OUTPUT_OR_STANDOUT)
private Path outPath = null;

private XPath xpath;

private static class State implements RDFExprParser.Context {
	State parent=null;
	String encoding;
	Document domIn;
	Model model;
	Document domOut;
	Node root;
	Object current;
	final Map<String,Object> variables = new HashMap<>();
	State() {
		
	}
	State(State cp) {
		this.parent=cp;
		this.encoding = cp.encoding;
		this.domIn = cp.domIn;
		this.model = cp.model;
		this.domOut= cp.domOut;
		this.current = cp.current;
		this.root = cp.root;
		}
	public State setCurrent(Object current) {
		this.current = current;
		return this;
		}
	
	@Override
	public Object getCurrent() {
		return current;
		}
	@Override
	public Model getModel() {
		return model;
		}
	@Override
	public Object getVariable(final String key) {
		if(variables.containsKey(key)) return variables.get(key);
		if(parent!=null) return parent.getVariable(key);
		throw new IllegalArgumentException("undefined variable $"+key);
		}
	}

void process(final State state,Node node) throws XMLStreamException,XPathException {
	switch(node.getNodeType()) {
		case Node.DOCUMENT_NODE: {
			final Document doc = (Document)node;
			final Element root = doc.getDocumentElement();
			if(root==null) throw new XMLException("no root");
			if(!XmlUtils.isA(root, NS, "template")) throw new XMLException(root,"not a <template>");
			final NodeList L=(NodeList)this.xpath.evaluate("t:main", root,XPathConstants.NODESET);
			if(L.getLength()!=1) throw new XMLException(root,"expected one {"+NS+"}main but got "+L.getLength());
			final Element mainE = (Element)L.item(0);
			process(new State(state),mainE);
			break;
			}
		case Node.ELEMENT_NODE:
			{
			Element E = (Element)node;
			if(NS.equals(E.getNamespaceURI())) {
				String lclName  = E.getLocalName();
				if(lclName.equals("comment")) {
					
					}
				else if(lclName.equals("variable")) {
					final Attr attN = E.getAttributeNode("name");
					if(attN==null) throw new XMLException(E,"@name missing.");
					
					Object value;
					final Attr attS = E.getAttributeNode("select");
					if(attS==null)
						{
						DocumentFragment frg = state.domOut.createDocumentFragment();
						final State state2 = new State(state);
						state2.root = frg;
						for(Node c=E.getFirstChild();c!=null;c=c.getNextSibling()) {
							process(new State(state2),c);
							}
						value = frg.getTextContent();
						if(value==null) value="";
						}
					else
						{
						value = RDFExprParser.valueOf(state, attS.getValue());;
						}
					state.variables.put(attN.getValue(), value);
					}
				else if(lclName.equals("value-of")) {
					final Attr att = E.getAttributeNode("select");
					if(att==null) throw new XMLException(E,"@select missing.");
					final String context = RDFExprParser.valueOf(state, att.getValue());;
					if(!context.isEmpty()) state.root.appendChild(state.domOut.createTextNode(context));
					}
				else if(lclName.equals("main")) {
					for(Node c=E.getFirstChild();c!=null;c=c.getNextSibling()) {
						if(c.getNodeType()==Node.TEXT_NODE && StringUtils.isBlank(c.getNodeValue()) && state.root.getNodeType()==Node.DOCUMENT_NODE) continue;
						process(new State(state),c);
						}
					}
				else if(lclName.equals("for-each")) {
					final Attr att = E.getAttributeNode("select");
					if(att==null) throw new XMLException(E,"@select missing.");
					final ExtendedIterator<?> iter = RDFExprParser.iterator(state, att.getValue());
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
					throw new XMLException(E,"unknown element");
					}
				}
			else
				{
				Element newRoot = (Element)state.domOut.importNode(E,false);
				final State newState = new State(state);
				newState.root.appendChild(newRoot);
				newState.root = newRoot;
				
				
				final NodeList L=(NodeList)this.xpath.evaluate("t:attribute", E,XPathConstants.NODESET);
				for(int i=0;i< L.getLength();i++) {
					final Element A = (Element)L.item(i);
					final Attr att = A.getAttributeNode("name");
					if(att==null) throw new XMLException(A,"@name missing.");
					newRoot.setAttribute(att.getValue(), RDFExprParser.valueOf(newState, att.getValue()));
					}
				
				if(E.hasAttributes()) {
					final NamedNodeMap atts=E.getAttributes();
					for(int i=0;i< atts.getLength();i++) {
						if(NS.equals(atts.item(i).getNamespaceURI())) continue;
						newRoot.setAttributeNode((Attr)newState.domOut.importNode(atts.item(i), false));
						}
					}
				
				if(E.hasChildNodes()) {
					for(Node c=E.getFirstChild();c!=null;c=c.getNextSibling()) {
						process(newState,c);
						}
					}
				}
			break;
			}
		case Node.COMMENT_NODE:{
			break;
			}
		case Node.CDATA_SECTION_NODE: // continue
		case Node.PROCESSING_INSTRUCTION_NODE: // continue
		case Node.TEXT_NODE:{
			state.root.appendChild(state.domOut.importNode(node,false));
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
			dbf.setNamespaceAware(true);
			final DocumentBuilder db = dbf.newDocumentBuilder();
			state.domIn  = db.parse(args.get(0));
			state.domOut  = db.newDocument();
			state.model = ModelFactory.createDefaultModel() ;
			state.model.read(IOUtils.isURL(args.get(1))?args.get(1):Paths.get(args.get(1)).toUri().toASCIIString(),"RDF/XML");
			
			state.encoding = state.domIn.getInputEncoding();
			state.encoding=StringUtils.isBlank(state.encoding)?"UTF-8":state.encoding;
			state.root = state.domOut;
			process(state,state.domIn);
				
				
			final TransformerFactory trf = TransformerFactory.newInstance();
			final Transformer tr= trf.newTransformer();
			tr.setOutputProperty(OutputKeys.METHOD,"xml");
			tr.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION,"false");
			try(OutputStream out  = super.openPathAsOuputStream(outPath)) {
				tr.transform(new DOMSource(state.domOut), new StreamResult(out));
				out.flush();
				}
			return 0;
			}
		else
			{
			LOG.error("illegal number of arguments: expected  <XML> <RDF>");
			return -1;
			}
		}
	catch(final Throwable err) {
		LOG.error(err);
		return -1;
		}
	}
public static void main(String[] args) {
	new RDFTemplate().instanceMainWithExit(args);
	}
}
