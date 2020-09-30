package com.asd412id.absenqr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

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

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        queue = Volley.newRequestQueue(this);
        configs = getApplicationContext().getSharedPreferences("configs", Context.MODE_PRIVATE);
        editor = configs.edit();

//        ip_server = configs.getString("ip_server","absenqr.webarsip.com"+api);
        ip_server = "absenqr.webarsip.com"+api;
        _token = configs.getString("_token",null);
        progress_wrap = findViewById(R.id.progress_wrap);
        noserver_wrap = findViewById(R.id.noserver_wrap);
        server_addr = findViewById(R.id.server_addr);
        server_addr_submit = findViewById(R.id.server_addr_submit);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if (ip_server == null) {
            findServer();
        }else{
            connectServer(ip_server);
        }

    }

    private void connectServer(final String ip_server) {
        String url = "https://"+ip_server+"check-server";
        request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                serverNotFound();
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
                        if (error.networkResponse.statusCode==302 && errorJson.getString("status").equals("connected")){
                            editor.putString("ip_server",ip_server);
                            editor.putString("nama_instansi",errorJson.getString("nama_instansi"));
                            editor.putString("background",errorJson.getString("background"));
                            editor.commit();
                            serverFound();
                            return;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    serverNotFound();
                }
            }
        });
        request.setRetryPolicy(new DefaultRetryPolicy(3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(request);
    }

    private void findServer(){
        WifiManager wifimanager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);
        if (wifimanager != null) {
            subnet = getSubnetAddress(wifimanager.getDhcpInfo().gateway);
        }

        checkServer(subnet,1);
    }

    private void checkServer(String subnet,int i){
        if (i>=255){
            serverNotFound();
            return;
        }
        final String addr = subnet+"."+i+api;
        String url = "https://"+addr+"check-server";
        final String addrFinal = subnet;
        final int[] ip = {i};
        request = new JsonObjectRequest(Request.Method.GET, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                ip[0]++;
                checkServer(addrFinal, ip[0]);
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof NoConnectionError || error.networkResponse==null){
                    ip[0]++;
                    checkServer(addrFinal, ip[0]);
                }else{
                    VolleyError volleyError = new VolleyError(new String(error.networkResponse.data));
                    try {
                        JSONObject errorJson = new JSONObject(Objects.requireNonNull(volleyError.getMessage()));
                        if (error.networkResponse.statusCode==302 && errorJson.getString("status").equals("connected")){
                            editor.putString("ip_server",addr);
                            editor.putString("nama_instansi",errorJson.getString("nama_instansi"));
                            editor.putString("background",errorJson.getString("background"));
                            editor.commit();
                            serverFound();
                            return;
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    ip[0]++;
                    checkServer(addrFinal, ip[0]);
                }
            }
        });
        request.setRetryPolicy(new DefaultRetryPolicy(200,
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
                if (String.valueOf(server_addr.getText()).equals("")){
                    findServer();
                }else {
                    ip_server = server_addr.getText()+api;
                    connectServer(ip_server);
                }
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
        String url = "https://"+ip_server+"user";
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

    @SuppressLint("DefaultLocale")
    private String getSubnetAddress(int address)
    {
        return String.format(
            "%d.%d.%d",
            (address & 0xff),
            (address >> 8 & 0xff),
            (address >> 16 & 0xff));
    }

}
