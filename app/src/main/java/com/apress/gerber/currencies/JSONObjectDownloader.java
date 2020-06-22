package com.apress.gerber.currencies;

import android.content.DialogInterface;
import android.os.AsyncTask;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import androidx.appcompat.app.AlertDialog;

/**
 * Created by Tom Buczynski on 25.05.2019.
 */
public  class JSONObjectDownloader extends AsyncTask<String, Integer, JSONObject> {

    private final AlertDialog.Builder mWaitDialogBuilder;
    private AlertDialog mWaitDialog = null;
    private final String mCancelButtonText;
    private String mErrorMsg;
    private int mErrorCode;
    private ResultCallback mResultCallback;

    public JSONObjectDownloader(ResultCallback resultCallback, AlertDialog.Builder waitDialogBuilder, String cancelButtonText) {
        if (resultCallback == null) {
            throw new NullPointerException();
        }

        mResultCallback = resultCallback;
        mWaitDialogBuilder = waitDialogBuilder;
        mCancelButtonText = cancelButtonText;
    }


    public interface ResultCallback {

        int ERR_OK = 0;
        int ERR_HTTPS_URL_REQUIRED = 1;
        int ERR_IO = 2;
        int ERR_JSON = 3;
        int ERR_CANCELED = 4;

        void ResultReturned(JSONObject jsonObject);
        void Error(int errCode, String errMsg);

    }

    @Override
    protected void onPreExecute() {
        mErrorMsg = "";
        mErrorCode = ResultCallback.ERR_OK;

        if (mWaitDialogBuilder != null) {
            mWaitDialogBuilder.setCancelable(false);

            if (mCancelButtonText != null && mCancelButtonText.trim().length() > 0) {
                mWaitDialogBuilder.setNegativeButton(mCancelButtonText, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        JSONObjectDownloader.this.cancel(true);
                    }
                });
            }

            mWaitDialog = mWaitDialogBuilder.create();
            if (!isCancelled()) {
                mWaitDialog.show();
            }
        }

    }

    @Override
    protected void onPostExecute(JSONObject jo) {
        if (mWaitDialog != null) {
            mWaitDialog.dismiss();
        }

        if (jo != null) {
            mResultCallback.ResultReturned(jo);
        } else {
            mResultCallback.Error(getErrorCode(), getErrorMsg());
        }
    }

    @Override
    protected void onCancelled() {
        setErrorMsg("Canceled");
        setErrorCode(ResultCallback.ERR_CANCELED);
        mResultCallback.Error(getErrorCode(), getErrorMsg());
    }

    @Override
    protected JSONObject doInBackground(String... strings) {
        if (! isCancelled() && strings != null && strings.length >=1) {

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                return null;
            }

            InputStream stream = null;
            try {
                URL url = new URL(strings[0]);
                HttpsURLConnection urlConnection = (HttpsURLConnection) url.openConnection();
                urlConnection.setDoInput(true);
                urlConnection.setRequestMethod("GET");
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(2000);
                urlConnection.setUseCaches(false);
                urlConnection.connect();

                int responseCode = urlConnection.getResponseCode();
                if (responseCode != HttpsURLConnection.HTTP_OK) {
                    throw new IOException("HTTP error code: " + responseCode);
                }

                stream = urlConnection.getInputStream();
                String jsonString = readJSONString(stream);

                return new JSONObject(jsonString);

            } catch (IOException e) {
                setErrorMsg(e.getLocalizedMessage());
                setErrorCode(ResultCallback.ERR_IO);
            } catch (ClassCastException e) {
                setErrorMsg(e.getLocalizedMessage());
                setErrorCode(ResultCallback.ERR_HTTPS_URL_REQUIRED);
            } catch (JSONException e) {
                setErrorMsg(e.getLocalizedMessage());
                setErrorCode(ResultCallback.ERR_JSON);
            }
        }

        return null;
    }

    private void setErrorCode(int errorCode) {
        mErrorCode = errorCode;
    }

    public String getErrorMsg() {
        return mErrorMsg;
    }

    private void setErrorMsg(String errorMsg) {
        if (mErrorMsg.isEmpty()) {
            mErrorMsg = errorMsg;
        } else {
            mErrorMsg += "; " + errorMsg;
        }
    }

    public int getErrorCode() {
        return mErrorCode;
    }

    private String readJSONString(InputStream stream) throws IOException {
        StringBuilder sb;
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"));
            sb = new StringBuilder();

            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } finally {
            stream.close();
        }

        return sb.toString();
    }
}
