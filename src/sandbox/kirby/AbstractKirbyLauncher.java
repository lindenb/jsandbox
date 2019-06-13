package sandbox.kirby;

import java.awt.Color;
import java.awt.Dimension;
import java.nio.file.Path;

import com.beust.jcommander.Parameter;

import sandbox.ColorParser;
import sandbox.DimensionConverter;
import sandbox.IOUtils;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.SimpleGraphics;

public abstract class AbstractKirbyLauncher extends Launcher {
	private static final Logger LOG = Logger.builder(AbstractKirbyLauncher.class).build();
	@Parameter(names= {"-d","--size","--dim","--dimension"},converter=DimensionConverter.StringConverter.class,description=DimensionConverter.OPT_DESC,required=true)
	protected Dimension dimIn = null;
	@Parameter(names= {"-b","--background",},description="background-color. "+ColorParser.OPT_DESC,converter=ColorParser.Converter.class)
	protected Color background_color = null;
	@Parameter(names= {"-o","--output",},description="output")
	protected Path output = null;

	protected abstract void paint(final SimpleGraphics g);
	
	protected int paint() {
		SimpleGraphics g = null;
		try {
			g = SimpleGraphics.openPath(this.output,this.dimIn.width,this.dimIn.height);
			Color bckg = this.background_color;
			
			if(bckg==null && this.output!=null && !this.output.getFileName().toString().toLowerCase().endsWith(".png")) {
				bckg = Color.WHITE;
				}
			
			if(bckg!=null) {
				g.setFill(bckg);
				g.rect(0, 0, g.getWidth(), g.getHeight());
				}
			g.setFill(Color.BLACK);
			paint(g);
			g.close();
			g=null;
			return 0;
			}
		catch(final Throwable err) {
			LOG.error(err);
			return -1;
			}
		finally
			{
			IOUtils.close(g);
			}
		}
}
