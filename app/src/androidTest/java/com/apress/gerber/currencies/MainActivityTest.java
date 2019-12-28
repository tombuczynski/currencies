package com.apress.gerber.currencies;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import static androidx.test.espresso.Espresso.closeSoftKeyboard;
import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.apress.gerber.currencies.ViewValueMatchers.withValue;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;

/**
 * Created by Tom Buczynski on 18.12.2019.
 */

@RunWith(AndroidJUnit4.class)
@LargeTest
public class MainActivityTest {

    @Rule
    public MainActivityTestRule mActivityRule = new MainActivityTestRule(MainActivity.class);

    @Before
    public void setUp() throws Exception {
        MainActivity activity = mActivityRule.getActivity();

    }

    @After
    public void tearDown() throws Exception {
    }


    @Test
    public void testFloat() {
        onView(withId(R.id.spin_foreign)).perform(click());
        onData(allOf(instanceOf(String.class), containsString("PLN"))).perform(click());

        onView(withId(R.id.spin_home)).perform(click());
        onData(instanceOf(String.class)).atPosition(1).perform(click());

        onView(withId(R.id.edit_foreign)).perform(typeText("102,,50"));
        closeSoftKeyboard();
        onView(withId(R.id.btn_calc)).perform(click());

        onView(withId(R.id.txt_home)).check(matches(withValue(102.50)));
    }

}