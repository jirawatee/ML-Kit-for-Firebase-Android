package com.example.mlkit;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentification;
import com.google.firebase.ml.naturallanguage.languageid.FirebaseLanguageIdentificationOptions;
import com.google.firebase.ml.naturallanguage.languageid.IdentifiedLanguage;

import java.util.List;

public class LanguageActivity extends AppCompatActivity implements View.OnClickListener {
	private EditText edt;
	private TextView txt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_language);
		edt = findViewById(R.id.edt);
		txt = findViewById(R.id.txt);
		findViewById(R.id.btn1).setOnClickListener(this);
		findViewById(R.id.btn2).setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		txt.setText(null);
		String text = edt.getText().toString().trim();
		switch (view.getId()) {
			case R.id.btn1:
				identifyLanguage(text);
				break;
			case R.id.btn2:
				identifyAllLanguages(text);
				break;
		}
	}

	private void identifyLanguage(String text) {
		FirebaseLanguageIdentificationOptions options = new FirebaseLanguageIdentificationOptions.Builder().setConfidenceThreshold(0.5f).build();
		FirebaseLanguageIdentification languageIdentifier = FirebaseNaturalLanguage.getInstance().getLanguageIdentification(options);
		languageIdentifier.identifyLanguage(text).addOnSuccessListener(new OnSuccessListener<String>() {
			@Override
			public void onSuccess(@Nullable String languageCode) {
				if (!"und".equalsIgnoreCase(languageCode)) {
					txt.setText(getString(R.string.language_identification_result, languageCode));
				} else {
					txt.setText(getString(R.string.language_identification_error));
				}
			}
		}).addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				txt.setText(e.getMessage());
			}
		});
	}

	private void identifyAllLanguages(String text) {
		FirebaseLanguageIdentification languageIdentifier = FirebaseNaturalLanguage.getInstance().getLanguageIdentification();
		languageIdentifier.identifyPossibleLanguages(text).addOnSuccessListener(new OnSuccessListener<List<IdentifiedLanguage>>() {
			@Override
			public void onSuccess(List<IdentifiedLanguage> identifiedLanguages) {
				for (IdentifiedLanguage identifiedLanguage : identifiedLanguages) {
					txt.append(getString(R.string.language_identification_result, identifiedLanguage.getLanguageCode()));
					txt.append("(" + identifiedLanguage.getConfidence() + ")");
					txt.append("\n");
				}
			}
		}).addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				txt.setText(e.getMessage());
			}
		});
	}
}