package com.asd412id.absenqr;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NoConnectionError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.zxing.Result;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import me.dm7.barcodescanner.zxing.ZXingScannerView;

public class QrScannerActivity extends AppCompatActivity implements ZXingScannerView.ResultHandler {
    private ZXingScannerView mScannerView;
    SharedPreferences configs;
    String ip_server;
    String _token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if( ContextCompat.checkSelfPermission(this,android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.CAMERA},5);
            }
        }

        configs = getApplicationContext().getSharedPreferences("configs", Context.MODE_PRIVATE);
        ip_server = configs.getString("ip_server",null);
        _token = configs.getString("_token",null);

        mScannerView = new ZXingScannerView(this);
        setContentView(mScannerView);
    }

    @Override
    public void handleResult(final Result rawResult) {
        setContentView(R.layout.activity_qr_scanner);

        String url = "http://"+ip_server+"absensi/check";

        RequestQueue queue = Volley.newRequestQueue(this);

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.POST, url, null, new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    if (response.getString("status").equals("success")){
                        absenSuccess(response);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (error instanceof NoConnectionError){
                    absenError("network");
                }else if (error.networkResponse!=null && error.networkResponse.data!=null){
                    VolleyError volleyError = new VolleyError(new String(error.networkResponse.data));
                    try {
                        JSONObject errorJson = new JSONObject(Objects.requireNonNull(volleyError.getMessage()));
                        absenError(errorJson.getString("message"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }){
            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> params = new HashMap<>();
                params.put("Accept", "application/json");
                params.put("Authorization", _token);
                params.put("Ruang-Token", rawResult.getText());
                return params;
            }
        };
        jsonObjectRequest.setRetryPolicy(new DefaultRetryPolicy(3000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        queue.add(jsonObjectRequest);
    }

    private void absenError(String message) {
        Intent intent = new Intent(this,DashboardActivity.class);
        intent.putExtra("status","error");
        if (message.equals("network")){
            intent.putExtra("error","Server tidak ditemukan!");
        }else {
            intent.putExtra("error",message);
        }
        Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0,200,200,200,200,200,200};
        Objects.requireNonNull(v).vibrate(pattern,-1);
        startActivity(intent);
        finishAffinity();
    }

    private void absenSuccess(JSONObject data) throws JSONException {
        Intent intent = new Intent(this,DashboardActivity.class);
        intent.putExtra("status","success");
        if (data.getString("status").equals("success")){
            intent.putExtra("ruang", data.getString("ruang"));
            intent.putExtra("time", data.getString("time"));
        }
        Vibrator v = (Vibrator) getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
        long[] pattern = {0,200,100,500,200};
        Objects.requireNonNull(v).vibrate(pattern,-1);
        startActivity(intent);
        finishAffinity();
    }

    @Override
    public void onResume() {
        super.onResume();
        mScannerView.setResultHandler(this);
        mScannerView.startCamera();
    }

    @Override
    public void onPause() {
        super.onPause();
        mScannerView.stopCamera();
    }
}
