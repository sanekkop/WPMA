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
import com.intek.wpma.Helpers.Helper
import com.intek.wpma.Ref.RefEmployer
import com.intek.wpma.Ref.RefSection
import kotlinx.android.synthetic.main.activity_free_complectation.*
import kotlinx.android.synthetic.main.activity_free_complectation.FExcStr
import kotlinx.android.synthetic.main.activity_free_complectation.btnCansel
import kotlinx.android.synthetic.main.activity_free_complectation.btnScan
import kotlinx.android.synthetic.main.activity_free_complectation.lblInfo1
import kotlinx.android.synthetic.main.activity_free_complectation.lblState
import kotlinx.android.synthetic.main.activity_new_complectation.*


class FreeComplectation : BarcodeDataReceiver() {

    private var docDown:MutableMap<String, String> = mutableMapOf()
    private var badDoc: MutableMap<String, String> = mutableMapOf()

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
                    catch (e: Exception) {
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
        if(scanRes != null){
            try {
                barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(barcode)
            }
            catch (e: Exception){
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
        setContentView(R.layout.activity_free_complectation)
        title = ss.title
        ss.CurrentMode = Global.Mode.FreeDownComplete

        if (ss.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@FreeComplectation, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "FreeComplectation")
                startActivity(scanAct)
            }
        }
        btnCansel.setOnClickListener {
            ss.FEmployer = RefEmployer()
            val mainInit = Intent(this, MainActivity::class.java)
            startActivity(mainInit)
            finish()
        }
        var oldx = 0F
        FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                return true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                    if (!loadCC()) return true

                    FExcStr.text = "Подгружаю состояние..."
                    val shoiseWorkInit = Intent(this, ShowInfoNewComp::class.java)
                    shoiseWorkInit.putExtra("BadDocID", badDoc["ID"])
                    shoiseWorkInit.putExtra("BadDocView", badDoc["View"])
                    startActivity(shoiseWorkInit)
                    finish()
                }
            }
            return true
        })
        toModeFreeDownComplete()
    }
    fun loadCC(): Boolean {
        if (badDoc["ID"] == null) {
            FExcStr.text = "Нет текущего сборочного!"
            return false
        }
        return true
    }

    private fun toModeFreeDownComplete() {

        var textQuery = "select * from dbo.WPM_fn_ToModeFreeDownComplete(:Employer)"
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        //QuerySetParam(ref TextQuery, "EmptyDate",   GetVoidDate());
        //QuerySetParam(ref TextQuery, "EmptyID",     GetVoidID());
        val dt = ss.executeWithReadNew(textQuery) ?: return
        if (dt.isEmpty()) {
            FExcStr.text = "Отсканируйте место!"
        }
        else {
            if (badDoc["ID"] == null) {

                loadBadDoc(dt[0]["id"].toString())
            }


            docDown["ID"] = dt[0]["id"].toString()
            docDown["View"] = dt[0]["Sector"].toString()
                .trim() + "-" + dt[0]["Number"].toString() + " Заявка " + dt[0]["docno"].toString() + " (" + dt[0]["DateDoc"].toString() + ")"
            docDown["NumberBill"] = dt[0]["docno"].toString().trim()
            docDown["NumberCC"] = dt[0]["Number"].toString()
            docDown["MainSectorName"] = dt[0]["Sector"].toString()
            docDown["SetterName"] = dt[0]["SetterName"].toString()
            docDown["Boxes"] = dt[0]["NumberBox"].toString()
            FExcStr.text = "Отсканируйте адрес!"
        }
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


        lblState.text = "Свободный спуск или комплектация"
        if (docDown["ID"] != null)
        {
            lblInfo1.text = docDown["NumberBill"].toString().substring(
                docDown["NumberBill"].toString().length - 5,
                docDown["NumberBill"].toString().length - 3
            ) + " " +
                    docDown["NumberBill"].toString()
                        .substring(docDown["NumberBill"].toString().length - 3) +
                    " сектор: " + docDown["MainSectorName"].toString()
                .trim() + "-" + docDown["NumberCC"].toString()
            lblInfo2.text = " место № " + docDown["Boxes"].toString()
        }

    }

    private fun reactionBarcode(Barcode: String): Boolean {

        val helper = Helper()
        val barcoderes = helper.disassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if (typeBarcode == "113") {
            val idd = barcoderes["IDD"].toString()
            when {
                ss.isSC(idd, "Сотрудники") -> {
                    ss.FEmployer = RefEmployer()
                    val mainInit = Intent(this, MainActivity::class.java)
                    startActivity(mainInit)
                    finish()
                }
                ss.isSC(idd, "Секции") -> {
                    if (docDown["ID"] == null) {
                        FExcStr.text = "Отсканируйте место !!!"
                        badVoise()
                        return false
                    }

                    val section = RefSection()
                    section.foundIDD(idd)
                    if (!section.selected) {
                        FExcStr.text = "Не найден адрес"
                        badVoise()
                        return false
                    }
                    //Прописываем адрес
                    var textQuery =
                        "declare @res int; exec WPM_PutInAdress :employer, :adress, @res output; select @res as result; "
                    textQuery = ss.querySetParam(textQuery, "employer", ss.FEmployer.id)
                    textQuery = ss.querySetParam(textQuery, "adress", section.id)
                    val dt = ss.executeWithReadNew(textQuery) ?: return false
                    if (dt[0]["result"].toString() != "1") {
                        toModeFreeDownComplete()
                        badVoise()
                        return false
                    }
                    toModeFreeDownComplete()
                    refreshActivity()
                }
                else -> {
                    FExcStr.text = "Нет действий с данным штрихкодом!"
                    badVoise()
                    return false
                }
            }

        } else if (typeBarcode == "6") {
            //это место
            val id = barcoderes["ID"].toString()
            if (ss.isSC(id, "МестаПогрузки")) {
                var textQuery =
                    "declare @res int; exec WPM_TakeBoxFDC :employer, :box, @res output; select @res as result;"
                textQuery = ss.querySetParam(textQuery, "employer", ss.FEmployer.id)
                textQuery = ss.querySetParam(textQuery, "box", id)
                val dt = ss.executeWithReadNew(textQuery) ?: return false

                loadBadDoc(id)//Подсосем данные по документу для просмотра состояния

                if (dt[0]["result"].toString() != "1") {
                    toModeFreeDownComplete()
                    badVoise()
                    return false
                }

                toModeFreeDownComplete()
                refreshActivity()
            } else {
                FExcStr.text = "Нет действий с данным штрихкодом!"
                badVoise()
                return false
            }
        }
        goodVoise()
        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?):Boolean {

        if (keyCode == 4 || keyCode == 67) {
            btnCansel.setOnClickListener {
                ss.FEmployer = RefEmployer()
                val mainInit = Intent(this, MainActivity::class.java)
                startActivity(mainInit)
                finish()
            }
            return true
        }
        if (ss.helper.whatDirection(keyCode) == "Left") {
            if (!loadCC()) return true

            FExcStr.text = "Подгружаю состояние..."
            val shoiseWorkInit = Intent(this, ShowInfoNewComp::class.java)
            shoiseWorkInit.putExtra("BadDocID", badDoc["ID"])
            shoiseWorkInit.putExtra("BadDocView", badDoc["View"])

            startActivity(shoiseWorkInit)
            finish()
            return true
        }
        return false
    }

}
