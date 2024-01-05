package sandbox.swing.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;

import javax.swing.event.EventListenerList;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

public class ObservableList<T> extends AbstractList<T> {
 		protected final EventListenerList listeners = new EventListenerList();
 		private final List<T> delegate;
 	 
 		public ObservableList() {
			this(new ArrayList<>());
	   		}
 		public ObservableList(final List<T> wrap) {
			this.delegate = wrap;
	   		}
		
		@Override
		public T get(int index) {
			return this.delegate.get(index);
			}
		
		@Override
		public int size() {
			return this.delegate.size();
			}
		
		@Override
		public boolean add(final T e) {
			add(size(),e);
			return true;
			}
		@Override
		public void add(int index, T element) {
			this.delegate.add(index,element);
			fireIntervalAdded(index,index+1);
			}
		@Override
		public T remove(int index) {
			final T old= this.delegate.remove(index);
			fireIntervalRemoved(index, index+1);
			return old;
			}
		
		@Override
		public T set(int index, T element) {
			T old= this.delegate.set(index, element);
			fireContentsChanged(index, index+1);
			return old;
			}
		
 	    public void addListDataListener(ListDataListener l) {
 	    	listeners.add(ListDataListener.class, l);
 	    }

 	    public void removeListDataListener(ListDataListener l) {
 	    	listeners.remove(ListDataListener.class, l);
 	    }
		public void fireContentsChanged(int index0, int index1) {
			ListDataEvent evt = null;
			for(ListDataListener listener:listeners.getListeners(ListDataListener.class)) {
				if(evt==null) evt =new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, index0, index1);
				listener.contentsChanged(evt);
				}
			}
		public void fireIntervalAdded(int index0, int index1) {
			ListDataEvent evt = null;
			for(ListDataListener listener:listeners.getListeners(ListDataListener.class)) {
				if(evt==null) evt =new ListDataEvent(this, ListDataEvent.INTERVAL_ADDED, index0, index1);
				listener.intervalAdded(evt);
				}
			}
		public void fireIntervalRemoved(int index0, int index1) {
			ListDataEvent evt = null;
			for(ListDataListener listener:listeners.getListeners(ListDataListener.class)) {
				if(evt==null) evt =new ListDataEvent(this, ListDataEvent.INTERVAL_REMOVED, index0, index1);
				listener.intervalRemoved(evt);
				}
			}
	}
