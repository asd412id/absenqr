package com.asd412id.absenqr;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
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

public class LoginActivity extends AppCompatActivity {

    SharedPreferences configs;
    SharedPreferences.Editor editor;

    Button btnLogin;
    EditText username;
    EditText password;
    TextView login_subtitle;
    String ip_server;

    @SuppressLint("CommitPrefEdits")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        configs = getApplicationContext().getSharedPreferences("configs", Context.MODE_PRIVATE);
        editor = configs.edit();
        ip_server = configs.getString("ip_server",null);

        login_subtitle = findViewById(R.id.login_subtitle);
        btnLogin = findViewById(R.id.btn_login);
        username = findViewById(R.id.username);
        password = findViewById(R.id.password);

        login_subtitle.setText(configs.getString("nama_instansi", String.valueOf(R.string.login_subtitle)));

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                btnLogin.setText(R.string.btn_login_text_process);
                username.setEnabled(false);
                password.setEnabled(false);
                btnLogin.setEnabled(false);
                loginProcess();
            }
        });
    }

    private void loginProcess() {
        String url = "https://"+ip_server+"login";

        RequestQueue queue = Volley.newRequestQueue(LoginActivity.this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getString("status").equals("success")){
                        JSONObject data = response.getJSONObject("data");
                        editor.putString("_token","Bearer "+response.getString("_token"));
                        editor.putString("name",data.getString("name"));
                        editor.commit();
                        Intent intent = new Intent(LoginActivity.this,AfterLoginActivity.class);
                        intent.putExtra("error","activate");
                        startActivity(intent);
                        finishAffinity();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof NoConnectionError){
                    loginError("network");
                }else if (error.networkResponse!=null && error.networkResponse.data!=null){
                    VolleyError volleyError = new VolleyError(new String(error.networkResponse.data));
                    try {
                        JSONObject errorJson = new JSONObject(Objects.requireNonNull(volleyError.getMessage()));
                        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
                        builder.setTitle("Login Gagal!")
                                .setMessage(errorJson.getString("message")+"!")
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                })
                                .show();
                        btnLogin.setText(R.string.btn_login_text);
                        password.setText("");
                        username.setEnabled(true);
                        password.setEnabled(true);
                        btnLogin.setEnabled(true);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else{
                    loginError("server");
                }
            }
        }){
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Accept", "application/json");
                params.put("Username", String.valueOf(username.getText()));
                params.put("Password", String.valueOf(password.getText()));
                return params;
            }
        };
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        queue.add(jsonObjectRequest);
    }

    private void loginError(String status){
        String msg;
        switch (status){
            case "network":
                msg = "Jaringan tidak terhubung!";
                break;
            case "server":
                msg = "Server tidak ditemukan!";
                break;

            default:
                msg = "Terjadi kesalahan!";
                break;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(LoginActivity.this);
        builder.setTitle("Login Gagal!")
                .setMessage(msg)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .show();
        btnLogin.setText(R.string.btn_login_text);
        btnLogin.setEnabled(true);
    }
}
