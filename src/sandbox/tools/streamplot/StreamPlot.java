package sandbox.tools.streamplot;

import java.awt.Dimension;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.graphics.Canvas;
import sandbox.io.IOUtils;
import sandbox.jcommander.DimensionConverter;
import sandbox.jcommander.NoSplitter;
import sandbox.tools.streamplot.parser.StreamPlotParser;
import sandbox.util.function.FunctionalMap;

public class StreamPlot extends Launcher {
	private static final Logger LOG=Logger.builder(StreamPlot.class).build();

	@Parameter(names= {"-o","--out"},description=OUTPUT_OR_STANDOUT)
	private Path outPath = null;
	@Parameter(names= {"--size","--dimension"},description=DimensionConverter.OPT_DESC,converter = DimensionConverter.StringConverter.class, splitter = NoSplitter.class )
	private Dimension dimension=new Dimension(600,300);


@Override
public int doWork(List<String> args) {
	try {
		final FunctionalMap<String, Object> props = new FunctionalMap<String,Object>().
				plus("width", dimension.getWidth(),"height", dimension.getHeight());
		final List<Path> paths=IOUtils.unrollPaths(args);
		try(Canvas canvas = Canvas.open(outPath, props)) {
		if(paths.isEmpty()) {
			final StreamPlotParser parser=new StreamPlotParser(System.in);
			parser.setCanvas(canvas);
			parser.input();
			}
		else for(Path p:paths) {
			try(InputStream in =  IOUtils.openPathAsInputStream(p)) {
				final StreamPlotParser parser=new StreamPlotParser(in);
				parser.setCanvas(canvas);
				parser.input();
				}
			}
		return 0;
		}
	} catch (Throwable e) {
		LOG.error(e);
		return -1;
		}
	}
public static void main(String[] args) {
	new StreamPlot().instanceMainWithExit(args);
}
}
