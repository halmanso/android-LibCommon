package edu.purdue.libwaterapps.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class NotifyArrayList<E> extends ArrayList<E> {
	/**
	 * ArrayList implements serializable, so a serialVersionID is needed
	 */
	private static final long serialVersionUID = 1L;
	private List<OnListChangeListener<E>> listeners = Collections.synchronizedList(new ArrayList<OnListChangeListener<E>>());
	private enum ChangeAction { ADD, REMOVE };

	@Override
	public boolean add(E rock) {
		boolean result = super.add(rock);
		
		// Send a notify out if now in list
		if(result) {
			notifyListeners(ChangeAction.ADD, rock);
		}

		return result;
	}
	
	@Override
	public void add(int index, E e) {
		super.add(index, e);
		notifyListeners(ChangeAction.ADD, e);
	}
	
	@Override
	public boolean addAll(Collection<? extends E> es) {
		boolean result = super.addAll(es);
		
		// Send a notify out, one for each one if now in list
		if(result) {
			for(E e : es) {
				notifyListeners(ChangeAction.ADD, e);
			}
		}
		
		return result;
	}

	@Override
	public boolean addAll(int index, Collection<? extends E> es) {
		boolean result = super.addAll(index, es);
	
		// Send a notify out, one for each one if now in list
		if(result) {
			for(E e : es) {
				notifyListeners(ChangeAction.ADD, e);
			}
		}
		
		return result;
	}

	@Override
	public void clear() {
		// Keep a local copy to use during the later notifications
		List<E> localCopy = new ArrayList<E>(this);
		
		super.clear();
		
		// Send a delete notification for all the elements
		for(E e : localCopy) {
			notifyListeners(ChangeAction.REMOVE, e);
		}
	}

	@Override
	public E remove(int index) {
		E e = super.remove(index);
		
		notifyListeners(ChangeAction.REMOVE, e);
		
		return e;
	}

	// Cast is safe. If object is removed from list then it must have been type E.
	@SuppressWarnings("unchecked")
	@Override
	public boolean remove(Object e) {
		boolean result = super.remove(e);
		
		// If it was in the list and removed then we should tell the world
		if(result) {
			notifyListeners(ChangeAction.REMOVE, (E)e);
		}
		
		return result;
	}

	@Override
	protected void removeRange(int fromIndex, int toIndex) {
		List<E> es = new ArrayList<E>(this.subList(fromIndex, toIndex));
		
		super.removeRange(fromIndex, toIndex);
		
		// Send a delete notification for all the listeners
		for(E e : es) {
			notifyListeners(ChangeAction.REMOVE, e);
		}
	}

	@Override
	public E set(int index, E newE) {
		E oldE = super.set(index, newE);
		
		notifyListeners(ChangeAction.REMOVE, oldE);
		notifyListeners(ChangeAction.ADD, newE);
		
		return oldE;
	}
	
	/*
	 * Register a listener to the ArrayList
	 */
	public void addOnListChangeListener(OnListChangeListener<E> listener) {
		this.listeners.add(listener);
	}
	
	/*
	 * Remove a previously registered listener of the ArrayList
	 */
	public void removeOnListChangeListener(OnListChangeListener<E> listener) {
		this.listeners.remove(listener);
	}

	/*
	 * Notifies all registered listeners of the changes
	 */
	private void notifyListeners(ChangeAction action, E e) {
		for (OnListChangeListener<E> listener : listeners) {
			switch (action) {
				case ADD:	
					listener.onAdd(e);
				break;

				case REMOVE:
					listener.onRemove(e);
				break;
			}
		}
	}

	/*
	 * An interface that is used to track changes to the list
	 */
	public interface OnListChangeListener<E> {

		public void onAdd(E e);
		public void onRemove(E e);
	}
}
