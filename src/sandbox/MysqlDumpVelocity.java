package sandbox;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;

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

	
	public static class VelocityWriter extends Writer
			{
			private PrintWriter stdout;
			private Map<String,PrintWriter> uri2writer=new HashMap<String, PrintWriter>();
			private Stack<PrintWriter> writerStack=new Stack<PrintWriter>();
			
			public VelocityWriter() throws IOException
				{
				stdout=new PrintWriter(System.out);
				this.writerStack.push(this.stdout);
				}
			
			public void push(String filename) throws IOException
				{
				PrintWriter pw=null;
				if(filename==null || filename.equals("") || filename.equals("-") || filename.equals("stdout"))
					{
					pw=stdout;
					}
				else
					{
					pw=uri2writer.get(filename);
					if(pw==null)
						{
						pw=new PrintWriter(filename);
						uri2writer.put(filename,pw);
						}
					}
				this.writerStack.push(pw);
				}
			
			public void pop()throws IOException
				{
				if(this.writerStack.size()==0)
					{
					throw new IllegalStateException("Writer 'pop'-ed but no more writer in the stack.");
					}
				this.writerStack.pop();
				}

			
			
			public PrintWriter getWriter()
				{
				return this.writerStack.peek();
				}
			
			@Override
			public void close() throws IOException
				{
				PrintWriter pw=getWriter();
				if(pw!=stdout)
					{
					pw.flush();
					pw.close();
					}
				}
	
			@Override
			public void flush() throws IOException
				{
				getWriter().flush();
				}
	
			@Override
			public void write(char[] cbuf, int off, int len) throws IOException
				{
				getWriter().write(cbuf,off,len);
				
				}
			void closeAll() throws IOException
				{
				for(PrintWriter pw:this.uri2writer.values())
					{
					pw.flush();
					pw.close();
					}
				stdout.flush();
				
				}
			}
	
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
		
		out.println();
		out.println("$mysqldump is the mysqldump structure passed to velocity.");
		out.println("$vmwriter is the writer passed to velocity. default: print to stdout. but one can use\n" +
				"$vmwriter.push(filename); #filename=null or empty or '-' = stdout\n" +
				"$vmwriter.pop();\n" 
				);
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
			else if((
					  args[optind].equals("--vm") ||
					  args[optind].equals("-vm")  ||
					  args[optind].equals("-v")
					  )&& optind+1< args.length)
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
				  	Mysqldump.class,Options.class
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
		VelocityWriter writer=new VelocityWriter();
		ctx.put("vmwriter",writer);
		Template template= ve.getTemplate(this.vmTemplateName);
		template.merge(ctx, writer);
		writer.closeAll();
		}		

public static void main(String[] args)
	{
	try
		{
		MysqlDumpVelocity app=new MysqlDumpVelocity();
		app.run(args);
		}
	catch(Throwable err)
		{
		err.printStackTrace();	
		}
	}

}
