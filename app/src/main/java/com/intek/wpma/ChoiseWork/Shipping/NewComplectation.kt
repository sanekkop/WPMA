package com.intek.wpma.ChoiseWork.Shipping

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
import com.intek.wpma.*
import com.intek.wpma.Ref.RefEmployer
import com.intek.wpma.Ref.RefSection
import com.intek.wpma.SQL.SQL1S
import kotlinx.android.synthetic.main.activity_new_complectation.*


class NewComplectation : BarcodeDataReceiver() {

    private var docDown: MutableMap<String, String> = mutableMapOf()
    private var badDoc: MutableMap<String, String> = mutableMapOf()

    enum class Action { Complectation, ComplectationComplete }

    private var curentAction: Action = Action.Complectation
    private var downSituation: MutableList<MutableMap<String, String>> = mutableListOf()
    private var scaningBox = ""
    private var scaningBoxIddoc = ""
    private var needAdressComplete = ss.getVoidID()

    private var remain = 0
    private var lastGoodAdress = ""
    private var nameLastGoodAdress = ""
    private var ccrp: MutableList<MutableMap<String, String>> = mutableListOf()

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
        val oldMode = ss.CurrentMode
        ss.CurrentMode = Global.Mode.NewComplectation
        if (ss.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@NewComplectation, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "NewComplectation")
                startActivity(scanAct)
            }
        }
        btnCansel.setOnClickListener {
            if (curentAction == Action.Complectation) {
                repealNewComplectation()
            }
            else {
                if (needAdressComplete != ss.getVoidID()) {
                    FExcStr.text = "Адрес полон, фиксирую..."
                    if (!adressFull()) {
                        lastGoodAdress = ""
                        docDown = mutableMapOf()
                        needAdressComplete = ss.getVoidID()
                        toModeNewComplectationComplete()
                    }
                }
            }

        }
        btnKey1.setOnClickListener {
            if (curentAction == Action.Complectation) {
                return@setOnClickListener
            }

            if (lastGoodAdress != "")
            {
                //Можно завершить
                FExcStr.text = "Комплектую остальные..."
                if (completeAll())
                {
                    goodVoise()
                }
                else
                {
                    badVoise()
                }
                refreshActivity()
            }
        }
        var oldx = 0F
        FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                    if (!loadCC()) return true

                    FExcStr.text = "Подгружаю состояние..."
                    val shoiseWorkInit = Intent(this, ShowInfoNewComp::class.java)
                    shoiseWorkInit.putExtra("ParentForm", "NewComplectation")
                    shoiseWorkInit.putExtra("BadDocID", badDoc["ID"])
                    shoiseWorkInit.putExtra("BadDocView", badDoc["View"])
                    startActivity(shoiseWorkInit)
                    finish()
                }
                if (event.x > oldx) {
                    FExcStr.text = "Подгружаю маршрут..."
                    val showRouteInit = Intent(this, ShowRoute::class.java)
                    startActivity(showRouteInit)
                    finish()
                }
            }
            return true
        })
        //если пришлииз выбора режима, то получаем первое задание
        if (oldMode == Global.Mode.ChoiseDown) {
            newComplectationGetFirstOrder()
        }
        else if (oldMode == Global.Mode.ChoiseDown) {
            toModeNewComplectation()
            reactionBarcode(intent.extras!!.getString("Barcode")!!)
        }
        else {
            toModeNewComplectation()
        }

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
        ss.CurrentMode = Global.Mode.NewComplectation
        docDown = mutableMapOf()
        var textQuery = "select * from dbo.WPM_fn_ToModeNewComplectation(:Employer)"
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        val dt = ss.executeWithReadNew(textQuery) ?: return
        if (dt.isEmpty()) {
            //Собирать - нечего,
            toModeNewComplectationComplete()
            return
        }

        if (badDoc["ID"] == null ) {
        //обновим документ для просмотра состояния иначе он не обновляется
            loadBadDoc(dt[0]["RefID"].toString())
        }
        else //обновим документ для просмотра состояния иначе он не обновляется
        {
            if (badDoc["ID"] != dt[0]["iddoc"].toString()){
                loadBadDoc(dt[0]["RefID"].toString())
            }
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
        //Подсосем маршруты
        FExcStr.text = "Сканируйте коробки и адрес комплектации!"
        scaningBox = ""
        curentAction = Action.ComplectationComplete
        ss.CurrentMode = Global.Mode.NewComplectationComplete
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

        //lblPrinter.text = ss.FPrinter.description
        if (curentAction == Action.Complectation) {

            remain = docDown["AllBoxes"].toString().toInt() - docDown["Boxes"].toString().toInt()

            lblInfo1.text =
                "Отобрано " + remain.toString() + " из " + docDown["AllBoxes"].toString()
            /* if (Screan === 1) {
                ShowInfoNewComp()
            }

           */
            lblState.text = "Комплектация в тележку (новая)"//docDown["View"].toString()
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
        }
        else if (curentAction == Action.ComplectationComplete) {


            /*
            if (Screan === 1) {
                ShowInfoNewComp()
            }

             */


            if (docDown.isNotEmpty()) {
                lblAdress.text =
                    "Ворота: " + docDown["Sector"].toString() + "  " + docDown["Boxes"].toString() + " м"
                lblNumber.text = docDown["NumberBill"].toString().substring(
                    docDown["NumberBill"].toString().length - 5,
                    docDown["NumberBill"].toString().length - 3
                ) +
                        " " + docDown["NumberBill"].toString()
                    .substring(docDown["NumberBill"].toString().length - 3) +
                        " сектор: " + docDown["MainSectorName"].toString()
                    .trim() + "-" + docDown["NumberCC"].toString()
                lblSetter.text = docDown["AdressCollect"].toString().trim()
            }
            else {
                lblAdress.text =""
                lblNumber.text = ""
                lblSetter.text = ""
            }

            btnKey1.visibility = if (lastGoodAdress == "") View.INVISIBLE else View.VISIBLE
            if (lastGoodAdress != "") {
                lblAdress.text =""
                lblNumber.text = ""
                lblSetter.text = ""
            }
            btnKey1.text = "TAB - Все"
            if (downSituation[0]["NumberOfOrder"].toString() != "0") {
                val number: String = downSituation[0]["NumberOfOrder"].toString()
                lblNumber.text = number.substring(if (number.length > 4) number.length - 4 else 0)
            }
            lblInfo1.text = "Всего " + downSituation[0]["AllBox"].toString() + " мест"
            lblAdress.visibility = View.VISIBLE
            lblSetter.visibility = View.VISIBLE
            if (lastGoodAdress != "") {
                //lblSetter.ForeColor = Color.DarkGray
                lblSetter.text = "'все' -->$nameLastGoodAdress"
            }
            btnCansel.visibility = View.INVISIBLE
            btnCansel.isEnabled = false
            btnCansel.text = "DEL - ПОЛОН"
            if (needAdressComplete != ss.getVoidID()) {
                btnCansel.visibility = View.VISIBLE
                btnCansel.isEnabled = true
            }

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

                    //начинается!
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
                                "CONVERT(char(8), CAST(LEFT(journForBill.date_time_iddoc, 8) as datetime), 4) as datedoc, " +
                                "journForBill.docno as docno, " +
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
                    refreshActivity()
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
                if (scaningBox == "") {
                    FExcStr.text = "Отсканируйте место!"
                    return false
                }
                val section = RefSection()
                if (!section.foundIDD(idd)) {
                    FExcStr.text = "Не найден адрес!"
                    return false
                }
                val id = section.id
                if (needAdressComplete != ss.getVoidID()) {
                    if (id != needAdressComplete) {
                        FExcStr.text = "Неверный адрес!"
                        return false
                    }
                }
                else {
                    //нужно проверить зону ворот, к тем ли воротам он относится
                    var textQuery =
                        "Select " +
                                "Zone.\$Спр.ЗоныВорот.Секция as Section, " +
                                "Gate.descr as Gate " +
                                "from \$Спр.МестаПогрузки as Ref (nolock) " +
                                "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                                "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                                "inner join DH\$КонтрольРасходной as DocCB (nolock) " +
                                "on DocCB.iddoc = DocCC.\$КонтрольНабора.ДокументОснование " +
                                "inner join \$Спр.Ворота as Gate (nolock) " +
                                "on Gate.id = DocCB.\$КонтрольРасходной.Ворота " +
                                "inner join \$Спр.ЗоныВорот as Zone (nolock) " +
                                "on Gate.id = Zone.parentext " +
                                "where Ref.id = :id"
                    textQuery = ss.querySetParam(textQuery, "id", scaningBox)
                    val dt = ss.executeWithReadNew(textQuery) ?: return false
                    if (dt.isNotEmpty()) {
                        //зона задана, надо проверять адреса
                        var findAdres = false
                        for (dr in dt) {
                            if (id == dr["Section"].toString()) {
                                findAdres = true
                                break
                            }
                        }

                        if (!findAdres) {
                            //нет такого адреса в зоне
                            FExcStr.text = "Нужен адрес из зоны " + dt[0]["Gate"].toString().trim()
                            return false
                        }
                    }
                }
                var textQuery =
                    "declare @res int; " +
                            "begin tran; " +
                            "exec WPM_CompleteBox :box, :adress, @res output; " +
                            "if @res = 0 rollback tran else commit tran; " +
                            "select @res as result; "
                textQuery = ss.querySetParam(textQuery, "box", scaningBox)
                textQuery = ss.querySetParam(textQuery, "adress", id)
                val dt = ss.executeWithReadNew(textQuery) ?: return false
                if (dt.isEmpty()) return false
                if (dt[0]["result"] == "0") {
                    FExcStr.text = "Не удалось зафиксировать комплектацию места!"
                    return false
                }
                lastGoodAdress = id
                nameLastGoodAdress = section.name

                checkFullNewComplete(scaningBox)
                toModeNewComplectationComplete()

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

    private fun checkFullNewComplete(box:String) {
        var textQuery = "declare @docCB char(9) " +
                "select @DocCB = DocCC.\$КонтрольНабора.ДокументОснование " +
                "from \$Спр.МестаПогрузки as Ref " +
                "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                "where Ref.ismark = 0 and Ref.id = :box " +
                "if exists (" +
                "select Ref.id from \$Спр.МестаПогрузки as Ref (nolock) " +
                "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                "where " +
                "Ref.ismark = 0 " +
                "and DocCC.\$КонтрольНабора.ДокументОснование = @docCB " +
                "and Ref.\$Спр.МестаПогрузки.Дата9 = :EmptyDate ) " +
                " select 1 as result " +
                " else select 0 as result, @DocCB as DocCB "
        textQuery = ss.querySetParam(textQuery, "box", box)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        val dt = ss.executeWithReadNew(textQuery)
        if (dt == null) {
            FExcStr.text = "Ошибка Запроса!"
            return
        }
        if (dt.isEmpty()) {
            FExcStr.text = "Не удалось!"
            return
        }
        if (dt[0]["result"] == "0") {
            //Тут проверяем все ли места по накладной скомплектованы и если все то хуячим!!!
            val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
            dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] =
                ss.extendID(ss.FEmployer.id, "Спр.Сотрудники")
            dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход2"] =
                ss.extendID(lastGoodAdress, "Спр.Секции")
            dataMapWrite["Спр.СинхронизацияДанных.ДокументВход"] =
                ss.extendID(dt[0]["DocCB"].toString(), "КонтрольРасходной")
            dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = 1

            execCommandNoFeedback("Complete", dataMapWrite)
        }
    }

    private fun completeAll():Boolean  {
        var textQuery =
        "begin tran; " +
                "declare @res int; " +
                "declare @box char(9) " +
                "exec WPM_CompleteAll :Employer, :Adress, :iddoc, @res OUTPUT, @box output; " +
                "if @res = 0 rollback tran " +
                "else begin commit tran select @box as box end"

        textQuery = ss.querySetParam(textQuery, "Employer",    ss.FEmployer.id)
        textQuery = ss.querySetParam(textQuery, "Adress",      lastGoodAdress)
        textQuery = ss.querySetParam(textQuery, "iddoc",       docDown["ID"].toString())
        val dt = ss.executeWithReadNew(textQuery)
        if (dt != null && dt.isNotEmpty())
        {
            checkFullNewComplete(dt[0]["box"].toString())
        }
        lastGoodAdress = ""
        docDown = mutableMapOf()
        toModeNewComplectationComplete()
        return true
    }

    private fun adressFull():Boolean {
        var textQuery =
        "begin tran " +
                "declare @res int " +
                "exec WPM_AdressCompleteFull :iddoc, @res output " +
                "if @res = 0 rollback tran else commit tran "
        textQuery = ss.querySetParam(textQuery, "iddoc",    docDown["ID"].toString())

        if (ss.executeWithoutRead(textQuery))
        {
            return false
        }

        toModeNewComplectation()
        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        // нажали назад, выйдем и разблокируем доки
        if (keyCode == 4 || keyCode == 67) {

            if (curentAction == Action.Complectation) {
                repealNewComplectation()
            }
            else {
                if (needAdressComplete != ss.getVoidID()) {
                    FExcStr.text = "Адрес полон, фиксирую..."
                    if (!adressFull()){
                        lastGoodAdress = ""
                        docDown = mutableMapOf()
                        needAdressComplete = ss.getVoidID()
                        toModeNewComplectationComplete()
                    }
                }
            }
            return true
        }
        //это таб кнопка
        if (keyCode == 61) {
            if (curentAction == Action.Complectation) {
                return true
            }

            if (lastGoodAdress != "")
            {
                //Можно завершить
                FExcStr.text = "Комплектую остальные..."
                if (completeAll())
                {
                    goodVoise()
                }
                else
                {
                    badVoise()
                }
                refreshActivity()
            }
            return true
        }
        if (ss.helper.whatDirection(keyCode) == "Left") {
            if (!loadCC()) return true

            FExcStr.text = "Подгружаю состояние..."
            val shoiseWorkInit = Intent(this, ShowInfoNewComp::class.java)
            shoiseWorkInit.putExtra("ParentForm", "NewComplectation")
            shoiseWorkInit.putExtra("BadDocID", badDoc["ID"])
            shoiseWorkInit.putExtra("BadDocView", badDoc["View"])

            startActivity(shoiseWorkInit)
            finish()
            return true
        }
        else if (ss.helper.whatDirection(keyCode) == "Right") {
            FExcStr.text = "Подгружаю маршрут..."
            val showRouteInit = Intent(this, ShowRoute::class.java)
            startActivity(showRouteInit)
            finish()
            return true
        }
        return false
    }

    fun loadCC():Boolean {
        if (badDoc["ID"] == null)
        {
            FExcStr.text = "Нет текущего сборочного!"
            return false
        }
        var textQuery = "select * from WPM_fn_ViewBillStatus(:iddoc)"

        textQuery = ss.querySetParam(textQuery, "iddoc", badDoc["ID"].toString())
        ccrp.clear()
        ccrp = ss.executeWithReadNew(textQuery) ?: return false
        return true
    }
}
