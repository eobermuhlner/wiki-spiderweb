package ch.obermuhlner.android.lib.view.graph;

import java.util.LinkedHashMap;

public class LruMap<K,V> extends LinkedHashMap<K, V> {

	private static final long serialVersionUID = 0L;

	private int maxSize;

	public LruMap(int maxSize) {
		super(16, 0.75f, true);
		
		this.maxSize = maxSize;
	}
	
	@Override
	protected boolean removeEldestEntry(java.util.Map.Entry<K, V> eldest) {
		return size() > maxSize;
	}
}
