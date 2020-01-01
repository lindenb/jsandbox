package sandbox.xml;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.beust.jcommander.Parameter;

import sandbox.IOUtils;
import sandbox.Launcher;
import sandbox.StringUtils;

public class XmlStream extends Launcher {
	private static final sandbox.Logger LOG = sandbox.Logger.builder(XmlStream.class).build();
    @Parameter(names={"-o","--output"},description="Output file or stdout")
	private Path outPath  = null;

	private class State {
		final String nodeName;
		final Map<String,Long> childCount = new HashMap<>();
		State(final String nodeName) {
			this.nodeName = nodeName;
		}
	}
	private class TheHandler extends DefaultHandler {
		final Stack<State> states = new Stack<>();
		final PrintWriter pw;
		TheHandler(final PrintWriter pw) {
			this.pw = pw;
		}
		
		private String path() {
			final StringBuilder sb=new StringBuilder();
			for(int i=1;i< this.states.size();i++) {
				final State state=this.states.get(i);
				final State parent  = this.states.get(i-1);
				sb.append(state.nodeName);
				sb.append("[");
				sb.append(parent.childCount.getOrDefault(state.nodeName, 0L));
				sb.append("]");
			}
			return sb.toString();
		}
		
		@Override
		public void startDocument() throws SAXException {
			states.add(new State(""));
			}
		@Override
		public void endDocument() throws SAXException {
			states.clear();
			pw.flush();
			}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes)
				throws SAXException {
			final State state = new State("/"+qName);
			final State parent = this.states.peek();
			parent.childCount.put(state.nodeName, 1L + parent.childCount.getOrDefault(state.nodeName, 0L));
			this.states.add(state);
			final String xpath = this.path();
			pw.println(xpath);
			for(int i=0;i< attributes.getLength();i++) {
				pw.print(xpath);
				pw.print("/@");
				pw.print(attributes.getQName(i));
				pw.print("\t");
				pw.println(StringUtils.escapeC(attributes.getValue(i)));
				}
			}
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			this.states.pop();
			}
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			pw.print(this.path());
			pw.print("\t");
			pw.println(StringUtils.escapeC(new String(ch,start,length)));
			}
		}
	@Override
	public int doWork(final List<String> args) {
		PrintWriter pw=null;
		try {
			pw = IOUtils.openPathAsPrintWriter(this.outPath);
		    final SAXParserFactory spf = SAXParserFactory.newInstance();
		    spf.setNamespaceAware(true);
		if(args.isEmpty()) {
			spf.newSAXParser().parse(System.in, new TheHandler(pw));
		} else
		{
			for(final String fname:args) {
				spf.newSAXParser().parse(new File(fname), new TheHandler(pw));
			}
		}
			pw.flush();
			pw.close();
			return 0;
		} catch (final Throwable err) {
			LOG.error(err);
			return -1;
		}
		finally {
			
		}
		}
	
	public static void main(String[] args) {
		new XmlStream().instanceMainWithExit(args);

	}

}
