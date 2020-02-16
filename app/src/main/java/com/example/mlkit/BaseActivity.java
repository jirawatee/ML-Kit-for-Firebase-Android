package com.example.mlkit;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;

import com.example.mlkit.helpers.MyHelper;

import java.io.File;

public class BaseActivity extends AppCompatActivity {
	public static final int RC_STORAGE_PERMS1 = 101;
	public static final int RC_STORAGE_PERMS2 = 102;
	public static final int RC_SELECT_PICTURE = 103;
	public static final int RC_TAKE_PICTURE = 104;
	public static final String ACTION_BAR_TITLE = "action_bar_title";
	public File imageFile;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ActionBar actionBar = getSupportActionBar();
		if (actionBar != null) {
			actionBar.setDisplayHomeAsUpEnabled(true);
			actionBar.setTitle(getIntent().getStringExtra(ACTION_BAR_TITLE));
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_new, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_gallery:
				checkStoragePermission(RC_STORAGE_PERMS1);
				break;
			case R.id.action_camera:
				checkStoragePermission(RC_STORAGE_PERMS2);
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		switch (requestCode) {
			case RC_STORAGE_PERMS1:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					selectPicture();
				} else {
					MyHelper.needPermission(this, requestCode, R.string.confirm_storage);
				}
				break;
			case RC_STORAGE_PERMS2:
				if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
					openCamera();
				} else {
					MyHelper.needPermission(this, requestCode, R.string.confirm_camera);
				}
				break;
		}
	}

	public void checkStoragePermission(int requestCode) {
		switch (requestCode) {
			case RC_STORAGE_PERMS1:
				int hasWriteExternalStoragePermission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
				if (hasWriteExternalStoragePermission == PackageManager.PERMISSION_GRANTED) {
					selectPicture();
				} else {
					ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, requestCode);
				}
				break;
			case RC_STORAGE_PERMS2:
				String[] PERMISSIONS = { android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.CAMERA };
				if (!hasPermissions(this, PERMISSIONS)) {
					ActivityCompat.requestPermissions(this, PERMISSIONS, requestCode);
				} else {
					openCamera();
				}
				break;
		}
	}

	@Override
	public boolean onSupportNavigateUp() {
		finish();
		return super.onSupportNavigateUp();
	}

	public static boolean hasPermissions(Context context, String... permissions) {
		if (context != null && permissions != null) {
			for (String permission : permissions) {
				if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
					return false;
				}
			}
		}
		return true;
	}

	private void selectPicture() {
		imageFile = MyHelper.createTempFile(imageFile);
		Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
		startActivityForResult(intent, RC_SELECT_PICTURE);
	}

	private void openCamera() {
		imageFile = MyHelper.createTempFile(imageFile);
		Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
		Uri photo = FileProvider.getUriForFile(this, getPackageName() + ".provider", imageFile);
		intent.putExtra(MediaStore.EXTRA_OUTPUT, photo);
		startActivityForResult(intent, RC_TAKE_PICTURE);
	}
}