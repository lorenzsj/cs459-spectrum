package cs.spectrum;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcel;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;
import android.widget.ImageView.ScaleType;

import com.google.android.gms.common.api.GoogleApiClient;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnMatrixChangedListener;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;
import static android.os.Environment.getExternalStoragePublicDirectory;


public class MainActivity extends AppCompatActivity {

    /* start touch variables */
    private static final String DEBUG_TAG = "SJL";
    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;


    private static final int RESULT_TAKE_PHOTO = 1;
    private static final int RESULT_LOAD_IMG = 2;
    private static final int RESIZED_IMG_WIDTH = 1080;

    //pixel width and height of device
    private static int DISPLAY_WIDTH;
    private static int DISPLAY_HEIGHT;

    //pixel width and height of primaryImage imageView
    private static int PRIMARY_IMAGE_VIEW_WIDTH;
    private static int PRIMARY_IMAGE_VIEW_HEIGHT;

    //pixel width and height of bitmap loaded into primaryImage
    private static int BITMAP_IMAGE_VIEW_WIDTH;
    private static int BITMAP_IMAGE_VIEW_HEIGHT;

    private Uri mCurrentPhotoUri = null;
    private PhotoView photoView;

    //value after request granted

    /////////////////////////////////

    static final String PHOTO_TAP_TOAST_STRING = "Photo Tap! X: %.2f %% Y:%.2f %% ID: %d";
    static final String SCALE_TOAST_STRING = "Scaled to: %.2ff";
    static final String FLING_LOG_STRING = "Fling velocityX: %.2f, velocityY: %.2f";

    private TextView mCurrMatrixTv;

    private PhotoViewAttacher mAttacher;

    private Toast mCurrentToast;

    private Matrix mCurrentDisplayMatrix = null;
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;
    private final String[] PERMISSIONS = {Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};
    private final int PERMISSION_ALL = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        getDisplaySize(); //BUG

        PhotoView mImageView = (PhotoView) findViewById(R.id.primaryImage);
        mCurrMatrixTv = (TextView) findViewById(R.id.tv_current_matrix);

        //Drawable bitmap = ContextCompat.getDrawable(this, R.drawable.wallpaper);
        //mImageView.setImageDrawable(bitmap);

        // The MAGIC happens here!
        mAttacher = new PhotoViewAttacher(mImageView);

        // Lets attach some listeners, not required though!
        mAttacher.setOnMatrixChangeListener(new MatrixChangeListener());
        mAttacher.setOnPhotoTapListener(new PhotoTapListener());
        mAttacher.setOnSingleFlingListener(new SingleFlingListener());
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        //client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();

        ImageButton cameraButton = (ImageButton) findViewById(R.id.cameraButton);
        ImageButton importButton = (ImageButton) findViewById(R.id.importButton);

        //cameraButton listener
        cameraButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                dispatchTakePictureIntent();
            }
        });

        //import button listener
        importButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                loadImageFromGallery( view );
            }

        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        // Need to call clean-up
        mAttacher.cleanup();
    }

    /* test 123 */
    private class PhotoTapListener implements OnPhotoTapListener {

        @Override
        public void onPhotoTap(View view, float x, float y) {
            float xPercentage = x * 100f;
            float yPercentage = y * 100f;

            showToast(String.format(PHOTO_TAP_TOAST_STRING, xPercentage, yPercentage, view == null ? 0 : view.getId()));
            //getColorInfo(view, );

            final Snackbar alert = Snackbar.make(findViewById(R.id.primaryImage), "color here",
                    Snackbar.LENGTH_INDEFINITE).setActionTextColor(Color.parseColor("#bbbbbb"));

            View snackBarView = alert.getView();
            snackBarView.setBackgroundColor(Color.parseColor("#313031"));

            alert.setAction("Dismiss", new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    alert.dismiss();
                }
            });

            alert.show();
        }

        @Override
        public void onOutsidePhotoTap() {
            showToast("You have a tap event on the place where out of the photo.");
        }
    }

    private void showToast(CharSequence text) {
        if (null != mCurrentToast) {
            mCurrentToast.cancel();
        }

        mCurrentToast = Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT);
        mCurrentToast.show();
    }

    private class MatrixChangeListener implements OnMatrixChangedListener {

        @Override
        public void onMatrixChanged(RectF rect) {
            mCurrMatrixTv.setText(rect.toString());
        }
    }

    private class SingleFlingListener implements PhotoViewAttacher.OnSingleFlingListener {

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            if (BuildConfig.DEBUG) {
                Log.d("PhotoView", String.format(FLING_LOG_STRING, velocityX, velocityY));
            }
            return true;
        }
    }
    /* test 123 */


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem zoomToggle = menu.findItem(R.id.menu_zoom_toggle);
        assert null != zoomToggle;
        zoomToggle.setTitle(mAttacher.canZoom() ? R.string.menu_zoom_disable : R.string.menu_zoom_enable);

        return super.onPrepareOptionsMenu(menu);
    }

    private void getDisplaySize(){ //BUG
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();

        display.getSize(size);

        DISPLAY_WIDTH = size.x;
        DISPLAY_HEIGHT = size.y;

        System.out.println("Display width x height: " + DISPLAY_WIDTH + " x " + DISPLAY_HEIGHT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        ImageButton cameraButton;
        ImageButton importButton;

        switch (item.getItemId()) {
            case R.id.action_Hide_Buttons:
                 cameraButton = (ImageButton)findViewById(R.id.cameraButton);
                 importButton = (ImageButton)findViewById(R.id.importButton);

                cameraButton.setVisibility(View.GONE);
                importButton.setVisibility(View.GONE);
                return true;

            case R.id.action_Show_Buttons:
                cameraButton = (ImageButton)findViewById(R.id.cameraButton);
                importButton = (ImageButton)findViewById(R.id.importButton);

                cameraButton.setVisibility(View.VISIBLE);
                importButton.setVisibility(View.VISIBLE);
                return true;

            case R.id.menu_zoom_toggle:
                mAttacher.setZoomable(!mAttacher.canZoom());
                return true;

            case R.id.menu_scale_fit_center:
                mAttacher.setScaleType(ScaleType.FIT_CENTER);
                return true;

            case R.id.menu_scale_fit_start:
                mAttacher.setScaleType(ScaleType.FIT_START);
                return true;

            case R.id.menu_scale_fit_end:
                mAttacher.setScaleType(ScaleType.FIT_END);
                return true;

            case R.id.menu_scale_fit_xy:
                mAttacher.setScaleType(ScaleType.FIT_XY);
                return true;

            case R.id.menu_scale_scale_center:
                mAttacher.setScaleType(ScaleType.CENTER);
                return true;

            case R.id.menu_scale_scale_center_crop:
                mAttacher.setScaleType(ScaleType.CENTER_CROP);
                return true;

            case R.id.menu_scale_scale_center_inside:
                mAttacher.setScaleType(ScaleType.CENTER_INSIDE);
                return true;

            case R.id.menu_scale_random_animate:
            case R.id.menu_scale_random:
                Random r = new Random();

                float minScale = mAttacher.getMinimumScale();
                float maxScale = mAttacher.getMaximumScale();
                float randomScale = minScale + (r.nextFloat() * (maxScale - minScale));
                mAttacher.setScale(randomScale, item.getItemId() == R.id.menu_scale_random_animate);

                showToast(String.format(SCALE_TOAST_STRING, randomScale));

                return true;

            /* it's dead, jim
            case R.id.menu_matrix_restore:
                if (mCurrentDisplayMatrix == null) {}
                    //showToast("You need to capture display matrix first");
                else
                    mAttacher.setDisplayMatrix(mCurrentDisplayMatrix);
                return true;


            case R.id.menu_matrix_capture:
                mCurrentDisplayMatrix = new Matrix();
                mAttacher.getDisplayMatrix(mCurrentDisplayMatrix);
                return true;
            case R.id.extract_visible_bitmap:
                try {
                    Bitmap bmp = mAttacher.getVisibleRectangleBitmap();
                    File tmpFile = File.createTempFile("photoview", ".png",
                            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS));
                    FileOutputStream out = new FileOutputStream(tmpFile);
                    bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
                    out.close();
                    Intent share = new Intent(Intent.ACTION_SEND);
                    share.setType("image/png");
                    share.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tmpFile));
                    startActivity(share);
                    Toast.makeText(this, String.format("Extracted into: %s", tmpFile.getAbsolutePath()), Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    t.printStackTrace();
                    Toast.makeText(this, "Error occured while extracting bitmap", Toast.LENGTH_SHORT).show();
                }
                return true;
            */
        }

        return super.onOptionsItemSelected(item);
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;

            try {
                photoFile = createImageFile();

            } catch (IOException ex) {
                // Error occurred while creating the File

            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "cs.spectrum.fileprovider",
                        photoFile);
                mCurrentPhotoUri = photoURI;
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, RESULT_TAKE_PHOTO);
                System.out.println("Picture taken.");

            }
        }
    }

    public void loadImageFromGallery(View view) {
        // Create intent to Open Image applications like Gallery, Google Photos
        Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        // Start the Intent
        startActivityForResult(galleryIntent, RESULT_LOAD_IMG);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        try {
            photoView = (PhotoView) findViewById(R.id.primaryImage);
            // When an Image is picked from gallery
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK && null != data) {
                // Get the Image from data

                Uri selectedImage = data.getData();

                // Set the Image in ImageView after resizing if too large
                Picasso
                        .with(photoView.getContext())
                        .load(selectedImage)
                        .error(R.mipmap.ic_failed)
                        .resize(DISPLAY_WIDTH,0)
                        .onlyScaleDown()
                        .into(photoView);

                mCurrentPhotoUri = selectedImage;

                // When an image is taken from camera
            } else if (requestCode == RESULT_TAKE_PHOTO && resultCode == RESULT_OK){
                // Set the Image in ImageView after resizing if too large
                Picasso
                        .with(photoView.getContext())
                        .load(mCurrentPhotoUri)
                        .error(R.mipmap.ic_failed)
                        .rotate(90)
                        .resize(DISPLAY_WIDTH,0)
                        .onlyScaleDown()
                        .into(photoView);
            } else {
                Toast.makeText(this, "You haven't selected an image.",
                        Toast.LENGTH_LONG).show();
            }

            PRIMARY_IMAGE_VIEW_HEIGHT = photoView.getHeight();
            PRIMARY_IMAGE_VIEW_WIDTH = photoView.getWidth();

        } catch (Exception e) {
            System.out.println("******************************************************");
            e.printStackTrace();
            System.out.println("******************************************************");

            Toast.makeText(this, "Something went wrong.", Toast.LENGTH_LONG)
                    .show();
        }
    }

    //returns a unique filename using a date-time stamp
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        return image;
    }


    //will check all permissions in PERMISSIONS String array to see if they have been granted
    public static boolean hasPermissions(Context context, String... permissions) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }

    //currently unused from default activity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
}