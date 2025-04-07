package sandbox.oauth;

import java.awt.Desktop;
import java.io.Console;
import java.util.Scanner;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.JOptionPane;


import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.Api;
import org.scribe.model.Verifier;
import org.scribe.oauth.OAuthService;
import com.beust.jcommander.Parameter;

import sandbox.Launcher;
import sandbox.Logger;
import sandbox.lang.StringUtils;

/**
 *
 *  AbstractOAuthApplication
 *
 */
public abstract class AbstractOAuthApplication
	extends Launcher
	{
	private static final Logger LOG=Logger.builder(AbstractOAuthApplication.class).build();
	private OAuthService service;
	private org.sandbox.minilang.minilang2.Token accessToken;
	private Preferences preferences;
	private boolean force_manual_access=false;
	private boolean force_ignore_prefs=false;
	

	
	
	protected AbstractOAuthApplication()
		{
		}
	
	/** prefix used to define keys when looking in the user preferences */
	protected abstract String getPreferencesPrefix();
		
	
	protected OAuthService getService()
		{
		return service;
		}
	
	protected org.sandbox.minilang.minilang2.Token getAccessToken()
		{
		return accessToken;
		}
	
	@Parameter(names={"-secret","--secret"},description="api secret (or will use the one stored in the preferences)")
	private String api_secret=null;
	
	@Parameter(names={"-key","--key"},description="api key (or will use the one stored in the preferences)")
	private String api_key=null;


	

	protected boolean isIgnoringPrefs()
		{
		return force_ignore_prefs;
		}
	
	protected Preferences getPreferences()
		{
		if(this.preferences==null)
			{
			this.preferences=Preferences.userNodeForPackage(getClass());
			}
		return this.preferences;
		}
	
	protected String prefApiKey()
		{
		return getPreferencesPrefix()+".api.key";
		}
	protected String prefApiSecret()
		{
		return getPreferencesPrefix()+".api.secret";
		}

	protected String prefAccessToken()
		{
		return getPreferencesPrefix()+".access.token";
		}

	protected String prefAccessSecret()
		{
		return getPreferencesPrefix()+".access.secret";
		}
	protected String prefAccessRaw()
		{
		return getPreferencesPrefix()+".access.raw";
		}
	
	protected abstract Class<? extends Api> getApiClass();
	
	
	protected String askToken(String www)
		{
		System.out.println("Authorize the application here:\n\t"+ www +"\n");
    	if( Desktop.isDesktopSupported())
    		{
    		java.awt.Desktop desktop=java.awt.Desktop.getDesktop();
    		if(desktop.isSupported( java.awt.Desktop.Action.BROWSE ))
    			{
    			try
	    			{
	                java.net.URI uri = new java.net.URI( www );
	                System.err.println(uri);
	                desktop.browse( uri );
	    			}
    			catch(Exception err)
    				{
    				LOG.info("Desktop.error:"+err.getMessage());
    				}
    			}
    		else
    			{
    			LOG.info("Desktop is not supported");
    			}
    		}
    	
    	try
    		{
    		return JOptionPane.showInputDialog(null, www);
    		}
    	catch(final Exception err)
    		{
    		LOG.info("GUI error: "+err);
    		}
    	Console console=System.console();
    	String token=null;
    	if( console!=null )
    		{
    		char pass[]=console.readPassword("Enter access token: ");
    		token=new String(pass);
    		}
    	else
    		{
	    	try(Scanner in = new Scanner(System.in)) {
		    	token=in.nextLine();
		    	}
    		}
    	if(StringUtils.isBlank(token))
    		{
    		LOG.info("no token");
    		return null;
    		}
    	return token;
		}
	
	protected void connect()
		{
		if(api_key==null)
			{
			api_key=(isIgnoringPrefs()?null:getPreferences().get(prefApiKey(),null));
			if(api_key==null)
				{
				System.err.println("Undefined api_key");
				System.exit(-1);
				}
			}
		if(api_secret==null)
			{
			api_secret=(force_ignore_prefs?null:getPreferences().get(prefApiSecret(),null));
			if(api_secret==null)
				{
				System.err.println("Undefined api_secret");
				System.exit(-1);
				}
			}
		LOG.info("api_key:"+api_key);
		LOG.info("api_secret:"+api_secret);
		 this.service = new ServiceBuilder()
	        .provider(getApiClass())
	        .apiKey(api_key)
	        .apiSecret(api_secret)
	        .build();
	        
	        
	     sandbox.minilang.minilang2.scribe.model.Token requestToken=null;
	     LOG.info("getRequestToken....");
		requestToken  =  this.service.getRequestToken();
		LOG.info("got request token");
		 
		 this.accessToken=null;
		 if(!(this.force_manual_access || force_ignore_prefs))
			 {
			 String access_token=getPreferences().get(prefAccessToken(), null);
			 String access_secret=getPreferences().get(prefAccessSecret(), null);
			 String access_raw=getPreferences().get(prefAccessRaw(), null);
	    	 if(access_token!=null && access_secret!=null)
	    		{
	    		this.accessToken=new sandbox.minilang.minilang2.scribe.model.Token(
	    				access_token,
	    				access_secret,
	    				access_raw
	    				);
	    		}
		     }
		     
		    
	    if(this.accessToken==null || accessToken.isEmpty())
	    	{
	    	String www=this.service.getAuthorizationUrl(requestToken);
	    	LOG.info("confirm url:"+www);
	    	String token=askToken(www);
	    	if(token==null)
	    		{
	    		System.err.println("Error");
	    		System.exit(-1);
	    		}
	    	Verifier verifier = new Verifier(token.trim());
	    	
	    	this.accessToken = service.getAccessToken(requestToken, verifier);
	    	LOG.info("got AccessToken");
	    	}
		}
	
	protected void savePreferences()
		throws BackingStoreException
		{
		getPreferences().put(prefApiKey(), api_key);
		getPreferences().put(prefApiSecret(), api_secret);
		getPreferences().put(prefAccessToken(), getAccessToken().getToken());
		getPreferences().put(prefAccessSecret(),getAccessToken().getSecret());
		if(getAccessToken().getRawResponse()!=null)
			{
			getPreferences().put(prefAccessRaw(), getAccessToken().getRawResponse());
			}
		getPreferences().sync();
		}
	}
