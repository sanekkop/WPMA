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
import com.intek.wpma.*
import com.intek.wpma.Ref.RefEmployer
import com.intek.wpma.Ref.RefPrinter
import com.intek.wpma.Ref.RefSection
import kotlinx.android.synthetic.main.activity_downing.*

class Downing : BarcodeDataReceiver() {

    private var docDown: MutableMap<String, String> = mutableMapOf()
    private var remain = 0
    private var downSituation: MutableList<MutableMap<String, String>> = mutableListOf()
    private var flagPrintPallete: Boolean = false
    private var oldKey = 0

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
                        reactionBarcode(barcode)
                    } catch (e: Exception) {
                        FExcStr.text = e.toString()
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
                FExcStr.text = e.toString()
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
        val oldMode = ss.CurrentMode

        ss.CurrentMode = Global.Mode.Down
        title = ss.title

        if (ss.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@Downing, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "Downing")
                startActivity(scanAct)
            }
        }
        btnCansel.setOnClickListener {
            FExcStr.text = "Для отказа удерживайте кнопку"
        }
        btnCansel.setOnLongClickListener {
            btnCansel2.visibility = View.VISIBLE
            FExcStr.text = "Подтвердите выход"
            true
        }
        btnCansel2.setOnClickListener {
            repealDown()
            btnCansel2.visibility = View.INVISIBLE
        }

        btnKey1.setOnClickListener {
            if (ss.CurrentMode == Global.Mode.DownComplete) {
                if (!flagPrintPallete) {
                    printPallete()
                    refreshActivity()
                }
            } else {
                remain =
                    docDown["AllBoxes"].toString().toInt() - docDown["Boxes"].toString().toInt()
                if (docDown["MaxStub"].toString().toInt() <= remain) {
                    //Можно завершить
                    FExcStr.text = ("Закрываю остальные $remain места...")
                    endCC()
                }
            }
        }
        if (oldMode == Global.Mode.ChoiseDown || oldMode == Global.Mode.Main) {
            toModeDown()
        }
        else {
            try {
                refreshActivity()
            } catch (e: Exception) {
                val toast = Toast.makeText(
                    applicationContext,
                    "Ошибка перехода в режим",
                    Toast.LENGTH_LONG
                )
                toast.show()
                ss.FEmployer = RefEmployer()
                val mainInit = Intent(this, MainActivity::class.java)
                startActivity(mainInit)
                finish()
            }

        }
    }

    private fun toModeDown() {
        if (!ss.FEmployer.selected){
            //не выбран сотрудник ошибка
            FExcStr.text = "Не выбран сотрудник! Ошибка!"
            badVoise()
            ss.FEmployer = RefEmployer()
            val mainInit = Intent(this, MainActivity::class.java)
            mainInit.putExtra("ParentForm", "Downing")
            startActivity(mainInit)
            finish()
            return
        }
        ss.CurrentMode = Global.Mode.Down
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
        if (ss.FEmployer.getAttribute("ПосланныйСектор").toString() != ss.getVoidID()) {
            if (sectorPriory.foundID(ss.FEmployer.getAttribute("ПосланныйСектор").toString())) {
                if (dt[0]["ParentSector"].toString().trim() != sectorPriory.name.trim()) {
                    FExcStr.text = ("Нельзя! Можно только " + sectorPriory.name.trim() + " сектор!")
                    toModeDownComplete()
                    return
                }
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
        FExcStr.text = "Сканируйте места"
        refreshActivity()
    }

    private fun toModeDownComplete() {
        if (!ss.FEmployer.selected){
            //не выбран сотрудник ошибка
            FExcStr.text = "Не выбран сотрудник! Ошибка!"
            badVoise()
            ss.FEmployer = RefEmployer()
            val mainInit = Intent(this, MainActivity::class.java)
            mainInit.putExtra("ParentForm", "Downing")
            startActivity(mainInit)
            finish()
            return
        }
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
        downSituation = ss.executeWithReadNew(textQuery) ?: return

        if (downSituation[0]["AllBox"] == "0") {
            //Нету ничего!
            val shoiseDown = Intent(this, ChoiseDown::class.java)
            shoiseDown.putExtra("ParentForm", "ChoiseDown")
            startActivity(shoiseDown)
            finish()
            return
        }
        FExcStr.text = "Напечатать этикетку, отсканировать адрес паллеты"
        //тут надо переходить в режим downComplete
        ss.CurrentMode = Global.Mode.DownComplete
        refreshActivity()
    }


    private fun repealDown() {
        var textQuery =
            "declare @res int; exec WPM_RepealSetDown :iddoc, @res output; " +
                    "select @res as result" +
                    ""
        textQuery = ss.querySetParam(textQuery, "iddoc", docDown["ID"].toString())
        if (!ss.executeWithoutRead(textQuery)) return
        toModeDown()
    }

    private fun printPallete(): Boolean {
        if (!ss.FPrinter.selected) {
            FExcStr.text = "Принтер не выбран!"
            return false
        }
        if (downSituation[0]["NumberOfOrder"].toString() == "0") {
            var textQuery =
                "declare @res int; exec WPM_GetNumberOfOrder :employer, @res output; " +
                        "select @res as result" +
                        ""
            textQuery = ss.querySetParam(textQuery, "employer", ss.FEmployer.id)
            if (!ss.executeWithoutRead(textQuery)) {
                return false
            }
            toModeDownComplete()
            //Повторно проверим, должно было присвоится!
            if (downSituation[0]["NumberOfOrder"].toString() == "0") {
                FExcStr.text = "Не удается присвоить номер задания!"
                return false
            }
        }
        FExcStr.text = "Отсканируйте адрес палетты!"
        if (!flagPrintPallete) {
            val no = downSituation[0]["NumberOfOrder"].toString()
            val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
            dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ss.extendID(
                ss.FPrinter.id,
                "Спр.Принтеры"
            )
            dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = "LabelRT.ert"
            dataMapWrite["Спр.СинхронизацияДанных.ДатаВход2"] =
                no.substring(if (no.length < 4) 0 else no.length - 4)
            if (!execCommandNoFeedback("Print", dataMapWrite)) {
                return false
            }
            flagPrintPallete = true
        }
        return true
    }

    private fun endCC(): Boolean {
        if (!ss.FEmployer.selected){
            //не выбран сотрудник ошибка
            FExcStr.text = "Не выбран сотрудник! Ошибка!"
            badVoise()
            ss.FEmployer = RefEmployer()
            val mainInit = Intent(this, MainActivity::class.java)
            mainInit.putExtra("ParentForm", "Downing")
            startActivity(mainInit)
            finish()
            return false
        }
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

        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        textQuery = ss.querySetParam(textQuery, "iddoc", docDown["ID"].toString())
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        textQuery = ss.querySetParam(textQuery, "NameParent", docDown["Sector"].toString().trim())
        if (!ss.executeWithoutRead(textQuery)) {
            return false
        }
        toModeDown()
        return true
    }

    private fun reactionBarcode(Barcode: String): Boolean {

        //из-за ошибки мертвых душ сначала проверим сотрудника
        if (!ss.FEmployer.selected){
            //не выбран сотрудник ошибка
            FExcStr.text = "Не выбран сотрудник! Ошибка!"
            badVoise()
            ss.FEmployer = RefEmployer()
            val mainInit = Intent(this, MainActivity::class.java)
            mainInit.putExtra("ParentForm", "Downing")
            startActivity(mainInit)
            finish()
            return false
        }
        val barcoderes = ss.helper.disassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if (typeBarcode == "6" && ss.CurrentMode == Global.Mode.Down) {
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
                goodVoise()
                return true
            } else {
                FExcStr.text = "Нет действий с данным ШК в данном режиме!"
                badVoise()
                return false
            }

        } else if (typeBarcode == "113") {
            //справочники типовые
            val idd = barcoderes["IDD"].toString()
            if (ss.isSC(idd, "Сотрудники")) {
                ss.FEmployer = RefEmployer()
                val mainInit = Intent(this, MainActivity::class.java)
                mainInit.putExtra("ParentForm", "Downing")
                startActivity(mainInit)
                finish()
            } else if (ss.isSC(idd, "Принтеры")) {
                if (ss.FPrinter.selected) {
                    ss.FPrinter = RefPrinter()
                } else ss.FPrinter.foundIDD(idd)
                refreshActivity()
            } else if (ss.CurrentMode == Global.Mode.DownComplete && ss.isSC(idd, "Секции")) {
                val section = RefSection()
                section.foundIDD(barcoderes["IDD"].toString())
                if (!section.selected) {
                    FExcStr.text = "Не найден адрес!"
                    badVoise()
                    return false
                }
                val id = section.id
                if (downSituation[0]["NumberOfOrder"].toString() == "0") {
                    FExcStr.text = "Не присвоен номер задания! Напечатайте этикетку!"
                    badVoise()
                    return false
                }
                var textQuery =
                    "declare @res int; exec WPM_CompletePallete :employer, :adress, @res output; "
                textQuery = ss.querySetParam(textQuery, "employer", ss.FEmployer.id)
                textQuery = ss.querySetParam(textQuery, "adress", id)
                if (!ss.executeWithoutRead(textQuery)) {
                    badVoise()
                    return false
                }
                flagPrintPallete = false
                toModeDown()
                goodVoise()
                return true
            }
        } else {
            FExcStr.text = "Нет действий с данным ШК в данном режиме!"
            badVoise()
            return false
        }
        goodVoise()
        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        // нажали назад, выйдем и разблокируем доки 67 - это делит
        if (keyCode == 4 || keyCode == 67) {
            if (btnCansel2.visibility == View.VISIBLE) {
                if (oldKey != keyCode) {
                    repealDown()
                    btnCansel2.visibility = View.INVISIBLE
                }
                else {
                    FExcStr.text = "Для отказа подтвердите выход"
                }
            }
            else {
                oldKey = keyCode
                btnCansel2.visibility = View.VISIBLE
                if (keyCode == 4) {
                    btnCansel2.text = ("DEL-ВЫХОД")
                    FExcStr.text = ("Для отказа подтвердите выход кнопкой DEL")
                }
                else {
                    btnCansel2.text = "НАЗАД-ВЫХОД"
                    FExcStr.text = "Для отказа подтвердите выход кнопкой НАЗАД"
                }

            }

            return true
        }

        //это таб кнопка
        if (keyCode == 61) {
            if (ss.CurrentMode == Global.Mode.DownComplete) {
                if (!flagPrintPallete) {
                    printPallete()
                    refreshActivity()
                }
            }
            else {
                remain =
                    docDown["AllBoxes"].toString().toInt() - docDown["Boxes"].toString().toInt()
                if (docDown["MaxStub"].toString().toInt() <= remain) {
                    //Можно завершить
                    FExcStr.text = ("Закрываю остальные $remain места...")
                    endCC()
                }
            }
            return true

        }

        //не наши кнопки вернем ложь
        return false
    }

    private fun refreshActivity() {

        lblPrinter.text = ss.FPrinter.description
        if (ss.CurrentMode == Global.Mode.Down) {
            remain = docDown["AllBoxes"].toString().toInt() - docDown["Boxes"].toString().toInt()
            lblState.text = docDown["View"].toString()
            lblInfo1.text =
                ("Отобрано " + remain.toString() + " из " + docDown["AllBoxes"].toString())
            lblNumber.text = (docDown["NumberBill"].toString().substring(
                docDown["NumberBill"].toString().length - 5,
                docDown["NumberBill"].toString().length - 3
            ) + " " +
                    docDown["NumberBill"].toString()
                        .substring(docDown["NumberBill"].toString().length - 3) +
                    " сектор: " + docDown["MainSectorName"].toString()
                .trim() + "-" + docDown["NumberCC"].toString())
            lblAdress.text = docDown["AdressCollect"].toString()
            lblSetter.text = ("отборщик: " + ss.helper.getShortFIO(docDown["SetterName"].toString()))
            lblAdress.visibility = View.VISIBLE
            lblSetter.visibility = View.VISIBLE
            btnKey1.visibility = if (docDown["MaxStub"].toString()
                    .toInt() <= remain
            ) View.VISIBLE else View.INVISIBLE
            btnKey1.text = ("TAB-Все")
        }
        else if (ss.CurrentMode == Global.Mode.DownComplete) {
            btnKey1.visibility = if (flagPrintPallete) View.INVISIBLE else View.VISIBLE
            btnKey1.text = ("TAB-Печать")
            if (downSituation[0]["NumberOfOrder"].toString() != "0") {
                val number: String = downSituation[0]["NumberOfOrder"].toString()
                lblNumber.text = number.substring(if (number.length > 4) number.length - 4 else 0)
            }
            else {
                lblNumber.text = ""
            }
            lblInfo1.text = ("Всего " + downSituation[0]["AllBox"].toString() + " мест")
            lblAdress.visibility = View.INVISIBLE
            lblSetter.visibility = View.INVISIBLE

        }
    }


}
