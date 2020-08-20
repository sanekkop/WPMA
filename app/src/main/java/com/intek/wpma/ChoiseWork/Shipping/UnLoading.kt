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
import com.intek.wpma.Ref.Doc
import com.intek.wpma.Ref.RefSection
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_unloading.*


class UnLoading : BarcodeDataReceiver() {

    var currentAction: Global.ActionSet = Global.ActionSet.ScanBox
    var adressUnLoad: String = ""
    var boxUnLoad: String = ""
    var docUnload = Doc()

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
        setContentView(R.layout.activity_unloading)

        title = ss.title
        header.text = "Свободная разргузка"
        currentAction = Global.ActionSet.ScanBox

        if (ss.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@UnLoading, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "UnLoading")
                startActivity(scanAct)
            }
        }
    }

    private fun reactionBarcode(Barcode: String): Boolean {
        val helper = Helper()
        val barcoderes = helper.disassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if (typeBarcode == "113") {
            //это справочник типовой
            val idd = barcoderes["IDD"].toString()
            if (ss.isSC(idd, "Секции")) {

                if (currentAction != Global.ActionSet.ScanAdress) {
                    FExcStr.text = "Неверно! Отсканируйте адрес."
                    badVoise()
                    return false
                }

                //получим данные адреса
                val adressScan = RefSection()
                adressScan.foundIDD(idd)
                adressUnLoad = adressScan.id
                var textQuery =
                    "UPDATE \$Спр.МестаПогрузки " +
                            "SET " +
                            "\$Спр.МестаПогрузки.Адрес9 = :AdressID ," +
                            "\$Спр.МестаПогрузки.Сотрудник8 = :EmployerID ," +
                            "\$Спр.МестаПогрузки.Дата9 = :NowDate ," +
                            "\$Спр.МестаПогрузки.Время9 = :NowTime "
                if (docUnload.selected) {

                    textQuery += " \$Спр.МестаПогрузки .КонтрольНабора = :DocUnload "
                    textQuery = ss.querySetParam(textQuery, "ID", docUnload.id)
                } else {
                    textQuery += " \$Спр.МестаПогрузки .ID = :ID "
                    textQuery = ss.querySetParam(textQuery, "ID", boxUnLoad)
                }
                textQuery = ss.querySetParam(textQuery, "AdressID", adressUnLoad)
                textQuery = ss.querySetParam(textQuery, "EmployerID", ss.FEmployer.id)
                if (!ss.executeWithoutRead(textQuery)) {
                    FExcStr.text = "Не удалось зафиксировать! Отсканируйте адрес."
                    badVoise()
                    return false
                }

                currentAction = Global.ActionSet.ScanBox
            }
            else {
                val doc = ss.getDoc(idd, false)
                if (doc == null) {
                    FExcStr.text =
                        "Неверно! " + if (currentAction == Global.ActionSet.ScanAdress) "Отсканируйте адрес." else "Отсканируйте коробку."
                    badVoise()
                    return false
                }

                docUnload.foundIDD(doc["IDD"].toString())
                if (docUnload.typeDoc == "КонтрольНабора") {
                    //нам нужны все места этого документа, запомним документ и найдем первое место
                    var textQuery =
                        "SELECT TOP 1 Ref.ID as ID " +
                                "FROM " +
                                "\$Спр.МестаПогрузки as Ref (nolock) " +
                                "WHERE " +
                                "\$Спр.МестаПогрузки.КонтрольНабора = :DocUnload "
                    textQuery = ss.querySetParam(textQuery, "DocUnload", docUnload.id)
                    val dt = ss.executeWithReadNew(textQuery)
                    if (dt == null) {
                        FExcStr.text =
                            "Ошибка запроса! " + if (currentAction == Global.ActionSet.ScanAdress) "Отсканируйте адрес." else "Отсканируйте коробку."
                        return false
                    }

                    if (dt.isEmpty()) {
                        FExcStr.text =
                            "Не найдено место! " + if (currentAction == Global.ActionSet.ScanAdress) "Отсканируйте адрес." else "Отсканируйте коробку."
                        return false
                    }
                    boxUnLoad = dt[0]["ID"].toString()
                    currentAction = Global.ActionSet.ScanAdress
                    adressUnLoad = ""
                } else {
                    FExcStr.text =
                        "Неверно! " + if (currentAction == Global.ActionSet.ScanAdress) "Отсканируйте адрес." else "Отсканируйте коробку."
                    badVoise()
                    return false
                }
            }
        }
        else if (typeBarcode == "6") {
            val id = barcoderes["ID"].toString()
            if (ss.isSC(id, "МестаПогрузки")) {
                if (currentAction != Global.ActionSet.ScanBox) {
                    FExcStr.text = "Неверно! Отсканируйте адрес."
                    badVoise()
                    return false
                }

                currentAction = Global.ActionSet.ScanAdress
                adressUnLoad = ""
                boxUnLoad = id

            } else {
                FExcStr.text = "Неверно! Отсканируйте коробку."
                badVoise()
                return false
            }
        }
        else {
            FExcStr.text =
                "Нет действий с данным ШК! " + if (currentAction == Global.ActionSet.ScanAdress) "Отсканируйте адрес." else "Отсканируйте коробку."
            badVoise()
            return false
        }
        goodVoise()
        refreshActivity()
        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        // нажали назад, выйдем
        if (keyCode == 4) {
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            shoiseWorkInit.putExtra("ParentForm", "Loading")
            startActivity(shoiseWorkInit)
            finish()
            return true
        }
        return false

    }

    private fun refreshActivity() {
        //пока не отсканировали место обновлять нечего
        if (boxUnLoad == "") {
            lblInfo1.text = ""
            lblInfo2.text = ""
            return
        }
        var textQuery = "Select " +
                "isnull(Sections.descr, 'Пу') as Sector, " +
                "CONVERT(char(8), CAST(LEFT(journForBill.date_time_iddoc, 8) as datetime), 4) as DateDoc, " +
                "journForBill.docno as DocNo, " +
                "DocCC.\$КонтрольНабора.НомерЛиста as Number, " +
                "isnull(Adress.descr, 'Пу') as Adress, " +
                "TabBox.CountAllBox as CountAllBox, " +
                "Ref.\$Спр.МестаПогрузки.НомерМеста as NumberBox, " +
                "Gate.descr as Gate " +
                "from \$Спр.МестаПогрузки as Ref (nolock) " +
                "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                "left join \$Спр.Секции as Sections (nolock) " +
                "on Sections.id = DocCC.\$КонтрольНабора.Сектор " +
                "left join \$Спр.Секции as Adress (nolock) " +
                "on Adress.id = Ref.\$Спр.МестаПогрузки.Адрес9 " +
                "inner join DH\$КонтрольРасходной as DocCB (nolock) " +
                "on DocCB.iddoc = DocCC.\$КонтрольНабора.ДокументОснование " +
                "inner JOIN DH\$Счет as Bill (nolock) " +
                "on Bill.iddoc = DocCB.\$КонтрольРасходной.ДокументОснование " +
                "INNER JOIN _1sjourn as journForBill (nolock) " +
                "on journForBill.iddoc = Bill.iddoc " +
                "left join \$Спр.Ворота as Gate (nolock) " +
                "on Gate.id = DocCB.\$КонтрольРасходной.Ворота " +
                "left join ( " +
                "select " +
                "DocCB.iddoc as iddoc, " +
                "count(*) as CountAllBox " +
                "from \$Спр.МестаПогрузки as Ref (nolock) " +
                "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                "inner join DH\$КонтрольРасходной as DocCB (nolock) " +
                "on DocCB.iddoc = DocCC.\$КонтрольНабора.ДокументОснование " +
                "where " +
                "Ref.ismark = 0 " +
                "group by DocCB.iddoc ) as TabBox " +
                "on TabBox.iddoc = DocCB.iddoc " +
                "where Ref.id = :id"
        textQuery = ss.querySetParam(textQuery, "id", boxUnLoad)
        val dataTable = ss.executeWithRead(textQuery)
        if (dataTable!!.isEmpty()) {
            FExcStr.text = "Не найдено место! Отсканируйте коробку."
        }

        lblInfo1.text = dataTable[1][2].substring(
            dataTable[1][2].trim().length - 5,
            dataTable[1][2].trim().length - 3
        ) + " " +
                dataTable[1][2].substring(dataTable[1][2].trim().length - 3) +
                " сектор: " + dataTable[1][0].trim() + "-" + dataTable[1][3].trim() +
                " ворота: " + dataTable[1][7].trim() + " адрес: " + dataTable[1][4].trim()
        lblInfo2.text = "место № " + dataTable[1][6].trim() + " из " + dataTable[1][5].trim()
        if (adressUnLoad != "") {

            val adressScan = RefSection()
            adressScan.foundID(adressUnLoad)
            lblDocInfo.text = "Новый адрес: " + adressScan.name
        }
        FExcStr.text =
            if (currentAction == Global.ActionSet.ScanAdress) "Отсканируйте адрес." else "Отсканируйте коробку."

    }


}
