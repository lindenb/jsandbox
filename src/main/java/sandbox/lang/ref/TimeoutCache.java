package sandbox.lang.ref;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public abstract class TimeoutCache<T> extends AbstractCache<T> {
	 private static final long CACHE_EXPIRATION_TIME = 60000; // 1 minute
	    private final Map<String, CacheEntry> imageCache = new LinkedHashMap<>(16, 0.75f, true); // LRU cache

	    public TimeoutCache() {
	    	this(CACHE_EXPIRATION_TIME);
	    	}
	    
	    public TimeoutCache(long timeout) {
	        Timer timer = new Timer(true);
	        timer.schedule(new CacheCleanupTask(), timeout, timeout);
	    }

	    @Override
	    public T get(String key) {
	        CacheEntry entry = imageCache.get(key);
	        if (entry != null) {
	            entry.lastAccessed = System.currentTimeMillis();
	            return entry.image;
	        }
	        // Load the image into the cache if not present
	        T image = fetch(key);// Load your image here
	        this.put(key, image);
	        return image;
	    }
	    
	    @Override
	    public void put(String key, T value) {
	    	 imageCache.put(key, new CacheEntry(value));
	    	}

	    private class CacheEntry {
	        private T image;
	        private long lastAccessed;

	        public CacheEntry(final T image) {
	            this.image = image;
	            this.lastAccessed = System.currentTimeMillis();
	        }

	       
	    }

	    private class CacheCleanupTask extends TimerTask {
	        @Override
	        public void run() {
	            long currentTime = System.currentTimeMillis();
	            for (Map.Entry<String, CacheEntry> entry : imageCache.entrySet()) {
	                if (currentTime - entry.getValue().lastAccessed > CACHE_EXPIRATION_TIME) {
	                    // Image not accessed for a long time, remove it from the cache
	                    imageCache.remove(entry.getKey());
	                }
	            }
	        }
	    }
}
