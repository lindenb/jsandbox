package sandbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;

public class CookieStoreUtils  {
	public static final String OPT_DESC="Cookie file. Obtained from sqlite3 -header -separator '   '  ~/.mozilla/firefox/xxx/cookies.sqlite 'select * from moz_cookies' ";
	private CookieStoreUtils()
		{
		
		}
	
	public static BasicCookieStore readXml(final File xml) throws IOException,XMLStreamException {
		final BasicCookieStore store = new BasicCookieStore();
		FileReader fr = null;
		XMLEventReader r=null;
		try {
			final XMLInputFactory xif = XMLInputFactory.newFactory();
			fr = new FileReader(xml);
			r = xif.createXMLEventReader(fr);
			while(r.hasNext())
				{
				XMLEvent evt= r.nextEvent();
				if(!evt.isStartElement()) continue;
				StartElement E= evt.asStartElement();
				if(!E.getName().getLocalPart().equals("cookie")) continue;
				Attribute att = E.getAttributeByName(new QName("name"));
				if(att==null) throw new IOException("@name missing");
				final String name=att.getValue();
				att = E.getAttributeByName(new QName("value"));
				if(att==null) throw new IOException("@ame missing");
				final String value=att.getValue();
				
				final BasicClientCookie cookie= new BasicClientCookie(name, value);
				att = E.getAttributeByName(new QName("domain"));
				if(att!=null) cookie.setDomain(att.getValue());
				att = E.getAttributeByName(new QName("path"));
				if(att!=null) cookie.setPath(att.getValue());
				att = E.getAttributeByName(new QName("expire"));
				if(att!=null) cookie.setExpiryDate(new Date(Long.valueOf(att.getValue())));
				att = E.getAttributeByName(new QName("secure"));
				if(att!=null) cookie.setSecure(Boolean.valueOf(att.getValue()));
				att = E.getAttributeByName(new QName("persistent"));

				store.addCookie(cookie);
				}
			fr.close();
		} 	finally {
			IOUtils.close(fr);
			}
		return store;
	}
	
	public static void writeXml(final OutputStream out,final CookieStore store) throws IOException,XMLStreamException {	
		XMLStreamWriter w=null;
		try {
			final XMLOutputFactory xof = XMLOutputFactory.newFactory();
			w = xof.createXMLStreamWriter(out,"UTF-8");
			w.writeStartDocument("UTF-8", "1.0");
			w.writeStartElement("cookies");
			w.writeAttribute("date", new SimpleDateFormat().format(new Date()));
			for(final Cookie c:store.getCookies())
				{
				w.writeEmptyElement("cookie");
				w.writeAttribute("name", c.getName());
				w.writeAttribute("value", c.getValue());

				if(c.getDomain()!=null) w.writeAttribute("domain", c.getDomain());
				if(c.getPath()!=null) w.writeAttribute("path", c.getPath());
				if(c.getExpiryDate()!=null) w.writeAttribute("expire", String.valueOf(c.getExpiryDate().getTime()));
				w.writeAttribute("secure", String.valueOf(c.isSecure()));
				w.writeAttribute("persistent", String.valueOf(c.isPersistent()));
				
				
				}
			w.writeEndElement();
			w.writeEndDocument();
			w.flush();
			w.close();
			out.flush();
		} finally {
			}
		}
	
	/**  sqlite3 -header -separator '   '  ~/.mozilla/firefox/xxx/cookies.sqlite 'select * from moz_cookies' */
	public static BasicCookieStore readTsv(final File path) throws IOException {
		final BasicCookieStore store = new BasicCookieStore();
		BufferedReader in = null;
		Pattern tab=Pattern.compile("[\t]");
		try {
			in = IOUtils.openBufferedReaderFromFile(path);
			String line =in.readLine();
			if(line==null) throw new IOException("first line is missing");
			String tokens[]=tab.split(line);
			Map<String, Integer> column2index= new HashMap<String, Integer>(tokens.length);
			for(int i=0;i< tokens.length;++i)
				{
				if(tokens[i].isEmpty()) continue;
				column2index.put(tokens[i], i);
				}
			for(final String col:new String[]{
					"id", 
					"baseDomain", 
					"originAttributes", 
					"name", 
					"value", 
					"host", 
					"path", 
					"expiry", 
					"lastAccessed", 
					"creationTime", 
					"isSecure", 
					"isHttpOnly", 
					"inBrowserElement"
				})
				{
				if(!column2index.containsKey(col))
					{
					in.close();
					throw new IOException("Column header \""+col+"\" missing in "+path);
					}
				}
			while((line=in.readLine())!=null)
				{
				
				tokens=tab.split(line);
				
				final BasicClientCookie cookie= new BasicClientCookie(
						tokens[column2index.get("name")],
						tokens[column2index.get("value")]
						);
				String att = tokens[column2index.get("baseDomain")];
				cookie.setDomain(att);
				att = tokens[column2index.get("path")];
				cookie.setPath(att);
				att = tokens[column2index.get("expiry")];
				cookie.setExpiryDate(new Date(1000L*Long.parseLong(att)));
				att = tokens[column2index.get("isSecure")];
				cookie.setSecure(att.equals("1"));
				cookie.setVersion(1);
				
				
				store.addCookie(cookie);
;				}
			in.close();in=null;
			return store;
		} finally {
			IOUtils.close(in);
			}
	}
	
	public static void main(String[] args) {
		if(args.length!=1)
			{
			System.err.println("Usage: file.xml or file.tsv");
			return ;
			}
		try {
		File in= new File(args[0]);
		CookieStore csf=null;
		if(in.getName().endsWith(".xml")) {
			csf = CookieStoreUtils.readXml(in); 
		} else
			{
			csf = CookieStoreUtils.readTsv(in); 
			}
		CookieStoreUtils.writeXml(System.out,csf);
		} catch(Exception err) 
		{
			err.printStackTrace();
		}

	}

}
