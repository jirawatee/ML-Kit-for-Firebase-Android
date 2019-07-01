package com.example.mlkit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mlkit.helpers.MyHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.face.FirebaseVisionFace;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetector;
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions;

import java.util.List;

public class FaceActivity extends BaseActivity {
	private ImageView mImageView;
	private TextView mTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device);
		mImageView = findViewById(R.id.image_view);
		mTextView = findViewById(R.id.text_view);
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
						detectFaces(bitmap);
					}
					break;
				case RC_TAKE_PICTURE:
					bitmap = MyHelper.resizeImage(imageFile, imageFile.getPath(), mImageView);
					if (bitmap != null) {
						mTextView.setText(null);
						mImageView.setImageBitmap(bitmap);
						detectFaces(bitmap);
					}
					break;
			}
		}
	}

	private void detectFaces(Bitmap bitmap) {
		FirebaseVisionFaceDetectorOptions options = new FirebaseVisionFaceDetectorOptions.Builder()
				.setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE)
				.setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS)
				.setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS)
				.build();
		FirebaseVisionFaceDetector detector = FirebaseVision.getInstance().getVisionFaceDetector(options);
		FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
		detector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionFace>>() {
			@Override
			public void onSuccess(List<FirebaseVisionFace> faces) {
				mTextView.setText(getInfoFromFaces(faces));
			}
		}).addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				mTextView.setText(R.string.error_detect);
			}
		});
	}

	private String getInfoFromFaces(List<FirebaseVisionFace> faces) {
		StringBuilder result = new StringBuilder();
		float smileProb = 0;
		float leftEyeOpenProb = 0;
		float rightEyeOpenProb = 0;
		for (FirebaseVisionFace face : faces) {
			
			// If landmark detection was enabled (mouth, ears, eyes, cheeks, and nose available):

			// If classification was enabled:
			if (face.getSmilingProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
				smileProb = face.getSmilingProbability();
			}
			if (face.getLeftEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
				leftEyeOpenProb = face.getLeftEyeOpenProbability();
			}
			if (face.getRightEyeOpenProbability() != FirebaseVisionFace.UNCOMPUTED_PROBABILITY) {
				rightEyeOpenProb = face.getRightEyeOpenProbability();
			}

			result.append("Smile: ");
			if (smileProb > 0.5) {
				result.append("Yes");
			} else {
				result.append("No");
			}
			result.append("\nLeft eye: ");
			if (leftEyeOpenProb > 0.5) {
				result.append("Open");
			} else {
				result.append("Close");
			}
			result.append("\nRight eye: ");
			if (rightEyeOpenProb > 0.5) {
				result.append("Open");
			} else {
				result.append("Close");
			}
			result.append("\n\n");
		}
		return result.toString();
	}
}