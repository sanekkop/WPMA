package com.intek.wpma.choiseWork.revise

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
import com.intek.wpma.R
import com.intek.wpma.ref.Doc
import com.intek.wpma.ref.RefItem
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_revise_mark.*

class ReviseMark : BarcodeDataReceiver() {

    private var mark:MutableMap<String,String> = mutableMapOf()

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
        setContentView(R.layout.activity_revise_mark)

        title = ss.title
        header.text = "Сверка маркировки"

        if (ss.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@ReviseMark, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "ReviseMark")
                startActivity(scanAct)
            }
        }
    }

    private fun reactionBarcode(Barcode: String): Boolean {

        if (codeId == barcodeId) {
            val testBatcode = Barcode.replace("'", "''")
            var textQuery =
                "SELECT " +
                        "\$Спр.МаркировкаТовара.ФлагОтгрузки as Отгружен ," +
                        "SUBSTRING(\$Спр.МаркировкаТовара.ДокОтгрузки , 5 , 9) as ДокОтгрузки ," +
                        "\$Спр.МаркировкаТовара.Товар as Товар " +
                        "FROM \$Спр.МаркировкаТовара (nolock) " +
                        "where \$Спр.МаркировкаТовара.УИТ = SUBSTRING('${testBatcode.trim()}',1,31) "

            var dtMark = ss.executeWithReadNew(textQuery) ?: return false
            when {
                dtMark.count() > 1 -> {
                    FExcStr.text = "ВНИМАНИЕ! Маркировка задвоена!"
                    return false
                }
                dtMark.count() == 1 -> {
                    mark = dtMark[0]
                    goodVoice()
                    refreshActivity()
                    return true
                }
            }
            textQuery =
                "SELECT " +
                        "\$Спр.МаркировкаТовара.ФлагОтгрузки as Отгружен ," +
                        "SUBSTRING(\$Спр.МаркировкаТовара.ДокОтгрузки , 5 , 9) as ДокОтгрузки ," +
                        "\$Спр.МаркировкаТовара.Товар as Товар " +
                        "FROM \$Спр.МаркировкаТовара (nolock) " +
                        "where \$Спр.МаркировкаТовара.Маркировка like ('%' +SUBSTRING('${testBatcode.trim()}',1,31) + '%') "

            dtMark = ss.executeWithReadNew(textQuery) ?: return false
            when {
                dtMark.isEmpty() -> {
                    FExcStr.text = "Маркировка не найдена"
                    return false
                }
                dtMark.count() > 1 -> {
                    FExcStr.text = "ВНИМАНИЕ! Маркировка задвоена!"
                    return false
                }
                dtMark.count() == 1 -> {
                    mark = dtMark[0]
                }
            }
            goodVoice()
        }
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
        return false

    }

    private fun refreshActivity() {
        //пока не отсканировали место обновлять нечего
        if (mark.isEmpty()) {
            FExcStr.text = "Отсканируйте маркировку"
            return
        }
        val item = RefItem()
        item.foundID(mark["Товар"].toString())
        lblInfo2.text = (item.invCode + " " + item.name)
        lblInfo1.text = if (mark["Отгружен"] == "0") "не отгружен" else " отгружен"
        val docCC = Doc()
        docCC.foundID(mark["ДокОтгрузки"].toString())
        lblDocInfo.text = ""
        if (docCC.selected) {
            lblDocInfo.text = docCC.view
        }

    }


}
