package sandbox;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

public class StackExchangeSkills {
	
	private static final Logger LOG=Logger.getLogger("sandbox");
	private static long id_generator=System.currentTimeMillis();
	private String user_id="";//58082;
	private Map<Long,Answer> id2answer=new HashMap<Long, Answer>();
	private Map<Long,Question> id2question=new HashMap<Long, Question>();
	private String siteName="stackoverflow";
	private int max_items=50;
	private class Answer
		{
		long answer_id;
		boolean accepted;
		long question_id;
		int score;
		public Question getQuestion()
			{
			return id2question.get(this.question_id);
			}
		}
	private static class Question
		{
		long question_id;
		int answer_count;
		int favorite_count;
		int score;
		int view_count;
		String title;
		Set<String> tags=new HashSet<String>();
		}
	
	
private class TagAndScore implements Comparable<TagAndScore>
	{
	String tag;
	int score;
	@Override
	public int compareTo(TagAndScore other)
		{
		return other.score-this.score;
		}
	}
private abstract class Section
	{
	String id=String.valueOf(id_generator++);
	
	public String getId()
		{
		return id;
		}	
	
	public abstract String getLabel();
	public String getTitle()
		{
		return getLabel();
		}
	
	boolean includeAnswer(Answer a,String tag)
		{
		return a.getQuestion().tags.contains(tag);
		}
	
	Collection<Answer> getAnswers(String tag)
		{
		List<Answer> L=new ArrayList<Answer>(id2answer.size());
		for(Answer a:id2answer.values())
			{
			if(!includeAnswer(a,tag)) continue;
			L.add(a);
			}
		return L;
		}
	
	public int weight(Answer a)
		{
		return 1;
		}
	public TagAndScore score(String tag)
		{
		TagAndScore ts=new TagAndScore();
		ts.tag=tag;
		ts.score=0;
		for(Answer q:getAnswers(tag))
			{
			ts.score+=weight(q);
			}
		
		return ts;
		}
	
	public List<TagAndScore> getTagAndScores()
		{
		List<TagAndScore> L=new ArrayList<TagAndScore>();
		Set<String> all=new HashSet<String>();
		for(Question question:id2question.values())
			{
			all.addAll(question.tags);
			}
		for(String tag:all)
			{
			TagAndScore ts=score(tag);
			if(ts==null || ts.score<=0) continue;
			L.add(ts);
			}
		Collections.sort(L);
		while(L.size()>max_items)
			{
			L.remove(L.size()-1);
			}
		return L;
		}
	
	void writeHTML(XMLStreamWriter w) throws XMLStreamException
		{
		List<TagAndScore> tagandscores=getTagAndScores();

		w.writeStartElement("script");
		w.writeAttribute("type","text/javascript");
		w.writeCharacters("function drawChart"+getId()+"() {");
		if(!tagandscores.isEmpty())
			{
			w.writeCharacters("var data"+getId()+" = new google.visualization.DataTable();");
			w.writeCharacters("data"+getId()+".addColumn('string', 'Tag');");
			w.writeCharacters("data"+getId()+".addColumn('number', 'count');");
			w.writeCharacters("data"+getId()+".addRows([");
			
			boolean first=true;
			for(TagAndScore ts:tagandscores)
				{
				if(!first) w.writeCharacters(",");
				first=false;
				w.writeCharacters("['"+ts.tag+"',"+ts.score+"]");
				}

			
			w.writeCharacters("]);\n");
			w.writeCharacters("var dashboard"+getId()+" = new google.visualization.Dashboard(document.getElementById('dashboard"+getId()+"'));");
			w.writeCharacters("var donutRangeSlider"+getId()+" = new google.visualization.ControlWrapper({");
			w.writeCharacters("          'controlType': 'NumberRangeFilter',");
			w.writeCharacters("          'containerId': 'filter"+getId()+"',");
			w.writeCharacters("          'options': {");
			w.writeCharacters("            'filterColumnLabel': 'count'");
			w.writeCharacters("          }");
			w.writeCharacters("        });");
			w.writeCharacters("var chart"+getId()+" = new google.visualization.ChartWrapper({");
			w.writeCharacters("          'chartType': 'PieChart',");
			w.writeCharacters("          'containerId': 'chart"+getId()+"',");
			w.writeCharacters("          'options': {");
			w.writeCharacters("            'width': 600,");
			w.writeCharacters("            'height': 600,");
			w.writeCharacters("            'pieSliceText': 'value',");
			w.writeCharacters("            'legend': 'right'");
			w.writeCharacters("          }");
			w.writeCharacters("        });");
			w.writeCharacters("dashboard"+getId()+".bind(donutRangeSlider"+getId()+",chart"+getId()+");");
			w.writeCharacters("dashboard"+getId()+".draw(data"+getId()+");");
			w.writeCharacters("}");
			w.writeEndElement();//script
			
			w.writeStartElement("div");
			w.writeAttribute("id", "dashboard"+getId());
			
			w.writeStartElement("div");
			w.writeAttribute("id", "filter"+getId());
			w.writeEndElement();
			
			w.writeStartElement("div");
			w.writeAttribute("id", "chart"+getId());
			w.writeEndElement();
			
			w.writeEndElement();
			
			
			w.writeStartElement("div");
			
			w.writeStartElement("p");
			w.writeCharacters(this.getTitle());
			w.writeEndElement();//p
			
			w.writeEndElement();//div
			}
		}

	}
	
private Reader openReader(String urls) throws IOException
	{
	IOException lastE=null;
	LOG.info(urls);
	for(int i=0;i< 10;++i)
		{
		try
			{
			URL url=new URL(urls);
			Reader in=new InputStreamReader(new GZIPInputStream(url.openStream()));
			return in;
			}
		catch(IOException err)
			{
			LOG.info("warning "+err.getMessage());
			try{Thread.sleep(1000*30);}catch (Exception e) {
			
				}
			lastE=err;
			}
		}
	throw lastE;
	}

private Section[] getSections()
	{
	return new Section[]{
		new Section()
			{
			@Override
			public String getLabel()
				{
				return "Score";
				}
			},
		new Section()
			{
			@Override
			boolean includeAnswer(Answer a, String tag)
				{
				return a.accepted && a.getQuestion().tags.contains(tag);
				}
			@Override
			public String getLabel()
				{
				return "Accepted";
				}
			},
		new Section()
			{
			public int weight(Answer a)
				{
				return a.score;
				}
			@Override
			public String getLabel()
				{
				return "Weighted";
				}
			}
		};
	}

private void usage()
	{
	System.err.println("Usage:");
	System.err.println("    [options] user-id");
	System.err.println("Options:");
	System.err.println(" -h help; This screen.");
	System.err.println(" -s (string) site default:"+this.siteName);
	}
private  void run(String[] args) throws Exception
	{
	int optind=0;
	while(optind< args.length)
		{
		if(args[optind].equals("-h") ||
		   args[optind].equals("-help") ||
		   args[optind].equals("--help"))
			{
			usage();
			return;
			}
		else if(args[optind].equals("-s") && optind+1<args.length)
			{
			this.siteName=args[++optind];
			}
		else if(args[optind].equals("--"))
			{
			optind++;
			break;
			}
		else if(args[optind].startsWith("-"))
			{
			System.err.println("Unknown option "+args[optind]);
			usage();
			return;
			}
		else 
			{
			break;
			}
		++optind;
		}
	if(optind+1!=args.length)
		{
		System.err.println("Illegal number of arguments.");
		usage();
		return;
		}
	this.user_id=args[optind];
	
	final int pagesize=50;
	int page=1;
	Set<String> alltags=new HashSet<String>();
	JsonParser parser=new JsonParser();
	for(;;)
		{
		Reader in=openReader("http://api."+this.siteName+".com/1.1/users/"+
				user_id+"/answers?" +
				"pagesize="+pagesize+
				"&page="+page
				);
		JsonElement root=parser.parse(in);
		in.close();
		if(root==null || !root.isJsonObject()) throw new JsonParseException("not an object "+root);
		JsonArray answers=root.getAsJsonObject().get("answers").getAsJsonArray();
		if(answers.size()==0) break;
		for(int i=0;i< answers.size();++i)
			{
			JsonObject answer=answers.get(i).getAsJsonObject();
			Answer a=new Answer();
			a.answer_id=answer.get("answer_id").getAsLong();
			a.accepted=answer.get("accepted").getAsBoolean();
			a.question_id=answer.get("question_id").getAsLong();
			a.score=answer.get("score").getAsInt();
			this.id2answer.put(a.answer_id, a);
			}
		++page;
		}
	for(Answer answer:this.id2answer.values())
		{
		Reader in=openReader("http://api."+this.siteName+".com/1.1/questions/"+answer.question_id);
		JsonElement root=parser.parse(in);
		in.close();
		JsonObject question=root.getAsJsonObject().get("questions").getAsJsonArray().get(0).getAsJsonObject();
		Question q=new Question();
		q.question_id=answer.question_id;
		JsonArray tags=question.get("tags").getAsJsonArray();
		for(int i=0;i< tags.size();++i) q.tags.add(tags.get(i).getAsString());
		
		q.score=question.get("score").getAsInt();
		q.answer_count=question.get("answer_count").getAsInt();
		q.title=question.get("title").getAsString();
		q.favorite_count=question.get("favorite_count").getAsInt();
		this.id2question.put(q.question_id, q);
		alltags.addAll(q.tags);
		
		
		}

	XMLOutputFactory xmlfactory= XMLOutputFactory.newInstance();
	XMLStreamWriter w= xmlfactory.createXMLStreamWriter(System.out,"UTF-8");
	w.writeStartElement("html");
	w.writeStartElement("head");
	w.writeEndElement();//head
	w.writeStartElement("body");
	
	w.writeStartElement("script");
	w.writeAttribute("type","text/javascript");
	w.writeAttribute("src","https://www.google.com/jsapi");
	w.writeEndElement();
	
	w.writeStartElement("script");
	w.writeAttribute("type","text/javascript");
	w.writeCharacters("google.load('visualization', '1.0', {'packages':['corechart','controls']});");
	w.writeEndElement();
	Section sections[]=getSections();
	for(Section section:sections)
		{
		section.writeHTML(w);
		}
	
	w.writeStartElement("script");
	w.writeAttribute("type","text/javascript");
	long drawCharts_id=(++id_generator);
	w.writeCharacters("function drawCharts"+drawCharts_id+"(){");
	for(Section section:sections)
		{
		w.writeCharacters("drawChart"+section.getId()+"();");
		}
	w.writeCharacters("}");
	w.writeCharacters("google.setOnLoadCallback(drawCharts"+drawCharts_id+");");
	w.writeEndElement();//script
	
	w.writeEndElement();//body
	w.writeEndElement();//html
	w.flush();
	w.close();
	}

public static void main(String[] args) throws Exception
	{
	new StackExchangeSkills().run(args);
	}
}
