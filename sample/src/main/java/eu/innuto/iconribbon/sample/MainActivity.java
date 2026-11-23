package eu.innuto.iconribbon.sample;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView label = new TextView(this);
        label.setText("Icon Ribbon Sample\n" + BuildConfig.FLAVOR + " / " + BuildConfig.BUILD_TYPE
                + "\n\nCheck the launcher icon.");
        label.setGravity(Gravity.CENTER);
        label.setTextSize(20);
        setContentView(label);
    }
}
