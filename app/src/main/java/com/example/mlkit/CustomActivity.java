package com.example.mlkit;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.mlkit.helpers.MyHelper;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.ml.common.FirebaseMLException;
import com.google.firebase.ml.common.modeldownload.FirebaseModelDownloadConditions;
import com.google.firebase.ml.common.modeldownload.FirebaseModelManager;
import com.google.firebase.ml.custom.FirebaseCustomLocalModel;
import com.google.firebase.ml.custom.FirebaseCustomRemoteModel;
import com.google.firebase.ml.custom.FirebaseModelDataType;
import com.google.firebase.ml.custom.FirebaseModelInputOutputOptions;
import com.google.firebase.ml.custom.FirebaseModelInputs;
import com.google.firebase.ml.custom.FirebaseModelInterpreter;
import com.google.firebase.ml.custom.FirebaseModelInterpreterOptions;
import com.google.firebase.ml.custom.FirebaseModelOutputs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;

public class CustomActivity extends BaseActivity {
	private ImageView mImageView;
	private TextView mTextView;

	// Name of the model file hosted with Firebase
	private static final String HOSTED_MODEL_NAME = "mobilenet_v1_224_quant";
	private static final String LOCAL_MODEL_ASSET = "mobilenet_v1_1.0_224_quant.tflite";
	private FirebaseCustomLocalModel localModel;
	private FirebaseCustomRemoteModel remoteModel;

	// Name of the label file stored in Assets.
	private static final String LABEL_PATH = "labels.txt";

	private static final int RESULTS_TO_SHOW = 3;
	private static final int DIM_BATCH_SIZE = 1;
	private static final int DIM_PIXEL_SIZE = 3;
	private static final int DIM_IMG_SIZE_X = 224;
	private static final int DIM_IMG_SIZE_Y = 224;

	// Labels corresponding to the output of the vision model
	private List<String> mLabelList;

	private final PriorityQueue<Map.Entry<String, Float>> sortedLabels =
			new PriorityQueue<>(RESULTS_TO_SHOW, new Comparator<Map.Entry<String, Float>>() {
				@Override
				public int compare(Map.Entry<String, Float> o1, Map.Entry<String, Float> o2) {
					return (o1.getValue()).compareTo(o2.getValue());
				}
			});

	// Preallocated buffers for storing image data
	private final int[] intValues = new int[DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y];

	private FirebaseModelInterpreter mInterpreter;

	// Data configuration of input & output data of model.
	private FirebaseModelInputOutputOptions mDataOptions;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_device);
		mImageView = findViewById(R.id.image_view);
		mTextView = findViewById(R.id.text_view);

		mLabelList = loadLabelList(this);

		int[] inputDims = {DIM_BATCH_SIZE, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, DIM_PIXEL_SIZE};
		int[] outputDims = {DIM_BATCH_SIZE, mLabelList.size()};
		try {
			mDataOptions = new FirebaseModelInputOutputOptions.Builder()
					.setInputFormat(0, FirebaseModelDataType.BYTE, inputDims)
					.setOutputFormat(0, FirebaseModelDataType.BYTE, outputDims)
					.build();

			localModel = new FirebaseCustomLocalModel.Builder().setAssetFilePath(LOCAL_MODEL_ASSET).build();
			remoteModel = new FirebaseCustomRemoteModel.Builder(HOSTED_MODEL_NAME).build();

			FirebaseModelDownloadConditions.Builder conditionsBuilder = new FirebaseModelDownloadConditions.Builder().requireWifi();
			/*
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				// Enable advanced conditions on Android Nougat and newer.
				conditionsBuilder = conditionsBuilder.requireCharging().requireDeviceIdle();
			}
			*/
			FirebaseModelDownloadConditions conditions = conditionsBuilder.build();
			FirebaseModelManager.getInstance().download(remoteModel, conditions).addOnCompleteListener(new OnCompleteListener<Void>() {
				@Override
				public void onComplete(@NonNull Task<Void> task) {
					Toast.makeText(getApplicationContext(), "Remote model is ready", Toast.LENGTH_LONG).show();
				}
			});

			FirebaseModelManager.getInstance().isModelDownloaded(remoteModel).addOnSuccessListener(new OnSuccessListener<Boolean>() {
				@Override
				public void onSuccess(Boolean isDownloaded) {
					FirebaseModelInterpreterOptions options;
					if (isDownloaded) {
						options = new FirebaseModelInterpreterOptions.Builder(remoteModel).build();
					} else {
						options = new FirebaseModelInterpreterOptions.Builder(localModel).build();
					}
					try {
						mInterpreter = FirebaseModelInterpreter.getInstance(options);
					} catch (FirebaseMLException e) {
						e.printStackTrace();
					}
				}
			});
		} catch (FirebaseMLException e) {
			mTextView.setText(R.string.error_setup_model);
			e.printStackTrace();
		}
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
						runModelInference(bitmap);
					}
					break;
				case RC_TAKE_PICTURE:
					bitmap = MyHelper.resizeImage(imageFile, imageFile.getPath(), mImageView);
					if (bitmap != null) {
						mTextView.setText(null);
						mImageView.setImageBitmap(bitmap);
						runModelInference(bitmap);
					}
					break;
			}
		}
	}

	private void runModelInference(Bitmap bitmap) {
		if (mInterpreter == null) {
			mTextView.setText(R.string.error_image_init);
			return;
		}
		ByteBuffer imgData = convertBitmapToByteBuffer(bitmap);

		try {
			FirebaseModelInputs inputs = new FirebaseModelInputs.Builder().add(imgData).build();
			mInterpreter.run(inputs, mDataOptions).addOnFailureListener(new OnFailureListener() {
				@Override
				public void onFailure(@NonNull Exception e) {
					e.printStackTrace();
					mTextView.setText(R.string.error_run_model);
				}
			}).continueWith(new Continuation<FirebaseModelOutputs, List<String>>() {
				@Override
				public List<String> then(Task<FirebaseModelOutputs> task) {
					byte[][] labelProbArray = task.getResult().<byte[][]>getOutput(0);
					List<String> topLabels = getTopLabels(labelProbArray);
					for (String label : topLabels) {
						mTextView.append(label + "\n\n");
					}
					return topLabels;
				}
			});
		} catch (FirebaseMLException e) {
			e.printStackTrace();
			mTextView.setText(R.string.error_run_model);
		}
	}

	private synchronized List<String> getTopLabels(byte[][] labelProbArray) {
		for (int i = 0; i < mLabelList.size(); ++i) {
			sortedLabels.add(new AbstractMap.SimpleEntry<>(mLabelList.get(i), (labelProbArray[0][i] & 0xff) / 255.0f));
			if (sortedLabels.size() > RESULTS_TO_SHOW) {
				sortedLabels.poll();
			}
		}
		List<String> result = new ArrayList<>();
		final int size = sortedLabels.size();
		for (int i = 0; i < size; ++i) {
			Map.Entry<String, Float> label = sortedLabels.poll();
			result.add(label.getKey() + ": " + label.getValue());
		}
		return result;
	}

	private List<String> loadLabelList(Activity activity) {
		List<String> labelList = new ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(activity.getAssets().open(LABEL_PATH)))) {
			String line;
			while ((line = reader.readLine()) != null) {
				labelList.add(line);
			}
		} catch (IOException e) {
			mTextView.setText(R.string.error_read_label);
		}
		return labelList;
	}

	private synchronized ByteBuffer convertBitmapToByteBuffer(Bitmap bitmap) {
		ByteBuffer imgData = ByteBuffer.allocateDirect(DIM_BATCH_SIZE * DIM_IMG_SIZE_X * DIM_IMG_SIZE_Y * DIM_PIXEL_SIZE);
		imgData.order(ByteOrder.nativeOrder());
		Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, DIM_IMG_SIZE_X, DIM_IMG_SIZE_Y, true);
		imgData.rewind();
		scaledBitmap.getPixels(intValues, 0, scaledBitmap.getWidth(), 0, 0, scaledBitmap.getWidth(), scaledBitmap.getHeight());
		// Convert the image to int points.
		int pixel = 0;
		for (int i = 0; i < DIM_IMG_SIZE_X; ++i) {
			for (int j = 0; j < DIM_IMG_SIZE_Y; ++j) {
				final int val = intValues[pixel++];
				imgData.put((byte) ((val >> 16) & 0xFF));
				imgData.put((byte) ((val >> 8) & 0xFF));
				imgData.put((byte) (val & 0xFF));
			}
		}
		return imgData;
	}
}
