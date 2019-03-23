package com.example.xieyo.lifepieces;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.SaveListener;


public class RegisterActivity extends BaseActivity{
    private EditText UserAcountText,UserPasswordText;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        Button UserRegisterButton;
        UserRegisterButton=findViewById(R.id.user_register_button);
        UserAcountText=findViewById((R.id.user_register_acount));
        UserPasswordText=findViewById(R.id.user_register_password);

        UserRegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String account=UserAcountText.getText().toString();
                final String password=UserPasswordText.getText().toString();
                UserManager user = new UserManager();
                user.setUsername("user"+account);
                user.setPassword(password);
                user.setMobilePhoneNumber(account);
//注意：不能用save方法进行注册

                user.signUp(new SaveListener<UserManager>() {
                    @Override
                    public void done(UserManager s, BmobException e) {
                        if(e==null){
                            Toast.makeText(RegisterActivity.this, "注册成功,请登录",Toast.LENGTH_LONG).show();
                            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                            startActivity(intent);
                            //toast("注册成功:" +s.toString());
                        }else{

                            // loge(e);
                        }
                    }
                });
            }
        });





    }
}
