package sandbox.tools.timeline;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.apache.jena.rdf.model.AnonId;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;

import com.beust.jcommander.Parameter;
import com.google.gson.stream.JsonWriter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.io.IOUtils;
import sandbox.lang.StringUtils;
import sandbox.tools.central.ProgramDescriptor;
import sandbox.util.stream.MyCollectors;

public class TimeLineMaker  extends Launcher
	{
	private static final Logger LOG = Logger.builder(TimeLineMaker.class).build();
	@Parameter(names={"-o","--output"},description="Output directory",required=true)
	private File outputDir = null;
	@Parameter(names={"-lib","--lib"},description="library path")
	private String libpath="..";
	private static final String NS="http://xxx./timeline#";
	private final Model model = ModelFactory.createDefaultModel();
	private static final Resource TYPE_EVENT = ResourceFactory.createProperty(NS, "Event");
	
	
	
	
	
	public TimeLineMaker() {
		}
	
	
	private String readline(final BufferedReader br) throws IOException {
		StringBuilder line=null;
		String line2;
		while((line2=br.readLine())!=null)
			{
			if(line==null)
				{
				line=new StringBuilder(line2);
				}
			else
				{
				line.append(line2);
				}
			if(!line2.endsWith("\\")) break;
			line.deleteCharAt(line.length()-1);
			}
		return line==null?null:line.toString();
		}
	
	
	private Resource toType(final String value) {
		if(value.equals("event")) return TYPE_EVENT;
		throw new IllegalArgumentException("Undefined type "+value);
	}
	
	private void parseDocument(final File docFile,BufferedReader br) throws IOException {
		final List<Map.Entry<String, String>> entries = new ArrayList<>();
		for(;;)  {
			final String line = readline(br);
			if(line!=null && line.startsWith("#")) continue;
			if(StringUtils.isBlank(line)) {
				if(!entries.isEmpty()) {
					final Resource type = entries.stream().
						filter(KV->KV.getKey().equals("type")).
						map(KV->toType(KV.getValue())).
						collect(MyCollectors.oneOrNone()).
						orElse(TYPE_EVENT);
					
					final Resource id = entries.stream().
							filter(KV->KV.getKey().equals("id")).
							map(KV->model.createResource(AnonId.create(KV.getValue()))).
							collect(MyCollectors.oneOrNone()).
							orElse(model.createResource());
					
					final Optional<Literal> title = entries.stream().
							filter(KV->KV.getKey().equals("title")).
							map(KV->model.createLiteral(KV.getValue())).
							collect(MyCollectors.oneOrNone())
							;
					final Optional<Literal> description = entries.stream().
							filter(KV->KV.getKey().equals("description")).
							map(KV->model.createLiteral(KV.getValue())).
							collect(MyCollectors.oneOrNone())
							;
					
					if(model.containsResource(id)) {
						throw new IllegalArgumentException("id already defined in "+entries);
						}
					model.add(id,RDF.type,type);
					}
				if(line==null) break;
				entries.clear();
				}
			final int colon = line.indexOf(":");
			if(colon<=0) throw new IOException("key missing in "+line);
			final String key = line.substring(0,colon).trim();
			if(StringUtils.isBlank(key))  throw new IOException("key missing in "+line);
			final String value = line.substring(colon+1).trim();
			entries.add(new AbstractMap.SimpleEntry<>(key, value));
			}
		}
	
	
	@Override
	public int doWork(final List<String> args) {
		try {
			IOUtils.assertDirectoryExist(this.outputDir);
			
			
			return 0;
			}
		catch(final Exception err) {
			LOG.error(err);
			return -1;
			}
		}
	
	
	public static ProgramDescriptor getProgramDescriptor() {
    	return new ProgramDescriptor() {
    		@Override
    		public String getName() {
    			return "timelinemaker";
    			}
    		};
    	}
	
	
	public static void main(final String[] args) {
		new TimeLineMaker().instanceMainWithExit(args);
		}
	}
