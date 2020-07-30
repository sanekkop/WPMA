package com.intek.wpma.ChoiseWork.Shipping


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.widget.Toast
import com.intek.wpma.*
import com.intek.wpma.Menu
import kotlinx.android.synthetic.main.activity_menu_shipping.*
import kotlinx.android.synthetic.main.activity_menu_shipping.terminalView
import kotlinx.android.synthetic.main.activity_unloading.*

class ChoiseWorkShipping: BarcodeDataReceiver() {

    var Barcode: String = ""
    var codeId: String = ""  //показатель по которому можно различать типы штрих-кодов

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
                    } catch (e: Exception) {
                        val toast = Toast.makeText(
                            applicationContext,
                            "Отсутствует соединение с базой!",
                            Toast.LENGTH_LONG
                        )
                        toast.show()
                    }
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_shipping)

        ParentForm = intent.extras!!.getString("ParentForm")!!
        //terminalView.text = intent.extras!!.getString("terminalView")!!
        terminalView.text = SS.terminal
        title = SS.FEmployer.Name

        btnCancel.setOnClickListener {
            val shoiseWorkInit = Intent(this, Menu::class.java)
            shoiseWorkInit.putExtra("ParentForm", "ChoiseWorkShipping")
            startActivity(shoiseWorkInit)
            finish()
        }
        btnLoad.setOnClickListener {
            val loadingInit = Intent(this, Loading::class.java)
            loadingInit.putExtra("ParentForm", "ChoiseWorkShipping")
            startActivity(loadingInit)
            finish()
        }

        btnUnLoad.setOnClickListener {
            val unLoadingInit = Intent(this, UnLoading::class.java)
            unLoadingInit.putExtra("ParentForm", "ChoiseWorkShipping")
            startActivity(unLoadingInit)
            finish()

        }
        btnDown.setOnClickListener {
            BadVoise()
            FExcStr.text = "Режим в разработке!"

            /*
                 val downingInit = Intent(this, Downing::class.java)
                 downingInit.putExtra("Employer", Employer)
                 downingInit.putExtra("EmployerIDD",EmployerIDD)
                 downingInit.putExtra("EmployerFlags",EmployerFlags)
                 downingInit.putExtra("EmployerID",EmployerID)
                 downingInit.putExtra("terminalView",terminalView.text.trim())
                 downingInit.putExtra("ParentForm","ChoiseWorkShipping")
                 startActivity(downingInit)
                 finish()
                 */
        }
        btnFree.setOnClickListener {
            BadVoise()
            FExcStr.text = "Режим в разработке!"

            /*
                val freeComplectationInit = Intent(this, FreeComplectation::class.java)
                freeComplectationInit.putExtra("Employer", Employer)
                freeComplectationInit.putExtra("EmployerIDD",EmployerIDD)
                freeComplectationInit.putExtra("EmployerFlags",EmployerFlags)
                freeComplectationInit.putExtra("EmployerID",EmployerID)
                freeComplectationInit.putExtra("terminalView",terminalView.text.trim())
                freeComplectationInit.putExtra("ParentForm","ChoiseWorkShipping")
                startActivity(freeComplectationInit)
                finish()

                 */
        }
    }

    private fun reactionBarcode(Barcode: String) {
        //выход из сессии
        if (SS.FEmployer.IDD == "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12)) {
            if (!Logout(SS.FEmployer.ID)) {
                Lbl.text = "Ошибка выхода из системы!"
                return
            }
            val main = Intent(this, MainActivity::class.java)
            startActivity(main)
            return
        } else {
            Lbl.text = "Нет действий с ШК в данном режиме!"
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == 7) {
            //нажали 0
            startActivity(7)
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun startActivity(num: Int) {
        var intent: Intent

        if (num == 7) {     // режим отбора
            intent = Intent(this, Menu::class.java)
        } else {
            intent = Intent(this, Menu::class.java)
        }
        intent.putExtra("ParentForm", "ChoiseWorkShipping")
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        //        IntentFilter intentFilter = new IntentFilter("hsm.RECVRBI");
        registerReceiver(barcodeDataReceiver, IntentFilter(ACTION_BARCODE_DATA))
        claimScanner()
        Log.d("IntentApiSample: ", "onResume")
        if (MainActivity.scanRes != null) {
            try {
                reactionBarcode(MainActivity.scanRes.toString())
            } catch (e: Exception) {
                val toast = Toast.makeText(
                    applicationContext,
                    "Отсутствует соединение с базой!",
                    Toast.LENGTH_LONG
                )
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


