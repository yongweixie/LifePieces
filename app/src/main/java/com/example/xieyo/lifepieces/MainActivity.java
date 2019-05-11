package com.example.xieyo.lifepieces;

import android.Manifest;
import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.AlarmClock;
import android.provider.CalendarContract;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.aip.nlp.AipNlp;
import com.example.xieyo.lifepieces.setting.IatSettings;

import com.example.xieyo.lifepieces.time.nlp.TimeNormalizer;
import com.example.xieyo.lifepieces.time.nlp.TimeUnit;
import com.example.xieyo.lifepieces.time.util.DateUtil;
import com.example.xieyo.lifepieces.util.JsonParser;
import com.iflytek.cloud.ErrorCode;
import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.LexiconListener;
import com.iflytek.cloud.RecognizerListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;
import com.iflytek.cloud.util.ContactManager;
import com.iflytek.sunflower.FlowerCollector;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.bmob.v3.BmobQuery;
import cn.bmob.v3.BmobSMS;
import cn.bmob.v3.exception.BmobException;
import cn.bmob.v3.listener.FindListener;
import cn.bmob.v3.listener.QueryListener;
import cn.bmob.v3.listener.SaveListener;
import cn.bmob.v3.listener.UpdateListener;

import static android.provider.AlarmClock.EXTRA_SKIP_UI;
import static com.example.xieyo.lifepieces.time.nlp.TimeNormalizer.writeModel;
import static org.apache.log4j.spi.Configurator.NULL;


public class MainActivity extends BaseActivity implements View.OnClickListener{
    private Toast mToast;
    private static final String TAG = MainActivity.class.getSimpleName();

    /*  发送消息*/
    private ListView lv_chat_list;
    private EditText ed_send;
    private Button btn_send;
    private List<Msg> mList = new ArrayList<>();
    private MsgAdapter adapter;
    /**/

    /*语音识别*/
    TimeNormalizer normalizer;
    private Button startRecord;
    private com.iflytek.cloud.SpeechRecognizer mIat;
    // 语音听写UI
    private RecognizerDialog mIatDialog;
    // 用HashMap存储听写结果
    private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();


    private SharedPreferences mSharedPreferences;
    // 引擎类型
    private String mEngineType = SpeechConstant.TYPE_CLOUD;

    private boolean mTranslateEnable = false;
    /**/

    @Override
    protected void onCreate (Bundle savedInstanceState) {
        SpeechUtility.createUtility(MainActivity.this, "appid=" + getString(R.string.app_id));
        super.onCreate(savedInstanceState);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);

        setContentView(R.layout.activity_main);
        //初始化控件
        initView();
        mToast = Toast.makeText(this, "", Toast.LENGTH_SHORT);
       /* startRecongizer=(Button) findViewById(R.id.StartRecognize);
        startRecongizer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent;
                intent= new Intent(MainActivity.this, IatActivity.class);
                if (intent != null) {
                    startActivity(intent);
                }
            }
        });*/



        mIat = com.iflytek.cloud.SpeechRecognizer.createRecognizer(MainActivity.this, mInitListener);

        // 初始化听写Dialog，如果只使用有UI听写功能，无需创建SpeechRecognizer
        // 使用UI听写功能，请根据sdk文件目录下的notice.txt,放置布局文件和图片资源
        mIatDialog = new RecognizerDialog(MainActivity.this, mInitListener);

        mSharedPreferences = getSharedPreferences(IatSettings.PREFER_NAME,
                Activity.MODE_PRIVATE);
         normalizer = new TimeNormalizer(getApplicationContext().getFilesDir().getPath() + "/TimeExp.m",false);

    }

    private void initView() {
        lv_chat_list = (ListView) findViewById(R.id.lv_chat_dialog);
        ed_send = (EditText) findViewById(R.id.et_chat_message);
        btn_send = (Button) findViewById(R.id.btn_chat_message_send);
        lv_chat_list.setDivider(null);

        //设置适配器
        adapter = new MsgAdapter  (this,mList);
        lv_chat_list.setAdapter(adapter);

        //设置发送按钮监听
        btn_send.setOnClickListener(this);
        startRecord=(Button) findViewById(R.id.start_record);
        startRecord.setOnClickListener(this);
        //设置欢迎语
        addlefttext("你好呀！");
    }
    int ret = 0; // 函数调用返回值

    private void showTip(final String str) {
        mToast.setText(str);
        mToast.show();
    }

    @Override
    protected void onResume() {
        // 开放统计 移动数据统计分析
        FlowerCollector.onResume(MainActivity.this);
        FlowerCollector.onPageStart(TAG);
        super.onResume();
    }

    @Override
    protected void onPause() {
        // 开放统计 移动数据统计分析
        FlowerCollector.onPageEnd(TAG);
        FlowerCollector.onPause(MainActivity.this);
        super.onPause();
    }
    public static String getResult(final String url) {
        final StringBuilder sb = new StringBuilder();
        FutureTask<String> task = new FutureTask<String>(new Callable<String>() {
            @Override
            public String call() throws Exception {
                BufferedReader br = null;
                InputStreamReader isr = null;
                URLConnection conn;
                try {
                    URL geturl = new URL(url);
                    conn = geturl.openConnection();//创建连接
                    conn.connect();//get连接
                    InputStreamReader is = new InputStreamReader(conn.getInputStream());//输入流
                    br = new BufferedReader(is);
                    String line = null;
                    while ((line = br.readLine()) != null) {
                        sb.append(line);//获取输入流数据
                    }
                    //System.out.println(sb.toString());

                } catch (Exception e) {
                    e.printStackTrace();
                } finally {//执行流的关闭
                    if (br != null) {
                        try {
                            if (br != null) {
                                br.close();
                            }
                            if (isr != null) {
                                isr.close();
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } }}
                return sb.toString();
            }
        });
        new Thread(task).start();
        String s = null;
        try {
            s = task.get();//异步获取返回值
        } catch (Exception e) {
            e.printStackTrace();
        }
        return s;
    }




    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.btn_chat_message_send:
                String message = ed_send.getText().toString().trim();
                ed_send.setText("");

                OnClickSend(message);
                break;
            case R.id.start_record:
                // 移动数据分析，收集开始听写事件
                FlowerCollector.onEvent(MainActivity.this, "iat_recognize");

                ed_send.setText(null);// 清空显示内容
                mIatResults.clear();
                // 设置参数
                setParam();
               // boolean isShowDialog = mSharedPreferences.getBoolean(getString(R.string.pref_key_iat_show), true);
                boolean isShowDialog=false;
                if (isShowDialog) {
                    // 显示听写对话框
                    mIatDialog.setListener(mRecognizerDialogListener);
                    mIatDialog.show();
                    //获取字体所在的控件，设置为"",隐藏字体，
                    TextView txt = (TextView)mIatDialog.getWindow().getDecorView().findViewWithTag("textlink");
                    txt.setText("");
                    showTip(getString(R.string.text_begin));
                } else {
                    // 不显示听写对话框
                    ret = mIat.startListening(mRecognizerListener);
                    if (ret != ErrorCode.SUCCESS) {
                        showTip("听写失败,错误码：" + ret);
                    } else {
                        showTip(getString(R.string.text_begin));
                    }
                }
                break;
        }
    }
    private void OnClickSend(String message)
    {
        if(!TextUtils.isEmpty(message)){
            //点击发送后清空输入框
            addrighttext(message);
            //定义URL
            //图灵机器人接口地址：http://www.tuling123.com/openapi/api
            //key=后接在图灵官网申请到的apikey
            //info后接输入的内容
            String path ="http://www.tuling123.com/openapi/api?"+
                    "key="+UserBaseInfo.UserAppid+"&info="+message;
            //RxVolley将信息发出（添加RxVolley依赖，
            //getResult(path);
            if(message.contains("记住"))
            {
                if (message.length()>=3)
                {
                    //if (message.contains("花了"))
                  //  {
                      //  UpLoadbillData(message);


                  //  }
                  //  else
                  //  {
                        TimeInfo timer =GetDataFromText(message);

                        UpLoadData(message);
                        AddCalendar(timer);
                }

                else
                {
                    addlefttext("记住什么呢");
                }
            }
            else if(message.contains("查询"))
            {
                if (message.length()>=3)
                {
                    if(message.contains("天气")||message.contains("快递"))
                        pasingJson(getResult(path));
                    else
                        QueryData(message);
                }
                else
                {
                    addlefttext("查询什么呢");

                }
            }
            else if(message.contains("提醒"))
            {
                if(message.length()>=3)
                    Remind(message);
                else
                    addlefttext("提醒什么呢");

            }
            else if(message.contains("退出登录")||message.contains("登出")||message.contains("注销"))
            {
                SharedPreferences mSpSettings=getSharedPreferences("NamePwd",MODE_PRIVATE);
                SharedPreferences.Editor edit=mSpSettings.edit();//得到Editor对象        
               // edit.putBoolean("isRS",false);//记录保存标记
                edit.putString("loginstate","NO");

                edit.commit();
                Intent intent = new Intent(MainActivity.this, StartActivity.class);
                startActivity(intent);
                finish();
            }
            else
            {
                pasingJson(getResult(path));
            }


        }else{
            return;
        }

    }
    private void UpLoadbillData(String message)
    {
        final BillObject billobject =new BillObject();

        StrictMode.ThreadPolicy policy=new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);//加上这两行；得知Android不能再主线程去访问网络，需要另起线程去访问
        final String APP_ID = "14202177";
        final String API_KEY = "2zuwF5GEN6ndGOZCir7a0ZR4";
        final String SECRET_KEY = "5KM9TpynDvzjfY7DzzZ55LvLcC0m1j89";
        AipNlp client = new AipNlp(APP_ID, API_KEY, SECRET_KEY);

        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);

        // 可选：设置代理服务器地址, http和socket二选一，或者均不设置
        // client.setHttpProxy("proxy_host", proxy_port);  // 设置http代理
        // client.setSocketProxy("proxy_host", proxy_port);  // 设置socket代理

        // 可选：设置log4j日志输出格式，若不设置，则使用默认配置
        // 也可以直接通过jvm启动参数设置此环境变量
        // System.setProperty("aip.log4j.conf", "path/to/your/log4j.properties");

        // 调用接口
        String text = message;
        JSONObject res = client.lexer(text, null);





        try{
            final  String objectdata=res.getString("text");

            String billtime="";
            // System.out.println("111"+Nlp_text);
            for (int i=0;i<res.getJSONArray("items").length();i++)
            {
                String Nlp_ne=res.getJSONArray("items").getJSONObject(i).getString("ne");
                String Nlp_item=res.getJSONArray("items").getJSONObject(i).getString("item");

                // Toast.makeText(MainActivity.this, Nlptext,Toast.LENGTH_LONG).show();
                // System.out.println("result+"+Nlptext);

                if (Nlp_ne.equals("TIME"))
                {
                    //System.out.println("result+"+Nlp_item);
                    TimeInfo time=GetDataFromText(Nlp_item);
                    String formittime=time.year+"-"+time.month+"-"+time.day;
                    billobject.setbillTime(formittime);//记下时间
                    billtime=Nlp_item;
                }

            }
            final  String billaim=objectdata.replaceAll("记住|是|。|花了|支出|收入","").replaceAll(billtime,"");
            //objectname=objectname.replaceAll(objecttime,"");//
            Pattern Money=Pattern.compile("花了(.*?)元|支出(.*?)元");
            Matcher money=Money.matcher(objectdata);
            String billamount="";

            if (money.find())
            {
                billamount=money.group(1);
            }

            billobject.setbillAim(billaim.replaceAll("元","").replaceAll(billamount,""));
            billobject.setbillamount(billamount);
            if(objectdata.contains("支出")||objectdata.contains("花了"))
            {
                billobject.setbillType("支出");
            }
            if(objectdata.contains("收入"))
            {
                billobject.setbillType("收入");
            }
            billobject.save(new SaveListener<String>() {

                @Override
                public void done(String objectId, BmobException e) {
                    if(e==null){
                        // Toast.makeText(MainActivity.this, "发布信息成功",Toast.LENGTH_LONG).show();
                    }else{
                        //   Log.i("bmob","失败："+e.getMessage()+","+e.getErrorCode());
                    }
                }
            });
            addlefttext("已为您记下");





        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private  TimeInfo GetDataFromText(String message){
        //editTimeExp();

        TimeInfo time=new TimeInfo();
        normalizer.setPreferFuture(false);
        normalizer.parse(message);// 抽取时间
        TimeUnit[] unit = normalizer.getTimeUnit();
        if(unit.length>=1)
        {
            System.out.println("err"+DateUtil.formatDateDefault(unit[0].getTime()));
            String timestr=DateUtil.formatDateDefault(unit[0].getTime());

            time.year=Integer.parseInt(timestr.substring(0,4));
            //  System.out.println("年"+time.year);

            time.month=Integer.parseInt(timestr.substring(5,7));
            // System.out.println("月"+time.month);

            time.day=Integer.parseInt(timestr.substring(8,10));
            // System.out.println("日"+time.day);

            time.hour=Integer.parseInt(timestr.substring(11,13));
            // System.out.println("小时"+time.hour);

            time.minute=Integer.parseInt(timestr.substring(14,16));
            // System.out.println("分钟"+time.minute);

            time.second=Integer.parseInt(timestr.substring(17,18));
            String objectname=message.replaceAll("提醒|是|。|我|记住","").replaceAll(GetDataFromNpl( message),"");

            time.msg=objectname;


        }

        return time;

    }
    private String GetDataFromNpl(String message) {
        final UserObject userobject =new UserObject();

        StrictMode.ThreadPolicy policy=new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);//加上这两行；得知Android不能再主线程去访问网络，需要另起线程去访问
        final String APP_ID = "14202177";
        final String API_KEY = "2zuwF5GEN6ndGOZCir7a0ZR4";
        final String SECRET_KEY = "5KM9TpynDvzjfY7DzzZ55LvLcC0m1j89";
        AipNlp client = new AipNlp(APP_ID, API_KEY, SECRET_KEY);

        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);

        // 可选：设置代理服务器地址, http和socket二选一，或者均不设置
        // client.setHttpProxy("proxy_host", proxy_port);  // 设置http代理
        // client.setSocketProxy("proxy_host", proxy_port);  // 设置socket代理

        // 可选：设置log4j日志输出格式，若不设置，则使用默认配置
        // 也可以直接通过jvm启动参数设置此环境变量
        // System.setProperty("aip.log4j.conf", "path/to/your/log4j.properties");

        // 调用接口
        String text = message;
        JSONObject res = client.lexer(text, null);
        String objecttime="";

        try
        {
            // System.out.println("111"+Nlp_text);
            for (int i=0;i<res.getJSONArray("items").length();i++)
            {
                String Nlp_ne=res.getJSONArray("items").getJSONObject(i).getString("ne");
                String Nlp_item=res.getJSONArray("items").getJSONObject(i).getString("item");

                // Toast.makeText(MainActivity.this, Nlptext,Toast.LENGTH_LONG).show();
                // System.out.println("result+"+Nlptext);

                if (Nlp_ne.equals("TIME"))
                {
                    //System.out.println("result+"+Nlp_item);
                    userobject.setObjecttime(Nlp_item);//记下时间
                    objecttime=Nlp_item;
                }

            }
        }
        catch (JSONException e) {
            e.printStackTrace();
        }
        return  objecttime;
    }
    private static int daysBetween(TimeInfo startDate, TimeInfo endDate) {
         Calendar fromCalendar = Calendar.getInstance();
         fromCalendar.set(startDate.year,startDate.month,startDate.day,0,0,0);


         Calendar toCalendar = Calendar.getInstance();
         toCalendar.set(endDate.year,endDate.month,endDate.day,0,0,0);


         return (int) ((toCalendar.getTime().getTime() - fromCalendar.getTime().getTime()) / (1000 * 60 * 60 * 24));
}

    private static int hoursBetween(TimeInfo startDate, TimeInfo endDate) {
        Calendar fromCalendar = Calendar.getInstance();
        fromCalendar.set(startDate.year,startDate.month,startDate.day,startDate.hour,startDate.minute,startDate.second);


        Calendar toCalendar = Calendar.getInstance();
        toCalendar.set(endDate.year,endDate.month,endDate.day,endDate.hour,endDate.minute,endDate.second);


        return (int) ((toCalendar.getTime().getTime() - fromCalendar.getTime().getTime()) / (1000 * 60 * 60 ));
    }


    private  void Remind(String message) {

        TimeInfo timer =GetDataFromText(message);
        System.out.println("时间"+timer.day+"-"+timer.hour+"-"+ timer.minute);

        TimeInfo nowtime=new TimeInfo();
        Calendar calendar = Calendar.getInstance();
         nowtime.year = calendar.get(Calendar.YEAR);
        nowtime. month = calendar.get(Calendar.MONTH)+1;
        nowtime. day = calendar.get(Calendar.DAY_OF_MONTH);
        nowtime. hour = calendar.get(Calendar.HOUR_OF_DAY);
        nowtime. minute = calendar.get(Calendar.MINUTE);
        nowtime. second = calendar.get(Calendar.SECOND);
        if (hoursBetween(nowtime,timer)>=24)
        {
            AddCalendar(timer);
        }
        else
        {
            //System.out.println("时间"+timer.day+"-"+timer.hour+"-"+ timer.minute);
            if(hoursBetween(nowtime,timer)>=0)
            CreateAlarm(timer.msg,timer.hour,timer.minute);
            else
                addlefttext("您再检查一下时间哦");
        }

    }
    private void CreateAlarm(String msg,int hour,int minute) {
        Intent alarmIntent = new Intent(AlarmClock.ACTION_SET_ALARM);
        alarmIntent.putExtra(AlarmClock.EXTRA_MESSAGE, msg);
        alarmIntent.putExtra(AlarmClock.EXTRA_HOUR, hour);
       // alarmIntent.putExtra(AlarmClock.EXTRA_IS_PM,1);

        alarmIntent.putExtra(AlarmClock.EXTRA_MINUTES, minute);
        alarmIntent.putExtra(EXTRA_SKIP_UI, true);
        alarmIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        alarmIntent.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true);
        startActivity(alarmIntent);
        addlefttext("已为您定下闹钟");
    }


    private void AddCalendar(TimeInfo time) {
        //Calendar cal = Calendar.getInstance();
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(time.year,time.month-1,time.day,8,30);



        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setData(CalendarContract.Events.CONTENT_URI);
        intent.setType("vnd.android.cursor.dir/event");


        intent.putExtra("beginTime", startCalendar.getTimeInMillis());
        intent.putExtra(CalendarContract.Events.ALL_DAY, true);
       // intent.putExtra("rrule", "FREQ=YEARLY");
        intent.putExtra("endTime", startCalendar.getTimeInMillis()+60*60*1000);
        intent.putExtra("title", time.msg);
        intent.putExtra("eventLocation","");

        if (intent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
            MainActivity.this.startActivity(intent);
        }




       // addlefttext("已为您定下日程");

    }


    private void  QueryData(final String message)
    {
        final UserObject userobject =new UserObject();
            String objecttime=GetDataFromNpl(message);

            final  String objectname=message.replaceAll("查询|是|。|什么时候|什么时间|多久了|还有多久","").replaceAll(objecttime,"");
            //objectname=objectname.replaceAll(objecttime,"");//


        final TimeInfo timer =GetDataFromText(message);
        System.out.println("时间"+timer.day+"-"+timer.hour+"-"+ timer.minute);

        final TimeInfo nowtime=new TimeInfo();
        Calendar calendar = Calendar.getInstance();
        nowtime.year = calendar.get(Calendar.YEAR);
        nowtime. month = calendar.get(Calendar.MONTH)+1;
        nowtime. day = calendar.get(Calendar.DAY_OF_MONTH);
        nowtime. hour = calendar.get(Calendar.HOUR_OF_DAY);
        nowtime. minute = calendar.get(Calendar.MINUTE);
        nowtime. second = calendar.get(Calendar.SECOND);

            BmobQuery query =new BmobQuery(UserBaseInfo.UserName);
            query.addWhereEqualTo("objectname", objectname);
            query.setLimit(2);
            query.order("createdAt");
            //v3.5.0版本提供`findObjectsByTable`方法查询自定义表名的数据
            query.findObjectsByTable(new QueryListener<JSONArray>() {
                @Override
                public void done(JSONArray ary, BmobException e) {
                    if(e==null)
                    {
                        Log.i("bmob","查询成功："+ary.toString());
                        Pattern P_time=Pattern.compile("objecttime\":\"(.*?)\"");
                        Matcher time=P_time.matcher(ary.toString());
                        String objecttime="";
                        if(time.find())
                        {
                            objecttime=time.group(1);
                           // if (message.contains(objectname))
                            Log.i("bmob","查询成功："+objecttime);

                           // addlefttext(objectname+"是"+objecttime);
                            if(message.contains("多久了"))
                            {
                                addlefttext(objectname+"是"+objecttime+","+"已经"+Math.abs(daysBetween(nowtime,GetDataFromText(objecttime)))+"天");
                                //addlefttext(objectname+"已经"+Math.abs(daysBetween(nowtime,timer))+"天");
                            }
                           else if(message.contains("还有多久"))
                            {
                                addlefttext(objectname+"是"+objecttime+","+"还有"+Math.abs(daysBetween(nowtime,GetDataFromText(objecttime)))+"天");
                               // addlefttext("还有"+Math.abs(daysBetween(nowtime,timer))+"天");
                            }
                            else if(message.contains(""))
                            {
                                addlefttext(objectname+"是"+objecttime);
                            }
                        }
                        else
                        {
                            Log.i("123456", "done: "+"1234");
                            addlefttext("对不起，, ，没有查到");
                        }

                    }
                    else
                        {
                            Log.i("123456", "done: "+e.toString());
                            Log.i("bmob","失败："+e.getMessage()+","+e.getErrorCode());
                        }
                }
            });

            BmobQuery query2 =new BmobQuery(UserBaseInfo.UserName);
            final String outtime=objecttime;
            query2.addWhereEqualTo("objecttime", GetDataFromText(objecttime).year+"-"+GetDataFromText(objecttime).month+"-"+GetDataFromText(objecttime).day);
            query2.setLimit(2);
            query2.order("createdAt");
            //v3.5.0版本提供`findObjectsByTable`方法查询自定义表名的数据
            query2.findObjectsByTable(new QueryListener<JSONArray>() {
                @Override
                public void done(JSONArray ary, BmobException e) {
                    if(e==null)
                    {
                        Log.i("bmob","查询成功："+ary.toString());
                        Pattern P_name=Pattern.compile("objectname\":\"(.*?)\"");
                        Matcher name=P_name.matcher(ary.toString());
                        String objectname="";
                        while(name.find())
                        {
                            objectname=name.group().replaceAll("objectname","").replaceAll("\"|:","");
                            // if (message.contains(objectname))
                           // Log.i("bmob","查询成功："+objectname);
                            addlefttext(outtime+"是"+objectname);
                        }


                    }
                    else
                    {
                        Log.i("bmob","失败："+e.getMessage()+","+e.getErrorCode());
                    }
                }
            });

    }
    private void UpLoadData(String message){
       final UserObject userobject =new UserObject();

        StrictMode.ThreadPolicy policy=new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);//加上这两行；得知Android不能再主线程去访问网络，需要另起线程去访问
        final String APP_ID = "14202177";
        final String API_KEY = "2zuwF5GEN6ndGOZCir7a0ZR4";
        final String SECRET_KEY = "5KM9TpynDvzjfY7DzzZ55LvLcC0m1j89";
        AipNlp client = new AipNlp(APP_ID, API_KEY, SECRET_KEY);

        // 可选：设置网络连接参数
        client.setConnectionTimeoutInMillis(2000);
        client.setSocketTimeoutInMillis(60000);

        // 可选：设置代理服务器地址, http和socket二选一，或者均不设置
        // client.setHttpProxy("proxy_host", proxy_port);  // 设置http代理
        // client.setSocketProxy("proxy_host", proxy_port);  // 设置socket代理

        // 可选：设置log4j日志输出格式，若不设置，则使用默认配置
        // 也可以直接通过jvm启动参数设置此环境变量
        // System.setProperty("aip.log4j.conf", "path/to/your/log4j.properties");

        // 调用接口
        String text = message;
        JSONObject res = client.lexer(text, null);





        try{
            final  String objectdata=res.getString("text");



             String objecttime="";
            // System.out.println("111"+Nlp_text);
            for (int i=0;i<res.getJSONArray("items").length();i++)
            {
                String Nlp_ne=res.getJSONArray("items").getJSONObject(i).getString("ne");
                String Nlp_item=res.getJSONArray("items").getJSONObject(i).getString("item");

                // Toast.makeText(MainActivity.this, Nlptext,Toast.LENGTH_LONG).show();
               // System.out.println("result+"+Nlptext);

                if (Nlp_ne.equals("TIME"))
                {
                    //System.out.println("result+"+Nlp_item);
                    TimeInfo time=GetDataFromText(Nlp_item);
                    String formittime=time.year+"-"+time.month+"-"+time.day;
                    userobject.setObjecttime(formittime);//记下时间
                    objecttime=Nlp_item;
                }
            }
            final  String objectname=objectdata.replaceAll("记住|是|。","").replaceAll(objecttime,"");
            //objectname=objectname.replaceAll(objecttime,"");//


            BmobQuery query =new BmobQuery(UserBaseInfo.UserName);
            query.addWhereEqualTo("objectname", objectname);
            query.setLimit(2);
            query.order("createdAt");
            //v3.5.0版本提供`findObjectsByTable`方法查询自定义表名的数据
            query.findObjectsByTable(new QueryListener<JSONArray>() {
                @Override
                public void done(JSONArray ary, BmobException e) {
                    if(e==null){
                        Log.i("bmob","查询成功："+ary.toString());
                        Pattern ID=Pattern.compile("objectId\":\"(.*?)\"");
                        Matcher id=ID.matcher(ary.toString());
                        String objectid="";
                        if(id.find())
                        {
                           // Log.i("bmob","查询成功："+id.group(1));
                            objectid=id.group(1);
                            userobject.setObjectname(objectname);
                            userobject.setObjectData(objectdata);
                            userobject.update(objectid, new UpdateListener() {

                                @Override
                                public void done(BmobException e) {
                                    if(e==null){
                                        Log.i("bmob","更新成功");
                                    }else{
                                        Log.i("bmob","更新失败："+e.getMessage()+","+e.getErrorCode());
                                    }
                                }
                            });
                            addlefttext("已为您记下");
                        }
                        else
                        {
                            userobject.setObjectname(objectname);
                            userobject.setObjectData(objectdata);
                            userobject.save(new SaveListener<String>() {

                                @Override
                                public void done(String objectId, BmobException e) {
                                    if(e==null){
                                        // Toast.makeText(MainActivity.this, "发布信息成功",Toast.LENGTH_LONG).show();
                                    }else{
                                        //   Log.i("bmob","失败："+e.getMessage()+","+e.getErrorCode());
                                    }
                                }
                            });
                            addlefttext("已为您记下");

                        }

                    }else{

                        Log.i("bmob","失败："+e.getMessage()+","+e.getErrorCode());
                    }
                }
            });


        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    private void pasingJson(String message){
        JSONObject jsonObject = null;
        try {
            jsonObject = new JSONObject(message);
            //通过key（text）获取value
            String text = jsonObject.getString("text");
            addlefttext(text);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    //添加右侧消息
    private void addrighttext(String message) {
        Msg data = new Msg();
        data.setType(MsgAdapter.chat_right);
        data.setText(message);
        mList.add(data);
        //通知adapter刷新页面
        adapter.notifyDataSetChanged();
        lv_chat_list.setSelection(lv_chat_list.getBottom());

    }

    //添加左侧消息
    private void addlefttext(String message) {
        Msg data = new Msg();
        data.setType(MsgAdapter.chat_left);
        data.setText(message);
        mList.add(data);
        adapter.notifyDataSetChanged();
        lv_chat_list.setSelection(lv_chat_list.getBottom());

    }



    private InitListener mInitListener = new InitListener() {

        @Override
        public void onInit(int code) {
            Log.d(TAG, "SpeechRecognizer init() code = " + code);
            if (code != ErrorCode.SUCCESS) {
                showTip("初始化失败，错误码：" + code);
            }
        }
    };

    /**

     * 听写监听器。
     */
    private RecognizerListener mRecognizerListener = new RecognizerListener() {

        @Override
        public void onBeginOfSpeech() {
            // 此回调表示：sdk内部录音机已经准备好了，用户可以开始语音输入
            showTip("开始说话");
        }

        @Override
        public void onError(SpeechError error) {

            // Tips：
            // 错误码：10118(您没有说话)，可能是录音机权限被禁，需要提示用户打开应用的录音权限。
            if(mTranslateEnable && error.getErrorCode() == 14002) {
                showTip( error.getPlainDescription(true)+"\n请确认是否已开通翻译功能" );
            } else {
                showTip(error.getPlainDescription(true));
            }

        }

        @Override
        public void onEndOfSpeech() {
            // 此回调表示：检测到了语音的尾端点，已经进入识别过程，不再接受语音输入
            //showTip("结束说话");
            String message = ed_send.getText().toString().trim();
            OnClickSend(message);

        }

        @Override
        public void onResult(RecognizerResult results, boolean isLast) {
            Log.d(TAG, results.getResultString());
            if( mTranslateEnable ){
                printTransResult( results );
            }else{
                printResult(results);
            }

            if (isLast) {
                // TODO 最后的结果
                ed_send.setText("");

            }
        }

        @Override
        public void onVolumeChanged(int volume, byte[] data) {
            showTip("当前正在说话，音量大小：" + volume);
            Log.d(TAG, "返回音频数据："+data.length);
        }

        @Override
        public void onEvent(int eventType, int arg1, int arg2, Bundle obj) {
            // 以下代码用于获取与云端的会话id，当业务出错时将会话id提供给技术支持人员，可用于查询会话日志，定位出错原因
            // 若使用本地能力，会话id为null
            //	if (SpeechEvent.EVENT_SESSION_ID == eventType) {
            //		String sid = obj.getString(SpeechEvent.KEY_EVENT_SESSION_ID);
            //		Log.d(TAG, "session id =" + sid);
            //	}
        }
    };

    private void printResult(RecognizerResult results) {
        String text = JsonParser.parseIatResult(results.getResultString());

        String sn = null;
        // 读取json结果中的sn字段
        try {
            JSONObject resultJson = new JSONObject(results.getResultString());
            sn = resultJson.optString("sn");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mIatResults.put(sn, text);

        StringBuffer resultBuffer = new StringBuffer();
        for (String key : mIatResults.keySet()) {
            resultBuffer.append(mIatResults.get(key));
        }

        ed_send.setText(resultBuffer.toString());
        ed_send.setSelection(ed_send.length());


    }

    /**
     * 听写UI监听器
     */
    private RecognizerDialogListener mRecognizerDialogListener = new RecognizerDialogListener() {
        public void onResult(RecognizerResult results, boolean isLast) {
            if( mTranslateEnable ){
                printTransResult( results );
            }else{
                printResult(results);
            }

        }

        /**
         * 识别回调错误.
         */
        public void onError(SpeechError error) {
            if(mTranslateEnable && error.getErrorCode() == 14002) {
                showTip( error.getPlainDescription(true)+"\n请确认是否已开通翻译功能" );
            } else {
                showTip(error.getPlainDescription(true));
            }

        }


    };





    /**
     * 参数设置
     *
     * @return
     */
    public void setParam() {
        // 清空参数
        mIat.setParameter(SpeechConstant.PARAMS, null);

        // 设置听写引擎
        mIat.setParameter(SpeechConstant.ENGINE_TYPE, mEngineType);
        // 设置返回结果格式
        mIat.setParameter(SpeechConstant.RESULT_TYPE, "json");

        this.mTranslateEnable = mSharedPreferences.getBoolean( this.getString(R.string.pref_key_translate), false );
        if( mTranslateEnable ){
            Log.i( TAG, "translate enable" );
            mIat.setParameter( SpeechConstant.ASR_SCH, "1" );
            mIat.setParameter( SpeechConstant.ADD_CAP, "translate" );
            mIat.setParameter( SpeechConstant.TRS_SRC, "its" );
        }

        String lag = mSharedPreferences.getString("iat_language_preference",
                "mandarin");
        if (lag.equals("en_us")) {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "en_us");
            mIat.setParameter(SpeechConstant.ACCENT, null);

            if( mTranslateEnable ){
                mIat.setParameter( SpeechConstant.ORI_LANG, "en" );
                mIat.setParameter( SpeechConstant.TRANS_LANG, "cn" );
            }
        } else {
            // 设置语言
            mIat.setParameter(SpeechConstant.LANGUAGE, "zh_cn");
            // 设置语言区域
            mIat.setParameter(SpeechConstant.ACCENT, lag);

            if( mTranslateEnable ){
                mIat.setParameter( SpeechConstant.ORI_LANG, "cn" );
                mIat.setParameter( SpeechConstant.TRANS_LANG, "en" );
            }
        }
        //此处用于设置dialog中不显示错误码信息
        mIat.setParameter("view_tips_plain","false");

        // 设置语音前端点:静音超时时间，即用户多长时间不说话则当做超时处理
        mIat.setParameter(SpeechConstant.VAD_BOS, mSharedPreferences.getString("iat_vadbos_preference", "4000"));

        // 设置语音后端点:后端点静音检测时间，即用户停止说话多长时间内即认为不再输入， 自动停止录音
        mIat.setParameter(SpeechConstant.VAD_EOS, mSharedPreferences.getString("iat_vadeos_preference", "1000"));

        // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
        mIat.setParameter(SpeechConstant.ASR_PTT, mSharedPreferences.getString("iat_punc_preference", "1"));

        // 设置音频保存路径，保存音频格式支持pcm、wav，设置路径为sd卡请注意WRITE_EXTERNAL_STORAGE权限
        // 注：AUDIO_FORMAT参数语记需要更新版本才能生效
        mIat.setParameter(SpeechConstant.AUDIO_FORMAT,"wav");
        mIat.setParameter(SpeechConstant.ASR_AUDIO_PATH, Environment.getExternalStorageDirectory()+"/msc/iat.wav");
    }

    private void printTransResult (RecognizerResult results) {
        String trans  = JsonParser.parseTransResult(results.getResultString(),"dst");
        String oris = JsonParser.parseTransResult(results.getResultString(),"src");

        if( TextUtils.isEmpty(trans)||TextUtils.isEmpty(oris) ){
            showTip( "解析结果失败，请确认是否已开通翻译功能。" );
        }else{
            ed_send.setText( "原始语言:\n"+oris+"\n目标语言:\n"+trans );
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if( null != mIat ){
            // 退出时释放连接
            mIat.cancel();
            mIat.destroy();
        }
    }
}
