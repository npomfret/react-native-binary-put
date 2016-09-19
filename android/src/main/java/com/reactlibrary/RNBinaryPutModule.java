
package com.reactlibrary;

import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.WritableMap;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;

import static com.reactlibrary.ReactNativeJson.convertJsonToMap;

public class RNBinaryPutModule extends ReactContextBaseJavaModule {
    public static final String TAG = "RNBinaryPut";

    private final ReactApplicationContext reactContext;

    public RNBinaryPutModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
    }

    @Override
    public String getName() {
        return "RNBinaryPut";
    }

    @ReactMethod
    public void put(String authHeader, String sourceUri, String targetUri, String contentType, Callback callback) {
        if (sourceUri == null) {
            WritableMap map = Arguments.createMap();
            map.putString("invalid parameter", "sourceUri");
            callback.invoke(map, null);
            return;
        }

        if (targetUri == null) {
            WritableMap map = Arguments.createMap();
            map.putString("invalid parameter", "targetUri");
            callback.invoke(map, null);
            return;
        }

        SaveAttachmentTask saveAttachmentTask = new SaveAttachmentTask(authHeader, sourceUri, targetUri, contentType, callback);
        saveAttachmentTask.execute();
    }

    private class SaveAttachmentTask extends AsyncTask<URL, Integer, UploadResult> {
        private final String authHeader;
        private final String sourceUri;
        private final String targetUri;
        private final String contentType;
        private final Callback callback;

        private SaveAttachmentTask(String authHeader, String sourceUri, String targetUri, String contentType, Callback callback) {
            this.authHeader = authHeader;
            this.sourceUri = sourceUri;
            this.targetUri = targetUri;
            this.contentType = contentType;
            this.callback = callback;
        }

        @Override
        protected UploadResult doInBackground(URL... params) {
            try {
                Log.i(TAG, "Uploading attachment '" + sourceUri + "' to '" + targetUri + "'");

                InputStream input;
                if (sourceUri.startsWith("/")) {
                    input = new FileInputStream(new File(sourceUri));
                } else if (sourceUri.startsWith("content://")) {
                    input = RNBinaryPutModule.this.reactContext.getContentResolver().openInputStream(Uri.parse(sourceUri));
                } else {
                    URLConnection urlConnection = new URL(sourceUri).openConnection();
                    input = urlConnection.getInputStream();
                }

                if(input == null) {
                    return new UploadResult(-1, "no input");
                }

                try {
                    HttpURLConnection conn = (HttpURLConnection) new URL(targetUri).openConnection();

                    if(contentType != null)
                        conn.setRequestProperty("Content-Type", contentType);

                    if(authHeader != null)
                        conn.setRequestProperty("Authorization", authHeader);

                    conn.setReadTimeout(100000);
                    conn.setConnectTimeout(100000);
                    conn.setRequestMethod("PUT");
                    conn.setDoInput(true);
                    conn.setDoOutput(true);

                    OutputStream os = conn.getOutputStream();
                    try {
                        byte[] buffer = new byte[1024];
                        int bytesRead;
                        while ((bytesRead = input.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                            publishProgress(bytesRead);
                        }
                    } finally {
                        os.close();
                    }

                    int responseCode = conn.getResponseCode();

                    StringBuilder responseText = new StringBuilder();
                    BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    try {
                        String line;
                        while ((line = br.readLine()) != null) {
                            responseText.append(line);
                        }
                    } finally {
                        br.close();
                    }

                    return new UploadResult(responseCode, responseText.toString());
                } finally {
                    input.close();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to save attachment", e);
                return new UploadResult(-1, "Failed to save attachment " + e.getMessage());
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            Log.d(TAG, "Uploaded " + Arrays.toString(values));
        }

        @Override
        protected void onPostExecute(UploadResult uploadResult) {
            int responseCode = uploadResult.statusCode;
            WritableMap map = Arguments.createMap();
            map.putInt("statusCode", responseCode);

            if(callback == null)
                return;

            if (responseCode == 200 || responseCode == 202) {
                try {
                    JSONObject jsonObject = new JSONObject(uploadResult.response);
                    map.putMap("resp", convertJsonToMap(jsonObject));
                    callback.invoke(null, map);
                } catch (JSONException e) {
                    map.putString("error", uploadResult.response);
                    callback.invoke(map, null);
                    Log.e(TAG, "Failed to parse response from clb: " + uploadResult.response, e);
                }
            } else {
                map.putString("error", uploadResult.response);
                callback.invoke(map, null);
            }
        }
    }

    private static class UploadResult {
        public final int statusCode;
        public final String response;

        public UploadResult(int statusCode, String response) {
            this.statusCode = statusCode;
            this.response = response;
        }
    }
}
