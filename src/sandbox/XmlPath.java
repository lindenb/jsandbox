package sandbox;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.stream.Collectors;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;


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
			final StringBuilder sb = new StringBuilder(s.length());
			for(int i=0;i< s.length();i++)
				{
				final char c = s.charAt(i);
				switch(c) {
					case '\n' : sb.append("\\n");break;
					case '\r' : sb.append("\\r");break;
					case '\t' : sb.append("\\t");break;
					case '\\' : sb.append("\\\\");break;
					case '\'' : sb.append("\\\'");break;
					case '\"' : sb.append("\\\"");break;
					default:sb.append(c);break;
					}
				}
			return sb.toString();
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
		InputStream in = null;
		try
			{
			String file = oneFileOrNull(args);
			if(file==null)
				{
				in  = System.in;
				}
			else
				{
				in = IOUtils.openStream(file);
				}
			final SAXParserFactory spf = SAXParserFactory.newInstance();
			spf.setNamespaceAware(true);
			spf.setValidating(false);
			final SAXParser saxParser = spf.newSAXParser();
			final MyHandler dh = new MyHandler();
			saxParser.parse(in, dh);
			
			return 0;
			}
		catch(final Exception err) {
			LOG.error(err);
			return -1;
			}
		finally
			{
			IOUtils.close(in);
			}
		
	}	
	
public static void main(final String[] args) {
	new XmlPath().instanceMainWithExit(args);
	}
}
