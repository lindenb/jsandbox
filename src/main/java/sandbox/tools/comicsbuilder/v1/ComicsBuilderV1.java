package sandbox.tools.comicsbuilder.v1;

import java.awt.Dimension;
import java.awt.geom.Dimension2D;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;

import sandbox.Launcher;
import sandbox.tools.central.ProgramDescriptor;


public class ComicsBuilderV1 extends Launcher {
	
	public class Page {
		Page() {
			}
		}
	
	public class Case {
		Case() {
			}
		}
	
	public class Context  {
		private final Document dom;
		Context() {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			try {
				DocumentBuilder db=dbf.newDocumentBuilder();
				this.dom = db.newDocument();
			} catch (ParserConfigurationException e) {
				throw new IllegalArgumentException(e);
				}
		 	}
		public Page page(final int w,final int h) {
			return new Page();
			}
		
		public Dimension2D dimension(final double w,final double h) {
			return new Dimension2D() {
				@Override
				public double getWidth() {
					return w;
					}
				@Override
				public double getHeight() {
					return h;
					}
				@Override
				public void setSize(Dimension2D d) {
					setSize(d.getWidth(),d.getHeight());
					}
				@Override
				public void setSize(double width, double height) {
					throw new UnsupportedOperationException();
					}
				};
		}
		public Rectangle2D rect(double x, double y,double w,double h) {
			return new Rectangle2D.Double(x,y,w,h);
		}
	}
	
	
	
	@Override
	public int doWork(List<String> args) {
		try {
			   // Here we are generating Nashorn JavaScript Engine 
	        ScriptEngine ee = new ScriptEngineManager().getEngineByName("Nashorn"); 
			if(ee==null) {
				System.err.println("nashorn not available");
				return -1;
				}
			
			final Context ctx= new Context();
			ee.put("ctx",ctx);
			ee.eval("java.lang.System.err.println(ctx.rect(10,20,100,200));");
			return 0;
			}
		catch(Throwable err) {
			err.printStackTrace();
			return -1;
			}
		
		}
	
	 public static ProgramDescriptor getProgramDescriptor() {
    	return new ProgramDescriptor() {
    		@Override
    		public String getName() {
    			return "comicsbuilder1";
    			}
    		};
    	}
	
	public static void main(String[] args) {
		new ComicsBuilderV1().instanceMainWithExit(args);
	}
}
