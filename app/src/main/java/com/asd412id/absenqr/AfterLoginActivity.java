package com.asd412id.absenqr;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

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

public class AfterLoginActivity extends AppCompatActivity {
    SharedPreferences configs;
    Bundle error;
    String ip_server;
    String _token;

    LinearLayout activation_wrap;
    LinearLayout change_password_wrap;

    EditText activation_code;
    EditText new_password;

    Button activation_submit;
    Button new_password_submit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_after_login);

        configs = getApplicationContext().getSharedPreferences("configs", Context.MODE_PRIVATE);
        ip_server = configs.getString("ip_server",null);
        _token = configs.getString("_token",null);

        error = getIntent().getExtras();
        activation_wrap = findViewById(R.id.activation_wrap);
        change_password_wrap = findViewById(R.id.change_password_wrap);
        activation_code = findViewById(R.id.activate_code);
        new_password = findViewById(R.id.new_password);
        activation_submit = findViewById(R.id.activation_submit);
        new_password_submit = findViewById(R.id.new_password_submit);

        if (Objects.equals(error.getString("error"), "new_password")){
            activation_wrap.setVisibility(View.GONE);
            change_password_wrap.setVisibility(View.VISIBLE);
        }else{
            activation_wrap.setVisibility(View.VISIBLE);
            change_password_wrap.setVisibility(View.GONE);
        }

        activation_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activateProcess();
            }
        });
        new_password_submit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changePassword();
            }
        });

    }

    private void activateProcess() {
        String url = "https://"+ip_server+"activate";

        RequestQueue queue = Volley.newRequestQueue(AfterLoginActivity.this);

        activation_code.setEnabled(false);
        activation_submit.setEnabled(false);
        activation_submit.setText(R.string.btn_login_text_process);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getString("status").equals("success")){
                        activation_code.setEnabled(true);
                        activation_submit.setEnabled(true);
                        activation_submit.setText(R.string.konfirmasi_kode);
                        JSONObject data = response.getJSONObject("data");
                        if (data.getInt("changed_password")==0){
                            activation_wrap.setVisibility(View.GONE);
                            change_password_wrap.setVisibility(View.VISIBLE);
                        }else{
                            loginSuccess();
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof NoConnectionError){
                    loginError();
                }else if (error.networkResponse!=null && error.networkResponse.data!=null){
                    VolleyError volleyError = new VolleyError(new String(error.networkResponse.data));
                    try {
                        JSONObject errorJson = new JSONObject(Objects.requireNonNull(volleyError.getMessage()));
                        AlertDialog.Builder builder = new AlertDialog.Builder(AfterLoginActivity.this);
                        builder.setTitle("Kesalahan!")
                                .setMessage(errorJson.getString("message")+"!")
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                })
                                .show();
                        activation_code.setText("");
                        activation_code.setEnabled(true);
                        activation_submit.setEnabled(true);
                        activation_submit.setText(R.string.konfirmasi_kode);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else{
                    loginError();
                }
            }
        }){
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Accept", "application/json");
                params.put("Authorization", _token);
                params.put("Activate-Key", String.valueOf(activation_code.getText()));
                return params;
            }
        };
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);
    }

    private void changePassword() {
        String url = "https://"+ip_server+"change-password";

        RequestQueue queue = Volley.newRequestQueue(AfterLoginActivity.this);

        new_password.setEnabled(false);
        new_password_submit.setEnabled(false);
        new_password_submit.setText(R.string.btn_login_text_process);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.PUT, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getString("status").equals("success")){
                        new_password.setEnabled(true);
                        new_password_submit.setEnabled(true);
                        new_password_submit.setText(R.string.save);
                        loginSuccess();
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof NoConnectionError){
                    loginError();
                }else if (error.networkResponse!=null && error.networkResponse.data!=null){
                    VolleyError volleyError = new VolleyError(new String(error.networkResponse.data));
                    try {
                        JSONObject errorJson = new JSONObject(Objects.requireNonNull(volleyError.getMessage()));
                        AlertDialog.Builder builder = new AlertDialog.Builder(AfterLoginActivity.this);
                        builder.setTitle("Kesalahan!")
                                .setMessage(errorJson.getString("message")+"!")
                                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.cancel();
                                    }
                                })
                                .show();
                        new_password.setEnabled(true);
                        new_password_submit.setEnabled(true);
                        new_password_submit.setText(R.string.save);

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }else{
                    loginError();
                }
            }
        }){
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Accept", "application/json");
                params.put("Authorization", _token);
                params.put("New-Password", String.valueOf(new_password.getText()));
                return params;
            }
        };
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);
    }

    private void loginError() {
        Intent intent = new Intent(AfterLoginActivity.this,MainActivity.class);
        startActivity(intent);
        finishAffinity();
    }

    private void loginSuccess() {
        Intent intent = new Intent(AfterLoginActivity.this,DashboardActivity.class);
        startActivity(intent);
        finishAffinity();
    }
}
