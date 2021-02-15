package com.intek.wpma.ChoiseWork.Revise

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
import com.intek.wpma.Helpers.Helper
import com.intek.wpma.R
import com.intek.wpma.Ref.Doc
import com.intek.wpma.Ref.RefItem
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_mark_menu.*
import kotlinx.android.synthetic.main.activity_remark_mark.*
import kotlinx.android.synthetic.main.activity_remark_mark.FExcStr
import kotlinx.android.synthetic.main.activity_remark_mark.header
import kotlinx.android.synthetic.main.activity_search_acc.*
import java.text.SimpleDateFormat
import java.util.*


class ReMark : BarcodeDataReceiver() {

    private var mark:MutableMap<String,String> = mutableMapOf()
    private var item = RefItem()
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
                        barcode = intent.getStringExtra("data")!!
                        codeId = intent.getStringExtra("codeId")!!
                        reactionBarcode(barcode)
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
        onWindowFocusChanged(true)
        Log.d("IntentApiSample: ", "onResume")
        if (scanRes != null) {
            try {
                barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(barcode)
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

        return if (reactionKey(keyCode, event)) true else super.onKeyDown(keyCode, event)
    }

    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_remark_mark)

        title = ss.title
        header.text = "Сверка маркировки"

        if (ss.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@ReMark, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "ReMark")
                startActivity(scanAct)
            }
        }
        btnApply.setOnClickListener {
            remarkItem()
        }
    }

    private fun reactionBarcode(Barcode: String): Boolean {
        val helper = Helper()
        val barcoderes = helper.disassembleBarcode(Barcode)
        mark = mutableMapOf()
        if (codeId == barcodeId){
            val testBatcode = Barcode.replace("'", "''")
                val textQuery =
                    "SELECT " +
                            "\$Спр.МаркировкаТовара.ФлагОтгрузки as Отгружен ," +
                            "SUBSTRING(\$Спр.МаркировкаТовара.ДокОтгрузки , 5 , 9) as ДокОтгрузки ," +
                            "SUBSTRING(\$Спр.МаркировкаТовара.Маркировка , 1 , 31) as Маркировка ," +
                            "\$Спр.МаркировкаТовара.Товар as Товар , " +
                            "ID as id " +
                            "FROM \$Спр.МаркировкаТовара (nolock) " +
                            "where \$Спр.МаркировкаТовара.Маркировка like ('%' +SUBSTRING('${testBatcode.trim()}',1,31) + '%') "

                val dtMark = ss.executeWithReadNew(textQuery) ?: return false
                when {
                    dtMark.isEmpty() -> {
                        FExcStr.text = "Маркировка не найдена"
                        badVoise()
                        return false
                    }
                    dtMark.count() > 1 -> {
                        FExcStr.text = "ВНИМАНИЕ! Маркировка задвоена!"
                        badVoise()
                        return false
                    }
                    dtMark.count() == 1 -> {
                        mark = dtMark[0]
                        FExcStr.text = "Маркировка принята. Зафиксируйте перемаркировку."
                    }
                }

        }
        else {
            val itemTemp = RefItem()
            if (itemTemp.foundBarcode(Barcode) == true) {
                FExcStr.text = "Товар найден, сканируйте маркировку"
                item = RefItem()
                item.foundID(itemTemp.id)
            }
            else {
                FExcStr.text = "ВНИМАНИЕ! Товар не найден!"
                badVoise()
                return false
            }
        }
        goodVoise()
        refreshActivity()
        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        // нажали назад, выйдем
        if (keyCode == 4) {
            val menu = Intent(this, MarkMenu::class.java)
            startActivity(menu)
            finish()
            return true
        }
        if (keyCode == 61) {
            remarkItem()
        }
        return false

    }

    private fun refreshActivity() {
        //пока не отсканировали место обновлять нечего
        if (!item.selected) {
            FExcStr.text = "Отсканируйте товар"
            return
        }
        lblInfo2.text = item.invCode + " " + item.name
        if (mark.isNotEmpty()) {
            lblInfo1.text = mark["Маркировка"] + " " + if (mark["Отгружен"] == "0") "не отгружен" else " отгружен"
        }
        else {
            lblInfo1.text = "Маркировка не отсканированна!"
        }
        val docCC = Doc()
        docCC.foundID(mark["ДокОтгрузки"].toString())
        lblDocInfo.text = ""
        if (docCC.selected) {
            lblDocInfo.text = docCC.view
        }

    }

    private fun remarkItem() {
        if (mark.isEmpty()) {
            FExcStr.text = "ВНИМАНИЕ! Маркировка не отсканированна!"
            badVoise()
            return
        }
        var textQuery =
            "UPDATE \$Спр.МаркировкаТовара WITH (rowlock) " +
                    "SET \$Спр.МаркировкаТовара.Товар = :ItemID " +
                    "WHERE ID = :MarkID ; "

        textQuery = ss.querySetParam(textQuery, "ItemID", item.id)
        textQuery = ss.querySetParam(textQuery, "MarkID",mark["id"].toString())
        if (!ss.executeWithoutRead(textQuery)) {

            FExcStr.text = "ВНИМАНИЕ! Перемаркировка не зафиксированна!"
            badVoise()
            return
        }
        FExcStr.text = "Перемаркировка зафиксированна!"
        goodVoise()
        mark = mutableMapOf()
        refreshActivity()
    }


}
