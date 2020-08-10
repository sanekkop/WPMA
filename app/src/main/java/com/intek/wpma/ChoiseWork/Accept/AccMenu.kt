package com.intek.wpma.ChoiseWork.Accept

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import com.intek.wpma.*
import com.intek.wpma.ChoiseWork.Menu
import kotlinx.android.synthetic.main.activity_acc_menu.*
import kotlinx.android.synthetic.main.activity_acc_menu.FExcStr

class AccMenu : BarcodeDataReceiver() {

    //region шапка с необходимыми функциями для работы сканеров перехватчиков кнопок и т.д.
    var barcode: String = ""
    var codeId: String = ""             //показатель по которому можно различать типы штрих-кодов
    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    // ту прописываем что делать при событии сканирования
                    try {
                        barcode = intent.getStringExtra("data")
                        reactionBarcode(barcode)
                    }
                    catch(e: Exception) {
                        val toast = Toast.makeText(applicationContext, "Не удалось отсканировать штрихкод!", Toast.LENGTH_LONG)
                        toast.show()
                    }

                }
            }
        }
    }
    override fun onResume() {
        super.onResume()
        registerReceiver(barcodeDataReceiver, IntentFilter(ACTION_BARCODE_DATA))
        claimScanner()
        Log.d("IntentApiSample: ", "onResume")
        if(scanRes != null){
            try {
                barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(barcode)
            }
            catch (e: Exception){
                val toast = Toast.makeText(applicationContext, "Ошибка! Возможно отсутствует соединение с базой!", Toast.LENGTH_LONG)
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
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        return if (reactionKey(keyCode, event)) true else super.onKeyDown(keyCode, event)
    }
    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_acc_menu)

        ParentForm = intent.extras!!.getString("ParentForm")!!
        title = ss.title

        btnBack.setOnClickListener {
            startActivity(0)
        }
        btnAcc.setOnClickListener {
            startActivity(1)
        }
  /*      btnCrossDoc.setOnClickListener {
            startActivity(2)
        } */


    }

    private fun reactionBarcode(Barcode: String){
        //выход из сессии
        if (ss.FEmployer.idd == "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12)) {
            if (!logout(ss.FEmployer.id)) {
                FExcStr.text = "Ошибка выхода из системы!"
                return
            }
            val main = Intent(this, MainActivity::class.java)
            startActivity(main)
            return
        } else {
            FExcStr.text = "Нет действий с ШК в данном режиме!"
        }
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?):Boolean {

        val key = ss.helper.whatInt(keyCode)
        if (key in 0..9) {
            //нажали 0
            startActivity(key)
            return true
        }
        else if (keyCode == 4)
        {
            //выход
            startActivity(0)
            return true
        }
        //не наши кнопки вернем ложь
        return  false
    }


      private fun startActivity(num: Int) {
          var intent: Intent
          intent = Intent(this, Menu::class.java)
          when (num)
          {
              0 -> intent = Intent(this, Menu::class.java)
              1 -> intent = Intent(this, Search::class.java)
            //  2 -> intent = Intent(this, CrossDoc::class.java)
          }
          intent.putExtra("ParentForm", "AccMenu")
          startActivity(intent)
          finish()
      }


}