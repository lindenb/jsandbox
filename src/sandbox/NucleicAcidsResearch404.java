/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	April-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Motivation:
 *   http://biostar.stackexchange.com/questions/7527
 * Compilation:
 *        ant nar404
 * Usage:
 *        java -jar nar404.jar 
 */
package sandbox;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class NucleicAcidsResearch404
	{
	private String email="plindenbaum at yahoo fr";
	private String tool=NucleicAcidsResearch404.class.getSimpleName();
	private int timeout_milliseconds=10*1000;
	private static final Logger LOG=Logger.getLogger("nar404");
	private String query="\"Nucleic Acids Res\"[JOUR] \"Database issue\"[ISS]";
	private XPath xpath;
	/** credit: http://stackoverflow.com/questions/5261136 **/
	private Pattern hrefPattern=Pattern.compile("((https?:\\/\\/|www.)([-\\w.]+)+(:\\d+)?(\\/([\\w\\/_.]*(\\?\\S+)?)?)?)");
	private Map<String,Database> url2databases=new  HashMap<String,Database>();
	private int limit=-1;
	
	private static class Article
		{
		long pmid;
		String title;
		int year;
		
		void write(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeCharacters("(");
			w.writeCharacters(String.valueOf(year));
			w.writeCharacters(") ");
			
			w.writeStartElement("a");
			w.writeAttribute("target","blank");
			w.writeAttribute("href", "http://www.ncbi.nlm.nih.gov/pubmed/"+pmid);
			w.writeCharacters(title);
			w.writeEndElement();
			}
		}
	
	private static class Database
		{
		String url;
		int code=200;
		Exception error=null;
		List<Article> articles=new ArrayList<Article>();
		
		void write(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeStartElement("div");
			if(!(code==200 && error==null))
				{
				w.writeAttribute("style", "background-color:rgb(245,222,179);padding:5px;");
				}
			else
				{
				w.writeAttribute("style", "background-color:rgb(152,251,152);padding:5px;");
				}
			w.writeStartElement("h3");
			w.writeStartElement("a");
			w.writeAttribute("target","blank");
			w.writeAttribute("href", url);
			w.writeCharacters(url);
			w.writeEndElement();
			w.writeEndElement();
			
			//code
			w.writeStartElement("div");
			w.writeStartElement("b");
			w.writeCharacters("http status:");
			w.writeEndElement();
			w.writeCharacters(String.valueOf(code));
			w.writeEndElement();
			
			if(this.error!=null)
				{
				w.writeStartElement("div");
				w.writeStartElement("b");
				w.writeCharacters("Exception:");
				w.writeEndElement();
				w.writeCharacters(this.error.getClass().getCanonicalName());
				w.writeEndElement();
				}
			
			w.writeStartElement("ul");
			for(Article article:articles)
				{
				w.writeStartElement("li");
				article.write(w);
				w.writeEndElement();
				}
			w.writeEndElement();
			
			w.writeEndElement();//div
			w.writeCharacters("\n");
			}
		}	
	
	private NucleicAcidsResearch404() throws Exception
		{
		XPathFactory f=XPathFactory.newInstance();
		this.xpath=f.newXPath();
		}
	private void analyse(Document dom)  throws Exception
		{
		Element root=dom.getDocumentElement();
		Article article=new Article();
		article.pmid=Long.parseLong(xpath.evaluate("MedlineCitation/PMID", root));
		
		article.title=xpath.evaluate("MedlineCitation/Article/ArticleTitle", root);
		article.year=Integer.parseInt(xpath.evaluate("MedlineCitation/Article/Journal/JournalIssue/PubDate/Year", root));
		String abstractText=xpath.evaluate("MedlineCitation/Article/Abstract", root);
		if(abstractText==null || abstractText.trim().isEmpty()) return;
		abstractText=abstractText.replaceAll("http://nar. oupjo", "http://nar.oupjo");
		
		
		Matcher matcher=hrefPattern.matcher(abstractText);
		Set<String> urls=new HashSet<String>();
		int pos=0;
		while(pos<abstractText.length() &&  matcher.find(pos))
			{
			int b=matcher.start();
			pos=matcher.end();
			String url=abstractText.substring(b,pos);
			if(url.endsWith(".")) url=url.substring(0,url.length()-1);
			if(url.endsWith("/")) url=url.substring(0,url.length()-1);
			if(url.startsWith("www")) url="http://"+url;
			if(url.startsWith("http://github")) continue;
			boolean ok=true;
			for(String s:urls)
				{
				if(url.startsWith(s) || s.startsWith(url))
					{
					ok=false;
					break;
					}
				}
			if(!ok) continue;
			urls.add(url);
			}
		if(urls.isEmpty()) return;
		for(String url:urls)
			{
			Database database=url2databases.get(url);
			if(database==null)
				{
				database=new Database();
				database.url=url;
				url2databases.put(url, database);
				}
			database.articles.add(article);
			}
		}
	private void run() throws Exception
			{
			URL url= new URL(
				"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term="+
				URLEncoder.encode(query, "UTF-8")+	
				"&retstart=0&retmax=0&usehistory=y&retmode=xml&email=plindenbaum_at_yahoo.fr&tool=gender");
			LOG.info(url.toString());
			XMLInputFactory f= XMLInputFactory.newInstance();
			f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
			f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE,Boolean.FALSE);
			f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,Boolean.TRUE);
			f.setProperty(XMLInputFactory.IS_VALIDATING,Boolean.FALSE);
			f.setProperty(XMLInputFactory.SUPPORT_DTD,Boolean.FALSE);
			InputStream in=url.openStream();
			XMLEventReader reader=f.createXMLEventReader(in);
			XMLEvent evt;
			String QueryKey=null;
			String WebEnv=null;
			int count=-1;
	
			while(!(evt=reader.nextEvent()).isEndDocument())
				{
				if(!evt.isStartElement()) continue;	
				String tag= evt.asStartElement().getName().getLocalPart();
				if(tag.equals("QueryKey"))
					{
					QueryKey= reader.getElementText().trim();
					}
				else if(tag.equals("WebEnv"))
					{
					WebEnv= reader.getElementText().trim();
					}
				else  if(tag.equals("Count") && count==-1)
					{
					count=Integer.parseInt(reader.getElementText());
					}
				}
			reader.close();
			in.close();
			
			url= new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&WebEnv="+
					URLEncoder.encode(WebEnv,"UTF-8")+
					"&query_key="+URLEncoder.encode(QueryKey,"UTF-8")+
					"&retmode=xml&retmax="+count+
					(email==null?"":"&email="+URLEncoder.encode(email,"UTF-8"))+
					(tool==null?"":"&tool="+URLEncoder.encode(tool,"UTF-8"))
					)
					;
			LOG.info(url.toString());
						
			DocumentBuilderFactory domF=DocumentBuilderFactory.newInstance();
			domF.setCoalescing(true);
			domF.setNamespaceAware(false);
			domF.setValidating(false);
			domF.setExpandEntityReferences(true);
			domF.setIgnoringComments(false);
			domF.setIgnoringElementContentWhitespace(true);
			DocumentBuilder docBuilder= domF.newDocumentBuilder();
			Document dom=docBuilder.newDocument();
				
	
			in=url.openStream();
			reader=f.createXMLEventReader(in);
			while(reader.hasNext())
				{
				if(this.limit>0 && this.url2databases.size()>=this.limit)
					{
					break;
					}
				evt=reader.peek();
				if(!(evt.isStartElement() &&
					 evt.asStartElement().getName().getLocalPart().equals("PubmedArticle")))
					{
					reader.next();//consumme
					continue;
					}
				evt=reader.nextEvent();
				Element root=parseDom(reader,evt.asStartElement(),dom);
				dom.appendChild(root);
				analyse(dom);
				dom.removeChild(root);
				
				}
			reader.close();
			in.close();
			
			int index=0;
			HttpURLConnection.setFollowRedirects(true);
			for(Database database:this.url2databases.values())
				{
				++index;
				LOG.info(database.url+" "+index+"/"+this.url2databases.size());
				try
					{
					url=new URL(database.url);
					URLConnection connection = url.openConnection();
					connection.setConnectTimeout(timeout_milliseconds);
					
					connection.connect();
					if(connection instanceof HttpURLConnection)
						{
						HttpURLConnection httpURLConnection=HttpURLConnection.class.cast(connection);
						database.code=httpURLConnection.getResponseCode();
						}
					}
				catch (Exception error)
					{
					database.error=error;
					LOG.info(database.url);
					error.printStackTrace();
					}
				}
			Map<Integer,int[]> year2count=new TreeMap<Integer, int[]>();
			XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
			XMLStreamWriter w= xmlfactory.createXMLStreamWriter(System.out,"UTF-8");
			w.writeStartDocument("UTF-8","1.0");
			w.writeStartElement("html");
			w.writeStartElement("body");
			w.writeStartElement("div");
			for(Database database:this.url2databases.values())
				{
				database.write(w);
				for(Article a:database.articles)
					{
					int[] array=  year2count.get(a.year);
					if(array==null)
						{
						array=new int[]{0,0};
						year2count.put(a.year,array);
						}
					array[0]++;
					if(database.code==200 && database.error==null )
						{
						array[1]++;
						}
					}
				}
			
			w.writeStartElement("div");
			w.writeStartElement("table");
			w.writeAttribute("border","1");
			  w.writeStartElement("tr");
			   w.writeStartElement("th");
			   w.writeCharacters("Year");
			   w.writeEndElement();//th
			   w.writeStartElement("th");
			   w.writeCharacters("Count");
			   w.writeEndElement();//th
			   w.writeStartElement("th");
			   w.writeCharacters("OK");
			   w.writeEndElement();//th
			   w.writeStartElement("th");
			   w.writeCharacters("%");
			   w.writeEndElement();//th
			  w.writeEndElement();//tr
			for(Integer year:year2count.keySet())
				{
				int[] array=  year2count.get(year);
				w.writeStartElement("tr");
				  w.writeStartElement("td");
				   w.writeCharacters(String.valueOf(year));
				  w.writeEndElement();//td
				  w.writeStartElement("td");
				   w.writeCharacters(String.valueOf(array[0]));
				  w.writeEndElement();//td
				  w.writeStartElement("td");
				   w.writeCharacters(String.valueOf(array[1]));
				  w.writeEndElement();//td
				  w.writeStartElement("td");
				   w.writeCharacters(String.valueOf((int)(100*(array[1]/(float)array[0]))));
				  w.writeEndElement();//td
				w.writeEndElement();//tr
				}
			w.writeEndElement();//table
			w.writeEndElement();//div
			
			w.writeEndElement();//div
			w.writeEndElement();//body
			w.writeEndElement();//html
			w.writeEndDocument();
			w.flush();
			}
	
		private Element parseDom(XMLEventReader reader,StartElement start,Document dom) throws XMLStreamException
			{
			Element root=dom.createElement(start.getName().getLocalPart());
			Iterator<?> iter=start.getAttributes();
			while(iter.hasNext())
				{
				Attribute att=(Attribute)iter.next();
				root.setAttribute(att.getName().getLocalPart(), att.getValue());
				}
			while(reader.hasNext())
				{
				XMLEvent evt=reader.nextEvent();
				if(evt.isStartElement())
					{
					root.appendChild(parseDom(reader, evt.asStartElement(), dom));
					}
				else if(evt.isEndElement())
					{
					break;
					}
				else if(evt.isCharacters())
					{
					String s=evt.asCharacters().getData().trim();
					if(!s.isEmpty())
						{
						root.appendChild(dom.createTextNode(s));
						}
					}
				}
			return root;
			}
	
		public static void main(String[] args)
			{
			
			try
				{
				NucleicAcidsResearch404 app=new NucleicAcidsResearch404();
				int optind=0;
				while(optind< args.length)
					{
					if(args[optind].equals("-h") ||
					   args[optind].equals("-help") ||
					   args[optind].equals("--help"))
						{
						System.err.println("Pierre Lindenbaum PhD 2011");
						System.err.println("Options:");
						System.err.println(" -h help; This screen.");
						System.err.println(" -q <query> default is:"+app.query);
						System.err.println(" -L <int> (limit to N database (for debugging).");
						return;
						}
					else if(args[optind].equals("-q"))
						{
						app.query=args[++optind];
						}
					else if(args[optind].equals("-L"))
						{
						app.limit=Integer.parseInt(args[++optind]);
						}
					else if(args[optind].equals("--"))
						{
						optind++;
						break;
						}
					else if(args[optind].startsWith("-"))
						{
						System.err.println("Unknown option "+args[optind]);
						return;
						}
					else 
						{
						break;
						}
					++optind;
					}
				if(args.length!=optind)
					{
					System.err.println("illegal number of arguments.");
					return;
					}
				app.run();
				} 
			catch(Throwable err)
				{
				err.printStackTrace();
				}
			}
	}
