package sandbox.tools.xmlpath;

import java.io.InputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.io.IOUtils;
import sandbox.lang.StringUtils;
import sandbox.tools.central.ProgramDescriptor;


public class XmlPath extends Launcher {
	private static final Logger LOG = Logger.builder(XmlPath.class).build();
	
	private class State
		{
		String qname;
		boolean has_element=false;
		final StringBuilder content= new StringBuilder();
		final Map<String,String> attributes = new HashMap<>();
		}
	
	private class MyHandler extends DefaultHandler
		{
		final LinkedList<State> stacks = new LinkedList<>();
		
		private String escape(final String s) {
			return StringUtils.escapeC(s);
			}
		
		private String path() {
			final StringBuilder sb=new StringBuilder();
			for(int i=0;i< stacks.size();++i)
				{
				final State st = stacks.get(i);
				sb.append("/");
				sb.append(st.qname);
				if(i+1!=stacks.size() && st.has_element && !st.attributes.isEmpty())
					{
					sb.append("[");
					sb.append(st.attributes.entrySet().stream().
							map(KV->"@"+escape(KV.getKey())+"=\""+escape(KV.getValue())+"\"").
							collect(Collectors.joining(" and "))
							);
					sb.append("]");
					}
					
				}
			return sb.toString();
			}
		
		@Override
		public void startElement(final String uri,final  String localName,final  String qName,final  Attributes attributes)
				throws SAXException {
			if(!stacks.isEmpty())
				{
				stacks.getLast().content.setLength(0);
				stacks.getLast().has_element = true;
				}
			final State st = new State();
			st.qname = qName;
			stacks.add(st);
			for(int i=0;i< attributes.getLength();i++)
				{
				st.attributes.put(attributes.getQName(i), attributes.getValue(i));
				}	
			}
		@Override
		public void endElement(final String uri,final String localName, final String qName) throws SAXException {
			final String p= path();
			final State st = stacks.removeLast();
			System.out.print(p);
			if(!st.has_element) System.out.print("\t"+escape(st.content.toString()));
			System.out.println();
			for(final String key:st.attributes.keySet())
				{
				System.out.println(p+"/@"+escape(key)+"\t"+escape(st.attributes.get(key)));
				}
			}
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			if(!stacks.isEmpty() && !stacks.getLast().has_element)
				{
				stacks.getLast().content.append(ch,start,length);
				}
			}
		}
	
@Override
public int doWork(final List<String> args) {
		try
			{
			String file = oneFileOrNull(args);
			try(InputStream in = (file==null?System.in:IOUtils.openStream(file))) {
				final SAXParserFactory spf = SAXParserFactory.newInstance();
				spf.setNamespaceAware(true);
				spf.setValidating(false);
				final SAXParser saxParser = spf.newSAXParser();
				final MyHandler dh = new MyHandler();
				saxParser.parse(in, dh);
				}
			return 0;
			}
		catch(final Exception err) {
			LOG.error(err);
			return -1;
			}	
	}		

public static ProgramDescriptor getProgramDescriptor() {
	return new ProgramDescriptor() {
		@Override
		public String getName() {
			return "xmpath";
			}
		};
	}

public static void main(final String[] args) {
	new XmlPath().instanceMainWithExit(args);
	}
}
