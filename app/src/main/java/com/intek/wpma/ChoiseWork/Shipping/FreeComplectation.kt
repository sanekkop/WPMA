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
import com.intek.wpma.Helpers.Helper
import com.intek.wpma.MainActivity
import com.intek.wpma.R
import com.intek.wpma.Ref.RefEmployer
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_free_complectation.*


class FreeComplectation : BarcodeDataReceiver() {

    var DocDown:MutableMap<String,String> = mutableMapOf()
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
        setContentView(R.layout.activity_free_complectation)
        title = SS.title

        if (SS.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@FreeComplectation, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "FreeComplectation")
                startActivity(scanAct)
            }
        }
        ToModeFreeDownComplete()
    }

    fun ToModeFreeDownComplete() {

        var textQuery = "select * from dbo.WPM_fn_ToModeFreeDownComplete(:Employer)";
        textQuery = SS.QuerySetParam(textQuery, "Employer", SS.FEmployer.ID);
        //QuerySetParam(ref TextQuery, "EmptyDate",   GetVoidDate());
        //QuerySetParam(ref TextQuery, "EmptyID",     GetVoidID());
        val DT = SS.ExecuteWithReadNew(textQuery) ?: return
        if (DT.isEmpty()) {
            FExcStr.text = "Отсканируйте место!";
        }
        else {
            if (BadDoc["ID"] == null) {

                LoadBadDoc(DT[0]["id"].toString())
            }

            DocDown.put("ID", DT[0]["id"].toString())
            DocDown.put(
                "View",
                DT[0]["Sector"].toString()
                    .trim() + "-" + DT[0]["Number"].toString() + " Заявка " + DT[0]["docno"].toString() + " (" + DT[0]["datedoc"].toString() + ")"
            )
            DocDown.put("NumberBill", DT[0]["DocNo"].toString().trim())
            DocDown.put("NumberCC", DT[0]["Number"].toString())
            DocDown.put("MainSectorName", DT[0]["Sector"].toString())
            DocDown.put("SetterName", DT[0]["SetterName"].toString())
            DocDown.put("Boxes", DT[0]["NumberBox"].toString())
            FExcStr.text = "Отсканируйте адрес!"
        }
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


        lblState.text = "Свободный спуск или комплектация"
        if (DocDown["ID"] != null)
        {
            lblInfo1.text = DocDown["NumberBill"].toString().substring(
                DocDown["NumberBill"].toString().length - 5,
                DocDown["NumberBill"].toString().length - 3
            ) + " " +
                    DocDown["NumberBill"].toString()
                        .substring(DocDown["NumberBill"].toString().length - 3) +
                    " сектор: " + DocDown["MainSectorName"].toString()
                .trim() + "-" + DocDown["NumberCC"].toString()
            lblInfo2.text = " место № " + DocDown["Boxes"].toString();
        }

    }


    private fun reactionBarcode(Barcode: String): Boolean {

        val helper: Helper = Helper()
        val barcoderes = helper.DisassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if (typeBarcode == "113")
        {
            val idd = barcoderes["IDD"].toString()
            if (SS.IsSC(idd, "Сотрудники")) {
                SS.FEmployer = RefEmployer()
                val mainInit = Intent(this, MainActivity::class.java)
                mainInit.putExtra("ParentForm", "NewComplectation")
                startActivity(mainInit)
                finish()
            }
            else if (SS.IsSC(idd, "Секции")) {
                if (DocDown["ID"] == null)
                {
                    FExcStr.text = "Отсканируйте место !!!";
                    return false;
                }

                //Прописываем адрес
                var textQuery = "declare @res int; exec WPM_PutInAdress :employer, :adress, @res output; select @res as result; ";
                textQuery = SS.QuerySetParam(textQuery, "employer", SS.FEmployer.ID);
                textQuery = SS.QuerySetParam(textQuery, "adress", barcoderes["ID"].toString());
                val DT = SS.ExecuteWithReadNew(textQuery) ?:return false
                if (DT[0]["result"].toString() != "1") {
                    ToModeFreeDownComplete();
                    return false;
                }
                ToModeFreeDownComplete()
                RefreshActivity()
            }
            else
            {
                FExcStr.text = "Нет действий с данным штрихкодом!";
                return false;
            }
        }
        else if (typeBarcode == "6") {
            //это место
            val id = barcoderes["ID"].toString()
            var textQuery = "declare @res int; exec WPM_TakeBoxFDC :employer, :box, @res output; select @res as result;";
            textQuery = SS.QuerySetParam(textQuery, "employer", SS.FEmployer.ID);
            textQuery = SS.QuerySetParam(textQuery, "box",  id);
            val DT = SS.ExecuteWithReadNew(textQuery) ?:return false

            LoadBadDoc(id);//Подсосем данные по документу для просмотра состояния

            if (DT[0]["result"].toString() != "1")
            {
                ToModeFreeDownComplete()
                return false
            }

            ToModeFreeDownComplete()
            RefreshActivity()
        }
        return true
    }

    private fun ReactionKey(keyCode: Int, event: KeyEvent?):Boolean {

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
