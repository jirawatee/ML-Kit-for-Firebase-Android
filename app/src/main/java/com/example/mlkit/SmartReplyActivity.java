package com.example.mlkit;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.naturallanguage.FirebaseNaturalLanguage;
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseSmartReply;
import com.google.firebase.ml.naturallanguage.smartreply.FirebaseTextMessage;
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestion;
import com.google.firebase.ml.naturallanguage.smartreply.SmartReplySuggestionResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SmartReplyActivity extends AppCompatActivity implements View.OnClickListener {
	private final String REMOTE_USER_ID = UUID.randomUUID().toString();
	private List<FirebaseTextMessage> chatHistory = new ArrayList<>();
	private EditText edt;
	private TextView txt;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_smart_reply);
		edt = findViewById(R.id.edt);
		txt = findViewById(R.id.txt);
		findViewById(R.id.btn).setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		txt.setText(null);
		String text = edt.getText().toString().trim();
		chatHistory.add(FirebaseTextMessage.createForRemoteUser(text, System.currentTimeMillis(), REMOTE_USER_ID));
		if (view.getId() == R.id.btn) {
			suggestReplyingMessages(chatHistory);
		}
	}

	private void suggestReplyingMessages(List<FirebaseTextMessage> chat) {
		FirebaseSmartReply smartReply = FirebaseNaturalLanguage.getInstance().getSmartReply();
		smartReply.suggestReplies(chat).addOnSuccessListener(new OnSuccessListener<SmartReplySuggestionResult>() {
			@Override
			public void onSuccess(SmartReplySuggestionResult result) {
				if (result.getStatus() == SmartReplySuggestionResult.STATUS_NOT_SUPPORTED_LANGUAGE) {
					txt.setText(getString(R.string.smart_reply_error));
				} else if (result.getStatus() == SmartReplySuggestionResult.STATUS_SUCCESS) {
					for (SmartReplySuggestion suggestion : result.getSuggestions()) {
						txt.append(suggestion.getText());
						txt.append("\n");
					}
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
