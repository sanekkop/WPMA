package com.intek.wpma.ChoiseWork.Set

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.Model.Model
import com.intek.wpma.R
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_correct.*
import kotlinx.android.synthetic.main.activity_correct.PreviousAction
import kotlinx.android.synthetic.main.activity_correct.terminalView
import kotlinx.android.synthetic.main.activity_menu_shipping.*
import kotlinx.android.synthetic.main.activity_set.*

class Correct : BarcodeDataReceiver() {

    var iddoc: String = ""
    var AddressID: String = ""
    var Employer: String = ""
    var EmployerFlags: String = ""
    var EmployerIDD: String = ""
    var EmployerID: String = ""
    val MainWarehouse = "     D   "
    var CCItem: Model.StructItemSet? = null
    var DocSet: Model.StrictDoc? = null
    var PrinterPath = ""
    var Barcode: String = ""
    var ChoiseCorrect: Int = 0          //тип корректировки
    var CountFact: Int = 0              //при наборе маркировок, чтобы не сбились уже отсканированные QR-коды
    var EnterCount: Int = 0             //колво позиций для корректировки (вводится вручную)
    var EnterCountWithoutQRCode = 0     //колво позиций без QR - кода (вводится вручную)
    var countWithoutQRCode: Int = 0     //колво уже скорректированных позиций без QR - кода
    var countCorrect: Int = 0           //общее колво уже скорректированных позиций (с QR - кодом и без)
    var codeId: String = ""             //показатель по которому можно различать типы штрих-кодов
    var flagBtn = 0
    var flagMark = 0                    //флаг маркировки

    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    // ту прописываем что делать при событии сканирования
                    try {
                        Barcode = intent.getStringExtra("data")
                        codeId = intent.getStringExtra("codeId")
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_correct)

        Employer = intent.extras!!.getString("Employer")!!
        EmployerFlags = intent.extras!!.getString("EmployerFlags")!!
        EmployerIDD = intent.extras!!.getString("EmployerIDD")!!
        EmployerID = intent.extras!!.getString("EmployerID")!!
        iddoc = intent.extras!!.getString("iddoc")!!
        AddressID = intent.extras!!.getString("AddressID")!!
        title = SS.title
        terminalView.text = SS.terminal
        CountFact = intent.extras!!.getString("CountFact")!!.toInt()
        PrinterPath = intent.extras!!.getString("PrinterPath")!!
        //заполним заново товар и док
        GetItemAndDocSet()
        val label: TextView = findViewById(R.id.label)

        label.text = "Корректировка позиции ${CCItem!!.InvCode}"
        val enterCountCorrect: EditText = findViewById(R.id.enterCountCorrect)
        NoQRCode.setOnClickListener {
            PreviousAction.text = "Введите колво товара без QR-кода"
            enterCountCorrect?.setText("")
            enterCountCorrect.visibility = View.VISIBLE
            NoQRCode.isFocusable = false
            flagBtn = 1
        }

        //дабы не дублировать код будем эмулировать нажатие кнопки
        btnDefect.setOnClickListener {
            btnShortage.visibility = View.INVISIBLE
            btnRejection.visibility = View.INVISIBLE
            btnDefect.isFocusable = false
            ChoiseCorrect = 1
            enterCountCorrect()
        }
        btnShortage.setOnClickListener {
            btnDefect.visibility = View.INVISIBLE
            btnRejection.visibility = View.INVISIBLE
            btnShortage.isFocusable = false
            ChoiseCorrect = 2
            enterCountCorrect()
        }
        btnRejection.setOnClickListener {
            btnDefect.visibility = View.INVISIBLE
            btnShortage.visibility = View.INVISIBLE
            btnRejection.isFocusable = false
            ChoiseCorrect = 3
            enterCountCorrect()
        }
        if (SS.isMobile){
            btnScanCorrect.visibility = View.VISIBLE
            btnScanCorrect!!.setOnClickListener {
                val scanAct = Intent(this@Correct, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","SetCorrect")
                startActivity(scanAct)
            }
        }

    }

    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }

    private fun reactionBarcode(Barcode: String) {
        if (countCorrect < EnterCount) {
            if (codeId == BarcodeId) {//проверим DataMatrix ли пришедший код
                //проверим, был ли уже принят этот товар с маркировкой
                var testBatcode = Barcode.replace("'","''")

                var textQuery =
                    "SELECT SP7271, SP7274, SP7275 " +
                            "FROM SC7277 " +
                            "WHERE SC7277.SP7270 like ('%' + SUBSTRING('${testBatcode.trim()}',1,31) + '%') " +
                            "and SC7277.SP7271 = '${CCItem!!.ID}' "
                val dt = SS.ExecuteWithRead(textQuery) ?: return
                if (dt.isEmpty()){
                    PreviousAction.text = "Маркировка не найдена, либо товар уже набран/скорректирован! Отсканируйте QR - код"
                    return
                }
                if (dt[1][2].toInt() == 1 && dt[1][1] == SS.ExtendID(iddoc,"КонтрольНабора")) {
                    //корректируют позицию, которую только что набрали
                    CountFact -= 1
                }
                //найдем маркировку в справочнике МаркировкаТовара, занулим флаг
                textQuery =
                    "UPDATE SC7277 " +
                            "SET SP7274 = '${SS.ExtendID(iddoc,"КонтрольНабора")}', SP7275 = 0 " +
                            "where SC7277.SP7270 like ('%' +SUBSTRING('${testBatcode.trim()}',1,31) + '%') " +
                            "and SC7277.SP7271 = '${CCItem!!.ID}' "
                if (!SS.ExecuteWithoutRead(textQuery)) {
                    FExcStr.text = "Не удалось освободить маркировку!"
                    return
                }
                countCorrect += 1
                PreviousAction.text = "Корректировка принята " + CCItem!!.InvCode.trim() + " - " + countCorrect.toString() + " шт. ( Осталось: " + (EnterCount - countCorrect).toString() + ") Отсканируйте QR - код!"
                if (countCorrect == EnterCount) { // скорректировали задданное колво позиций
                    CompleteCorrect(ChoiseCorrect, countCorrect)
                }
            } else {
                PreviousAction.text = "Неправильный тип QR - кода"
            }
        }
    }

    private fun GetItemAndDocSet(): Boolean {
        var textQuery =
            "DECLARE @curdate DateTime; " +
                    "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock); " +
                    "select top 1 " +
                    "DocCC.SP3109 as ID, " +
                    "DocCC.lineno_ as LINENO_, " +
                    "Goods.descr as ItemName, " +
                    "Goods.SP1036 as InvCode, " +
                    "Goods.SP5086 as Details, " +
                    "DocCC.SP3110 as Count, " +
                    "DocCC.SP5508 as Adress, " +
                    "DocCC.SP3112 as Price, " +
                    "Sections.descr as AdressName, " +
                    "ISNULL(AOT.Balance, 0) as Balance, " +
                    //Реквизиты документа
                    "DocCC.iddoc as IDDOC, " +
                    "journForBill.docno as DocNo, " +
                    "CONVERT(char(8), CAST(LEFT(journForBill.date_time_iddoc, 8) as datetime), 4) as DateDoc, " +
                    "journForBill.iddoc as Bill, " +
                    "DocCCHead.SP2814 as Rows, " +
                    "Sector.descr as Sector, " +
                    "DocCCHead.SP3114 as Sum, " +
                    "DocCCHead.SP3595 as Number, " +
                    "DocCCHead.SP2841 as SelfRemovel, " +
                    "Clients.descr as Client, " +
                    "Bill.SP3094 as TypeNakl, " +
                    "isnull(DocCCHead.SP6525 , :EmptyID) as BoxID, " +
                    "AdressBox.descr as Box " +
                    "from " +
                    "DT2776 as DocCC (nolock) " +
                    "LEFT JOIN DH2776 as DocCCHead (nolock) " +
                    "ON DocCCHead.iddoc = DocCC.iddoc " +
                    "LEFT JOIN SC33 as Goods (nolock) " +
                    "ON Goods.id = DocCC.SP3109 " +
                    "LEFT JOIN SC1141 as Sections (nolock) " +
                    "ON Sections.id = DocCC.SP5508 " +
                    "LEFT JOIN ( " +
                    "select " +
                    "RegAOT.SP4342 as item, " +
                    "RegAOT.SP4344 as adress, " +
                    "sum(RegAOT.SP4347 ) as balance " +
                    "from " +
                    "RG4350 as RegAOT (nolock) " +
                    "where " +
                    "period = @curdate " +
                    "and SP4343 = :Warehouse " +
                    "and SP4345 = 2 " +
                    "group by RegAOT.SP4342 , RegAOT.SP4344 " +
                    ") as AOT " +
                    "ON AOT.item = DocCC.SP3109 and AOT.adress = DocCC.SP5508 " +
                    "LEFT JOIN DH2763 as DocCB (nolock) " +
                    "ON DocCB.iddoc = DocCCHead.SP2771 " +
                    "LEFT JOIN DH196 as Bill (nolock) " +
                    "ON Bill.iddoc = DocCB.SP2759 " +
                    "LEFT JOIN _1sjourn as journForBill (nolock) " +
                    "ON journForBill.iddoc = Bill.iddoc " +
                    "LEFT JOIN SC1141 as Sector (nolock) " +
                    "ON Sector.id = DocCCHead.SP2764 " +
                    "LEFT JOIN SC46 as Clients (nolock) " +
                    "ON Bill.SP199 = Clients.id " +
                    "LEFT JOIN SC1141 as AdressBox (nolock) " +
                    "ON AdressBox.id = DocCCHead.SP6525 " +
                    "where " +
                    "DocCC.SP5986 = :EmptyDate " +
                    "and DocCC.SP3116 = 0 " +
                    "and DocCC.SP3110 > 0 " +
                    "and DocCC.iddoc = :iddoc " +
                    "and DocCC.SP5508 = :AddressID " +
                    "order by " +
                    "DocCCHead.SP2764 , Sections.SP5103 , LINENO_"
        textQuery = SS.QuerySetParam(textQuery, "EmptyID", SS.GetVoidID())
        textQuery = SS.QuerySetParam(textQuery, "Warehouse", MainWarehouse)
        textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate())
        textQuery = SS.QuerySetParam(textQuery, "iddoc", iddoc)
        textQuery = SS.QuerySetParam(textQuery, "AddressID", AddressID)
        val dataTable = SS.ExecuteWithRead(textQuery) ?: return false

        CCItem = Model.StructItemSet(
            dataTable[1][0],                            //ID
            dataTable[1][3],                            //InvCode
            dataTable[1][2].trim(),                     //Name
            dataTable[1][7].toBigDecimal(),             //Price
            dataTable[1][5].toBigDecimal().toInt(),     //Count
            dataTable[1][5].toBigDecimal().toInt(),     //CountFact
            dataTable[1][6],                            //AdressID
            dataTable[1][8].trim(),                     //AdressName
            dataTable[1][1].toInt(),                    //CurrLine
            dataTable[1][9].toBigDecimal().toInt(),     //Balance
            dataTable[1][4].toBigDecimal().toInt(),     //Details
            dataTable[1][5].toBigDecimal().toInt(),     //OKEI2Count
            "шт",                                //OKEI2
            1                                 //OKEI2Coef
        )

        DocSet = Model.StrictDoc(
            dataTable[1][10],                           //ID
            dataTable[1][18].toInt(),                   //SelfRemovel
            "",                                   //View
            dataTable[1][14].toInt(),                   //Rows
            dataTable[1][13],                           //FromWarehouseID
            dataTable[1][19].trim(),                    //Client
            dataTable[1][16].toBigDecimal(),            //Sum
            dataTable[1][20].toInt() == 2,      //Special
            dataTable[1][22],                           //Box
            dataTable[1][21]                            //BoxID
        )

        return true

    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // нажали назад, вернемся на форму набора
        if (keyCode == 4) {
           if (ChoiseCorrect == 0) {
               PreviousAction.text = ""
               val setInitialization = Intent(this, SetInitialization::class.java)
               setInitialization.putExtra("Employer", Employer)
               setInitialization.putExtra("EmployerIDD", EmployerIDD)
               setInitialization.putExtra("EmployerFlags", EmployerFlags)
               setInitialization.putExtra("EmployerID", EmployerID)
               setInitialization.putExtra("ParentForm", "Correct")
               setInitialization.putExtra("DocSetID", DocSet!!.ID)  //вернемся на определенную, так как что-то еще осталось
               setInitialization.putExtra("AddressID", CCItem!!.AdressID)
               setInitialization.putExtra("PrinterPath", PrinterPath)
               setInitialization.putExtra("PreviousAction", PreviousAction.text.toString())
               setInitialization.putExtra("CountFact", CountFact.toString())
               setInitialization.putExtra("isMobile",SS.isMobile.toString())
               startActivity(setInitialization)
               finish()
           }
        }

        ReactionKey(keyCode, event)
        return super.onKeyDown(keyCode, event)
    }

    private fun ReactionKey(keyCode: Int, event: KeyEvent?) {

        if (keyCode in 8..10) {

            ChoiseCorrect = 0
            // нажали 1 - брак
            if (keyCode.toString() == "8") {
                btnShortage.visibility = View.INVISIBLE
                btnRejection.visibility = View.INVISIBLE
                btnDefect.isFocusable = false
                ChoiseCorrect = 1
            }
            // нажали 2 - недостача
            if (keyCode.toString() == "9") {
                btnDefect.visibility = View.INVISIBLE
                btnRejection.visibility = View.INVISIBLE
                btnShortage.isFocusable = false
                ChoiseCorrect = 2
            }
            // нажали 3 - отказ
            if (keyCode.toString() == "10") {
                btnDefect.visibility = View.INVISIBLE
                btnShortage.visibility = View.INVISIBLE
                btnRejection.isFocusable = false
                ChoiseCorrect = 3
            }
            enterCountCorrect()
        }
    }

    private fun enterCountCorrect(){
        enterCountCorrect.visibility = View.VISIBLE
        PreviousAction.text = "Укажите количество в штуках"
        enterCountCorrect.setOnKeyListener { v: View, keyCode: Int, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                try {
                    if (flagBtn == 0) {
                        EnterCount = enterCountCorrect.getText().toString().toInt()
                        if (EnterCount > CCItem!!.Count || EnterCount > (CCItem!!.Count - CountFact)) {
                            PreviousAction.text = "Нельзя скорректировать столько! "
                            if (EnterCount > (CCItem!!.Count - CountFact)) {
                                PreviousAction.text =
                                    "Нельзя скорректировать столько! Возможно: " + (CCItem!!.Count - CountFact).toString() + " шт"
                            }
                        } else {
                            //проверим есть ли маркировка
                            val textQuery =
                                "SELECT " +
                                        "Product.SP1036, Product.descr, Product.SP1436 " +
                                        "FROM " +
                                        "SC33 as Product " +
                                        "INNER JOIN SC1434 as Categories " +
                                        "ON Categories.id = Product.SP1436 " +
                                        "WHERE " +
                                        "Product.id = '${CCItem!!.ID}' and Categories.SP7268 = 1"

                            val dt = SS.ExecuteWithRead(textQuery)

                            //есть маркировка, пусть сканируют QR-code
                            if (dt!!.isNotEmpty()) {
                                flagMark = 1
                                PreviousAction.text = "Отсканируйте QR - код! (Осталось: " + (EnterCount - countCorrect).toString() + " шт)"
                                enterCountCorrect.visibility = View.INVISIBLE
                                //без QR - кода можно корректировать только при недостачи
                                if(btnShortage.isVisible) {
                                    NoQRCode.visibility = View.VISIBLE
                                }

                            } else {
                                //if (countCorrect == EnterCount) {
                                countCorrect = EnterCount
                                CompleteCorrect(ChoiseCorrect, countCorrect)
                                // }
                            }
                        }
                    } else {
                        //нажали кн "без QR - кода "
                        EnterCountWithoutQRCode = enterCountCorrect.getText().toString().toInt()
                        if (EnterCountWithoutQRCode > EnterCount - countCorrect) {
                            PreviousAction.text =
                                "Нельзя скорректировать столько! Возможно: " + (EnterCount - countCorrect).toString() + " шт"

                        } else {
                            flagBtn = 0
                            enterCountCorrect.visibility = View.INVISIBLE
                            countWithoutQRCode += EnterCountWithoutQRCode
                            countCorrect += EnterCountWithoutQRCode
                            if (countCorrect == EnterCount) {
                                //все позиций скорректированы, завершим корректировку
                                CompleteCorrect(ChoiseCorrect, countCorrect)
                            }
                            PreviousAction.text = "Корректировка принята " + CCItem!!.InvCode.trim() + " - " + countCorrect.toString() + " шт. ( Осталось: " + (EnterCount - countCorrect).toString() + ") Отсканируйте QR - код!"
                        }
                    }

                } catch (e: Exception) {

                }
            }
            false
        }
    }

    private fun CompleteCorrect(Choise: Int, CountCorrect: Int): Boolean {
        //Заглушка, рефрешим позицию, чтобы не было проблем, если оборвется связь
//        if (!ToModeSet(CCItem.AdressID, DocSet.ID))
//        {
//            FCurrentMode = Mode.SetCorrect;
//            return false;
//        }
//        FCurrentMode = Mode.SetCorrect;
        //конец заглушки

        if (CountCorrect <= 0 || CountCorrect > CCItem!!.Count) {
            PreviousAction.text = "Нельзя скорректировать столько!"
            return false
        }

        var adressCode: Int = 0
        var correctReason: String
        var what: String
        when (Choise) {
            1 -> {
                adressCode = 7
                correctReason = "   2EU   "
                what = "брак"

            }

            2 -> {
                adressCode = 12
                correctReason = "   2EV   "
                what = "недостача"
            }

            3 -> {
                adressCode = 2
                correctReason = "   2EW   "
                what = "отказ"
            }

            4 -> {
                adressCode = 2
                correctReason = "   4MG   "
                what = "отказ по ШК"
            }

            else -> {
                PreviousAction.text = "Неясная причина корректировки!"
                return false
            }
        }

        var textQuery =
            "begin tran; " +
                    "update DT2776 " +
                    "set SP3110 = :count, " +
                    "SP3114 = SP3112 *:count " +
                    "where DT2776 .iddoc = :iddoc and DT2776 .lineno_ = :currline; " +
                    "if @@rowcount > 0 begin " +
                    "insert into DT2776 (SP3108 , SP3109 , SP3110 ," +
                    "SP3111 , SP3112 , SP3113 , SP3114 ," +
                    "SP3115 , SP3116 , SP3117 , SP4977 ," +
                    "SP5507 , SP5508 , SP5509 , SP5510 ," +
                    "SP5673 , SP5986 , SP5987 , SP5988 , " +
                    "lineno_, iddoc, SP6447 ) " +
                    "select SP3108 , SP3109 , :CountCorrect ," +
                    "SP3111 , SP3112 , SP3113 , SP3112 * :CountCorrect A," +
                    "SP3115 , :CountCorrect , :Reason, SP4977 ," +
                    "SP5507 , SP5508 , :AdressCode , SP5508 ," +
                    "SP5673 , SP5986 , SP5987 , SP5988 , " +
                    "(select max(lineno_) + 1 from DT2776 where iddoc = :iddoc), iddoc, 0 " +
                    "from DT2776 as ForInst where ForInst.iddoc = :iddoc and ForInst.lineno_ = :currline; " +
                    "if @@rowcount = 0 rollback tran else commit tran " +
                    "end " +
                    "else rollback"
        textQuery = SS.QuerySetParam(textQuery, "count", CCItem!!.Count - CountCorrect)
        textQuery = SS.QuerySetParam(textQuery, "CountCorrect", CountCorrect)
        textQuery = SS.QuerySetParam(textQuery, "iddoc", DocSet!!.ID)
        textQuery = SS.QuerySetParam(textQuery, "currline", CCItem!!.CurrLine)
        textQuery = SS.QuerySetParam(textQuery, "Reason", correctReason)
        textQuery = SS.QuerySetParam(textQuery, "AdressCode", adressCode)

        if (!SS.ExecuteWithoutRead(textQuery)) {
            return false
        }
        PreviousAction.text =
            "Корректировка принята " + CCItem!!.InvCode.trim() + " - " + CountCorrect.toString() + " шт. (" + what + ")"

        // переходим обратно на форму отбора и завершаем корректировку
        val setInitialization = Intent(this, SetInitialization::class.java)
        if (CountCorrect == CCItem!!.Count) {
            setInitialization.putExtra("Employer", Employer)
            setInitialization.putExtra("EmployerIDD", EmployerIDD)
            setInitialization.putExtra("EmployerFlags", EmployerFlags)
            setInitialization.putExtra("EmployerID", EmployerID)
            setInitialization.putExtra("ParentForm", "Correct")
            setInitialization.putExtra("DocSetID", "")  //скорректировали полностью
            setInitialization.putExtra("AddressID", "")
        } else {
            setInitialization.putExtra("Employer", Employer)
            setInitialization.putExtra("EmployerIDD", EmployerIDD)
            setInitialization.putExtra("EmployerFlags", EmployerFlags)
            setInitialization.putExtra("EmployerID", EmployerID)
            setInitialization.putExtra("ParentForm", "Correct")
            setInitialization.putExtra("DocSetID", DocSet!!.ID)  //вернемся на определенную, так как что-то еще осталось
            if (CountCorrect == CCItem!!.Count) {
                setInitialization.putExtra("AddressID", "")
            } else setInitialization.putExtra("AddressID", CCItem!!.AdressID)
        }
        setInitialization.putExtra("PrinterPath", PrinterPath)
        setInitialization.putExtra("PreviousAction", PreviousAction.text.toString())
        setInitialization.putExtra("isMobile",SS.isMobile.toString())
        setInitialization.putExtra("CountFact", CountFact.toString())
        startActivity(setInitialization)
        finish()


        return true
    } // CompleteCorrect

    override fun onResume() {
        super.onResume()
        //        IntentFilter intentFilter = new IntentFilter("hsm.RECVRBI");
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
                val toast = Toast.makeText(applicationContext, "Отсутствует соединение с базой!", Toast.LENGTH_LONG)
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

}

