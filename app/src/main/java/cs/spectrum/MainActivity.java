package cs.spectrum;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
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
import android.widget.Toast;
import android.util.Log;

import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static android.os.Environment.getExternalStoragePublicDirectory;


public class MainActivity extends AppCompatActivity {

    /* start touch variables */
    private static final String DEBUG_TAG = "SJL";

    private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    /* end touch variables  */

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

    private final String[] PERMISSIONS = {Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE};

    private final int PERMISSION_ALL = 1; //value after request granted


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        addTouchListener(); // stephen


        ImageButton cameraButton = (ImageButton) findViewById(R.id.cameraButton);
        ImageButton importButton = (ImageButton) findViewById(R.id.importButton);

        if (!hasPermissions(this, PERMISSIONS)){
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL);
        }

        getDisplaySize();

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

    private void getDisplaySize(){
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();

        display.getSize(size);

        DISPLAY_WIDTH = size.x;
        DISPLAY_HEIGHT = size.y;

        System.out.println("Display width x height: " + DISPLAY_WIDTH + " x " + DISPLAY_HEIGHT);
    }

    /* start touch functions */
    private void addTouchListener() {
        ImageView image = (ImageView)findViewById(R.id.primaryImage);

        image.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                float x = event.getX();
                float y = event.getY();


                if (event.getAction() == MotionEvent.ACTION_UP) { //only runs the following code when finger is lifted
                    try {

                        getBitmapSize(v);

                        //resizeImageView(v, BITMAP_IMAGE_VIEW_HEIGHT, BITMAP_IMAGE_VIEW_WIDTH);
                        /*
                           Something about this code is off but I cannot figure out what. The
                           returned colors are that of a pixel close to where the input is given.

                         */

                        System.out.println("ImageView Height: " + PRIMARY_IMAGE_VIEW_HEIGHT);
                        System.out.println("ImageView Width: " + PRIMARY_IMAGE_VIEW_WIDTH);

                        System.out.println("Bitmap Height: " + BITMAP_IMAGE_VIEW_HEIGHT);
                        System.out.println("Bitmap Width: " + BITMAP_IMAGE_VIEW_WIDTH);


                        float widthRatio = (float) BITMAP_IMAGE_VIEW_WIDTH / (float) PRIMARY_IMAGE_VIEW_WIDTH;
                        float heightRatio = (float) BITMAP_IMAGE_VIEW_HEIGHT / (float) PRIMARY_IMAGE_VIEW_HEIGHT;

                        int X_Image = (int) (x * widthRatio);
                        int Y_Image = (int) (y * heightRatio);

                        System.out.println("widthRatio: " + widthRatio);
                        System.out.println("heightRatio: " + heightRatio);

                        System.out.println("User pressed at: (" + Math.round(x) + ", " + Math.round(y) + ")");
                        System.out.println("SCALED COORDS: (" + X_Image + ", " + Y_Image + ")");

                        if (X_Image <= BITMAP_IMAGE_VIEW_WIDTH && Y_Image <= BITMAP_IMAGE_VIEW_HEIGHT ) {
                            getColorInfo(v, X_Image, Y_Image);
                        } else {
                            Toast.makeText(getApplicationContext(), "Touched outside of image.", Toast.LENGTH_SHORT).show();
                        }

                    } catch (Exception e) {
                        Toast.makeText(getApplicationContext(), String.format("Coordinates not on image: (%.0f, %.0f)", x, y), Toast.LENGTH_LONG).show();
                    }
                }
                return false;
            }
        });
    }

    public boolean onTouchEvent(MotionEvent e) {
        float x = e.getX();
        float y = e.getY();

        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                System.out.print("Pressed");
        }

        return true;
    }
    /* end touch functions */


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
            ImageView imgView = (ImageView) findViewById(R.id.primaryImage);
            // When an Image is picked from gallery
            if (requestCode == RESULT_LOAD_IMG && resultCode == RESULT_OK && null != data) {
                // Get the Image from data
                Uri selectedImage = data.getData();

                // Set the Image in ImageView after resizing if too large
                Picasso
                        .with(imgView.getContext())
                        .load(selectedImage)
                        .error(R.mipmap.ic_failed)
                        .resize(DISPLAY_WIDTH,0)
                        .onlyScaleDown()
                        .into(imgView);


                // When an image is taken from camera
            } else if (requestCode == RESULT_TAKE_PHOTO && resultCode == RESULT_OK){
                // Set the Image in ImageView after resizing if too large
                Picasso
                        .with(imgView.getContext())
                        .load(mCurrentPhotoUri)
                        .error(R.mipmap.ic_failed)
                        .rotate(90)
                        .resize(DISPLAY_WIDTH,0)
                        .onlyScaleDown()
                        .into(imgView);


            } else {
                Toast.makeText(this, "You haven't selected an image.",
                        Toast.LENGTH_LONG).show();
            }

            PRIMARY_IMAGE_VIEW_HEIGHT = imgView.getHeight();
            PRIMARY_IMAGE_VIEW_WIDTH = imgView.getWidth();

        } catch (Exception e) {
            System.out.println("******************************************************");
            e.printStackTrace();
            System.out.println("******************************************************");

            Toast.makeText(this, "Something went wrong.", Toast.LENGTH_LONG)
                    .show();
        }

    }

    private void getBitmapSize( View v ) {
        ImageView imageView = ((ImageView) v);
        Bitmap bitmap = ((BitmapDrawable) imageView.getDrawable()).getBitmap();

        BITMAP_IMAGE_VIEW_HEIGHT = bitmap.getHeight();
        BITMAP_IMAGE_VIEW_WIDTH = bitmap.getWidth();

        System.out.println("Bitmap Height: " + BITMAP_IMAGE_VIEW_HEIGHT);
        System.out.println("Bitmap Width: " + BITMAP_IMAGE_VIEW_WIDTH);

    }

    //causes more problems than it fixes
    private void resizeImageView(View v, int height, int width){
        v.requestLayout();

        v.getLayoutParams().height = height;
       // v.getLayoutParams().width = width;

        PRIMARY_IMAGE_VIEW_WIDTH = v.getWidth();
        PRIMARY_IMAGE_VIEW_HEIGHT = v.getHeight();
    }


    private void getColorInfo( View v, int x, int y){
        ImageView imageView = ((ImageView)v);
        Bitmap bitmap = ((BitmapDrawable)imageView.getDrawable()).getBitmap();

        System.out.println("BITMAP SIZE w x h: " + bitmap.getHeight() + " x " + bitmap.getWidth());

       try {
           int pixel = bitmap.getPixel(x, y);

           int red = Color.red(pixel);
           int green = Color.green(pixel);
           int blue = Color.blue(pixel);


           System.out.println("Pixel Color: R " + red + "  G " + green + "  B " + blue);
       } catch (Exception e){
           System.out.println("EXCEPTION IN GETCOLORINFO");
           e.printStackTrace();
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


    //currently unused from default activity
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //noinspection SimplifiableIfStatement
        if (id == R.id.action_Hide_Buttons) {
            ImageButton cameraButton = (ImageButton)findViewById(R.id.cameraButton);
            ImageButton importButton = (ImageButton)findViewById(R.id.importButton);

            cameraButton.setVisibility(View.GONE);
            importButton.setVisibility(View.GONE);
        } else if (id == R.id.action_Show_Buttons) {
            ImageButton cameraButton = (ImageButton)findViewById(R.id.cameraButton);
            ImageButton importButton = (ImageButton)findViewById(R.id.importButton);

            cameraButton.setVisibility(View.VISIBLE);
            importButton.setVisibility(View.VISIBLE);
        }

        return super.onOptionsItemSelected(item);
    }
}