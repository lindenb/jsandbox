package sandbox;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

public class HtmlToTTY extends Launcher {
	private static final Logger LOG = Logger.builder(HtmlToTTY.class).build();
	private static final String KEY_ITALIC = "italic";
	private static final String KEY_BOLD = "bold";
	private static final String KEY_PRE = "pre";
	private class State
		{
		final State parent;
		final Map<String, Object> props;
		final String tag;
		State() {
			parent = null;
			props = new HashMap<>();
			tag="";
			}
		State(final State st,final String tag) {
			this.parent = st;
			props = new HashMap<>(st.props);
			this.tag=tag;
			}
		void print(final String s) {
			
			}
		}
	
	private void scan(final PrintStream out,final XMLEventReader r,final State state)
		throws XMLStreamException
		{
		while(r.hasNext())	{
			final XMLEvent evt = r.nextEvent();
			switch(evt.getEventType())
				{
				case XMLEvent.END_DOCUMENT: return;
				case XMLEvent.ATTRIBUTE:break;
				case XMLEvent.COMMENT:break;
				case XMLEvent.CDATA:
				case XMLEvent.CHARACTERS:
					{
					
					break;
					}
				case XMLEvent.START_ELEMENT:
					{
					final StartElement se = evt.asStartElement();
					final String localName = se.getName().getLocalPart().toLowerCase();
					
					if( localName.equals("pre") || localName.equals("div") || 
						localName.equals("p") || localName.equals("br")) {
						out.println();
					}
					
					final State st = new State(state,localName);
					scan(out,r,st);
					break;
					}
				case XMLEvent.END_ELEMENT:
					{
					return;
					}
				}
			}
		}
	@Override
	public int doWork(final List<String> args) {
		try {
			final State root = new State();
			PrintStream out = System.out;
			final String filein = oneFileOrNull(args);
			final InputStream in = filein==null?System.in:IOUtils.openStream(filein);
			final XMLEventReader r = XMLInputFactory.newInstance().createXMLEventReader(in);
			scan(out,r,root);
			in.close();
			r.close();
			out.flush();
			return 0;
		} catch (Exception e) {
			return -1;
			}
		
		}
	
	public static void main(final String[] args) {
		new HtmlToTTY().instanceMainWithExit(args);

	}

}
