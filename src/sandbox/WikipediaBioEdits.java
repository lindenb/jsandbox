/**
 * Author Pierre Lindenbaum PhD
 * http://plindenbaum.blogspot.com
 * 
 */
package sandbox;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

/**
 * 
 * WikipediaBioEdits
 *
 */
public class WikipediaBioEdits
	{
	private static final Logger LOG=Logger.getLogger(WikipediaBioEdits.class.getName());
	private XMLInputFactory xmlInputFactory;
	private List<Person> persons=new ArrayList<Person>();
	private static final int THUMB_WIDTH=64;
	private int minRevision=20;
	private Integer limitStartYear=null;
	private Integer limitEndYear=null;
	private int imageWidth=1000;
	private int imageHeight=(int)(imageWidth/((1+Math.sqrt(5))/2.0));
	private Integer limitCountPersons=null;
	private boolean removeIfTooMany=false;
	
	private static class Person
		{
		String name=null;
		long pageid=0L;
		Integer birth=null;
		Integer death=null;
		String imageURL=null;
		String imageDescriptionurl=null;
		int countEdits=0;
		Dimension thumbDim=null;
		
		
		
		@Override
		public int hashCode()
			{
			return 31  + (int) (pageid ^ (pageid >>> 32));
			}

		@Override
		public boolean equals(Object obj)
			{
			if (this == obj)  return true; 
			if (obj == null || getClass() != obj.getClass()) return false; 
			return (pageid == Person.class.cast(obj).pageid);
			}

		public String getCaption()
			{
			return String.valueOf(name)+" ("+birth+" / "+ death+ ") ";
			}
		
		@Override
		public String toString()
			{
			return getCaption();
			}
		}
	
	private WikipediaBioEdits()
		{
		this.xmlInputFactory = XMLInputFactory.newInstance();
		xmlInputFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
		xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
		xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, Boolean.TRUE);
		}
	
	private XMLEventReader openXMLEventReader(URL url)
		throws IOException
		{
		LOG.info(url.toString());
		IOException error=null;
		for(int tryCount=0;tryCount< 10;tryCount++)
			{
			try
				{
				InputStream in=url.openStream();
				return xmlInputFactory.createXMLEventReader(in);	
				}
			catch(Exception err)
				{
				System.err.println("Trying...."+(tryCount+2)+"/10 "+url);
				error=new IOException(err);
				try { Thread.sleep(10*1000); } catch(Exception err2) {}
				}
			}
		throw error;
		}
	
	private String getOneImage(Person person)
		throws Exception
		{
		final QName titleAtt=new QName("title");
		
		String urlStr="http://en.wikipedia.org/w/api.php?format=xml&action=query&prop=images&pageids="+
			person.pageid+
			"&iilimit=100";
		
		URL url=new URL(urlStr);
		
		XMLEventReader r=openXMLEventReader(url);
		url=null;
		while(r.hasNext())
			{
			XMLEvent evt=r.nextEvent();
			if(!evt.isStartElement()) continue;
			StartElement start=evt.asStartElement();
			String localName=start.getName().getLocalPart();
			if(localName.equals("im"))
				{
				Attribute att=start.getAttributeByName(titleAtt);
				String image=att.getValue();
				if(!(image.toLowerCase().contains("svg") ||
					 image.toLowerCase().contains("logo") ||
					 image.toLowerCase().contains("animated") ||
					 image.toLowerCase().contains("3D") ||
					 image.toLowerCase().endsWith(".ogg")))
					{
					r.close();
					return image;
					}
				}
			}
		r.close();
		return null;
		}
	
	private void findImage(Person person)
	throws Exception
		{
		String imageName=getOneImage(person);
		if(imageName==null) return;
		
		
		String urlStr="http://en.wikipedia.org/w/api.php?format=xml&action=query&prop=imageinfo&titles=" +
				URLEncoder.encode(imageName,"UTF-8") +
				"&iiurlwidth="+THUMB_WIDTH+
				"&iiurlheight="+THUMB_WIDTH+
				"&iiprop=url";
		
		
		URL url=new URL(urlStr);
		
		XMLEventReader r=openXMLEventReader(url);
		url=null;
		while(r.hasNext())
			{
			XMLEvent evt=r.nextEvent();
			if(!evt.isStartElement()) continue;
			StartElement start=evt.asStartElement();
			String localName=start.getName().getLocalPart();
			if(localName.equals("ii"))
				{
				person.thumbDim=new Dimension();
				Attribute att=start.getAttributeByName(new QName("thumbwidth"));
				person.thumbDim.width=Integer.parseInt(att.getValue());
				att=start.getAttributeByName(new QName("thumbheight"));
				person.thumbDim.height=Integer.parseInt(att.getValue());
				att=start.getAttributeByName(new QName("thumburl"));
				person.imageURL= att.getValue();
				att=start.getAttributeByName(new QName("descriptionurl"));
				person.imageDescriptionurl=att.getValue();
				r.close();
				}
			}
		r.close();
		}
	
	
	private void countEdits(Person person)
		throws Exception
		{
		final QName rvstartidAtt=new QName("rvstartid");
		
		String urlStr="http://en.wikipedia.org/w/api.php?format=xml&action=query&prop=revisions&pageids="+
			person.pageid+
			"&rvprop=size&rvlimit=500";
		
		URL url=new URL(urlStr);
		while(url!=null)
			{
			String rvstartid=null;
			XMLEventReader r=openXMLEventReader(url);
			url=null;
			while(r.hasNext())
				{
				XMLEvent evt=r.nextEvent();
				if(evt.isStartElement())
					{
					StartElement start=evt.asStartElement();
					String localName=start.getName().getLocalPart();
					if(localName.equals("rev"))
						{
						person.countEdits++;
						}
					else if(localName.equals("revisions"))
						{
						Attribute att=start.getAttributeByName(rvstartidAtt);
						if(att!=null) rvstartid=att.getValue();
						}
					}
				}
			r.close();
			if(rvstartid!=null)
				{
				url=new URL(urlStr+"&rvstartid="+rvstartid);
				}
			}
		}
	
	private boolean addPerson(Person person)
		{
		if(  this.limitCountPersons!=null &&
                     this.removeIfTooMany==false &&
                     this.persons.size()>=this.limitCountPersons)
                        {
                        return false;
                        }
		this.persons.add(person);
		LOG.info(person.toString()+"N:"+this.persons.size());
		
		if(  this.limitCountPersons!=null &&
                     this.removeIfTooMany==true &&
                     this.persons.size()>=this.limitCountPersons)
                        {
                        int indexOf=-1;
                        for(int i=0;i< this.persons.size();++i)
                           {
                           if(indexOf==-1 ||
                              this.persons.get(indexOf).countEdits > this.persons.get(i).countEdits )
                              {
                              indexOf=i;
                              } 
                           }
                        LOG.info("Removing "+this.persons.get(indexOf));
                        this.persons.remove(indexOf);
                        }
		
		
		
		return true;					
		}
	
	private boolean getYears(Person person)
		throws Exception
		{
		final QName titleAtt=new QName("title");
		final QName clcontinueAtt=new QName("clcontinue");
		
		String urlStr="http://en.wikipedia.org/w/api.php?format=xml&action=query&prop=categories&pageids="+
			person.pageid+
			"&cllimit=500";
		
		
		URL url=new URL(urlStr);
		while(url!=null)
			{
			String eicontinue=null;
			XMLEventReader r=openXMLEventReader(url);
			url=null;
			while(r.hasNext())
				{
				XMLEvent evt=r.nextEvent();
				if(evt.isStartElement())
					{
					StartElement start=evt.asStartElement();
					String localName=start.getName().getLocalPart();
					if(localName.equals("cl"))
						{
						Attribute att=start.getAttributeByName(titleAtt);
						String title=att.getValue();
						if(title.startsWith("Category:"))
							{
							title=title.substring(9);
							if(title.matches("(\\d)+ births"))
								{
								person.birth=Integer.parseInt(title.substring(0,title.indexOf(' ')));
								}
							else if(title.matches("(\\d)+ BC births"))
								{
								person.birth=-Integer.parseInt(title.substring(0,title.indexOf(' ')));
								}
							else if(title.matches("(\\d)+ deaths"))
								{
								person.death=Integer.parseInt(title.substring(0,title.indexOf(' ')));
								}
							else if(title.matches("(\\d)+ BC deaths"))
								{
								person.death=-Integer.parseInt(title.substring(0,title.indexOf(' ')));
								}
							}
						}
					else if(localName.equals("categories"))
						{
						Attribute att=start.getAttributeByName(clcontinueAtt);
						if(att!=null) eicontinue=att.getValue();
						}
					}
				}
			r.close();
			if(person.birth!=null && person.death!=null) break;
			if(eicontinue!=null)
				{
				url=new URL(urlStr+"&clcontinue="+eicontinue);
				}
			}
		return (person.birth!=null&&
				person.death!=null &&
				person.birth < person.death
				);
		}
	
	private void havingInfobox(String template)
		throws Exception
		{
		final QName pageidAtt=new QName("pageid");
		final QName titleAtt=new QName("title");
		final QName eicontinueAtt=new QName("eicontinue");
		String urlStr="http://en.wikipedia.org/w/api.php?format=xml&action=query&einamespace=0&list=embeddedin&eititle=Template:"+
				template.replace(' ', '+')+
				"&eilimit=500";
		URL url=new URL(urlStr);
		while(url!=null)
			{
			String eicontinue=null;
			XMLEventReader r=openXMLEventReader(url);
			url=null;
			while(r.hasNext())
				{
				XMLEvent evt=r.nextEvent();
				if(evt.isStartElement())
					{
					StartElement start=evt.asStartElement();
					String localName=start.getName().getLocalPart();
					if(localName.equals("ei"))
						{
						Person person=new Person();
						Attribute att=start.getAttributeByName(pageidAtt);
						person.pageid=Long.parseLong(att.getValue());
						att=start.getAttributeByName(titleAtt);
						person.name=att.getValue();
						
						if(getYears(person))
							{
							if(this.limitStartYear!=null && this.limitStartYear> person.death)
								{
								LOG.info("ignore (death) "+person);
								continue;
								}
							if(this.limitEndYear!=null && this.limitEndYear< person.birth)
								{
								LOG.info("ignore (birth) "+person);
								continue;
								}
							countEdits(person);
							if(this.minRevision> person.countEdits)
								{
								LOG.info("ignore (edit) "+person);
								continue;
								}
							findImage(person);
							if(!addPerson(person)) return;
							}
						
						}
					else if(localName.equals("embeddedin"))
						{
						Attribute att=start.getAttributeByName(eicontinueAtt);
						if(att!=null) eicontinue=att.getValue();
						}
					}
				}
			r.close();
			if(eicontinue!=null)
				{
				url=new URL(urlStr+"&eicontinue="+eicontinue);
				}
			}
		
		}
	
	private void paint()
		throws Exception
		{
		int minYear=Integer.MAX_VALUE;
		int maxYear=Integer.MIN_VALUE;
		int maxRev=0;
		
		
		Collections.sort(this.persons,new Comparator<Person>()
			{
			@Override
			public int compare(Person o1, Person o2)
				{
				return o1.countEdits-o2.countEdits;
				}
			});
		
		
		for(Person person: this.persons)
			{
			minYear= Math.min(minYear, person.birth-1);
			maxYear= Math.max(maxYear, person.death+1);
			maxRev= Math.max(maxRev, person.countEdits);
			}
		double duration=(maxYear-minYear);
		int adjustedHeight=this.imageHeight-(THUMB_WIDTH+4);
		
		XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
		XMLStreamWriter w= xmlfactory.createXMLStreamWriter(System.out);
		w.writeStartElement("html");
		w.writeStartElement("body");
		
		w.writeStartElement("div");
		w.writeAttribute("style",
			"position:relative;white-space:nowrap;width:" +
			this.imageWidth+	
			"px;height:" +
			this.imageHeight+
			"px;background:-moz-linear-gradient(left,white,lightgray);"+
			"border: 1px solid;"
			);
		//y axis
		for(int i=1;i<= 10;++i)
			{
			int rev= (int)((i*(maxRev/10.0)));
			int y=(int)(this.imageHeight-(i*this.imageHeight/10.0));
			w.writeStartElement("span");
			w.writeAttribute("style", "position:absolute;font-weight: bold; color:gray;top:"+y+"px;left:5px;");
			w.writeCharacters(String.valueOf(rev));
			w.writeEndElement();
			}
		//x axis
		for(int i=1;i< 10;++i)
			{
			int year=minYear+(int)((i*(duration/10.0)));
			int x=(int)(i*this.imageWidth/10.0);
			w.writeStartElement("span");
			w.writeAttribute("style", "font-weight: bold; font-size:18px;color:gray; position:absolute;-moz-transform: translate("+x+"px, 19px) rotate(90deg);");
			w.writeCharacters(String.valueOf(year));
			w.writeEndElement();
			}
		
		
		for(Person person:this.persons)
			{
			w.writeStartElement("div");
			w.writeAttribute("id", "wp"+person.pageid);
			StringBuilder style=new StringBuilder("position:absolute;opacity:.5;");
			Rectangle viewRect=new  Rectangle();
			viewRect.x=(int)(((person.birth-minYear)/duration)*this.imageWidth);
			viewRect.width=(int)(((person.death-person.birth)/duration)*this.imageWidth);
			viewRect.height=(int)(THUMB_WIDTH+4);
			viewRect.y=adjustedHeight-(int)((person.countEdits/(float)maxRev)*adjustedHeight);
			
			
			
			if(viewRect.width< THUMB_WIDTH*2)
				{
				viewRect.width=THUMB_WIDTH*2;
				if(viewRect.width+viewRect.x>=this.imageWidth)
					{
					viewRect.x=this.imageWidth-viewRect.width;
					}
				}
			
			style.append("left:").append(viewRect.x).append("px;");
			style.append("width:").append(viewRect.width).append("px;");
			style.append("height:").append(viewRect.height).append("px;");
			style.append("top:").append(viewRect.y).append("px;");
			style.append("border: 1px solid;");
			style.append("background:-moz-linear-gradient(top,gray,lightgray);");
			style.append("overflow:hidden;-moz-border-radius:3px;");
			w.writeAttribute("style", style.toString());
			
			if(person.imageURL!=null)
				{
				w.writeStartElement("a");
				w.writeAttribute("href",person.imageDescriptionurl);
				w.writeAttribute("target","_blank");
				style=new StringBuilder("border-style:none;float:left;margin:");
				style.append(2+(THUMB_WIDTH-person.thumbDim.height)/2).append("px ");//top
				style.append("2px ");//right
				style.append(2+(THUMB_WIDTH-person.thumbDim.height)/2).append("px ");//bottom
				style.append("2px;");//left
				w.writeEmptyElement("img");
				w.writeAttribute("title", person.name);
				w.writeAttribute("width", String.valueOf(person.thumbDim.width));
				w.writeAttribute("height", String.valueOf(person.thumbDim.height));
				w.writeAttribute("style", style.toString());
				w.writeAttribute("src", person.imageURL);
				w.writeEndElement();
				}
			
			w.writeStartElement("a");
			w.writeAttribute("href", "http://en.wikipedia.org/wiki/"+person.name);
			w.writeAttribute("title",person.getCaption());
			w.writeAttribute("target","_blank");
			w.writeAttribute("style","color:black;");
			w.writeCharacters(person.getCaption());
			w.writeEndElement();
			
			w.writeEndElement();
			w.writeCharacters("\n");
			}
		w.writeEndElement();
		
		w.writeEndElement();
		w.writeEndElement();
		w.flush();
		w.close();
		}
	
	private void run()
		throws Exception
		{
		havingInfobox("Infobox scientist");
		paint();
		}
	
	public static void main(String[] args)
		{
		LOG.setLevel(Level.OFF);
		WikipediaBioEdits app=null;
		try
			{
			app=new WikipediaBioEdits();
			int optind=0;
			while(optind< args.length)
				{
				if(args[optind].equals("-h") ||
				   args[optind].equals("-help") ||
				   args[optind].equals("--help"))
					{
					System.err.println("Options:");
					System.err.println(" -h help; This screen.");
					System.err.println(" -v print logging information");
					System.err.println(" -d (used with -N ) delete the individuals with the smallest number of revisions if there's too many individuals");
					System.err.println(" -N <int> max-num-individuals default:"+app.limitCountPersons);
					System.err.println(" -s <int> start-year default:"+app.limitStartYear);
					System.err.println(" -e <int> end-year default:"+app.limitEndYear);
					System.err.println(" -r <int> min revison default:"+app.minRevision);
					System.err.println(" -w <int> screen width:"+app.imageWidth);
					System.err.println(" -H <int> screen height:"+app.imageHeight);
					return;
					}
				else if(args[optind].equals("-d"))
				        {
				        app.removeIfTooMany=true;
				        }
				else if(args[optind].equals("-v"))//verbose
					{
					LOG.setLevel(Level.ALL);
					}
				else if(args[optind].equals("-N"))
					{
					app.limitCountPersons=Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-s"))
					{
					app.limitStartYear=Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-e"))
					{
					app.limitEndYear=Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-r"))
					{
					app.minRevision=Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-w"))
					{
					app.imageWidth=Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-H"))
					{
					app.imageHeight=Integer.parseInt(args[++optind]);
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
			app.run();
			LOG.info("Done");
			} 
		catch(Throwable err)
			{
			err.printStackTrace();
			}
		}
	}
