package com.example.mlkit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.mlkit.helpers.MyHelper;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.automl.FirebaseAutoMLLocalModel;
import com.google.firebase.ml.vision.automl.FirebaseAutoMLRemoteModel;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionImageLabeler;
import com.google.firebase.ml.vision.label.FirebaseVisionOnDeviceAutoMLImageLabelerOptions;

import java.util.List;

public class AutoMLActivity extends BaseActivity {
	private ImageView mImageView;
	private TextView mTextView;
	private static final String REMOTE_MODEL_NAME = "LINE_FRIENDS";
	private FirebaseAutoMLLocalModel localModel;
	private FirebaseAutoMLRemoteModel remoteModel;
	private FirebaseVisionImageLabeler labeler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device);
		mImageView = findViewById(R.id.image_view);
		mTextView = findViewById(R.id.text_view);


		localModel = new FirebaseAutoMLLocalModel.Builder().setAssetFilePath("automl/manifest.json").build();
		remoteModel = new FirebaseAutoMLRemoteModel.Builder(REMOTE_MODEL_NAME).build();

		FirebaseModelDownloadConditions conditions = new FirebaseModelDownloadConditions.Builder().requireWifi().build();
		FirebaseModelManager.getInstance().download(remoteModel, conditions).addOnCompleteListener(new OnCompleteListener<Void>() {
			@Override
			public void onComplete(@NonNull Task<Void> task) {
				// Success.
			}
		});

		FirebaseModelManager.getInstance().isModelDownloaded(remoteModel).addOnSuccessListener(new OnSuccessListener<Boolean>() {
			@Override
			public void onSuccess(Boolean isDownloaded) {
				FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder optionsBuilder;
				if (isDownloaded) {
					optionsBuilder = new FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(remoteModel);
				} else {
					optionsBuilder = new FirebaseVisionOnDeviceAutoMLImageLabelerOptions.Builder(localModel);
				}
				FirebaseVisionOnDeviceAutoMLImageLabelerOptions options = optionsBuilder.setConfidenceThreshold(0.5f).build();
				try {
					labeler = FirebaseVision.getInstance().getOnDeviceAutoMLImageLabeler(options);
				} catch (FirebaseMLException e) {
					e.printStackTrace();
					mTextView.setTextColor(Color.RED);
					mTextView.setText(e.getMessage());
				}
			}
		});
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		Bitmap bitmap;
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
				case RC_STORAGE_PERMS1:
				case RC_STORAGE_PERMS2:
					checkStoragePermission(requestCode);
					break;
				case RC_SELECT_PICTURE:
					Uri dataUri = data.getData();
					String path = MyHelper.getPath(this, dataUri);
					if (path == null) {
						bitmap = MyHelper.resizeImage(imageFile, this, dataUri, mImageView);
					} else {
						bitmap = MyHelper.resizeImage(imageFile, path, mImageView);
					}
					if (bitmap != null) {
						mTextView.setText(null);
						mImageView.setImageBitmap(bitmap);
						runMyModel(bitmap);
					}
					break;
				case RC_TAKE_PICTURE:
					bitmap = MyHelper.resizeImage(imageFile, imageFile.getPath(), mImageView);
					if (bitmap != null) {
						mTextView.setText(null);
						mImageView.setImageBitmap(bitmap);
						runMyModel(bitmap);
					}
					break;
			}
		}
	}

	private void runMyModel(Bitmap bitmap) {
		FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
		labeler.processImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionImageLabel>>() {
			@Override
			public void onSuccess(List<FirebaseVisionImageLabel> labels) {
				extractLabel(labels);
			}
		}).addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				mTextView.setTextColor(Color.RED);
				mTextView.setText(e.getMessage());
			}
		});
	}

	private void extractLabel(List<FirebaseVisionImageLabel> labels) {
		for (FirebaseVisionImageLabel label : labels) {
			mTextView.append(label.getText() + "\n");
			mTextView.append(label.getConfidence() + "\n\n");
		}
	}
}
