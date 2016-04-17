package ch.obermuhlner.android.wikibrowser;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import ch.obermuhlner.android.lib.util.SimplePreferences;
import ch.obermuhlner.android.lib.view.graph.ImageLoader;
import ch.obermuhlner.android.wikibrowser.trial.R;

abstract class WikiData implements ImageLoader {

	public abstract void setLanguage(String language);

	public abstract Page loadPage(String name);
	
	public abstract List<SearchResult> findPages(String searchText);
	
	public abstract String getBaseUrl();
	
	public static WikiData createWikiData(Context context) {
		return createWikiData(context, new SimplePreferences(context));
	}
	
	public static WikiData createWikiData(Context context, SimplePreferences simplePreferences) {
		WikiData wikiData;

		
		if (Config.DUMMY_DATA) {
			wikiData = new DummyWikiData();
		} else {
			boolean useLinkServer = false;
			if (Config.SUPPORT_LINK_SERVER) {
				useLinkServer = simplePreferences.getBoolean(R.string.pref_useLinkServer, false);
			}
			if (useLinkServer) {
				wikiData = new FileWikiData(context);
			} else {
				wikiData = new WebPageWikiData(context);
			}
		}		
		String language = WikiBrowserActivity.getPreferencesLanguage(simplePreferences, context.getResources());
		wikiData.setLanguage(language);

		return new CacheWikiData(wikiData, context.getResources().getString(R.string.failedLoading));
	}
	
	public static String encodeName(String decodedName) {
		return URLEncoder.encode(decodedName.replace(' ', '_'));
	}
	
	public static String decodeName(String encodedName) {
		return URLDecoder.decode(encodedName.replace('_', ' '));
	}
}