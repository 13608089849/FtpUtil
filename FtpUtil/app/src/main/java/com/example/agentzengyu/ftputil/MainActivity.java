package com.example.agentzengyu.ftputil;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.zy.FtpUtil;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FtpUtil ftpUtil = new FtpUtil();
        ftpUtil.setParams(21,"","","","","");
        ftpUtil.download();
    }
}
