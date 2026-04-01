package sandbox.tools.java2jni;

import java.io.File;
import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.context.Context;
import org.apache.velocity.runtime.RuntimeConstants;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.io.IOUtils;
import sandbox.tools.central.ProgramDescriptor;


public class JavaToJNI extends Launcher
	{
	protected static final Logger LOG=Logger.builder(JavaToJNI.class).build();

	
	
	@Override
	public int doWork(final List<String> argv) {
		try {
			final String input = super.oneAndOnlyOneFile(argv);
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document dom = db.parse(new File(input));
			Element root = dom.getDocumentElement();
			for(Node c=root.getFirstChild();c!=null;c=c.getNextSibling()) {
				if(c.getNodeType()!=Node.ELEMENT_NODE) continue;
				if(c.getNodeName().equals("constant")) {
					
					}
				}
			
			final Context context = new VelocityContext();
		    
            final VelocityEngine ve = new VelocityEngine();
            ve.setProperty(RuntimeConstants.RESOURCE_LOADER, "file");
            ve.setProperty("file.resource.loader.class","org.apache.velocity.runtime.resource.loader.FileResourceLoader");
            ve.setProperty("file.resource.loader.path","/home/lindenb/src/jsandbox/src/main/java/sandbox/tools/java2jni");
            //ve.setProperty("file.resource.loader.path",file.getParent()==null?".":file.getParent().toString());
            ve.init();
            final Template template = ve.getTemplate("java2jni.c.vm");
           
            try(PrintWriter w =new PrintWriter(System.out)) {
	            template.merge( context, w);
	            w.flush();
	            }
            
		

		    
		    return 0;
		} catch (final Exception e) {
			e.printStackTrace();
			return -1;
		}
	}

	 public static ProgramDescriptor getProgramDescriptor() {
	    	return new ProgramDescriptor() {
	    		@Override
	    		public String getName() {
	    			return "java2jni";
	    			}
	    		};
	    	}	
	
	
	public static void main(String[] args)
		{
		new JavaToJNI().instanceMainWithExit(args);
		}
	
}
