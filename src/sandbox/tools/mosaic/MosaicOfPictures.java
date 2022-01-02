package sandbox.tools.mosaic;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Set;
import javax.imageio.ImageIO;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import com.beust.jcommander.Parameter;

import sandbox.ColorParser;
import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.http.CookieStoreUtils;
import sandbox.io.IOUtils;

public class MosaicOfPictures extends Launcher
	{
	private static final Logger LOG=Logger.builder(MosaicOfPictures.class).build();
	private HttpClientBuilder builder = null;
	
	private MosaicOfPictures()
		{
		}
	
	private Set<String> readPictures(BufferedReader r)
		throws IOException
		{
		final Set<String> set=new LinkedHashSet<String>();
		String line;
		while((line=r.readLine())!=null)
			{
			if(StringUtils.isBlank(line)|| line.startsWith("#")) continue;
			final int tab=line.indexOf('\t');
			if(tab==-1)
				{
				set.add(line.trim());
				}
			else
				{
				set.add(line.substring(0, tab));
				}
			}
		return set;
		}
	
	@Parameter(names={"-w","-width","--width"},description="final image width")
	private int image_size=400;
	@Parameter(names={"-o","--out"},description="image output")
	private File outFile=null;
	@Parameter(names={"-x","--extend"},description="square INSIDE picture")
	private boolean squareinside=false;
	@Parameter(names={"-b","--background"},description="background color")
	private String backgroundStr="black";
	@Parameter(names="--cache",description="Cache directory")
	private File cacheDir = null;
	@Parameter(names={"-c","--cookies"},description=CookieStoreUtils.OPT_DESC)
	private Path cookieStoreFile  = null;
	@Parameter(names={"-i","--skip"},description="skip errors")
	private boolean skip_errors = false;

	
	
	private BufferedImage readRemoteImage(String url) throws IOException{
		String suff="JPG";
		if(url.toLowerCase().endsWith("png")) suff="png";
		final String md5 = StringUtils.md5(url)+"."+suff;
		File cached =null;
		if(cacheDir!=null && this.cacheDir.exists() && this.cacheDir.isDirectory()) {
			cached = new File(this.cacheDir,md5);
			if(cached.exists() && cached.isFile() && cached.canRead()) {
				LOG.error("read from cache "+url);
				return ImageIO.read(cached);
				}
			}
		try(CloseableHttpClient client = this.builder.build()) {
			CloseableHttpResponse resp=null;
			InputStream in=null;
			try {
				resp = client.execute(new HttpGet(url));
				if(resp.getStatusLine().getStatusCode()!=200) {
					LOG.error("cannot fetch "+url+" "+resp.getStatusLine());
					return null;
					}
				in = resp.getEntity().getContent();
				final BufferedImage img =  ImageIO.read(in);
				if(cached!=null) {
					ImageIO.write(img, suff, cached);
					}
				return img;
				}
			catch(final IOException err) {
				LOG.error(err);
				throw err;
				}
			finally
				{
				IOUtils.close(in);
				IOUtils.close(resp);
				}

			}
		}
	
	private BufferedImage readImage(String imgFile) throws IOException{
		if(IOUtils.isURL(imgFile)) {
			return readRemoteImage(imgFile);
			}
		else
			{
			return ImageIO.read(new File(imgFile));
			}
		}
	
	@Override
	public int doWork(final java.util.List<String> args) {
		
		try
			{
			this.builder = HttpClientBuilder.create();
			this.builder.setUserAgent(IOUtils.getUserAgent());
			if(this.cookieStoreFile!=null) {
				final BasicCookieStore cookies = CookieStoreUtils.readTsv(this.cookieStoreFile);
				this.builder.setDefaultCookieStore(cookies);
				}

			
			
			final Set<String>  picts;	
			final Color background= ColorParser.getInstance().apply(this.backgroundStr);
			if(background==null) {
				LOG.error("Bad background color : "+this.backgroundStr);
				return -1;
				}
			if(this.outFile==null)
				{
				LOG.error("No output file");
				return -1;
				}			
			if(args.isEmpty())
				{
				picts=readPictures(new BufferedReader(new InputStreamReader(System.in)));
				}
			else if(args.size()==1)
				{
				try(final BufferedReader r= Files.newBufferedReader(Paths.get(args.get(0)))) {
					picts=readPictures(r);
					}
				}
			else
				{
				LOG.error("Illegal number of arguments.");
				return -1;
				}
			
			LOG.info("Number of images: "+picts.size());
			if(picts.isEmpty())
				{
				LOG.error("No images");
				return -1;
				}
			final int per_side=(int)Math.ceil(Math.sqrt(picts.size()));
			final double one_length =image_size/Math.ceil(Math.sqrt(picts.size()));
			
			
			BufferedImage img=new BufferedImage(
					image_size,
					image_size,
					BufferedImage.TYPE_INT_RGB
					);
			final Graphics2D g=img.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g.setColor(background);
			g.fillRect(0,0,image_size,image_size);
			
			
			int y=0;
			int x=0;
			int nReads=0;
			for(final String file:picts)
				{
				++nReads;
				LOG.info(file+" "+nReads+"/"+picts.size());
				BufferedImage img2;
				try {
					img2 = this.readImage(file);
				} catch(Throwable err) {
					LOG.error(err);
					img2 = null;
					}
				if(img2==null)
					{
					LOG.error("Cannot read "+file);
					if(skip_errors) continue;
					return -1;
					}
				double w=img2.getWidth();
				double h=img2.getHeight();
				
				
			    if(squareinside) {
				  if(w>h ) {
				        img2 = img2.getSubimage((int)((w-h)/2.0),0,(int)h,(int)h);
				        w=h;
				        }
				   else //h>w
				        {
				        img2 = img2.getSubimage(0,(int)((h-w)/2.0),(int)w,(int)w);
				        h=w;
				        }
				    }
				double ratio=w/h;

				    while(w>one_length || h> one_length)
					    {
					    w*=0.99999;
					    h=w/ratio;
					    }
				    g.drawImage(
						    img2,
						    (int)(x*one_length+(one_length-(int)w)/2.0),
						    (int)(y*one_length+(one_length-(int)h)/2.0),
						    (int)w,
						    (int)h,
						    null);
				img2=null;
				
				x++;
				if(x>=per_side)
					{
					x=0;
					++y;
					}
				}
			g.dispose();
			LOG.info("Saving");
			ImageIO.write(img, outFile.getName().toLowerCase().endsWith(".png")?"PNG":"JPG", outFile);
			return 0;
			}
		catch(final Throwable err)
			{
			LOG.error(err.getMessage());
			return -1;
			}
		}
	public static void main(final String[] args) throws Exception
		{
		new MosaicOfPictures().instanceMainWithExit(args);
		}
	}
