package sandbox.tools.yaml2xml;

import java.io.OutputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

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

    
    private void recurse(XMLStreamWriter w,final PeekIterator<Event> iter,String mykey) throws XMLStreamException {
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
				w.writeCharacters(str);
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
