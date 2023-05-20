package com.asd412id.absenqr;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.AppOpsManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    SharedPreferences configs;
    SharedPreferences.Editor editor;
    RequestQueue queue;
    JsonObjectRequest request;
    String ip_server;
    String _token;
    LinearLayout progress_wrap;
    LinearLayout noserver_wrap;
    EditText server_addr;
    Button server_addr_submit;
    String api = "/api/v1/";
    private static final String CHANNEL_ID = "NOTIFICATION";
    private static final String TAG = "MainActivity";
    private NotificationManager notificationManager;
    private static AlarmManager alarmManager;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel();
        }

        // Create alarm manager
        alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);

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
            queue = Volley.newRequestQueue(this);
            configs = getApplicationContext().getSharedPreferences("configs", Context.MODE_PRIVATE);
            editor = configs.edit();

            if (BuildConfig.BUILD_TYPE.equals("debug")){
                ip_server = getString(R.string.api_dev)+api;
            }else{
                ip_server = getString(R.string.api_prod)+api;
            }
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

    @RequiresApi(Build.VERSION_CODES.O)
    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "AbsenQR Alarm Notification Channel",
                NotificationManager.IMPORTANCE_HIGH
        );
        channel.setDescription("Notification channel for AbsenQR alarm notifications");
        channel.setSound(null,null);
        channel.setVibrationPattern(new long[]{1000, 1000, 1000, 1000, 1000});
        channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
        channel.setShowBadge(true);
        channel.setBypassDnd(true);
        channel.setLightColor(Color.WHITE);
        notificationManager.createNotificationChannel(channel);
    }

    public boolean isMockLocationEnabled() {
        boolean isMockLocation = false;
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

    private void connectServer(final String ip_server) {
        String url = ip_server+"check-server";
        request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                editor.putString("ip_server",ip_server);
                try {
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

    private void createAlarm(JSONArray jadwals) throws JSONException, ParseException {
        if (jadwals.length() > 0){
            @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            Intent intent = new Intent(this, NotificationReceiver.class);
            for (int i = 0; i < jadwals.length(); i++) {
                JSONObject jadwal = jadwals.getJSONObject(i);
                intent.putExtra("ruang",jadwal.getString("ruang"));
                intent.putExtra("name",jadwal.getString("name"));
                intent.putExtra("code", jadwal.getInt("id"));
                String alarmTime = jadwal.getString("start_cin");
                Date date;
                try {
                    date = format.parse(alarmTime);
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing datetime", e);
                    return;
                }
                assert date != null;
                long milliseconds = date.getTime();
                intent.setAction("START_ALARM");

                PendingIntent pendingIntent = PendingIntent.getBroadcast(this, jadwal.getInt("id"), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                alarmManager.set(AlarmManager.RTC_WAKEUP, milliseconds, pendingIntent);
            }
        }
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
                try {
                    editor.putString("jadwals", String.valueOf(response.getJSONArray("jadwals_today")));
                    editor.commit();

                    createAlarm(response.getJSONArray("jadwals"));
                } catch (JSONException e) {
                    e.printStackTrace();
                } catch (ParseException e) {
                    throw new RuntimeException(e);
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
                        Log.i("AERROR", String.valueOf(errorJson));
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
