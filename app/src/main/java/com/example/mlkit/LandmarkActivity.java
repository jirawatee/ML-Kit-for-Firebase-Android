package com.example.mlkit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mlkit.helpers.MyHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.cloud.landmark.FirebaseVisionCloudLandmark;
import com.google.firebase.ml.vision.cloud.landmark.FirebaseVisionCloudLandmarkDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.common.FirebaseVisionLatLng;

import java.util.List;

public class LandmarkActivity extends BaseActivity {
	private ImageView mImageView;
	private TextView mTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device);
		mTextView = findViewById(R.id.text_view);
		mImageView = findViewById(R.id.image_view);
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
						landmarkDectector(bitmap);
					}
					break;
				case RC_TAKE_PICTURE:
					bitmap = MyHelper.resizeImage(imageFile, imageFile.getPath(), mImageView);
					if (bitmap != null) {
						mTextView.setText(null);
						mImageView.setImageBitmap(bitmap);
						landmarkDectector(bitmap);
					}
					break;
			}
		}
	}

	private void landmarkDectector(Bitmap bitmap) {
		MyHelper.showDialog(this);
		FirebaseVisionCloudDetectorOptions options = new FirebaseVisionCloudDetectorOptions.Builder()
				.setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
				.setMaxResults(5)
				.build();

		FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
		FirebaseVisionCloudLandmarkDetector detector = FirebaseVision.getInstance().getVisionCloudLandmarkDetector(options);

		detector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionCloudLandmark>>() {
			@Override
			public void onSuccess(List<FirebaseVisionCloudLandmark> firebaseVisionCloudLandmarks) {
				MyHelper.dismissDialog();

				StringBuilder result = new StringBuilder();
				for (FirebaseVisionCloudLandmark landmark : firebaseVisionCloudLandmarks) {
					String landmarkName = landmark.getLandmark();
					float confidence = landmark.getConfidence();
					result.append("Landmark: " + landmarkName + "\n");
					result.append("Confidence: " + confidence + "\n");
					for (FirebaseVisionLatLng loc: landmark.getLocations()) {
						result.append("Location: " + loc.getLatitude() + "," + loc.getLongitude() + "\n");
					}
					result.append("\n");
				}
				if ("".equals(result.toString())) {
					mTextView.setText(R.string.error_detect);
				} else {
					mTextView.setText(result.toString());
				}
			}
		}).addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				MyHelper.dismissDialog();
				mTextView.setText(e.getMessage());
			}
		});
	}
}
