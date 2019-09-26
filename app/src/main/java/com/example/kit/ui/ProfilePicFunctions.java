package com.example.kit.ui;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.kit.R;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.File;
import java.util.HashMap;

public class ProfilePicFunctions extends AppCompatActivity {
    Intent GalIntent;
    Intent CamIntent;
    Intent CropIntent;
    File file;
    Uri uri;
    StorageReference ProfileImagesRef = FirebaseStorage.getInstance().getReference().child("profile images");

    private void getImageFromGallery(){
        // option #1:
        GalIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(Intent.createChooser(GalIntent, "Choose Image From Gallery"), 2);

        // option #2:
        CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).setAspectRatio(1,1).start(this);
        // option #2 from fragment:
        // CropImage.activity().setGuidelines(CropImageView.Guidelines.ON).setAspectRatio(1,1).start(getContext(), this);
    }

    private void getImageFromCamera(){
        CamIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        file = new File(Environment.getExternalStorageDirectory(), "file" + System.currentTimeMillis() + "jpg");
        uri = Uri.fromFile(file);
        CamIntent.putExtra(MediaStore.EXTRA_OUTPUT, uri);
        CamIntent.putExtra("return-data", true);
        startActivityForResult(CamIntent, 0);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0 && resultCode == RESULT_OK) {
            imageCropFunction();
        } else if (requestCode == 2){
            if (data != null) {
                uri = data.getData();
                imageCropFunction();
            }
        } else if (requestCode == 1) {
            Bundle bundle = data.getExtras();
            Bitmap bitmap = bundle.getParcelable("data");

            // set image i.e: imageview.setImageBitmap(bitmap)

        } else if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                Uri resultUri = result.getUri();

                // saves the image
                final StorageReference filePath = ProfileImagesRef.child(FirebaseAuth.getInstance().getUid() + ".jpg");
                filePath.putFile(resultUri).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                        if (task.isSuccessful()){
                            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    // sets the avatar
                                    HashMap<String, String> avatar = new HashMap<>();
                                    avatar.put("avatar", uri.toString());
                                    FirebaseFirestore.getInstance().collection(getString(R.string.collection_users)).document(FirebaseAuth.getInstance().getUid()).update("avatar", uri.toString());
                                }
                            });
                        }
                    }
                });
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
            }
        }
    }

    private void imageCropFunction(){
        // option #1:
        try {
            CropIntent = new Intent("com.android.action.CROP");
            CropIntent.setDataAndType(uri, "image/*");
            CropIntent.putExtra("crop", true);
            CropIntent.putExtra("outputX", 180);
            CropIntent.putExtra("outputY", 180);
            CropIntent.putExtra("aspectX", 4);
            CropIntent.putExtra("aspectY", 4);
            CropIntent.putExtra("scaleUpIfNeeded", true);
            CropIntent.putExtra("return-data", true);
            startActivityForResult(CropIntent, 1);

        }
        catch (ActivityNotFoundException e){

        }

        // option #2:
        CropImage.activity(uri).setAspectRatio(1, 1).start(this);
        // option #2 from fragment:
        // CropImage.activity(uri).setAspectRatio(1, 1).start(this);

    }
}
