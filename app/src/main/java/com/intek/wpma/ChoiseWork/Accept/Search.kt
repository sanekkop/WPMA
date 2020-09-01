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
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import kotlinx.android.synthetic.main.activity_accept.*
import kotlinx.android.synthetic.main.activity_accept.table
import com.intek.wpma.*
import com.intek.wpma.Model.Model
import com.intek.wpma.Ref.RefPalleteMove
import com.intek.wpma.SQL.SQL1S
import com.intek.wpma.SQL.SQL1S.FBarcodePallet
import com.intek.wpma.SQL.SQL1S.FPallet
import com.intek.wpma.SQL.SQL1S.FPrinter
import com.intek.wpma.SQL.SQL1S.executeWithoutRead
import com.intek.wpma.SQL.SQL1S.getSCData
import com.intek.wpma.SQL.SQL1S.isSC
import com.intek.wpma.SQL.SQL1S.sqlToDateTime
import kotlinx.android.synthetic.main.activity_accept.FExcStr
import kotlinx.android.synthetic.main.activity_set.*
import net.sourceforge.jtds.jdbc.DateTime

class Search : BarcodeDataReceiver() {

    var iddoc: String = ""
    var number: String = ""
    var barcode: String = ""
    var codeId: String = ""  //показатель по которому можно различать типы штрих-кодов

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accept)

        ParentForm = intent.extras!!.getString("ParentForm")!!

        title = ss.title
        var oldx : Float = 0F
        FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                    val backAcc = Intent(this, YapItem::class.java)
                    backAcc.putExtra("ParentForm", "Search")
                    backAcc.putExtra("Docs", iddoc)
                    backAcc.putExtra("FPrinter", ss.FPrinter.path)
                    backAcc.putExtra("FPallet", FPallet)
                    startActivity(backAcc)
                    finish()
                } else if (event.x > oldx) {
                    val backAcc = Intent(this, NoneItem::class.java)
                    backAcc.putExtra("ParentForm", "Search")
                    backAcc.putExtra("Docs", iddoc)
                    backAcc.putExtra("FPrinter", ss.FPrinter.path)
                    backAcc.putExtra("FPallet", FPallet)
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

        printPal.text = "'принтер не выбран'"
        palletPal.text = "НЕТ ПАЛЛЕТЫ"

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
        number.textSize = 12F
        number.setTextColor(-0x1000000)
        val docum = TextView(this)
        docum.text = "Накладная"
        docum.typeface = Typeface.SERIF
        docum.gravity = Gravity.CENTER
        docum.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.18).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        docum.textSize = 12F
        docum.setTextColor(-0x1000000)
        val address = TextView(this)
        address.text = "Дата"
        address.typeface = Typeface.SERIF
        address.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.2).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        address.gravity = Gravity.CENTER
        address.textSize = 12F
        address.setTextColor(-0x1000000)
        val boxes = TextView(this)
        boxes.text = "Ост-сь"
        boxes.typeface = Typeface.SERIF
        boxes.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.13).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        boxes.gravity = Gravity.CENTER
        boxes.textSize = 12F
        boxes.setTextColor(-0x1000000)
        val boxesfact = TextView(this)
        boxesfact.text = "Поставщик"
        boxesfact.typeface = Typeface.SERIF
        boxesfact.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.44).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        boxesfact.gravity = Gravity.CENTER
        boxesfact.textSize = 12F
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
                numb.textSize = 12F
                numb.setTextColor(-0x1000000)
                val doc = TextView(this)
                doc.text = DR["DocNo"]
                doc.typeface = Typeface.SERIF
                doc.gravity = Gravity.CENTER
                doc.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.18).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                doc.textSize = 12F
                doc.setTextColor(-0x1000000)
                val addr = TextView(this)
                //addr.text = DR["DateDoc"]
                addr.text = sqlToDateTime(DR["DateDoc"].toString())
                addr.typeface = Typeface.SERIF
                addr.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.2).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                addr.gravity = Gravity.CENTER
                addr.textSize = 12F
                addr.setTextColor(-0x1000000)
                val box = TextView(this)
                box.text = DR["CountRow"]
                box.typeface = Typeface.SERIF
                box.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.13).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                box.gravity = Gravity.CENTER
                box.textSize = 12F
                box.setTextColor(-0x1000000)
                val boxf = TextView(this)
                boxf.text = DR["Client"].toString().trim()
                boxf.typeface = Typeface.SERIF
                boxf.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.44).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                boxf.gravity = Gravity.CENTER
                boxf.textSize = 12F
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
        catch (e: Exception){
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

    private fun reactionBarcode(Barcode: String): Boolean {
        val idd: String = "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12)

        if (ss.isSC(idd, "Принтеры")) {
            //получим путь принтера
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

        var pal = RefPalleteMove()

        //чет через пизду работает, вроде все так, но не то
        if (FPallet == "") {
            goodVoise()
            scanPalletBarcode(FPallet)
            //FPallet = pal.pallete
            palletPal.text = FBarcodePallet
            }
        else {
            badVoise()
            palletPal.text = "НЕТ ПАЛЛЕТЫ"
            FExcStr.text = "Не выбрана паллета!"
            return false
            //scanPalletBarcode(FPallet)

        }
        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?):Boolean {

        if (keyCode == 4){
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
            backAcc.putExtra("FPrinter", ss.FPrinter.path)
            backAcc.putExtra("FPallet", FPallet)
            startActivity(backAcc)
            finish()
            return true
        }


        if (ss.helper.whatDirection(keyCode) == "Right") {
            val backAcc = Intent(this, YapItem::class.java)
            backAcc.putExtra("ParentForm", "Search")
            backAcc.putExtra("Docs", iddoc)
            backAcc.putExtra("FPrinter", ss.FPrinter.path)
            backAcc.putExtra("FPallet", FPallet)
            startActivity(backAcc)
            finish()
            return true
        }

        return false
    }

    fun scanPalletBarcode (strBarcode : String) {

        var textQuery = "declare @result char(9); exec WPM_GetIDNewPallet :Barcode, Employer, @result out; select @result;"
        textQuery = ss.querySetParam(textQuery, "Barcode", strBarcode)
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        FBarcodePallet = strBarcode
        FPallet = ss.executeScalar(textQuery) ?: return

        textQuery =
            "UPDATE \$Спр.ПеремещенияПаллет " +
                    "SET " +
                    "\$Спр.ПеремещенияПаллет.Сотрудник1 = :EmployerID, " +
                    "\$Спр.ПеремещенияПаллет.ФлагОперации = 1, " +
                    "\$Спр.ПеремещенияПаллет.Дата10 = :NowDate, " +
                    "\$Спр.ПеремещенияПаллет.Время10 = :NowTime, " +
                    "\$Спр.ПеремещенияПаллет.ТипДвижения = 4 " +
                    "WHERE \$Спр.ПеремещенияПаллет .id = :Pallet "
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        textQuery = ss.querySetParam(textQuery, "EmployerID", ss.FEmployer.id)
        textQuery = ss.querySetParam(textQuery, "Pallet", FPallet)
        var tmpDR = ss.executeWithReadNew(textQuery) ?: return



      /*  if (tmpDR.isNotEmpty()) {

            for(DR in tmpDR) {

                if (tmpDR == 0) {
                    //нет строки, значит надо добавить
                    var pal = DR["ID"]
                    strBarcode = DR["Barcode"]
                    tmpDR["Name"] = strBarcode.Substring(8, 4)
                    tmpDR["AdressID"] = ss.getVoidID()
                    FPallet = pal
                }
            }
            }*/

       //DataRow[] DR = FPallets.Select("ID = '" + FPalletID + "'")

    }
}

