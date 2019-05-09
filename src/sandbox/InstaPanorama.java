package sandbox;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;

import com.beust.jcommander.Parameter;

public class InstaPanorama extends Launcher {
	private static final String TOKEN="__TOKEN__";
	private static final Logger LOG = Logger.builder(InstaPanorama.class).build();

	@Parameter(names={"-o","--output"},description="Output file pattern. MUST contain \""+TOKEN+"\" .",required=true)
	private String output;
	@Parameter(names={"--size"},description="Output size")
	private int ig_size = 1080;
	@Parameter(names={"-b","--background"},description="background Color",converter=ColorParser.Converter.class,splitter=NoSplitter.class)
	private Color bckgColor = Color.WHITE;


	
	private ImageUtils imageUtils = ImageUtils.getInstance();
	@Override
	public int doWork(List<String> args) {
		try {
			if(StringUtils.isBlank(this.output) || !this.output.contains(TOKEN)) {
				LOG.error("output "+ this.output+" must contain "+TOKEN);
				return -1;
			}
			
			if(ig_size < 100) {
				LOG.error("IG height is too small");
				return -1;
			}
			
			BufferedImage img = this.imageUtils.read(oneFileOrNull(args));
			img = imageUtils.scaleForHeight(img,this.ig_size);
			final BufferedImage sliceImg = new BufferedImage(this.ig_size,this.ig_size, BufferedImage.TYPE_INT_RGB);
			
			int x=0;
			int n = 1;
			while(x< img.getWidth()) {
				final Graphics2D g = imageUtils.createGraphics(sliceImg);
				g.setColor(this.bckgColor);
				g.fillRect(0, 0, this.ig_size, this.ig_size);
				
				System.err.println("dx2 "+Math.min(img.getWidth()-(x+this.ig_size), this.ig_size));
				
				g.drawImage(img,
						//destination
						0,0,
						Math.min((x+this.ig_size)-img.getWidth(), this.ig_size),
						this.ig_size,
						//source
						x,
						0, 
						Math.min(x+this.ig_size, img.getWidth()),
						this.ig_size,
						
						null
						);
				
				g.dispose();
				final String filename= this.output.replaceAll(TOKEN, String.format("%02d", n));
				LOG.info(filename);
				ImageIO.write(sliceImg, imageUtils.formatForFile(filename), new File(filename));
				x += this.ig_size;
				n++;
			}
			
			return 0;
		} catch(final Throwable err) {
			LOG.error(err);
			return -1;
			}
		}
	
	public static void main(final String[] args) {
		new InstaPanorama().instanceMainWithExit(args);

	}

}
