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

    var DocDown: MutableMap<String, String> = mutableMapOf()
    var BadDoc: MutableMap<String, String> = mutableMapOf()

    enum class Action { Complectation, ComplectationComplete }

    var CurentAction: Action = Action.Complectation
    var DownSituation: MutableList<MutableMap<String, String>> = mutableListOf()
    var ScaningBox = ""
    var ScaningBoxIddoc = ""
    var NeedAdressComplete = ""

    var remain = 0
    var LastGoodAdress = ""
    var NameLastGoodAdress = ""

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
        setContentView(R.layout.activity_new_complectation)

        title = SS.title

        if (SS.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@NewComplectation, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "NewComplectation")
                startActivity(scanAct)
            }
        }
        btnCansel.setOnClickListener {
            RepealNewComplectation()
        }
        NewComplectationGetFirstOrder()
        var oldx : Float = 0F
        FExcStr.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                    val shoiseWorkInit = Intent(this, ShowInfoNewComp::class.java)
                    shoiseWorkInit.putExtra("ParentForm", "NewComplectation")
                    shoiseWorkInit.putExtra("BadDocID", BadDoc["ID"])
                    shoiseWorkInit.putExtra("BadDocView", BadDoc["View"])
                    startActivity(shoiseWorkInit)
                    finish()
                }
            }
            true
        }

    }

    fun NewComplectationGetFirstOrder() {
        var textQuery =
        "declare @res int " +
                "begin tran " +
                "exec WPM_GetFirstOrderComplectationNew :Employer, @res output " +
                "if @res = 0 rollback tran else commit tran " +
                "";
        textQuery = SS.QuerySetParam(textQuery, "Employer",    SS.FEmployer.ID);

        if (!SS.ExecuteWithoutRead(textQuery))
        {
            BadVoise()
            return
        }
        ToModeNewComplectation()
    }

    fun RepealNewComplectation() {
        if (DocDown["IsFirstOrder"] == "1") {
            FExcStr.text = "Нельзя отказаться от этого задания!";
            BadVoise()
            return
        }

        var textQuery =
            "declare @res int; exec WPM_RepealNewComplectation :iddoc, @res output; " +
                    "";
        textQuery = SS.QuerySetParam(textQuery, "iddoc", DocDown["ID"].toString())
        if (!SS.ExecuteWithoutRead(textQuery)) return
        ToModeNewComplectation()
    }

    fun ToModeNewComplectation() {

        CurentAction = Action.Complectation
        DocDown = mutableMapOf()
        var textQuery = "select * from dbo.WPM_fn_ToModeNewComplectation(:Employer)";
        textQuery = SS.QuerySetParam(textQuery, "Employer", SS.FEmployer.ID)
        val DT = SS.ExecuteWithReadNew(textQuery) ?: return
        if (DT.isEmpty()) {
            //Собирать - нечего,
            ToModeNewComplectationComplete()
            return
        }

        if (BadDoc["ID"] == null) {
            LoadBadDoc(DT[0]["RefID"].toString())
        }
        DocDown.put("ID", DT[0]["iddoc"].toString())
        DocDown.put("Boxes", DT[0]["CountBox"].toString())
        DocDown.put(
            "View",
            DT[0]["Sector"].toString()
                .trim() + "-" + DT[0]["Number"].toString() + " Заявка " + DT[0]["docno"].toString() + " (" + DT[0]["DateDoc"].toString() + ")"
        )
        DocDown.put("AdressCollect", DT[0]["AdressCollect"].toString())
        DocDown.put("Sector", DT[0]["ParentSector"].toString())
        DocDown.put("MaxStub", DT[0]["MaxStub"].toString())
        DocDown.put("AllBoxes", DT[0]["CountAllBox"].toString())
        DocDown.put("NumberBill", DT[0]["docno"].toString().trim())
        DocDown.put("NumberCC", DT[0]["Number"].toString())
        DocDown.put("MainSectorName", DT[0]["Sector"].toString())
        DocDown.put("SetterName", DT[0]["SetterName"].toString())
        DocDown.put("IsFirstOrder", DT[0]["FlagFirstOrder"].toString())
        FExcStr.text = "Сканируйте место"
        RefreshActivity()
    }

    fun ToModeNewComplectationComplete() {
        var textQuery =
            "select " +
                    "min(Ref.\$Спр.МестаПогрузки.НомерЗадания7 ) as NumberOfOrder, " +
                    "Count(*) as AllBox " +
                    "from \$Спр.МестаПогрузки as Ref (nolock) " +
                    "where " +
                    "Ref.ismark = 0 " +
                    "and Ref.\$Спр.МестаПогрузки.Сотрудник8 = :Employer " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата8 = :EmptyDate " +
                    "and Ref.\$Спр.МестаПогрузки.Дата9 = :EmptyDate ";
        textQuery = SS.QuerySetParam(textQuery, "Employer", SS.FEmployer.ID);
        textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate());
        DownSituation = SS.ExecuteWithReadNew(textQuery) ?: return

        if (DownSituation[0]["AllBox"] == "0") {
            //Нету ничего!
            val shoiseDown = Intent(this, ChoiseDown::class.java)
            shoiseDown.putExtra("ParentForm", "ChoiseDown")
            startActivity(shoiseDown)
            finish()
            return
        }
        FExcStr.text = "Сканируйте коробки и адрес комплектации!";
        ScaningBox = ""
        CurentAction = Action.ComplectationComplete
        RefreshActivity()
    }

    fun LoadBadDoc(ID: String) {
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
                    "where Ref.id = :id";
        textQuery = SS.QuerySetParam(textQuery, "id", ID);
        val DT = SS.ExecuteWithReadNew(textQuery) ?: return
        if (DT.isEmpty()) {
            return
        }
        BadDoc.put("ID", DT[0]["Doc"].toString())
        BadDoc.put(
            "View",
            DT[0]["Sector"].toString()
                .trim() + "-" + DT[0]["Number"].toString() + " " + DT[0]["DocNo"].toString() + " (" + DT[0]["DateDoc"].toString() + ") мест " + DT[0]["CountBox"].toString()
        )
    } // LoadBadDoc()

    fun RefreshActivity() {

        lblPrinter.text = SS.FPrinter.Description
        if (CurentAction == Action.Complectation) {

            remain = DocDown["AllBoxes"].toString().toInt() - DocDown["Boxes"].toString().toInt()

            lblInfo1.text =
                "Отобрано " + remain.toString() + " из " + DocDown["AllBoxes"].toString()
            /* if (Screan === 1) {
                ShowInfoNewComp()
            }

           */
            lblState.text = DocDown["View"].toString()
            lblNumber.text = DocDown["NumberBill"].toString().substring(
                DocDown["NumberBill"].toString().length - 5,
                DocDown["NumberBill"].toString().length - 3
            ) + " " +
                    DocDown["NumberBill"].toString()
                        .substring(DocDown["NumberBill"].toString().length - 3) +
                    " сектор: " + DocDown["MainSectorName"].toString()
                .trim() + "-" + DocDown["NumberCC"].toString()
            lblAdress.text = DocDown["AdressCollect"].toString()
            lblSetter.text = "отборщик: " + SS.helper.GetShortFIO(DocDown["SetterName"].toString())
            lblAdress.visibility = View.VISIBLE
            lblSetter.visibility = View.VISIBLE
            btnKey1.visibility = if (DocDown["MaxStub"].toString()
                    .toInt() <= remain
            ) View.VISIBLE else View.INVISIBLE
            btnKey1.text = "Все"
        } else if (CurentAction == Downing.Action.DownComplete) {


            /*
            if (Screan === 1) {
                ShowInfoNewComp()
            }

             */

            btnKey1.visibility = if (LastGoodAdress != "") View.INVISIBLE else View.VISIBLE
            btnKey1.text = "Печать"
            if (DownSituation[0]["NumberOfOrder"].toString() != "0") {
                val Number: String = DownSituation[0]["NumberOfOrder"].toString()
                lblNumber.text = Number.substring(if (Number.length > 4) Number.length - 4 else 0)
            }
            lblInfo1.text = "Всего " + DownSituation[0]["AllBox"].toString() + " мест"
            lblAdress.visibility = View.INVISIBLE
            lblSetter.visibility = View.VISIBLE
            if (LastGoodAdress != "0") {
                //lblSetter.ForeColor = Color.DarkGray
                lblSetter.text = "'все' --> " + NameLastGoodAdress
            } else {
                // lblSetter.ForeColor = Color.Black
            }
            btnCansel.visibility = View.INVISIBLE
            btnCansel.isEnabled = false
            btnCansel.text = "ПОЛОН"

        }
    }

    private fun reactionBarcode(Barcode: String): Boolean {
        val barcoderes = SS.helper.DisassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if (typeBarcode == "6") {
            val id = barcoderes["ID"].toString()
            if (SS.IsSC(id, "МестаПогрузки")) {
                if (CurentAction == Action.Complectation) {

                    var textQuery =
                        "Select " +
                                "\$Спр.МестаПогрузки.Дата8 as Date, " +
                                "\$Спр.МестаПогрузки.КонтрольНабора as Doc, " +
                                "\$Спр.МестаПогрузки.Сотрудник8 as Employer " +
                                "from \$Спр.МестаПогрузки (nolock) where id = :id";
                    textQuery = SS.QuerySetParam(textQuery, "id", id)
                    val DT = SS.ExecuteWithReadNew(textQuery) ?: return false
                    if (DT.isEmpty()) {
                        FExcStr.text = "Нет действий с данным штрихкодом!"
                        BadVoise()
                        return false
                    }
                    LoadBadDoc(id);//Подсосем данные по документу для просмотра состояния
                    if (DT[0]["Doc"].toString() != DocDown["ID"]) {
                        FExcStr.text = "Место от другого сборочного!"
                        BadVoise()
                        return false
                    }
                    if (!SS.IsVoidDate(DT[0]["Date"].toString())) {
                        FExcStr.text = "Место уже отобрано!"
                        BadVoise()
                        return false
                    }
                    if (DT[0]["Employer"].toString() != SS.FEmployer.ID) {
                        FExcStr.text = "Этого места нет в задании!"
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
                                "end ";

                    textQuery = SS.QuerySetParam(textQuery, "Employer", SS.FEmployer.ID);
                    textQuery = SS.QuerySetParam(textQuery, "itemid", id);
                    textQuery = SS.QuerySetParam(textQuery, "iddoc", DocDown["ID"].toString())
                    textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate());
                    textQuery = SS.QuerySetParam(
                        textQuery,
                        "NameParent",
                        DocDown["Sector"].toString().trim()
                    )

                    if (!SS.ExecuteWithoutRead(textQuery)) {
                        return false
                    }
                    ToModeNewComplectation()
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
                                "where Ref.id = :id";
                    textQuery = SS.QuerySetParam(textQuery, "id", id)
                    textQuery = SS.QuerySetParam(textQuery, "EmptyID", SS.GetVoidID())
                    textQuery = SS.QuerySetParam(textQuery, "Employer", SS.FEmployer.ID)
                    textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate())

                    LoadBadDoc(id);//Подсосем данные по документу для просмотра состояния

                    val DT = SS.ExecuteWithReadNew(textQuery) ?: return false
                    if (DT.isEmpty()) {
                        FExcStr.text = "Нет действий с данным штрихкодом!"
                        return false
                    }
                    if (!SS.IsVoidDate(DT[0]["Date"].toString())) {
                        FExcStr.text = "Место уже скомплектовано!"
                        return false
                    }
                    if (DT[0]["Employer"].toString() != SS.FEmployer.ID) {
                        FExcStr.text = "Этого места нет в задании!"
                        return false
                    }

                    DocDown.put("ID", DT[0]["Doc"].toString())
                    DocDown.put("Boxes", DT[0]["CountBox"].toString())
                    DocDown.put(
                        "View",
                        DT[0]["Sector"].toString()
                            .trim() + "-" + DT[0]["Number"].toString() + " Заявка " + DT[0]["docno"].toString() + " (" + DT[0]["datedoc"].toString() + ")"
                    )
                    DocDown.put("AdressCollect", DT[0]["Adress"].toString())
                    DocDown.put("Sector", DT[0]["Gate"].toString().trim())
                    DocDown.put("NumberBill", DT[0]["docno"].toString().trim())
                    DocDown.put("NumberCC", DT[0]["Number"].toString())
                    DocDown.put("MainSectorName", DT[0]["Sector"].toString())

                    ScaningBox = id;
                    ScaningBoxIddoc = DT[0]["Doc"].toString();
                    NeedAdressComplete = DT[0]["Adress9"].toString()
                    FExcStr.text = "Отсканируйте адрес!"
                    //OnScanBox()
                    return true

                }
            }
            else {
                FExcStr.text = "Нет действий с данным ШК в данном режиме!"
                BadVoise()
                return false
            }

        }
        else if (typeBarcode == "113") {
            //справочники типовые
            val idd = barcoderes["IDD"].toString()
            if (SS.IsSC(idd, "Сотрудники")) {
                SS.FEmployer = RefEmployer()
                val mainInit = Intent(this, MainActivity::class.java)
                mainInit.putExtra("ParentForm", "NewComplectation")
                startActivity(mainInit)
                finish()
            }
            else if (CurentAction == Action.ComplectationComplete && SS.IsSC(idd, "Секции")) {
                val id = barcoderes["ID"].toString()
                if (DownSituation[0]["NumberOfOrder"].toString() == "0") {
                    FExcStr.text = "Не присвоен номер задания! Напечатайте этикетку!"
                    return false
                }
                var textQuery =
                    "declare @res int; exec WPM_CompletePallete :employer, :adress, @res output; ";
                textQuery = SS.QuerySetParam(textQuery, "employer", SS.FEmployer.ID);
                textQuery = SS.QuerySetParam(textQuery, "adress", id);
                if (!SS.ExecuteWithoutRead(textQuery)) {
                    return false;
                }
                // FlagPrintPallete = false;
                // ToModeDown()
                return true
            }
        }
        else {
            FExcStr.text = "Нет действий с данным ШК в данном режиме!"
            BadVoise()
            return false
        }
        return true
    }

    private fun ReactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        // нажали назад, выйдем и разблокируем доки
        if (keyCode == 4) {
            return true
        }
        if (SS.helper.WhatDirection(keyCode) == "Left") {
            val shoiseWorkInit = Intent(this, ShowInfoNewComp::class.java)
            shoiseWorkInit.putExtra("ParentForm", "NewComplectation")
            shoiseWorkInit.putExtra("BadDocID", BadDoc["ID"])
            shoiseWorkInit.putExtra("BadDocView", BadDoc["View"])

            startActivity(shoiseWorkInit)
            finish()
            return true
        }
        return false
    }


}
