package sandbox.tools.velocityjson;


import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;

import com.beust.jcommander.DynamicParameter;
import com.beust.jcommander.Parameter;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.io.IOUtils;
import sandbox.io.RuntimeIOException;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VelocityJson extends Launcher {
	protected static final Logger LOG=Logger.builder(VelocityJson.class).build();
    @Parameter(names={"-o","--output"},description="output name")
    private Path out = null; 
    @Parameter(names={"-f","--json-file"},description="load json files")
    private List<String> jsonFiles = new ArrayList<>(); 
    @Parameter(names={"-j","--json"},description="load json string")
    private List<String> jsonStrs = new ArrayList<>(); 
    @DynamicParameter(names={"-D","--params"},description="dynamic paramters -Dkey=value")
    private Map<String,String> dynaParams = new HashMap<>(); 
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
	
	private Map.Entry<String,String> keyValue(final String kvs) {
			final int eq = kvs.indexOf('=');
			if(eq<1) {
				throw new IllegalArgumentException("No '=' found in "+kvs);
				}
			final String k = kvs.substring(0,eq).trim();
			final String v = kvs.substring(eq+1).trim();
			return new AbstractMap.SimpleEntry<String,String>(k,v);
			}
	
	@Override
	public int doWork(final List<String> args) {
			final Context context = new VelocityContext();
			context.put("tool", new Tool());
		  	
			try {
				for(final String k: dynaParams.keySet()) {
					if(context.containsKey(k)) {
						throw new IllegalArgumentException("duplicate key "+k);
						}
					context.put(k, dynaParams.get(k));
					}
				this.jsonStrs.stream().map(S->keyValue(S)).forEach(KV->{
					if(context.containsKey(KV.getKey())) {
						throw new IllegalArgumentException("duplicate key "+KV.getKey());
						}
		  			final JsonParser jr = new JsonParser();
		  			final JsonElement element = jr.parse(KV.getValue());
		  			context.put(KV.getValue(), flatten(element) );
					});
				
				this.jsonFiles.stream().map(S->keyValue(S)).forEach(KV->{
					if(context.containsKey(KV.getKey())) {
						throw new IllegalArgumentException("duplicate key "+KV.getKey());
						}
					final Path f = Paths.get(KV.getValue());
					IOUtils.assertFileExists(f);
		  			final JsonParser jr = new JsonParser();
		  			
		  			final JsonElement element ;
		  			try {
		  			try(Reader r = Files.newBufferedReader(f)) {
		  				element = jr.parse(r);
		  				}
		  			} catch(IOException err) {
		  				throw new RuntimeIOException(err);
		  				}
		  			context.put(KV.getValue(),flatten( element) );
					});
				
				
				
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
		                    final VelocityEngine ve = new VelocityEngine();
		                    ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
		                    ve.setProperty("file.resource.loader.class","org.apache.velocity.runtime.resource.loader.FileResourceLoader");
		                    ve.setProperty("file.resource.loader.path",file.getParent());
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
				err.printStackTrace();
				return -1;
				}
	    	}
	
		public static void main(final String[] args) {
				new VelocityJson().instanceMainWithExit(args);
			}
		}
