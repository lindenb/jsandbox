package sandbox.tools.mail2xml;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;

import org.apache.james.mime4j.MimeException;
import org.apache.james.mime4j.parser.AbstractContentHandler;
import org.apache.james.mime4j.parser.ContentHandler;
import org.apache.james.mime4j.parser.MimeStreamParser;
import org.apache.james.mime4j.stream.BodyDescriptor;
import org.apache.james.mime4j.stream.Field;
import org.apache.james.mime4j.stream.MimeConfig;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.io.IOUtils;
import sandbox.tools.central.ProgramDescriptor;
import sandbox.xml.stream.XmlStreamWriter;

public class Mail2Xml extends Launcher {
	private static final Logger LOG = Logger.builder(Mail2Xml.class).build();
	
	private class MyHandler extends AbstractContentHandler {
		final XmlStreamWriter w;
		
		MyHandler( XmlStreamWriter w ) {
			this.w=w;
			}
		@Override
		public void preamble(InputStream is) throws MimeException, IOException {
			IOUtils.copyTo(is, System.err);
			}
		@Override
		public void epilogue(InputStream is) throws MimeException, IOException {
			IOUtils.copyTo(is, System.err);
			}
		@Override
		public void startMessage() throws MimeException {
			w.writeStartElement("message");
			}
		@Override
		public void endMessage() throws MimeException {
			w.writeEndElement();
			}
		@Override
		public void startHeader() throws MimeException {
			w.writeStartElement("header");
			}
		@Override
		public void field(Field field) throws MimeException {
			w.writeStartElement("field");
			w.writeAttribute("name", field.getName());
			w.writeCharacters(field.getBody());
			w.writeEndElement();
			}
		@Override
		public void endHeader() throws MimeException {
			w.writeEndElement();
			}	
		@Override
		public void body(BodyDescriptor bd, InputStream is) throws MimeException, IOException {
			w.writeStartElement("body");
			System.err.println(bd);
			w.writeAttribute("charset", bd.getCharset());
			if(bd.getContentLength()>=0) w.writeAttribute("content-length",bd.getContentLength());
			w.writeAttribute("media-type",bd.getMediaType());
			w.writeAttribute("mime-type",bd.getMimeType());
			w.writeAttribute("sub-type",bd.getSubType());
			w.writeAttribute("transfert-encoding",bd.getTransferEncoding());
			if(bd.getBoundary()!=null) w.writeAttribute("boundary",bd.getBoundary());
			if(bd.getMediaType().equals("text/plain")) {
				try(InputStreamReader isr=new InputStreamReader(is, bd.getCharset())) {
					char[] buf=new char[2024];
					for(;;) {
						int n=isr.read(buf);
						if(n==-1) break;
						w.writeCharacters(buf, 0, n);
						}
					}
				}
			else
				{
				IOUtils.consume(is);
				}
			}
		@Override
		public void endBodyPart() throws MimeException {
			w.writeEndElement();
			}
		}
	
	private void parse(XmlStreamWriter w,InputStream in) throws IOException, MimeException {
		final ContentHandler handler = new MyHandler(w);
        final  MimeConfig config = MimeConfig.DEFAULT;
        final MimeStreamParser parser = new MimeStreamParser(config);
        parser.setContentHandler(handler);
        parser.parse(in);
		}

@Override
public int doWork(final List<String> args) {
	try {		
		final List<Path> files = IOUtils.unrollPaths(args);
		XMLOutputFactory xof = XMLOutputFactory.newFactory();
		xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES, Boolean.TRUE);
		XmlStreamWriter w = new XmlStreamWriter(xof.createXMLStreamWriter(System.out,"UTF-8"));
		w.writeStartDocument("UTF-8", "1.0");
		w.writeStartElement("mail2xml");
		if(files.isEmpty()) {
			w.writeStartElement("mailbox");
			w.writeAttribute("name", "-");
			this.parse(w,System.in);
			
			w.writeEndElement();
			}
		else
			{
			for(Path p:files) {
				try(InputStream in=IOUtils.openPathAsInputStream(p)) {
					w.writeStartElement("mailbox");
					w.writeAttribute("name", p.toString());
					this.parse(w,in);
					w.writeEndElement();
					}
				}
			}
		w.writeEndElement();
		w.writeEndDocument();
		w.flush();
		w.close();
		return 0;
		}
	catch(final Throwable err) {
	LOG.error(err);
	return -1;
	}
}

public static ProgramDescriptor getProgramDescriptor() {
	return new ProgramDescriptor() {
		@Override
		public String getName() {
			return "mail2xml";
			}
		};
	}

public static void main(String[] args) {
	new Mail2Xml().instanceMainWithExit(args);
	}

}
