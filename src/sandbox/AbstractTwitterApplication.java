package sandbox;

import org.scribe.builder.api.Api;
import org.scribe.builder.api.TwitterApi;


public  class AbstractTwitterApplication
	extends AbstractOAuthApplication
	{
	private static final String BASE_REST="https://api.twitter.com/1.1";

	
	protected AbstractTwitterApplication()
		{
		}
	
	protected void usage()
		{
		super.usage();

		return;
		}
	
	public String getBaseURL()
		{
		return  BASE_REST;
		}
	

	@Override
	protected String getPreferencesPrefix()
		{
		return "twitter";
		}

	@Override
	protected Class<? extends Api> getApiClass()
		{
		return TwitterApi.class;
		}
	
	
	    
	}
