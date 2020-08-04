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
import com.intek.wpma.Global
import com.intek.wpma.MainActivity
import com.intek.wpma.R
import kotlinx.android.synthetic.main.activity_menu.*



class Menu : BarcodeDataReceiver() {

    var Barcode: String = ""
    var codeId:String = ""  //показатель по которому можно различать типы штрих-кодов
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

        ParentForm = intent.extras!!.getString("ParentForm")!!
        title = SS.title
        btnSet.setOnClickListener {
            startActivity(0)
        }
        btnShipping.setOnClickListener {
            startActivity(3)
        }
    }

    private fun reactionBarcode(Barcode: String){
        //выход из сессии
        val barcoderes =  SS.helper.DisassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        //если это не типовой справочник, то выходим
        if (typeBarcode != "113")   {
            Lbl.text = "Нет действий с этим ШК в данном режиме"
        }
        val idd = barcoderes["IDD"].toString()
        //если это не сотрудник выходим
        if (!SS.IsSC(idd, "Сотрудники")) {
            Lbl.text = "Нет действий с этим ШК в данном режиме"
        }
        if(SS.FEmployer.IDD == idd){
            if(!Logout(SS.FEmployer.ID)){
                Lbl.text = "Ошибка выхода из системы!"
                return
            }
            val main = Intent(this, MainActivity::class.java)
            startActivity(main)
            finish()
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
            SS.CurrentMode = Global.Mode.SetInicialization
            SS.CurrentAction = Global.ActionSet.Waiting
            val setInit = Intent(this, SetInitialization::class.java)
            setInit.putExtra("ParentForm","Menu")
            startActivity(setInit)
            finish()
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
            finish()
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
