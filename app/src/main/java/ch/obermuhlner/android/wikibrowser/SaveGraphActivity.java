package ch.obermuhlner.android.wikibrowser;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import ch.obermuhlner.android.wikibrowser.trial.R;

public class SaveGraphActivity extends Activity {

	public static final String EXTRA_GRAPH_NAME = "graphName";

	@Override
	public void onCreate(Bundle savedInstanceState) {
	    super.onCreate(savedInstanceState);
	    
        setContentView(R.layout.save_graph);

        final EditText nameEditText = (EditText) findViewById(R.id.saveGraphName);
        final ImageView graphImageView = (ImageView) findViewById(R.id.saveGraphImage);
        final Button saveButton = (Button) findViewById(R.id.SaveGraphOkButton);
        
        String graphName = getIntent().getExtras().getString(EXTRA_GRAPH_NAME);
        nameEditText.setText(graphName);
        
        saveButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				Intent intent = new Intent();
				intent.putExtra(EXTRA_GRAPH_NAME, nameEditText.getText().toString());
				setResult(RESULT_OK, intent);
				finish();
			}
        });
	}
}
