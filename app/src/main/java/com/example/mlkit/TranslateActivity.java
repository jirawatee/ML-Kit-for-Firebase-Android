package com.example.mlkit;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslateLanguage;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslator;
import com.google.firebase.ml.naturallanguage.translate.FirebaseTranslatorOptions;

public class TranslateActivity extends AppCompatActivity implements View.OnClickListener {
	private EditText edt;
	private TextView txt;
	private FirebaseTranslator EnThTranslator;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_translate);
		edt = findViewById(R.id.edt);
		txt = findViewById(R.id.txt);
		findViewById(R.id.btn).setOnClickListener(this);

		FirebaseTranslatorOptions options = new FirebaseTranslatorOptions.Builder()
				.setSourceLanguage(FirebaseTranslateLanguage.TH)
				.setTargetLanguage(FirebaseTranslateLanguage.EN)
				.build();
		EnThTranslator = FirebaseNaturalLanguage.getInstance().getTranslator(options);

		FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder().requireWifi().build();
		EnThTranslator.downloadModelIfNeeded(conditions).addOnSuccessListener(new OnSuccessListener<Void>() {
			@Override
			public void onSuccess(Void v) {
				txt.setText(null);
				findViewById(R.id.btn).setEnabled(true);
			}
		}).addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				txt.setText(e.getMessage());
			}
		});
	}

	@Override
	public void onClick(View view) {
		txt.setText(null);
		String text = edt.getText().toString().trim();
		if (view.getId() == R.id.btn) {
			translate(text);
		}
	}

	private void translate(String msg) {
		Log.e("msg", msg);
		EnThTranslator.translate(msg).addOnSuccessListener(new OnSuccessListener<String>() {
			@Override
			public void onSuccess(String translatedText) {
				txt.setText(translatedText);
			}
		}).addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				txt.setText(e.getMessage());
			}
		});
	}
}
