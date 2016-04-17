package ch.obermuhlner.android.wikibrowser;

import java.util.Collection;

public class Page {
	
	private final String description;

	private final Collection<String> links;

	private final Collection<String> images;
	
	public Page(String description, Collection<String> links, Collection<String> imageLinks) {
		this.description = description;
		this.links = links;
		this.images = imageLinks;
	}
	
	public Collection<String> getLinks() {
		return links;
	}
	
	public Collection<String> getImages() {
		return images;
	}
	
	public String getDescription() {
		return description;
	}
}
