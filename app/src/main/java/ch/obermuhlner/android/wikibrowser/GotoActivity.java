package ch.obermuhlner.android.wikibrowser;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import ch.obermuhlner.android.lib.view.graph.BackgroundImageLoader;
import ch.obermuhlner.android.lib.view.graph.Graph.OnImageLoaded;
import ch.obermuhlner.android.wikibrowser.trial.R;

public class GotoActivity extends Activity {

	public static final String EXTRA_PAGE_NAME = "pageName";

	private WikiData wikiData;

	private EditText searchText;
	private ImageButton searchButton;
	private TextView searchResultInfo;
	private ListView searchResultList;

	private String lastSearchText;

	private BackgroundImageLoader imageLoader;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		wikiData = WikiData.createWikiData(this);
		
        imageLoader = new BackgroundImageLoader(wikiData, null);

        setContentView(R.layout.goto_page);
        searchText = (EditText) findViewById(R.id.searchText);
        searchButton = (ImageButton) findViewById(R.id.searchButton);
        searchResultInfo = (TextView) findViewById(R.id.searchResultInfo);
        searchResultList = (ListView) findViewById(R.id.searchResultList);
		
		searchResultInfo.setText(getResources().getString(R.string.searchResultInitial));
        
        searchButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				lastSearchText = searchText.getText().toString();
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(searchText.getWindowToken(), 0);

				updateSearchList(lastSearchText);
			}
		});
        searchResultList.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent intent = new Intent();
				SearchResult searchResult = (SearchResult) parent.getItemAtPosition(position);
				intent.putExtra(EXTRA_PAGE_NAME, searchResult.name);
				GotoActivity.this.setResult(RESULT_OK, intent);
				GotoActivity.this.finish();
			}
		});
        
        if (savedInstanceState != null) {
        	String search = savedInstanceState.getString("search");
        	if (search != null) {
        		searchText.setText(search);
        	}
        	lastSearchText = savedInstanceState.getString("lastSearch");
        	updateSearchList(lastSearchText);
        }
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);

		outState.putString("search", searchText.getText().toString());
		outState.putString("lastSearch", lastSearchText);
	}
	
	@Override
	protected void onResume() {
		super.onResume();

		imageLoader.start();
	}
	
	@Override
	protected void onPause() {
		imageLoader.stop();
		
		super.onPause();
	}
	
	protected void updateSearchList(final String text) {
		new Thread() {
			public void run() {
				final Context context = GotoActivity.this;
				
				GotoActivity.this.runOnUiThread(new Runnable() {
					public void run() {
						searchResultInfo.setText(getResources().getString(R.string.searchResultStarted));
					}
				});
				
				final List<SearchResult> searchResults = wikiData.findPages(text);
				preloadImages(searchResults);

				GotoActivity.this.runOnUiThread(new Runnable() {
					public void run() {
						if (searchResults == null) {
							searchResultInfo.setText(getResources().getString(R.string.searchResultError));
							return;
						}
						
						switch(searchResults.size()) {
						case 0 :
							searchResultInfo.setText(getResources().getString(R.string.searchResult0));
							break;
						case 1 :
							searchResultInfo.setText(getResources().getString(R.string.searchResult1));
							break;
						default :
							searchResultInfo.setText(getResources().getString(R.string.searchResultN, searchResults.size()));
							break;
						}
						
						searchResultList.setAdapter(new SearchResultAdapter(context, searchResults));
					}
				});
			}
		}.start();
	}
	
	private void preloadImages(List<SearchResult> searchResults) {
		for (SearchResult searchResult : searchResults) {
			if (searchResult.imageUrl != null) {
	        	GotoActivity.this.imageLoader.loadImage(searchResult.imageUrl, null); // just to cache the images
			}
		}
	}

	private class SearchResultAdapter extends BaseAdapter {

		private final Context context;
		private final List<SearchResult> searchResults;

		public SearchResultAdapter(Context context, List<SearchResult> searchResults) {
			this.context = context;
			this.searchResults = searchResults;
		}
		
		@Override
		public int getCount() {
			return searchResults.size();
		}

		@Override
		public Object getItem(int i) {
			return searchResults.get(i);
		}

		@Override
		public long getItemId(int i) {
			return i;
		}

		@Override
		public View getView(int i, View view, ViewGroup viewgroup) {
			LayoutInflater layoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		    view = layoutInflater.inflate(R.layout.search_result_list_item, null);

		    if (view != null) {
		    	SearchResult searchResult = searchResults.get(i);
		    	
		        final ImageView iconImageView = (ImageView) view.findViewById(R.id.searchResultIcon);		    	
		        TextView titleTextView = (TextView) view.findViewById(R.id.searchResultTitle);
		        TextView descriptionTextView = (TextView) view.findViewById(R.id.searchResultDescription);
		        
		        titleTextView.setText(searchResult.name);
		        descriptionTextView.setText(searchResult.description);
		        
		        if (searchResult.imageUrl != null) {
		        	GotoActivity.this.imageLoader.loadImage(searchResult.imageUrl, new OnImageLoaded() {
						@Override
						public void onImageLoaded(String imageName, final Bitmap image) {
							GotoActivity.this.runOnUiThread(new Runnable() {
								public void run() {
									iconImageView.setImageBitmap(image);
								}
							});
						}
					});
		        }
		    }
		    
			return view;
		}
		
	}
}
