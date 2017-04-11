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
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;

import uk.co.senab.photoview.PhotoView;
import uk.co.senab.photoview.PhotoViewAttacher;
import uk.co.senab.photoview.PhotoViewAttacher.OnMatrixChangedListener;
import uk.co.senab.photoview.PhotoViewAttacher.OnPhotoTapListener;

import static android.graphics.Color.argb;
import static android.os.Environment.getExternalStoragePublicDirectory;

/**
 * There are several instances of 'dead' code within this activity. That being said, it is best to
 * leave them alone for now as it may break the application. A quick and dirty implementation of the
 * PhotoView library did not assess or provide solutions to conflicting code.
 *
 * TODO:
 *  1. Line 463    - Rework color averaging system
 *  2. Line 354-74 - Picasso should be looked at if possible. It seems fine after using the
 *                   PhotoView class *1
 *
 * Notes:
 * *1: https://github.com/chrisbanes/PhotoView/blob/10e13fd761084dcbceb5245b7b5bfb6903f11e5d/sample/src/main/java/uk/co/senab/photoview/sample/PicassoSampleActivity.java
 *
 * @author Stephen Lorenz
 * @author Travis Davey
 */

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
    private ColorLabels colors;

    static final String PHOTO_TAP_TOAST_STRING = "Photo Tap! X: %.2f %% Y:%.2f %% ID: %d";
    static final String SCALE_TOAST_STRING = "Scaled to: %.2ff";
    static final String FLING_LOG_STRING = "Fling velocityX: %.2f, velocityY: %.2f";

    private TextView mCurrMatrixTv;
    private PhotoViewAttacher mAttacher;
    private Toast mCurrentToast;
    private Matrix mCurrentDisplayMatrix = null;
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

        colors = new ColorLabels();

        PhotoView mImageView = (PhotoView) findViewById(R.id.primaryImage);
        mCurrMatrixTv = (TextView) findViewById(R.id.tv_current_matrix);

        // The MAGIC happens here!
        mAttacher = new PhotoViewAttacher(mImageView);

        mAttacher.setOnMatrixChangeListener(new MatrixChangeListener());
        mAttacher.setOnPhotoTapListener(new PhotoTapListener());
        mAttacher.setOnSingleFlingListener(new SingleFlingListener());

        ImageButton cameraButton = (ImageButton) findViewById(R.id.cameraButton);
        ImageButton importButton = (ImageButton) findViewById(R.id.importButton);

        //cameraButton listener
        cameraButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                dispatchTakePictureIntent();
                mAttacher.setScaleType(ScaleType.CENTER);
            }
        });

        //import button listener
        importButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick( View view ) {
                loadImageFromGallery( view );
            }

        });

        photoView = (PhotoView) findViewById(R.id.primaryImage);

        if (savedInstanceState != null) {
            System.out.println("LOADING URI" );

            mCurrentPhotoUri = savedInstanceState.getParcelable("imageUri");

            System.out.println("Reloading image.");

            Picasso
                    .with(photoView.getContext())
                    .load(mCurrentPhotoUri)
                    .error(R.mipmap.ic_failed)
                    .resize(DISPLAY_WIDTH,0)
                    .onlyScaleDown()
                    .into(photoView);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable("imageUri", mCurrentPhotoUri);

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mAttacher.cleanup();
    }

    private class PhotoTapListener implements OnPhotoTapListener {

        @Override
        public void onPhotoTap(View view, float x, float y) {
            float xPercentage = x * 100f;
            float yPercentage = y * 100f;

            showToast(String.format(PHOTO_TAP_TOAST_STRING, xPercentage, yPercentage, view == null ? 0 : view.getId()));
            getColorInfo(view, x, y);
        }

        @Override
        public void onOutsidePhotoTap() {
            showToast("Please tap within the image.");
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
        mAttacher.setScaleType(ScaleType.CENTER);
        mAttacher.setScaleType(ScaleType.FIT_CENTER);

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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    private boolean getColorInfo( View v, float x, float y) {
        Bitmap bitmap = ((BitmapDrawable) photoView.getDrawable()).getBitmap();

        /* debug */
        String resolution = "h:" + bitmap.getHeight() + " x:" + bitmap.getWidth();
        System.out.println(resolution);
        /* debug */

        //convert coordinates to integers
        int tap_location_x = (int)(bitmap.getWidth() * x);
        int tap_location_y = (int)(bitmap.getHeight()* y);

        /* debug */
        String tap_location = "x: " + tap_location_x + " y: " + tap_location_y;
        System.out.println(tap_location);
        /* debug */

        try {
            int pixel = bitmap.getPixel(tap_location_x, tap_location_y); //touched pixel

            int red = Color.red(pixel);
            int green = Color.green(pixel);
            int blue = Color.blue(pixel);

            //Rework color averaging system - Old code below

            //totals to be used for averaging
            int redTotal = 0;
            int greenTotal = 0;
            int blueTotal = 0;

            //square property setup for gathering pixel info
            int squareWidth = Math.round(DISPLAY_WIDTH * .025f); //square width is 5% of screen size
            int numPixels = Math.round((float)Math.pow(squareWidth, 2)); //number of pixels in square
            int offset = (int)Math.floor((float)squareWidth/2.0f);

            System.out.println("Square width: " + squareWidth);
            System.out.println("Number of pixels in square: " + numPixels);
            System.out.println("offset: " + offset);

            for (int i = tap_location_x - offset; i < tap_location_x + offset; i++) {
                for (int j = tap_location_y - offset; j < tap_location_y + offset; j++) {
                    //totalling the RGB values
                    redTotal += Color.red(bitmap.getPixel(i,j));
                    greenTotal += Color.green(bitmap.getPixel(i,j));
                    blueTotal += Color.blue(bitmap.getPixel(i,j));
                }
            }

            System.out.println("Red Total: " + redTotal + "   Green Total: " + greenTotal + "   Blue Total: " + blueTotal);

            //calculating average values
            int avgRed = redTotal/numPixels;
            int avgGreen = greenTotal/numPixels;
            int avgBlue = blueTotal/numPixels;

            System.out.println("Average Red: " + avgRed);
            System.out.println("Average Green: " + avgGreen);
            System.out.println("Average Blue: " + avgBlue);


            /* output color data to snackbar */
            colors.setRGB(avgRed, avgGreen, avgBlue);
            String color = "Average Color: " + colors.getColor() + "\nR: " + avgRed + " G: " + avgGreen + " B: " + avgBlue;


            final Snackbar alert = Snackbar.make(findViewById(R.id.primaryImage), color,
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
            /* output color data to snackbar */

            return true;

        } catch (Exception e) {
            Context context = getApplicationContext();
            String text = "It's dead, Jim. Unable to locate color data.";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();

            e.printStackTrace();

            return false;
        }
    }
}