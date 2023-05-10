package com.asd412id.absenqr;

import static com.google.gson.internal.$Gson$Types.arrayOf;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    SharedPreferences configs;
    SharedPreferences.Editor editor;
    RequestQueue queue;
    JsonObjectRequest request;
    String subnet = null;
    String ip_server;
    String _token;
    LinearLayout progress_wrap;
    LinearLayout noserver_wrap;
    EditText server_addr;
    Button server_addr_submit;
    String api = "/api/v1/";
    FusedLocationProviderClient fusedLocationClient;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        int devSettings = Settings.Global.getInt(getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED , 0);
        if(devSettings == 1 || locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) && locationManager.isProviderEnabled("mock")){
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
            queue = Volley.newRequestQueue(this);
            configs = getApplicationContext().getSharedPreferences("configs", Context.MODE_PRIVATE);
            editor = configs.edit();

//        ip_server = "http://10.0.2.2:8000"+api;
            ip_server = "https://absen.smpn39sinjai.sch.id"+api;
            _token = configs.getString("_token",null);
            progress_wrap = findViewById(R.id.progress_wrap);
            noserver_wrap = findViewById(R.id.noserver_wrap);
            server_addr = findViewById(R.id.server_addr);
            server_addr_submit = findViewById(R.id.server_addr_submit);

            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);

            connectServer(ip_server);
        }
    }

    private void connectServer(final String ip_server) {
        String url = ip_server+"check-server";
        request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    editor.putString("ip_server",ip_server);
                    editor.putString("nama_instansi",response.getString("nama_instansi"));
                    editor.putString("background",response.getString("background"));
                    editor.commit();
                    serverFound();
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                serverNotFound();
            }
        });
        request.setRetryPolicy(new DefaultRetryPolicy(3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(request);
    }

    private void serverNotFound() {
        if (ip_server!=null){
            server_addr.setText(ip_server.split(api)[0]);
        }
        progress_wrap.setVisibility(View.GONE);
        noserver_wrap.setVisibility(View.VISIBLE);
        server_addr_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progress_wrap.setVisibility(View.VISIBLE);
                noserver_wrap.setVisibility(View.GONE);
                ip_server = server_addr.getText()+api;
                connectServer(ip_server);
            }
        });
    }

    private void serverFound(){
        if (_token==null){
            errorPage("login");
        }else{
            checkLogin();
        }
    }

    private void checkLogin() {
        String url = ip_server+"user";
        request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                JSONObject data;
                try {
                    data = response.getJSONObject("data");
                    editor.putString("name",data.getString("name"));
                    editor.commit();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                loginSuccess();
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof NoConnectionError || error.networkResponse==null){
                    serverNotFound();
                }else{
                    VolleyError volleyError = new VolleyError(new String(error.networkResponse.data));
                    try {
                        JSONObject errorJson = new JSONObject(Objects.requireNonNull(volleyError.getMessage()));
                        if (error.networkResponse.statusCode==401){
                            if (errorJson.getString("message").equals("Unauthenticated")){
                                errorPage("login");
                            }else if (errorJson.getString("status").equals("error")){
                                errorPage(errorJson.getString("error"));
                            }
                            return;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    errorPage("login");
                }
            }
        }){
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Accept", "application/json");
                params.put("Authorization", _token);
                return params;
            }
        };
        request.setRetryPolicy(new DefaultRetryPolicy(3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(request);
    }

    private void loginSuccess() {
        Intent intent = new Intent(MainActivity.this,DashboardActivity.class);
        startActivity(intent);
        finishAffinity();
    }

    private void errorPage(String error){
        Intent intent;
        if (error.equals("login")){
            intent = new Intent(MainActivity.this,LoginActivity.class);
        }else{
            intent = new Intent(MainActivity.this,AfterLoginActivity.class);
            intent.putExtra("error",error);
        }
        startActivity(intent);
        finishAffinity();
    }

}
