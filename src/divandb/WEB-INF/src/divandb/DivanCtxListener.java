package divandb;

import java.io.File;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletException;

import org.lindenb.io.IOUtils;

public class DivanCtxListener
	implements ServletContextListener
	{
	@Override
	public void contextInitialized(ServletContextEvent ctx)
		{
		
		try
			{
			DivanDB db=new DivanDB();
			File dir=IOUtils.createTempDir();
			db.open(dir);
			ctx.getServletContext().setAttribute("divan.database",db);
			}
		catch (Exception e)
			{
			throw new RuntimeException(e);
			}
		}
	
	@Override
	public void contextDestroyed(ServletContextEvent ctx)
		{
		DivanDB db=(DivanDB)ctx.getServletContext().getAttribute("divan.database");
		if(db==null) return;
		ctx.getServletContext().removeAttribute("divan.database");
		db.close();
		}

	
	}
