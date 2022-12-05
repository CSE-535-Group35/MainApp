package com.mc.MCProject;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;

public class MainActivity extends AppCompatActivity {

    private ImageButton uploadImageId;
    private ImageView imageViewId;
    private Bitmap image;
    ActivityResultLauncher<Intent> someActivityResultLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        uploadImageId = findViewById(R.id.imageButton);
        imageViewId = findViewById(R.id.imageView2);
        View uploadButtonId = findViewById(R.id.upload);

        someActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
                        image = (Bitmap)data.getExtras().get("data");
                        imageViewId.setImageBitmap(image);
                        uploadButtonId.setEnabled(true);
                    }
                });

        uploadImageId.setOnClickListener(v -> addImage());
        uploadButtonId.setOnClickListener(v -> uploadImage());
    }

    private void addImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        someActivityResultLauncher.launch(intent);
    }

    private void uploadImage() {
        //INference for full image
        /*
        ByteArrayOutputStream fullStream = new ByteArrayOutputStream();
        image.compress(Bitmap.CompressFormat.PNG, 50, fullStream);
        byte[] fullByteArray = fullStream.toByteArray();
        String imageData = Base64.encodeToString(fullByteArray, 0);
        getInference(imageData,0);
        */
        //Inference for Quadrant
        Bitmap[] imageQuadrants = new Bitmap[4];
        imageQuadrants[0] = Bitmap.createBitmap(
                image,
                0, 0,
                image.getWidth() / 2, image.getHeight() / 2
        );
        imageQuadrants[1] = Bitmap.createBitmap(
                image,
                image.getWidth() / 2, 0,
                image.getWidth() / 2, image.getHeight() / 2
        );
        imageQuadrants[2] = Bitmap.createBitmap(
                image,
                0, image.getHeight() / 2,
                image.getWidth() / 2, image.getHeight() / 2
        );
        imageQuadrants[3] = Bitmap.createBitmap(
                image,
                image.getWidth() / 2, image.getHeight() / 2,
                image.getWidth() / 2, image.getHeight() / 2
        );

        for (int i = 0;i < 4;i +=1){
            Bitmap imageQuadrant = imageQuadrants[i];
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageQuadrant.compress(Bitmap.CompressFormat.PNG, 50, stream);
            byte[] byteArray = stream.toByteArray();
            String imageData = Base64.encodeToString(byteArray, 0);
            getInference(imageData,i);

        }
    }

    private void getInference(String imageData,int i){
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = R.string.serverUrl+"/infer";
            JSONObject jsonBody = new JSONObject();
            //Quadrant Position
            jsonBody.put("position",i);
            jsonBody.put("imageData", imageData);

            JsonObjectRequest stringRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // Display the first 500 characters of the response string.
                            Log.i("TAG",response.toString());
                            String classification = "";
                            if (response.has("classification")) {
                                classification = response.optString("classification").toString();
                                saveImageInFile(classification);
                                //                                getSnackBar("Uploaded image to server in folder " + classification);
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            getSnackBar("Server error");
                        }
                    }
            );

            // Add the request to the RequestQueue.
            queue.add(stringRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void saveImageInFile(String classification){
        System.out.println("==================Called Save Image");
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED)
        {
            System.out.println("permissdbsdjfbsj");
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    990807079);
        }
        String MCPath = Environment.getExternalStorageDirectory()+"/Pictures/MCProject/";
        File MCProject = new File( MCPath);
        if (!MCProject.exists()) {
            System.out.println("creating mc");
            File wallpaperDirectory = new File(MCPath);
            System.out.println(wallpaperDirectory.mkdirs());
        }

        File classDir = new File(Environment.getExternalStorageDirectory() + "/"+classification);
        if (!classDir.exists()) {
            File wallpaperDirectory = new File(MCPath+classification+"/");
            wallpaperDirectory.mkdirs();
        }

        String fileName = ""+System.currentTimeMillis();

        File file = new File(MCPath+classification+"/", fileName+".jpeg");
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(file);
            image.compress(Bitmap.CompressFormat.JPEG, 100, out);
            out.flush();
            out.close();
            getSnackBar("Saved image in folder "+classification);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getSnackBar(String message) {
        View parentLayout = findViewById(android.R.id.content);
        Snackbar.make(parentLayout, message, Snackbar.LENGTH_LONG)
                .setAction("CLOSE", new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {

                    }
                })
                .show();
    }
}