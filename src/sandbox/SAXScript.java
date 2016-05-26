package sandbox;

import java.util.logging.Logger;
import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonParser;

public class SAXScript  extends AbstractApplication 
	{
	private static final Logger LOG=Logger.getLogger("jsandbox");
	
	private SAXScript()
		{
		}
	
	
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
		protected String getProgramDescription() {
			return "A SAX Parser paring one or more XML file and invoking some SAX CallBack written in javascript";
		}
	
		@Override
			protected void usage() {
			super.usage();
			
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
			}
		
		@Override
		protected void fillOptions(Options options) {
			options.addOption(Option.builder("f").longOpt("script").hasArg(true).desc("read javascript script from file").build());
			options.addOption(Option.builder("e").longOpt("expression").hasArg(true).desc(" read javascript script from argument").build());
			options.addOption(Option.builder("notns").longOpt("notns").hasArg(false).desc("SAX parser is NOT namespace aware").build());
			options.addOption(Option.builder("valid").longOpt("valid").hasArg(false).desc(" SAX parser is validating").build());
			options.addOption(Option.builder("j").longOpt("json").hasArg(true).desc("insert JSON document in the javascript context as 'userData' using google gson library. Default is null json element.").build());
			super.fillOptions(options);
			}
		
		@Override
		protected int execute(final CommandLine cmd) {
			JsonElement userData= JsonNull.INSTANCE;;
			boolean namespaceAware=true;
			boolean validating=false;
			File scriptFile=null;
			String scriptString=null;
			if(cmd.hasOption("f")) {
				scriptFile = new File(cmd.getOptionValue("f"));
				}
			if(cmd.hasOption("e")) {
				scriptString = cmd.getOptionValue("e");
				}
			if(cmd.hasOption("notns")) {
				namespaceAware=false;
				}
			if(cmd.hasOption("valid")) {
				validating=true;
				}
			if(cmd.hasOption("j")) {
				Reader r=null;
				try {
				r= new FileReader(cmd.getOptionValue("j"));
				final JsonParser parser = new JsonParser();
				userData = parser.parse(r);
				r.close();
				} catch(Exception err) {
					err.printStackTrace();
					return -1;
				} finally {
					IOUtils.close(r);
				}
				}
		    
		 if(scriptFile==null && scriptString==null)
		 	{
			LOG.severe("Undefined Script");
			return -1;
		 	}
		 if(scriptFile!=null && scriptString!=null)
		 	{
			 LOG.severe("options '-f' and '-e' both defined");
			return -1; 
		 	}
		 final ScriptEngineManager mgr=new  ScriptEngineManager();
		 final  ScriptEngine scripEngine= mgr.getEngineByExtension("js");
		 scripEngine.put("userData", userData);
		 
		 
	     if(scripEngine==null)
	     	{
	    	LOG.severe("Cannot get a javascript engine");
	    	return -1;
	     	}
	     
	     Reader scriptReader=null;
	     try {
	     if(scriptFile!=null)
	     	{
	    	scriptReader = new java.io.FileReader(scriptFile);
	     	}
	     else 
	     	{
	    	 scriptReader = new java.io.StringReader(scriptString);
	     	}
	    
	     scripEngine.eval(scriptReader);
	     scriptReader.close();scriptReader=null;
	     
	     final SAXParserFactory saxFactory= SAXParserFactory.newInstance();
	     saxFactory.setNamespaceAware(namespaceAware);
	     saxFactory.setValidating(validating);
	     final SAXParser parser= saxFactory.newSAXParser();
	     
	     final DefaultHandler handler= new SAXScriptHandler(scripEngine);
	     final List<String> args = cmd.getArgList();
	     
	     
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
		    		java.io.Reader reader= IOUtils.openReader(file);
		    		parser.parse(new org.xml.sax.InputSource(reader), handler);
		    		reader.close();
	    			}
	    		else
	    			{
	    			parser.parse(new org.xml.sax.InputSource(file), handler);
	    			}
	    		}
	    	}
	     return 0;
		}
		catch (Exception err) {
		err.printStackTrace();
		LOG.severe(err.getMessage());
		return -1;
		}
	     finally {
	    	 IOUtils.close(scriptReader);
	     }
		}
	
	
	public static void main(String[] args) {
		new SAXScript().instanceMainWithExit(args);
		}
	}
