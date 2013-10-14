package com.skripsie.nfcpay6;

import android.app.Application;

public class GlobalVar extends Application 
{
    private String userPass;
    private String userName;

    public String getUserName() 
    {
        return userName;
    }
    
    public String getUserPassword() 
    {
        return userPass;
    }

    public void setUserName(String userName) 
    {
        this.userName = userName;
    }
    
    public void setUserPassword(String userPass) 
    {
        this.userPass = userPass;
    }
}