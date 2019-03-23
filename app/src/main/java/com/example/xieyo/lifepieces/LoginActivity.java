package com.example.xieyo.lifepieces;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import cn.bmob.v3.BmobUser;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.LogInListener;
import cn.bmob.v3.listener.SaveListener;

public class LoginActivity extends BaseActivity{
    private EditText UserAcountText,UserPasswordText;
    private SharedPreferences mSpSettings,pSpSettings=null;//声明一个sharedPreferences用于保存数据
    private static final String PREPS_NAME="NamePwd";
    private CheckBox isRmenberpassword,isRememberstate;
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        Button UserloginButton;
        UserloginButton=findViewById(R.id.user_login_button);
        UserAcountText=findViewById((R.id.user_login_acount));
        UserPasswordText=findViewById(R.id.user_login_password);
        isRmenberpassword=findViewById(R.id.isrememberpassword);
        isRememberstate=findViewById(R.id.isrememberstate);
        mSpSettings=getSharedPreferences(PREPS_NAME,MODE_PRIVATE);
        pSpSettings=getSharedPreferences(PREPS_NAME,MODE_PRIVATE);
        if (pSpSettings.getBoolean("isRP",true))
        {
            UserAcountText.setText(pSpSettings.getString("useraccount",""));
            UserPasswordText.setText(pSpSettings.getString("password",""));
        }

        UserloginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String account=UserAcountText.getText().toString();
                final String password=UserPasswordText.getText().toString();

                if (isRmenberpassword.isChecked())
                {
                    mSpSettings=getSharedPreferences(PREPS_NAME,MODE_PRIVATE);
                    SharedPreferences.Editor edit=mSpSettings.edit();//得到Editor对象        
                    edit.putBoolean("isRP",true);//记录保存标记
                    edit.putString("useraccount",account);//记录用户名        
                    edit.putString("password",password);//记录密码        
                    edit.commit();//**提交  
                }
                else
                {
                    mSpSettings=getSharedPreferences(PREPS_NAME,MODE_PRIVATE);
                    SharedPreferences.Editor edit=mSpSettings.edit();//得到Editor对象        
                    edit.putBoolean("isRP",true);//记录保存标记 
                    edit.putString("useraccount","");//记录用户名        
                    edit.putString("password","");//记录密码        
                    edit.commit();//**提交
                }
                if (isRememberstate.isChecked())
                {
                    mSpSettings=getSharedPreferences(PREPS_NAME,MODE_PRIVATE);
                    SharedPreferences.Editor edit=mSpSettings.edit();//得到Editor对象        
                    //edit.putBoolean("isRS",true);
                    edit.putString("loginstate","YES");
                    edit.commit();


                }
                else
                {
                    mSpSettings=getSharedPreferences(PREPS_NAME,MODE_PRIVATE);
                    SharedPreferences.Editor edit=mSpSettings.edit();//得到Editor对象        
                   // edit.putBoolean("isRS",false);
                    edit.putString("loginstate","NO");

                    edit.commit();

                }
                BmobUser  user = new BmobUser ();
                user.setMobilePhoneNumber(account);
                user.setPassword(password);
                BmobUser.loginByAccount(account, password, new LogInListener<UserManager>() {

                    @Override
                    public void done(UserManager user, BmobException e) {
                        if(user!=null){

                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                            startActivity(intent);
                            finish();

                            UserBaseInfo.UserName="user"+account;
                            final UserObject userobject =new UserObject();
                            userobject.save(new SaveListener<String>() {

                                @Override
                                public void done(String objectId, BmobException e) {
                                    if(e==null){
                                       // toast("创建数据成功：" + objectId);
                                    }else{
                                        Log.i("bmob","失败："+e.getMessage()+","+e.getErrorCode());

                                        //Log.i("bmob","失败："+e.getMessage()+","+e.getErrorCode());
                                    }
                                }
                            });
                          /*  UserBaseInfo.BillName="bill"+account;
                            final BillObject billobject=new BillObject();
                            billobject.save(new SaveListener<String>() {

                                @Override
                                public void done(String objectId, BmobException e) {
                                    if(e==null){
                                        // toast("创建数据成功：" + objectId);
                                    }else{
                                        Log.i("bmob","失败："+e.getMessage()+","+e.getErrorCode());

                                        //Log.i("bmob","失败："+e.getMessage()+","+e.getErrorCode());
                                    }
                                }
                            });*/

                            //Log.i("smile","用户登陆成功");
                        }
                        else
                        {
                            Toast.makeText(LoginActivity.this, "登录失败，请检查账户或者密码",Toast.LENGTH_LONG).show();

                        }
                    }
                });
//                user.login(new SaveListener<BmobUser >() {
//
//                    @Override
//                    public void done(BmobUser  bmobUser, BmobException e) {
//                        if(e==null){
//                            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
//                            startActivity(intent);
//                            UserBaseInfo.Loginaccount=account;
//                            //toast("登录成功:");
//                            //通过BmobUser user = BmobUser.getCurrentUser()获取登录成功后的本地用户信息
//                            //如果是自定义用户对象MyUser，可通过MyUser user = BmobUser.getCurrentUser(MyUser.class)获取自定义用户信息
//                        }else{
//                           // loge(e);
//                        }
//                    }
//                });
            }
        });


    }

}
