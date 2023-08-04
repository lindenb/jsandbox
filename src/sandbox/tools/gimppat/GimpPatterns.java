package sandbox.tools.gimppat;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.DoubleSupplier;

import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;

public class GimpPatterns extends Launcher {
	private static final Logger LOG = Logger.builder(GimpPatterns.class).build();
	@Parameter(names= {"-o"},description=OUTPUT_OR_STANDOUT)
	private Path output;
	@Parameter(names= {"-D"},description="base dir",required = true)
	private Path baseDir;
	@Parameter(names= {"-p"},description="prefix")
	private String prefix = "generated.";
	
	private static int ID_GENERATOR=0;
	
	private abstract class Image {
		final int id = ++ID_GENERATOR;
		abstract String getTitle();
		abstract int getWidth();
		abstract int getHeight();
		abstract int getPixel(int x,int y);
		
		@Override
		public int hashCode() {
			int h = Integer.hashCode(getWidth());
			h += 31* Integer.hashCode(getHeight());
			for(int x=0;x<getWidth();++x) {
				for(int y=0;y<getHeight();++y) {
					h += 31* Integer.hashCode(getPixel(x, y));
					}
				}
			return h;
			}
		@Override
		public boolean equals(Object obj) {
			if(obj==this) return true;
			if(obj==null || !(obj instanceof Image)) return false;
			Image o = Image.class.cast(obj);
			if(getWidth()!=o.getWidth()) return false;
			if(getHeight()!=o.getHeight()) return false;
			for(int y=0;y< getHeight();y++) {
				for(int x=0;x< getWidth();x++) {
					if(getPixel(x, y)!=o.getPixel(x, y)) return false;
					}
				}
			return true;
			}
		
		int getMaxPixelValue() {
			int maxV=1;
			for(int y=0;y< getHeight();y++) {
				for(int x=0;x< getWidth();x++) {
					maxV = Math.max(maxV, getPixel(x,y));
					}
				}
			return maxV;
			}
		
		 void writePNM(PrintWriter w){
			final int maxV=getMaxPixelValue();
			w.println("P"+(maxV>1?2:1));
			w.println("# This is a comment");
			w.println(""+getWidth()+" "+getHeight());
			if(maxV>1) {
				w.println(maxV);
				}
			
			for(int y=0;y< this.getHeight();++y) {
				for(int x=0;x< this.getWidth();++x) {
					if(x>0) w.print(" ");
					w.print(getPixel(x,y));
					}
				w.println();
				}
			}

		}
	
	private abstract class DelegateImage extends Image {
		final Image base;
		DelegateImage(Image base) {
			this.base=base;
			}
		@Override
		int getHeight() {
			return base.getHeight();
			}
		@Override
		int getWidth() {
			return base.getWidth();
			}
		@Override
		int getPixel(int x, int y) {
			return base.getPixel(x,y);
			}
		@Override
		String getTitle() {
			return base.getTitle();
			}
		}
	
	private class MirrorHImage extends DelegateImage {
		MirrorHImage(Image base) {
			super(base);
			}
		@Override
		int getPixel(int x, int y) {
			return base.getPixel((getWidth()-1)-x,y);
			}
		@Override
		String getTitle() {
			return base.getTitle()+".H";
			}
		}
	private class MirrorVImage extends DelegateImage {
		MirrorVImage(Image base) {
			super(base);
			}
		@Override
		int getPixel(int x, int y) {
			return base.getPixel(x,(getHeight()-1)-y);
			}
		@Override
		String getTitle() {
			return base.getTitle()+".H";
			}
		}
	
	private class InvertImage extends DelegateImage {
		final int maxV;
		InvertImage(Image base) {
			super(base);
			this.maxV= base.getMaxPixelValue();
			}
		@Override
		int getPixel(int x, int y) {
			return this.maxV - base.getPixel(x,y);
			}
		@Override
		String getTitle() {
			return base.getTitle()+"inv";
			}
		} 
	
	private class ScaledImage extends DelegateImage {
		final int scale;
		ScaledImage(Image base,int scale) {
			super(base);
			this.scale = scale;
			}
		@Override
		int getHeight() {
			return base.getHeight()*scale;
			}
		@Override
		int getWidth() {
			return base.getWidth()*scale;
			}
		@Override
		int getPixel(int x, int y) {
			return base.getPixel(x/scale,y/scale);
			}
		@Override
		String getTitle() {
			return base.getTitle()+"x"+scale;
			}
		} 
	private class BitMapImage extends Image {
		final int width;
		final int height;
		final int[] pixels;
		
		BitMapImage(int w, int h) {
			this.width=w;
			this.height=h;
			this.pixels = new int[w*h];
			}
		
		BitMapImage(int w,int h,int...array) {
			this(w,h);
			if(array.length!=(width*height)) throw new IllegalArgumentException("bad array size");
			System.arraycopy(array, 0, this.pixels, 0, array.length);
			}
		@Override
		public String getTitle() {
			return String.valueOf(id);
			}
		@Override
		public int getWidth() {
			return width;
			}
		@Override
		public int getHeight() {
			return height;
			}

		@Override
		public int getPixel(int x, int y) {
			return this.pixels[y*getWidth()+x];
			}
		
		void fillRect(int x,int y,int w,int h) {
			for(int i=0;i<w;i++) {
				for(int j=0;j<h;j++) {
					int x2 = (x+i)%getWidth();
					int y2 = (y+j)%getHeight();
					this.pixels[y2*getWidth()+x2] = 1;
					}
				}
			}
		void fillOval(int cx,int cy,int r) {
			int x= cx-r;
			int y= cy-r;
			int w = r*2;
			int h = r*2;
			for(int i=0;i<w;i++) {
				for(int j=0;j<h;j++) {
					if(Math.sqrt(i*i+j*j)>r) continue;
					int x2 = (x+i)%getWidth();
					int y2 = (y+j)%getHeight();
					this.pixels[y2*getWidth()+x2] = 1;
					}
				}
			}
		}
	
	private String quote(String s) {
		return "\""+s+"\"";
	}
	
	
	
	private void save(PrintWriter w, Image img) {
		String filein = "jeter.pnm";
		String fileout = prefix+String.valueOf(img.id)+".pat";
		w.println("cat << __EOF__ > " +filein );
		img.writePNM(w);
		w.println("__EOF__");
		w.println(
		"gimp -i -b '(let* (" +
	    "   (image (car (gimp-file-load RUN-NONINTERACTIVE "+ quote(filein) + " " + quote(filein) +" )))" +
	    "   (drawable (car (gimp-image-get-active-layer image))) "+
	    "    ) "+
	    "(file-pat-save RUN-NONINTERACTIVE image drawable  " + quote(fileout)+ "  "+ quote(fileout) +" " + quote("")+ " )" +
	    "(gimp-image-delete image) "+
	    "(gimp-patterns-refresh) " +
	    ")'  -b '(gimp-quit 0)'"
	    );
	}
	
	@Override
	public int doWork(final List<String> args) {
		
		try(PrintWriter w= new PrintWriter(super.openPathAsOuputStream(this.output))) {
			w.println("#!/bin/bash");
			w.println("set -e");
			w.println("set -u");
			w.println("set -x");
			final Set<Image> images = new HashSet<>();
			
			Image img = new BitMapImage(2,2, 0,1,1,0);
			for(int scale=1;scale < 5;++scale) {
				Image img2 = new ScaledImage(img, scale);
				images.add(img2);
				images.add(new InvertImage(img2) );
				images.add(new MirrorHImage(img2) );
				images.add(new MirrorVImage(img2) );
				}
			for(Image i:images) {
				save(w,i);
				}
			
			w.println("rm -f jeter.pnm");
			w.flush();
			}
		catch(IOException err ) {
			LOG.error(err);
			return -1;
			}
		return 0;
		}
	
	public static void main(final String[] args) {
		new GimpPatterns().instanceMainWithExit(args);
	}
}
