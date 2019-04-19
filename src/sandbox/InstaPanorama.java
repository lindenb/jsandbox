package sandbox;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

import javax.imageio.ImageIO;

public class InstaPanorama extends Launcher {
	private static final String TOKEN="__ID__";
	private static final Logger LOG = Logger.builder(InstaPanorama.class).build();

	private ImageUtils imageUtils = ImageUtils.getInstance();
	private static final int IG_SIZE = 500;
	@Override
	public int doWork(List<String> args) {
		try {
			BufferedImage img = this.imageUtils.read(oneFileOrNull(args));
			img = imageUtils.scaleForHeight(img,IG_SIZE);
			
			BufferedImage sliceImg = new BufferedImage(IG_SIZE, IG_SIZE, BufferedImage.TYPE_INT_RGB);
			
			int x=0;
			while(x< img.getWidth()) {
				Graphics2D g = imageUtils.createGraphics(sliceImg);
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, IG_SIZE, IG_SIZE);
				g.drawImage(img,
						x, 0, 
						Math.min(x+IG_SIZE, img.getWidth()),IG_SIZE,
						Math.min(0, 0),0,
						0,IG_SIZE,
						null
						);
				
				g.dispose();
				String filename=".png";
				LOG.info(filename);
				filename.replaceAll("", String.format("%02d", x)));
				ImageIO.write(sliceImg, imageUtils.formatForFile(filename), new File(filename));
				x += IG_SIZE;
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
