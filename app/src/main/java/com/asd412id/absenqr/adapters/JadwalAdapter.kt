package com.asd412id.absenqr.adapters

import android.content.Context
import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import com.asd412id.absenqr.R
import java.time.LocalTime
import java.time.format.DateTimeFormatter

class JadwalAdapter(private val context: Context, private val items: List<Item>) : BaseAdapter() {
    override fun getCount(): Int {
        return items.size
    }

    override fun getItem(position: Int): Any {
        return items[position]
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val viewHolder: ViewHolder

        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(R.layout.jadwal_lists, parent, false)
            viewHolder = ViewHolder(view)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }

        val jadwalText = view.findViewById<TextView>(R.id.jadwal)
        val card = view.findViewById<CardView>(R.id.jadwalcard)

        val item = getItem(position) as Item

        if (item.ruang != ""){
            view.findViewById<TextView>(R.id.ruang).visibility = View.VISIBLE
        }
        if (item.jadwal != ""){
            jadwalText.visibility = View.VISIBLE
        }
        if (item.checkin != ""){
            view.findViewById<TextView>(R.id.checkin).visibility = View.VISIBLE
        }
        if (item.checkout != ""){
            view.findViewById<TextView>(R.id.checkout).visibility = View.VISIBLE
        }

        if (item.ruang == ""){
            jadwalText.textAlignment = View.TEXT_ALIGNMENT_CENTER
        }

        val now = LocalTime.now()
        val formatter = DateTimeFormatter.ofPattern("HH:mm")

        if (now.isAfter(LocalTime.parse(item.checkout, formatter))){
            val color = Color.parseColor("#ddffdd")
            card.setCardBackgroundColor(color)
        }else{
            card.setCardBackgroundColor(Color.WHITE)
        }

        viewHolder.ruang.text = item.ruang
        viewHolder.jadwal.text = item.jadwal
        viewHolder.checkin.text = "Mulai: " + item.checkin
        viewHolder.checkout.text = "Selesai: " + item.checkout

        return view
    }

    private class ViewHolder(view: View) {
        val ruang: TextView = view.findViewById(R.id.ruang)
        val jadwal: TextView = view.findViewById(R.id.jadwal)
        val checkin: TextView = view.findViewById(R.id.checkin)
        val checkout: TextView = view.findViewById(R.id.checkout)
    }

    data class Item(val ruang: String, val jadwal: String, val checkin: String, val checkout: String)
}