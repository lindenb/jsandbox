package sandbox.tools.velocityjson;


import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;

import com.beust.jcommander.Parameter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.io.FileHeader;
import sandbox.io.IOUtils;
import sandbox.tools.central.ProgramDescriptor;

public class VelocityJson extends Launcher {
	protected static final Logger LOG=Logger.builder(VelocityJson.class).build();
    @Parameter(names={"-o","--output"},description=OUTPUT_OR_STANDOUT)
    private Path out = null; 
    @Parameter(names={"-m","--manifest"},description="TSV file with the following required header: key/value/type",required = true)
    private Path manifestPath=null;
    @Parameter(names={"--no-flatten"},description="don't convert google json to standard java types.")
    private boolean no_flatten=false;

	
	public static class Tool {
		public JsonElement json(final String j) {
			return  new JsonParser().parse(j);
		}
		public String readURL(final String path) throws IOException {
			final Reader r = IOUtils.openReader(path);
			final String s= IOUtils.readReaderContent(r);
			r.close();
			return  s;
		}
		
		public String readFile(final String f) throws IOException {
			return  IOUtils.readFileContent(new File(f));
		}
		public String escapeC(final String s) {
			return StringUtils.escapeC(s);
			}
		@Override
		public String toString() {
			return "TOOLS "+getClass().getName();
			}
	}
	
	private Object flatten(final JsonElement e) {
		if(no_flatten) return e;
		if(e.isJsonNull()) {
			return null;
		} else if(e.isJsonPrimitive()) {
			final JsonPrimitive prim = e.getAsJsonPrimitive();
			if(prim.isBoolean()) {
				return prim.getAsBoolean();
				}
			else if(prim.isNumber()) {
				return prim.getAsNumber();
				}
			else if(prim.isString()) {
				return prim.getAsString();
				}
			}
		else if(e.isJsonObject()) {
			final JsonObject x = e.getAsJsonObject();
			final Map<String,Object> w = new HashMap<>();
			w.entrySet().stream().forEach(KV->{
				w.put(KV.getKey(),flatten(x.get(KV.getKey())));
				});
			return w;
		}else if(e.isJsonArray()) {
			final JsonArray x = e.getAsJsonArray();
			final List<Object> w = new ArrayList<>(x.size());
			for(int i=0;i< x.size();++i) {
				w.add(flatten(x.get(i)));
				}
			return w;
		}
		throw new IllegalArgumentException();
	}
	
	
	@Override
	public int doWork(final List<String> args) {
			final Context context = new VelocityContext();
			context.put("tool", new Tool());
		  	
			try {
				try(BufferedReader br= IOUtils.openBufferedReaderFromPath(this.manifestPath) ) {
					String line = br.readLine();
					if(line==null) {
						LOG.error("cannot read first line");
						return -1;
						}
					final FileHeader header = new FileHeader(line,FileHeader.TSV_SPLITTER);
					header.assertColumnExists("name");
					header.assertColumnExists("key");
					header.assertColumnExists("value");
					while((line=br.readLine())!=null) {
						final FileHeader.RowMap row = header.toMap(line);
						final String key = row.get("key");
						if(StringUtils.isBlank(key)) {
							LOG.error("empty key in "+row);
							return -1;
							}
						if(context.containsKey(key)) {
							LOG.error("duplicate key in "+row);
							return -1;
							}
						final String type= row.get("type").toLowerCase();
						final String value= row.get("value");
						final Object o;
						if(type.equals("string")) {
							o=value;
							}
						else if(type.equals("int") || type.equals("integer")) {
							o = Integer.parseInt(value);
							}
						else if(type.equals("long")) {
							o = Long.parseLong(value);
							}
						else if(type.equals("float")) {
							o = Float.parseFloat(value);
							}
						else if(type.equals("double")) {
							o = Double.parseDouble(value);
							}
						else if(type.equals("json")) {
							final Path f = ((value.trim().startsWith("[") || value.trim().startsWith("{"))?null:Paths.get(value));
							final JsonParser jr = new JsonParser();
							if(f==null || !Files.exists(f)) {
		  						o=flatten(jr.parse(value));
		  						}
		  					else
		  						{
		  						try(Reader r = Files.newBufferedReader(f)) {
					  				o = flatten(jr.parse(r));
					  				}
		  						}
							}
						else if(type.equals("class")) {
							final Class<?> c=Class.forName(value);
		  					o=c;
							}
						else if(type.equals("instance-of") || type.equals("instanceof")) {
							Class<?> c=Class.forName(value);
		  					o=c.getConstructor().newInstance();
							}
						else	{
							LOG.error("unknow type:"+type+" in "+row);
							return -1;
							}
						context.put(key,o);
						}
					}//end open manifest
				
				
		    try(final PrintWriter w=IOUtils.openPathAsPrintWriter(this.out)) {
		    	if(args.isEmpty())
		    		{
                    final VelocityEngine ve = new VelocityEngine();
                    ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "string");
                    ve.setProperty("string.resource.loader.class","org.apache.velocity.runtime.resource.loader.StringResourceLoader");
                    ve.init();
                    final String templText = IOUtils.readStreamContent(System.in);
                    StringResourceLoader.getRepository().putStringResource("t1", templText);
                    final Template template = ve.getTemplate("t1");
                    template.merge( context, w);
		    		}
		    	else
			    	{
		    		for(final String filename: args)
		                    {
		                    final File file=new File(filename);
		                    IOUtils.assertFileExists(file);
		                    final VelocityEngine ve = new VelocityEngine();
		                    ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
		                    ve.setProperty("file.resource.loader.class","org.apache.velocity.runtime.resource.loader.FileResourceLoader");
		                    //ve.setProperty("file.resource.loader.path",Collections.singletonList(file.getParent()==null?new File("."):file.getParent()));
		                    ve.setProperty("file.resource.loader.path",file.getParent()==null?".":file.getParent().toString());
		                    ve.init();
		                    final Template template = ve.getTemplate(file.getName());
		                    template.merge( context, w);
		                    }
			    	}
		    	w.flush();
		    	}
	    	return 0;
        	}
		catch(final Throwable err) {
			LOG.error(err);
			return -1;
			}
    	}
	  
public static ProgramDescriptor getProgramDescriptor() {
	return new ProgramDescriptor() {
		@Override
		public String getName() {
			return "velocityjson";
			}
		};
	}

	  
  
	public static void main(final String[] args) {
			new VelocityJson().instanceMainWithExit(args);
		}
	}
