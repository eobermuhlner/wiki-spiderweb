package ch.obermuhlner.android.lib.activity;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import ch.obermuhlner.android.wikibrowser.trial.R;

public class HtmlHelpActivity extends Activity {

	private WebView webView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		webView = new WebView(this);
		setContentView(webView);

		webView.loadUrl("file:///android_asset/" + getResources().getString(R.string.help_file));
	}
}
