package sandbox;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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

public class TimeLineMaker  extends Launcher
	{
	private static final Logger LOG = Logger.builder(TimeLineMaker.class).build();
	@Parameter(names={"-o","--output"},description="Output directory",required=true)
	private File outputDir = null;

	
	
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
			final String key = line.substring(0, colon).trim().toLowerCase();
			if(key.isEmpty()) throw new IllegalArgumentException("empty/no key in "+line);
			this.add(key,line.substring(colon+1).replace(' ', '_').replace('-', '_'));
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
			XMLStreamWriter w = xof.createXMLStreamWriter(pw);
			w.writeDTD("DOCTYPE html");
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
			w.writeAttribute("href", "../css/timeline.css?v1");
			
			w.writeEmptyElement("link");
			w.writeAttribute("rel", "stylesheet");
			w.writeAttribute("href", "../css/fonts/font.default.css?v1");
			
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
			w.writeAttribute("src","../js/timeline.js");
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
	
	private class TMedia extends Entity
		{
		String url = null;
		String link_target = null;
		String link = null;
		String title = null;
		String alt = null;
		String thumbnail = null;
		String credit = null;
		String caption = null;
		void writeJson(JsonWriter w) throws IOException {
			w.beginObject();
			w.name("url");
			w.value(this.url);
			if(this.link!=null) {
				w.name("link");
				w.value(this.link);
			}
			if(this.link_target!=null) {
				w.name("link_target");
				w.value(this.link_target);
			}
			if(this.title!=null) {
				w.name("title");
				w.value(this.title);
			}
			if(this.alt!=null) {
				w.name("alt");
				w.value(this.alt);
			}
			if(this.credit!=null) {
				w.name("credit");
				w.value(this.credit);
			}
			if(this.caption!=null) {
				w.name("caption");
				w.value(this.caption);
			}
			if(this.thumbnail!=null) {
				w.name("thumbnail");
				w.value(this.thumbnail);
			}
			w.endObject();
			}
		@Override
		void add(String key, String value) {
			if(key.equals("url")) { this.url = value; }
			else if(key.equals("link_target")) { this.link_target = value; }
			else if(key.equals("link")) { this.link = value; }
			else if(key.equals("title")) { this.title = value; }
			else if(key.equals("alt")) { this.alt = value; }
			else if(key.equals("caption")) { this.caption = value; }
			else if(key.equals("credit")) { this.credit = value; }
			else if(key.equals("thumbnail")) { this.thumbnail = value; }
			else
				{
				LOG.error("unknown key "+key);
				}
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
		String background="";
		boolean autolink = false;
		String id = null;
		Supplier<TMedia> mediaSupplier = ()->null;
		
		@Override
		void add(String key,String value) {
			if(key.equals("media")) {
				this.mediaSupplier = parseMedia(value);
				}
			 else
			 	{
				super.add(key, value); 
			 	}
			}
		void writeJson(JsonWriter w) throws IOException {
			w.beginObject();
			writeJsonBody(w);
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
			m.url = s;
			return ()->m;
			}
		else
			{
			LOG.info("cannot parse media "+s);
			return ()->null;
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
