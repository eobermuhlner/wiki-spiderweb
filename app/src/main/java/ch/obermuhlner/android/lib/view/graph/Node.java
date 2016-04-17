package ch.obermuhlner.android.lib.view.graph;

import ch.obermuhlner.android.lib.view.graph.Graph.OnImageLoaded;
import android.graphics.Bitmap;

public class Node implements OnImageLoaded {

	float textSizeFactor = 1.0f;
	
	public String name;

	public String content;

	public Bitmap bitmap;
	
	public boolean image;
	
	public float x;
	public float y;

	float forceX;
	float forceY;
	
	float speedX;
	float speedY;
	
	public boolean visited;
	public boolean fix;

	boolean drag;
	boolean exploded;

	int connectionCount;

	public Node() {
	}
	
	@Override
	public String toString() {
		return getClass().getSimpleName() + "{" + x + "," + y + "}";
	}

	@Override
	public void onImageLoaded(String imageName, Bitmap image) {
		bitmap = image;
	}
}
