package sandbox;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.imageio.ImageIO;

import com.beust.jcommander.Parameter;

public class MontageGif extends Launcher
	{
	private static final Logger LOG = Logger.builder(MontageGif.class).build();
	private static final String ITERWORD="._ITER_.";
	@Parameter(names={"-s","--size"},description="One square size.")
	private int squareSize=100;
	@Parameter(names={"-n"},description="number of images per side")
	private int imagePerSide=3;
	@Parameter(names={"-o","--output"},description="output files. Must end with .png and contain '"+ITERWORD+"'",required=true)
	private File outputFile=null;
	@Parameter(names={"-S","--steps"},description="number of steps between two transitions")
	private int numSteps = 5;

	private final Random rand=new Random(System.currentTimeMillis());
	private final ImageUtils imageUtils = ImageUtils.getInstance();
	private class StepInfo
		{
		int x=0;
		int y=0;
		int stepIdx=0;
		int imageIdx=0;
		int direction=rand.nextInt(4);
		List<String> imageUrls=new ArrayList<>();
		
		Image get(int step) throws IOException {
			if(imageUrls.isEmpty()) {
				final BufferedImage img=new BufferedImage(squareSize, squareSize, BufferedImage.TYPE_INT_RGB);
				final Graphics2D g=img.createGraphics();
				g.setColor(Color.BLACK);
				g.fillRect(0, 0, squareSize, squareSize);
				g.dispose();
				return img;
				}
			else if(imageUrls.size()==1) {
				return getScaledImage(imageUrls.get(0));
				}
			else
				{
				final Image img1 = getScaledImage(this.imageUrls.get((imageIdx  )%this.imageUrls.size()));
				final Image img2 = getScaledImage(this.imageUrls.get((imageIdx+1)%this.imageUrls.size()));
				int sizeStep = (int)(this.stepIdx*(squareSize/(double)numSteps));
				
				
				final BufferedImage img=new BufferedImage(squareSize, squareSize, BufferedImage.TYPE_INT_RGB);
				final Graphics2D g=img.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				switch(this.direction)
					{
					//left to right
					case 0:
					default:
						g.drawImage(
								img1,
								0, 0, sizeStep , squareSize,
								squareSize-sizeStep, 0,squareSize,squareSize, null);
						g.drawImage(
								img2,
								sizeStep, 0, squareSize , squareSize,
								0, 0,squareSize-sizeStep,squareSize, null);
						break;
					}
				
				
				g.dispose();
				
				
				this.stepIdx++;
				if(this.stepIdx>numSteps) {
					LOG.info("REDOOO");
					this.stepIdx=0;
					this.imageIdx++;
					this.direction = rand.nextInt(4);
					}
				return img;
				}
			}
		}
	
	private Image getScaledImage(final String url) throws IOException {
		LOG.info("loading "+url);
		final BufferedImage img = imageUtils.read(url);
		
		double w=img.getWidth();
		double h=img.getHeight();
		double ratio=w/h;
		while(w>this.squareSize || h> this.squareSize)
			    {
			    w*=0.99999;
			    h=w/ratio;
			    }
		return img.getScaledInstance((int)w,(int)h, Image.SCALE_SMOOTH);
		}	
	
	@Override
	public int doWork(final List<String> args)
		{
		try {
			if(outputFile==null || !outputFile.getName().endsWith(".png") || !outputFile.getName().contains(ITERWORD)) {
				LOG.error("bad output file");
				return -1;
				}
			final List<String> imgUrls = IOUtils.unroll(args);			
			
			
			final BufferedImage img=new BufferedImage(
					this.squareSize*this.imagePerSide,
					this.squareSize*this.imagePerSide,
					BufferedImage.TYPE_INT_RGB
					);
			
			final List<StepInfo> steps=new ArrayList<>();
			for(int x=0;x< this.imagePerSide;x++) {
				for(int y=0;y< this.imagePerSide;y++) {
					final StepInfo info = new StepInfo();
					info.x=x*this.squareSize;
					info.y=y*this.squareSize;
					steps.add(info);
					}
				}
			
			int img_idx =0;
			for(final String imFile : imgUrls) {
				steps.get(img_idx%steps.size()).imageUrls.add(imFile);
				img_idx++;
				}
			
			for(int step=0;step <= this.numSteps;++step) {
				LOG.info("step " +(1+step)+"/"+ this.numSteps);
				final Graphics2D g=img.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				
				for(final StepInfo stepInfo: steps) {
					Image img2=stepInfo.get(step);
					g.drawImage(img2,stepInfo.x,stepInfo.y,squareSize,squareSize,null);
					}
								
				g.dispose();
				final File stepFile = new File(outputFile.getParentFile(),outputFile.getName().replace(ITERWORD, String.format(".%03d.", step)));
				LOG.info("saving to "+stepFile);
				ImageIO.write(img, "PNG", stepFile);
				}
			
			
			return 0;
			}
		catch(Exception err) {
			LOG.error(err);
			return -1;
			}
		}
		
	public static void main(final String[] args)
		{
		new MontageGif().instanceMainWithExit(args);
		}
	}
