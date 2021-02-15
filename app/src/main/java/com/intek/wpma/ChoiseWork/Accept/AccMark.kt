package com.intek.wpma.ChoiseWork.Accept

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
import com.intek.wpma.*
import com.intek.wpma.Helpers.Helper
import com.intek.wpma.Helpers.Translation
import com.intek.wpma.Ref.RefEmployer
import com.intek.wpma.Ref.RefItem
import kotlinx.android.synthetic.main.activity_downing.*
import kotlinx.android.synthetic.main.activity_acc_mark.*
import kotlinx.android.synthetic.main.activity_acc_mark.FExcStr
import kotlinx.android.synthetic.main.activity_acc_mark.btnScan
import kotlinx.android.synthetic.main.activity_acc_mark.lblPlacer
import kotlinx.android.synthetic.main.activity_acc_mark.table



class AccMark : BarcodeDataReceiver() {

    var idDoc = ""
    var flagBarcode = ""
    var itemID = ""
    var markItemDT : MutableList<MutableMap<String, String>> = mutableListOf()
    private var naklAcc: MutableMap<String,String> = mutableMapOf()
    val item = RefItem()

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
                        FExcStr.text = "Не удалось отсканировать штрихкод!" + e.toString()
                        badVoise()
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
        setContentView(R.layout.activity_acc_mark)
        flagBarcode = intent.extras!!.getString("flagBarcode")!!
        title = ss.title
        itemID = intent.extras!!.getString("itemID")!!
        idDoc = intent.extras!!.getString("iddoc")!!
        item.foundID(itemID)

        if (ss.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@AccMark, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "AccMark")
                startActivity(scanAct)
            }
        }
        btnFinishAccMarkMode!!.setOnClickListener {
            //еслинажали финиш, значит переходим в режим погрузки
            completeAccMark()
        }

        toModeAccMarkInicialization()
    }

    private fun completeAccMark() {

        //проверим чек погрузки
        var textQuery =
            "SELECT " +
                    "Main.DocFull as DocFull " +
                    "FROM ("
                    ") as Main " +
                    "INNER JOIN (" +
                    "SELECT " +
                    "Boxes.\$Спр.МестаПогрузки.Док as DocID " +
                    "FROM \$Спр.МестаПогрузки as Boxes (nolock) " +
                    "WHERE Boxes.ismark = 0 and Boxes.\$Спр.МестаПогрузки.Дата6 = :EmptyDate " +
                    "GROUP BY Boxes.\$Спр.МестаПогрузки.Док " +
                    ") as Boxes " +
                    "ON Boxes.DocID = Main.DocFull " +
                    ""
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
        textQuery = ss.querySetParam(textQuery, "iddoc","")


        if (!ss.executeWithoutRead(textQuery)) {
            FExcStr.text = "Ошибка фиксации маркировки"
            badVoise()
            return
        }
        val itemCardInit = Intent(this, ItemCard::class.java)
        itemCardInit.putExtra("ParentForm", "AccMark")
        itemCardInit.putExtra("flagBarcode", flagBarcode)
        itemCardInit.putExtra("itemID", itemID)
        itemCardInit.putExtra("idDoc", idDoc)
        startActivity(itemCardInit)
        finish()
    }

    private fun toModeAccMarkInicialization() {
        var textQuery = "SELECT " +
                "ISNULL(journ.iddoc, AC.iddoc ) as iddoc , " +
                "ISNULL(journ.docno, journAC.docno ) as DocNo , " +
                "CAST(LEFT(ISNULL(journ.date_time_iddoc, journAC.date_time_iddoc ), 8) as datetime) as DateDoc , " +
                "CONVERT(char(8), CAST(LEFT(ISNULL(journ.date_time_iddoc, journAC.date_time_iddoc ),8) as datetime), 4) as DateDocText " +
                "FROM " +
                " DH\$АдресПоступление as AC (nolock) " +
                "INNER JOIN _1sjourn as journAC (nolock) " +
                "     ON journAC.iddoc = AC.iddoc " +
                "LEFT JOIN _1sjourn as journ (nolock) " +
                "     ON journ.iddoc = right(AC.\$АдресПоступление.ДокументОснование , 9) " +
                "LEFT JOIN DH\$ПриходнаяКредит as PK (nolock) " +
                "     ON journ.iddoc = PK.iddoc " +
                "WHERE" +
                " AC.iddoc = '${idDoc}' "

       val naklAccTemp = ss.executeWithReadNew(textQuery)
        if (naklAccTemp == null || naklAccTemp.isEmpty())
        {
            //не вышли на накладную - косяк какой-то
            ss.excStr = "Ошибка перехода в режим маркировки!"
            val itemCardInit = Intent(this, ItemCard::class.java)
            itemCardInit.putExtra("ParentForm", "AccMark")
            itemCardInit.putExtra("flagBarcode", flagBarcode)
            itemCardInit.putExtra("itemID", itemID)
            itemCardInit.putExtra("idDoc", idDoc)
            startActivity(itemCardInit)
            finish()
            return
        }
        //не пусто
        naklAcc = naklAccTemp[0]
        textQuery = "select id as ID , " +
                    "\$Спр.МаркировкаТовара.Маркировка as Mark , " +
                    "isFolder as Box " +
                    "from \$Спр.МаркировкаТовара (nolock) " +
                    "where \$Спр.МаркировкаТовара.ДокПоступления = '${ss.extendID(naklAcc["iddoc"].toString(), "ПриходнаяКредит")}' " +
                    "and \$Спр.МаркировкаТовара.Товар = '${itemID}' "
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        val markItemDT = ss.executeWithReadNew(textQuery) ?: return

        if (markItemDT.isNotEmpty()) {
            //существует документ!
            return
        }
        refreshActivity()
        return
    }

    private fun reactionBarcode(Barcode: String): Boolean {

        val helper = Helper()
        val barcoderes = helper.disassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        val idd = barcoderes["IDD"].toString()
        if (ss.isSC(idd, "Сотрудники")) {

        } else {
            FExcStr.text = "Нет действий с данным ШК! Отсканируйте маркировку."
            badVoise()
            return false
        }
        goodVoise()
        refreshActivity()
        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        // нажали назад, выйдем
        if (keyCode == 4) {
            val itemCardInit = Intent(this, ItemCard::class.java)
            itemCardInit.putExtra("ParentForm", "AccMark")
            itemCardInit.putExtra("flagBarcode", flagBarcode)
            itemCardInit.putExtra("itemID", itemID)
            itemCardInit.putExtra("idDoc", idDoc)
            startActivity(itemCardInit)
            finish()
            return true
        }
        else if (keyCode == 67){
            completeAccMark()
            return true
       }
        return false
    }

    fun refreshActivity() {
        table.removeAllViewsInLayout()
        var linearLayout = LinearLayout(this)
        val rowTitle = TableRow(this)
        //добавим столбцы
        var number = TextView(this)
        number.text = "№"
        number.typeface = Typeface.SERIF
        number.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay * 0.1).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        number.gravity = Gravity.CENTER
        number.textSize = 20F
        number.setTextColor(-0x1000000)

        var boxes = TextView(this)
        boxes.text = "Упаковка"
        boxes.typeface = Typeface.SERIF
        boxes.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay * 0.3).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        boxes.gravity = Gravity.CENTER
        boxes.textSize = 20F
        boxes.setTextColor(-0x1000000)

        var mark = TextView(this)
        mark.text = "Маркировка"
        mark.typeface = Typeface.SERIF
        mark.gravity = Gravity.CENTER
        mark.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay * 0.6).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        mark.textSize = 20F
        mark.setTextColor(-0x1000000)

        linearLayout.addView(number)
        linearLayout.addView(boxes)
        linearLayout.addView(mark)

        rowTitle.addView(linearLayout)
        table.addView(rowTitle)
        var linenom = 0 //1 строку шапки не считаем

        for (rowDT in markItemDT) {
            //строки теперь
            linenom++
            val rowTitle = TableRow(this)
            linearLayout = LinearLayout(this)
            var colorline = Color.WHITE
            rowTitle.setBackgroundColor(colorline)
            //добавим строки
            number = TextView(this)
            number.text = linenom.toString()
            number.typeface = Typeface.SERIF
            number.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay * 0.1).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            number.gravity = Gravity.CENTER
            number.textSize = 20F
            number.setTextColor(-0x1000000)
            boxes = TextView(this)
            boxes.text = rowDT["Box"]
            boxes.typeface = Typeface.SERIF
            boxes.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay * 0.3).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            boxes.gravity = Gravity.CENTER
            boxes.textSize = 20F
            boxes.setTextColor(-0x1000000)
            mark = TextView(this)
            mark.text = rowDT["Mark"]
            mark.typeface = Typeface.SERIF
            mark.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay * 0.6).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            mark.gravity = Gravity.CENTER
            mark.textSize = 20F
            mark.setTextColor(-0x1000000)
            linearLayout.addView(number)
            linearLayout.addView(boxes)
            linearLayout.addView(mark)

            rowTitle.addView(linearLayout)
            table.addView(rowTitle)

        }
        lblPlacer.visibility = View.VISIBLE
        lblPlacer.text = naklAcc["DocNo"] + " (" + naklAcc["DateDoc"] + ") " + item.invCode + " ВСЕГО МАРКИРОВОК " + linenom.toString()

    }

}