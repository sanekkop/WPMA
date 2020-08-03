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
import kotlinx.android.synthetic.main.activity_downing.FExcStr
import kotlinx.android.synthetic.main.activity_downing.lblState
import kotlinx.android.synthetic.main.activity_downing.terminalView


class Downing : BarcodeDataReceiver() {

    var DocDown:MutableMap<String,String> = mutableMapOf()
    var remain = 0
    enum class Action {Down,DownComplete}
    var CurentAction:Action = Action.Down
    var DownSituation:MutableList<MutableMap<String,String>> = mutableListOf()
    var FlagPrintPallete:Boolean = false

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
                Barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(Barcode)
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

        return if (ReactionKey(keyCode, event)) true else super.onKeyDown(keyCode, event)
    }
    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_downing)

        terminalView.text = SS.terminal
        title = SS.title

        if (SS.isMobile){
            btnScanDowningMode.visibility = View.VISIBLE
            btnScanDowningMode!!.setOnClickListener {
                val scanAct = Intent(this@Downing, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","Downing")
                startActivity(scanAct)
            }
        }
        btnCansel.setOnClickListener {
            RepealDown()
        }
        btnKey1.setOnClickListener {
            if (CurentAction == Action.DownComplete) {
                if (!FlagPrintPallete) {
                    PrintPallete();
                    RefreshActivity()
                }
            }
            else
            {
                remain = DocDown["AllBoxes"].toString().toInt() - DocDown["Boxes"].toString().toInt()
                if (DocDown["MaxStub"].toString().toInt() <= remain) {
                    //Можно завершить
                    FExcStr.text = "Закрываю остальные " + remain.toString().toString() + " места..."
                    EndCC()
                }
            }
        }

        ToModeDown()
    }
    fun ToModeDown() {

        CurentAction = Action.Down
        DocDown = mutableMapOf()
        var textQuery = "select * from dbo.WPM_fn_ToModeDown(:Employer)"
        textQuery = SS.QuerySetParam(textQuery, "Employer", SS.FEmployer.ID)
        val DT = SS.ExecuteWithReadNew(textQuery) ?: return
        if (DT.isEmpty()) {
            //Собирать - нечего,
            ToModeDownComplete()
            return
        }
        //проверим полученный сектор
        SS.FEmployer.Refresh()
        val SectorPriory = RefSection()
        if (SectorPriory.FoundID(SS.FEmployer.GetAttribute("ПосланныйСектор").toString())) {
            if (DT[0]["ParentSector"].toString().trim() != SectorPriory.Name.trim()) {
                FExcStr.text = "Нельзя! Можно только " + SectorPriory.Name.trim() + " сектор!"
                ToModeDownComplete()
                return
            }
        }
        DocDown.put("ID", DT[0]["iddoc"].toString())
        DocDown.put("Boxes", DT[0]["CountBox"].toString())
        DocDown.put("View", DT[0]["Sector"].toString().trim() + "-" + DT[0]["Number"].toString() + " Заявка " + DT[0]["docno"].toString() + " (" + DT[0]["DateDoc"].toString() + ")")
        DocDown.put("AdressCollect", DT[0]["AdressCollect"].toString())
        DocDown.put("Sector", DT[0]["ParentSector"].toString())
        DocDown.put("MaxStub", DT[0]["MaxStub"].toString())
        DocDown.put("AllBoxes", DT[0]["CountAllBox"].toString())
        DocDown.put("NumberBill", DT[0]["docno"].toString().trim())
        DocDown.put("NumberCC", DT[0]["Number"].toString())
        DocDown.put("MainSectorName", DT[0]["Sector"].toString())
        DocDown.put("SetterName", DT[0]["SetterName"].toString())
        FExcStr.text = "Сканируйте места"
        RefreshActivity()
    }

    fun ToModeDownComplete() {

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
                "and Ref.\$Спр.МестаПогрузки.Дата5 = :EmptyDate ";
        textQuery = SS.QuerySetParam(textQuery, "Employer", SS.FEmployer.ID);
        textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate());
        DownSituation = SS.ExecuteWithReadNew(textQuery) ?:return

        if (DownSituation[0]["AllBox"] == "0")
        {
            //Нету ничего!
            val shoiseDown = Intent(this, ChoiseDown::class.java)
            shoiseDown.putExtra("ParentForm", "ChoiseDown")
            startActivity(shoiseDown)
            finish()
            return
        }
        FExcStr.text = "Напечатать этикетку, отсканировать адрес паллеты";
        //тут надо переходить в режим downComplete
        CurentAction = Action.DownComplete
        RefreshActivity()
    }

    fun RefreshActivity(){

        lblPrinter.text = SS.FPrinter.Description
        if (CurentAction == Action.Down){
            lblState.text = DocDown["View"].toString()
            lblInfo1.text =
                "Отобрано " + remain.toString() + " из " + DocDown["AllBoxes"].toString()
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
        }
        else if (CurentAction == Action.DownComplete) {
            btnKey1.visibility = if (FlagPrintPallete) View.INVISIBLE else View.VISIBLE
            btnKey1.text = "Печать"
            if (DownSituation[0]["NumberOfOrder"].toString() != "0") {
                val Number: String = DownSituation[0]["NumberOfOrder"].toString()
                lblNumber.text = Number.substring(if (Number.length > 4) Number.length - 4 else 0)
            }
            lblInfo1.text = "Всего " + DownSituation[0]["AllBox"].toString() + " мест"
            lblAdress.visibility = View.INVISIBLE
            lblSetter.visibility = View.INVISIBLE

        }
    }

    fun RepealDown() {
        var textQuery =
        "declare @res int; exec WPM_RepealSetDown :iddoc, @res output; " +
                "select @res as result" +
                "";
        textQuery = SS.QuerySetParam(textQuery, "iddoc",    DocDown["ID"].toString())
        if (!SS.ExecuteWithoutRead(textQuery)) return
        ToModeDown()
    }

    fun PrintPallete():Boolean    {
        if (!SS.FPrinter.Selected)
        {
            FExcStr.text = "Принтер не выбран!"
            return false
        }
        if (DownSituation[0]["NumberOfOrder"].toString() == "0")
        {
            var textQuery =
            "declare @res int; exec WPM_GetNumberOfOrder :employer, @res output; " +
                    "select @res as result" +
                    "";
            textQuery = SS.QuerySetParam(textQuery, "employer",    SS.FEmployer.ID)
            if (!SS.ExecuteWithoutRead(textQuery))
            {
                return false
            }
            ToModeDownComplete()
            //Повторно проверим, должно было присвоится!
            if (DownSituation[0]["NumberOfOrder"].toString() == "0")
            {
                FExcStr.text = "Не удается присвоить номер задания!";
                return false
            }
        }
        FExcStr.text = "Отсканируйте адрес палетты!";
        if (!FlagPrintPallete)
        {
            val no = DownSituation[0]["NumberOfOrder"].toString()
            var dataMapWrite: MutableMap<String, Any> = mutableMapOf()
            dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = SS.ExtendID(SS.FPrinter.ID, "Спр.Принтеры")
            dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = "LabelRT.ert"
            dataMapWrite["Спр.СинхронизацияДанных.ДатаВход2"] = no.substring(if(no.length < 4) 0 else no.length - 4)
            if (!ExecCommandNoFeedback("Print", dataMapWrite))
            {
                return false
            }
            FlagPrintPallete = true
        }
        return true
    }

    fun EndCC():Boolean{
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
                "end ";

        textQuery = SS.QuerySetParam(textQuery, "Employer",    SS.FEmployer.ID)
        textQuery = SS.QuerySetParam(textQuery, "iddoc",       DocDown["ID"].toString())
        textQuery = SS.QuerySetParam(textQuery, "EmptyDate",   SS.GetVoidDate())
        textQuery = SS.QuerySetParam(textQuery, "NameParent",  DocDown["Sector"].toString().trim())
        if (!SS.ExecuteWithoutRead(textQuery))
        {
            return false;
        }
        ToModeDown()
        return true
    }

    private fun reactionBarcode(Barcode: String): Boolean {

        val barcoderes = SS.helper.DisassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if (typeBarcode == "6" && CurentAction == Action.Down) {
            val id = barcoderes["ID"].toString()
            if (SS.IsSC(id, "МестаПогрузки")) {

                var textQuery =
                    "Select " +
                            "\$Спр.МестаПогрузки.Дата4 as Date, " +
                            "\$Спр.МестаПогрузки.КонтрольНабора as Doc " +
                            "from \$Спр.МестаПогрузки (nolock) where id = :id";
                textQuery = SS.QuerySetParam(textQuery, "id", id)
                val DT = SS.ExecuteWithReadNew(textQuery) ?: return false
                if (DT.isEmpty()) {
                    FExcStr.text = "Нет действий с данным штрихкодом!"
                    BadVoise()
                    return false
                }
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
                            "end ";

                textQuery = SS.QuerySetParam(textQuery, "Employer", SS.FEmployer.ID);
                textQuery = SS.QuerySetParam(textQuery, "itemid", id);
                textQuery = SS.QuerySetParam(textQuery, "iddoc", DocDown["ID"].toString())
                textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate())
                textQuery =
                    SS.QuerySetParam(textQuery, "NameParent", DocDown["Sector"].toString().trim())

                if (!SS.ExecuteWithoutRead(textQuery)) {
                    BadVoise()
                    return false
                }
                ToModeDown()
                return true
            } else {
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
                mainInit.putExtra("ParentForm", "ChoiseDown")
                startActivity(mainInit)
                finish()
            }
            else if (SS.IsSC(idd, "Принтеры")) {
                if (SS.FPrinter.Selected) {
                    SS.FPrinter = RefPrinter()
                }
                else SS.FPrinter.FoundIDD(idd)
                RefreshActivity()
            }
            else if (CurentAction == Action.DownComplete && SS.IsSC(idd, "Секции")) {
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
                FlagPrintPallete = false;
                ToModeDown()
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


    private fun ReactionKey(keyCode: Int, event: KeyEvent?):Boolean {

        // нажали назад, выйдем и разблокируем доки
        if (keyCode == 4){

            return  true

        }
        //не наши кнопки вернем ложь
        return  false
    }


}
