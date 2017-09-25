/**
 * Author:
 * 	Pierre Lindenbaum PhD
 * Date:
 * 	May-2011
 * Contact:
 * 	plindenbaum@yahoo.fr
 * Reference:
 *   http://manual.gimp.org/en/gimp-filters-script-fu.html
 * WWW:
 * 	http://plindenbaum.blogspot.com
 * Wiki
 *  
 * Motivation:
 * 		sends a scheme script to a 
 * Compilation:
 *        cd jsandbox; ant gimpclient
 * Usage:
 *        jar gimpclient.jar -P {port} -H {host} (-e {script} ) (file|stdin)
 */
package sandbox;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.net.Socket;

/**
 * 
 * GimpClient
 *
 */
public class GimpClient
	{
	private String host="localhost";
	private int port=10008;
	
	private GimpClient()
		{
		}
	private void run(Reader r) throws Exception
		{
		StringBuilder schemeScriptBuilder=new StringBuilder();
		int c;
		while((c=r.read())!=-1) schemeScriptBuilder.append((char)c);
		String schemeScript=schemeScriptBuilder.toString();
		if(schemeScript.length()>Short.MAX_VALUE) throw new IOException("Script is too large ("+schemeScript.length()+")");
		byte input[]=schemeScript.getBytes();
		OutputStream out=null;
		InputStream in=null;
		Socket socket=null;
		try
			{
			socket= new Socket(this.host,this.port);
			//socket.setSoTimeout(10*1000);
			out=socket.getOutputStream();
			in=socket.getInputStream();
			out.write('G');
			
			
			short queryLength=(short)input.length;
			out.write(queryLength/256);
			out.write(queryLength%256);
			out.write(input);
			out.flush();
			
			int G=in.read();
			if(G!='G') throw new IOException("Expected first byte as G");
			int code=in.read();
			if(code!=0)  throw new IOException("Error code:"+code);
			int contentLength= in.read()*255 + in.read();
			
			byte array[]=new byte[contentLength];
			int nRead=0;
			int n=0;
			while((n=in.read(array, nRead, array.length-nRead))>0)
				{
				nRead+=n;
				}
			if(nRead!=contentLength) throw new IOException("expected "+contentLength+" but got "+nRead+" bytes");
			System.out.println(new String(array));
			}
		catch(Exception err)
			{
			throw new IOException(err);
			}
		finally
			{
			if(in!=null) in.close();
			if(out!=null) out.close();
			if(socket!=null) socket.close();
			}
		}
	
	public static void main(String[] args)
		{
		try {
			GimpClient app=new GimpClient();
			String script=null;
			int optind=0;
			while(optind<args.length)
				{
				if(args[optind].equals("-h"))
					{
					System.out.println("GimpClient. Author:Pierre Lindenbaum PhD.");
					System.out.println("Options:");
					System.out.println("  -H <host> default:"+app.host);
					System.out.println("  -P <port> default:"+app.port);
					System.out.println("  -e 'script' (optional)");
					System.out.println("<stdin|files>");
					return;
					}
				else if(args[optind].equals("-H"))
					{
					app.host=args[++optind];
					}
				else if(args[optind].equals("-P"))
					{
					app.port=Integer.parseInt(args[++optind]);
					}
				else if(args[optind].equals("-e"))
					{
					script=args[++optind];
					}
				else if(args[optind].equals("--"))
					{
					optind++;
					break;
					}
				else if(args[optind].startsWith("-"))
					{
					System.err.println("Unnown option: "+args[optind]);
					return;
					}
				else
					{
					break;
					}
				++optind;
				}
			if(script!=null && optind!=args.length)
				{
				System.err.println("Illegal number of arguments");
				return;
				}
			
			if(script!=null)
				{
				StringReader r=new StringReader(script);
				app.run(r);
				r.close();
				}
			else if(optind==args.length)
				{
				app.run(new InputStreamReader(System.in));
				}
			else
				{
				while(optind< args.length)
					{
					String inputName=args[optind++];
					FileReader in=new FileReader(inputName);
					app.run(in);
					in.close();
					}
				}
			} 
		catch (final Exception err)
			{
			err.printStackTrace();
			}
		}
}	
