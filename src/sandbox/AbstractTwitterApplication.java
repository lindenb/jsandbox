package sandbox;



import org.scribe.builder.api.Api;
import org.scribe.builder.api.TwitterApi;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;



public  class AbstractTwitterApplication
	extends AbstractOAuthApplication
	{
	private static final String BASE_REST="https://api.twitter.com/1.1";

	protected static abstract class Entity
		{
		int begin;
		int end;
		protected Entity(JsonObject o)
			{
			JsonArray indices=o.getAsJsonArray("indices");
			this.begin=indices.get(0).getAsInt();
			this.end=indices.get(1).getAsInt();
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
		String screen_name;
		String name;
		String id_str;
		UserEntity(JsonObject o)
			{
			super(o);
			this.screen_name=o.get("screen_name").getAsString();
			this.name=o.get("name").getAsString();
			this.id_str=o.get("id_str").getAsString();
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
