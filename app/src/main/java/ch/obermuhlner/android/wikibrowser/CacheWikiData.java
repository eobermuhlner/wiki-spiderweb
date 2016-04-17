package ch.obermuhlner.android.wikibrowser;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.graphics.Bitmap;

public class CacheWikiData extends WikiData {

	private final WikiData wikiData;

	private final Map<String, Page> cache = new HashMap<String, Page>();

	private final String messageFailedLoading; 

	public CacheWikiData(WikiData wikiData, String messageFailedLoading) {
		this.wikiData = wikiData;
		this.messageFailedLoading = messageFailedLoading;
	}
	
	@Override
	public String getBaseUrl() {
		return wikiData.getBaseUrl();
	}
	
	@Override
	public void setLanguage(String language) {
		wikiData.setLanguage(language);
	}
	
	@Override
	public Page loadPage(String name) {
		if (cache.containsKey(name)){
			return cache.get(name);
		}
		
		Page page = wikiData.loadPage(name);
		if (page == null) {
			page = new Page(messageFailedLoading, Collections.<String> emptyList(), Collections.<String> emptyList());
		} else {
			cache.put(name, page);
		}
		return page;
	}

	@Override
	public List<SearchResult> findPages(String searchText) {
		return wikiData.findPages(searchText);
	}

	@Override
	public Bitmap loadImage(String name) {
		return wikiData.loadImage(name);
	}
}
