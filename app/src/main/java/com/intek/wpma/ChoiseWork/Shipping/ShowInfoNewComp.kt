package com.intek.wpma.ChoiseWork.Shipping

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.Toast
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.Global
import com.intek.wpma.Helpers.Helper
import com.intek.wpma.R
import com.intek.wpma.Ref.RefSection
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_unloading.*

class ShowInfoNewComp: BarcodeDataReceiver() {

    var CCRP: MutableList<MutableMap<String, String>> = mutableListOf()
    var BadDoc: MutableMap<String, String> = mutableMapOf()


    //region шапка с необходимыми функциями для работы сканеров перехватчиков кнопок и т.д.
    var Barcode: String = ""
    var codeId: String = ""             //показатель по которому можно различать типы штрих-кодов
    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    // ту прописываем что делать при событии сканирования
                    try {
                        Barcode = intent.getStringExtra("data")
                        reactionBarcode(Barcode)
                    } catch (e: Exception) {
                        val toast = Toast.makeText(
                            applicationContext,
                            "Не удалось отсканировать штрихкод!",
                            Toast.LENGTH_LONG
                        )
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
        if (scanRes != null) {
            try {
                Barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(Barcode)
            } catch (e: Exception) {
                val toast = Toast.makeText(
                    applicationContext,
                    "Ошибка! Возможно отсутствует соединение с базой!",
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

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        return if (ReactionKey(keyCode, event)) true else super.onKeyDown(keyCode, event)
    }

    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_info_new_comp)

        title = SS.title
        if (SS.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@ShowInfoNewComp, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "UnLoading")
                startActivity(scanAct)
            }
        }
        RefreshRoute()
        BadDoc.put("ID",intent.extras!!.getString("BadDocID")!!)
        BadDoc.put("View",intent.extras!!.getString("View")!!)

    }

    fun RefreshRoute() {
        var textQuery =
            "select " +
                    "right(min(journForBill.docno), 5) as Bill, " +
                    "rtrim(min(isnull(Sections.descr, 'Пу'))) + '-' + cast(min(DocCC.\$КонтрольНабора.НомерЛиста ) as char) as CC, " +
                    "max(AllTab.CountAllBox) as Boxes, " +
                    "rtrim(max(RefAdress9.descr)) as Adress, " +
                    "max(Gate.descr) as Gate " +
                    "from \$Спр.МестаПогрузки as Ref (nolock) " +
                    "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                    "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                    "left join \$Спр.Секции as Sections (nolock) " +
                    "on Sections.id = DocCC.\$КонтрольНабора.Сектор " +
                    "inner join DH\$КонтрольРасходной as DocCB (nolock) " +
                    "on DocCB .iddoc = DocCC.\$КонтрольНабора.ДокументОснование " +
                    "inner JOIN DH\$Счет as Bill (nolock) " +
                    "on Bill.iddoc = DocCB.\$КонтрольРасходной.ДокументОснование " +
                    "inner join _1sjourn as journForBill (nolock) " +
                    "on journForBill.iddoc = Bill.iddoc " +
                    "left join \$Спр.Секции as RefAdress9 (nolock) " +
                    "on RefAdress9.id = dbo.WMP_fn_GetAdressComplete(Ref.id) " +
                    "left join \$Спр.Ворота as Gate (nolock) " +
                    "on Gate.id = DocCB.\$КонтрольРасходной.Ворота " +
                    "inner join ( " +
                    "select " +
                    "DocCC.iddoc as iddoc, " +
                    "count(*) as CountAllBox " +
                    "from \$Спр.МестаПогрузки as Ref (nolock) " +
                    "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                    "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                    "where " +
                    "Ref.ismark = 0 " +
                    "and Ref.\$Спр.МестаПогрузки.Сотрудник8 = :Employer " +
                    "and Ref.\$Спр.МестаПогрузки.Дата9 = :EmptyDate " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата8 = :EmptyDate " +
                    "group by DocCC.iddoc ) as AllTab " +
                    "on AllTab.iddoc = DocCC.iddoc " +
                    "where " +
                    "Ref.ismark = 0 " +
                    "and Ref.\$Спр.МестаПогрузки.Сотрудник8 = :Employer " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата8 = :EmptyDate " +
                    "and Ref.\$Спр.МестаПогрузки.Дата9 = :EmptyDate " +
                    "group by DocCC.iddoc";
        textQuery = SS.QuerySetParam(textQuery, "Employer", SS.FEmployer.ID);
        textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate());
        CCRP = SS.ExecuteWithReadNew(textQuery) ?: return
    }


    private fun reactionBarcode(Barcode: String): Boolean {

        RefreshActivity()
        return true
    }

    private fun ReactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        // нажали назад, выйдем
        if (keyCode == 4|| SS.helper.WhatDirection(keyCode) == "Right") {
            val shoiseWorkInit = Intent(this, NewComplectation::class.java)
            shoiseWorkInit.putExtra("ParentForm", "ShowInfoNewComp")
            startActivity(shoiseWorkInit)
            finish()
            return true
        }
        return false

    }

    fun RefreshActivity() {


    }

}