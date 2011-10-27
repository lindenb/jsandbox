/**
 * Author:
 *	Pierre Lindenbaum PhD
 *	plindenbaum@yahoo.fr
 * Source of data:
 *      http://cpansearch.perl.org/src/EDALY/Text-GenderFromName-0.33/GenderFromName.pm
 */
package sandbox;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Collator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

/**
 * PubmedGender
 */
public class PubmedGender
	{
	private Map<String,Float> males=null;
	private Map<String,Float> females=null;
	private String query="";
	private int canvasSize=200;
	private boolean ignoreUndefined=false;
	private boolean html=true;
	private boolean firstAuthor=false;

	private PubmedGender()
		{
		Collator collator= Collator.getInstance(Locale.US);
		collator.setStrength(Collator.PRIMARY);
		this.males=new TreeMap<String, Float>(collator);
		this.females=new TreeMap<String, Float>(collator);
		}

	private void loadNames()
		throws IOException
		{
		BufferedReader in=new BufferedReader(new InputStreamReader(new URL(
				"http://cpansearch.perl.org/src/EDALY/Text-GenderFromName-0.33/GenderFromName.pm").openStream()));
		String  line;
		Map<String,Float> map=null;
		int posAssign=-1;
		while((line=in.readLine())!=null)
			{
			if(line.startsWith("$Males = {"))
				{
				map=this.males;
				}
			else if(line.startsWith("$Females = {"))
				{
				map=this.females;
				}
			else if(line.contains("}"))
				{
				map=null;
				}
			else if(map!=null && ((posAssign=line.indexOf("=>"))!=-1))
				{
				String name=line.substring(0,posAssign).replaceAll("'","").toLowerCase().trim();
				Float freq=Float.parseFloat(line.substring(posAssign+2).replaceAll("[',]","").toLowerCase().trim());
				map.put(name, freq);
				}
			else
				{
				map=null;
				}
			}
		in.close();
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
		int countMales=0;
		int countFemales=0;
		int countUnknown=0;
		int countIgnored=0;
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
			int authorIndex=0;

			while(reader.hasNext())
				{
				evt=reader.nextEvent();
				if(!evt.isStartElement()) continue;
				String tagName= evt.asStartElement().getName().getLocalPart();
				if(tagName.equals("AuthorList"))
					{
					authorIndex=-1;
					}
				if(!tagName.equals("Author")) continue;
				String firstName=null;
				String initials=null;
				authorIndex++;
				while(reader.hasNext())
					{
					evt=reader.nextEvent();
					if(evt.isStartElement())
						{
						String localName=evt.asStartElement().getName().getLocalPart();
						if(localName.equals("ForeName") || localName.equals("FirstName"))
							{
							firstName=reader.getElementText().toLowerCase();
							}
						else if(localName.equals("Initials"))
							{
							initials=reader.getElementText().toLowerCase();
							}
						}
					else if(evt.isEndElement())
						{
						if(evt.asEndElement().getName().getLocalPart().equals("Author"))
							{
							break;
							}
						}
					}
				if(this.firstAuthor && authorIndex!=0) continue;
				if(	firstName==null ) {countIgnored++;continue;}
				if(	firstName.length()==1 ||
					firstName.equals(initials))
					{
					countIgnored++;
					continue;
					}

				String tokens[]=firstName.split("[ ]+");
				firstName="";
				for(String s:tokens)
					{
					if(s.length()> firstName.length())
						{
						firstName=s;
						}
					}


				if(	firstName.length()==1 ||
					firstName.equals(initials))
					{
					countIgnored++;
					continue;
					}

				Float male= this.males.get(firstName);
				Float female= this.females.get(firstName);

				if(male==null && female==null)
					{
					//System.err.println("Undefined "+firstName+" / "+lastName);
					countUnknown++;
					}
				else if(male!=null && female==null)
					{
					countMales++;
					}
				else if(male==null && female!=null)
					{
					countFemales++;
					}
				else if(male < female)
					{
					countFemales++;
					}
				else if(female < male)
					{
					countMales++;
					}
				else
					{
					//System.err.println("Undefined "+firstName+" / "+lastName);
					countUnknown++;
					}
				}
			reader.close();
			}
		int originalUndef=countUnknown;
		if(ignoreUndefined) countUnknown=0;

		float total= countMales+countFemales+countUnknown;

		double radMale=(countMales/total)*Math.PI*2.0;
		double radFemale=(countFemales/total)*Math.PI*2.0;
		int radius= (canvasSize-2)/2;
		String id= "ctx"+System.currentTimeMillis()+""+(int)(Math.random()*1000);
		XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
		XMLStreamWriter w= xmlfactory.createXMLStreamWriter(System.out,"UTF-8");
		if(this.html)
			{
			w.writeStartElement("html");
			w.writeStartElement("body");
			}

		if(countMales+countFemales>0)
			{
			w.writeStartElement("div");
			w.writeAttribute("style","margin:10px;padding:10px;text-align:center;");
			w.writeStartElement("div");
			w.writeEmptyElement("canvas");
			w.writeAttribute("width", String.valueOf(canvasSize+1));
			w.writeAttribute("height", String.valueOf(canvasSize+1));
			w.writeAttribute("id", id);
			w.writeStartElement("script");
			w.writeCharacters(
			"function paint"+id+"(){var canvas=document.getElementById('"+id+"');"+
			"if (!canvas.getContext) return;var c=canvas.getContext('2d');"+
			"c.fillStyle='white';c.strokeStyle='black';"+
			"c.fillRect(0,0,"+canvasSize+","+canvasSize+");"+
			"c.fillStyle='gray';c.beginPath();c.arc("+(canvasSize/2)+","+(canvasSize/2)+","+radius+",0,Math.PI*2,true);c.fill();c.stroke();"+
			"c.fillStyle='blue';c.beginPath();c.moveTo("+(canvasSize/2)+","+(canvasSize/2)+");c.arc("+(canvasSize/2)+","+(canvasSize/2)+","+radius+",0,"+radMale+",false);c.closePath();c.fill();c.stroke();"+
			"c.fillStyle='pink';c.beginPath();c.moveTo("+(canvasSize/2)+","+(canvasSize/2)+");c.arc("+(canvasSize/2)+","+(canvasSize/2)+","+radius+","+radMale+","+(radMale+radFemale)+",false);c.closePath();c.fill();c.stroke();}"+
			"window.addEventListener('load',function(){ paint"+id+"(); },true);"
			);
			w.writeEndElement();
			w.writeEndElement();

			w.writeStartElement("span");
			w.writeAttribute("style","color:pink;");
			w.writeCharacters("Women: "+countFemales+" ("+(int)((countFemales/total)*100.0)+"%)");
			w.writeEndElement();
			w.writeCharacters(" ");
			w.writeStartElement("span");
			w.writeAttribute("style","color:blue;");
			w.writeCharacters("Men: "+countMales+" ("+(int)((countMales/total)*100.0)+"%)");
			w.writeEndElement();

			w.writeEmptyElement("br");


			w.writeStartElement("span");
			w.writeAttribute("style","color:gray;");
			if(!this.ignoreUndefined)
				{
				w.writeCharacters("Undefined : "+countUnknown+" ("+(int)((countUnknown/total)*100.0)+"%)");
				w.writeCharacters(" Ignored : "+countIgnored);
				}
			else
				{
				w.writeCharacters("Undefined : "+originalUndef);
				w.writeCharacters(" Ignored : "+countIgnored);
				}
			w.writeEndElement();


			w.writeEmptyElement("br");

			w.writeStartElement("a");
			w.writeAttribute("target","_blank");
			w.writeAttribute("href","http://www.ncbi.nlm.nih.gov/sites/entrez?db=pubmed&amp;cmd=search&amp;term="+URLEncoder.encode(this.query,"UTF-8"));
			w.writeCharacters(this.query);
			w.writeEndElement();


			w.writeEndElement();
			}

		if(this.html)
			{
			w.writeEndElement();//body
			w.writeEndElement();//html
			}
		w.flush();
		w.close();
		}

	public static void main(String[] args)
		{
		try
			{
			PubmedGender app=new PubmedGender();

			int optind=0;
			while(optind< args.length)
				{
				if(args[optind].equals("-h") ||
				   args[optind].equals("-help") ||
				   args[optind].equals("--help"))
					{
					System.err.println("Options:");
					System.err.println(" -h help; This screen.");
					System.err.println(" -w <int> canvas size default:"+app.canvasSize);
					System.err.println(" -i ignore undefined default:"+app.ignoreUndefined);
					System.err.println(" -H ignore html header & footer default:"+app.html);
					System.err.println(" -f only first author");
					System.err.println(" query terms...");
					return;
					}
				else if(args[optind].equals("-w"))
					{
					app.canvasSize=Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-H"))
					{
					app.html=false;
					}
				else if(args[optind].equals("-i"))
					{
					app.ignoreUndefined=true;
					}
				else if(args[optind].equals("-f"))
					{
					app.firstAuthor=true;
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
			app.loadNames();

			app.run();

			}
		catch (Exception e)
			{
			e.printStackTrace();
			}
		}
	}
