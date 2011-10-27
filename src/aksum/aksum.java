/*
	aksum.java

	Title:			TestJava
	Author:			Pierre Lindenbaum
	Description:
*/

//package AksumPack;

import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import java.lang.*;
import java.util.*;

class Angle
	{
	//private members
		private int value=0;
		private int sens=1;
		private int pas=1,senspas=1;
		int minimal=0,maximal=360;
	//constructor
	public Angle()
		{
		value=(int)(360.0*Math.random());
		pas = 1;
		sens=1;
		setBound(0,720);
		}

	public void setBound(int m, int M)
		{
		minimal=m;
		maximal=M;
		value=minimal+(int)((double)(maximal-minimal)*Math.random());
		}
	public int getValue()
		{
		return(value%360);
		}
	public void compute()
		{
		int d=pas;
		//d= 1+(int)Math.abs((int)( (double)pas* Math.sin(  (double)(value-minimal) / ((double)(maximal-minimal))*2.0 -1.0) ) );



		value+=d*sens;
		if(value<minimal)
			{
			value=minimal;
			sens*=-1;
			}
		else if(value>maximal)
			{
			value=maximal;
			sens*=-1;
			}
		}
	}

class edge
	{
	//private members
		private int[] T=new int[3];

	//constructor
	public edge()
		{
		set(0,0,0);
		}
	public edge(double x,double y,double z)
		{
		set(x,y,z);
		}
	//X,Y,Z
	public int X() {return(T[0]);}
	public int Y() {return(T[1]);}
	public int Z() {return(T[2]);}
	public double distance()
		{
		return(Math.sqrt(Math.pow(X(),2)+Math.pow(Y(),2)+Math.pow(Z(),2)));
		}
	public void set(edge e)
		{
		int i;
		for(i=0;i<3;i++){T[i]=e.T[i];}
		}

	public void set(double x,double y,double z)
		{
		T[0]=(int)x;T[1]=(int)y;T[2]=(int)z;
		}

	public void scale(double d)
		{
		int i;
		if(d!=0)
			{
			for(i=0;i<3;i++)
				{
				T[i]=(int)((double)T[i]*d);
				}
			}
		}

	public void decal( int alpha, int beta, int gamma)
		{
		T[0]+=alpha;
		T[1]+=beta;
		T[2]+=gamma;
		}

	public void rotate(double[] lesCos,double[] lesSin, int alpha, int beta, int gamma)
		{
		double x=T[0],y=T[1],z=T[2],w;
		// x et y
		w=(x)*lesCos[alpha]-(y)*lesSin[alpha];
		y=(x)*lesSin[alpha]+(y)*lesCos[alpha];
		x=w;
		// x et z
		w=(x)*lesCos[beta]-(z)*lesSin[beta];
		z=(x)*lesSin[beta]+(z)*lesCos[beta];
		x=w;
		// y et z
		w=(y)*lesCos[gamma]-(z)*lesSin[gamma];
		y=(y)*lesSin[gamma]+(z)*lesCos[gamma];
		z=w;
		set(x,y,z);
		}
	}

class face
	{
	//private members
		private int[] indexSommet= new int[4];
		private edge[] sommets=null;
		int[] xpt= new int[5];
		int[] ypt= new int[5];
	//constructor
	public face(edge[] som,int A,int B,int C,int D)
		{
		sommets=som;
		indexSommet[0]=A;
		indexSommet[1]=B;
		indexSommet[2]=C;
		indexSommet[3]=D;
		}

	public void draw(Graphics g)
		{
		int i;
		for(i=0;i<4;i++)
			{
			xpt[i]=sommets[ indexSommet[i] ].X();
			ypt[i]=sommets[ indexSommet[i] ].Y();
			}
		xpt[4]=xpt[0];
		ypt[4]=ypt[0];

		g.fillPolygon(xpt,ypt,5);
		g.setColor(Color.black);
		g.drawPolygon(xpt,ypt,5);
		}

	public double Z()
		{
		double z;
		z=( sommets[ indexSommet[0] ].distance() + sommets[ indexSommet[2] ].distance()) /2.0;
		return(z);
		}

	public boolean isFarest(face cp)
		{
		return( Z() > cp.Z() ?true:false);
		}

	}


public class aksum extends Applet  implements Runnable
{
	Dimension		size;
	Image			buffer;
	Graphics		bufferGraphics;
	Thread			animator;
	boolean 		please_stop=false;
	edge[]			sommets=new edge[14];
	face[]			FaceDuCube=new face[6];
	Angle[]			anglesxyz=new Angle[3];
	int[]			orderOfApparition=new int[6];
	double 			angle=(Math.PI/2)*Math.random(),dangle=Math.PI/100;
	long 		when;

	int 			d;
	int			wait=0;

	double[] lesCos=new double [360],lesSin=new double[360];
	boolean isStandalone = false;

	public void init()
		{
		try {
			initComponents();


			when= System.currentTimeMillis();
			size=this.getSize();
			buffer=this.createImage(size.width,size.height);
			bufferGraphics= buffer.getGraphics();
			d=(int)((double)size.height/6.0);


			FaceDuCube[0]=new face(sommets,3,0,1,4);
			FaceDuCube[1]=new face(sommets,7,2,3,8);
			FaceDuCube[2]=new face(sommets,8,3,4,9);
			FaceDuCube[3]=new face(sommets,9,4,5,10);
			FaceDuCube[4]=new face(sommets,10,5,6,11);
			FaceDuCube[5]=new face(sommets,12,8,9,13);

			anglesxyz[0]=new Angle();
			anglesxyz[1]=new Angle();
			anglesxyz[2]=new Angle();


			anglesxyz[0].setBound(0, 712);
			anglesxyz[1].setBound(0, 712);
			anglesxyz[2].setBound(0, 712);

			for(int i=0;i<360;i++)
				{
				lesCos[i]=Math.cos( ((double)i/180.0)*Math.PI);
				lesSin[i]=Math.sin( ((double)i/180.0)*Math.PI);
				}
			}
		catch (Exception e)
			{
			e.printStackTrace();
			}

		}
	public void drawCube(Graphics g)
		{
		long  now;
		Color c;
		int i;
		double theCos,theSin,ouverture;


		for(i=0;i<3;i++)
			{
			anglesxyz[i].compute();
			}


		theCos=2*d*(Math.cos(angle));
		theSin=2*d*(Math.sin(angle));

		sommets[0]=new edge(-d,	d + theCos,	/*-d+*/theSin);
		sommets[1]=new edge(d,	d + theCos,	/*-d+*/theSin);
		sommets[2]=new edge(-d -theCos,	d,	/*-d+*/theSin);
		sommets[3]=new edge(-d,	d, /*-d*/0);
		sommets[4]=new edge(d,	d,	/*-d*/0);
		sommets[5]=new edge(d+ theCos,d,/*-d*/ +theSin);
		sommets[6]=new edge(d+ theCos -2*d*Math.cos(Math.PI-2*angle),d,/*-d+*/theSin+2*d*Math.sin(Math.PI-2*angle));
		sommets[7]=new edge(-d -theCos,	-d,	/*-d+*/theSin);
		sommets[8]=new edge(-d,-d,/*-d*/0);
		sommets[9]=new edge(d,-d,/*-d*/0);
		sommets[10]=new edge(d+ theCos,	-d,	/*-d+*/theSin);
		sommets[11]=new edge(d+ theCos - 2*d*Math.cos(Math.PI-2*angle),-d,/*-d+*/theSin+2*d*Math.sin(Math.PI-2*angle));
		sommets[12]=new edge(-d,	- d - theCos,	/*-d+*/theSin);
		sommets[13]=new edge(d,	- d - theCos,	/*-d+*/theSin);

		for(i=0;i<14;i++)
			{
			//sommets[i].scale(angle);
			sommets[i].rotate(lesCos, lesSin, anglesxyz[0].getValue() ,anglesxyz[1].getValue(), anglesxyz[2].getValue());
			sommets[i].decal(size.width/2,size.height/2,10*d);
			}

		//ordre d'apparition
		for(i=0;i<6;i++)
			{
			orderOfApparition[i]=i;
			}

		//sort
		boolean done=false;
		int k;
		while(done==false)
			{
			done=true;
			for(i=0;i<5;i++)
				{
				if(FaceDuCube[ orderOfApparition[i+1] ].isFarest(FaceDuCube[ orderOfApparition[i] ] ) == true)
					{
					k=orderOfApparition[i];
					orderOfApparition[i]=orderOfApparition[i+1];
					orderOfApparition[i+1]=k;

					done=false;
					}
				}
			}

		//draw
		bufferGraphics.setColor(Color.white);
		bufferGraphics.fillRect(0,0,size.width,size.height);
		bufferGraphics.setColor(Color.blue);
		bufferGraphics.drawString("lindenb.integragen.com",5,12);


		for(i=0;i<6;i++)
			{
			float f= (float)(0.5-0.3*(float)((5.0-(float) i )/5.0));
			Color bckgcolor = new Color(0,f,0);
			bufferGraphics.setColor(bckgcolor);
			FaceDuCube[ orderOfApparition[i] ].draw(bufferGraphics);
			}

		now=  System.currentTimeMillis();

		if(now - when > 0)
			{
			when=now;
			angle+=dangle;
			if(angle<-Math.PI/2)
				{
				angle=-Math.PI/2;
				dangle*=-1;
				}
			if(angle>Math.PI/2)
				{
				angle=Math.PI/2;
				dangle*=-1;
				}
			}
		g.drawImage(buffer,0,0,this);
		}

	public void paint(Graphics g)
		{

		drawCube(g);

		}

	public void update(Graphics g)
		{

		paint(g);

		}

	public void run()
		{
		while(true)
			{
			repaint(0,0,size.width,size.height);
			}
		}

	public void start()
		{

		if(animator==null)
			{
			please_stop=false;
			animator=new Thread(this);
			animator.start();
			}
		}

	public void stop()
		{
		please_stop=true;
		}



	// Retrieve the value of an applet parameter
	public String getParameter(String key, String def)
	{
		return isStandalone ? System.getProperty(key, def) :
			(getParameter(key) != null ? getParameter(key) : def);
	}

	// Get info on the applet parameters
	public String[][] getParameterInfo()
	{
		return null;
	}

	// Get applet information
	public String getAppletInfo()
	{
		return "Pierre Lindenbaum ©2000 lindenb@wanadoo.fr";
	}
	public void initComponents() throws Exception
	{
		// the following code sets the frame's initial state
		//setLayout(null);


	}

	// Main entry point when running standalone
	public static void main(String[] args)
	{

		aksum applet = new aksum();
		applet.isStandalone = true;
		Frame frame = new Frame();
		frame.setTitle("Aksum");
		frame.add( applet, BorderLayout.CENTER );

		applet.init();

		applet.start();
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation( (d.width - frame.getSize().width) / 2,
			(d.height - frame.getSize().height) / 2);
		frame.setVisible( true );
	}
}
