package sandbox.tools.yaml2xml;

import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.List;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.events.Event;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.io.IOUtils;

public class YamlToXml extends Launcher {
	private static final Logger LOG = Logger.builder(YamlToXml.class).build();

	@Override
	public int doWork(List<String> args) {
		try {
			final Yaml yaml = new Yaml();
			XMLOutputFactory xof  = XMLOutputFactory.newFactory();
			XMLStreamWriter w=xof.createXMLStreamWriter(System.out, "UTF-8");
			for(Path path :IOUtils.unrollPaths(args)) {
				try(Reader r= Files.newBufferedReader(path)) {
					Iterator<Event> iter = yaml.parse(r).iterator();
					while(iter.hasNext()) {
						final Event evt= iter.next();
						switch(evt.getEventId()) {
							case DocumentStart:
							case DocumentEnd:
								break;
							case Comment:
								w.writeComment(evt.toString());
								break;
							case SequenceStart:
							case SequenceEnd:
							}
						}
					}
				}
			return 0;
			}
		catch(Throwable err ) {
			
			return -1;
			}
		}
	
	public static void main(String[] args) {
		new YamlToMoodle().instanceMainWithExit(args);

	}

}
