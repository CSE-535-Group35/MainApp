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
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

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
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ImageButton uploadImageId;
    private ImageView imageViewId;
    private Bitmap image;
    private Bitmap[] imageQuadrants = null;
    private int[] responses = new int[4];
    private int responsesCount = 0;
    private TextView[] results;
    private List<String> edges = new ArrayList<String>();
    private View uploadButtonId;
    ActivityResultLauncher<Intent> someActivityResultLauncher;

    private boolean imageReady = false;
    private boolean serversReady = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final int threadCount = 50;
        for (int i = 0; i < 255; i += 255 / threadCount) {
            final int threadValue = i;
            Thread thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    String base = "10.1.1.";
                    for (int j = 0; j < 5; j++) {
                        tryServer(base + (threadValue + j), 5000);
                    }
                }
            });
            thread.start();
        }

        uploadImageId = findViewById(R.id.imageButton);
        imageViewId = findViewById(R.id.imageView2);
        uploadButtonId = findViewById(R.id.upload);

        someActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK) {
                    // There are no request codes
                    Intent data = result.getData();
                    image = (Bitmap)data.getExtras().get("data");
                    imageQuadrants = divideImage(image);
                    imageReady = true;
                    tryEnableUploadButton();
                }
            }
        );

        results = new TextView[4];
        results[0] = findViewById(R.id.results1);
        results[1] = findViewById(R.id.results2);
        results[2] = findViewById(R.id.results3);
        results[3] = findViewById(R.id.results4);

        uploadImageId.setOnClickListener(v -> addImage());
        uploadButtonId.setOnClickListener(v -> uploadImage());
    }

    private void tryServer(String ipAddress, int port) {
        Socket socket;
        try {
            socket = new Socket();
            socket.connect(new InetSocketAddress(ipAddress, port), 1000);
            socket.close();
            addEdge(ipAddress + ":" + port);
        } catch (Exception e) {}
    }

    private synchronized void addEdge(String ipAddress)  {
        edges.add(ipAddress);
        TextView text = findViewById(R.id.textView);
        if (edges.size() != 1) {
            text.setText("Consensus (" + edges.size() + " edges):");
        } else {
            text.setText("Consensus (" + edges.size() + " edge):");
        }

        tryEnableUploadButton();
    }

    private synchronized void tryEnableUploadButton() {
        if (edges.size() > 0 && imageReady) {
            uploadButtonId.setEnabled(true);
        } else {
            uploadButtonId.setEnabled(false);
        }
    }

    private void addImage() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        someActivityResultLauncher.launch(intent);
    }

    public Bitmap toGrayscale(Bitmap bmpOriginal) {
        int w, h;
        h = bmpOriginal.getHeight();
        w = bmpOriginal.getWidth();

        Bitmap bmpGrayscale = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmpGrayscale);
        Paint paint = new Paint();
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        ColorMatrixColorFilter f = new ColorMatrixColorFilter(cm);
        paint.setColorFilter(f);
        c.drawBitmap(bmpOriginal, 0, 0, paint);
        return bmpGrayscale;
    }

    private Bitmap convertInvert(Bitmap src) {
        Bitmap dst = Bitmap.createBitmap(src.getWidth(), src.getHeight(), src.getConfig());
        // color info
        int A, R, G, B;
        int pixelColor;
        // image size
        int h = src.getHeight();
        int w = src.getWidth();

        // scan through every pixel
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                // get one pixel
                pixelColor = src.getPixel(x, y);
                // saving alpha channel
                A = Color.alpha(pixelColor);
                R = 255 - Color.red(pixelColor);
                G = 255 - Color.green(pixelColor);
                B = 255 - Color.blue(pixelColor);
                dst.setPixel(x, y, Color.argb(A, R, G, B));
            }
        }

        // return output bitmap
        return dst;
    }

    private Bitmap thresholdImage(Bitmap src) {
        int h = src.getHeight();
        int w = src.getWidth();

        Bitmap dst = Bitmap.createBitmap(w, h, src.getConfig());

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixelColor = src.getPixel(x, y);

                int R = Color.red(pixelColor);
                int G = Color.green(pixelColor);
                int B = Color.blue(pixelColor);

                R = R < 100 ? 0 : R;
                G = G < 100 ? 0 : G;
                B = B < 100 ? 0 : B;

                dst.setPixel(x, y, Color.argb(255, R, G, B));
            }
        }

        return dst;
    }

    private Bitmap normalizeImage(Bitmap src) {
        int h = src.getHeight();
        int w = src.getWidth();

        Bitmap dst = Bitmap.createBitmap(w, h, src.getConfig());

        int highestR = 0;
        int highestG = 0;
        int highestB = 0;

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixelColor = src.getPixel(x, y);

                int R = Color.red(pixelColor);
                int G = Color.green(pixelColor);
                int B = Color.blue(pixelColor);

                if (R > highestR) {
                    highestR = R;
                }

                if (G > highestG) {
                    highestG = G;
                }

                if (B > highestB) {
                    highestB = B;
                }
            }
        }

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int pixelColor = src.getPixel(x, y);

                int R = (int)((float)Color.red(pixelColor) * (255.0 / (float)highestR));
                int G = (int)((float)Color.green(pixelColor) * (255.0 / (float)highestG));
                int B = (int)((float)Color.blue(pixelColor) * (255.0 / (float)highestB));

                dst.setPixel(x, y, Color.argb(255, R, G, B));
            }
        }

        return dst;
    }

    private Bitmap addBorder(Bitmap source, int borderSize) {
        int width = source.getWidth();
        int height = source.getHeight();

        int xBias = 0;
        int yBias = 0;
        if (width > height) {
            yBias = width - height;
        } else {
            xBias = height - width;
        }

        Bitmap borderedBitmap = Bitmap.createBitmap(
                source.getWidth() + borderSize * 2 + xBias,
                source.getHeight() + borderSize * 2 + yBias,
                source.getConfig()
        );
        Canvas canvas = new Canvas(borderedBitmap);
        canvas.drawColor(Color.BLACK);
        canvas.drawBitmap(source, borderSize + xBias / 2, borderSize + yBias / 2, null);
        return borderedBitmap;
    }

    private Bitmap[] divideImage(Bitmap imageOriginal) {
        Bitmap grayImage = toGrayscale(imageOriginal);
        Bitmap invertedImage = convertInvert(grayImage);
        Bitmap normalizedImage = normalizeImage(invertedImage);
        Bitmap thresholdedImage = thresholdImage(normalizedImage);
        int width = thresholdedImage.getWidth();
        int height = thresholdedImage.getHeight();

        // determine the smallest box around the number
        int minX = width + 1, maxX = 0;
        int minY = height + 1, maxY = 0;
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int pixelColor = thresholdedImage.getPixel(x, y);
                int color = Color.red(pixelColor);
                if (color > 50) {
                    minX = Math.min(x, minX);
                    maxX = Math.max(x, maxX);
                    minY = Math.min(y, minY);
                    maxY = Math.max(y, maxY);
                }
            }
        }

        Bitmap centered = addBorder(
                Bitmap.createBitmap(
                        thresholdedImage,
                        minX,
                        minY,
                        maxX - minX,
                        maxY - minY
                ),
                (int)(width * 0.1) // border is a ratio of the image size
        );

        Bitmap display = Bitmap.createScaledBitmap(centered, 28, 28, true);
        imageViewId.setImageBitmap(display);

        Bitmap[] imgs = new Bitmap[4];
        imgs[0] = Bitmap.createBitmap( // top left
                centered,
                0,
                0,
                centered.getWidth() / 2 ,
                centered.getHeight() / 2
        );
        imgs[1] = Bitmap.createBitmap( // bottom left
                centered,
                0,
                centered.getHeight() / 2,
                centered.getWidth() / 2,
                centered.getHeight() / 2
        );
        imgs[2] = Bitmap.createBitmap( // top right
                centered,
                centered.getWidth() / 2,
                0,
                centered.getWidth() / 2,
                centered.getHeight() / 2
        );
        imgs[3] = Bitmap.createBitmap( // bottom right
                centered,
                centered.getWidth() / 2,
                centered.getHeight() / 2,
                centered.getWidth() / 2,
                centered.getHeight() / 2
        );

        return imgs;
    }

    private void uploadImage() {
        responsesCount = 0;

        for (int i = 0; i < 4; i++) {
            results[i].setText("");
        }

        Collections.shuffle(edges);

        for (int i = 0; i < 4; i +=1) {
            Bitmap imageQuadrant = imageQuadrants[i];
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            imageQuadrant.compress(Bitmap.CompressFormat.PNG, 50, stream);
            byte[] byteArray = stream.toByteArray();
            String imageData = Base64.encodeToString(byteArray, 0);
            getInference(edges.get(i % edges.size()), imageData, i);
        }
    }

    private void getInference(String host, String imageData, int quadrant) {
        try {
            RequestQueue queue = Volley.newRequestQueue(this);
            String url = "http://" + host + "/infer";
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("imageData", imageData);

            JsonObjectRequest stringRequest = new JsonObjectRequest(
                    Request.Method.POST,
                    url,
                    jsonBody,
                    new Response.Listener<JSONObject>() {
                        @Override
                        public void onResponse(JSONObject response) {
                            // Display the first 500 characters of the response string.
                            Log.i("TAG", response.toString());
                            if (response.has("classification")) {
                                int classification = response.optInt("classification");
                                if (responsesCount < 4) {
                                    results[responsesCount].setText("Response " + (responsesCount + 1) + ": predicted " + classification);
                                }

                                responses[quadrant] = classification;
                                responsesCount++;
                                gotResponse();
                            }
                        }
                    },
                    new Response.ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError error) {
                            getSnackBar("Server error: " + error.toString());
                        }
                    }
            );

            // Add the request to the RequestQueue.
            queue.add(stringRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void gotResponse() {
        if (responsesCount >= 4) {
            HashMap<Integer, Integer> histogram = new HashMap<Integer, Integer>();
            for (int i = 0; i < 4; i++) {
                int total = 0;
                if (!histogram.containsKey(responses[i])) {
                    histogram.put(responses[i], 0);
                } else {
                    total = histogram.get(responses[i]);
                }

                histogram.put(responses[i], total + 1);
            }

            int bestNumber = 0;
            int total = 0;
            for (int i = 0; i < 10; i++) {
                if (histogram.containsKey(i) && histogram.get(i) > total) {
                    bestNumber = i;
                    total = histogram.get(i);
                }
            }

            saveImageInFile(Integer.valueOf(bestNumber).toString());
        }
    }

    private void saveImageInFile(String classification) {
        System.out.println("==================Called Save Image");
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(MainActivity.this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    990807079);
        }
        String MCPath = Environment.getExternalStorageDirectory()+"/Pictures/MCProject/";
        File MCProject = new File( MCPath);
        if (!MCProject.exists()) {
            File wallpaperDirectory = new File(MCPath);
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
            getSnackBar("Saved image in folder " + classification);
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
