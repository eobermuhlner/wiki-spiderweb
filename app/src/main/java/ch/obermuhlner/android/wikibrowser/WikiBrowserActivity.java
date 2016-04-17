package ch.obermuhlner.android.wikibrowser;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import ch.obermuhlner.android.lib.activity.HtmlHelpActivity;
import ch.obermuhlner.android.lib.util.SimplePreferences;
import ch.obermuhlner.android.lib.view.graph.BackgroundImageLoader;
import ch.obermuhlner.android.lib.view.graph.Graph;
import ch.obermuhlner.android.lib.view.graph.Graph.OnImageLoaded;
import ch.obermuhlner.android.lib.view.graph.GraphView;
import ch.obermuhlner.android.lib.view.graph.GraphView.OnNodeSelectListener;
import ch.obermuhlner.android.lib.view.graph.Node;
import ch.obermuhlner.android.wikibrowser.trial.R;

public class WikiBrowserActivity extends Activity implements OnImageLoaded {
    private static final int REQUEST_GOTO_PAGE = 1;
    
	private static final int REQUEST_LOAD_GRAPH = 2;

	private static final int REQUEST_SAVE_GRAPH = 3;

	private WikiData wikiData;
	private GraphView graphView;

	private WebView webView;

	private String language;
	
	private SimplePreferences simplePreferences;

	private Set<String> loadingPages = Collections.synchronizedSet(new HashSet<String>());

	private String webPageName;

	private TextView textWebHandle;

	private boolean vibrate;

	private Vibrator vibrator;
	
	private String currentGraphName;
	
	private static String BASE64_MARKET_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAhRmrmGZO6usCJ1eJAhmXCXH9YuIS+LsPzKz8MzaQkK7yDtWovqLAsj10F8+waSHJPXDxsUQiL4PKyvYixwtfnYOevz1Q6wNn8JbPm3D9cWfvdWhqhWWXMDOsTj1ddp5PRNUYmHWVknYUvbs3wrRLul60eB0lhrY6tWxyuPo82V8I13C9P9O3dYpJqewWZuZdQNJ2yfNKRaKEf4HONEUoyuRZAOFzBLCZ3LFQImTF9ZQQnYJiJyGImS2tK0vWlwpd0iT3wmCWTtHXnoEHw1EeEA7hBQyEul1Flq1ttnf1mm2LLyXevulGP1TxeE7RleDcXwaUnNLLqpRynDsm8NCnZwIDAQAB";
	
	public BackgroundImageLoader imageLoader;

	@Override
	public void onImageLoaded(String imageName, Bitmap image) {
		WikiBrowserActivity.this.runOnUiThread(new Runnable() {
			public void run() {
				graphView.resetInteractionTime();
				WikiBrowserActivity.this.graphView.invalidate();
			}
		});
	}
	
	@Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);

        simplePreferences = new SimplePreferences(this);

        vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        
        setContentView(R.layout.main_panel);
        graphView = (GraphView) findViewById(R.id.graphView);
		/*
        webView = (WebView) findViewById(R.id.webView);
        webView.setWebViewClient(new MyWebViewClient());
        
        View webHandle= findViewById(R.id.webHandle);
        if (webHandle instanceof TextView) {
        	textWebHandle = (TextView) webHandle;
        }
		*/
		wikiData = WikiData.createWikiData(this, simplePreferences);

        imageLoader = new BackgroundImageLoader(wikiData, this);
        graphView.getGraph().setImageLoader(imageLoader);
        
        graphView.setOnNodeSelectListener(new OnNodeSelectListener() {
        	@Override
        	public boolean selectNode(Node node) {
        		showNodeContent(node);
        		return true;
        	}
        	
			@Override
			public boolean singleTapNode(Node node) {
				showNodeContent(node);
				addNodes(node.name);
				return true;
			}
			@Override
			public boolean doubleTapNode(Node node) {
				showNodeContent(node);
				Uri uri = getUri(node);
				//System.out.println("View " + uri);
				WikiBrowserActivity.this.startActivity(new Intent(Intent.ACTION_VIEW, uri));
				return true;
			}
		});

        if (inState == null) {
			addNodes(simplePreferences.getString(R.string.pref_startPage, getResources().getString(R.string.default_startPage)));
        } else{
            graphView.getGraph().load(inState);
            
        	graphView.setScaleFactor(inState.getFloat("scaleFactor"));
        	graphView.setOffsetX(inState.getFloat("offsetX"));
        	graphView.setOffsetY(inState.getFloat("offsetY"));
        	graphView.setTargetScaleFactor(inState.getFloat("targetScaleFactor"));
        	graphView.setTargetOffsetX(inState.getFloat("targetOffsetX"));
        	graphView.setTargetOffsetY(inState.getFloat("targetOffsetY"));
        	Node webPage = graphView.selectNode(inState.getString("webPage"));
        	if (webPage != null) {
        		showNodeContent(webPage);
        	}
        	graphView.selectNode(inState.getString("current"));
        	String[] pagesToLoad = inState.getStringArray("loadingPages");
        	for (String name : pagesToLoad) {
        		addNodes(name);
			}
        	// TODO load images
        }
        
		language = getPreferencesLanguage(simplePreferences, getResources());
	
//		graphView.setDrawingCacheEnabled(true);
//		Bitmap drawingCache = graphView.getDrawingCache();
//		graphView.setDrawingCacheEnabled(false);
		
		try {
			//simplePreferences.setString(R.string.pref_acceptedVersion, ""); // comment out to test
			PackageInfo packageInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
			String currentVersion = packageInfo.versionName;
			if (simplePreferences.getString(R.string.pref_acceptedVersion, "").compareTo(currentVersion) < 0) {
				simplePreferences.setString(R.string.pref_acceptedVersion, currentVersion);
				showMessage(this, R.string.new_version_message);
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}
	}

	private static void showMessage(Context context, int messageId) {
		showMessage(context, context.getResources().getString(messageId));
	}
	
	private static void showMessage(Context context, String message) {
		new AlertDialog.Builder(context)
	    .setMessage(message)
	    .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
	        @Override
	        public void onClick(DialogInterface dialog, int which) {
	            dialog.dismiss();
	        }
	    })
	    .show();
	}

	private String getDeviceId() {
		TelephonyManager telephonyManager =  (TelephonyManager) getSystemService(TELEPHONY_SERVICE);
		
		String androidId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
		String phoneId = telephonyManager.getDeviceId();
		
		return androidId + "," + phoneId;
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
		graphView.getGraph().save(outState);
		
		outState.putStringArray("loadingPages", loadingPages.toArray(new String[0]));
		outState.putFloat("scaleFactor", graphView.getScaleFactor());
		outState.putFloat("offsetX", graphView.getOffsetX());
		outState.putFloat("offsetY", graphView.getOffsetY());
		outState.putFloat("targetScaleFactor", graphView.getTargetScaleFactor());
		outState.putFloat("targetOffsetX", graphView.getTargetOffsetX());
		outState.putFloat("targetOffsetY", graphView.getTargetOffsetY());
		outState.putString("current", graphView.getSelectedNode());
		outState.putString("webPage", webPageName);
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		imageLoader.start();

		wikiData = WikiData.createWikiData(this, simplePreferences);
		
		String newLanguage = getPreferencesLanguage(simplePreferences, getResources());
		if (!newLanguage.equals(language)) {
			language = newLanguage;
			String newStartPage = getResources().getString(R.string.default_startPage);
			simplePreferences.setString(R.string.pref_startPage, newStartPage);
			graphView.clear();
			addNodes(newStartPage);
		}
		
		int maxChildrenCount = Integer.parseInt(simplePreferences.getString(R.string.pref_maxChildrenCount, "30"));
		graphView.getGraph().setMaxChildrenCount(maxChildrenCount);

		int maxImageCount = Integer.parseInt(simplePreferences.getString(R.string.pref_maxImageCount, "5"));
		graphView.getGraph().setMaxImageCount(maxImageCount);

		int keepOpenCount = Integer.parseInt(simplePreferences.getString(R.string.pref_keepOpen, "2"));
		graphView.getGraph().setKeepExplodedCount(keepOpenCount);
		
        float simulationTimeout = Float.parseFloat(simplePreferences.getString(R.string.pref_simulationTimeout, "10"));
        graphView.setSimulationTimeout((long) simulationTimeout * 1000);

        float speedLevel = Float.parseFloat(simplePreferences.getString(R.string.pref_speedLevel, "0.002"));
		graphView.setTimeFactor(speedLevel);
		
        float attractionForce = Float.parseFloat(simplePreferences.getString(R.string.pref_attractionLevel, "0.055"));
		graphView.getGraph().setAttractionForce(attractionForce);
		
        float repulsionForce = Float.parseFloat(simplePreferences.getString(R.string.pref_repulsionLevel, "200"));
		graphView.getGraph().setRepulsionForce(repulsionForce);
		
        float dampingLevel = Float.parseFloat(simplePreferences.getString(R.string.pref_dampingLevel, "0.85"));
		graphView.getGraph().setDamping(dampingLevel);
		
		float textSize = Float.parseFloat(simplePreferences.getString(R.string.pref_textSize, "16"));
		graphView.setTextSize(textSize);

		graphView.setMaxImageSize(64);
		
        vibrate = simplePreferences.getBoolean(R.string.pref_vibrate, true);
        graphView.setVibrate(vibrate);
	}
	
	@Override
	protected void onPause() {
		imageLoader.stop();
		
		super.onPause();
	}
	
	public static String getPreferencesLanguage(SimplePreferences simplePreferences, Resources resources) {
		String language = simplePreferences.getString(R.string.pref_language, resources.getString(R.string.default_language));
		boolean useCustomLanguage = simplePreferences.getBoolean(R.string.pref_useCustomLanguage);
		if (useCustomLanguage) {
			language = simplePreferences.getString(R.string.pref_customLanguage);
		}
		return language;
	}
	
    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main_menu, menu);
		
		return super.onCreateOptionsMenu(menu);
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		return onOptionsItemSelected(this, item);
	}

	public boolean onOptionsItemSelected(Activity activity, MenuItem item) {
		switch(item.getItemId()) {
		case R.id.menu_main_load:
			activity.startActivityForResult(new Intent(activity, LoadGraphActivity.class), REQUEST_LOAD_GRAPH);
			break;
		case R.id.menu_main_save:
			Intent intent = new Intent(activity, SaveGraphActivity.class);
			String graphName = currentGraphName;
			if (graphName == null) {
				graphName = webPageName;
			}
			intent.putExtra(SaveGraphActivity.EXTRA_GRAPH_NAME, graphName);
			activity.startActivityForResult(intent, REQUEST_SAVE_GRAPH);
			break;
		case R.id.menu_main_settings:
			activity.startActivity(new Intent(activity, ApplicationPreferencesActivity.class));
			break;
		case R.id.menu_main_goto:
			activity.startActivityForResult(new Intent(activity, GotoActivity.class), REQUEST_GOTO_PAGE);
			break;
		case R.id.menu_main_help:
			activity.startActivity(new Intent(activity, HtmlHelpActivity.class));
			break;
		}

		return true;
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode != RESULT_OK) {
			return;
		}
		
		switch(requestCode) {
			case REQUEST_GOTO_PAGE: {
				String pageName = data.getExtras().getString(GotoActivity.EXTRA_PAGE_NAME);
				graphView.clear();
				addNodes(pageName);
				break;
			}
			case REQUEST_LOAD_GRAPH: {
				String graphName = data.getExtras().getString(LoadGraphActivity.EXTRA_GRAPH_NAME);
				if (graphName != null) {
					Graph loadedGraph = GraphStorage.loadGraph(this, graphName);
					if (loadedGraph != null) {
						graphView.setGraph(loadedGraph);
						currentGraphName = graphName;
					}
				}
				break;
			}
			case REQUEST_SAVE_GRAPH: {
				String graphName = data.getExtras().getString(SaveGraphActivity.EXTRA_GRAPH_NAME);
				if (graphName != null) {
					GraphStorage.saveGraph(this, graphName, graphView.getGraph());
				}
				break;
			}
		}
	}
	
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		return super.onKeyDown(keyCode, event);
//		
//	    if ((keyCode == KeyEvent.KEYCODE_BACK)) {
//	    	// TODO handle back key
//	    }
//	    
//	    return false;
	}
	
	private Uri getUri(Node node) {
		return Uri.parse("http://" + language + ".wikipedia.org/wiki/" + WikiData.encodeName(node.name));
	}

	private void showNodeContent(final Node node) {
		if (Config.SHOW_DESCRIPTION) {
			if (node.image) {
				showImage(node.name, node.bitmap);
			} else {
				if (node.content != null) {
					showWebContent(node.name, node.content);
				} else {
					new Thread() {
						@Override
						public void run() {
							graphView.setStatusMessage(WikiBrowserActivity.this.getResources().getString(R.string.loading, node.name));
							Page page = wikiData.loadPage(node.name);
							node.content = page.getDescription();
							graphView.setStatusMessage(null);
							
							WikiBrowserActivity.this.runOnUiThread(new Runnable() {
								public void run() {
									showWebContent(node.name, node.content);
								}
							});
						}
					}.start();
				}
			}
		}
	}

	private void showImage(String name, Bitmap bitmap) {
		webPageName = name;
		if (textWebHandle != null) {
			textWebHandle.setText(fileName(name));
		}
		//webView.loadData(Uri.encode("<img src=\"" + name + "\"/>"), "text/html", "utf-8");
	}

	private String fileName(String name) {
		int slashIndex = name.lastIndexOf("/");
		if (slashIndex >= 0) {
			name = name.substring(slashIndex + 1);
		}
		int dotIndex = name.lastIndexOf(".");
		if (dotIndex >= 0) {
			name = name.substring(0, dotIndex);
		}
		return name;
	}

	private void showWebContent(String name, String content) {
		webPageName = name;
		if (textWebHandle != null) {
			textWebHandle.setText(name);
		}
		//webView.loadDataWithBaseURL(wikiData.getBaseUrl(), content, "text/html", "utf-8", null);
	}
	
	private void addNodes(final String name) {
		loadingPages.add(name);
		new Thread() {
			@Override
			public void run() {
				graphView.setStatusMessage(WikiBrowserActivity.this.getResources().getString(R.string.loading, name));
				final Page page = wikiData.loadPage(name);
				loadingPages.remove(name);
				graphView.setStatusMessage(null);
				
				WikiBrowserActivity.this.runOnUiThread(new Runnable() {
					public void run() {

						graphView.resetInteractionTime();
						Node node = graphView.getGraph().addNodes(name, page.getDescription(), page.getLinks(), page.getImages(), true);
						node.visited = true;
						showNodeContent(node);

						if (!node.image) {
							if (simplePreferences.getBoolean(R.string.pref_rememberLastPage)) {
								simplePreferences.setString(R.string.pref_startPage, name);
							}
						}

						WikiBrowserActivity.this.graphView.invalidate();
					}
				});
			}
		}.start();
	}

	private void setNodeImage(String imageUrl, Bitmap image) {
		Node node = graphView.getGraph().getNode(imageUrl);
		node.bitmap = image;
		graphView.invalidate();
	}

	private class MyWebViewClient extends WebViewClient {
	    @Override
	    public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    	System.out.println("Following browser link " + url);
	    	int wikiPathPos = url.indexOf("/wiki/");
	    	if (wikiPathPos >= 0) {
	    		final String relativeUrl = url.substring(wikiPathPos + "/wiki/".length());
	    		String link = WebPageWikiData.extractNameFromLink(url);
	    		if (link != null) {
	    			if (vibrate) {
	    				vibrator.vibrate(20);
	    			}
	    			
	    			final String linkName = WebPageWikiData.decodeName(link);
					WikiBrowserActivity.this.runOnUiThread(new Runnable() {
						public void run() {
					    	System.out.println("Selecting node from browser link " + linkName + " from " + relativeUrl);
			    			graphView.getGraph().addNodes(webPageName, null, Arrays.asList(linkName), Collections.<String> emptySet(), false); // make sure the link exists
			    			graphView.selectNode(linkName);
			    			graphView.centerCurrentNode();
			    			//addNodes(linkName); // explode link // TODO does not work correctly (threading problems?)
							WikiBrowserActivity.this.graphView.invalidate();
						}
					});
	    		}
	    		return true;
	    	}
	    	return false;
	    }
	}
	
	public void showMessageDialog(String message) {
		new AlertDialog.Builder(this)
		.setMessage(message)
		.setCancelable(false)
		.setIcon(R.drawable.wiki_browser)
		.setPositiveButton(getResources().getString(R.string.ok), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				WikiBrowserActivity.this.finish();
			}
		})
		.show();
	}
}