package sandbox;

import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.xml.namespace.QName;
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
		final StartElement startE;
		State() {
			parent = null;
			props = new HashMap<>();
			startE=null;
			}
		State(final State st,final StartElement tag) {
			this.parent = st;
			props = new HashMap<>(st.props);
			this.startE=tag;
			}
		
		void print(final PrintStream out,final String s) {
			int flag = 0;
			if(startE!=null) {
				final String qName = startE.getName().getLocalPart().toLowerCase();
				if(qName.equals("b")) flag+=1;
				if(qName.equals("i")) flag+=2;
				}
			
			if(flag!=0) out.print("\033["+flag+"m");
			out.print(s);
			if(flag!=0) out.print("\033[0m");
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
					state.print(out,evt.asCharacters().getData());
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
					
					final State st = new State(state,se);
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
