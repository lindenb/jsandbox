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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
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
	private boolean pileup=true;
	private int maxDepth=4;

	private static class Category
		{
		String title=null;
		Integer pageId=null;

		public Category(String title)
			{
			this(title,null);
			}

		public Category(String title,Integer pageId)
			{
			this.title=title;
			this.pageId=pageId;
			}

		@Override
		public int hashCode()
			{
			return title.hashCode();
			}

		@Override
		public boolean equals(Object obj)
			{
			if (this == obj) { return true; }
			if (obj == null) { return false; }
			if (getClass() != obj.getClass()) { return false; }
			Category other = (Category) obj;
			if(pageId != null && other.pageId!=null)
				{
				return this.pageId.equals(other.pageId);
				}
			if(title != null && other.title!=null)
				{
				return this.title.equals(other.title);
				}
			throw new IllegalStateException();
			}



		@Override
		public String toString()
			{
			return String.valueOf(title);
			}
		}

	private abstract class PersonListHandler
		{
		protected WikipediaBioEdits owner()
			{
			return WikipediaBioEdits.this;
			}
		protected XMLStreamWriter getXMLStreamWriter()
			throws Exception
			{
			XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
			XMLStreamWriter w= xmlfactory.createXMLStreamWriter(System.out);
			return w;
			}


		public abstract void paint(List<Person> persons) throws Exception;
		}

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
		Rectangle viewRect=null;


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
			return String.valueOf(name)+" ("+birth+"/"+ death+ ")";
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

	private StringBuilder removeTemplates(StringBuilder content)
		{
		int level=0;
		int pos=0;
		while(pos< content.length())
			{
			if( pos+2<content.length() &&
				content.charAt(pos)=='{' &&
				content.charAt(pos+1)=='{' &&
				!Character.isWhitespace(content.charAt(pos+2))
				)
				{
				content.delete(pos, pos+2);
				++level;
				}
			else if( pos+1<content.length() &&
				content.charAt(pos)=='}' &&
				content.charAt(pos+1)=='}')
				{
				content.delete(pos, pos+2);
				--level;
				}
			else if(level!=0)
				{
				content.delete(pos, pos+1);
				}
			else
				{
				pos++;
				}
			}
		return content;
		}


	private StringBuilder removeInternalLinks(StringBuilder content)
		{
		int pos=-1;
		while((pos=content.indexOf("[["))!=-1)
			{
			int i1=content.indexOf("]]",pos+1);
			if(i1==-1) break;
			int i2=content.indexOf("|",pos+1);
			if(i2!=-1 && i2<i1)
				{
				content.delete(i1, i1+2);//]]
				content.delete(pos, i2+1);//|
				}
			else if(i2==-1 || i2>i1)
				{
				content.delete(i1, i1+2);//]]
				content.delete(pos,pos+2);//[[
				}
			else
				{
				System.err.println("?? "+pos+" "+i1+" "+i2);
				break;
				}
			}
		return content;
		}

	private StringBuilder getArticleContent(long articleId)
		throws XMLStreamException,IOException
		{
		boolean inRev=false;
		StringBuilder sb=new StringBuilder();
		String urlStr="http://en.wikipedia.org/w/api.php?format=xml&action=query&prop=revisions&rvprop=content&pageids="+articleId;
		XMLEventReader r=openXMLEventReader(new URL(urlStr));
		while(r.hasNext())
			{
			XMLEvent evt=r.nextEvent();
			if(evt.isStartElement())
				{
				StartElement start=evt.asStartElement();
				String localName=start.getName().getLocalPart();
				if(localName.equals("rev"))
					{
					inRev=true;
					}
				}
			else if(evt.isEndElement())
				{
				if(inRev) break;
				}
			else if(evt.isCharacters() && inRev)
				{
				sb.append(evt.asCharacters().getData());
				}
			}
		r.close();
		return sb;
		}

	private String getArticleFirstLine(long articleId)
		throws XMLStreamException,IOException
		{
		String content= removeInternalLinks(removeTemplates(getArticleContent(articleId))).toString().trim();
		int i=content.indexOf(".");
		if(i==-1) return content;
		return content.substring(0,i);
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
					 image.toLowerCase().endsWith(".ogg") ||
					 image.toLowerCase().endsWith(".mp3")))
					{
					r.close();
					return image;
					}
				}
			}
		r.close();
		return null;
		}


	private Set<Category> findSubCategories(Category parent)
		throws Exception
		{
		Set<Category> children=new HashSet<Category>();
		QName cmcontinueAttr=new QName("cmcontinue");
		QName pageIdAttr=new QName("pageid");
		QName titleAttr=new QName("title");
		String urlStr="http://en.wikipedia.org/w/api.php?action=query&list=categorymembers&cmnamespace=14&cmlimit=500&format=xml"+
			"&cmtitle="+URLEncoder.encode(parent.title.replace(' ', '_'),"UTF-8")
			;

		URL url=new URL(urlStr);
		while(url!=null)
			{
			String cmcontinue=null;
			XMLEventReader r=openXMLEventReader(url);
			url=null;
			while(r.hasNext())
				{
				XMLEvent evt=r.nextEvent();
				if(!evt.isStartElement()) continue;
				StartElement start=evt.asStartElement();
				String localName=start.getName().getLocalPart();
				if(localName.equals("cm"))
					{
					Attribute attId =start.getAttributeByName(pageIdAttr);
					Attribute attTitle =start.getAttributeByName(titleAttr);
					children.add(new Category(
							attTitle.getValue(),
							Integer.parseInt(attId.getValue())
							));
					}
				else if(localName.equals("categorymembers"))
					{
					Attribute att=start.getAttributeByName(cmcontinueAttr);
					if(att!=null) cmcontinue=att.getValue();
					}

				}
			r.close();
			if(cmcontinue!=null)
				{
				url=new URL(urlStr+"&cmcontinue="+cmcontinue);
				}
			}
		return children;
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
		throws Exception
		{
		for(Person other:this.persons)
			{
			if(other.pageid==person.pageid) return false;
			}

		if(!getYears(person)) return false;

		if(this.limitStartYear!=null && this.limitStartYear> person.death)
			{
			LOG.info("ignore (death) "+person);
			return false;
			}
		if(this.limitEndYear!=null && this.limitEndYear< person.birth)
			{
			LOG.info("ignore (birth) "+person);
			return false;
			}
		countEdits(person);
		if(this.minRevision> person.countEdits)
			{
			LOG.info("ignore (edit) "+person);
			return false;
			}

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
                int indexOf=this.persons.size()-1;//last
                for(int i=0;i< this.persons.size();++i)
                   {
                   if(this.persons.get(indexOf).countEdits > this.persons.get(i).countEdits )
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
							int space=title.indexOf(' ');
							if(space==-1)
								{
								//ignore
								}
							else if(title.matches("(\\d)+ births"))
								{
								person.birth=Integer.parseInt(title.substring(0,space));
								}
							else if(title.matches("(\\d)+ BC births"))
								{
								person.birth=-Integer.parseInt(title.substring(0,space));
								}
							else if(title.matches("(\\d)+ deaths"))
								{
								person.death=Integer.parseInt(title.substring(0,space));
								}
							else if(title.matches("(\\d)+ BC deaths"))
								{
								person.death=-Integer.parseInt(title.substring(0,space));
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


						addPerson(person);
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


	private void findInCategory(Category category)
		throws Exception
		{
		LOG.info("finding individuals in category "+category.title);
		Set<Category> children=new HashSet<Category>();
		QName cmcontinueAttr=new QName("cmcontinue");
		QName pageIdAttr=new QName("pageid");
		QName titleAttr=new QName("title");
		String urlStr="http://en.wikipedia.org/w/api.php?cmlimit=500&format=xml&action=query&list=categorymembers&cmnamespace=0&cmtitle="+
			"&cmtitle="+URLEncoder.encode(category.title.replace(' ', '_'),"UTF-8")
			;

		URL url=new URL(urlStr);
		while(url!=null)
			{
			String cmcontinue=null;
			XMLEventReader r=openXMLEventReader(url);
			url=null;
			while(r.hasNext())
				{
				XMLEvent evt=r.nextEvent();
				if(!evt.isStartElement()) continue;
				StartElement start=evt.asStartElement();
				String localName=start.getName().getLocalPart();
				if(localName.equals("cm"))
					{
					Person person=new Person();
					Attribute att=start.getAttributeByName(pageIdAttr);
					person.pageid=Long.parseLong(att.getValue());
					att=start.getAttributeByName(titleAttr);
					person.name=att.getValue();

					addPerson(person);
					}
				else if(localName.equals("categorymembers"))
					{
					Attribute att=start.getAttributeByName(cmcontinueAttr);
					if(att!=null) cmcontinue=att.getValue();
					}

				}
			r.close();
			if(cmcontinue!=null)
				{
				url=new URL(urlStr+"&cmcontinue="+cmcontinue);
				}
			}
		}


	private class RevisionHandler
		extends PersonListHandler
		{
		@Override
		public void paint(List<Person> persons)
			throws Exception
			{
			int minYear=Integer.MAX_VALUE;
			int maxYear=Integer.MIN_VALUE;
			int maxRev=0;
			int minRev=Integer.MAX_VALUE;


			Collections.sort(persons,new Comparator<Person>()
				{
				@Override
				public int compare(Person o1, Person o2)
					{
					return o1.countEdits-o2.countEdits;
					}
				});


			for(Person person: persons)
				{
				minYear= Math.min(minYear, person.birth-1);
				maxYear= Math.max(maxYear, person.death+1);
				maxRev= Math.max(maxRev, person.countEdits);
				minRev= Math.min(minRev, person.countEdits);
				}
			double duration=(maxYear-minYear);
			int adjustedHeight=owner().imageHeight-(THUMB_WIDTH+4);

			XMLStreamWriter w= getXMLStreamWriter();
			w.writeStartElement("html");
			w.writeStartElement("body");

			w.writeStartElement("div");
			w.writeAttribute("style",
				//white-space:nowrap;
				"position:relative;width:" +
				owner().imageWidth+
				"px;height:" +
				owner().imageHeight+
				"px;background:-moz-linear-gradient(left,white,lightgray);"+
				"border: 1px solid;"
				);
			//y axis
			for(int i=1;i<= 10;++i)
				{
				int rev= minRev+(int)((i*((maxRev-minRev)/10.0)));
				int y=(int)(owner().imageHeight-(i*owner().imageHeight/10.0));
				w.writeStartElement("span");
				w.writeAttribute("style", "position:absolute;font-weight: bold; color:gray;top:"+y+"px;left:5px;");
				w.writeCharacters(String.valueOf(rev));
				w.writeEndElement();
				}
			//x axis
			for(int i=1;i< 10;++i)
				{
				int year=minYear+(int)((i*(duration/10.0)));
				int x=(int)(i*owner().imageWidth/10.0);
				w.writeStartElement("span");
				w.writeAttribute("style", "font-weight: bold; font-size:18px;color:gray; position:absolute;-moz-transform: translate("+x+"px, 19px) rotate(90deg);");
				w.writeCharacters(String.valueOf(year));
				w.writeEndElement();
				}


			for(Person person:persons)
				{
				w.writeStartElement("div");
				w.writeAttribute("id", "wp"+person.pageid);
				StringBuilder style=new StringBuilder("position:absolute;opacity:.5;");
				person.viewRect=new  Rectangle();
				person.viewRect.x=(int)(((person.birth-minYear)/duration)*owner().imageWidth);
				person.viewRect.width=(int)(((person.death-person.birth)/duration)*owner().imageWidth);
				person.viewRect.height=(int)(THUMB_WIDTH+4);
				person.viewRect.y=adjustedHeight-(int)(((person.countEdits-minRev)/(float)(maxRev-minRev))*adjustedHeight);



				if(person.viewRect.width< THUMB_WIDTH*2)
					{
					person.viewRect.width=THUMB_WIDTH*2;
					if(person.viewRect.width+person.viewRect.x>=owner().imageWidth)
						{
						person.viewRect.x=owner().imageWidth-person.viewRect.width;
						}
					}

				style.append("left:").append(person.viewRect.x).append("px;");
				style.append("width:").append(person.viewRect.width).append("px;");
				style.append("height:").append(person.viewRect.height).append("px;");
				style.append("top:").append(person.viewRect.y).append("px;");
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

				String textContent= "";//getArticleFirstLine(person.pageid);
				if(textContent.isEmpty())
					{
					textContent=person.getCaption();
					}

				w.writeCharacters(textContent);
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
		}

	private class PileupHandler
		extends PersonListHandler
		{
		int minYear=Integer.MAX_VALUE;
		int maxYear=Integer.MIN_VALUE;

		PileupHandler() {}


		private double convertDate2Pixel(int date)
			{
			return owner().imageWidth*((date-this.minYear)/(double)(this.maxYear-this.minYear));
			}

		private double x1(Person person)
    		{
    		return convertDate2Pixel(person.birth);
    		}

		private double x2(Person person)
    		{
    		return convertDate2Pixel(person.death);
    		}

		@Override
		public void paint(List<Person> persons)
			throws Exception
			{
			Collections.sort(persons,new Comparator<Person>()
				{
				@Override
				public int compare(Person o1, Person o2)
					{
					int n= o1.birth-o2.birth;
					if(n!=0) return n;
					n= o1.death-o2.death;
					if(n!=0) return n;
					return o1.name.compareTo(o2.name);
					}
				});


			for(Person person: persons)
				{
				minYear= Math.min(minYear, person.birth-1);
				maxYear= Math.max(maxYear, person.death+1);
				}
			for(Person person: persons)
				{
				person.viewRect=new Rectangle();
				person.viewRect.y=0;
				person.viewRect.height=THUMB_WIDTH;
				person.viewRect.x=(int)x1(person);
				person.viewRect.width=(int)(x2(person)-x1(person));
				}
			double duration=(maxYear-minYear);
			int adjustedHeight=owner().imageHeight-(THUMB_WIDTH+4);

			List<Person> remains=new ArrayList<Person>(persons);
			int nLine=-1;
			while(!remains.isEmpty())
				{
				++nLine;
				Person first=remains.get(0);
				remains.remove(0);
				first.viewRect.y=nLine*THUMB_WIDTH;
				first.viewRect.height=THUMB_WIDTH;

				while(true)
					{
					Person best=null;
					int bestIndex=-1;
					for(int i=0;i< remains.size();++i)
						{
						Person next=remains.get(i);
						if(next.viewRect.getX() < first.viewRect.getMaxX()+5) continue;
						if(best==null ||
						  (next.viewRect.getX()- first.viewRect.getMaxX() < best.viewRect.getX()- first.viewRect.getMaxX()))
							{
							best=next;
							bestIndex=i;
							}
						}
					if(best==null) break;
					first=best;
					first.viewRect.y=nLine*THUMB_WIDTH;
					remains.remove(bestIndex);
					}
				}


			XMLStreamWriter w= getXMLStreamWriter();
			w.writeStartElement("html");
			w.writeStartElement("body");

			w.writeStartElement("div");
			w.writeAttribute("style",
				//white-space:nowrap;
				"position:relative;width:" +
				owner().imageWidth+
				"px;height:" +
				((nLine+1)*THUMB_WIDTH)+
				"px;background:-moz-linear-gradient(left,white,lightgray);"+
				"border: 1px solid;"
				);

			//x axis
			for(int i=1;i< 10;++i)
				{
				int year=minYear+(int)((i*(duration/10.0)));
				int x=(int)(i*owner().imageWidth/10.0);
				w.writeStartElement("span");
				w.writeAttribute("style", "font-weight: bold; font-size:18px;color:gray; position:absolute;-moz-transform: translate("+x+"px, 19px) rotate(90deg);");
				w.writeCharacters(String.valueOf(year));
				w.writeEndElement();
				}


			for(Person person:persons)
				{
				w.writeStartElement("div");
				w.writeAttribute("id", "wp"+person.pageid);
				StringBuilder style=new StringBuilder("position:absolute;opacity:.5;");

				if(person.viewRect.width< THUMB_WIDTH*2)
					{
					person.viewRect.width=THUMB_WIDTH*2;
					if(person.viewRect.width+person.viewRect.x>=owner().imageWidth)
						{
						person.viewRect.x=owner().imageWidth-person.viewRect.width;
						}
					}

				style.append("left:").append(person.viewRect.x).append("px;");
				style.append("width:").append(person.viewRect.width).append("px;");
				style.append("height:").append(person.viewRect.height).append("px;");
				style.append("top:").append(person.viewRect.y).append("px;");
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

				String textContent= "";//getArticleFirstLine(person.pageid);
				if(textContent.isEmpty())
					{
					textContent=person.getCaption();
					}

				w.writeCharacters(textContent);
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
		}



	private void paint()
		throws Exception
		{
		if(this.pileup)
			{
			new PileupHandler().paint(this.persons);
			}
		else
			{
			new RevisionHandler().paint(this.persons);
			}
		}

	private void recursiveCategory(
		Set<Category> pool,
		Set<Category> remains,
		int depth
		) throws Exception
		{
		if(depth>=this.maxDepth) return;
		pool.addAll(remains);
		Set<Category> remains2=new HashSet<Category>();
		for(Category c1: remains)
			{
			Set<Category> subCats=findSubCategories(c1);
			for(Category c2: subCats)
				{
				if(pool.add(c2))
					{
					remains2.add(c2);
					}
				}
			}
		if(!remains2.isEmpty())
			{
			recursiveCategory(pool,remains2,depth+1);
			}
		}

	private void findInCategories(Category cat)
		throws Exception
		{
		Set<Category> pool=new HashSet<Category>(1);
		Set<Category> remains=new HashSet<Category>(1);
		pool.add(cat);
		remains.add(cat);
		recursiveCategory(pool,remains,0);
		LOG.info("adding category "+pool);
		for(Category c1:pool)
			{
			findInCategory(c1);
			}
		}


	private void run()
		throws Exception
		{
		//havingInfobox("Infobox scientist");
		findInCategories(new Category("Category:Japanese scientists"));
		for(Person p:this.persons)
			{
			findImage(p);
			}
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
					System.err.println(" -D max depth for finding sub categories: "+app.maxDepth);
					System.err.println(" -d (used with -N ) delete the individuals with the smallest number of revisions if there's too many individuals");
					System.err.println(" -N <int> max-num-individuals default:"+app.limitCountPersons);
					System.err.println(" -s <int> start-year default:"+app.limitStartYear);
					System.err.println(" -e <int> end-year default:"+app.limitEndYear);
					System.err.println(" -r <int> min revison default:"+app.minRevision);
					System.err.println(" -w <int> screen width:"+app.imageWidth);
					System.err.println(" -H <int> screen height:"+app.imageHeight);
					System.err.println(" -p pileup instead of revision");
					return;
					}
				else if(args[optind].equals("-d"))
			        {
			        app.removeIfTooMany=true;
			        }
				else if(args[optind].equals("-p"))
			        {
			        app.pileup=true;
			        }
				else if(args[optind].equals("-v"))//verbose
					{
					LOG.setLevel(Level.ALL);
					}
				else if(args[optind].equals("-D"))
					{
					app.maxDepth=Integer.parseInt(args[++optind]);
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
