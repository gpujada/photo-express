package gp.fall2020.photoexpress;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LightingColorFilter;
import android.graphics.Paint;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private final int REQUEST_EXTERNAL_WRITE_PERMISSIONS = 0;
    private final int REQUEST_TAKE_PHOTO = 1;

    private String mPhotoPath;
    private ImageView mPhotoImageView;
    private SeekBar mSeekBar;
    private Button mSaveButton;
    private int mMultColor = 0xffffffff;
    private int mAddColor = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPhotoImageView = findViewById(R.id.photo);

        mSaveButton = findViewById(R.id.saveButton);
        mSaveButton.setEnabled(false);

        mSeekBar = findViewById(R.id.brightnessSeekBar);
        mSeekBar.setVisibility(View.INVISIBLE);

        mSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                changeBrightness(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
    }

    @SuppressLint("MissingSuperCall")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {
            displayPhoto();
            addPhotoToGallery();

            // Center seekbar at default lighting
            mSeekBar.setProgress(100);
            mSeekBar.setVisibility(View.VISIBLE);
            mSaveButton.setEnabled(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case REQUEST_EXTERNAL_WRITE_PERMISSIONS: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    takePhotoClick(null);
                }
                return;
            }
        }
    }

    private boolean hasExternalWritePermission() {
        // Get permission to write to external storage
        String permission = Manifest.permission.WRITE_EXTERNAL_STORAGE;
        if (ContextCompat.checkSelfPermission(this,
                permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { permission }, REQUEST_EXTERNAL_WRITE_PERMISSIONS);
            return false;
        }

        return true;
    }

    public void takePhotoClick(View view) {
        if (!hasExternalWritePermission()) return;

        // Create implicit intent
        Intent photoCaptureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (photoCaptureIntent.resolveActivity(getPackageManager()) != null) {

            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
                mPhotoPath = photoFile.getAbsolutePath();
            } catch (IOException ex) {
                // Error occurred while creating the File
                ex.printStackTrace();
            }

            // If the File was successfully created, start camera app
            if (photoFile != null) {

                // Create content URI to grant camera app write permission to photoFile
                Uri photoUri = FileProvider.getUriForFile(this,
                        "gp.fall2020.photoexpress.fileprovider",
                        photoFile);

                // Add content URI to intent
                photoCaptureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);

                // Start camera app
                startActivityForResult(photoCaptureIntent, REQUEST_TAKE_PHOTO);
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create a unique filename
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFilename = "photo_" + timeStamp + ".jpg";

        // Create the file in the Pictures directory on external storage
        File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = new File(storageDir, imageFilename);
        return image;
    }

    private void displayPhoto() {
        // Get ImageView dimensions
        int targetWidth = mPhotoImageView.getWidth();
        int targetHeight = mPhotoImageView.getHeight();

        // Get bitmap dimensions
        BitmapFactory.Options bitmapOptions = new BitmapFactory.Options();
        bitmapOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mPhotoPath, bitmapOptions);
        int photoWidth = bitmapOptions.outWidth;
        int photoHeight = bitmapOptions.outHeight;

        // Determine how much to scale down the image
        int scaleFactor = Math.min(photoWidth/targetWidth, photoHeight/targetHeight);

        // Decode the image file into a smaller bitmap that fills the ImageView
        bitmapOptions.inJustDecodeBounds = false;
        bitmapOptions.inSampleSize = scaleFactor;
        bitmapOptions.inPurgeable = true;
        Bitmap bitmap = BitmapFactory.decodeFile(mPhotoPath, bitmapOptions);

        // Display smaller bitmap
        mPhotoImageView.setImageBitmap(bitmap);
    }

    private void addPhotoToGallery() {
        // Send broadcast to Media Scanner about new image file
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        File file = new File(mPhotoPath);
        Uri fileUri = Uri.fromFile(file);
        mediaScanIntent.setData(fileUri);
        sendBroadcast(mediaScanIntent);
    }

    private void changeBrightness(int brightness) {
        // 100 is the middle value
        if (brightness > 100) {
            // Add color
            float addMult = brightness / 100.0f - 1;
            mAddColor = Color.argb(255, (int) (255 * addMult), (int) (255 * addMult), (int) (255 * addMult));
            mMultColor = 0xffffffff;
        }
        else {
            // Scale color down
            float brightMult = brightness / 100.0f;
            mMultColor = Color.argb(255, (int) (255 * brightMult), (int) (255 * brightMult), (int) (255 * brightMult));
            mAddColor = 0;
        }

        LightingColorFilter colorFilter = new LightingColorFilter(mMultColor, mAddColor);
        mPhotoImageView.setColorFilter(colorFilter);
    }

    private boolean saveAlteredPhoto() {
        // Read original image
        Bitmap bitmap = BitmapFactory.decodeFile(mPhotoPath, null);

        // Create a new bitmap with the same dimensions as the original
        Bitmap alteredBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), bitmap.getConfig());

        // Draw original bitmap on canvas and apply the color filter
        Canvas canvas = new Canvas(alteredBitmap);
        Paint paint = new Paint();
        LightingColorFilter colorFilter = new LightingColorFilter(mMultColor, mAddColor);
        paint.setColorFilter(colorFilter);
        canvas.drawBitmap(bitmap, 0, 0, paint);

        // Save altered bitmap over the original image
        File imageFile = new File(mPhotoPath);
        try {
            FileOutputStream outStream = new FileOutputStream(imageFile);
            alteredBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outStream);
            outStream.flush();
            outStream.close();
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void savePhotoClick(View view) {
        // Don't allow Save button to be pressed while image is saving
        mSaveButton.setEnabled(false);

        // Save the image in a background thread
        SaveBitmapTask saveTask = new SaveBitmapTask();
        saveTask.execute();
    }

    private class SaveBitmapTask extends AsyncTask<Void, Void, Boolean> {
        @Override
        protected Boolean doInBackground(Void... params) {
            return saveAlteredPhoto();
        }

        @Override
        protected void onPostExecute(Boolean result) {
            // Tell the user what happened
            if (result) {
                Toast.makeText(MainActivity.this, R.string.photo_saved, Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(MainActivity.this, R.string.photo_not_saved, Toast.LENGTH_LONG).show();
            }

            mSaveButton.setEnabled(true);
        }
    }
}