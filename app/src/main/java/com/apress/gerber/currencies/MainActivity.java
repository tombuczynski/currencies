package com.apress.gerber.currencies;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static android.widget.AdapterView.INVALID_POSITION;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemSelectedListener, JSONObjectDownloader.ResultCallback {
    public static final String CURR_LIST_NAME = "CURRIENCIES_LIST";
    private static final String SUPPORTED_CURRENCIES_URL = "https://oxr.readme.io/docs/supported-currencies";
    private static final String SPINNER_KEY_FOREIGN = "spinner_foreign";
    private static final String SPINNER_KEY_HOME = "spinner_home";
    private static final String API_KEYS_PROPS = "api_keys.properties";
    private static final String OXR_API_KEY = "oxr_key";
    private static final String OXR_API_URL = "https://openexchangerates.org/api/";
    private static final String OXR_RATES = "latest.json";
    private static final String OXR_APP_ID = "?app_id=";
    private static final String OXR_RATES_OBJECT = "rates";
    private static final String OXR_RATES_TIMESTAMP = "timestamp";
    private static final DecimalFormat RESULT_FORMAT = new DecimalFormat("#,##0.00");
    private static final int RATES_TIMEOUT = 3600;

    private String mOXRKey;

    private String[] mCurrencies;
    private double mForeignVal;

    private Spinner mSpinForeign, mSpinHome;
    private EditText mEditForeign;
    private TextView mTxtHome;

    private JSONObject mRates;
    private int mRatesTimestamp = 0; // seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setIcon(R.mipmap.ic_launcher_round);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }

        mCurrencies = null;

        Intent intent = getIntent();
        if (intent != null) {
            @SuppressWarnings("unchecked")
            ArrayList<String> s = (ArrayList<String>) intent.getSerializableExtra(CURR_LIST_NAME);
            if (s != null) {
                mCurrencies = s.toArray(new String[0]);
                Arrays.sort(mCurrencies);
            }
        }

        if (mCurrencies == null) {
            finish();
            return;
        }


        mSpinForeign = findViewById(R.id.spin_foreign);
        mSpinHome = findViewById(R.id.spin_home);
        mEditForeign = findViewById(R.id.edit_foreign);
        mTxtHome = findViewById(R.id.txt_home);

        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(this, R.layout.spinner_item, mCurrencies);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        mSpinForeign.setAdapter(spinnerAdapter);
        mSpinForeign.setOnItemSelectedListener(this);

        mSpinHome.setAdapter(spinnerAdapter);
        mSpinHome.setOnItemSelectedListener(this);

        if (savedInstanceState == null) {
            restoreCurrCodeSpinnerSelection(mSpinForeign, SPINNER_KEY_FOREIGN, "EUR");
            restoreCurrCodeSpinnerSelection(mSpinHome, SPINNER_KEY_HOME, "PLN");
        }

        mOXRKey = AssetsProperties.getStringProp(this, API_KEYS_PROPS, OXR_API_KEY);

        mEditForeign.addTextChangedListener(new TextWatcher() {
            private String mLastText = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                mLastText = s.toString();
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {

                try {
                    String newText = s.toString().trim();
                    if (newText.length() > 0) {
                        mForeignVal = Double.parseDouble(newText.replace(',', '.'));
                    } else {
                        mForeignVal = 0.0;
                    }
                } catch (NumberFormatException e) {
                    mForeignVal = 0.0;
                    s.replace(0, s.length(), mLastText);
                }
            }
        });

        mEditForeign.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    onCalcButtonClick(v);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_settings, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_exit:
                finish();
                break;

            case R.id.menu_swap_currencies:
                int pos1 = mSpinForeign.getSelectedItemPosition();
                int pos2 = mSpinHome.getSelectedItemPosition();
                mSpinForeign.setSelection(pos2);
                mSpinHome.setSelection(pos1);
                break;

            case R.id.menu_view_currencies:
                openWebBrowser(SUPPORTED_CURRENCIES_URL);
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        int viewId = parent.getId();

        mTxtHome.setText(" ");

        if (viewId == R.id.spin_foreign) {
            saveCurrCodeSpinnerSelection(mSpinForeign, SPINNER_KEY_FOREIGN);
            return;
        }

        if (viewId == R.id.spin_home) {
            saveCurrCodeSpinnerSelection(mSpinHome, SPINNER_KEY_HOME);
            return;
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    public void onCalcButtonClick(View v) {

        hideSoftKeyboard(v);

        long currentTime = System.currentTimeMillis();
        if ((System.currentTimeMillis() / 1000L - mRatesTimestamp) > RATES_TIMEOUT) {
            AlertDialog.Builder dialBuilder = new AlertDialog.Builder(this);

            dialBuilder.setTitle("Przeliczanie");

            LayoutInflater li = getLayoutInflater();
            View dlgView = li.inflate(R.layout.task_cancel_dlg_view, null);
            dialBuilder.setView(dlgView);

            JSONObjectDownloader downloader = new JSONObjectDownloader(this, dialBuilder, "Anuluj");
            downloader.execute(OXR_API_URL + OXR_RATES + OXR_APP_ID + mOXRKey);
        } else {
            calcResult();
        }

    }

    private void hideSoftKeyboard(View v) {
        InputMethodManager methodManager = (InputMethodManager)getSystemService(INPUT_METHOD_SERVICE);
        methodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
    }

    private void saveCurrCodeSpinnerSelection(Spinner spinner, String spinnerKey) {
        int pos = spinner.getSelectedItemPosition();
        if (pos != INVALID_POSITION) {
            String currCode = extractCurrCode(mCurrencies[pos]);
            ActivityPrefs.putStringPref(this, spinnerKey, currCode);
        }
    }

    private void restoreCurrCodeSpinnerSelection(Spinner spinner, String spinnerKey, String defCode) {
        String code = ActivityPrefs.getStringPref(this, spinnerKey, defCode);

        spinner.setSelection(findPositionByCurrCode(code, mCurrencies));
    }

    private boolean isNetworkConnected() {
        ConnectivityManager manager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);

        NetworkInfo info = manager.getActiveNetworkInfo();

        return info.isConnected();
    }

    private void openWebBrowser(String url) {
        if (isNetworkConnected()) {
            Uri uri = Uri.parse(url);
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
        } else {
            Toast.makeText(this, "Brak połączenia z Internetem", Toast.LENGTH_SHORT).show();
        }
    }

    private static int findPositionByCurrCode(String code, String[] currList) {
        for (int i = 0; i < currList.length; i++) {
            if (extractCurrCode(currList[i]).equalsIgnoreCase(code))
                return i;
        }

        return 0;
    }

    private static String extractCurrCode(String s) {
        return s.trim().substring(0, 3);
    }

    @Override
    public void ResultReturned(JSONObject jsonObject) {
        try {
            mRates = jsonObject.getJSONObject(OXR_RATES_OBJECT);
            mRatesTimestamp = jsonObject.getInt(OXR_RATES_TIMESTAMP);
            calcResult();
        } catch (JSONException e) {
            mRates = null;
            mRatesTimestamp = 0;
            Error(JSONObjectDownloader.ResultCallback.ERR_JSON, "JSON object iteration error");
        }

    }

    @Override
    public void Error(int errCode, String errMsg) {
        if (errCode == JSONObjectDownloader.ResultCallback.ERR_CANCELED)
            errMsg = "Przeliczanie przerwane";
        Toast.makeText(this, "Error:" + errCode + ", " + errMsg, Toast.LENGTH_LONG).show();
    }

    private void calcResult() {
        try {
            String homeCode = extractCurrCode((String)mSpinHome.getSelectedItem());
            String foreignCode = extractCurrCode((String)mSpinForeign.getSelectedItem());

            double homeRate =mRates.getDouble(homeCode);
            double foreignRate =mRates.getDouble(foreignCode);

            double result = mForeignVal / foreignRate * homeRate;

            mTxtHome.setText(RESULT_FORMAT.format(result));
        } catch (JSONException e) {
            mTxtHome.setText(" ");
            Error(JSONObjectDownloader.ResultCallback.ERR_JSON, "JSON object iteration error");
        }
    }
}
