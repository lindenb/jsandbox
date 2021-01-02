/* for P in in {1..50} ; do wget -q -O  - "http://<site>.tumblr.com/page/${P}" | tr '"' '\n' | grep  https | grep media | grep -F '_500.jpg'  | grep -v 0afe983308d5f82323f9006f85dcaafe | sort | uniq ; done | sort | uniq > jeter.list 
 */
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
import java.util.WeakHashMap;

import javax.imageio.ImageIO;

import com.beust.jcommander.Parameter;

import sandbox.io.IOUtils;

public class MontageGif extends Launcher
	{
	private static final Logger LOG = Logger.builder(MontageGif.class).build();
	private static final String ITERWORD="._ITER_.";
	@Parameter(names={"-s","--size"},description="One square size.")
	private int squareSize= 70;
	@Parameter(names={"-n"},description="number of images per side")
	private int imagePerSide = 5;
	@Parameter(names={"-o","--output"},description="output files. Must end with .png and contain '"+ITERWORD+"'",required=true)
	private File outputFile=null;
	@Parameter(names={"-S","--steps"},description="number of steps between two transitions")
	private int numSteps = 10;
	@Parameter(names={"-b","--background"},description="background color.",converter=ColorParser.Converter.class)
	private Color backgroundColor = Color.BLACK;

	private final Random rand=new Random(System.currentTimeMillis());
	private final ImageUtils imageUtils = ImageUtils.getInstance();
	private final List<String> all_urls = new ArrayList<>();
	private int image_index = 0;
	private int count_complete = 0;
	private final WeakHashMap<String,Image> imageCache = new WeakHashMap<>();
	
	private class StepInfo
		{
		int x=0;
		int y=0;
		int stepIdx=0;
		int imageIdx1=0;
		int imageIdx2=0;
		int direction=rand.nextInt(4);
	
		
		Image get(int step) throws IOException {
			if(MontageGif.this.all_urls.isEmpty()) {
				final BufferedImage img=new BufferedImage(squareSize, squareSize, BufferedImage.TYPE_INT_RGB);
				final Graphics2D g=img.createGraphics();
				g.setColor(backgroundColor);
				g.fillRect(0, 0, squareSize, squareSize);
				g.dispose();
				return img;
				}
			else if(MontageGif.this.all_urls.size()==1) {
				return getScaledImage(0);
				}
			else
				{
				final Image img1 = getScaledImage(this.imageIdx1);
				final Image img2 = getScaledImage(this.imageIdx2);
				final int dxy1 = (int)(this.stepIdx*(squareSize/(double)numSteps));
				
				final BufferedImage img=new BufferedImage(squareSize, squareSize, BufferedImage.TYPE_INT_RGB);
				final Graphics2D g=img.createGraphics();
				g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
				g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				
				switch(this.direction)
					{
					//left to right
					case 0:
						g.drawImage(img1,-1 * (dxy1),0,null);
						g.drawImage(img2,-1 * (dxy1) + squareSize,0,null);
						break;
					//right to left
					case 1:
						g.drawImage(img1, 1 * (dxy1),0,null);
						g.drawImage(img2, 1 * (dxy1) - squareSize,0,null);
						break;
					case 2:
						g.drawImage(img1,0,-1 * (dxy1),null);
						g.drawImage(img2,0,-1 * (dxy1) + squareSize,null);
						break;
					default:
						g.drawImage(img1,0, 1 * (dxy1),null);
						g.drawImage(img2,0, 1 * (dxy1) - squareSize,null);
						break;
					}
				
				
				g.dispose();
				
				
				this.stepIdx++;
				if(this.stepIdx>numSteps) {
					this.stepIdx=0;
					//update index1
					this.imageIdx1 = this.imageIdx2;
					this.imageIdx2 = MontageGif.this.nextIndex();
					
					this.direction = rand.nextInt(4);
					}
				return img;
				}
			}
		}
	
	private Image getScaledImage(final int index) throws IOException {
		return getScaledImage(all_urls.get(index%this.all_urls.size()));
		}
	
	private Image getScaledImage(final String url) throws IOException {
		Image cached = this.imageCache.get(url);
		if(cached!=null) return cached;
	
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
		cached =  img.getScaledInstance((int)w,(int)h, Image.SCALE_SMOOTH);
		this.imageCache.put(url,cached);
		return cached;
		}	
	
	private int nextIndex() {
		final int i= this.image_index;
		this.image_index++;
		if(this.image_index>=this.all_urls.size()) {
			this.image_index=0;
			this.count_complete++;
			}
		return i;
		}
	
	@Override
	public int doWork(final List<String> args)
		{
		try {
			if(outputFile==null || !outputFile.getName().endsWith(".png") || !outputFile.getName().contains(ITERWORD)) {
				LOG.error("bad output file");
				return -1;
				}
			this.all_urls.addAll(IOUtils.unroll(args));			
			if(this.all_urls.isEmpty()) {
				LOG.error("no input");
				return -1;
			}
			
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
					
					info.imageIdx1 = nextIndex();
					info.imageIdx2 = nextIndex();
					steps.add(info);
					}
				}
			int file_out_index = 0;
			while(this.count_complete==0) {
				for(int step=0;step <= this.numSteps;++step) {
					LOG.info("step " +(1+step)+"/"+ this.numSteps);
					final Graphics2D g=img.createGraphics();
					g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
					g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
					
					for(final StepInfo stepInfo: steps) {
						final Image img2=stepInfo.get(step);
						g.drawImage(img2,stepInfo.x,stepInfo.y,squareSize,squareSize,null);
						}
									
					g.dispose();
					final File stepFile = new File(outputFile.getParentFile(),outputFile.getName().replace(ITERWORD, String.format(".%05d.", file_out_index)));
					LOG.info("saving to "+stepFile);
					ImageIO.write(img, "PNG", stepFile);
					file_out_index++;
					}
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
