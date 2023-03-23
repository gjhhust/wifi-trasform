package com.zjh.btim.textmessage;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.zjh.btim.R;

public class Setting extends AppCompatActivity {
    public static Activity main;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
//        设置标题栏
        getSupportActionBar().setTitle("端口设置");
//        获取输入框并设置成端口号
        final EditText input_port=findViewById(R.id.port_number);
        input_port.setText(String.valueOf(MainWifiActivity.port));
        Button confirm=findViewById(R.id.confirm_change);
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                int port=0;
                try{
                    port=Integer.valueOf(input_port.getText().toString());
                } catch (NumberFormatException e){
                    Toast.makeText(Setting.this,"请输入整数",Toast.LENGTH_LONG).show();
                    return;
                }
                MainWifiActivity.port=port;
                main.finish();
                finish();
            }
        });

    }
}
