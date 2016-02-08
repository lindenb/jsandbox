package sandbox;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import org.apache.commons.cli.CommandLine;

/*
2016-02-05	26840489,26841432,26839247,26832396,26840156,26829318,26824625,26840174,26829317,26839413,26829649,26839177,26837755,26839406,26840090,26841430,26840485,26837467,26837003,26814963
2016-02-06	26840489,26841432,26842033,26839247,26829318,26840156,26832396,26824625,26840174,26829317,26839413,26828088,26829649,26839177,26842604,26837755,26839406,26840090,26834413,26841430
2016-02-07	26849762,26840489,26842033,26841432,26832396,26839247,26840156,26829318,26824625,26840174,26829317,26839413,26828088,26842604,26839177,26829649,26837755,26839406,26834413,26840090
2016-02-08	26842033,26849762,26840489,26842604,26841432,26845405,26845534,26839247,26840156,17540228,26824625,26832396,26829318,26849748,24149212,26834413,26848089,26840174,26829317,26839413

 */

public class PubmedTrending extends AbstractApplication {
	
	private static class Article implements Comparable<Article>
		{
		String pmid;
		Date dateStart;
		int nDays=1;
		String title;
		int y=0;
		
		Date getDateEnd() {
			final Calendar cal = Calendar.getInstance();
			cal.setTimeInMillis(this.dateStart.getTime());
			cal.add(Calendar.DAY_OF_MONTH,nDays);
			final Date dateEnd= new Date(cal.getTimeInMillis());
			return dateEnd;
			}
		
		@Override
		public int compareTo(final Article o) {
			int i= dateStart.compareTo(o.dateStart);
			if(i!=0) return i;
			i= getDateEnd().compareTo(o.getDateEnd());
			if(i!=0) return i;
			return pmid.compareTo(o.pmid);
			}
		}
	
	private List<Article> run(final Date date,final String pmids) throws Exception
		{
		LOG.info("fetch "+date+" "+pmids);
		List<Article> articles= new ArrayList<>();
		final QName nameAtt=new QName("Name");
		final String uri = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&retmode=xml&id=" + pmids;
		LOG.info(uri);
		InputStream in  = new URL(uri).openStream();
		XMLInputFactory xif = XMLInputFactory.newFactory();
		XMLEventReader r= xif.createXMLEventReader(in);
		String pmid=null;
		String title=null;
		while(r.hasNext())
			{
			XMLEvent evt =r.nextEvent();
			if(!evt.isStartElement()) continue;
			final StartElement startE = evt.asStartElement();
			if(startE.getName().getLocalPart().equals("Id")) {
				pmid =r.getElementText();
				title = null;
				continue;
				}
			else if(startE.getName().getLocalPart().equals("Item") && 
					startE.getAttributeByName(nameAtt)!=null &&
					startE.getAttributeByName(nameAtt).getValue().equals("Title") &&
					pmid != null &&
					title == null) {
				title =r.getElementText();
				final Article article = new Article();
				article.dateStart =  date;
				article.pmid = pmid;
				article.title = title;
				articles.add(article);
				continue;
				}
			}
		r.close();
		in.close();
		return articles;
		}
	
	@Override
	protected int execute(CommandLine cmd)
		{
		final List<String> args = cmd.getArgList();
		BufferedReader r=null;
		final List<Article> articles = new ArrayList<>();
		try 
			{
			if(args.isEmpty())
				{
				r=(new BufferedReader(new InputStreamReader(System.in)));
				}
			else if(args.size()==1)
				{
				r=(new BufferedReader(new FileReader(args.get(0))));
				}
			else
				{
				LOG.severe("Illegal number of args");
				return -1;
				}
			String line;
			while((line=r.readLine())!=null)
				{
				if(line.trim().isEmpty()) continue;
				if(line.startsWith("#")) continue;
				int n= line.indexOf('\t');
				if(n==-1) n=line.indexOf(' ');
				if(n==-1) throw new IOException("no space/tab in "+line);
				articles.addAll( run(
					Date.valueOf(line.substring(0,n).trim()),
					line.substring(n+1).trim()
					));
				}
			r.close();
			Collections.sort(articles);
			LOG.info("x");
			//merge adjacent articles
			int i=0;
			while(i+1< articles.size())
				{
				final Article a = articles.get(i);
				int j=i+1;
				boolean changed=false;
				while(j< articles.size())
					{
					final Article b = articles.get(j);
					if(a.pmid.equals(b.pmid) && a.getDateEnd().equals(b.dateStart)) {
						a.nDays += b.nDays;
						articles.remove(j);
						changed=true;
						break;
						}
					++j;
					}
				if(!changed)  i++;
				}
			//get max y
			int maxy=1;
			for( i=0;i< articles.size();++i)
				{
				final Article a = articles.get(i);
				
				boolean changed=false;
				for(int j=i+1;j< articles.size();++j)
					{
					final Article b = articles.get(j);
					if(b.y!=a.y) continue;
					if(a.getDateEnd().compareTo(b.dateStart)<=0) continue;
					if(b.getDateEnd().compareTo(a.dateStart)<=0) continue;
					changed=true;
					a.y= b.y+1;
					}
				if(!changed)  i++;
				maxy=Math.max(a.y+1, maxy);
				}
			XMLOutputFactory xof=XMLOutputFactory.newFactory();
			XMLStreamWriter w = xof.createXMLStreamWriter(System.out,"UTF-8");
			w.writeStartElement("html");
			w.writeStartElement("body");
			w.writeStartElement("table");
			LOG.info("maxy "+maxy);
			for(int y=0;y<maxy;++y)
				{
				w.writeStartElement("tr");
				for(Article a:articles)
					{
					if(a.y!=y) continue;
					w.writeStartElement("td");
					w.writeStartElement("a");
					w.writeAttribute("href", "#");
					w.writeCharacters(a.title);
					w.writeEndElement();//A
					w.writeEndElement();//span
					}
				w.writeEndElement();//tr
				}
			
			w.writeEndElement();//div
			w.writeEndElement();//body
			w.writeEndElement();//html
			w.flush();
			w.close();
			return 0;
			}
		catch(Exception err)
			{
			err.printStackTrace();
			return -1;
			}
		
		}
	public static void main(String[] args) {
		new PubmedTrending().instanceMainWithExit(args);

	}

}
