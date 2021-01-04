package sandbox.flickr;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;


public class FlickrExtractor implements Function<JsonElement,List<FlickrExtractor.Image>>{
public interface Image {
	public String getId();
	public String getHeight();
	public String getWidth();
	public String getLicense();
	public String getTitle();
	public String getPageUrl();
	public String getImageSrc();
	}
private static class ImageImpl implements Image {
	String id;
	String ownerNsid;
	String title;
	@SuppressWarnings("unused")
	String username;
	String displayUrl;
	String license;	
	String width;
	String height;	
				
	@Override
	public int hashCode() { return id.hashCode();}
	
	@Override
	public boolean equals(final Object o) {
		if(o==this) return true;
		return this.id.equals(ImageImpl.class.cast(o).id);
		}
	
	@Override public String getLicense() { return this.license;}
	@Override public String getTitle() { return this.title;}

	@Override
	public String getId() {
		return id;
		}
	
	@Override
	public String getHeight() {
		return height;
		}
	@Override
	public String getWidth() {
		return width;
		}
	@Override
	public String getImageSrc() {
		return "https:"+displayUrl;
		}
	@Override
	public String getPageUrl() {
		return "https://www.flickr.com/photos/"+ this.ownerNsid +"/"+ this.id +"/";
		}
	}

@Override
public List<Image> apply(final JsonElement root) {
	final Map<String,Image> id2image = new LinkedHashMap<>();
	JsonElement data =  search(null,root,"_data");
	if(data!=null && data.isJsonArray()) {
		final JsonArray array  = data.getAsJsonArray();
		for(int i=0;i< array.size();i++) {
			final Image img = parseImage(array.get(i).getAsJsonObject());
			if(img!=null) id2image.put(img.getId(), img);
			}
		}
	return new ArrayList<>(id2image.values());
	}

private Image parseImage(final JsonObject elt) {
	final ImageImpl entry = new ImageImpl();
	entry.id = elt.getAsJsonPrimitive("id").getAsString();
	JsonElement e = search(null,elt,"ownerNsid");
	if(e==null) return null;
	entry.ownerNsid = e.getAsString();
	if(elt.get("title")!=null) {
		entry.title = elt.getAsJsonPrimitive("title").getAsString();
		}
	else
		{
		entry.title="";
		}
	entry.license = elt.getAsJsonPrimitive("license").getAsString();
	e = search(null,elt,"username");
	if(e!=null) {
		entry.username = elt.getAsJsonPrimitive("username").getAsString();
	}
	JsonObject sizes = elt.getAsJsonObject("sizes");
	JsonObject sq = sizes.getAsJsonObject("t");
	if(sq==null) sq = sizes.getAsJsonObject("sq");
	entry.displayUrl = sq.getAsJsonPrimitive("displayUrl").getAsString();
	entry.width = sq.getAsJsonPrimitive("width").getAsString();
	entry.height = sq.getAsJsonPrimitive("height").getAsString();
	return entry;
}


private JsonElement search(final String name,final JsonElement node,final String searchKey) {
	if(name!=null && name.equals(searchKey)) return node;
	if(node.isJsonObject()) {
		final JsonObject object = node.getAsJsonObject();
		for(Map.Entry<String,JsonElement> kv: object.entrySet()) {
			JsonElement r = search(kv.getKey(),kv.getValue(),searchKey);
			if(r!=null) return r;
			}
		}
	else if(node.isJsonArray()) {
		final JsonArray array  = node.getAsJsonArray();
		for(int i=0;i< array.size();i++) {
			JsonElement r = search(null,array.get(i),searchKey);
			if(r!=null) return r;
			}
		}
	return null;
	}


}
