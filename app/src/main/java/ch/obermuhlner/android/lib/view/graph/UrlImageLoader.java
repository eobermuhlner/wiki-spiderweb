package ch.obermuhlner.android.lib.view.graph;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class UrlImageLoader implements ImageLoader {

	public static final UrlImageLoader INSTANCE = new UrlImageLoader();
	
	@Override
	public Bitmap loadImage(String imageName) {
		try {
			URL url = new URL(imageName);
			InputStream stream = url.openStream();
			Bitmap image = BitmapFactory.decodeStream(stream);
			return image;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return null;
	}

}
