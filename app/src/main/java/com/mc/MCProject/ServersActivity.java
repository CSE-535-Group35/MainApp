package com.mc.MCProject;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.snackbar.Snackbar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Arrays;

public class ServersActivity extends AppCompatActivity {
    private Bitmap image;
    private double[][] arr = new double[40][1];
    @Override
    public void onBackPressed() {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.server_ip);

        View uploadButtonId = findViewById(R.id.upload);
        View classifyButtonId = findViewById(R.id.classify);

        uploadButtonId.setOnClickListener(v -> uploadImage());

        classifyButtonId.setOnClickListener(v -> classify());
//        classifyButtonId.setOnClickListener(v -> maxPrediction());
//        classifyButtonId.setOnClickListener(v -> maxSum());
    }

    private void uploadImage() {
        Bundle extras = getIntent().getExtras();

        //The key argument here must match that used in the other activity
        if (extras != null) {
            image = (Bitmap) extras.get("imageData");
        }
        String server1 = ((EditText)findViewById(R.id.server1)).getText().toString();
        String server2 = ((EditText)findViewById(R.id.server2)).getText().toString();
        String server3 = ((EditText)findViewById(R.id.server3)).getText().toString();
        String server4 = ((EditText)findViewById(R.id.server4)).getText().toString();
        String[] serverArr = {server1,server2,server3,server4};
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

            try {
                RequestQueue queue = Volley.newRequestQueue(this);
//                String url = getString(R.string.serverUrl)+"/infer";
                String url = "http://"+serverArr[i]+"/infer";
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
                                int position = (int) response.opt("position");
                                if(response.has("predictions")){
//                                    JsonA
                                    JSONArray currentJson = (JSONArray) response.opt("predictions");
//                                    double[][] current = new double[[1];

                                    for(int i = 0; i < currentJson.length(); i++){

                                        JSONArray innerArray = null;
                                        try {
                                            innerArray = currentJson.getJSONArray(i);
                                        } catch (JSONException e) {
                                            e.printStackTrace();
                                        }

                                        for(int j = 0; j < innerArray.length(); j++){
                                            try {
                                                arr[(position*10)+j][0] = (double) innerArray.getDouble(j);
                                                Log.d(String.valueOf(innerArray.getDouble(j)),"innerArray.getDouble(j)");
                                                Log.d(String.valueOf(arr[(position*10)+j][0]),"arr[(position*10)+j][0] ");
                                            } catch (JSONException e) {
                                                e.printStackTrace();
                                            }
                                        }
                                    }
                                }
                                if (response.has("classification")) {
                                    classification = response.optString("classification").toString();
//                                    saveImageInFile(classification);
                                    //                                getSnackBar("Uploaded image to server in folder " + classification);
                                }
                                getSnackBar(position+" received");
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

    }

    private void maxSum(){
        int finalResult = -1;
        double finalMax = 0;
        double[] freq = new double[10];
        for(int i=0;i<4;i++) {
            for (int index = i*10; index < (i*10)+10; index++) {
                freq[(index-(i*10))] += arr[index][0];
            }
        }
        Log.d("freq",Arrays.toString(freq));
        for(int i=0;i<10;i++){
            if(freq[i]>finalMax){
                finalMax = freq[i];
                finalResult = i;
            }
        }
        saveImageInFile(String.valueOf(finalResult));
    }

    private void maxPrediction(){
        int finalResult = -1;
        int finalMax = 0;
        int[] freq = new int[10];
        for(int i=0;i<4;i++) {
            int resultIndex = 0;
            double max = arr[0][0];
            for (int index = i*10; index < (i*10)+10; index++) {
                if (max < arr[index][0]) {
                    max = arr[index][0];
                    resultIndex = index;
                }
            }
            freq[(resultIndex-(i*10))]++;
        }
        Log.d("freq",Arrays.toString(freq));
        for(int i=0;i<10;i++){
            if(freq[i]>finalMax){
                finalMax = freq[i];
                finalResult = i;
            }
        }
        saveImageInFile(String.valueOf(finalResult));
    }

    private void classify(){
        saveImageInFile(String.valueOf(multiplyMatrix(arr)));
    }

    private int multiplyMatrix(double B[][])
    {
        double[][] A = {
                {
                        0.5753950296172932,-0.1010770031427106,-0.5464681693574877,-0.4049457587277583,0.32698113234056636,0.16967750888294955,0.30637552054272943,-0.12200783525582383,-0.10541797518600032,-0.10171563386413561,2.209565349474927,0.14347009914658848,0.9290314042949976,-0.1744770026175942,-0.4115792746211278,-0.9981436596365284,-2.2681614896143203,0.30285572755510165,-0.09019075405898096,0.354434370812046,2.254138907560464,-0.043550731053827806,-0.5251145818338645,-0.3849254173330275,-0.7465008145053869,-0.11820510875613581,1.3679770215205733,-1.1840085160476852,-0.01761442944661478,-0.6054018809309536,0.9282315197220748,-0.7070456892062178,-0.41092674207356195,0.4619050211019431,0.007195219443527231,0.21021525395664165,0.008031242864817613,0.41640080794020623,-0.5417958720207589,-0.3754095282583289
                },
                {
                        0.08024684178231692,0.47462905922795023,-0.11461528769399706,-0.3149036601053363,0.04492750098535925,0.1638636382517655,0.2982886644175254,-0.2955111500423093,-0.5061460846507069,0.15994681625405094,-1.2095254535700353,2.1803444867659794,-0.048416878125270964,0.009745748131098873,0.46436070101686067,-0.3657542338659558,-0.21598186154780727,-0.31591809406455784,-0.14072578609306705,-0.36740145314521117,0.34342837717996305,1.5848027323985092,0.3258588539571832,-0.06953916568810012,-0.8661661987794771,-0.09994349269900842,-0.3885332060221542,0.016262034528257082,-0.028863620334792096,-0.8265762888625169,-0.09201664911238414,1.4715511763631903,-0.7466606157692909,-0.04019196856708296,-0.33543198223584175,0.05116167612480702,-0.3633076537045164,-0.18223276655862825,-0.3201045128631327,0.5479583394824452
                },
                {
                        -0.628814745301174,-0.21514329633702495,0.7961846146273334,0.8405565794349182,-0.42223638319177925,0.13977343473878592,-0.27096014273821495,0.16955670204484394,-0.24404720004932792,-0.1725617619154553,0.48161801228869744,0.2977590124972986,0.7509422268994728,-0.23666850183526678,-0.09601040803928947,0.15797984780824395,-0.9304661177745223,-0.19229110795636667,-0.2965516567304087,0.055995328539933206,-0.7455871056829613,0.13622973721655757,1.9101112418179604,-0.6512677704107956,-0.5720202874500571,-0.048894789420787566,0.48092232604405344,-0.6980986499426194,0.7487395005335811,-0.5678224283119584,0.14207988281170322,-0.8580956874134416,2.646693493427478,-0.9223264418069682,0.45287789826429925,-0.7528841282359127,0.09382345734926999,0.06897437059267787,-1.0164963503550106,0.13766237763036782
                },
                {
                        -0.39108930227957334,0.4419307176006959,0.9811705838199488,1.3883354882560228,-0.7664123692127396,-0.8967007474871249,0.025190781907946092,1.0425963722999627,-1.0132382213314322,-0.8127554544479682,-0.30001610134587725,-0.07602416159518037,0.7347713044919449,1.6421399472784495,0.6325368027010947,-2.1903239693403944,-0.6270897034543365,-0.7968342138743901,0.32448050618360236,0.6553855469564385,-0.5841166130373525,-0.25645162319607784,-0.269633373186372,2.2179172287762112,0.07130352186557751,0.8330533894694417,-0.5042731595349255,-0.014419387288589077,-1.3900804266738327,-0.1042802554083558,0.597047628891708,-0.7357881054481975,-1.9700141119392756,1.1992779305324686,-0.5772202522689502,0.7977394939035855,0.5604677705748429,-0.9837786518231615,1.4375847918901443,-0.3262931572217013
                },
                {
                        0.6444940581422889,-1.2692318450107887,-0.6906998040269462,-0.29775374586370523,0.9659103584117193,0.8659862521359881,0.6050446345022052,-0.6465673528777117,0.3725528715175705,-0.5476475920047945,-0.19639433285744148,1.3640969528078115,-0.050894825157130306,0.43430627932171995,2.4916995587226674,-1.0487001158784044,-0.1210003476183128,-0.6661764410482018,-0.5132670998532889,-1.6915809910350332,-0.3634163594017213,-0.7577334626501695,-0.9236373105100899,-0.15579498840100148,1.4949019729309114,0.12674326708720277,-0.12622846400192983,-0.10567497457939386,0.07906844467510217,0.7338527897939976,-0.6290549363555883,0.02291431830209717,0.7806398851254439,-0.6902730251693983,1.2783734848773758,0.06502192996816727,-0.9758828281355996,0.7487437081434356,-0.9121163710657183,0.3137230754151
                },
                {
                        -0.3724910214496493,0.3350738371999567,0.14838729551965865,-1.2622188948823514,0.20780858601107735,1.0400668157984485,0.2495855547811705,-0.40497870729379754,0.14841503038110382,-0.0679012274424368,-0.010933456435398423,-0.880900158499787,-0.32537683119783706,-0.9172439668679547,-0.8506783853089428,3.421581660216853,0.8837203644568096,-0.12904553616468278,-0.5614405928221993,-0.6079330378714053,-0.7924672686864088,-0.05238260516806266,-0.2380993133084265,0.7788873016679493,0.6023098872981762,1.4908721349555303,-1.9286563063214048,0.3593069526830669,-0.5521572324073127,0.3541344128702492,0.2866092218004217,0.021045123951499174,0.06147539563378219,0.3613875492322367,-0.5777560139365889,0.7866417816546312,-0.004788525873365873,-0.5408506845532751,0.10747915278453485,-0.4794938850006527
                },
                {
                        0.1854013815167121,0.8520358813717862,-0.10613930111295991,0.2829995520323282,0.08059233534471505,-0.30326160873715774,0.1319373494941177,-0.3992539543639224,-0.5469841591373178,-0.15012484669639614,-1.2626335515473222,-0.5800778317818884,-0.47232481248542735,0.07575391724881765,-0.46962537832831075,1.2264138072832325,2.9292015695920246,-0.16923783529812475,-0.8580884661072198,-0.39218356424423156,0.49098034458487394,0.005792073020141221,0.4270914169866144,-1.3264736492426044,0.1618776151991331,-2.1622187115088023,1.964325549427626,0.14482055246371314,0.38745101306458973,-0.06643940161722212,0.07141816124954037,-0.8183721299311245,-0.4666208231024532,0.8991374190101376,-0.30769983426412656,0.08734170754388854,0.7925740125989024,-0.2509402600457234,-0.07474875772451346,0.09511408353645133
                },
                {
                        0.13119937038843604,-0.9274611899670271,0.5773484232610733,0.9972506493430069,-0.4389857719859964,0.1874515275924773,-0.4130142531329409,1.1154956792951614,-0.3845296263756265,-0.8487252688372086,-0.024366420787702504,-1.3780184030017357,0.5200691752914977,-0.32971079146324095,-1.0205288446143372,0.36731195754840423,0.4825239839053295,1.5269136145956541,0.3271338114733851,-0.47530236717749114,-1.2504996143062492,1.477734356580564,-0.6100203464820624,-0.7785077058782632,-0.02566339306184727,-0.7989583747403685,-0.20953841194141476,2.349890595763036,-0.2178007203314325,0.05939509080728911,0.622803798604509,0.11580060035473318,-1.1653670072341362,-0.5896095341772013,1.0145028681917314,-0.05953895268242624,-0.628723406071311,1.1344788638961858,-0.4829215657263194,0.03460152772954214
                },
                {
                        0.11593616676693382,-0.011445649768990278,-0.8643931966679181,-0.9411756078589475,0.03603115111949113,0.16889356119268553,0.039892550231731744,-0.09082461730887961,1.2036018620183593,0.34339543620088603,-0.23075226419018516,-0.7117889638752575,-0.06337872604382397,0.8269177406454621,-0.39882873360696824,-0.32940679601516376,-0.9809647742841502,-0.10776374968863242,1.8741643390018243,0.1217187594671624,-0.5692217660389266,0.29749062129969445,1.073608285077674,-1.567699174963742,0.39837721361813255,-1.285599268345122,-0.0013787018621575615,0.0074107206460311305,2.2730168079135353,-0.6260866050180642,0.08831288578936793,-0.07966917644018623,-1.0819671430368216,0.729346008031442,-0.5182805258585039,0.4233060421063085,0.32279915810335563,-0.5309256014882746,1.3243362579458209,-0.677346714391461
                },
                {
                        1.0781503638417218,-0.19336272957873768,-0.670547306144812,-2.0400925926598545,-0.8228271060020953,0.4221573988133791,0.873893840620581,-1.5376437645748928,1.1524841195999151,1.7284858819069233,0.6647901423959448,-0.5948016555157973,0.9894191694552519,0.7289934043015275,-1.3880763449748021,-1.9365834003736275,-1.2187367077956015,0.4278460043948295,0.5773971401601964,1.7404450317567204,-1.618489150226141,-0.36959853708371315,-0.7639355454341245,1.1438515055626342,0.4379722858832937,1.4884280861789663,0.07887313227653141,-0.5131011123946009,-1.5696581357748696,1.676349484622009,-0.7376018173387654,0.4654963337208878,-0.055100189812261204,-0.6492761836181027,0.35778285162501666,-0.6207651140348628,-0.2370020556482658,0.8039132268236167,-0.8544095194670368,1.5176600496779595
                }
        };
        int row1 = A.length;
        int col1 = A[0].length;
        int row2 = B.length;
        int col2 = B[0].length;
        int i, j, k;
        double C[][] = new double[row1][col2];
        for (i = 0; i < row1; i++) {
            for (j = 0; j < col2; j++) {
                for (k = 0; k < row2; k++)
                    C[i][j] += A[i][k] * B[k][j];
            }
        }

        return maxIndex(C);
    }

    private int maxIndex(double[][] C) {
        int resultIndex = 0;
        double max = C[0][0];
        for(int index=0;index<10;index++){
            if(max< C[index][0]){
                max = C[index][0];
                resultIndex = index;
            }
        }
        return resultIndex;
    }

    private void saveImageInFile(String classification){
        System.out.println("==================Called Save Image");
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_CALENDAR)
                != PackageManager.PERMISSION_GRANTED)
        {
            System.out.println("permissdbsdjfbsj");
            ActivityCompat.requestPermissions(ServersActivity.this,
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
