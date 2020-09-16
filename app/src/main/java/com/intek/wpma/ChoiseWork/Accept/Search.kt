package com.intek.wpma.ChoiseWork.Accept

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.ParentForm
import com.intek.wpma.R
import com.intek.wpma.Ref.RefPalleteMove
import com.intek.wpma.SQL.SQL1S.FBarcodePallet
import com.intek.wpma.SQL.SQL1S.sqlToDateTime
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_accept.*

class Search : BarcodeDataReceiver() {

    var iddoc: String = ""
    var number: String = ""
    var barcode: String = ""
    var codeId: String = ""  //показатель по которому можно различать типы штрих-кодов
    val pal = RefPalleteMove()

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
                        badVoise()
                    }

                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accept)

        ParentForm = intent.extras!!.getString("ParentForm")!!

        if (ss.FPrinter.selected) {
            printPal.text = ss.FPrinter.path
        }
            if (ss.FPallet != "") {
            pal.foundID(ss.FPallet)
            palletPal.text = pal.pallete
        }

        title = ss.title
        var oldx = 0F
        FExcStr.setOnTouchListener(fun(v : View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                    val backAcc = Intent(this, YapItem::class.java)
                    backAcc.putExtra("ParentForm", "Search")
                    backAcc.putExtra("Docs", iddoc)
                    startActivity(backAcc)
                    finish()
                } else if (event.x > oldx) {
                    val backAcc = Intent(this, NoneItem::class.java)
                    backAcc.putExtra("ParentForm", "Search")
                    backAcc.putExtra("Docs", iddoc)
                    startActivity(backAcc)
                    finish()
                }
            }
            return true
        })

        if (ss.isMobile){
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@Search, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","Search")
                startActivity(scanAct)
            }
        }

        toModeAcceptance()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun toModeAcceptance() {

        val linearLayout1 = LinearLayout(this)
        val rowTitle1 = TableRow(this)

        //добавим столбцы
        val number = TextView(this)
        number.text = "№"
        number.typeface = Typeface.SERIF
        number.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.05).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        number.gravity = Gravity.CENTER
        number.textSize = 14F
        number.setTextColor(-0x1000000)
        val docum = TextView(this)
        docum.text = "Накладная"
        docum.typeface = Typeface.SERIF
        docum.gravity = Gravity.CENTER
        docum.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.18).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        docum.textSize = 14F
        docum.setTextColor(-0x1000000)
        val address = TextView(this)
        address.text = "Дата"
        address.typeface = Typeface.SERIF
        address.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.2).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        address.gravity = Gravity.CENTER
        address.textSize = 14F
        address.setTextColor(-0x1000000)
        val boxes = TextView(this)
        boxes.text = "Ост-сь"
        boxes.typeface = Typeface.SERIF
        boxes.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.13).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        boxes.gravity = Gravity.CENTER
        boxes.textSize = 14F
        boxes.setTextColor(-0x1000000)
        val boxesfact = TextView(this)
        boxesfact.text = "Поставщик"
        boxesfact.typeface = Typeface.SERIF
        boxesfact.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.44).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        boxesfact.gravity = Gravity.CENTER
        boxesfact.textSize = 14F
        boxesfact.setTextColor(-0x1000000)

        linearLayout1.addView(number)
        linearLayout1.addView(docum)
        linearLayout1.addView(address)
        linearLayout1.addView(boxes)
        linearLayout1.addView(boxesfact)

        rowTitle1.addView(linearLayout1)
        rowTitle1.setBackgroundColor(Color.GRAY)
        table.addView(rowTitle1)

        getDocsAccept()

        var textQuery = "SELECT " +
                "identity(int, 1, 1) as Number , " +
                "AC.iddoc as ACID , " +
                "journ.iddoc as ParentIDD , " +
                "Clients.descr as Client , " +
                "AC.\$АдресПоступление.КолСтрок as CountRow , " +
                "journ.docno as DocNo , " +
                "CAST(LEFT(journ.date_time_iddoc, 8) as datetime) as DateDoc , " +
                "CONVERT(char(8), CAST(LEFT(journ.date_time_iddoc,8) as datetime), 4) as DateDocText " +
                "into #temp " +
                "FROM" +
                " DH\$АдресПоступление as AC (nolock) " +
                "LEFT JOIN _1sjourn as journ (nolock) " +
                "     ON journ.iddoc = right(AC.\$АдресПоступление.ДокументОснование , 9) " +
                "LEFT JOIN DH\$ПриходнаяКредит as PK (nolock) " +
                "     ON journ.iddoc = PK.iddoc " +
                "LEFT JOIN \$Спр.Клиенты as Clients (nolock) " +
                "     ON PK.\$ПриходнаяКредит.Клиент = Clients.id " +
                "WHERE" +
                " AC.iddoc in ($iddoc) " +
                " select * from #temp " +
                " drop table #temp "

        textQuery = ss.querySetParam(textQuery, "Number", number)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        val dataTable = ss.executeWithReadNew(textQuery) ?: return

        if (dataTable.isNotEmpty()) {

            for (DR in dataTable) {

                val linearLayout = LinearLayout(this)
                val rowTitle = TableRow(this)

                //добавим столбцы
                val numb = TextView(this)
                numb.text = DR["Number"]
                numb.typeface = Typeface.SERIF
                numb.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.05).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                numb.gravity = Gravity.CENTER
                numb.textSize = 14F
                numb.setTextColor(-0x1000000)
                val doc = TextView(this)
                doc.text = DR["DocNo"]
                doc.typeface = Typeface.SERIF
                doc.gravity = Gravity.CENTER
                doc.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.18).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                doc.textSize = 14F
                doc.setTextColor(-0x1000000)
                val addr = TextView(this)
                //addr.text = DR["DateDoc"]
                addr.text = sqlToDateTime(DR["DateDoc"].toString())
                addr.typeface = Typeface.SERIF
                addr.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.2).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                addr.gravity = Gravity.CENTER
                addr.textSize = 14F
                addr.setTextColor(-0x1000000)
                val box = TextView(this)
                box.text = DR["CountRow"]
                box.typeface = Typeface.SERIF
                box.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.13).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                box.gravity = Gravity.CENTER
                box.textSize = 14F
                box.setTextColor(-0x1000000)
                val boxf = TextView(this)
                boxf.text = DR["Client"].toString().trim()
                boxf.typeface = Typeface.SERIF
                boxf.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.44).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                boxf.gravity = Gravity.CENTER
                boxf.textSize = 14F
                boxf.setTextColor(-0x1000000)

                linearLayout.addView(numb)
                linearLayout.addView(doc)
                linearLayout.addView(addr)
                linearLayout.addView(box)
                linearLayout.addView(boxf)

                rowTitle.addView(linearLayout)
                table.addView(rowTitle)
            }
        }
    }

    private fun getDocsAccept() : Boolean {
        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ss.extendID(ss.FEmployer.id, "Спр.Сотрудники")
        var dataMapRead: MutableMap<String, Any> = mutableMapOf()
        val fieldList: MutableList<String> = mutableListOf("Спр.СинхронизацияДанных.ДатаРез1")
        try {
            dataMapRead = execCommand("QuestAcceptance", dataMapWrite, fieldList, dataMapRead)
        }
        catch (e: Exception) {
            badVoise()
            val toast = Toast.makeText(applicationContext, "Не удалось получить задание!", Toast.LENGTH_SHORT)
            toast.show()
        }
        if ((dataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() == -3) {
            badVoise()
            FExcStr.text = "Заданий нет!"
            return false
        }
        if ((dataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() != 3) {
            badVoise()
            FExcStr.text = "Не известный ответ робота... я озадачен..."
            return false
        }
        iddoc = dataMapRead["Спр.СинхронизацияДанных.ДатаРез1"].toString()
        return true
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

    fun reactionBarcode(Barcode: String): Boolean {
        val idd: String = "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12)

        if (ss.isSC(idd, "Принтеры")) {
            if(!ss.FPrinter.foundIDD(idd)) {
                return false
            }
            goodVoise()
            printPal.text = ss.FPrinter.path
            return true
        }

        if (!ss.FPrinter.selected) {
            badVoise()
            FExcStr.text = "Не выбран принтер!"
            return false
        }
        if (palletPal.text == "НЕТ ПАЛЛЕТЫ") {
            scanPalletBarcode(ss.FPallet)
            pal.foundID(ss.FPallet)
            palletPal.text = pal.pallete
            goodVoise()
        }
        else {
            palletPal.text = "НЕТ ПАЛЛЕТЫ"
            FExcStr.text = "Не выбрана паллета!"
            badVoise()
            return false
        }
        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?):Boolean {

        if (keyCode == 4) {
            val accMen = Intent(this, AccMenu::class.java)
            accMen.putExtra("ParentForm", "Search")
            startActivity(accMen)
            finish()
            return true
        }

        if (ss.helper.whatDirection(keyCode) == "Left") {
            val backAcc = Intent(this, NoneItem::class.java)
            backAcc.putExtra("ParentForm", "Search")
            backAcc.putExtra("Docs", iddoc)
            startActivity(backAcc)
            finish()
            return true
        }

        if (ss.helper.whatDirection(keyCode) == "Right") {
            val backAcc = Intent(this, YapItem::class.java)
            backAcc.putExtra("ParentForm", "Search")
            backAcc.putExtra("Docs", iddoc)
            startActivity(backAcc)
            finish()
            return true
        }
        return false
    }

    private fun scanPalletBarcode (strBarcode : String) {

        var textQuery = "declare @result char(9); exec WPM_GetIDNewPallet :Barcode, Employer, @result out; select @result;"
        textQuery = ss.querySetParam(textQuery, "Barcode", barcode)
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        ss.FBarcodePallet = barcode
        ss.FPallet = ss.executeScalar(textQuery) ?: return

        textQuery = "UPDATE \$Спр.ПеремещенияПаллет " +
                    "SET " +
                    "\$Спр.ПеремещенияПаллет.Сотрудник1 = :EmployerID, " +
                    "\$Спр.ПеремещенияПаллет.ФлагОперации = 1, " +
                    "\$Спр.ПеремещенияПаллет.Дата10 = :NowDate, " +
                    "\$Спр.ПеремещенияПаллет.Время10 = :NowTime, " +
                    "\$Спр.ПеремещенияПаллет.ТипДвижения = 4 " +
                    "WHERE \$Спр.ПеремещенияПаллет .id = :Pallet "
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        textQuery = ss.querySetParam(textQuery, "EmployerID", ss.FEmployer.id)
        textQuery = ss.querySetParam(textQuery, "Pallet", ss.FPallet)
        var tmpDR = ss.executeWithReadNew(textQuery) ?: return
           }
}

