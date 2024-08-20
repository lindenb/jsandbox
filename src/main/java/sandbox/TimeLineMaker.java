package sandbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import com.beust.jcommander.Parameter;
import com.google.gson.stream.JsonWriter;

import sandbox.io.IOUtils;

public class TimeLineMaker  extends Launcher
	{
	private static final Logger LOG = Logger.builder(TimeLineMaker.class).build();
	@Parameter(names={"-o","--output"},description="Output directory",required=true)
	private File outputDir = null;
	@Parameter(names={"-lib","--lib"},description="library path")
	private String libpath="..";

	
	private class TDate
		{
		Long year = null;
		Integer month = null;
		Integer day = null;
		void writeJson(JsonWriter w) throws IOException {
			w.beginObject();
			if(year!=null) 
				{
				w.name("year");
				w.value(year);
				}
			if(month!=null) 
				{
				w.name("month");
				w.value(month);
				}
			if(day!=null) 
				{
				w.name("day");
				w.value(day);
				}
			w.endObject();
			}
		}
	
	private class TText
		{
		String headline;
		String text;
		
		void writeJson(final JsonWriter w) throws IOException {
			w.beginObject();
			if(this.headline!=null)
				{
				w.name("headline");
				w.value(this.headline);
				}
			if(this.text!=null)
				{
				w.name("text");
				w.value(this.text);
				}
			w.endObject();
			}
		}
	
	private abstract class Entity
		{
		String id = null;
		
		abstract void add(String key,String value);
		void add(final String line) {
			final int colon = line.indexOf(':');
			if(colon==-1) throw new IllegalArgumentException("no colon in "+line);
			if(colon==0) throw new IllegalArgumentException("no key in "+line);
			final String key = line.substring(0, colon).trim().toLowerCase().replace(' ', '_').replace('-', '_');
			if(key.isEmpty()) throw new IllegalArgumentException("empty/no key in "+line);
			final String v=line.substring(colon+1).trim();
			this.add(key,v);
			}
		}
	

	
	private  class TimeLineEntity
		extends Entity
		{
		String title = "";
		String scale="human";
		TimeLineEntity(final String id) {
			this.id = id;
			}
		@Override
		void add(String key,String value) {
			if(key.equals("title"))
				{
				this.title = value;
				}
			else if(key.equals("scale"))
				{
				this.scale = value;
				}
			else
				{
				LOG.warning("unknown key "+key);
				}
			}
		public String getId() {
			return this.id;
			}
		public String getTitle() {
			return this.title.trim().isEmpty()?this.getId():this.title;
			}
		
		public List<SlideEntity> getSlides() {
			return TimeLineMaker.this.all_slides.
					stream().
					filter(S->S.timelines.contains(this.getId())).
					collect(Collectors.toList());
			}
		public List<EraEntity> getEras() {
			return TimeLineMaker.this.all_eras.
					stream().
					filter(S->S.timelines.contains(this.getId())).
					collect(Collectors.toList());
			}
		
		public void save() throws IOException,XMLStreamException
			{
			final List<SlideEntity> slides= this.getSlides();
			if(slides.isEmpty()) {
				LOG.warning("no slide for "+this.getId());
				return;
				}
			final List<EraEntity> eras= this.getEras();

			final File tlDir = new File(TimeLineMaker.this.outputDir,this.getId());
			tlDir.mkdir();
			final File index_html = new File(tlDir,"index.html");
			
			XMLOutputFactory xof = XMLOutputFactory.newInstance();
			
			PrintWriter pw = new PrintWriter(index_html);
			pw.println("<!DOCTYPE html>\n");
			XMLStreamWriter w = xof.createXMLStreamWriter(pw);
			
			w.writeStartElement("html");
			w.writeAttribute("lang","en");
			
			w.writeStartElement("head");
			
			w.writeStartElement("title");
			w.writeCharacters(this.getTitle());
			w.writeEndElement();//title
			
			w.writeEmptyElement("meta");
			w.writeAttribute("charset", "utf-8");
			w.writeEmptyElement("meta");
			w.writeAttribute("name", "description");
			w.writeAttribute("content",getTitle());
			
			w.writeEmptyElement("link");
			w.writeAttribute("rel", "stylesheet");
			w.writeAttribute("href", TimeLineMaker.this.libpath+"/css/timeline.css?v1");
			
			w.writeEmptyElement("link");
			w.writeAttribute("rel", "stylesheet");
			w.writeAttribute("href", TimeLineMaker.this.libpath+"/css/fonts/font.default.css?v1");
			
			w.writeStartElement("style");
			w.writeCharacters("html, body {"
					+ "height:100%; width:100%; padding: 0px; margin: 0px;"
					+ "}\n");
			w.writeCharacters(".tl-timeline {}\n");
			w.writeEndElement();//style
			
			w.writeEndElement();//head
			
			w.writeStartElement("body");
			
			w.writeStartElement("div");
			w.writeAttribute("id", "timeline");
			w.writeCharacters("");
			w.writeEndElement();//div
			
			w.writeStartElement("script");
			w.writeAttribute("src",TimeLineMaker.this.libpath + "/js/timeline.js");
			w.writeCharacters("");
			w.writeEndElement();//script
			
			w.writeStartElement("script");
			w.writeCharacters(
					"var timeline = new TL.Timeline('timeline', '"+getId()+".json', {" + 
					"is_embed:true });" 
					);
			w.writeEndElement();//script

			w.writeEndElement();//body
			
			w.writeEndElement();
			w.flush();
			w.close();
			pw.flush();
			pw.close();
			
			final File json_file = new File(tlDir,getId()+".json");
			pw = new PrintWriter(json_file);
			JsonWriter js = new JsonWriter(pw);
			js.beginObject();
			if(!eras.isEmpty())
				{
				js.name("eras");
				js.beginArray();
				for(final EraEntity era: eras)
					{
					era.writeJson(js);
					}
				js.endArray();
				}
			js.name("events");
			js.beginArray();
			for(final SlideEntity slide: slides)
				{
				slide.writeJson(js);
				}
			js.endArray();
			js.endObject();
			js.close();
			pw.flush();
			pw.close();
			}
		}
	
	private static final String MEDIA_KEYS[]=new String[]{
			"url","link_target","link","title","alt",
			"thumbnail","credit","caption"
			};
	private class TMedia extends Entity
		{
		final Map<String,String> properties = new HashMap<>(MEDIA_KEYS.length);
		void writeJson(final JsonWriter w) throws IOException {
			w.beginObject();
			for(final String key:MEDIA_KEYS)
				{
				if(!this.properties.containsKey(key)) continue;
				final String v = this.properties.get(key);
				if(v==null || v.trim().isEmpty()) continue;
				w.name(key);
				w.value(this.properties.get(key));
				}
			
			w.endObject();
			}
		@Override
		void add(String key, String value) {
			for(final String k:MEDIA_KEYS)
				{
				if(k.equals(key)) {
					this.properties.put(k,value);
					return;
					}
				}
			LOG.error("unknown key "+key);	
			}
		}
	
	private  class EraEntity
	extends Entity
		{
		TDate start_date = null;
		TDate end_date = null;
		TText text= parseText("");
		final Set<String> timelines = new HashSet<String>();
		@Override
		void add(String key,String value) {
			if(key.equals("start_date") || key.equals("start"))
				{
				this.start_date = parseDate(value);
				}
			else if(key.equals("end_date") || key.equals("end"))
				{
				this.end_date = parseDate(value);
				}
			else if(key.equals("date"))
				{
				this.start_date = this.end_date = parseDate(value);
				}
			else if(key.equals("text") || key.equals("description") || key.equals("title"))
				{
				this.text = parseText(value);
				}
			else if(key.equals("timeline") || key.equals("timelines"))
				{
				final String tokens[] = value.split("[ ,;]");
				if(tokens.length==1 && !tokens[0].isEmpty())
					{
					this.timelines.add(tokens[0]);
					}
				else
					{
					Arrays.stream(tokens).
						filter(S->!S.trim().isEmpty()).
						forEach(S->add(key,S));
					}
				}
			else
				{
				LOG.warning("unknown key "+key);
				}
			}
		
		void writeJsonBody(final JsonWriter w) throws IOException {
			if(this.start_date!=null)
				{
				w.name("start_date");
				this.start_date.writeJson(w);
				}
			if(this.end_date!=null)
				{
				w.name("end_date");
				this.end_date.writeJson(w);
				}
			if(this.text!=null)
				{
				w.name("text");
				this.text.writeJson(w);
				}
			}
		void writeJson(final JsonWriter w) throws IOException {
			w.beginObject();
			writeJsonBody(w);
			w.endObject();
			}
		}
	
	private  class SlideEntity
	extends EraEntity
		{
		String display_date="";
		Supplier<TMedia> mediaSupplier = ()->null;
		
		@Override
		void add(String key,String value) {
			if(key.equals("media")) {
				this.mediaSupplier = parseMedia(value);
				}
			else if(key.equals("display_date")) {
				this.display_date =value;
				}
			else
			 	{
				super.add(key, value); 
			 	}
			}
		void writeJson(JsonWriter w) throws IOException {
			w.beginObject();
			writeJsonBody(w);
			if(this.display_date!=null) {
				w.name("display_date");
				w.value(this.display_date);
			}
			if(this.mediaSupplier.get()!=null)
				{
				w.name("media");
				this.mediaSupplier.get().writeJson(w);
				}
			w.endObject();
			}
		}
	
	private final Map<String,TimeLineEntity> all_timelines = new HashMap<>();
	private final List<SlideEntity> all_slides = new ArrayList<>();
	private final List<EraEntity> all_eras = new ArrayList<>();
	private final List<TMedia> all_medias = new ArrayList<>();
	
	TimeLineMaker() {
		}
	
	private Supplier<TMedia> parseMedia(String s) {
		s=s.trim();
		if(s.startsWith("#"))
			{
			final String id = s.substring(1);
			return ()->all_medias.stream().filter(M->id.equals(M.id)).findAny().orElse(null);
			}
		else if(IOUtils.isURL(s))
			{
			final TMedia m = new TMedia();
			m.properties.put("url" , s);
			return ()->m;
			}
		else
			{
			final TMedia m = new TMedia();
			try {
				final StreamTokenizer st = new StreamTokenizer(new StringReader(s));
				st.wordChars('_', '_');
				final List<String> array = new ArrayList<>();
				while(st.nextToken() != StreamTokenizer.TT_EOF) {
						switch(st.ttype) {
							case StreamTokenizer.TT_NUMBER: array.add(String.valueOf(st.nval));break;
							case '"': case '\'' : case StreamTokenizer.TT_WORD: array.add(st.sval);break;
							case ';': case ':' : break;
							default:break;
							}
						if(array.size()==2) {
							m.properties.put(array.get(0), array.get(1));
							array.clear();
							}
						}
				return ()->m;
				}
			catch(final IOException err) {
				throw new IllegalArgumentException(err);
				}
			}
		}
	
	private TDate parseDate(final String s) {
		TDate td=new TDate();
		if(s.matches("[-+]?\\d+([/\\-]\\d+([/\\-]d+)?)?"))
			{
			final String tokens[] = s.split("[/\\-]");
			td.year = Long.parseLong(tokens[0]);
			if(tokens.length>1) {
				td.month = Integer.parseInt(tokens[1]);
				if(tokens.length>2) {
					td.day = Integer.parseInt(tokens[2]);
					}
				}
			}
		else
			{
			throw new IllegalArgumentException("Cannot parse date "+s);
			}
		return td;
		}
	private TText parseText(final String s) {
		int colon = s.indexOf(":");
		final TText ttText = new TText();
		if(colon==-1) {
			ttText.text= s;
			ttText.headline= s;
			}
		else
			{
			ttText.text= s.substring(colon+1).trim();
			ttText.headline= s.substring(0,colon).trim();
			}
		return null;
	}
	
	private String readline(final BufferedReader br) throws IOException {
		String line=null;
		String line2;
		while((line2=br.readLine())!=null)
			{
			if(line==null)
				{
				line=line2;
				}
			else
				{
				line = line+line2;
				}
			if(!line2.endsWith("\\")) break;
			}
		return line;
		}
	
	private Entity createEntity(final String line) throws IOException {	
		final String tokens[]=line.split("[ \t]+");
		if(tokens[0].equals("@timeline"))
			{
			if(tokens.length!=2) throw new IOException("expected two tokens in "+line);
			final TimeLineEntity ent= new TimeLineEntity(tokens[1]);
			if(this.all_timelines.containsKey(ent.id))
				{
				throw new IOException("duplicate timeline.id "+line);
				}
			this.all_timelines.put(ent.id, ent);
			return ent;
			}
		else if(tokens[0].equals("@event")) {
			final SlideEntity ent= new SlideEntity();
			if(tokens.length>1) ent.id = tokens[1];
			this.all_slides.add(ent);
			return ent;
			}
		else if(tokens[0].equals("@era")) {
			final EraEntity ent= new EraEntity();
			if(tokens.length>1) ent.id = tokens[1];
			this.all_eras.add(ent);
			return ent;
			}
		else if(tokens[0].equals("@media")) {
			final TMedia ent= new TMedia();
			if(tokens.length>1) ent.id = tokens[1];
			this.all_medias.add(ent);
			return ent;
			}
		else
			{
			 throw new IOException("Bad entity : "+line);
			}
		}
	
	
	@Override
	public int doWork(final List<String> args) {
		try {
			IOUtils.assertDirectoryExist(this.outputDir);
			
			
			for(final String fname: args) {
				final BufferedReader br=IOUtils.openBufferedReader(fname);
				Entity current = null;
				TimeLineEntity lastTimeline = null;
				String line;
				while((line= readline(br))!=null)
					{
					if(line.trim().isEmpty())
						{
						if(current!=null && lastTimeline!=null && current instanceof EraEntity && EraEntity.class.cast(current).timelines.isEmpty())
							{
							EraEntity.class.cast(current).timelines.add(lastTimeline.getId());
							}
						current = null;
						}
					else if(current==null || line.startsWith("@")) {
						current = createEntity(line);
						if(current instanceof TimeLineEntity) {
							lastTimeline = TimeLineEntity.class.cast(current);
							}
						}
					else
						{
						current.add(line);
						}
					}
				br.close();
				if(current!=null && lastTimeline!=null && current instanceof EraEntity && EraEntity.class.cast(current).timelines.isEmpty())
					{
					EraEntity.class.cast(current).timelines.add(lastTimeline.getId());
					}
				}
			
			
			if(this.all_timelines.isEmpty())
				{
				LOG.error("no timeline defined.");
				return -1;
				}
			for(final TimeLineEntity tl:this.all_timelines.values())
				{
				tl.save();
				}
			return 0;
			}
		catch(final Exception err) {
			LOG.error(err);
			return -1;
			}
		}
	
	public static void main(final String[] args) {
		new TimeLineMaker().instanceMainWithExit(args);
		}
	}
