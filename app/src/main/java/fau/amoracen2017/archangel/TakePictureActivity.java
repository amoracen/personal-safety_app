package fau.amoracen2017.archangel;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.graphics.Matrix;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;

import static android.os.Environment.getExternalStoragePublicDirectory;

/**
 * Activity TakePicture
 * @author Prince Abraham
 */
public class TakePictureActivity extends AppCompatActivity {

    private FirebaseAuth mFireBaseAuth;
    Button btnTakePic;
    File image;
    ImageView imageViewPic;
    String pathToFile;
    private StorageReference mStorageRef;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_take_picture);

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mFireBaseAuth = FirebaseAuth.getInstance();

        if(Build.VERSION.SDK_INT >= 23){
            requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 2);
        }

        btnTakePic = findViewById(R.id.takePic);
        imageViewPic = findViewById(R.id.showPic);
        btnTakePic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TakePicture();
            }
        });
    }



    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK){
            if(requestCode == 1){
                Bitmap bitmap = BitmapFactory.decodeFile(pathToFile);
                bitmap = imageOrientationValidator(bitmap,pathToFile);
                imageViewPic.setImageBitmap(bitmap);
                uploadFile();

                try {
                    FileOutputStream out = new FileOutputStream(pathToFile);
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
                    out.flush();
                    out.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }


                MediaScannerConnection.scanFile(this, new String[]{pathToFile}, null, new MediaScannerConnection.OnScanCompletedListener() {
                    @Override
                    public void onScanCompleted(String path, Uri uri) {
                        Log.i("ExternalStorage", "Scanned" + path + ":");
                        Log.i("ExternalStorage", "->uri=" + uri);
                    }
                });

            }
        }
    }

    private void TakePicture() {
        Intent takePic = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if(takePic.resolveActivity(getPackageManager()) != null){
            File file = null;
            file = createFile();
            if(file != null){
                pathToFile = file.getAbsolutePath();
                Uri uri = FileProvider.getUriForFile(TakePictureActivity.this, "com.fau.amoracen2017.fileprovider", file);
                takePic.putExtra(MediaStore.EXTRA_OUTPUT, uri);
                startActivityForResult(takePic, 1);
            }
        }
    }

    private File createFile() {
        String name = new SimpleDateFormat("yyyy-MM-dd'T'HHmmss").format(new Date());
        File fStorage = getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(name, ".jpeg", fStorage);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;
    }

    private void uploadFile(){

        Uri file = Uri.fromFile(new File(pathToFile));
       // myRef.child(Objects.requireNonNull(mFireBaseAuth.getUid())).setValue(contacts);
        StorageReference riversRef = mStorageRef.child(Objects.requireNonNull(mFireBaseAuth.getUid())).child("Images/"+file.getLastPathSegment());
        riversRef.putFile(file)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // Get a URL to the uploaded content
//                      //Uri downloadUrl = taskSnapshot.getDownloadUrl();
                        Toast toast=Toast.makeText(getApplicationContext(),"Uploaded",Toast.LENGTH_SHORT);
                        toast.show();
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(Exception exception) {
                        // Handle unsuccessful uploads
                        // ...
                    }
                });
    }
    private Bitmap imageOrientationValidator(Bitmap bitmap, String path) {

        ExifInterface ei;
        try {
            ei = new ExifInterface(path);
            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    bitmap = rotateImage(bitmap, 90);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    bitmap = rotateImage(bitmap, 180);
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    bitmap = rotateImage(bitmap, 270);
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    private Bitmap rotateImage(Bitmap source, float angle) {

        Bitmap bitmap = null;
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        try {
            bitmap = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
                    matrix, true);
        } catch (OutOfMemoryError err) {
            err.printStackTrace();
        }
        return bitmap;
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (mFireBaseAuth.getCurrentUser() == null) {
            Intent inToMain = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(inToMain);
            finish();
        }
    }

}//EOF CLASS