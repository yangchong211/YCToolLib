package com.ycbjie.ycgroupadapter;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.yc.zxingserver.demo.EasyCaptureActivity;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.tv_1).setOnClickListener(this);
        findViewById(R.id.tv_2).setOnClickListener(this);
        findViewById(R.id.tv_3).setOnClickListener(this);
        findViewById(R.id.tv_4).setOnClickListener(this);
        findViewById(R.id.tv_5).setOnClickListener(this);
        findViewById(R.id.tv_6).setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.tv_1:
                startActivity(new Intent(this, EasyCaptureActivity.class));
                break;
            case R.id.tv_2:
                startActivity(new Intent(this,SecondActivity.class));
                break;
            case R.id.tv_3:
                startActivity(new Intent(this,ThirdActivity.class));
                break;
            case R.id.tv_4:
                startActivity(new Intent(this,FourActivity.class));
                break;
            case R.id.tv_5:
                startActivity(new Intent(this,FiveActivity.class));
                break;
            case R.id.tv_6:
                startActivity(new Intent(this,SixActivity.class));
                break;
            default:
                break;
        }
    }
    
}
