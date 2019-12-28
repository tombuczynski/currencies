package com.apress.gerber.currencies;

import android.view.View;
import android.widget.TextView;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;

/**
 * Created by Tom Buczynski on 19.12.2019.
 */
public final class ViewValueMatchers {
    static Matcher<View> withValue(final double val) {
        return new BaseMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("Double value equality test for: " + val);
            }

            @Override
            public boolean matches(Object item) {
                String viewText = ((TextView)item).getText().toString();
                double viewVal = Double.parseDouble(viewText.replace(',', '.'));

                return val == viewVal;
            }
        };
    }
}
