package sandbox;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.namespace.QName;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
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
	
	private static class DateRow  implements Comparable<DateRow>
		{
		int y=0;
		Date date;
		List<Article> articles = new ArrayList<>();
		
		DateRow(Date date)
			{
			this.date = date;
			}
		
		@Override
		public int compareTo(final DateRow o) {
			int i= date.compareTo(o.date);
			return i;
			}
		}
	
	private static class Article implements Comparable<Article>
		{
		String pmid;
		DateRow row;
		int nRows=1;
		String title;
		@Override
		public int compareTo(Article o) {
			return o.nRows - this.nRows;
		}
		}
	
	private List<DateRow> dates= new ArrayList<>();
	private List<Article> pmid2article= new ArrayList<>();
	private Set<String> buzzwords = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
	
	private void run(final Date date,final String pmids) throws Exception
		{
		final DateRow dateRow = new DateRow(date);
		dateRow.y = this.dates.size();
		this.dates.add(dateRow);
		
		LOG.info("fetch "+date+" "+pmids);
		final QName nameAtt=new QName("Name");
		final String uri = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esummary.fcgi?db=pubmed&retmode=xml&id=" + pmids;
		LOG.info(uri);
		final InputStream in  = new URL(uri).openStream();
		final XMLInputFactory xif = XMLInputFactory.newFactory();
		final XMLEventReader r= xif.createXMLEventReader(in);
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
				Article article = null;
				for(int i=0;i< this.pmid2article.size();++i)
					{
					final Article tmpA = this.pmid2article.get(i);
					if(tmpA.pmid.equals(pmid) && (tmpA.nRows+tmpA.row.y) == dateRow.y)
						{
						article=tmpA;
						break;
						}
					}
				
				if(article!=null)
					{
					article.nRows++;
					}
				else
					{
					article = new Article();
					article.row =  dateRow;
					article.pmid = pmid;
					article.title = title;
					dateRow.articles.add(article);
					this.pmid2article.add(article);
					}
				continue;
				}
			}
		r.close();
		in.close();
		}
	
	private void write(XMLStreamWriter w,final String text,int index) throws XMLStreamException
		{
		while(index< text.length())
			{
			if(index+5<text.length() && text.substring(index, index+5).equals("<sup>"))
				{
				w.writeStartElement("sup");
				index+=5;
				continue;
				}
			if(index+6<text.length() && text.substring(index, index+6).equals("</sup>"))
				{
				w.writeEndElement();
				index+=6;
				continue;
				}
			
			char c = text.charAt(index);
			if(!Character.isJavaIdentifierPart(c)) {
				w.writeCharacters(String.valueOf(c));
				++index;
				continue;
				}
			final StringBuilder sb=new StringBuilder();
			sb.append(c);
			index++;
			while(index< text.length() )
				{
				c = text.charAt(index);
				if(!Character.isJavaIdentifierPart(c)) break;
				sb.append(c);
				++index;
				}
			if(this.buzzwords.contains(sb.toString()))
				{
				w.writeStartElement("span");
				w.writeAttribute("class", "buzzword");
				w.writeCharacters(sb.toString());
				w.writeEndElement();
				}
			else
				{
				w.writeCharacters(sb.toString());
				}
			}
		}
	
	@Override
	protected int execute(CommandLine cmd)
		{
		this.buzzwords.add("Zika");
		this.buzzwords.add("CAS9");
		this.buzzwords.add("crispr");
		final List<String> args = cmd.getArgList();
		BufferedReader r=null;
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
				run(
					Date.valueOf(line.substring(0,n).trim()),
					line.substring(n+1).trim()
					);
				}
			r.close();
			Collections.sort(this.dates);
			
			for(int y0=0;y0< dates.size();++y0)
				{
				final DateRow r0 = dates.get(y0);
				for(Article a0: r0.articles)
					{
					for(int y1=y0+1;y1< dates.size();++y1)
						{
						final DateRow r1 = dates.get(y1);
						int x1=0;
						for(x1=0;x1 < r1.articles.size();++x1)
							{
							final Article a1 = r1.articles.get(x1);
							if(a1.pmid.equals(a0.pmid)) {
								
								break;
								}
							}
						
						}
					}
				}
			
			
			final XMLOutputFactory xof=XMLOutputFactory.newFactory();
			final XMLStreamWriter w = xof.createXMLStreamWriter(System.out,"UTF-8");
			w.writeStartElement("html");
			w.writeStartElement("head");
			w.writeStartElement("style");
			w.writeCharacters(
					"table { border-collapse: collapse;border: 1px solid black;}\n"+
					"td { border: 1px solid black; padding:10px; vertical-align: middle; }\n"+
					"th { border: 1px solid black;}\n"+
					"a {  text-decoration: none; color: black;}\n"+
					".buzzword {font-size:150%; color:green;}\n"
					);
			w.writeEndElement();//style
						
			w.writeEmptyElement("meta");
			w.writeAttribute("http-equiv", "Content-Type");
			w.writeAttribute("content", "text/html; charset=utf-8");
			
			w.writeEndElement();//head
			w.writeStartElement("body");
			w.writeStartElement("table");
			for(final DateRow row:this.dates)
				{
				w.writeStartElement("tr");
				w.writeStartElement("th");
				w.writeCharacters(row.date.toString());
				w.writeEndElement();
				
				Collections.sort(row.articles);
				
				for(final Article a:row.articles)
					{
					w.writeStartElement("td");
					w.writeAttribute("rowspan", String.valueOf(a.nRows));
					w.writeStartElement("a");
					w.writeAttribute("href", "http://www.ncbi.nlm.nih.gov/pubmed/"+a.pmid);
					write(w,a.title,0);
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
