package sandbox;
/**
 * Author:
 *	Pierre Lindenbaum PhD
 *	plindenbaum@yahoo.fr
 */


import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLEncoder;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.XMLEvent;

/**
 * PubmedDump
 */
public class PubmedDump
	{
	private String email=null;
	private String tool=null;
	
	private PubmedDump()
		{
		
		}
	
	
	
	public void run(String query,OutputStream out) throws Exception
		{
		
		URL url= new URL(
			"http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi?db=pubmed&term="+
			URLEncoder.encode(query, "UTF-8")+	
			"&retstart=0&retmax=0&usehistory=y&retmode=xml&email=plindenbaum_at_yahoo.fr&tool=gender");
		
		XMLInputFactory f= XMLInputFactory.newInstance();
		f.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
		f.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE,Boolean.FALSE);
		f.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES,Boolean.TRUE);
		f.setProperty(XMLInputFactory.IS_VALIDATING,Boolean.FALSE);
		f.setProperty(XMLInputFactory.SUPPORT_DTD,Boolean.FALSE);
		XMLEventReader reader=f.createXMLEventReader(url.openStream());
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

		
		url= new URL("http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi?db=pubmed&WebEnv="+
				URLEncoder.encode(WebEnv,"UTF-8")+
				"&query_key="+URLEncoder.encode(QueryKey,"UTF-8")+
				"&retmode=xml&retmax="+count+
				(email==null?"":"&email="+URLEncoder.encode(email,"UTF-8"))+
				(tool==null?"":"&tool="+URLEncoder.encode(tool,"UTF-8"))
				)
				;
		
		byte buff[]=new byte[2048];
		int nRead;
		InputStream in=url.openStream();
		while((nRead=in.read(buff))!=-1)
			{
			System.out.write(buff, 0, nRead);
			}
		in.close();
		System.out.flush();
		}
	
	public static void main(String[] args)
		{
		try
			{
			PubmedDump app=new PubmedDump();
			
			int optind=0;
			while(optind< args.length)
				{
				if(args[optind].equals("-h") ||
				   args[optind].equals("-help") ||
				   args[optind].equals("--help"))
					{
					System.err.println("Options:");
					System.err.println(" -h help; This screen.");
					System.err.println(" -mail <mail>");
					System.err.println(" -tool <tool>");
					System.err.println(" query terms...");
					return;
					}
				else if(args[optind].equals("-mail"))
					{
					app.email=args[++optind];
					}
				else if(args[optind].equals("-tool"))
					{
					app.tool=args[++optind];
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
			if(optind==args.length)
				{
				System.err.println("Query missing");
				return;
				}
			String query="";
			while(optind< args.length)
				{
				if(!query.isEmpty()) query+=" ";
				query+=args[optind++];
				}
			query=query.trim();
			if(query.trim().isEmpty())
				{
				System.err.println("Query is empty");
				return;
				}
			
			app.run(query,System.out);
			}
		catch (Exception e)
			{
			e.printStackTrace();
			}
		}	
	} 
