package com.afteredit.starwords;

import android.content.res.Resources;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class helpActivity extends AppCompatActivity {

    ImageView return_btn;
    ImageView help_page;
    BitmapDrawable bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_help);

        return_btn = findViewById(R.id.return_btn);
        help_page = findViewById(R.id.help_page);

        Resources res = getResources();
        bitmap = (BitmapDrawable) res.getDrawable(R.drawable.help_page_re);

        int bitmapHeight = bitmap.getIntrinsicHeight();
        help_page.setImageDrawable(bitmap);
        help_page.getLayoutParams().height=bitmapHeight;

        return_btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
    }
}