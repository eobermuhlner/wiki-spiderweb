package ch.obermuhlner.android.wikibrowser;

import java.util.List;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import ch.obermuhlner.android.wikibrowser.GraphStorage.StoredGraph;

public class ManageGraphActivity extends ListActivity {

	public static final String EXTRA_GRAPH_NAME = "graphName";

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);

	    setListAdapter(new StoredGraphListAdapter(GraphStorage.getStoredGraphs(this)));
	}
	
	@Override
	protected void onListItemClick(ListView listView, View parentView, int position, long id) {
		Intent intent = new Intent();
		StoredGraph storedGraph = (StoredGraph) listView.getItemAtPosition(position);
		intent.putExtra(EXTRA_GRAPH_NAME, storedGraph.name);
		setResult(RESULT_OK, intent);
		finish();
	}
	
	class StoredGraphListAdapter extends BaseAdapter {

		private final List<StoredGraph> storedGraphs;

		public StoredGraphListAdapter(List<StoredGraph> storedGraphs) {
			this.storedGraphs = storedGraphs;
		}

		@Override
		public int getCount() {
			return storedGraphs.size();
		}

		@Override
		public Object getItem(int position) {
			return storedGraphs.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View view = getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false);

			TextView titleTextView = (TextView) view.findViewById(android.R.id.text1);

			StoredGraph storedGraph = storedGraphs.get(position);
			titleTextView.setText(storedGraph.name);
			
			return view;
		}
		
	}
	
}
