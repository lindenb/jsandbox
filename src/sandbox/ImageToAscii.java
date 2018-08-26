package sandbox;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import javax.imageio.ImageIO;

import com.beust.jcommander.Parameter;

public class ImageToAscii extends Launcher {
	private static final Logger LOG = Logger.builder(ImageToAscii.class).build();
	private final static int SIZE=11;
	@Parameter(names={"-c","--cols","--columns"},description="Columns")
	private int columns=80;
	
	
private class Tixel {
	char c;
	BitSet bitSet;
	float percent;
	Tixel(char c) {
		this.c=c;
		this.bitSet = new BitSet(SIZE*SIZE);
		}
	}
private List<Tixel> alphabet = new ArrayList<>();
private void initAlphabet() {
	
	
	final BufferedImage img = new BufferedImage(
			SIZE,SIZE,
			BufferedImage.TYPE_INT_RGB
			);
	final Font font = new Font("Courier",Font.PLAIN,18);

	final String letters=" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~";
	for(int i=0;i < letters.length();++i) {
		final Tixel tixel=new Tixel(letters.charAt(i));
		final Graphics2D g=img.createGraphics();
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, SIZE, SIZE);
		g.setFont(font);
		g.setColor(Color.BLACK);
		g.drawString(letters.substring(i,i+1), 0, SIZE);
		g.dispose();
		//System.err.println("> "+tixel.c);
		 for (int y = 0; y < SIZE; y++) {
	         for (int x = 0; x < SIZE; x++) {
	        	 Color c=new Color(img.getRGB(x, y));
	        	 boolean isblack = c.getRed()==0;
	        	 tixel.bitSet.set(y*SIZE+x,isblack);
	        	 //System.err.print(isblack?"#":" ");
	         }
	         //System.err.println();
	     }
		tixel.percent = grayOf(tixel.bitSet);
		this.alphabet.add(tixel);
		}
	}

private float grayOf(final BitSet bs1) {
	float n=0;
	for(int x=0;x< SIZE*SIZE;++x) {
		n+=bs1.get(x)?1f:0f;
		}
	return n/(SIZE*SIZE);
	}
private int diff(final BitSet bs1,final BitSet bs2)
	{
	int n=0;
	for(int x=0;x< SIZE*SIZE;++x) {
		boolean b1 = bs1.get(x);
		boolean b2 = bs2.get(x);
		n+=(b1!=b2?-1:1);
		}
	return n;
	}
private void display(final PrintStream out,final BufferedImage srcImg) throws IOException {
	
	int newWidth = this.columns*SIZE;//100 w:1000 
	int newHeight =(int)( (srcImg.getHeight()/(double)srcImg.getWidth())*newWidth);
	System.err.println(newWidth+"x"+newHeight);
	final BufferedImage lImage=new BufferedImage(
			newWidth,
			newHeight,
			BufferedImage.TYPE_BYTE_BINARY
			);
	final Graphics2D g= lImage.createGraphics();
	
	g.drawImage(srcImg.getScaledInstance(newWidth, newHeight,
			Image.SCALE_SMOOTH
			),0,0, null);
	//ImageIO.write(lImage, "PNG", new File("jeter2.png"));
	final BitSet bs = new BitSet(SIZE*SIZE);
	for(int y=0;y + SIZE <= lImage.getHeight();y+=SIZE)
		{
		for(int x=0;x + SIZE <= lImage.getWidth();x+=SIZE)
			{
			bs.clear();
			for(int dx=0;dx< SIZE;dx++)
				{
				for(int dy=0;dy < SIZE;dy++)
					{
					int ci = lImage.getRGB(x+dx, y+dy);
					final Color c=new Color(ci);
		        	bs.set(dy*SIZE+dx, c.getRed()==0);
					}
				}
			float currGray=grayOf(bs);
			
			final List<Tixel> bestGrays = new ArrayList<>();
			for(final Tixel tixel:this.alphabet)
				{
				float distgray =  Math.abs(currGray-tixel.percent);
				if(bestGrays.isEmpty() ||  Math.abs(currGray-bestGrays.get(0).percent) == distgray) {
					bestGrays.add(tixel);
					}
				else if( Math.abs(currGray-bestGrays.get(0).percent) > distgray) {
					bestGrays.clear();
					bestGrays.add(tixel);
					}
				}
			
			int bestScore=0;
			Tixel best=null;
			for(final Tixel tixel:this.alphabet) {
				int score = diff(tixel.bitSet,bs);
				
				if(best==null || bestScore<score) {
					best=tixel;
					bestScore=score;
					}
				}
			//best=bestGrays.get(0);
			out.print(best.c);
			}
		out.println();
		}
	}

@Override
public int doWork(final List<String> args) {
	try {
		initAlphabet();
		PrintStream out=System.out;
		if(args.isEmpty()) {
			out.println("> STDIN");
			display(out,ImageIO.read(System.in));
			}
		else
			{
			for(final String filename:args) {
				BufferedImage img;
				try {
					img = ImageIO.read(new File(filename));
					}
				catch(Exception err)
					{
					LOG.error(err);
					continue;
					}
				out.println("> "+filename);
				display(out,img);
				}
			}
		return 0;
		}
	catch(final Exception err) {
		LOG.error(err);
		return -1;
		}
	}

public static void main(final String[] args) {
	new ImageToAscii().instanceMainWithExit(args);
}
}
