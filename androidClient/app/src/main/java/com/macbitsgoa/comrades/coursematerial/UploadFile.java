package com.macbitsgoa.comrades.coursematerial;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.macbitsgoa.comrades.BuildConfig;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static android.webkit.MimeTypeMap.getFileExtensionFromUrl;
import static com.macbitsgoa.comrades.CHC.TAG_PREFIX;
import static com.macbitsgoa.comrades.coursematerial.MetaDataAndPermissions.AUTHORIZATION_FIELD_KEY;
import static com.macbitsgoa.comrades.coursematerial.MetaDataAndPermissions.AUTHORIZATION_FIELD_VALUE_PREFIX;

/**
 * Code to upload file.
 * @author aayushSingla
 */

public class UploadFile extends AsyncTask<Void, Void, String> {
    private static final String TAG = TAG_PREFIX + UploadFile.class.getSimpleName();
    protected static final String UPLOADING_FILE = "Uploading your file";
    private final String path;
    private String fileId;
    private final String fName;
    private final String accessToken;
    private final Context context;
    private final ProgressDialog progressDialog;


    public UploadFile(final String path, final String accessToken,
                      final String fName, final Context context) {
        this.context = context;
        progressDialog = new ProgressDialog(context);
        this.path = path;
        this.accessToken = accessToken;
        this.fName = fName;
    }

    @Override
    protected String doInBackground(final Void... voids) {
        final String response = uploadFile();

        try {
            final JSONObject jsonObject = new JSONObject(response);
            fileId = (String) jsonObject.get("id");
        } catch (final JSONException e) {
            Log.e(TAG, e.getMessage(), e.fillInStackTrace());
        }

        return null;
    }

    private String uploadFile() {
        try {
            final File file = new File(path);
            final OkHttpClient okHttpClient = new OkHttpClient();
            final RequestBody requestBody = RequestBody.create(MediaType.parse(getMimeType(path)),
                    fileToBytes(file));

            final String driveUploadUrl =
                    "https://www.googleapis.com/upload/drive/v3/files?uploadType=media";
            final Request request = new Request.Builder()
                    .url(driveUploadUrl)
                    .addHeader("Content-Type", getMimeType(path))
                    .addHeader("Content-Length", String.valueOf(file.length()))
                    .addHeader(AUTHORIZATION_FIELD_KEY,
                            AUTHORIZATION_FIELD_VALUE_PREFIX + accessToken
                    )
                    .post(requestBody)
                    .build();
            final Response response = okHttpClient.newCall(request).execute();
            return response.body().string();
        } catch (final Exception e) {
            Log.e(TAG, e.getMessage(), e.fillInStackTrace());
        }
        return null;
    }

    private static byte[] fileToBytes(final File file) {
        byte[] bytes = new byte[0];
        try (final FileInputStream inputStream = new FileInputStream(file)) {
            bytes = new byte[inputStream.available()];
            //noinspection ResultOfMethodCallIgnored
            inputStream.read(bytes);
        } catch (final IOException e) {
            Log.e(TAG, e.getMessage(), e.fillInStackTrace());
        }
        return bytes;
    }

    private static String getMimeType(String filePath) {
        String type = null;
        final String filePath1 = filePath.replaceAll(" ", "");
        final String extension = getFileExtensionFromUrl(filePath1);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }


    @Override
    protected void onPreExecute() {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "Uploading file");
        }
        progressDialog.setTitle(UPLOADING_FILE);
        progressDialog.setMessage("Please wait.....");
        progressDialog.setIndeterminate(true);
        progressDialog.show();
    }

    @Override
    protected void onPostExecute(final String result) {
        if (BuildConfig.DEBUG) {
            Log.i(TAG, "File upload result is " + result);
        }
        progressDialog.hide();
        final MetaDataAndPermissions mdp =
                new MetaDataAndPermissions(fileId, accessToken, fName, context);
        mdp.execute();
    }


}