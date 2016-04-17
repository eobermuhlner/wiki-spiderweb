package ch.obermuhlner.android.wikibrowser;

import android.app.ListActivity;
import android.app.SearchManager;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class SearchActivity extends ListActivity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

	    setListAdapter(new SearchResultListAdapter());
	    
	    // Get the intent, verify the action and get the query
	    Intent intent = getIntent();
	    if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
	      String query = intent.getStringExtra(SearchManager.QUERY);
	      searchWikiData(query);
	    }
	}

	private void searchWikiData(String query) {
		System.out.println("SEARCH " + query);
	}
	
	class SearchResultListAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return 3;
		}

		@Override
		public Object getItem(int position) {
			return position;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);

			TextView titleTextView = (TextView) view.findViewById(android.R.id.text1);

			titleTextView.setText("This is title #" + position);
			
			return view;
		}

	}
	

	/*
	private WikiData wikiData;
	private EditText searchText;
	private ListView searchResultList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        
        wikiData = new FileWikiData(this);
        
        searchText = (EditText) findViewById(R.id.searchText);
        searchResultList = (ListView) findViewById(R.id.searchResultList);
        
        searchText.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					updateResultList();
				}
				return false;
			}
		});
	}

	private void updateResultList() {
		final String text = searchText.getText().toString();
		
		new Thread() {
			public void run() {
				Collection<String> pages = wikiData.findPages(text);
			}
		}.start();
	}
	*/
}
