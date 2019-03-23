package com.example.xieyo.lifepieces;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class StartActivity extends BaseActivity{

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_first_start);
        Button Buttonlogin,Buttonregister;
        Buttonlogin=findViewById(R.id.buttonlogin);
        Buttonregister=findViewById(R.id.buttonregister);

        Buttonlogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartActivity.this, LoginActivity.class);
                startActivity(intent);
                finish();

                // finish();
            }
        });
        Buttonregister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(StartActivity.this, RegisterActivity.class);
                startActivity(intent);
               finish();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        SharedPreferences getmSpSettings;
        getmSpSettings=getSharedPreferences("NamePwd",MODE_PRIVATE);
        String state=getmSpSettings.getString("loginstate","");
        if (state.equals("YES"))
        {
            Intent intent = new Intent(StartActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
