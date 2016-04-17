package ch.obermuhlner.android.wikibrowser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import ch.obermuhlner.android.lib.view.graph.UrlImageLoader;

import android.content.Context;
import android.graphics.Bitmap;

public class WebPageWikiData extends WikiData {

	private String language;

	private Pattern patternParagraph = Pattern.compile("[ \t]*<p>");
	private Pattern patternHref = Pattern.compile("href=\"/wiki/([^\"#?]*)([#?][^\"]*)?\"");
	private Pattern patternImgSrc = Pattern.compile("<img[^<]*src=\"([^\"]*)\"");

	private static Pattern patternLink = Pattern.compile("/wiki/([^\"#?]*)([#?][^\"]*)?");
	
	private static final Set<String> forbiddenImageLinks = new HashSet<String>();
	
	static {
		//forbiddenImageLinks.add("FNORD");
	}
	
	public WebPageWikiData(Context context) {
	}

	@Override
	public void setLanguage(String language) {
		this.language = language;
	}

	@Override
	public Page loadPage(String name) {
		return parseWebPage(name);
	}
	
	private Page parseWebPage(String name) {

		long startTime = System.currentTimeMillis();
		
		BufferedReader reader = null;
		
		StringBuilder description = new StringBuilder();
		Collection<String> pageLinks = new ArrayList<String>();
		Collection<String> imageLinks = new ArrayList<String>();

		try {
			URL url = getWikiUrl(name);
			//System.out.println("Start loading " + url);
			URLConnection connection = url.openConnection();
			//System.out.println("Headers: " + connection.getHeaderFields());
			reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			
			
			String line;
			do {
				line = reader.readLine();
				if (line != null) {
					//System.out.println(line);
					
					if (Config.SHOW_DESCRIPTION) {
						Matcher matcherParagraph = patternParagraph.matcher(line);
						if (matcherParagraph.find()) {
							description.append(line);
							description.append("\n");
						}
					}
					
					Matcher matcherHref = patternHref.matcher(line);
					while (matcherHref.find()) {
						String match = matcherHref.group(1);
						if (!match.contains(":")) {
							if (!pageLinks.contains(match)) {
								pageLinks.add(decodeName(match));
							}
						}
					}

					if (Config.SHOW_IMAGE_NODES) {
						Matcher matcherImgSrc = patternImgSrc.matcher(line);
						while (matcherImgSrc.find()) {
							String match = matcherImgSrc.group(1);
							//System.out.println("IMAGE " + match + " IN LINE " + line);
							if (match.startsWith("//")) {
								match = "http:" + match;
							}
							if (!forbiddenImageLinks.contains(match)) {
								if (!imageLinks.contains(match)) {
									imageLinks.add(match);
								}
							}
						}
					}

					if (Config.LOAD_PARTIAL_WEBPAGE) {
						if ((line.contains("<h2") && !line.contains("<h2>Contents</h2>")) || line.contains("<h3")) {
							return new Page(description.toString(), pageLinks, imageLinks);
						}
					}
				}
			} while (line != null);
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		finally {
			long endTime = System.currentTimeMillis();
			double deltaTime = (endTime - startTime) / 1000.0;
			System.out.println("Loaded " + name + " with "+ pageLinks.size() + " links in " + deltaTime + " s");
			
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException ignored) {
					// ignored
				}
			}
		}

		return new Page(description.toString(), pageLinks, imageLinks);
	}

	@Override
	public List<SearchResult> findPages(String searchText) {
		return findPagesDetails(searchText);
	}

	private List<SearchResult> findPagesQuick(String searchText) {
		String encodedSearchText = encodeName(searchText.trim());
		
		List<SearchResult> result = new ArrayList<SearchResult>();
	
		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet("http://" + language + ".wikipedia.org//w/api.php?action=opensearch&namespace=0&limit=20&search=" + encodedSearchText);
		try {
			HttpResponse response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			String content = toString(entity.getContent());
			JSONArray answer = new JSONArray(content);
			JSONArray suggestions = answer.getJSONArray(1);
			int count = suggestions.length();
			for (int i = 0; i < count; i++) {
				String suggestion = suggestions.getString(i);
				result.add(new SearchResult(suggestion, null, null));
			}
			
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		return result;
	}

	private List<SearchResult> findPagesDetails(String searchText) {
		String encodedSearchText = encodeName(searchText.trim());

		HttpClient httpClient = new DefaultHttpClient();
		HttpGet httpGet = new HttpGet("http://" + language + ".wikipedia.org//w/api.php?action=opensearch&namespace=0&limit=20&format=xml&search=" + encodedSearchText);
		try {
			HttpResponse response = httpClient.execute(httpGet);
			HttpEntity entity = response.getEntity();
			SAXParserFactory factory = SAXParserFactory.newInstance();
			try {
				SAXParser parser = factory.newSAXParser();
				SearchResultHandler handler = new SearchResultHandler();
				parser.parse(entity.getContent(), handler);
				return handler.getSearchResults();
			} catch (Exception e) {
				throw new RuntimeException(e);
			} 
		} catch (ClientProtocolException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
	
	private class SearchResultHandler extends DefaultHandler {
		
		private List<SearchResult> result = new ArrayList<SearchResult>();

		private StringBuilder stringBuilder = new StringBuilder();

		private SearchResult currentSearchResult;
		
		public List<SearchResult> getSearchResults() {
			return result;
		}
		
		@Override
		public void characters(char[] ch, int start, int length) throws SAXException {
			super.characters(ch, start, length);
	        stringBuilder.append(ch, start, length);
		}
		
		@Override
		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			super.startElement(uri, localName, qName, attributes);
			
			if (localName.equalsIgnoreCase("item")) {
	            currentSearchResult = new SearchResult();
	        }
			else if (localName.equalsIgnoreCase("image")) {
				if (currentSearchResult != null) {
					currentSearchResult.imageUrl = attributes.getValue("source");
				}
	        }
		}
		
		@Override
		public void endElement(String uri, String localName, String qName) throws SAXException {
			super.endElement(uri, localName, qName);

			if (localName.equalsIgnoreCase("text")) {
				if (currentSearchResult != null) {
					currentSearchResult.name = stringBuilder.toString();
				}
	        }
			else if (localName.equalsIgnoreCase("description")) {
				if (currentSearchResult != null) {
					currentSearchResult.description = stringBuilder.toString();
				}
	        }
			else if (localName.equalsIgnoreCase("item")) {
				if (currentSearchResult != null) {
					//System.out.println("SEARCHRESULT " + currentSearchResult);
					result.add(currentSearchResult);
				}
			}
			
			stringBuilder.setLength(0);
		}
	}
	
	private String toString(InputStream stream) throws IOException {
		BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
		char[] buffer = new char[1024];
		
		StringBuilder result = new StringBuilder();
		int count = reader.read(buffer);
		while (count != -1) {
			result.append(buffer, 0, count);
			count = reader.read(buffer);
		}
		
		return result.toString();
	}

	public String getBaseUrl() {
		return "https://" + language + ".m.wikipedia.org/wiki/";
	}
	
	@Override
	public Bitmap loadImage(String imageName) {
		return UrlImageLoader.INSTANCE.loadImage(imageName);
	}
	
	private URL getWikiUrl(String name) throws MalformedURLException {
		String encodedName = encodeName(name);
		return new URL(getBaseUrl() + encodedName);
	}
	
	public static String extractNameFromLink(String wikiLink) {
		Matcher matcher = patternLink.matcher(wikiLink);
		if (matcher.find()) {
			return matcher.group(1);
		}
		return null;
	}
}
