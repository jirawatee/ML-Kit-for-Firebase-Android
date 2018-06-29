package com.example.mlkit;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener {
	private String[] classNames;
	private static final Class<?>[] CLASSES = new Class<?>[]{
			TextActivity.class,
			BarcodeActivity.class,
			FaceActivity.class,
			ImageActivity.class,
			LandmarkActivity.class,
			CustomActivity.class,
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		classNames = getResources().getStringArray(R.array.class_name);

		ListView listView = findViewById(R.id.list_view);
		ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, classNames);
		listView.setAdapter(adapter);
		listView.setOnItemClickListener(this);
	}

	@Override
	public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
		Intent intent = new Intent(this, CLASSES[position]);
		intent.putExtra(BaseActivity.ACTION_BAR_TITLE, classNames[position]);
		startActivity(intent);
	}
}
