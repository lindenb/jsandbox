package sandbox.tools.img2palette;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.StringUtils;
import sandbox.io.IOUtils;

public class ImageToPalette extends Launcher {
	private static final Logger LOG=Logger.builder(ImageToPalette.class).build();
	@Parameter(names={"-o"},description=OUTPUT_OR_STANDOUT)
	private Path output;

	@Parameter(names={"-n"},description="palette size")
	private int palette_size=16;
	
	private  static double distance(Color c1,Color c2) {
		return 
			Math.pow(c1.getRed()-c2.getRed(),2) +
			Math.pow(c1.getGreen()-c2.getGreen(),2) +
			Math.pow(c1.getBlue()-c2.getBlue(),2)
			;
	}
	
	@Override
	public int doWork(final List<String> args) {
		try {
			final String input = super.oneFileOrNull(args);
			final BufferedImage img ;
			
			if(input==null) {
				img = ImageIO.read(System.in);
				} else if(IOUtils.isURL(input)) {
					try(InputStream in = IOUtils.openStream(input)) {
						img = ImageIO.read(in);
					}
				} else
					{
					img=	ImageIO.read(new File(input));
					}
			
					
				
			
			
			
			final BufferedImage lImage=new BufferedImage(
					img.getWidth(),
					img.getHeight(),
					BufferedImage.TYPE_BYTE_INDEXED
					);	
			Graphics2D g=lImage.createGraphics();
			g.drawImage(img, 0, 0, null);
			g.dispose();
			
			final Map<Integer,Long> color2count=new HashMap<>();
			for(int x=0;x< lImage.getWidth();++x) {
				for(int y=0;y< lImage.getHeight();++y) {
					final int color=lImage.getRGB(x, y);
					color2count.put(color,1L+color2count.getOrDefault(color, 0L));
				}
			}
			
			while(color2count.size()>this.palette_size)
				{
				//find two closest color
				int x1=-1;
				int x2=-1;
				double min_diff=-1;
				for(Integer i:color2count.keySet()) {
					for(Integer j:color2count.keySet()) {
						if(i>=j) continue;
						double diff= distance(new Color(i),new Color(j));
								
						if(min_diff<0 || diff < min_diff)
							{
							x1=i;
							x2=j;
							min_diff=diff;
							}
						}
					}
				Color c1 =new Color(x1);
				Color c2 =new Color(x2);
				long n1 = color2count.get(x1);
				long n2 = color2count.get(x2);
				int re= (int)((c1.getRed()*n1+c2.getRed()*n2)/(n1+n2));
				int gr= (int)((c1.getGreen()*n1+c2.getGreen()*n2)/(n1+n2));
				int bl= (int)((c1.getBlue()*n1+c2.getBlue()*n2)/(n1+n2));
				Color c3=new Color(Math.min(re,255),Math.min(gr,255), Math.min(bl,255));
				color2count.remove(x1);
				color2count.remove(x2);
				color2count.put(c3.getRGB(),n1+n2);
				
				}	

			
			
			final List<Color> colors1 = color2count.
				entrySet().
				stream().
				sorted((A,B)->Long.compare(B.getValue(), A.getValue())).
				map(E->E.getKey()).
				map(K->new Color(K)).
				collect(Collectors.toCollection(()->new ArrayList<>()));
			
			
			final List<Color> colors=new ArrayList<>(colors1.size());
			colors.add(colors1.remove(0));
			while(!colors1.isEmpty()) {
				final Color last=colors.get(colors.size()-1);
				int x=-1;
				for(int i=0;i< colors1.size();i++) {
					if(x==-1 || distance(colors1.get(x),last) > distance(colors1.get(i),last))
						{
						x=i;
						}
					}
				colors.add(colors1.remove(x));
				}
			if(this.output==null) {
				LOG.info("file should be saved as a '.gpl' file under ${HOME}/.config/GIMP/2.10/palettes/F ");
				}
			
			final String md5=StringUtils.md5(colors.stream().
						map(C->String.valueOf(C.getRGB())).collect(Collectors.joining(";"))
						);
			
			try(PrintWriter pw=super.openPathAsPrintWriter(this.output)) {
				pw.println("GIMP Palette");
				pw.println("Name: " + md5.substring(0,5)+"."+this.palette_size);
				pw.println("Columns: "+(int)Math.ceil(Math.sqrt(colors.size())));
				pw.println("#");
				
				for(Color c:colors) {
					pw.printf("%3d %3d %3d\n",c.getRed(),c.getGreen(),c.getBlue());
					}
				pw.flush();
				}
			return 0;
			}
		catch(Exception err)
			{
			LOG.error(err);
			return -1;
			}
		finally
			{
			
			}
		}
	public static void main(String[] args) {
		new ImageToPalette().instanceMain(args);

	}

}
