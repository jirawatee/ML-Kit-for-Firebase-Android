package com.example.mlkit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.mlkit.helpers.MyHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabel;
import com.google.firebase.ml.vision.cloud.label.FirebaseVisionCloudLabelDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.label.FirebaseVisionLabel;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetector;
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions;

import java.util.List;

public class ImageActivity extends BaseActivity implements View.OnClickListener {
	private Bitmap mBitmap;
	private ImageView mImageView;
	private TextView mTextView;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_cloud);

		mTextView = findViewById(R.id.text_view);
		mImageView = findViewById(R.id.image_view);
		findViewById(R.id.btn_device).setOnClickListener(this);
		findViewById(R.id.btn_cloud).setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		mTextView.setText(null);
		switch (view.getId()) {
			case R.id.btn_device:
				if (mBitmap != null) {
					FirebaseVisionLabelDetectorOptions options = new FirebaseVisionLabelDetectorOptions.Builder()
							.setConfidenceThreshold(0.7f)
							.build();
					FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mBitmap);
					FirebaseVisionLabelDetector detector = FirebaseVision.getInstance().getVisionLabelDetector(options);
					detector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionLabel>>() {
						@Override
						public void onSuccess(List<FirebaseVisionLabel> labels) {
							for (FirebaseVisionLabel label : labels) {
								mTextView.append(label.getLabel() + "\n");
								mTextView.append(label.getEntityId() + "\n");
								mTextView.append(label.getConfidence() + "\n\n");
							}
						}
					}).addOnFailureListener(new OnFailureListener() {
						@Override
						public void onFailure(@NonNull Exception e) {
							mTextView.setText(e.getMessage());
						}
					});
				}
				break;
			case R.id.btn_cloud:
				if (mBitmap != null) {
					MyHelper.showDialog(this);
					FirebaseVisionCloudDetectorOptions options = new FirebaseVisionCloudDetectorOptions.Builder()
									.setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
									.setMaxResults(5)
									.build();

					FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mBitmap);
					FirebaseVisionCloudLabelDetector detector = FirebaseVision.getInstance().getVisionCloudLabelDetector(options);
					detector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionCloudLabel>>() {
						@Override
						public void onSuccess(List<FirebaseVisionCloudLabel> labels) {
							MyHelper.dismissDialog();
							for (FirebaseVisionCloudLabel label : labels) {
								mTextView.append(label.getLabel() + ": " + label.getConfidence() + "\n\n");
								//mTextView.append(label.getEntityId() + "\n");
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
				break;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
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
						mBitmap = MyHelper.resizeImage(imageFile, this, dataUri, mImageView);
					} else {
						mBitmap = MyHelper.resizeImage(imageFile, path, mImageView);
					}
					if (mBitmap != null) {
						mTextView.setText(null);
						mImageView.setImageBitmap(mBitmap);
					}
					break;
				case RC_TAKE_PICTURE:
					mBitmap = MyHelper.resizeImage(imageFile, imageFile.getPath(), mImageView);
					if (mBitmap != null) {
						mTextView.setText(null);
						mImageView.setImageBitmap(mBitmap);
					}
					break;
			}
		}
	}
}
