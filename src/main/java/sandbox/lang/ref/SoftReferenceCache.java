package sandbox.lang.ref;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class SoftReferenceCache<T>  extends AbstractCache<T> {
	private static final Logger LOG = Logger.getLogger(SoftReferenceCache.class.getName());
    private final Map<String, SoftReference<T>> cache = new HashMap<>();
    private final ReferenceQueue<T> referenceQueue= new ReferenceQueue<>();

    @Override
    public T get(final String key) {
        // Vérifier si une référence douce est présente dans le cache
        final SoftReference<T> softRef = cache.get(key);

        if (softRef != null) {
            // Récupérer l'image de la référence douce
            final T image = softRef.get();
            if (image != null) {
                // L'image est toujours en mémoire, la renvoyer
                return image;
            }
        }
        
        cleanUpCache();
        // Charger l'image
       final T image = fetch(key);// Charger votre image ici

        // Mettre en cache l'image avec une SoftReference
       put(key, image);

        return image;
    }

    public void put(final String key,final T value) {
        final SoftReference<T> newSoftRef = new SoftReference<>(value, referenceQueue);
        this.cache.put(key, newSoftRef);
    	}
    
    // Méthode pour nettoyer le cache en fonction des références douces libérées
    public void cleanUpCache() {
        Reference<? extends T> ref;
        while ((ref = referenceQueue.poll()) != null) {
            // La référence a été libérée par le garbage collector
           LOG.log(Level.INFO,"SoftReference released: " + ref);
            // Effectuer d'autres actions si nécessaire, par exemple, retirer l'entrée du cache
            removeFromCache(ref);
        }
    }

    private void removeFromCache(final Reference<? extends T> ref) {
        for (Map.Entry<String, SoftReference<T>> entry : cache.entrySet()) {
            if (entry.getValue() == ref) {
                cache.remove(entry.getKey());
                break;
            }
        }
    }
}
