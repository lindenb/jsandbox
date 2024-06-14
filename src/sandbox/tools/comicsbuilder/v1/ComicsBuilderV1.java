package sandbox.tools.comicsbuilder.v1;

import java.nio.file.Paths;
import java.util.List;

import sandbox.Launcher;
import sandbox.xml.minidom.Element;

public class ComicsBuilderV1 extends Launcher {

	@Override
	public int doWork(List<String> args) {
		try {
			Element root=new sandbox.xml.minidom.MiniDomReader().
				parsePath(Paths.get(oneAndOnlyOneFile(args)));
			root.assertHasLocalName("comics");
			return 0;
			}
		catch(Throwable err) {
			err.printStackTrace();
			return -1;
			}
		
	}
	
	public static void main(String[] args) {
		new ComicsBuilderV1().instanceMainWithExit(args);
	}
}
