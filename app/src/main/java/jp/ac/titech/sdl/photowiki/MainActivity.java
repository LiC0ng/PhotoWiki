package jp.ac.titech.sdl.photowiki;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.FileProvider;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.vision.v1.Vision;
import com.google.api.services.vision.v1.VisionRequest;
import com.google.api.services.vision.v1.VisionRequestInitializer;
import com.google.api.services.vision.v1.model.AnnotateImageRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesRequest;
import com.google.api.services.vision.v1.model.BatchAnnotateImagesResponse;
import com.google.api.services.vision.v1.model.EntityAnnotation;
import com.google.api.services.vision.v1.model.Feature;
import com.google.api.services.vision.v1.model.Image;
import com.google.gson.Gson;

import org.litepal.crud.DataSupport;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import jp.ac.titech.sdl.photowiki.db.Page;
import jp.ac.titech.sdl.photowiki.db.Root;
import jp.ac.titech.sdl.photowiki.db.Wiki;

public class MainActivity extends AppCompatActivity {
    private static final String CLOUD_VISION_API_KEY = "AIzaSyBj18PLdUDQzFzLudKl0a2bl3lctD8C1-4";
    public static final String FILE_NAME = "temp.jpg";
    private static final String ANDROID_CERT_HEADER = "X-Android-Cert";
    private static final String ANDROID_PACKAGE_HEADER = "X-Android-Package";
    private static final int MAX_LABEL_RESULTS = 10;
    private static final int MAX_DIMENSION = 1200;

    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int GALLERY_PERMISSIONS_REQUEST = 0;
    private static final int GALLERY_IMAGE_REQUEST = 1;
    public static final int CAMERA_PERMISSIONS_REQUEST = 2;
    public static final int CAMERA_IMAGE_REQUEST = 3;

    private ImageView image;
    private TextView wikiContent;
    private TextView contentTitle;
    private TextView totalWiki;
    private TextView wikiUrl;
    private Button getWiki;
    private Button saveWiki;
    private Button translate;
    private Spinner spinner;
    private View line;
    private ListView wikiList;
    private DrawerLayout drawerLayout;

    private Wiki wiki = new Wiki();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        image = findViewById(R.id.image);
        wikiContent = findViewById(R.id.wikiContent);
        wikiUrl = findViewById(R.id.url);
        totalWiki = findViewById(R.id.totalWiki);
        contentTitle = findViewById(R.id.contentTitle);
        getWiki = findViewById(R.id.getWiki);
        saveWiki = findViewById(R.id.saveWiki);
        translate = findViewById(R.id.translate);
        spinner = findViewById(R.id.spanner);
        line = findViewById(R.id.line);
        wikiList = findViewById(R.id.wikiList);
        drawerLayout = findViewById(R.id.drawer_layout);

        setWikiList();

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(view -> {
            AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder
                    .setMessage(R.string.dialog_select_prompt)
                    .setPositiveButton(R.string.dialog_select_gallery, (dialog, which) -> startGalleryChooser())
                    .setNegativeButton(R.string.dialog_select_camera, (dialog, which) -> startCamera());
            builder.create().show();
        });

        saveWiki.setOnClickListener( view -> {
            if(saveWiki.getText().toString().equals("SAVE")){
                saveWiki(wiki);
            } else {
                deleteWiki(wiki);
            }
            checkButtonStatus();
        });

        translate.setOnClickListener(view -> test());
    }

    public void onButtonGet(String title) {
        new Thread(() -> {
            try {
                URL url = new URL("https://en.wikipedia.org/w/api.php?format=json&action=query&prop=extracts&exintro&explaintext&redirects=1&titles=" + title);
                // 処理開始時刻
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                final String str = InputStreamToString(con.getInputStream());
                // 処理終了時刻
                Log.d("HTTP", str);
                Root root = new Gson().fromJson(str, Root.class);
                for (final Page page : root.getQuery().getPages().values()) {
                    wiki.setTitle(page.getTitle());
                    wiki.setContent(page.getExtract());
                    runOnUiThread(() -> {
                        wikiContent.setText(wiki.getContent());
                        setWikiUrl();});
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }).start();
    }

    // InputStream -> String
    static String InputStreamToString(InputStream is) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            sb.append(line);
        }
        br.close();
        return sb.toString();
    }

    public void startGalleryChooser() {
        if (PermissionUtils.requestPermission(this, GALLERY_PERMISSIONS_REQUEST, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select a photo"),
                    GALLERY_IMAGE_REQUEST);
        }
    }

    public void startCamera() {
        if (PermissionUtils.requestPermission(
                this,
                CAMERA_PERMISSIONS_REQUEST,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA)) {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivityForResult(intent, CAMERA_IMAGE_REQUEST);
        }
    }

    public File getCameraFile() {
        File dir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return new File(dir, FILE_NAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == GALLERY_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            uploadImage(data.getData());
        } else if (requestCode == CAMERA_IMAGE_REQUEST && resultCode == RESULT_OK) {
            Uri photoUri = FileProvider.getUriForFile(this, getApplicationContext().getPackageName() + ".provider", getCameraFile());
            uploadImage(photoUri);
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CAMERA_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, CAMERA_PERMISSIONS_REQUEST, grantResults)) {
                    startCamera();
                }
                break;
            case GALLERY_PERMISSIONS_REQUEST:
                if (PermissionUtils.permissionGranted(requestCode, GALLERY_PERMISSIONS_REQUEST, grantResults)) {
                    startGalleryChooser();
                }
                break;
        }
    }

    public void uploadImage(Uri uri) {
        if (uri != null) {
            try {
                // scale the image to save on bandwidth
                Bitmap bitmap =
                        scaleBitmapDown(MediaStore.Images.Media.getBitmap(getContentResolver(), uri));

                callCloudVision(bitmap);
                image.setImageBitmap(bitmap);

            } catch (IOException e) {
                Log.d(TAG, "Image picking failed because " + e.getMessage());
                Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
            }
        } else {
            Log.d(TAG, "Image picker gave us a null image.");
            Toast.makeText(this, R.string.image_picker_error, Toast.LENGTH_LONG).show();
        }
    }

    private Vision.Images.Annotate prepareAnnotationRequest(Bitmap bitmap) throws IOException {
        HttpTransport httpTransport = AndroidHttp.newCompatibleTransport();
        JsonFactory jsonFactory = GsonFactory.getDefaultInstance();

        VisionRequestInitializer requestInitializer =
                new VisionRequestInitializer(CLOUD_VISION_API_KEY) {
                    /**
                     * We override this so we can inject important identifying fields into the HTTP
                     * headers. This enables use of a restricted cloud platform API key.
                     */
                    @Override
                    protected void initializeVisionRequest(VisionRequest<?> visionRequest)
                            throws IOException {
                        super.initializeVisionRequest(visionRequest);

                        String packageName = getPackageName();
                        visionRequest.getRequestHeaders().set(ANDROID_PACKAGE_HEADER, packageName);

                        String sig = PackageManagerUtils.getSignature(getPackageManager(), packageName);

                        visionRequest.getRequestHeaders().set(ANDROID_CERT_HEADER, sig);
                    }
                };

        Vision.Builder builder = new Vision.Builder(httpTransport, jsonFactory, null);
        builder.setVisionRequestInitializer(requestInitializer);

        Vision vision = builder.build();

        BatchAnnotateImagesRequest batchAnnotateImagesRequest =
                new BatchAnnotateImagesRequest();
        batchAnnotateImagesRequest.setRequests(new ArrayList<AnnotateImageRequest>() {{
            AnnotateImageRequest annotateImageRequest = new AnnotateImageRequest();

            // Add the image
            Image base64EncodedImage = new Image();
            // Convert the bitmap to a JPEG
            // Just in case it's a format that Android understands but Cloud Vision
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            wiki.setImage(byteArrayOutputStream.toByteArray());

            // Base64 encode the JPEG
            base64EncodedImage.encodeContent(wiki.getImage());
            annotateImageRequest.setImage(base64EncodedImage);

            // add the features we want
            annotateImageRequest.setFeatures(new ArrayList<Feature>() {{
                Feature labelDetection = new Feature();
                labelDetection.setType("LABEL_DETECTION");
                labelDetection.setMaxResults(MAX_LABEL_RESULTS);
                add(labelDetection);
            }});

            // Add the list of one thing to the request
            add(annotateImageRequest);
        }});

        Vision.Images.Annotate annotateRequest =
                vision.images().annotate(batchAnnotateImagesRequest);
        // Due to a bug: requests to Vision API containing large images fail when GZipped.
        annotateRequest.setDisableGZipContent(true);
        Log.d(TAG, "created Cloud Vision request object, sending request");

        return annotateRequest;
    }

    private class LableDetectionTask extends AsyncTask<Object, Void, List<String>> {
        private final WeakReference<MainActivity> mActivityWeakReference;
        private Vision.Images.Annotate mRequest;

        LableDetectionTask(MainActivity activity, Vision.Images.Annotate annotate) {
            mActivityWeakReference = new WeakReference<>(activity);
            mRequest = annotate;
        }

        @Override
        protected List<String> doInBackground(Object... params) {
            try {
                Log.d(TAG, "created Cloud Vision request object, sending request");
                BatchAnnotateImagesResponse response = mRequest.execute();
                return convertResponseToString(response);

            } catch (GoogleJsonResponseException e) {
                Log.d(TAG, "failed to make API request because " + e.getContent());
            } catch (IOException e) {
                Log.d(TAG, "failed to make API request because of other IOException " +
                        e.getMessage());
            }
            List<String> data_list = new ArrayList<>();
            data_list.add("failed");
            return data_list;
        }

        protected void onPostExecute(List<String> result) {
            MainActivity activity = mActivityWeakReference.get();

            if (activity != null && !activity.isFinishing()) {
                ArrayAdapter<String> arr_adapter;
                arr_adapter= new ArrayAdapter<>(activity, R.layout.spinner_item, result);
                arr_adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                spinner.setAdapter(arr_adapter);
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        getWiki.setOnClickListener(view1 -> {
                            wiki.setTitle((String)adapterView.getItemAtPosition(i));
                            contentTitle.setText(wiki.getTitle());
                            line.setVisibility(View.VISIBLE);
                            checkButtonStatus();
                            onButtonGet(wiki.getTitle());
                            saveWiki.setVisibility(View.VISIBLE);
                            translate.setVisibility(View.VISIBLE);
                        });
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                });
            }
        }
    }

    private void callCloudVision(final Bitmap bitmap) {
        // Do the real work in an async task, because we need to use the network anyway
        try {
            AsyncTask<Object, Void, List<String>> labelDetectionTask = new LableDetectionTask(this, prepareAnnotationRequest(bitmap));
            labelDetectionTask.execute();
        } catch (IOException e) {
            Log.d(TAG, "failed to make API request because of other IOException " +
                    e.getMessage());
        }
    }

    private Bitmap scaleBitmapDown(Bitmap bitmap) {

        int originalWidth = bitmap.getWidth();
        int originalHeight = bitmap.getHeight();
        int resizedWidth = MAX_DIMENSION;
        int resizedHeight = MAX_DIMENSION;

        if (originalHeight > originalWidth) {
            resizedWidth = (int) (resizedHeight * (float) originalWidth / (float) originalHeight);
        } else if (originalWidth > originalHeight) {
            resizedHeight = (int) (resizedWidth * (float) originalHeight / (float) originalWidth);
        }
        return Bitmap.createScaledBitmap(bitmap, resizedWidth, resizedHeight, false);
    }

    private static List<String> convertResponseToString(BatchAnnotateImagesResponse response) {
        List<EntityAnnotation> labels = response.getResponses().get(0).getLabelAnnotations();
        List<String> data_list = new ArrayList<>();
        if (labels != null) {
            for (EntityAnnotation label : labels) {
                data_list.add(label.getDescription());
            }
        }

        return data_list;
    }

    public void saveWiki(Wiki saveWiki){
        Log.d("HTTP", saveWiki.getTitle() + "   1");
        if(saveWiki.getTitle() != null & saveWiki.getContent() != null & saveWiki.getImage() != null){
            Wiki wiki = new Wiki();
            wiki.setTitle(saveWiki.getTitle());
            wiki.setContent(saveWiki.getContent());
            wiki.setImage(saveWiki.getImage());
            wiki.setCreate_time(System.currentTimeMillis());
            wiki.save();
            setWikiList();
        }
    }

    public void deleteWiki(Wiki wiki) {
        DataSupport.deleteAll(Wiki.class, "title = ?", wiki.getTitle());
        setWikiList();
    }

    public void setWikiList() {
        drawerLayout.setScrimColor(Color.TRANSPARENT);
        String count = "total:" + DataSupport.count(Wiki.class);
        totalWiki.setText(count);
        List<Wiki> wikiArray = DataSupport.select("title").find(Wiki.class);
        List<String> data = new ArrayList<>();
        for(Wiki myWiki : wikiArray) {
            data.add(myWiki.getTitle());
        }
        ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(MainActivity.this, android.R.layout.simple_list_item_1, data);
        wikiList.setAdapter(arrayAdapter);
        wikiList.setOnItemClickListener((adapterView, view, i, l) ->  {
                String titleOfList = (String)adapterView.getItemAtPosition(i);
                wiki.setTitle(titleOfList);
                List<Wiki> myWikiArray= DataSupport.where("title = ?", titleOfList).find(Wiki.class);
                for(Wiki myWiki : myWikiArray) {
                    wiki = myWiki;
                    Log.d("HTTP", "set");
                    drawerLayout.closeDrawer(Gravity.START);
                    image.setImageBitmap(BitmapFactory.decodeByteArray(wiki.getImage(), 0, wiki.getImage().length));
                    contentTitle.setText(wiki.getTitle());
                    checkButtonStatus();
                    line.setVisibility(View.VISIBLE);
                    wikiContent.setText(wiki.getContent());
                    setWikiUrl();
                    saveWiki.setVisibility(View.VISIBLE);
                    translate.setVisibility(View.VISIBLE);
                }
        });
    }

    public void checkButtonStatus()
    {
        if(DataSupport.isExist(Wiki.class, "title = ?", wiki.getTitle())){
            saveWiki.setText(R.string.delete_wiki);
            saveWiki.setBackgroundResource(R.drawable.button_shape_red);
        } else {
            saveWiki.setText(R.string.save_wiki);
            saveWiki.setBackgroundResource(R.drawable.button_shape);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if(wiki.getTitle() != null && wiki.getContent() != null && wiki.getImage()!=null) {
            Log.d("HTTP", "1");
            outState.putByteArray("imageBytes", wiki.getImage());
            outState.putString("title", wiki.getTitle());
            outState.putString("content", wiki.getContent());
        } else {
            outState.putString("title", " ");
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(!savedInstanceState.getString("title").equals(" ")) {
            Log.d("HTTP", "2");
            wiki.setImage(savedInstanceState.getByteArray("imageBytes"));
            wiki.setTitle(savedInstanceState.getString("title"));
            wiki.setContent(savedInstanceState.getString("content"));
            contentTitle.setText(wiki.getTitle());
            line.setVisibility(View.VISIBLE);
            image.setImageBitmap(BitmapFactory.decodeByteArray(wiki.getImage(), 0, wiki.getImage().length));
            wikiContent.setText(wiki.getContent());
            setWikiUrl();
            checkButtonStatus();
            saveWiki.setVisibility(View.VISIBLE);
            translate.setVisibility(View.VISIBLE);
        }
    }

    public void setWikiUrl() {
        String url = "<a href=\"http://blog.csdn.net/zhangjinhuang\">我的CSDN博客</a>";
        wikiUrl.setText(Html.fromHtml(url));
        wikiUrl.setMovementMethod(LinkMovementMethod.getInstance());
    }

    public void test() {
    }

}
