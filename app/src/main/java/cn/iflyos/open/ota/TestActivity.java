package cn.iflyos.open.ota;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.widget.Button;

public class TestActivity extends Activity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Button button = new Button(this);
        button.setOnClickListener(v ->
                startService(new Intent(this, UpdaterService.class)));

        setContentView(button);
    }

}
