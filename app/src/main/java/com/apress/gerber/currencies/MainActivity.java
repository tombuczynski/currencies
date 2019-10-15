package com.apress.gerber.currencies;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private static final String CURR_LIST_NAME = "CURRIENCIES_LIST";
    public static final String SUPPORTED_CURRENCIES_URL = "https://oxr.readme.io/docs/supported-currencies";
    private String[] mCurrencies = null;

    private Spinner mSpinForeign, mSpinHome;
    private EditText mEditForeign;
    private TextView mTxtHome;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCurrencies = null;

        Intent intent = getIntent();
        if (intent != null) {
            ArrayList<String> s = (ArrayList<String>) intent.getSerializableExtra(CURR_LIST_NAME);
            if (s != null) {
                mCurrencies = s.toArray(new String[0]);
                Arrays.sort(mCurrencies);
            }
        }

        if (mCurrencies == null)
            finish();

        mSpinForeign = findViewById(R.id.spin_foreign);
        mSpinHome = findViewById(R.id.spin_home);
        mEditForeign = findViewById(R.id.edit_foreign);
        mTxtHome = findViewById(R.id.txt_home);

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
                break;

            case R.id.menu_view_currencies:
                openWebBrowser(SUPPORTED_CURRENCIES_URL);
                break;

            default:
                return super.onOptionsItemSelected(item);
        }

        return true;
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

    private boolean isNetworkConnected() {
        ConnectivityManager manager = (ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);

        NetworkInfo info = manager.getActiveNetworkInfo();

        return info.isConnected();
    }
}
