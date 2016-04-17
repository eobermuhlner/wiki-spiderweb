package ch.obermuhlner.android.lib.view.graph;

import java.util.Collection;

import ch.obermuhlner.android.lib.view.graph.Graph.OnImageLoaded;

public interface AsyncImageLoader {
	
	void loadImage(String imageName, OnImageLoaded onImageLoaded);
	
	void clear();

	Collection<String> getPending();
}