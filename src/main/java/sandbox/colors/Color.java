package sandbox.colors;

import sandbox.colors.parser.ColorParser;

public interface Color {
	public int getRed();
	public int getGreen();
	public int getBlue();
	public int getAlpha();
	
	public default java.awt.Color toAWT() {
		return new java.awt.Color(getRed(),getGreen(),getBlue(),getAlpha());
		}
	public static Color ofAWT(final  java.awt.Color c) {
		return create(c.getRed(),c.getGreen(),c.getBlue(),c.getAlpha());
		}
    public static Color of( int rgb) {
		return ofAWT(new java.awt.Color(rgb));
		}

	public static Color parse(final String s) {
		return ColorParser.parse(s);
		}
	public static Color gray(int g) {
		return create(g,g,g);
		}
	public static Color gray(float f) {
		return gray(f2i(f));
		}
	public static Color create(float r,float g,float b) {
		return create(r,g,b,1f);
		}

	
	public static Color create(float r,float g,float b,float a) {
		return create(f2i(r),f2i(g),f2i(b),f2i(a));
		}
	public static Color create(int r,int g,int b) {
		return create(r,g,b,255);
		}
	public static Color create(int r,int g,int b,int a) {
		return new ColorImpl(r,g,b,a);
		}
	
	static int f2i(float v) {
		if(v< 0f || v>1f) throw new IllegalArgumentException("expected color component 0<"+v+"<1");
		return (int)(v*255f);
		}
	
	static class ColorImpl implements Color {
		final int r;
		final int g;
		final int b;
		final int a;
		ColorImpl(int r,int g,int b) {
			this(r,g,b,255);
			}
		ColorImpl(int r,int g,int b,int a) {
			this.r = r;
			this.g = g;
			this.b = b;
			this.a = a;
			}
		@Override
		public int hashCode() {
			int n= Integer.hashCode(r);
			n = n*31 +  Integer.hashCode(g);
			n = n*31 +  Integer.hashCode(b);
			n = n*31 +  Integer.hashCode(a);
			return n;
			}
		 @Override
                public boolean equals(final Object o) {
			if(o==this) return true;
			if(o==null || !(o instanceof Color)) return false;
			final Color c = Color.class.cast(o);
                   	return	this.getRed() == c.getRed() &&
				this.getGreen() == c.getGreen() &&
				this.getRed() == c.getRed() &&
				this.getAlpha() == c.getAlpha()
				;
                        }

		@Override
		public int getRed() {return r;}
		@Override
		public int getGreen() {return g;}
		@Override
		public int getBlue() {return b;}
		@Override
		public int getAlpha() {return a;}
		@Override
		public String toString() {
			return NamedColors.getInstance().findColorName(this).orElse("rgba("+r+","+g+","+b+","+a+")");
			}
		}
	}
