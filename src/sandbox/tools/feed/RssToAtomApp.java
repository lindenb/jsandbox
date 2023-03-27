package sandbox.tools.feed;

import java.io.File;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.io.IOUtils;
import sandbox.xml.XMLSerializer;

public class RssToAtomApp extends Launcher
	{
	private static final Logger LOG=Logger.builder(RssToAtomApp.class).build();
	
	@Parameter(names= {"-o","--out"},description = "ouput file or stdout")
	private Path outputFile = null;
	
	@Override
	public int doWork(final List<String> args)
		{
		try {
			final String input = oneFileOrNull(args);
			final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			final DocumentBuilder db = dbf.newDocumentBuilder();
			final Document rss;
			if(input==null) {
				rss = db.parse(System.in);
				}
			else if(IOUtils.isURL(input)) {
				rss = db.parse(input);
				}
			else
				{
				rss = db.parse(new File(input));
				}
			try(OutputStream out = super.openPathAsOuputStream(this.outputFile)) {
			    new XMLSerializer().serialize(
			    		new sandbox.feed.RssToAtom().apply(rss),
			    		out
			    		);
				out.flush();
				}
		    return 0;
			}
		catch(final Throwable err)
			{
			LOG.error(err);
			return -1;
			}
		
		}
	public static void main(final String[] args)
		{
		new RssToAtomApp().instanceMainWithExit(args);
		}
	}
