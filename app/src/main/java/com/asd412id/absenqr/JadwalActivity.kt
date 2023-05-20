package com.asd412id.absenqr

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.asd412id.absenqr.adapters.JadwalAdapter
import com.asd412id.absenqr.databinding.ActivityJadwalsBinding
import org.json.JSONArray

class JadwalActivity : AppCompatActivity() {

    private lateinit var binding: ActivityJadwalsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)

        binding = ActivityJadwalsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Daftar Jadwal Hari Ini"

        val listView = findViewById<ListView>(R.id.jadwallists)
        val items = ArrayList<JadwalAdapter.Item>()

        val configs = getSharedPreferences("configs", MODE_PRIVATE)
        val getJadwal = configs.getString("jadwals",null)
        val jadwals = JSONArray(getJadwal)

        if (jadwals.length() > 0){
            for (i in 0 until jadwals.length()){
                val item = jadwals.getJSONObject(i)
                val ruang = item.getJSONObject("get_ruang").getString("nama_ruang")
                val jadwal = item.getString("nama_jadwal")
                val checkin = item.getString("cin")
                val checkout = item.getString("cout")

                val list = JadwalAdapter.Item(ruang,jadwal,checkin,checkout)
                items.add(list)
            }
        }else{
            val list = JadwalAdapter.Item("","Jadwal tidak tersedia","","")
            items.add(list)
        }

        val adapter = JadwalAdapter(this, items)
        listView.adapter = adapter
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Handle back button click here
                val intent = Intent(this,DashboardActivity::class.java)
                startActivity(intent)
                finishAffinity()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}