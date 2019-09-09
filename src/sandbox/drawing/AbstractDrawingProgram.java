package sandbox.drawing;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.nio.file.Path;
import java.util.List;

import com.beust.jcommander.Parameter;

import sandbox.ColorParser;
import sandbox.ImageUtils;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.jcommander.DimensionConverter;
import sandbox.jcommander.NoSplitter;

public abstract class AbstractDrawingProgram extends Launcher {
	private static final Logger LOG = Logger.builder(AbstractDrawingProgram.class).build();
	@Parameter(names= {"-D","--size","--dim","--dimension"},converter=DimensionConverter.StringConverter.class,description=DimensionConverter.OPT_DESC,required=true)
	private Dimension dimIn = null;
	@Parameter(names= {"-b","--background","--paper"},description="background-color. "+ColorParser.OPT_DESC,converter=ColorParser.Converter.class,splitter=NoSplitter.class)
	private Color bckg = Color.WHITE;
	@Parameter(names= {"-o","--output",},description="output")
	private Path output = null;
	
	private final ImageUtils imageUtils = ImageUtils.getInstance();

	public ImageUtils getImageUtils() {
		return imageUtils;
		}
	
	protected abstract int paint(final BufferedImage img,final Graphics2D g);
	
	@Override
	public int doWork(final List<String> args) {
		if(!args.isEmpty()) {
			LOG.error("illegal number of arguments "+String.join(",",args));
			return -1;
			}
		try
			{
			final BufferedImage img= new BufferedImage(dimIn.width, dimIn.height, BufferedImage.TYPE_INT_RGB);
			final Graphics2D g=img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setColor(bckg);
			g.fillRect(0, 0, img.getWidth(), img.getHeight());
			if(paint(img,g)!=0) return -1;
			g.dispose();
			getImageUtils().saveToPathOrStandout(img, output);
			return 0;
			}
		catch(final Throwable err) {
			LOG.error(err);
			return -1;
			}
		}
}
