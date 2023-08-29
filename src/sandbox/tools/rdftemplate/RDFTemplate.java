package sandbox.tools.rdftemplate;

import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.StringUtils;

public class RDFTemplate extends Launcher {
@Parameter(names= {"-o","--out"},description=OUTPUT_OR_STANDOUT)
private Path outPath = null;

private static class State {
	State parent=null;
	String encoding;
	Document dom;
	Model model;
	XMLStreamWriter w;
	State() {
		
	}
	State(State cp) {
		this.parent=cp;
		this.encoding = cp.encoding;
		this.dom = cp.dom;
		this.model = cp.model;
		this.w= cp.w;
	}
}

void process(State state,Node node) throws XMLStreamException {
	switch(node.getNodeType()) {
		case Node.DOCUMENT_NODE: {
			state.w.writeStartDocument(state.encoding,"1.0");
			for(Node c=node.getFirstChild();c!=null;c=c.getNextSibling()) {
				process(new State(state),c);
				state.w.writeEndDocument();
				}
			break;
			}
		case Node.COMMENT_NODE:
			state.w.writeComment(Comment.class.cast(node).getData());
			break;
			}
		}

@Override
public int doWork(List<String> args) {
	try {
		if(args.size()==2) {
			final State state = new State();
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			state.dom  = db.parse(args.get(0));
			state.model = ModelFactory.createDefaultModel() ;
			state.model.read(args.get(1),"RDF/XML");
			
			XMLOutputFactory xof = XMLOutputFactory.newFactory();
			try(OutputStream out  = super.openPathAsOuputStream(outPath)) {
				state.encoding = state.dom.getInputEncoding();
				state.encoding=StringUtils.isBlank(encoding)?"UTF-8":encoding;
				final XMLStreamWriter w = xof.createXMLStreamWriter(out,encoding);
				w.writeStartDocument(state.encoding,"1.0");
				process(dom,model,w);
				w.writeEndDocument();
				w.close();
				w.flush();
				out.flush();
				}
			}
		else
			{
			
			}
		
		return 0;
		}
	catch(Throwable err) {
		err.printStackTrace();
		}
	}
public static void main(String[] args) {
	new RDFTemplate().instanceMainWithExit(args);
	}
}
