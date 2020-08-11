package com.intek.wpma.ChoiseWork.Shipping

import android.R.string
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.MainActivity
import com.intek.wpma.R
import com.intek.wpma.Ref.RefEmployer
import com.intek.wpma.Ref.RefPrinter
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_new_complectation.*


class NewComplectation : BarcodeDataReceiver() {

    private var docDown: MutableMap<String, String> = mutableMapOf()
    private var badDoc: MutableMap<String, String> = mutableMapOf()

    enum class Action { Complectation, ComplectationComplete }

    private var curentAction: Action = Action.Complectation
    private var downSituation: MutableList<MutableMap<String, String>> = mutableListOf()
    private var scaningBox = ""
    private var scaningBoxIddoc = ""
    private var needAdressComplete = ""

    private var remain = 0
    private var lastGoodAdress = ""
    private var nameLastGoodAdress = ""

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
        setContentView(R.layout.activity_new_complectation)

        title = ss.title

        if (ss.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@NewComplectation, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "NewComplectation")
                startActivity(scanAct)
            }
        }
        btnCansel.setOnClickListener {
            repealNewComplectation()
        }
        newComplectationGetFirstOrder()
        var oldx = 0F
        FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                    FExcStr.text = "Подгружаю состояние..."
                    val shoiseWorkInit = Intent(this, ShowInfoNewComp::class.java)
                    shoiseWorkInit.putExtra("ParentForm", "NewComplectation")
                    shoiseWorkInit.putExtra("BadDocID", badDoc["ID"])
                    shoiseWorkInit.putExtra("BadDocView", badDoc["View"])
                    startActivity(shoiseWorkInit)
                    finish()
                }
            }
            return true
        })

    }

    private fun newComplectationGetFirstOrder() {
        var textQuery =
        "declare @res int " +
                "begin tran " +
                "exec WPM_GetFirstOrderComplectationNew :Employer, @res output " +
                "if @res = 0 rollback tran else commit tran " +
                ""
        textQuery = ss.querySetParam(textQuery, "Employer",    ss.FEmployer.id)

        if (!ss.executeWithoutRead(textQuery))
        {
            badVoise()
            return
        }
        toModeNewComplectation()
    }

    private fun repealNewComplectation() {
        if (docDown["IsFirstOrder"] == "1") {
            FExcStr.text = "Нельзя отказаться от этого задания!"
            badVoise()
            return
        }

        var textQuery =
            "declare @res int; exec WPM_RepealNewComplectation :iddoc, @res output; " +
                    ""
        textQuery = ss.querySetParam(textQuery, "iddoc", docDown["ID"].toString())
        if (!ss.executeWithoutRead(textQuery)) return
        toModeNewComplectation()
    }

    private fun toModeNewComplectation() {

        curentAction = Action.Complectation
        docDown = mutableMapOf()
        var textQuery = "select * from dbo.WPM_fn_ToModeNewComplectation(:Employer)"
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        val dt = ss.executeWithReadNew(textQuery) ?: return
        if (dt.isEmpty()) {
            //Собирать - нечего,
            toModeNewComplectationComplete()
            return
        }

        if (badDoc["ID"] == null) {
            loadBadDoc(dt[0]["RefID"].toString())
        }
        docDown["ID"] = dt[0]["iddoc"].toString()
        docDown["Boxes"] = dt[0]["CountBox"].toString()
        docDown["View"] = dt[0]["Sector"].toString()
            .trim() + "-" + dt[0]["Number"].toString() + " Заявка " + dt[0]["docno"].toString() + " (" + dt[0]["DateDoc"].toString() + ")"
        docDown["AdressCollect"] = dt[0]["AdressCollect"].toString()
        docDown["Sector"] = dt[0]["ParentSector"].toString()
        docDown["MaxStub"] = dt[0]["MaxStub"].toString()
        docDown["AllBoxes"] = dt[0]["CountAllBox"].toString()
        docDown["NumberBill"] = dt[0]["docno"].toString().trim()
        docDown["NumberCC"] = dt[0]["Number"].toString()
        docDown["MainSectorName"] = dt[0]["Sector"].toString()
        docDown["SetterName"] = dt[0]["SetterName"].toString()
        docDown["IsFirstOrder"] = dt[0]["FlagFirstOrder"].toString()
        FExcStr.text = "Сканируйте место"
        refreshActivity()
    }

    private fun toModeNewComplectationComplete() {
        var textQuery =
            "select " +
                    "min(Ref.\$Спр.МестаПогрузки.НомерЗадания7 ) as NumberOfOrder, " +
                    "Count(*) as AllBox " +
                    "from \$Спр.МестаПогрузки as Ref (nolock) " +
                    "where " +
                    "Ref.ismark = 0 " +
                    "and Ref.\$Спр.МестаПогрузки.Сотрудник8 = :Employer " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата8 = :EmptyDate " +
                    "and Ref.\$Спр.МестаПогрузки.Дата9 = :EmptyDate "
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        downSituation = ss.executeWithReadNew(textQuery) ?: return

        if (downSituation[0]["AllBox"] == "0") {
            //Нету ничего!
            val shoiseDown = Intent(this, ChoiseDown::class.java)
            shoiseDown.putExtra("ParentForm", "ChoiseDown")
            startActivity(shoiseDown)
            finish()
            return
        }
        FExcStr.text = "Сканируйте коробки и адрес комплектации!"
        scaningBox = ""
        curentAction = Action.ComplectationComplete
        refreshActivity()
    }

    private fun loadBadDoc(ID: String) {
        var textQuery =
            "Select " +
                    "isnull(Sections.descr, 'Пу') as Sector, " +
                    "CONVERT(char(8), CAST(LEFT(journForBill.date_time_iddoc, 8) as datetime), 4) as DateDoc, " +
                    "journForBill.docno as DocNo, " +
                    "DocCC.\$КонтрольНабора.НомерЛиста as Number, " +
                    "Ref.\$Спр.МестаПогрузки.КонтрольНабора as Doc,  " +
                    "TabBox.CountAllBox as CountBox, " +
                    "Gate.descr as Gate " +
                    "from \$Спр.МестаПогрузки as Ref (nolock) " +
                    "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                    "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                    "left join \$Спр.Секции as Sections (nolock) " +
                    "on Sections.id = DocCC.\$КонтрольНабора.Сектор " +
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
        textQuery = ss.querySetParam(textQuery, "id", ID)
        val dt = ss.executeWithReadNew(textQuery) ?: return
        if (dt.isEmpty()) {
            return
        }
        badDoc["ID"] = dt[0]["Doc"].toString()
        badDoc["View"] = dt[0]["Sector"].toString()
            .trim() + "-" + dt[0]["Number"].toString() + " " + dt[0]["DocNo"].toString() + " (" + dt[0]["DateDoc"].toString() + ") мест " + dt[0]["CountBox"].toString()
    } // LoadBadDoc()

    private fun refreshActivity() {

        lblPrinter.text = ss.FPrinter.description
        if (curentAction == Action.Complectation) {

            remain = docDown["AllBoxes"].toString().toInt() - docDown["Boxes"].toString().toInt()

            lblInfo1.text =
                "Отобрано " + remain.toString() + " из " + docDown["AllBoxes"].toString()
            /* if (Screan === 1) {
                ShowInfoNewComp()
            }

           */
            lblState.text = docDown["View"].toString()
            lblNumber.text = docDown["NumberBill"].toString().substring(
                docDown["NumberBill"].toString().length - 5,
                docDown["NumberBill"].toString().length - 3
            ) + " " +
                    docDown["NumberBill"].toString()
                        .substring(docDown["NumberBill"].toString().length - 3) +
                    " сектор: " + docDown["MainSectorName"].toString()
                .trim() + "-" + docDown["NumberCC"].toString()
            lblAdress.text = docDown["AdressCollect"].toString()
            lblSetter.text = "отборщик: " + ss.helper.getShortFIO(docDown["SetterName"].toString())
            lblAdress.visibility = View.VISIBLE
            lblSetter.visibility = View.VISIBLE
            btnKey1.visibility = if (docDown["MaxStub"].toString()
                    .toInt() <= remain
            ) View.VISIBLE else View.INVISIBLE
            btnKey1.text = "Все"
        } else if (curentAction == Downing.Action.DownComplete) {


            /*
            if (Screan === 1) {
                ShowInfoNewComp()
            }

             */

            btnKey1.visibility = if (lastGoodAdress != "") View.INVISIBLE else View.VISIBLE
            btnKey1.text = "Печать"
            if (downSituation[0]["NumberOfOrder"].toString() != "0") {
                val number: String = downSituation[0]["NumberOfOrder"].toString()
                lblNumber.text = number.substring(if (number.length > 4) number.length - 4 else 0)
            }
            lblInfo1.text = "Всего " + downSituation[0]["AllBox"].toString() + " мест"
            lblAdress.visibility = View.INVISIBLE
            lblSetter.visibility = View.VISIBLE
            if (lastGoodAdress != "0") {
                //lblSetter.ForeColor = Color.DarkGray
                lblSetter.text = "'все' -->$nameLastGoodAdress"
            }
            btnCansel.visibility = View.INVISIBLE
            btnCansel.isEnabled = false
            btnCansel.text = "ПОЛОН"

        }
    }

    private fun reactionBarcode(Barcode: String): Boolean {
        val barcoderes = ss.helper.disassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if (typeBarcode == "6") {
            val id = barcoderes["ID"].toString()
            if (ss.isSC(id, "МестаПогрузки")) {
                if (curentAction == Action.Complectation) {

                    var textQuery =
                        "Select " +
                                "\$Спр.МестаПогрузки.Дата8 as Date, " +
                                "\$Спр.МестаПогрузки.КонтрольНабора as Doc, " +
                                "\$Спр.МестаПогрузки.Сотрудник8 as Employer " +
                                "from \$Спр.МестаПогрузки (nolock) where id = :id"
                    textQuery = ss.querySetParam(textQuery, "id", id)
                    val dt = ss.executeWithReadNew(textQuery) ?: return false
                    if (dt.isEmpty()) {
                        FExcStr.text = "Нет действий с данным штрихкодом!"
                        badVoise()
                        return false
                    }
                    loadBadDoc(id)//Подсосем данные по документу для просмотра состояния
                    if (dt[0]["Doc"].toString() != docDown["ID"]) {
                        FExcStr.text = "Место от другого сборочного!"
                        badVoise()
                        return false
                    }
                    if (!ss.isVoidDate(dt[0]["Date"].toString())) {
                        FExcStr.text = "Место уже отобрано!"
                        badVoise()
                        return false
                    }
                    if (dt[0]["Employer"].toString() != ss.FEmployer.id) {
                        FExcStr.text = "Этого места нет в задании!"
                        badVoise()
                        return false
                    }

                    //Лютый пиздец начинается!
                    textQuery =
                        "begin tran; " +
                                "UPDATE \$Спр.МестаПогрузки " +
                                "SET " +
                                "\$Спр.МестаПогрузки.Дата8 = :NowDate , " +
                                "\$Спр.МестаПогрузки.Время8 = :NowTime " +
                                //"$Спр.МестаПогрузки.Адрес9 = dbo.WMP_fn_GetAdressComplete(id) " +
                                "WHERE " +
                                "id = :itemid; " +
                                "if @@rowcount = 0 rollback tran " +
                                "else begin " +
                                "if exists ( " +
                                "select top 1 Ref.id from \$Спр.МестаПогрузки as Ref " +
                                "inner join \$Спр.Секции as SectionsCollect (nolock) " +
                                "ON SectionsCollect.id = Ref.\$Спр.МестаПогрузки.Адрес7 " +
                                "inner join \$Спр.Секции as RefSectionsParent (nolock) " +
                                "on left(SectionsCollect.descr, 2) = RefSectionsParent.descr " +
                                "where " +
                                "Ref.ismark = 0 " +
                                "and RefSectionsParent.descr = :NameParent " +
                                "and Ref.\$Спр.МестаПогрузки.КонтрольНабора = :iddoc " +
                                "and Ref.\$Спр.МестаПогрузки.Дата8 = :EmptyDate ) " +
                                "commit tran " +
                                "else begin " +
                                "declare @res int; " +
                                "exec WPM_GetOrderComplectationNew :Employer, :NameParent, 0, @res OUTPUT; " +
                                "if @res = 0 rollback tran else commit tran " +
                                "end " +
                                "end "

                    textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
                    textQuery = ss.querySetParam(textQuery, "itemid", id)
                    textQuery = ss.querySetParam(textQuery, "iddoc", docDown["ID"].toString())
                    textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
                    textQuery = ss.querySetParam(
                        textQuery,
                        "NameParent",
                        docDown["Sector"].toString().trim()
                    )

                    if (!ss.executeWithoutRead(textQuery)) {
                        return false
                    }
                    toModeNewComplectation()
                    return true

                }
                else {
                    var textQuery =
                        "Select " +
                                "isnull(Sections.descr, 'Пу') as Sector, " +
                                "DocCC.\$КонтрольНабора.НомерЛиста as Number, " +
                                "CONVERT(char(8), CAST(LEFT(journForBill.date_time_iddoc, 8) as datetime), 4) as DateDoc, " +
                                "journForBill.docno as DocNo, " +
                                "Ref.\$Спр.МестаПогрузки.Дата9 as Date, " +
                                "Ref.\$Спр.МестаПогрузки.КонтрольНабора as Doc, " +
                                "Ref.\$Спр.МестаПогрузки.Сотрудник8 as Employer, " +
                                "isnull(dbo.WMP_fn_GetAdressComplete(Ref.id), :EmptyID ) as Adress9, " +
                                "isnull(Adress.Descr, 'нет адреса') as Adress, " +
                                "AllTab.CountAllBox as CountBox, " +
                                "Gate.descr as Gate " +
                                "from \$Спр.МестаПогрузки as Ref (nolock) " +
                                "left join \$Спр.Секции as Adress (nolock) " +
                                "on Adress.id = dbo.WMP_fn_GetAdressComplete(Ref.id) " +
                                "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                                "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                                "left join \$Спр.Секции as Sections (nolock) " +
                                "on Sections.id = DocCC.\$КонтрольНабора.Сектор " +
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
                                "where Ref.id = :id"
                    textQuery = ss.querySetParam(textQuery, "id", id)
                    textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
                    textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
                    textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())

                    loadBadDoc(id)//Подсосем данные по документу для просмотра состояния

                    val dt = ss.executeWithReadNew(textQuery) ?: return false
                    if (dt.isEmpty()) {
                        FExcStr.text = "Нет действий с данным штрихкодом!"
                        return false
                    }
                    if (!ss.isVoidDate(dt[0]["Date"].toString())) {
                        FExcStr.text = "Место уже скомплектовано!"
                        return false
                    }
                    if (dt[0]["Employer"].toString() != ss.FEmployer.id) {
                        FExcStr.text = "Этого места нет в задании!"
                        return false
                    }

                    docDown["ID"] = dt[0]["Doc"].toString()
                    docDown["Boxes"] = dt[0]["CountBox"].toString()
                    docDown["View"] = dt[0]["Sector"].toString()
                        .trim() + "-" + dt[0]["Number"].toString() + " Заявка " + dt[0]["docno"].toString() + " (" + dt[0]["datedoc"].toString() + ")"
                    docDown["AdressCollect"] = dt[0]["Adress"].toString()
                    docDown["Sector"] = dt[0]["Gate"].toString().trim()
                    docDown["NumberBill"] = dt[0]["docno"].toString().trim()
                    docDown["NumberCC"] = dt[0]["Number"].toString()
                    docDown["MainSectorName"] = dt[0]["Sector"].toString()

                    scaningBox = id
                    scaningBoxIddoc = dt[0]["Doc"].toString()
                    needAdressComplete = dt[0]["Adress9"].toString()
                    FExcStr.text = "Отсканируйте адрес!"
                    //OnScanBox()
                    return true

                }
            }
            else {
                FExcStr.text = "Нет действий с данным ШК в данном режиме!"
                badVoise()
                return false
            }

        }
        else if (typeBarcode == "113") {
            //справочники типовые
            val idd = barcoderes["IDD"].toString()
            if (ss.isSC(idd, "Сотрудники")) {
                ss.FEmployer = RefEmployer()
                val mainInit = Intent(this, MainActivity::class.java)
                mainInit.putExtra("ParentForm", "NewComplectation")
                startActivity(mainInit)
                finish()
            }
            else if (curentAction == Action.ComplectationComplete && ss.isSC(idd, "Секции")) {
                val id = barcoderes["ID"].toString()
                if (downSituation[0]["NumberOfOrder"].toString() == "0") {
                    FExcStr.text = "Не присвоен номер задания! Напечатайте этикетку!"
                    return false
                }
                var textQuery =
                    "declare @res int; exec WPM_CompletePallete :employer, :adress, @res output; "
                textQuery = ss.querySetParam(textQuery, "employer", ss.FEmployer.id)
                textQuery = ss.querySetParam(textQuery, "adress", id)
                if (!ss.executeWithoutRead(textQuery)) {
                    return false
                }
                // FlagPrintPallete = false;
                // ToModeDown()
                return true
            }
        }
        else {
            FExcStr.text = "Нет действий с данным ШК в данном режиме!"
            badVoise()
            return false
        }
        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        // нажали назад, выйдем и разблокируем доки
        if (keyCode == 4) {
            return true
        }
        if (ss.helper.whatDirection(keyCode) == "Left") {
            FExcStr.text = "Подгружаю состояние..."
            val shoiseWorkInit = Intent(this, ShowInfoNewComp::class.java)
            shoiseWorkInit.putExtra("ParentForm", "NewComplectation")
            shoiseWorkInit.putExtra("BadDocID", badDoc["ID"])
            shoiseWorkInit.putExtra("BadDocView", badDoc["View"])

            startActivity(shoiseWorkInit)
            finish()
            return true
        }
        return false
    }


}
