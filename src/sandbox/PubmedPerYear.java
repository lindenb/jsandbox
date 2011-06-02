/**
 * Author:
 *	Pierre Lindenbaum PhD
 *	plindenbaum@yahoo.fr
 * Motivation:
 *	http://friendfeed.com/the-life-scientists/867edc13/understanding-it-wouldn-t-be-too-horrible-to
 * Compile:
 	javac PubmedPerYear.java
 * Execute:
 	java PubmedPerYear "Wikipedia"
 	
	2005    1
	2006    3
	2007    9
	2008    15
	2009    17
	2010    13


 */
package sandbox;
import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.XMLEvent;

/**
 * PubmedGender
 */
public class PubmedPerYear
	{
	private String query="";
	
	private PubmedPerYear()
		{
		}
	
	private XMLEventReader newReader(URL url) throws IOException,XMLStreamException
		{
		XMLInputFactory f= XMLInputFactory.newInstance();
		f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
		f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE,Boolean.FALSE);
		f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,Boolean.TRUE);
		f.setProperty(XMLInputFactory.IS_VALIDATING,Boolean.FALSE);
		f.setProperty(XMLInputFactory.SUPPORT_DTD,Boolean.FALSE);
		XMLEventReader reader=f.createXMLEventReader(url.openStream());
		return reader;
		}
	
	private void run() throws Exception
		{
		Map<Integer,Integer> year2count=new TreeMap<Integer,Integer>();
		URL url= new URL(
			"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term="+
			URLEncoder.encode(this.query, "UTF-8")+	
			"&retstart=0&retmax=0&usehistory=y&retmode=xml&email=plindenbaum_at_yahoo.fr&tool=gender");
		
		XMLEventReader reader= newReader(url);
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

		if(count>0)
			{
			url= new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&WebEnv="+
					URLEncoder.encode(WebEnv,"UTF-8")+
					"&query_key="+URLEncoder.encode(QueryKey,"UTF-8")+
					"&retmode=xml&retmax="+count+"&email=plindenbaum_at_yahoo.fr&tool=mail");
			
			reader= newReader(url);
			
			
			while(reader.hasNext())
				{
				evt=reader.nextEvent();
				if(!evt.isStartElement()) continue;
				String tagName= evt.asStartElement().getName().getLocalPart();
				if(!tagName.equals("DateCreated")) continue;
		
				while(reader.hasNext())
					{
					evt=reader.nextEvent();
					if(evt.isStartElement())
						{
						String localName=evt.asStartElement().getName().getLocalPart();
						if(localName.equals("Year"))
							{
							try
								{
								int year=Integer.parseInt(reader.getElementText());
								Integer num=year2count.get(year);
								if(num==null) num=0;
								num++;
								year2count.put(year,num);
								}
							catch(Exception numerr)
								{
								System.err.println(numerr.getMessage());
								}
							}
						}
					else if(evt.isEndElement())
						{
						if(evt.asEndElement().getName().getLocalPart().equals("DateCreated"))
							{
							break;
							}
						}
					}
				}
			reader.close();
			}
		for(Integer year:year2count.keySet())
			{
			System.out.println(""+year+"\t"+year2count.get(year));
			}
		}
	
	public static void main(String[] args)
		{
		try
			{
			PubmedPerYear app=new PubmedPerYear();
			
			int optind=0;
			
			
			app.query="";
			while(optind< args.length)
				{
				if(!app.query.isEmpty()) app.query+=" ";
				app.query+=args[optind++];
				}
			app.query=app.query.trim();
			if(app.query.trim().isEmpty())
				{
				System.err.println("Query is empty");
				return;
				}
			
			
			app.run();
			
			}
		catch (Exception e)
			{
			e.printStackTrace();
			}
		}	
	} 
