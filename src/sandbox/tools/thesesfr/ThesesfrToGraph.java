package sandbox.tools.thesesfr;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import com.beust.jcommander.Parameter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.gexf.Gexf;

public class ThesesfrToGraph extends Launcher {
	
	private static final Logger LOG = Logger.builder(ThesesfrToGraph.class).build();
    private static final String BASE="https://theses.fr/api/v1";
    
    @Parameter(names= {"-o","--out"},description=OUTPUT_OR_STANDOUT)
    private Path outFile = null;
    @Parameter(names= {"-d","--max-depth"},description="Max Depth")
    private int max_depth = 3;
    @Parameter(names= {"--short-title"},description="reduce size of title to this lentgh")
    private int short_title=-1;

    
    private static abstract class Entity {
        String id;
        boolean flag=false;
        int level=100000;
        abstract String getURL();
        @Override
        public int hashCode() {
            return id.hashCode();
            }
        boolean isAuthor() { return false;}
        boolean isThese() { return false;}
        }

    private static class Author extends Entity {
        String nom="";
        String prenom="";
        final Set<Relation> relations = new HashSet<>();
        @Override String getURL()  {
            return BASE + "/personnes/personne/"+id;
            }
        @Override
        public boolean equals(Object obj) {
            if(obj==this) return true;
            if(obj==null || !(obj instanceof Author)) return false;
            return this.id.equals(Author.class.cast(obj).id);
            }
        @Override
        boolean isAuthor() { return true;}
		@Override
		public String toString() {
			return ""+nom+" "+prenom;
			}
        }

    private static class These  extends Entity {
        String title;
        String date_soutenance;
        @Override String getURL()  {
            return BASE + "/theses/these/"+id;
            }

        @Override
        public boolean equals(Object obj) {
            if(obj==this) return true;
            if(obj==null || !(obj instanceof These)) return false;
            return this.id.equals(These.class.cast(obj).id);
            }
        @Override
        boolean isThese() { return true;}
		@Override
		public String toString() {
			return title;
			}

        }

    private static class Relation {
        final String property;
        final These these;
        Relation(final String property,final These these) {
            this.property=property;
            this.these=these;
            }
        @Override
        public int hashCode() {
            return property.hashCode()*21+these.hashCode();
            }
        @Override
        public boolean equals(Object obj) {
            if(obj==this) return true;
            if(obj==null || !(obj instanceof Relation)) return false;
            Relation cp = Relation.class.cast(obj);
            return this.property.equals(cp.property) &&
                    this.these.equals(cp.these);
            }
        }

    private JsonElement json(final String urlstr) {
    	LOG.info(urlstr);
        try(InputStream in =new URL(urlstr).openStream()){
        	JsonParser jsr=new JsonParser();
        	JsonElement E= jsr.parse(new InputStreamReader(in));
        	return E;
        	}
        catch(IOException err) {
        	err.printStackTrace();
        	return JsonNull.INSTANCE;
        	}
        }

    private final Map<String,Entity> id2entity = new HashMap<>();

    private Author findAuthorById(String id) {
        Entity entity = id2entity.get(id);
        if(entity!=null) {
            if(!entity.isAuthor()) throw new IllegalStateException();
            return (Author)entity;
            }
       final  Author a=new Author();
        a.id=id;
        this.id2entity.put(id, a);
        return a;
        }
    private These findTheseById(String id) {
        Entity entity = id2entity.get(id);
        if(entity!=null) {
            if(!entity.isThese()) throw new IllegalStateException();
            return (These)entity;
            }
        final These a=new These();
        a.id=id;
        this.id2entity.put(id, a);
        return a;
        }

    private void loop() {
        for(;;) {
        	LOG.info("N="+id2entity.size()+" remain="+id2entity.values().stream().filter(E->!E.flag).count());
            final Entity entity = id2entity.values().
                    stream().
                    filter(E->!E.flag).
                    findFirst().
                    orElse(null);
            if(entity==null) break;
            if(entity.level>0 && entity.level>=max_depth) {
            	 entity.flag=true;
            	continue;
            	}
            JsonElement json = json(entity.getURL());
            entity.flag=true;
            if(!json.isJsonObject()) continue;
            JsonObject object = json.getAsJsonObject();
            if(entity.isThese()) {
            	final String props[]= {"membresJury","directeurs","auteurs","rapporteurs"};
                final These these = These.class.cast(entity);
                these.title = object.get("titrePrincipal").getAsString();
                if(short_title>-1 && these.title.length()>short_title ) {
                	these.title=these.title.substring(0,short_title);
                	}
                
                these.date_soutenance = object.has("dateSoutenance") && !object.get("dateSoutenance").isJsonNull() ? object.get("dateSoutenance").getAsString():"";
                for(final String prop:props) {
	                if(!object.has(prop)) continue;
                    JsonArray array = object.get(prop).getAsJsonArray();
                    for(int i=0;i<array.size();i++) {
                        JsonObject o2=array.get(i).getAsJsonObject();
                        if(!o2.has("ppn") || o2.get("ppn").isJsonNull()) continue;
                        final Author a = findAuthorById(o2.get("ppn").getAsString());
                        a.level= Math.min(a.level, entity.level+1);
                        a.nom = o2.get("nom").getAsString();
                        a.prenom = o2.get("prenom").getAsString();
                        a.relations.add(new Relation(prop, these));
                        }
	                }
                }
            else if(entity.isAuthor()) {
            	if(object.has("theses")) {
            		 JsonObject o2 = object.get("theses").getAsJsonObject();
            		 for(Map.Entry<String,JsonElement> kv:o2.entrySet()) {
            			 final JsonArray array = kv.getValue().getAsJsonArray();
	            		 for(int i=0;i<array.size();i++) {
	                         final JsonObject o3=array.get(i).getAsJsonObject();
	                         These a = findTheseById(o3.get("id").getAsString());
	                         a.level= Math.min(a.level, entity.level+1);
	                         a.title = o3.get("titre").getAsString();
	                         if(short_title>-1 && a.title.length()>short_title ) {
	                         	a.title=a.title.substring(0,short_title);
	                         	}
	                         }
	            		 }
            		}
                }
            }
        }
    @Override
    public int doWork(List<String> args) {
        if(args.isEmpty()) {
                System.err.println("authors id missing");
                return -1;
               }
        for(final String arg:args) {
    	    Author a= findAuthorById(arg);
        	a.level=0;
            }
    	loop();
    	try {
		final XMLOutputFactory xof=XMLOutputFactory.newFactory();
		XMLStreamWriter w= null;
		Writer fw=null;
		
		if(this.outFile==null)
			{
			w=xof.createXMLStreamWriter(System.out, "UTF-8");
			}
		else
			{
			w=xof.createXMLStreamWriter((fw=Files.newBufferedWriter(this.outFile,Charset.forName("UTF-8"))));
			}
		w.writeStartDocument("UTF-8", "1.0");
		w.writeStartElement("gexf");
		w.writeAttribute("xmlns", Gexf.XMLNS);
		w.writeAttribute("xmlns:xsi",XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI);
		w.writeAttribute("xsi:schemaLocation",Gexf.SCHEMA_LOCATION);
		w.writeAttribute("version", Gexf.VERSION);
		w.writeStartElement("meta");
		  w.writeAttribute("lastmodifieddate",new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
		  w.writeStartElement("creator");
		  w.writeCharacters(ThesesfrToGraph.class.getName()+"  by Pierre Lindenbaum");
		  w.writeEndElement();
		 
		  w.writeStartElement("description");
		  w.writeCharacters("");
		  w.writeEndElement();
		
		w.writeEndElement();//meta
		  
		  w.writeStartElement("graph");
		  w.writeAttribute("mode", "static");
		  w.writeAttribute("defaultedgetype", "directed");
		  
		  w.writeStartElement("attributes");
		  w.writeAttribute("class", "node");
		  w.writeAttribute("mode", "static");
		  w.writeEndElement();//attributes
			
		  w.writeStartElement("attributes");                                                                                     
		  w.writeAttribute("class", "edge");
		  w.writeAttribute("mode", "static");
			  
		
          w.writeEmptyElement("attribute");
			w.writeAttribute("id", "0");
			w.writeAttribute("title", "date_soutenance");
			w.writeAttribute("type", "string");
			
			  /*
	      w.writeEmptyElement("attribute");
			w.writeAttribute("id", "1");
			w.writeAttribute("title", "title");
			w.writeAttribute("type", "string");
		  w.writeEmptyElement("attribute");
			w.writeAttribute("id", "2");
			w.writeAttribute("title", "pubdate");
			w.writeAttribute("type", "string");
			*/
			
          w.writeEndElement();//attributes
		  

		  
		  w.writeStartElement("nodes");
		  for(final Entity entity:this.id2entity.values())
		 	{
			w.writeStartElement("node");
			w.writeAttribute("id",entity.id);
			if(entity.isAuthor())
				{
				final Author t =  (Author)entity;
				w.writeAttribute("label",String.join(" ", t.prenom,t.nom));
				}
			else if(entity.isThese())
				{
				final These t = (These)entity;
				w.writeAttribute("label",String.valueOf(t.title));
				
				if(!StringUtils.isBlank(t.date_soutenance)) {
					w.writeEmptyElement("attvalue");
						w.writeAttribute("for", "0");
						w.writeAttribute("value",t.date_soutenance);
					}
				}

			w.writeEndElement();//node
		 	}
		  w.writeEndElement();//nodes
		
		  
		  w.writeStartElement("edges");
		  for(final Entity entity:this.id2entity.values())
		 	{
			 if(!entity.isAuthor()) continue;
			 Author t = (Author)entity;
			for(final Relation rel: t.relations)
				{
				w.writeStartElement("edge");
				w.writeAttribute("id","E"+t.id+"_"+rel.these.id);
				w.writeAttribute("label",rel.property);
				w.writeAttribute("type","directed");
				w.writeAttribute("source",t.id);
				w.writeAttribute("target",rel.these.id);
				w.writeEndElement();
				}
		 	}
		  w.writeEndElement();//edges
		  
		  w.writeEndElement();//graph

		
		w.writeEndElement();//gexf
		w.writeEndDocument();
		w.flush();
		if(fw!=null)
			{
			fw.flush();
			}
		else
			{
			System.out.flush();
			}
    	
    	return 0;
    	}
    	catch(Throwable err) {
    		LOG.error(err);
    		return -1;
    		}
    	}
    
    public static void main(String[] args) {
		new ThesesfrToGraph().instanceMainWithExit(args);
		}
    }

