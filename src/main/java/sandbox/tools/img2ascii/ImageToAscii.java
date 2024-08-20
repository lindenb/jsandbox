package sandbox.tools.img2ascii;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import javax.imageio.ImageIO;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.io.IOUtils;

public class ImageToAscii extends Launcher {
	private static final Logger LOG = Logger.builder(ImageToAscii.class).build();
	private final static int SIZE=32;
	@Parameter(names={"-c","--cols","--columns"},description="Columns")
	private int columns=80;
	@Parameter(names={"-D","--directory"},description="Debugging directory")
	private File debugDirectory = null;
	@Parameter(names={"-o","--output"},description="ouput filename")
	private Path outFile=null;

	
private class Tixel {
	char c;
	BitSet bitSet;
	//float percent;
	Tixel(char c) {
		this.c=c;
		this.bitSet = new BitSet(SIZE*SIZE);
		}
	}
private List<Tixel> alphabet = new ArrayList<>();
private void initAlphabet() {
	
	
	final BufferedImage img = new BufferedImage(
			SIZE,SIZE,
			BufferedImage.TYPE_BYTE_BINARY
			);
	final Font main_font = new Font("Courier",Font.BOLD,SIZE);

	final String letters=" !\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvwxyz{|}~" +
				"\u2581\u2582\u2583\u2584\u2585\u2586\u2587\u2588";
	double[] dArray=null;

	for(int i=0;i < letters.length();++i) {
		final Tixel tixel=new Tixel(letters.charAt(i));
		final Graphics2D g=img.createGraphics();
		g.setFont(main_font);
		//do NOT use TextLayout.getOutline to resize the char: a simple dot would fill all the img
		final String letter = letters.substring(i,i+1);
		AffineTransform oldtr=g.getTransform();
		AffineTransform tr= AffineTransform.getTranslateInstance(2, -15);
		tr.scale(1.4, 1.4);
		//tr.translate(-5, -5);
		
		g.setColor(Color.WHITE);
		g.fillRect(0, 0, SIZE, SIZE);
		g.setColor(Color.BLACK);
		
		g.setTransform(tr);
		g.drawString(letter, 0, SIZE);
		g.setTransform(oldtr);
		g.dispose();
		final WritableRaster raster=img.getRaster();
		for (int y = 0; y < SIZE; y++) {
	         for (int x = 0; x < SIZE; x++) {
	        	dArray = raster.getPixel(x,y, dArray);
				boolean isblack =  dArray[0]==0.0;
		        tixel.bitSet.set(y*SIZE+x,isblack);
	        	 //System.err.print(isblack?"#":" ");
	         }
	         //System.err.println();
	     }
		//tixel.percent = grayOf(tixel.bitSet);
		this.alphabet.add(tixel);
		
		if(debugDirectory!=null) {
			sandbox.io.IOUtils.assertDirectoryExist(this.debugDirectory);
			final File pngOut=new File(this.debugDirectory, String.format("debug.%d."+SIZE+".png", (int)letter.charAt(0)));
			try {
				LOG.info("save "+pngOut);
				ImageIO.write(img, "PNG", pngOut);
			} catch(IOException err) {
				LOG.error(err);
			}
		}
		
		}
	}
/*
private float grayOf(final BitSet bs1) {
	float n=0;
	for(int x=0;x< SIZE*SIZE;++x) {
		n+=bs1.get(x)?1f:0f;
		}
	return n/(SIZE*SIZE);
	}*/
private int diff(final BitSet bs1,final BitSet bs2)
	{
	int n=0;
	for(int x=0;x< SIZE*SIZE;++x) {
		boolean b1 = bs1.get(x);
		boolean b2 = bs2.get(x);
		n+=(b1!=b2?0:1);
		}
	return n;
	}
private void display(final BufferedWriter out,final BufferedImage srcImg) throws IOException {
	
	int newWidth = this.columns*SIZE;//100 w:1000 
	int newHeight =(int)( (srcImg.getHeight()/(double)srcImg.getWidth())*newWidth);
	//System.err.println(newWidth+"x"+newHeight);
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
	double[] dArray=null;
	final WritableRaster raster=lImage.getRaster();

	for(int y=0;y + SIZE <= lImage.getHeight();y+=SIZE)
		{
		for(int x=0;x + SIZE <= lImage.getWidth();x+=SIZE)
			{
			bs.clear();
			for(int dx=0;dx< SIZE;dx++)
				{
				for(int dy=0;dy < SIZE;dy++)
					{
					dArray = raster.getPixel(x+dx, y+dy, dArray);
					boolean flag =  dArray[0]==0.0;
		        	bs.set(dy*SIZE+dx, flag);
					}
				}
			/*
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
				}*/
			
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
			out.write(best.c);
			}
		out.newLine();
		}
	out.flush();
	}

@Override
public int doWork(final List<String> args) {
	try {
		initAlphabet();
		try(BufferedWriter out= new BufferedWriter(IOUtils.openPathAsWriter(outFile))) {
			if(args.isEmpty()) {
				out.write("> STDIN");
				out.newLine();
				display(out,ImageIO.read(System.in));
				}
			else
				{
				for(final String filename:args) {
					BufferedImage img;
					try {
						img = ImageIO.read(new File(filename));
						}
					catch(Exception err) {
						LOG.error(err);
						continue;
						}
					out.write("> "+filename);
					out.newLine();
					display(out,img);
					}
				}
			out.flush();
			}
		return 0;
		}
	catch(final Throwable err) {
		LOG.error(err);
		return -1;
		}
	}

public static void main(final String[] args) {
	new ImageToAscii().instanceMainWithExit(args);
}
}
