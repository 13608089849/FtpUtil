package com.example.agentzengyu.ftputil;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.zy.FtpUtil;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        File file = new File("D:\\LIBRARY_ESCAPE");
        System.out.println("getAbsolutePath"+file.getAbsolutePath());
        FtpUtil ftpUtil = new FtpUtil();
        ftpUtil.SetParams(21, "35.48.82.15", "ropeokRead", "1234", "\\LIBRARY_ESCAPE", "D:\\LIBRARY_ESCAPE");
        ftpUtil.Download();
    }
}
