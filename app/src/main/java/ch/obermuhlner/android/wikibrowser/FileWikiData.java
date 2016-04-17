package ch.obermuhlner.android.wikibrowser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.content.Context;
import android.graphics.Bitmap;

public class FileWikiData extends WikiData {

	private final Context context;

	private String language;

	public FileWikiData(Context context) {
		this.context = context;
	}
	
	@Override
	public String getBaseUrl() {
		return "file://android_asset/";
	}
	
	@Override
	public void setLanguage(String language) {
		this.language = language;
	}

	@Override
	public Page loadPage(String name) {
		final Set<String> links = new HashSet<String>();
		
		final String encodedName = encodeName(name);
		
		searchData(new OnLineReadListener() {
			String current = null;

			@Override
			public boolean readLine(String line) {
				if (line.startsWith("\t")) {
					if (encodedName.equals(current)) {
						links.add(decodeName(line.trim()));
					}
				} else {
					if (encodedName.equals(current)) {
						return true;
					}
					current = line.trim();
				}
				return false;
			}
		});

		return new Page(createFakeHtml(name, links), links, Collections.<String> emptySet());
	}
	
	private String createFakeHtml(String name, Set<String> links) {
		StringBuilder html = new StringBuilder();
		
		html.append("<p>This is the definition of <b>" + name + "</b>.</p>\n");
		if (links.size() == 0) {
			html.append("<p>No links available.</p>\n");
		} else {
			html.append("<p>The following " + links.size() + " links are available:</p>\n");
			html.append("<ul>");
			List<String> sortedLinks = new ArrayList<String>(links);
			Collections.sort(sortedLinks);
			for (String linkName : sortedLinks) {
				html.append("<li>");
				html.append("<a href=\"/wiki/" + WikiData.encodeName(linkName) + "\">" + linkName + "</a>");
				html.append("</li>\n");
			}
			html.append("</ul>");
		}
		
		return html.toString();
	}

	@Override
	public List<SearchResult> findPages(String searchText) {
		final List<SearchResult> result = new ArrayList<SearchResult>();

		final String encodedSearchText = encodeName(searchText.trim()).toLowerCase();

		searchData(new OnLineReadListener() {
			@Override
			public boolean readLine(String line) {
				if (!line.startsWith("\t")) {
					if (line.toLowerCase().startsWith(encodedSearchText)) {
						result.add(new SearchResult(decodeName(line), null, null));
					}
				}
				return false;
			}
		});

		return result;
	}
	
	@Override
	public Bitmap loadImage(String name) {
		return null;
	}
	
	private void searchData(OnLineReadListener onLineReadListener) {
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(context.getAssets().open(language + "_wiki_links.txt")));
						
			String line = null;
			do {
				line = reader.readLine();

				if (line != null) {
					if (onLineReadListener.readLine(line)) {
						return;
					}
				}
			} while (line != null);
			
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ignored) {
					// ignored
				}
			}
		}
	}

	private interface OnLineReadListener {
		boolean readLine(String line);
	}
}
