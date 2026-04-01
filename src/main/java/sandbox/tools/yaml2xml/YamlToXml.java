package sandbox.tools.yaml2xml;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.w3c.dom.Attr;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.events.CommentEvent;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.ScalarEvent;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.io.IOUtils;
import sandbox.iterator.PeekIterator;
import sandbox.jsonx.JSONX;
import sandbox.tools.central.ProgramDescriptor;

public class YamlToXml extends Launcher {
	private static final Logger LOG = Logger.builder(YamlToXml.class).build();
    @Parameter(names={"-o","--output"},description=OUTPUT_OR_STANDOUT)
    private Path out = null; 
    @Parameter(names={"-S","--string"},description="all scalar are strings")
    private boolean all_scalar_are_string=false;
    @Parameter(names={"-X","--xml"},description="try to convert String as DOM")
    private boolean try_dom=false;

    private void writeXml(final XMLStreamWriter w,Node root) throws XMLStreamException {
    	switch(root.getNodeType()) {
    		case Node.TEXT_NODE:w.writeCharacters(Text.class.cast(root).getData()); break;
    		case Node.COMMENT_NODE: w.writeCharacters(Comment.class.cast(root).getData()); break;
    		case Node.CDATA_SECTION_NODE: w.writeCData(CDATASection.class.cast(root).getData());break;
    		case Node.ATTRIBUTE_NODE:
    			Attr att=Attr.class.cast(root);
    			w.writeAttribute(att.getName()	,att.getValue());
    			break;
    		case Node.ELEMENT_NODE:
    			Element E=Element.class.cast(root);
    			if(E.hasChildNodes()) {
    				w.writeStartElement(E.getNodeName());
    				}
    			else
    				{	
    				w.writeEmptyElement(E.getNodeName());
    				}
    			if(E.hasAttributes()) {
    				NamedNodeMap m= E.getAttributes();
    				for(int i=0;i< m.getLength();i++) writeXml(w,m.item(i));
    				}
    			if(E.hasChildNodes()) {
    				for(Node c=E.getFirstChild();c!=null;c=c.getNextSibling()) {
        				writeXml(w,c);
    	    			}
    				w.writeEndElement();
    				}
    			break;
    		default: LOG.warning("Node.Type no handled:"+root.getNodeType());break;
    		}
    	}
    
    private void writeXmlString(final XMLStreamWriter w, final String s) throws XMLStreamException {
    	String xmlS="<node>"+s+"</node>";
    	try {
    		final DocumentBuilderFactory dbf=DocumentBuilderFactory.newInstance();
    		final DocumentBuilder db=dbf.newDocumentBuilder();
    		try(InputStream is=new ByteArrayInputStream(xmlS.getBytes())) {
    			Document dom= db.parse(is);
    			Element root=dom.getDocumentElement();
    			for(Node c=root.getFirstChild();c!=null;c=c.getNextSibling()) {
    				writeXml(w,c);
	    			}
	    		}
	    	}
    	catch(Throwable err) {
    		
    		}
    	}
    
    private void recurse(final XMLStreamWriter w,final PeekIterator<Event> iter,String mykey) throws XMLStreamException {
    	final Event evt = iter.next();
    	switch(evt.getEventId()) {
			case StreamStart: break;
			case StreamEnd: break;
			case DocumentStart: break;
			case DocumentEnd: break;
			case MappingStart:
		    	w.writeStartElement("j","object",JSONX.NS);
		    	if(mykey!=null) w.writeAttribute("name", mykey);
		    	while(iter.hasNext()) {
		    		final Event evt2 = iter.peek();
		    		if(evt2.is(Event.ID.MappingEnd)) {
		    			iter.next();
		    			break;
		    			}
		    		if(!evt2.is(Event.ID.Scalar)) throw new XMLStreamException("expected scalar got "+evt2);
		    		final String key = ScalarEvent.class.cast(iter.next()).getValue();
		    		recurse(w,iter,key);
		    		}
				w.writeEndElement();
		    	break;
    		case SequenceStart:
	    		{
	    		w.writeStartElement("j","array",JSONX.NS);
	    		if(mykey!=null) w.writeAttribute("name",mykey);
	    		while(iter.hasNext()) {
		    		final Event evt2 = iter.peek();
		    		if(evt2.is(Event.ID.SequenceEnd)) {
		    			iter.next();
		    			break;
		    			}
		    		recurse(w,iter,null);
		    		}
	    		w.writeEndElement();
	    		break;
	    		}
			case SequenceEnd:
    			break;
			case MappingEnd:
    			break;
			case Scalar:
				final String str=ScalarEvent.class.cast(evt).getValue();
				String tag="string";
				if(!all_scalar_are_string) {
					if(str.equals("true") || str.equals("false")) {
						tag="boolean";
						}
					else {
						try {
							new BigDecimal(str);
							tag="number";
							}
						catch(NumberFormatException err) {
							}
						}
					}
				w.writeStartElement("j",tag,JSONX.NS);
				if(mykey!=null) w.writeAttribute("name", mykey);
				if(this.try_dom) {
					writeXmlString(w,str);
					}
				else
					{
					w.writeCharacters(str);
					}
				w.writeEndElement();
				break;
    		case Comment:
				w.writeComment(CommentEvent.class.cast(evt).getValue());
				break;
    		case Alias:
    			throw new IllegalArgumentException("Aliases are not supported");
    		default: break;
			}
		}
    
   
    
	@Override
	public int doWork(final List<String> args) {
		try {
			final Yaml yaml = new Yaml();
			final String encoding="UTF-8";
			try(OutputStream os = (this.out==null?System.out:IOUtils.openPathAsOutputStream(this.out))) {
				final XMLOutputFactory xof  = XMLOutputFactory.newFactory();
				xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
				final  XMLStreamWriter w=xof.createXMLStreamWriter(os,encoding);
				w.writeStartDocument(encoding,"1.0");
				final List<Path> paths= IOUtils.unrollPaths(args);
				if(paths.size()>1) w.writeStartElement("j","array",JSONX.NS);
				for(Path path : paths) {
					try(Reader r= Files.newBufferedReader(path)) {
						final Iterator<Event> iter0 = yaml.parse(r).iterator();
						final PeekIterator<Event> iter = PeekIterator.wrap(iter0);
						while(iter.hasNext()) {
							recurse(w,iter,null);
							}
						}
					}
				if(paths.size()>1) w.writeEndElement();
				w.writeEndDocument();
				os.flush();
				}
			return 0;
			}
		catch(Throwable err ) {
			LOG.error(err);
			return -1;
			}
		}
	public static ProgramDescriptor getProgramDescriptor() {
		return new ProgramDescriptor() {
			@Override
			public String getName() {
				return "yaml2xml";
				}
			};
		}
	
	public static void main(final String[] args) {
		new YamlToXml().instanceMainWithExit(args);

	}

}
