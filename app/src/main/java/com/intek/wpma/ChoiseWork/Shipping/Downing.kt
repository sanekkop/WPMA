package com.intek.wpma.ChoiseWork.Shipping

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.MainActivity
import com.intek.wpma.R
import com.intek.wpma.Ref.RefEmployer
import com.intek.wpma.Ref.RefPrinter
import com.intek.wpma.Ref.RefSection
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_downing.*


class Downing : BarcodeDataReceiver() {

    private var docDown:MutableMap<String,String> = mutableMapOf()
    private var remain = 0
    enum class Action {Down,DownComplete}
    private var curentAction:Action = Action.Down
    private var downSituation:MutableList<MutableMap<String,String>> = mutableListOf()
    private var flagPrintPallete:Boolean = false

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
        setContentView(R.layout.activity_downing)

        title = ss.title

        if (ss.isMobile){
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@Downing, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","Downing")
                startActivity(scanAct)
            }
        }
        btnCansel.setOnClickListener {
            repealDown()
        }
        btnKey1.setOnClickListener {
            if (curentAction == Action.DownComplete) {
                if (!flagPrintPallete) {
                    printPallete()
                    refreshActivity()
                }
            }
            else
            {
                remain = docDown["AllBoxes"].toString().toInt() - docDown["Boxes"].toString().toInt()
                if (docDown["MaxStub"].toString().toInt() <= remain) {
                    //Можно завершить
                    FExcStr.text = "Закрываю остальные $remain места..."
                    endCC()
                }
            }
        }

        toModeDown()
    }
    private fun toModeDown() {

        curentAction = Action.Down
        docDown = mutableMapOf()
        var textQuery = "select * from dbo.WPM_fn_ToModeDown(:Employer)"
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        val dt = ss.executeWithReadNew(textQuery) ?: return
        if (dt.isEmpty()) {
            //Собирать - нечего,
            toModeDownComplete()
            return
        }
        //проверим полученный сектор
        ss.FEmployer.refresh()
        val sectorPriory = RefSection()
        if (sectorPriory.foundID(ss.FEmployer.getAttribute("ПосланныйСектор").toString())) {
            if (dt[0]["ParentSector"].toString().trim() != sectorPriory.name.trim()) {
                FExcStr.text = "Нельзя! Можно только " + sectorPriory.name.trim() + " сектор!"
                toModeDownComplete()
                return
            }
        }
        docDown["ID"] = dt[0]["iddoc"].toString()
        docDown["Boxes"] = dt[0]["CountBox"].toString()
        docDown["View"] = dt[0]["Sector"].toString().trim() + "-" + dt[0]["Number"].toString() + " Заявка " + dt[0]["docno"].toString() + " (" + dt[0]["DateDoc"].toString() + ")"
        docDown["AdressCollect"] = dt[0]["AdressCollect"].toString()
        docDown["Sector"] = dt[0]["ParentSector"].toString()
        docDown["MaxStub"] = dt[0]["MaxStub"].toString()
        docDown["AllBoxes"] = dt[0]["CountAllBox"].toString()
        docDown["NumberBill"] = dt[0]["docno"].toString().trim()
        docDown["NumberCC"] = dt[0]["Number"].toString()
        docDown["MainSectorName"] = dt[0]["Sector"].toString()
        docDown["SetterName"] = dt[0]["SetterName"].toString()
        FExcStr.text = "Сканируйте места"
        refreshActivity()
    }

    private fun toModeDownComplete() {

        //В этот режим попадает только если нечего собирать по Дате4, так что если и тут нет ничего - то уходит на режим ChoiseDown
        var textQuery =
        "select " +
                "min(Ref.\$Спр.МестаПогрузки.НомерЗадания5 ) as NumberOfOrder, " +
                "Count(*) as AllBox " +
                "from \$Спр.МестаПогрузки as Ref (nolock) " +
                "where " +
                "Ref.ismark = 0 " +
                "and Ref.\$Спр.МестаПогрузки.Сотрудник4 = :Employer " +
                "and not Ref.\$Спр.МестаПогрузки.Дата4 = :EmptyDate " +
                "and Ref.\$Спр.МестаПогрузки.Дата5 = :EmptyDate "
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        downSituation = ss.executeWithReadNew(textQuery) ?:return

        if (downSituation[0]["AllBox"] == "0")
        {
            //Нету ничего!
            val shoiseDown = Intent(this, ChoiseDown::class.java)
            shoiseDown.putExtra("ParentForm", "ChoiseDown")
            startActivity(shoiseDown)
            finish()
            return
        }
        FExcStr.text = "Напечатать этикетку, отсканировать адрес паллеты"
        //тут надо переходить в режим downComplete
        curentAction = Action.DownComplete
        refreshActivity()
    }

    private fun refreshActivity(){

        lblPrinter.text = ss.FPrinter.description
        if (curentAction == Action.Down){
            lblState.text = docDown["View"].toString()
            lblInfo1.text =
                "Отобрано " + remain.toString() + " из " + docDown["AllBoxes"].toString()
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
        else if (curentAction == Action.DownComplete) {
            btnKey1.visibility = if (flagPrintPallete) View.INVISIBLE else View.VISIBLE
            btnKey1.text = "Печать"
            if (downSituation[0]["NumberOfOrder"].toString() != "0") {
                val number: String = downSituation[0]["NumberOfOrder"].toString()
                lblNumber.text = number.substring(if (number.length > 4) number.length - 4 else 0)
            }
            lblInfo1.text = "Всего " + downSituation[0]["AllBox"].toString() + " мест"
            lblAdress.visibility = View.INVISIBLE
            lblSetter.visibility = View.INVISIBLE

        }
    }

    private fun repealDown() {
        var textQuery =
        "declare @res int; exec WPM_RepealSetDown :iddoc, @res output; " +
                "select @res as result" +
                ""
        textQuery = ss.querySetParam(textQuery, "iddoc",    docDown["ID"].toString())
        if (!ss.executeWithoutRead(textQuery)) return
        toModeDown()
    }

    private fun printPallete():Boolean    {
        if (!ss.FPrinter.selected)
        {
            FExcStr.text = "Принтер не выбран!"
            return false
        }
        if (downSituation[0]["NumberOfOrder"].toString() == "0")
        {
            var textQuery =
            "declare @res int; exec WPM_GetNumberOfOrder :employer, @res output; " +
                    "select @res as result" +
                    ""
            textQuery = ss.querySetParam(textQuery, "employer",    ss.FEmployer.id)
            if (!ss.executeWithoutRead(textQuery))
            {
                return false
            }
            toModeDownComplete()
            //Повторно проверим, должно было присвоится!
            if (downSituation[0]["NumberOfOrder"].toString() == "0")
            {
                FExcStr.text = "Не удается присвоить номер задания!"
                return false
            }
        }
        FExcStr.text = "Отсканируйте адрес палетты!"
        if (!flagPrintPallete)
        {
            val no = downSituation[0]["NumberOfOrder"].toString()
            val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
            dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ss.extendID(ss.FPrinter.id, "Спр.Принтеры")
            dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = "LabelRT.ert"
            dataMapWrite["Спр.СинхронизацияДанных.ДатаВход2"] = no.substring(if(no.length < 4) 0 else no.length - 4)
            if (!execCommandNoFeedback("Print", dataMapWrite))
            {
                return false
            }
            flagPrintPallete = true
        }
        return true
    }

    private fun endCC():Boolean{
        var textQuery =
        "begin tran; " +
                "UPDATE \$Спр.МестаПогрузки " +
                "SET " +
                "\$Спр.МестаПогрузки.Дата4 = :NowDate , " +
                "\$Спр.МестаПогрузки.Время4 = :NowTime " +
                "WHERE " +
                "ismark = 0 and \$Спр.МестаПогрузки.КонтрольНабора = :iddoc ; " +
                "if @@rowcount = 0 rollback tran " +
                "else begin " +
                "declare @res int; " +
                "exec WPM_GetOrderDown :Employer, :NameParent, @res OUTPUT; " +
                "if @res = 0 rollback tran " +
                "else commit tran " +
                "end "

        textQuery = ss.querySetParam(textQuery, "Employer",    ss.FEmployer.id)
        textQuery = ss.querySetParam(textQuery, "iddoc",       docDown["ID"].toString())
        textQuery = ss.querySetParam(textQuery, "EmptyDate",   ss.getVoidDate())
        textQuery = ss.querySetParam(textQuery, "NameParent",  docDown["Sector"].toString().trim())
        if (!ss.executeWithoutRead(textQuery))
        {
            return false
        }
        toModeDown()
        return true
    }

    private fun reactionBarcode(Barcode: String): Boolean {

        val barcoderes = ss.helper.disassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if (typeBarcode == "6" && curentAction == Action.Down) {
            val id = barcoderes["ID"].toString()
            if (ss.isSC(id, "МестаПогрузки")) {

                var textQuery =
                    "Select " +
                            "\$Спр.МестаПогрузки.Дата4 as Date, " +
                            "\$Спр.МестаПогрузки.КонтрольНабора as Doc " +
                            "from \$Спр.МестаПогрузки (nolock) where id = :id"
                textQuery = ss.querySetParam(textQuery, "id", id)
                val dt = ss.executeWithReadNew(textQuery) ?: return false
                if (dt.isEmpty()) {
                    FExcStr.text = "Нет действий с данным штрихкодом!"
                    badVoise()
                    return false
                }
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

                //начнем
                textQuery =
                    "begin tran; " +
                            "UPDATE \$Спр.МестаПогрузки " +
                            "SET " +
                            "\$Спр.МестаПогрузки.Дата4 = :NowDate , " +
                            "\$Спр.МестаПогрузки.Время4 = :NowTime " +
                            "WHERE " +
                            "id = :itemid; " +
                            "if @@rowcount = 0 rollback tran " +
                            "else begin " +
                            "if exists ( select top 1 id from \$Спр.МестаПогрузки as Ref " +
                            "where " +
                            "Ref.ismark = 0 " +
                            "and Ref.\$Спр.МестаПогрузки.КонтрольНабора = :iddoc " +
                            "and Ref.\$Спр.МестаПогрузки.Дата4 = :EmptyDate ) " +
                            "commit tran " +
                            "else begin " +
                            "declare @res int; " +
                            "exec WPM_GetOrderDown :Employer, :NameParent, @res OUTPUT; " +
                            "if @res = 0 rollback tran else commit tran " +
                            "end " +
                            "end "

                textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
                textQuery = ss.querySetParam(textQuery, "itemid", id)
                textQuery = ss.querySetParam(textQuery, "iddoc", docDown["ID"].toString())
                textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
                textQuery =
                    ss.querySetParam(textQuery, "NameParent", docDown["Sector"].toString().trim())

                if (!ss.executeWithoutRead(textQuery)) {
                    badVoise()
                    return false
                }
                toModeDown()
                return true
            } else {
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
                mainInit.putExtra("ParentForm", "ChoiseDown")
                startActivity(mainInit)
                finish()
            }
            else if (ss.isSC(idd, "Принтеры")) {
                if (ss.FPrinter.selected) {
                    ss.FPrinter = RefPrinter()
                }
                else ss.FPrinter.foundIDD(idd)
                refreshActivity()
            }
            else if (curentAction == Action.DownComplete && ss.isSC(idd, "Секции")) {
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
                flagPrintPallete = false
                toModeDown()
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


    private fun reactionKey(keyCode: Int, event: KeyEvent?):Boolean {

        // нажали назад, выйдем и разблокируем доки
        if (keyCode == 4){

            return  true

        }
        //не наши кнопки вернем ложь
        return  false
    }


}
