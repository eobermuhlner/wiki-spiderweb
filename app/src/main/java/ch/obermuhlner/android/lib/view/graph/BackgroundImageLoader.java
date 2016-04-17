package ch.obermuhlner.android.lib.view.graph;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.graphics.Bitmap;
import ch.obermuhlner.android.lib.view.graph.Graph.OnImageLoaded;

public class BackgroundImageLoader implements AsyncImageLoader {

	private Set<ImageToLoad> loadingImages = new HashSet<ImageToLoad>();
	private Thread thread;
	private volatile boolean finished;
	private Object lock = new Object();
	
	private static Map<String, Bitmap> cache = new LruMap<String, Bitmap>(30);
	private final ImageLoader imageLoader;
	private final OnImageLoaded globalOnImageLoaded;

	public BackgroundImageLoader(ImageLoader imageLoader, OnImageLoaded globalOnImageLoaded) {
		this.imageLoader = imageLoader;
		this.globalOnImageLoaded = globalOnImageLoaded;
	}
	
	public void start() {
		//System.out.println("BackgroundImageLoader started");
		finished = false;
		
		synchronized (lock) {
			thread = new Thread() {
				@Override
				public void run() {
					while (!finished) {
						//System.out.println("BackgroundImageLoader checking");
						boolean busy;
						synchronized (lock) {
							busy = !loadingImages.isEmpty();
						}
						if (busy) {
							ImageToLoad imageToLoad = loadingImages.iterator().next();
							loadingImages.remove(imageToLoad);
							//System.out.println("Loading image: " + imageToLoad.imageName);
							Bitmap image = imageLoader.loadImage(imageToLoad.imageName);
							if (image == null) {
								//System.out.println("Image not found: " + imageToLoad.imageName);
							} else {
								//System.out.println("Loaded image: " + imageToLoad.imageName + " " + image.getWidth() + "x" + image.getHeight());
								synchronized (cache) {
									cache.put(imageToLoad.imageName, image);
								}
								if (imageToLoad.onImageLoaded != null) {
									imageToLoad.onImageLoaded.onImageLoaded(imageToLoad.imageName, image);
								}
								if (globalOnImageLoaded != null) {
									globalOnImageLoaded.onImageLoaded(imageToLoad.imageName, image);
								}
							}
						}
						try {
							synchronized (lock) {
								if (loadingImages.isEmpty()) {
									//System.out.println("BackgroundImageLoader waiting");
									lock.wait();
								}
							}
						} catch (InterruptedException e) {
							finished = true;
						}
					}
					//System.out.println("BackgroundImageLoader died");
				}
			};
			thread.setPriority(2);
			thread.start();
		}
	}

	public void stop() {
		synchronized (lock) {
			//System.out.println("BackgroundImageLoader stopped");
			finished = true;
			thread = null;
			
			lock.notifyAll();
		}
	}

	@Override
	public void loadImage(String imageName, OnImageLoaded onImageLoaded) {
		synchronized (cache) {
			if (cache.containsKey(imageName)) {
				if (onImageLoaded != null) {
					onImageLoaded.onImageLoaded(imageName, cache.get(imageName));
				}
				return;
			}
		}
		
		synchronized (lock) {
			ImageToLoad imageToLoad = new ImageToLoad();
			imageToLoad.imageName = imageName;
			imageToLoad.onImageLoaded = onImageLoaded;

			loadingImages.add(imageToLoad);

			lock.notifyAll();
		}
	}
	
	@Override
	public void clear() {
		synchronized (lock) {
			loadingImages.clear();
		}
	}
	
	@Override
	public Collection<String> getPending() {
		synchronized (lock) {
			Collection<String> result = new ArrayList<String>();

			for (ImageToLoad imageToLoad : loadingImages) {
				result.add(imageToLoad.imageName);
			}
			return result;
		
		}
	}

	private static class ImageToLoad {
		String imageName;
		OnImageLoaded onImageLoaded;
	}
}
