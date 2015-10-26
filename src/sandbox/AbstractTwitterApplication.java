package sandbox;



import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.scribe.builder.api.Api;
import org.scribe.builder.api.TwitterApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;


import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;



public  abstract class AbstractTwitterApplication
	extends AbstractOAuthApplication
	{
	private static final String BASE_REST="https://api.twitter.com/1.1";
	private int sleep_minutes=5;

	protected static interface Consumer<T>
		{
		void accept(final T o);
		}
	
	protected static abstract class Entity
		{
		JsonObject delegate;
		protected Entity(JsonObject o)
			{
			this.delegate= o;
			}
		public abstract String getType();
		}

	
	protected static class HashTagEntity
		extends Entity
		{
		String text;
		HashTagEntity(JsonObject o)
			{
			super(o);
			this.text=o.get("text").getAsString();
			}
		@Override
		public  String getType()
			{
			return "hashtag";
			}
		}
	
	protected static class UrlEntity
	extends Entity
		{
		String url;
		String expanded_url;
		String display_url;
		UrlEntity(JsonObject o)
			{
			super(o);
			this.url=o.get("url").getAsString();
			this.expanded_url=o.get("expanded_url").getAsString();
			this.display_url=o.get("display_url").getAsString();
			}
		@Override
		public  String getType()
			{
			return "url";
			}
		}

	
	
	protected static class UserEntity
	extends Entity
		{
		UserEntity(JsonObject o)
			{
			super(o);
			}
		public String getName()
			{
			return this.delegate.get("name").getAsString();
			}
		public String getScreenName()
			{
			return this.delegate.get("screen_name").getAsString();
			}
		public String getIdStr()
			{
			return this.delegate.get("id_str").getAsString();
			}
		public BigInteger getId()
			{
			return new BigInteger(getIdStr());
			}
		@Override
		public int hashCode()
			{
			return getId().hashCode();
			}
		@Override
		public boolean equals(Object obj)
			{
			if(this==obj) return true;
			if(obj==null || !(obj instanceof UserEntity)) return false;
			return UserEntity.class.cast(obj).getId().equals(this.getId());
			}
		
		@Override
		public  String getType()
			{
			return "user";
			}
		}

	protected static class MediaEntity
	extends Entity
		{
		String id_str;
		String type;
		String expanded_url;
		String source_status_id_str;
		MediaEntity(JsonObject o)
			{
			super(o);
			this.id_str=o.get("id_str").getAsString();
			this.type=o.get("type").getAsString();
			this.expanded_url=o.get("expanded_url").getAsString();
			if(o.get("source_status_id_str")!=null)
				{
				this.source_status_id_str=o.get("source_status_id_str").getAsString();
				}
			}
		@Override
		public  String getType()
			{
			return "media";
			}
		}

	
	protected AbstractTwitterApplication()
		{
		}
	
	
	@Override
	protected void fillOptions(final Options options)
		{
		options.addOption(Option.builder("wait").
				hasArg().
				required(false).
				longOpt("wait").
				argName("MINUTES").
				type(Integer.class).
				desc("wait MINUTES after twitter says quotas was reached default:"+sleep_minutes).
				build()
				);
		
		super.fillOptions(options);
		}
	
	@Override
	protected Status decodeOptions(CommandLine cmd)
		{
		if(cmd.hasOption("wait"))
			{
			this.sleep_minutes = Integer.parseInt(cmd.getOptionValue("wait"));
			}
		return super.decodeOptions(cmd);
		}

	
	protected void listFollowers(
			String user_id_or_screen_name,
			Consumer<BigInteger> callback
			)
			throws Exception
			{	
			listFriendOrFollowers("followers",user_id_or_screen_name,callback);
			}
	
	protected List<UserEntity> usersLookup(final Set<BigInteger> ids) throws Exception
		{		
		 List<UserEntity> ret = new ArrayList<>();
		 JsonParser parser=new JsonParser();
		String url=getBaseURL()+"/users/lookup.json";
		LOG.info(url);
		ArrayList<BigInteger> copy=new ArrayList<>(ids);
		while(!copy.isEmpty())
			{
			StringBuilder user_id_str=new StringBuilder();
			for(int i=0;!copy.isEmpty() && i<100;++i)
				{
				if(user_id_str.length()!=0) user_id_str.append(",");
				user_id_str.append(copy.remove(0));
				}
			
			OAuthRequest request = new OAuthRequest(Verb.GET, url);
		    request.addQuerystringParameter("user_id",user_id_str.toString());
		    getService().signRequest(getAccessToken(), request);
	
		    Response response = request.send();
		    Reader  in=new InputStreamReader(new LogInputStream(response.getStream(),
		    		!Level.OFF.equals(LOG.getLevel())?System.err:null));
		    JsonElement jsonResponse=parser.parse(in);
		    in.close();
	
		    JsonArray array=jsonResponse.getAsJsonArray();
		   
			for(int i=0;i< array.size();++i)
				{
				ret.add(new UserEntity(array.get(i).getAsJsonObject()));
				}
			}
		return ret;
		}
	
	private void listFriendOrFollowers(
			final String verb,
			String user_id_or_screen_name,
			Consumer<BigInteger> callback
			)
			throws Exception
			{	
			BigInteger userId=null;
			String screen_name=null;
			try{
				userId=new BigInteger(user_id_or_screen_name);
				if(userId.compareTo(BigInteger.ONE)<0)
					{
					userId=null;
					throw  new NumberFormatException("negative id "+user_id_or_screen_name);
					}
				}
			catch (NumberFormatException err)
				{
				screen_name=String.valueOf(user_id_or_screen_name);
				}
			
			JsonParser parser=new JsonParser();
			BigInteger cursor=BigInteger.ONE.negate();
			String url=getBaseURL()+"/"+verb+"/ids.json";
			LOG.info(url);
			for(;;)
				{
			    JsonElement jsonResponse=null;
			    JsonArray array=null;
			    for(;;)
			    	{
			    	try
			    		{
						OAuthRequest request = new OAuthRequest(Verb.GET, url);
					    if(userId!=null) request.addQuerystringParameter("user_id", userId.toString());
					    else if(screen_name!=null) request.addQuerystringParameter("screen_name", screen_name);
					    else throw new IllegalArgumentException("user id/screen_name ?");
					    request.addQuerystringParameter("cursor", cursor.toString());
					    getService().signRequest(getAccessToken(), request);

					    Response response = request.send();
					    Reader  in=new InputStreamReader(new LogInputStream(response.getStream(),
					    		!Level.OFF.equals(LOG.getLevel())?System.err:null));
					    jsonResponse=parser.parse(in);
					    in.close();

					    array=jsonResponse.getAsJsonObject().get("ids").getAsJsonArray();
						break;
			    		}
			    	catch (Exception e)
			    		{
						LOG.info("Error: "+e.getMessage()+" sleep for "+sleep_minutes+" minutes.");
						Thread.sleep(sleep_minutes*1000);//5minutes
						}
			    	}
			    
			    for(int i=0;callback!=null && i< array.size();++i)
			    	{
			    	BigInteger friendid=array.get(i).getAsBigInteger();
			    	callback.accept(friendid);
			    	}
			    
			    cursor=null;
			    if(jsonResponse.getAsJsonObject().get("next_cursor")!=null)
			    	{
			    	cursor=jsonResponse.getAsJsonObject().get("next_cursor").getAsBigInteger();
			    	}
			    if(cursor==null || cursor.equals(BigInteger.ZERO)) break;
				}
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
