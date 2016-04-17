package ch.obermuhlner.android.wikibrowser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

public class DummyWikiData extends WikiData {

	@Override
	public void setLanguage(String language) {
		// ignore
	}

	@Override
	public Page loadPage(String name) {
		String description = "<p>This is the definition of: " + name +"</p>" +
				"<p>Link to <a href=\"x\">x</a></p>" +
				"<p>Link to <a href=\"y\">y</a></p>";
		
		String link1 = "" + (char)(name.charAt(0) - 1);
		String link2 = "" + (char)(name.charAt(0) + 1);
		String link3 = "" + (char)(name.charAt(0) + 2);
		
		Collection<String> links = Arrays.asList("x", link1, link2, link3);
		Collection<String> images = Arrays.asList(name + "1.jpg", name + "2.jpg", name + "3.jpg", name + "4.jpg", name + "5.jpg", name + "6.jpg");
		
		return new Page(description, links, images);
	}

	@Override
	public List<SearchResult> findPages(String searchText) {
		ArrayList<SearchResult> result = new ArrayList<SearchResult>();
		
		result.add(new SearchResult("a", "Short def of a", "a_small.jpg"));
		result.add(new SearchResult("b", "Short def of b", "b_small.jpg"));
		result.add(new SearchResult("c", "Short def of c", "c_small.jpg"));
		
		return result;
	}

	@Override
	public String getBaseUrl() {
		return "";
	}

	@Override
	public Bitmap loadImage(String imageName) {
		int width = 200;
		int height = 200;
		Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
		Canvas canvas = new Canvas(bitmap);
		
		canvas.drawColor(Color.WHITE);
		
		Paint paint = new Paint();
		paint.setColor(Color.BLUE);
		paint.setAntiAlias(true);
		paint.setAlpha(0x80);
		paint.setStyle(Style.STROKE);
		paint.setStrokeWidth(2);
		
		canvas.drawLine(0, 0, width, height, paint);
		canvas.drawLine(width, 0, 0, height, paint);
		
		Paint paintText = new Paint();
		paintText.setColor(Color.BLACK);
		paintText.setTextSize(26);
		paintText.setAntiAlias(true);
		
		canvas.drawText(imageName, 20, 20+paint.getFontSpacing(), paintText);
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return bitmap;
	}
}
