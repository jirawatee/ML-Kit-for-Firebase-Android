package com.example.mlkit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.example.mlkit.helpers.MyHelper;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;

import java.util.List;

public class BarcodeActivity extends BaseActivity {
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
						barcodeDetector(bitmap);
					}
					break;
				case RC_TAKE_PICTURE:
					bitmap = MyHelper.resizeImage(imageFile, imageFile.getPath(), mImageView);
					if (bitmap != null) {
						mTextView.setText(null);
						mImageView.setImageBitmap(bitmap);
						barcodeDetector(bitmap);
					}
					break;
			}
		}
	}

	private void barcodeDetector(Bitmap bitmap) {
		FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(bitmap);
		/*
		FirebaseVisionBarcodeDetectorOptions options = new FirebaseVisionBarcodeDetectorOptions.Builder()
				.setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE, FirebaseVisionBarcode.FORMAT_AZTEC)
				.build();
		*/
		FirebaseVisionBarcodeDetector detector = FirebaseVision.getInstance().getVisionBarcodeDetector();
		detector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
			@Override
			public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {
				mTextView.setText(getInfoFromBarcode(firebaseVisionBarcodes));
			}
		}).addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				mTextView.setText(R.string.error_detect);
			}
		});
	}

	private String getInfoFromBarcode(List<FirebaseVisionBarcode> barcodes) {
		StringBuilder result = new StringBuilder();
		for (FirebaseVisionBarcode barcode : barcodes) {
			//int valueType = barcode.getValueType();
			result.append(barcode.getRawValue() + "\n");

			/*
			int valueType = barcode.getValueType();
			switch (valueType) {
				case FirebaseVisionBarcode.TYPE_WIFI:
					String ssid = barcode.getWifi().getSsid();
					String password = barcode.getWifi().getPassword();
					int type = barcode.getWifi().getEncryptionType();
					break;
				case FirebaseVisionBarcode.TYPE_URL:
					String title = barcode.getUrl().getTitle();
					String url = barcode.getUrl().getUrl();
					break;
			}
			*/
		}
		if ("".equals(result.toString())) {
			return getString(R.string.error_detect);
		} else {
			return result.toString();
		}
	}
}
