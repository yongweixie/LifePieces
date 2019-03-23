package com.example.xieyo.lifepieces;

import cn.bmob.v3.BmobObject;

public class BillObject extends BmobObject {
    private String billTime;
    private String billamount;
    private String billAim;
    private  String billType;
    public BillObject(){this.setTableName(UserBaseInfo.BillName);}
    public void setbillTime(String billTime){this.billTime=billTime;}
    public void setbillamount(String billamount){this.billamount=billamount;}
    public void setbillAim(String billAim) {this.billAim=billAim;}
    public void setbillType(String billType) {this.billType=billType;}

}
