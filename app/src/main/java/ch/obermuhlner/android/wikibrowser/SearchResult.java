package ch.obermuhlner.android.wikibrowser;

public class SearchResult {
	public String name;
	public String description;
	public String imageUrl;

	public SearchResult() {
	}
	
	public SearchResult(String name, String description, String imageUrl) {
		this.name = name;
		this.description = description;
		this.imageUrl = imageUrl;
	}
	
	@Override
	public String toString() {
		return "SearchResult{" + name + "," + description + "," + imageUrl + "}";
	}
}
