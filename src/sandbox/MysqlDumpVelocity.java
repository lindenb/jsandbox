package sandbox;

import java.io.File;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.xml.transform.stream.StreamSource;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;


public class MysqlDumpVelocity
	{
	protected String vmTemplateDir=".";
	protected String vmTemplateName=null;
	protected Map<String,String> userProperties=new HashMap<String,String>();

	public VelocityContext createContext()
		{
		VelocityContext ctx=new VelocityContext();
		for(String k:this.userProperties.keySet())
			{
			ctx.put(k, this.userProperties.get(k));
			}
		return ctx;
		}
	
	
	protected void usage(PrintStream out)
		{
		out.println(" -h/--helo this screen.");
		out.println(" --vmpath (dir) path to velocity templates.");
		out.println(" --vm (file) velocity template.");
		out.println(" -D (key) (value) . Add this property/value to the velocity context.");
		}

	
	public void run(String[] args) throws Exception
	{

	int next_optind;
	int optind=0;
	while(optind<args.length)
		{
		
		if(args[optind].equals("-h") || args[optind].equals("--help"))
			{
			usage(System.out);
			}
		else if(args[optind].equals("--vmpath") && optind+1< args.length)
			{
			vmTemplateDir= args[++optind];
			}
		else if(args[optind].equals("--vm") && optind+1< args.length)
			{
			vmTemplateName = args[++optind];
			}
	else if(args[optind].equals("-D") && optind+2< args.length)
		{
		String k = args[++optind];
		String v = args[++optind];
		this.userProperties.put(k,v);
		}
		else if(args[optind].equals("--"))
			{
			optind++;
			break;
			}
		else if(args[optind].startsWith("-"))
			{
			System.err.println("Unnown option: "+args[optind]);
			System.exit(-1);
			}
		else
			{
			break;
			}
		++optind;
		}

	
	if(this.vmTemplateDir==null)
		{
		System.err.println("Undefined velocity template path.  Using '.'.");
		vmTemplateDir=".";
		}
	if(this.vmTemplateName==null)
		{
		System.err.println("Undefined velocity template name.");
		System.exit(-1);
		}
	
	 javax.xml.bind.JAXBContext jaxbCtxt=javax.xml.bind.JAXBContext.newInstance(
			  	Mysqldump.class
			  	);
	 javax.xml.bind.Unmarshaller unmarshaller=jaxbCtxt.createUnmarshaller();
	if(optind+1!=args.length)
		{
		System.exit(-1);
		}
	
	Mysqldump dump=unmarshaller.unmarshal(new StreamSource(new File(args[optind++])),
				Mysqldump.class
				).getValue();
	Properties props = new Properties();
	props.put(Velocity.RESOURCE_LOADER, "file");
	props.put("file.resource.loader.path",vmTemplateDir);
	VelocityContext ctx=createContext();
	ctx.put("mysqldump", dump);
	
	VelocityEngine ve = new VelocityEngine();
	ve.init(props);
	Template template= ve.getTemplate(this.vmTemplateName);
	Writer writer=new PrintWriter(System.out);
	template.merge(ctx, writer);
	writer.flush();
	writer.close();
	}		

public static void main(String[] args) throws Exception
	{
	MysqlDumpVelocity app=new MysqlDumpVelocity();
	app.run(args);
	}

}
