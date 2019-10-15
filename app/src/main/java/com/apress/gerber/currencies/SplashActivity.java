package com.apress.gerber.currencies;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Iterator;

public class SplashActivity extends Activity implements JSONObjectDownloader.ResultCallback {
    private static final String CURR_LIST_NAME = "CURRIENCIES_LIST";
    private final String CODES_LIST_URL = "https://openexchangerates.org/api/currencies.json";
    private ArrayList<String> mCurrenciesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_splash);

        JSONObjectDownloader downloader = new JSONObjectDownloader(this);
        downloader.execute(CODES_LIST_URL);
    }

    @Override
    public void ResultReturned(JSONObject jsonObject) {
        try {
            mCurrenciesList = JSONtoStringList(jsonObject);

            Intent intent = new Intent(this, MainActivity.class);
            intent.putExtra(CURR_LIST_NAME, mCurrenciesList);
            startActivity(intent);

        } catch (JSONException e) {
            Error(JSONObjectDownloader.ResultCallback.ERR_JSON, "JSON object iteration error");
        }
    }

    @Override
    public void Error(int errCode, String errMsg) {
        Toast.makeText(this, "Error:" + errCode + ", " + errMsg, Toast.LENGTH_LONG).show();
        finish();
    }

    private static ArrayList<String> JSONtoStringList(JSONObject jo) throws JSONException {
        ArrayList<String> list = new ArrayList<>();
        Iterator<String> i = jo.keys();
        String key;

        while (i.hasNext()) {
            key = i.next();
            list.add(key + " | " + jo.getString(key));
        }

        return list;
    }
}
