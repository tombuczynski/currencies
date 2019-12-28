package com.apress.gerber.currencies;

import android.app.Activity;
import android.content.Intent;

import java.util.ArrayList;

import androidx.test.rule.ActivityTestRule;

/**
 * Created by Tom Buczynski on 25.12.2019.
 */
public class MainActivityTestRule extends ActivityTestRule<MainActivity> {
    public MainActivityTestRule(Class<MainActivity> activityClass) {
        super(activityClass);
    }

    @Override
    protected Intent getActivityIntent() {
        ArrayList<String> currenciesList = new ArrayList<>();
        currenciesList.add("USD|dollar");
        currenciesList.add("EUR|euro");
        currenciesList.add("PLN|zloty");

        Intent intent = new Intent();
        intent.putExtra(MainActivity.CURR_LIST_NAME, currenciesList);

        return intent;
    }
}
