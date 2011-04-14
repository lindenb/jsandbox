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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
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
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
/**
 * 
 * NucleicAcidsResearch404
 *
 */
public class NucleicAcidsResearch404
	{
	private static final String NS="uri:nar404:";
	private static final String RDF="http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	private static final String FOAF="http://xmlns.com/foaf/0.1/";
	private static final String DC="http://purl.org/dc/elements/1.1/";
	private static final String HTML="http://www.w3.org/1999/xhtml";
	private static final String BIB="http://purl.org/ontology/bibo/";
	
	private String email="plindenbaum at yahoo fr";
	private String tool=NucleicAcidsResearch404.class.getSimpleName();
	private int timeout_milliseconds=10*1000;
	private static final Logger LOG=Logger.getLogger("nar404");
	private String query="\"Nucleic Acids Res\"[JOUR] \"Database issue\"[ISS]";
	private XPath xpath;
	/** credit: http://stackoverflow.com/questions/5261136 **/
	private Pattern hrefPattern=Pattern.compile("((https?:\\/\\/|www.)([-\\w.]+)+(:\\d+)?(\\/([\\-\\w\\/_.]*(\\?\\S+)?)?)?)");
	private Map<String,Database> url2databases=new  TreeMap<String,Database>(String.CASE_INSENSITIVE_ORDER);
	private int limit=-1;
	
	private static class Article
		implements Comparable<Article>
		{
		long pmid;
		String title;
		String year;
		
		public String getUri()
			{
			return "http://www.ncbi.nlm.nih.gov/pubmed/"+pmid;
			}
		
		void writeRDF(XMLStreamWriter w) throws XMLStreamException
			{
			w.writeStartElement("bib","Article",BIB);
			w.writeAttribute("rdf", "RDF", "about", getUri());
			
			w.writeStartElement("dc","title",DC);
			w.writeCharacters(title);
			w.writeEndElement();
			
			w.writeStartElement("dc","date",DC);
			w.writeCharacters(year);
			w.writeEndElement();
			
			w.writeStartElement("bib","pmid",BIB);
			w.writeCharacters(String.valueOf(pmid));
			w.writeEndElement();
			
			w.writeEndElement();
			}

		@Override
		public int compareTo(Article o)
			{
			return (this.pmid< o.pmid?-1:(this.pmid>o.pmid?1:0));
			}
		
		@Override
		public int hashCode()
			{
			return  31 + (int) (pmid ^ (pmid >>> 32));
			}

		@Override
		public boolean equals(Object obj)
			{
			if (this == obj) { return true; }
			if (obj == null) { return false; }
			if (getClass() != obj.getClass()) { return false; }
			Article other = (Article) obj;
			if (pmid != other.pmid) { return false; }
			return true;
			}
		@Override
		public String toString()
			{
			return getUri();
			}
		}
	
	private static class Database
		{
		String url;
		int httpStatus=200;
		String error=null;
		Set<Article> articles=new LinkedHashSet<Article>();
		
		void writeRDF(XMLStreamWriter w) throws XMLStreamException
			{
			boolean validURL=true;
			try
				{
				new URL(url);
				}
			catch (Throwable e)
				{
				validURL=false;
				}
			
			w.writeStartElement("j","WebResource",NS);
			if(validURL)
				{
				w.writeAttribute("rdf", "RDF", "about", url);
				}
			w.writeStartElement("j","hyperlink",NS);
			w.writeCharacters(url);
			w.writeEndElement();
			
			
			if(this.error!=null)
				{
				w.writeStartElement("j","error",NS);
				w.writeCharacters(error);
				w.writeEndElement();
				}
			else
				{
				w.writeStartElement("j","http-status",NS);
				w.writeCharacters(String.valueOf(httpStatus));
				w.writeEndElement();
				}
			
			w.writeEmptyElement("rdf","type",RDF);
			w.writeAttribute("rdf", "RDF", "resource",NS+(isValid()?"Alive":"Dead"));
			
			
			for(Article article:articles)
				{
				w.writeEmptyElement("j","article",NS);
				w.writeAttribute("rdf", RDF, "resource", article.getUri());
				}
			
			w.writeEndElement();
			}
		
		boolean isValid()
			{
			return (httpStatus==200 || httpStatus==301 || httpStatus==302) && error==null; 
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
		
		article.year=xpath.evaluate("MedlineCitation/Article/Journal/JournalIssue/PubDate/Year", root);
		NodeList list=(NodeList)xpath.evaluate("MedlineCitation/Article/Abstract/AbstractText", root,XPathConstants.NODESET);
		Set<String> urls=new HashSet<String>();

		for(int i=0;i< list.getLength();++i)
			{
			String abstractText=list.item(i).getTextContent();
			if(abstractText==null || abstractText.trim().isEmpty()) continue;
			
			Matcher matcher=hrefPattern.matcher(abstractText);
			
			int pos=0;
			while(pos<abstractText.length() &&  matcher.find(pos))
				{
				int b=matcher.start();
				pos=matcher.end();
				String url=abstractText.substring(b,pos);
				if(url.endsWith(".")) url=url.substring(0,url.length()-1);
				//if(url.endsWith("/")) url=url.substring(0,url.length()-1);
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
			
			}
		if(urls.isEmpty()) return;
		for(String url:urls)
			{
			Database database=url2databases.get(url);
			if(database==null && !url.endsWith("/") && url2databases.containsKey(url+"/"))
				{
				url+="/";
				database=url2databases.get(url);
				}
			if(database==null && url.endsWith("/") && url2databases.containsKey(url.substring(0,url.length()-1)))
				{
				url=url.substring(0,url.length()-1);
				database=url2databases.get(url);
				}
			if(database==null)
				{
				database=new Database();
				database.url=url;
				url2databases.put(url, database);
				}
			database.articles.add(article);
			}
		LOG.info(article.title+" "+urls);
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
			
			final int retmax=10000;
			for(int retstart=0;retstart<count;retstart+=retmax)
				{
				url= new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&WebEnv="+
						URLEncoder.encode(WebEnv,"UTF-8")+
						"&query_key="+URLEncoder.encode(QueryKey,"UTF-8")+
						"&retmode=xml&retmax="+retmax+"&retstart="+retstart+
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
				}
			int index=0;
			HttpURLConnection.setFollowRedirects(true);
			for(Database database:this.url2databases.values())
				{
				++index;
				LOG.info(database.url+" "+index+"/"+this.url2databases.size());
				try
					{
					in=null;
					url=new URL(database.url);
					URLConnection connection = url.openConnection();
					connection.setConnectTimeout(timeout_milliseconds);
					connection.setReadTimeout(timeout_milliseconds);
					connection.connect();
					if(connection instanceof HttpURLConnection)
						{
						HttpURLConnection httpURLConnection=HttpURLConnection.class.cast(connection);
						database.httpStatus=httpURLConnection.getResponseCode();
						}
					in=connection.getInputStream();
					in.read();//empty
					}
				catch (Throwable error)
					{
					database.error=error.getClass().getCanonicalName();
					if(error.getMessage()!=null && !error.getMessage().trim().isEmpty())
						{
						database.error+=" : "+error.getMessage();
						}
					LOG.info(database.url);
					error.printStackTrace();
					}
				finally
					{
					if(in!=null) in.close();
					in=null;
					}
				}
			Map<String,int[]> year2count=new TreeMap<String, int[]>();
			XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
			XMLStreamWriter w= xmlfactory.createXMLStreamWriter(System.out,"UTF-8");
			w.writeStartDocument("UTF-8","1.0");
			w.writeStartElement("rdf","RDF",RDF);
			w.writeNamespace("j", NS);
			w.writeNamespace("dc", DC);
			w.writeNamespace("rdf", RDF);
			w.writeNamespace("bib", BIB);
			w.writeNamespace("foaf", FOAF);
			w.writeNamespace("h", HTML);
			
			
			Set<Article> allArticles=new TreeSet<Article>();
			
			for(Database database:this.url2databases.values())
				{
				for(Article a:database.articles)
					{
					allArticles.add(a);
					
					int[] array=  year2count.get(a.year);
					if(array==null)
						{
						array=new int[]{0,0};
						year2count.put(a.year,array);
						}
					array[0]++;
					if(database.isValid())
						{
						array[1]++;
						}
					}
				}
			
			w.writeStartElement("rdf","Statement",RDF);
			w.writeAttribute("rdf",RDF,"about","");
			
				w.writeStartElement("foaf","maker",FOAF);
				
				  w.writeStartElement("foaf","Person",FOAF);
				    w.writeAttribute("rdf",RDF,"about","http://plindenbaum.blogspot.com");
				  	w.writeStartElement("foaf","name",FOAF);
				  		w.writeCharacters("Pierre Lindenbaum");
				    w.writeEndElement();
				  w.writeEndElement();//foaf Person
				  w.writeEndElement();//maker 
				  
				  
				  w.writeStartElement("dc","date",DC);
				    w.writeCharacters(new java.sql.Date(System.currentTimeMillis()).toString());
				  w.writeEndElement();
				  
				  w.writeStartElement("j","query",NS);
				    w.writeCharacters(this.query);
				  w.writeEndElement();
				  
				  w.writeStartElement("j","articles",NS);
				    w.writeCharacters(String.valueOf(count));
				  w.writeEndElement();
				  
				  w.writeStartElement("j","timeout",NS);
				    w.writeCharacters(String.valueOf(this.timeout_milliseconds));
				  w.writeEndElement();
				
				  w.writeStartElement("j","table",NS);
				  w.writeAttribute("rdf",RDF,"parseType","Literal");
				  
				  //begin table
				  w.writeStartElement("h","table",HTML);
					w.writeAttribute("h",HTML,"border","1");
					  w.writeStartElement("h","tr",HTML);
					   w.writeStartElement("h","th",HTML);
					   w.writeCharacters("Year");
					   w.writeEndElement();//th
					   w.writeStartElement("h","th",HTML);
					   w.writeCharacters("Count");
					   w.writeEndElement();//th
					   w.writeStartElement("h","th",HTML);
					   w.writeCharacters("OK");
					   w.writeEndElement();//th
					   w.writeStartElement("h","th",HTML);
					   w.writeCharacters("%");
					   w.writeEndElement();//th
					  w.writeEndElement();//tr
					for(String year:year2count.keySet())
						{
						int[] array=  year2count.get(year);
						w.writeStartElement("h","tr",HTML);
						  w.writeStartElement("h","td",HTML);
						   w.writeCharacters(year);
						  w.writeEndElement();//td
						  w.writeStartElement("h","td",HTML);
						   w.writeCharacters(String.valueOf(array[0]));
						  w.writeEndElement();//td
						  w.writeStartElement("h","td",HTML);
						   w.writeCharacters(String.valueOf(array[1]));
						  w.writeEndElement();//td
						  w.writeStartElement("h","td",HTML);
						   w.writeCharacters(String.valueOf((int)(100*(array[1]/(float)array[0]))));
						  w.writeEndElement();//td
						w.writeEndElement();//tr
						}
					w.writeEndElement();//table
				  //end table
				  
				 
				  
				w.writeEndElement();//maker
			w.writeEndElement();
				
				
			for(Database database:this.url2databases.values())
				{
				database.writeRDF(w);
				}
			
			for(Article a:allArticles)
				{
				a.writeRDF(w);
				}
			
			
			w.writeEndElement();//RDF
			
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
					String s=evt.asCharacters().getData();
					boolean empty=true;
					for(int i=0;i< s.length();++i)
						{
						if(!Character.isWhitespace(s.charAt(i)))
							{
							empty=false;
							break;
							}
						}
					if(!empty)
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