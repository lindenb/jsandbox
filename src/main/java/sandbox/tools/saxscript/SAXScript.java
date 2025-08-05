package sandbox.tools.saxscript;

import sandbox.Logger;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.beust.jcommander.Parameter;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;

import sandbox.Launcher;
import sandbox.io.IOUtils;
import sandbox.nashorn.NashornUtils;
import sandbox.tools.central.ProgramDescriptor;

public class SAXScript  extends Launcher {
	private static final Logger LOG = Logger.builder(SAXScript.class).build();

	
    @Parameter(names= {"-o","--out"},description=OUTPUT_OR_STANDOUT)
    private Path outFile = null;
    @Parameter(names= {"-f","--script"},description="read javascript script from file")
    private Path scripFile = null;
    @Parameter(names= {"-e","--expression"},description="read javascript script from argument")
    private String scriptExpr = null;
    @Parameter(names= {"-ns","--namespace-aware"},description="SAX parser namespace aware")
    private boolean namespace_aware = false;
    @Parameter(names= {"-valid","--valid"},description="read javascript script from argument")
    private boolean validating = false;
    @Parameter(names= {"-json","--json"},description="insert JSON document in the javascript context as 'userData' using google gson library. Default is null json element.")
    private Path jsonData = null;

    

	
	public static class SAXScriptHandler extends DefaultHandler {
		/** map of boolean anwsers if a SAX callback method was implemented in javascript */
		private final Map<String,Boolean> methodImplemented= new HashMap<String,Boolean>();
		/** the JAVASCRIPT scripting engine */
		private final ScriptEngine scriptEngine;
		private SAXScriptHandler(final ScriptEngine  scriptEngine)
			{
			
			this.scriptEngine=scriptEngine;
			}
		
		
		@Override
		public void characters(char[] ch, int start, int length)
				throws SAXException
			{
			invoke("characters",new String(ch,start,length));
			}
		
		@Override
		public void endDocument() throws SAXException
			{
			invoke("endDocument");
			}
		
		@Override
		public void endElement(String uri, String localName, String name)
				throws SAXException {
			invoke("endElement",uri,localName,name);
			}
		
		@Override
		public void endPrefixMapping(String prefix) throws SAXException {
			invoke("endPrefixMapping",prefix);
			}
		
		@Override
		public void ignorableWhitespace(char[] ch, int start, int length)
				throws SAXException {
			invoke("ignorableWhitespace",new String(ch,start,length));
			
		}
		
		@Override
		public void processingInstruction(String target, String data)
				throws SAXException {
			invoke("processingInstruction",target,data);
			
		}
		
		
		
		@Override
		public void skippedEntity(String name) throws SAXException {
			invoke("skippedEntity",name);
			
		}
		
		@Override
		public void startDocument() throws SAXException {
			invoke("startDocument");
			
		}
		
		@Override
		public void startElement(String uri, String localName, String name,
				Attributes atts) throws SAXException {
			invoke("startElement",uri,localName,name,atts);
			
		}
		
		@Override
		public void startPrefixMapping(String prefix, String uri)
				throws SAXException {
			invoke("startPrefixMapping",prefix,uri);
			
		}
		
		private void invoke(String function,Object ... parameters) throws SAXException
			{
			if(methodImplemented.get(function)!=null) return ;
			try
				{
				Invocable.class.cast(scriptEngine).invokeFunction(function,parameters);
				}
			catch(NoSuchMethodException err)
				{
				methodImplemented.put(function, Boolean.FALSE);
				}
			catch(ScriptException err)
				{
				throw new SAXException(err);
				}
			}
		}
		
		
		
		@Override
		public int doWork(List<String> args) {
			JsonElement userData= JsonNull.INSTANCE;;
			
			if(jsonData!=null) {
				try(Reader r=Files.newBufferedReader(this.jsonData)) {
				final JsonParser parser = new JsonParser();
				userData = parser.parse(r);
				} catch(Throwable err) {
					LOG.error(err);
					return -1;
					}
				}
				
			 if(scripFile==null && scriptExpr==null)
			 	{
				LOG.error("Undefined Script");
				return -1;
			 	}
			 if(scripFile!=null && scriptExpr!=null)
			 	{
				LOG.error("options '-f' and '-e' both defined");
				return -1; 
			 	}
			 final ScriptEngine scripEngine= NashornUtils.makeRequiredEngine();
			
		     scripEngine.put("userData", userData);
	     
	     try(Reader scriptReader=scripFile!=null?
	    		Files.newBufferedReader(scripFile):
	    			 new java.io.StringReader(scriptExpr)
	    		 )
			     {
			     scripEngine.eval(scriptReader);
			     
			     final SAXParserFactory saxFactory= SAXParserFactory.newInstance();
			     saxFactory.setNamespaceAware(!this.namespace_aware);
			     saxFactory.setValidating(this.validating);
			     final SAXParser parser= saxFactory.newSAXParser();
			     
			     final DefaultHandler handler= new SAXScriptHandler(scripEngine);
			     
			     
			     if(args.isEmpty())
			    	{
			    	scripEngine.put("__FILENAME__", "<STDIN>");
			    	parser.parse(System.in, handler);
			    	}
			    else
			    	{
			    	for(final String file:args)
			    		{
			    		scripEngine.put("__FILENAME__", file);
			    		if(file.toLowerCase().endsWith(".gz"))
			    			{
				    		try(java.io.Reader reader= IOUtils.openReader(file)) {
					    		parser.parse(new org.xml.sax.InputSource(reader), handler);
					    		}
			    			}
			    		else
			    			{
			    			parser.parse(new org.xml.sax.InputSource(file), handler);
			    			}
			    		}
			    	}
			    return 0;
				}
			catch (Throwable err) {
			LOG.error(err);
			return -1;
			}
		}
	    public static ProgramDescriptor getProgramDescriptor() {
	    	return new ProgramDescriptor() {
	    		@Override
	    		public String getName() {
	    			return "saxscript";
	    			}
	    	@Override
	    	public String getDescription() {
				
				
				

				System.err.println("\n\nScript Example:\n\n");
				System.err.println("function startDocument()\n"+
				"\t{println(\"Start doc\");}\n"+
				"function endDocument()\n"+
				"\t{println(\"End doc\");}\n"+
				"function startElement(uri,localName,name,atts)\n"+
				"\t{\n"+
				"\tprint(\"\"+__FILENAME__+\" START uri: \"+uri+\" localName:\"+localName);\n"+
				"\tfor(var i=0;atts!=undefined && i< atts.getLength();++i)\n"+
				"\t\t{\n"+
				"\t\tprint(\" @\"+atts.getQName(i)+\"=\"+atts.getValue(i));\n"+
				"\t\t}\n"+
				"\tprintln(\"\");\n"+
				"\t}\n"+
				"function characters(s)\n"+
				"\t{println(\"Characters :\" +s);}\n"+
				"function endElement(uri,localName,name)\n"+
				"\t{println(\"END: uri: \"+uri+\" localName:\"+localName);}\n\n\n");
				return "A SAX Parser paring one or more XML file and invoking some SAX CallBack written in javascript";
	    	}
	    		};
	    	}
	
	public static void main(String[] args) {
		new SAXScript().instanceMainWithExit(args);
		}
	}
