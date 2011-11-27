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
 * History
 * 	June 2011: faster results with rettype=2011

 */
package sandbox;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.GregorianCalendar;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;

/**
 * PubmedGender
 */
public class PubmedPerYear
	{
	private String query="";
	private int startYear=1900;
	private int endYear=3000;
	private PubmedPerYear()
		{
		this.endYear=new GregorianCalendar().get(Calendar.YEAR);
		}

	private void run() throws Exception
		{
		XMLInputFactory f= XMLInputFactory.newInstance();
		f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
		f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE,Boolean.FALSE);
		f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,Boolean.TRUE);
		f.setProperty(XMLInputFactory.IS_VALIDATING,Boolean.FALSE);
		f.setProperty(XMLInputFactory.SUPPORT_DTD,Boolean.FALSE);

		int year=this.startYear;

		while(year <=this.endYear)
			{
			URL url= new URL(
				"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?" +
				"db=pubmed&term="+
				URLEncoder.encode("("+this.query+")  "+year+"[PDAT]", "UTF-8")+
				"&retmode=xml&rettype=count&email=plindenbaum_at_yahoo.fr&tool=peryear");

			XMLEventReader reader= f.createXMLEventReader(url.openStream());
			XMLEvent evt;

			int count=-1;

			while(!(evt=reader.nextEvent()).isEndDocument())
				{
				if(!evt.isStartElement()) continue;
				String tag= evt.asStartElement().getName().getLocalPart();
				if(tag.equals("Count") && count==-1)
					{
					count=Integer.parseInt(reader.getElementText());
					break;
					}
				}
			reader.close();
			if(count>0)
				{
				System.out.println(""+year+"\t"+count);
				}
			++year;
			}
		}

	public static void main(String[] args)
		{
		try
			{
			PubmedPerYear app=new PubmedPerYear();

			int optind=0;
			while(optind< args.length)
				{
				if(args[optind].equals("-h") ||
				   args[optind].equals("-help") ||
				   args[optind].equals("--help"))
					{
					System.err.println("Options:");
					System.err.println(" -S <int> start year. Default:"+app.startYear);
					System.err.println(" -E <int> end year. Default:"+app.endYear);
					return;
					}
				else if(args[optind].equals("-S"))
					{
					app.startYear=Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-E"))
					{
					app.endYear=Integer.parseInt(args[++optind]);
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
			if(app.startYear>app.endYear)
				{
				System.err.println("start year > end year");
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
