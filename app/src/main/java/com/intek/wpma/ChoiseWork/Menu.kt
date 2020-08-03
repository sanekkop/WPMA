package com.intek.wpma.ChoiseWork

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.ChoiseWork.Set.SetInitialization
import com.intek.wpma.ChoiseWork.Shipping.ChoiseWorkShipping
import com.intek.wpma.MainActivity
import com.intek.wpma.R
import kotlinx.android.synthetic.main.activity_menu.*
import kotlinx.android.synthetic.main.activity_menu.terminalView


class Menu : BarcodeDataReceiver() {

    var Barcode: String = ""
    var codeId:String = ""  //показатель по которому можно различать типы штрих-кодов
    var Employer: String = ""
    var EmployerFlags: String = ""
    var EmployerIDD: String = ""
    var EmployerID: String = ""
    var ParentForm: String = ""

    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    try {
                        Barcode = intent.getStringExtra("data")
                        codeId = intent.getStringExtra("codeId")
                        reactionBarcode(Barcode)
                    }
                    catch (e: Exception){
                        val toast = Toast.makeText(applicationContext, "Отсутствует соединение с базой!", Toast.LENGTH_LONG)
                        toast.show()
                    }
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu)

        Employer = intent.extras!!.getString("Employer")!!
        EmployerFlags = intent.extras!!.getString("EmployerFlags")!!
        EmployerIDD = intent.extras!!.getString("EmployerIDD")!!
        EmployerID = intent.extras!!.getString("EmployerID")!!
        ParentForm = intent.extras!!.getString("ParentForm")!!
        title = SS.title
        btnSet.setOnClickListener {
            val setInit = Intent(this, SetInitialization::class.java)
            setInit.putExtra("Employer", Employer)
            setInit.putExtra("EmployerIDD",EmployerIDD)
            setInit.putExtra("EmployerFlags",EmployerFlags)
            setInit.putExtra("EmployerID",EmployerID)
            setInit.putExtra("ParentForm","Menu")
            startActivity(setInit)
        }
        btnShipping.setOnClickListener {
            val choiseWorkShipingInit = Intent(this, ChoiseWorkShipping::class.java)
            choiseWorkShipingInit.putExtra("ParentForm","Menu")
            startActivity(choiseWorkShipingInit)
        }
    }

    private fun reactionBarcode(Barcode: String){
        //выход из сессии
        if(EmployerIDD == "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12)){
            if(!Logout(EmployerID)){
                Lbl.text = "Ошибка выхода из системы!"
                return
            }
            val main = Intent(this, MainActivity::class.java)
            startActivity(main)
            finish()
            return
        }
        else  {
            Lbl.text = "Нет действий с ШК в данном режиме!"
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        val key = SS.helper.WhatInt(keyCode)
        if (key in 0..9) {
            //нажали цифру
            startActivity(key)
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun startActivity(num: Int) {

        if (num == 0) {     // режим отбора
            val setInit = Intent(this, SetInitialization::class.java)
            setInit.putExtra("Employer", Employer)
            setInit.putExtra("EmployerIDD",EmployerIDD)
            setInit.putExtra("EmployerFlags",EmployerFlags)
            setInit.putExtra("EmployerID",EmployerID)
            setInit.putExtra("ParentForm","Menu")
            startActivity(setInit)
        }
        else if (num == 1)
        {
            //приемка
            /*val choiseWorkShipingInit = Intent(this, ChoiseWorkShipping::class.java)
            choiseWorkShipingInit.putExtra("ParentForm","Menu")
            startActivity(choiseWorkShipingInit)

             */
        }
        else if (num == 3)
        {
            val choiseWorkShipingInit = Intent(this, ChoiseWorkShipping::class.java)
            choiseWorkShipingInit.putExtra("ParentForm","Menu")
            startActivity(choiseWorkShipingInit)
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(barcodeDataReceiver, IntentFilter(ACTION_BARCODE_DATA))
        claimScanner()
        Log.d("IntentApiSample: ", "onResume")
        if(MainActivity.scanRes != null){
            try {
                reactionBarcode(MainActivity.scanRes.toString())
            }
            catch (e: Exception){
                val toast = Toast.makeText(applicationContext, "Отсутствует соединение с базой!", Toast.LENGTH_LONG)
                toast.show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(barcodeDataReceiver)
        releaseScanner()
        Log.d("IntentApiSample: ", "onPause")
    }


}
