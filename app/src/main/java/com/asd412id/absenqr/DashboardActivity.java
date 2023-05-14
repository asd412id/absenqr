package com.asd412id.absenqr;

import android.Manifest;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

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

        if(isMockLocationEnabled()){
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("Tobatlah!!! Anda ingin memalsukan absensi.")
                    .setCancelable(false)
                    .setPositiveButton("TUTUP APLIKASI", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            // If user clicks "Yes", terminate the application
                            finish();
                            System.exit(0);
                        }
                    });
            AlertDialog alert = builder.create();
            alert.show();
        }else{

            if(ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},15);
                }
            }

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
                scan_hint.setText(Html.fromHtml("Pastikan Berada di Lokasi Absensi!<br/><br/>Sentuh <b>\"Mulai Absen\"</b> untuk melakukan scan QR Code!"));
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

    private boolean isMockLocationEnabled() {
        boolean isMockLocation;
        try {
            //if marshmallow
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                AppOpsManager opsManager = (AppOpsManager) getApplicationContext().getSystemService(Context.APP_OPS_SERVICE);
                isMockLocation = (opsManager.checkOp(AppOpsManager.OPSTR_MOCK_LOCATION, android.os.Process.myUid(), BuildConfig.APPLICATION_ID)== AppOpsManager.MODE_ALLOWED);
            } else {
                // in marshmallow this will always return true
                isMockLocation = !android.provider.Settings.Secure.getString(getApplicationContext().getContentResolver(), "mock_location").equals("0");
            }
        } catch (Exception e) {
            return false;
        }
        return isMockLocation;
    }
}
