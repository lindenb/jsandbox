package sandbox.tools.streamplot;

import java.util.List;

import sandbox.Launcher;
import sandbox.tools.streamplot.parser.ParseException;
import sandbox.tools.streamplot.parser.StreamPlotParser;

public class StreamPlot extends Launcher {


	
@Override
public int doWork(List<String> args) {
	try {
		new StreamPlotParser(System.in).input();
	} catch (ParseException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
		return -1;
	}
	return 0;
	}
public static void main(String[] args) {
	new StreamPlot().instanceMainWithExit(args);
}
}
