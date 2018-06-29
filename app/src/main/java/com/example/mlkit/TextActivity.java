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
import com.google.firebase.ml.vision.cloud.text.FirebaseVisionCloudDocumentTextDetector;
import com.google.firebase.ml.vision.cloud.text.FirebaseVisionCloudText;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextDetector;

import java.util.List;

public class TextActivity extends BaseActivity implements View.OnClickListener {
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
		switch (view.getId()) {
			case R.id.btn_device:
				if (mBitmap != null) {
					runTextRecognition();
				}
				break;
			case R.id.btn_cloud:
				if (mBitmap != null) {
					runCloudTextRecognition();
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

	private void runTextRecognition() {
		FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mBitmap);
		FirebaseVisionTextDetector detector = FirebaseVision.getInstance().getVisionTextDetector();
		detector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
			@Override
			public void onSuccess(FirebaseVisionText texts) {
				processTextRecognitionResult(texts);
			}
		}).addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				e.printStackTrace();
			}
		});
	}

	private void processTextRecognitionResult(FirebaseVisionText texts) {
		mTextView.setText(null);
		List<FirebaseVisionText.Block> blocks = texts.getBlocks();
		if (blocks.size() == 0) {
			mTextView.setText(R.string.error_not_found);
			return;
		}
		for (int i = 0; i < blocks.size(); i++) {
			List<FirebaseVisionText.Line> lines = blocks.get(i).getLines();
			for (int j = 0; j < lines.size(); j++) {
				List<FirebaseVisionText.Element> elements = lines.get(j).getElements();
				for (int k = 0; k < elements.size(); k++) {
					mTextView.append(elements.get(k).getText() + " ");
				}
			}
		}
	}

	private void runCloudTextRecognition() {
		MyHelper.showDialog(this);
		FirebaseVisionCloudDetectorOptions options = new FirebaseVisionCloudDetectorOptions.Builder()
				.setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
				.setMaxResults(15)
				.build();
		FirebaseVisionImage image = FirebaseVisionImage.fromBitmap(mBitmap);
		FirebaseVisionCloudDocumentTextDetector detector = FirebaseVision.getInstance().getVisionCloudDocumentTextDetector(options);
		detector.detectInImage(image).addOnSuccessListener(new OnSuccessListener<FirebaseVisionCloudText>() {
			@Override
			public void onSuccess(FirebaseVisionCloudText texts) {
				MyHelper.dismissDialog();
				processCloudTextRecognitionResult(texts);
			}
		}).addOnFailureListener(new OnFailureListener() {
			@Override
			public void onFailure(@NonNull Exception e) {
				MyHelper.dismissDialog();
				e.printStackTrace();
			}
		});
	}

	private void processCloudTextRecognitionResult(FirebaseVisionCloudText text) {
		mTextView.setText(null);
		if (text == null) {
			mTextView.setText(R.string.error_not_found);
			return;
		}
		StringBuilder sentenceStr = new StringBuilder();
		List<FirebaseVisionCloudText.Page> pages = text.getPages();
		for (int i = 0; i < pages.size(); i++) {
			FirebaseVisionCloudText.Page page = pages.get(i);
			List<FirebaseVisionCloudText.Block> blocks = page.getBlocks();
			for (int j = 0; j < blocks.size(); j++) {
				List<FirebaseVisionCloudText.Paragraph> paragraphs = blocks.get(j).getParagraphs();
				for (int k = 0; k < paragraphs.size(); k++) {
					FirebaseVisionCloudText.Paragraph paragraph = paragraphs.get(k);
					List<FirebaseVisionCloudText.Word> words = paragraph.getWords();
					for (int l = 0; l < words.size(); l++) {
						List<FirebaseVisionCloudText.Symbol> symbols = words.get(l).getSymbols();

						StringBuilder wordStr = new StringBuilder();
						for (int m = 0; m < symbols.size(); m++) {
							wordStr.append(symbols.get(m).getText());
						}
						//mTextView.append(wordStr);
						//mTextView.append(": " + words.get(l).getConfidence());
						//mTextView.append("\n");

						sentenceStr.append(wordStr).append(" ");
					}
				}
			}
		}
		mTextView.append("\n" + sentenceStr);
	}
}
