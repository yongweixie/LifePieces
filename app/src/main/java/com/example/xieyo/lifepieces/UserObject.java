package com.example.xieyo.lifepieces;

import cn.bmob.v3.BmobObject;

public class UserObject extends   BmobObject {
    private String objecttime;
    private String objectname;
    private String objectdata;

    public UserObject() {
        this.setTableName(UserBaseInfo.UserName);
    }
    public void setObjecttime(String objecttime){this.objecttime=objecttime;}
    public void setObjectname(String objectname){this.objectname=objectname;}
    public void setObjectData(String objectdata) {this.objectdata=objectdata;}
    public String getObjectName() {
        return objectname;
    }
    public String getObjecttime() {
        return objecttime;
    }

    public String getObjectdata() {
        return objectdata;
    }

}
