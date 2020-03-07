package com.asd412id.absenqr;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.media.Image;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class DashboardActivity extends AppCompatActivity {
    SharedPreferences configs;
    Bundle absen;

    TextView user_name;
    TextView dashboard_subtitle;
    TextView scan_hint;
    Button btn_scan;
    Button btn_exit;
    ImageView dashboard_img;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        configs = getApplicationContext().getSharedPreferences("configs", Context.MODE_PRIVATE);
        user_name = findViewById(R.id.user_name);
        dashboard_subtitle = findViewById(R.id.dashboard_subtitle);
        scan_hint = findViewById(R.id.scan_hint);
        btn_scan = findViewById(R.id.btn_scan);
        btn_exit = findViewById(R.id.btn_exit);
        dashboard_img = findViewById(R.id.dashboard_img);

        absen = getIntent().getExtras();

        user_name.setText(configs.getString("name","Nama User"));
        dashboard_subtitle.setText(configs.getString("nama_instansi",null));

        if (absen==null){
            scan_hint.setText(Html.fromHtml("Sentuh <b>\"Mulai Absen\"</b> untuk melakukan scan QR Code!"));
        }else {
            if (absen.getString("status").equals("error")){
                scan_hint.setText(Html.fromHtml("<b>"+absen.getString("error")));
                dashboard_img.setImageResource(R.drawable.error);
            }else{
                btn_scan.setVisibility(View.GONE);
                btn_exit.setVisibility(View.VISIBLE);
                scan_hint.setText(Html.fromHtml("<b>"+absen.getString("ruang")+"</b><br/>Waktu: "+absen.getString("time")));
                dashboard_img.setImageResource(R.drawable.success);
            }
        }

        btn_scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(DashboardActivity.this,QrScannerActivity.class);
                startActivity(intent);
            }
        });
        btn_exit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finishAffinity();
                System.exit(0);
            }
        });

    }
}
