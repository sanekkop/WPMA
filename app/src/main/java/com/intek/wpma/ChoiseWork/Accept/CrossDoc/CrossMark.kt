package com.intek.wpma.ChoiseWork.Accept.CrossDoc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.ChoiseWork.Accept.ItemCard
import com.intek.wpma.Helpers.Helper
import com.intek.wpma.R
import com.intek.wpma.Ref.RefItem
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_cross_mark.*

class CrossMark : BarcodeDataReceiver() {

    private var idDoc = ""
    private var flagBarcode = ""
    private var itemID = ""
    private var markItemDT : MutableList<MutableMap<String, String>> = mutableListOf()
    private var naklAcc: MutableMap<String,String> = mutableMapOf()
    var item = RefItem()
    private val hh = Helper()

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
                        codeId = intent.getStringExtra("codeId")!!
                        reactionBarcode(barcode)
                    } catch (e: Exception) {
                        FExcStr.text = ("Не удалось отсканировать штрихкод!$e")
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
        return if (reactionKey(keyCode)) true else super.onKeyDown(keyCode, event)
    }

    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cross_mark)
        flagBarcode = intent.extras!!.getString("flagBarcode")!!
        title = ss.title
        itemID = intent.extras!!.getString("itemID")!!
        idDoc = intent.extras!!.getString("iddoc")!!
        item.foundID(itemID)

        if (ss.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@CrossMark, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "AccMark")
                startActivity(scanAct)
            }
        }
        btnFinishAccMarkMode!!.setOnClickListener {
            completeAccMark()
        }       //если нажали финиш, значит переходим в режим погрузки
        toModeAccMarkInicialization()
    }

    private fun completeAccMark() {         //нет еще такого будем создавать
        val markItemDTNew : MutableList<MutableMap<String, String>> = mutableListOf()
        for (rowDT in markItemDT) {
            if (rowDT["id"].toString() == "null" || rowDT["id"].toString() == "") {
                val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
                dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ss.extendID(itemID, "Спр.Товары")
                dataMapWrite["Спр.СинхронизацияДанных.ДокументВход"] = ss.extendID(idDoc, "АдресПоступление")
                dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = rowDT["Mark"].toString()
                dataMapWrite["Спр.СинхронизацияДанных.ДатаВход2"] = rowDT["Box"].toString()
                if (!execCommandNoFeedback("MarkInsert", dataMapWrite)) {
                    markItemDTNew.add(rowDT)

                }
            } else continue
        }
        if (markItemDTNew.isEmpty()) {
            val itemCardInit = Intent(this, ItemCard::class.java)
            itemCardInit.putExtra("ParentForm", "AccMark")
            itemCardInit.putExtra("flagBarcode", flagBarcode)
            itemCardInit.putExtra("itemID", itemID)
            itemCardInit.putExtra("iddoc", idDoc)
            startActivity(itemCardInit)
            finish()
            return
        }
        //ошибка
        FExcStr.text = "Не все маркировки отправленны!"
        badVoise()
        markItemDT.clear()
        markItemDT.addAll(markItemDTNew)
    }

    //наличие маркировок по товару
    private fun toModeAccMarkInicialization() {

        var textQuery = "SELECT " +
                "ISNULL(journ.iddoc, AC.iddoc ) as iddoc , " +
                "AC.iddoc as ACiddoc , " +
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
        if (naklAccTemp == null || naklAccTemp.isEmpty()) {
            //не вышли на накладную - косяк какой-то
            ss.excStr = "Ошибка перехода в режим маркировки!"
            val itemCardInit = Intent(this, ItemCard::class.java)
            itemCardInit.putExtra("ParentForm", "AccMark")
            itemCardInit.putExtra("flagBarcode", flagBarcode)
            itemCardInit.putExtra("itemID", itemID)
            itemCardInit.putExtra("iddoc", idDoc)
            startActivity(itemCardInit)
            finish()
            return
        }
        //не пусто
        naklAcc = naklAccTemp[0]
        textQuery = "select id as ID , " +
                "\$Спр.МаркировкаТовара.Маркировка as Mark , " +
                "-1*(isFolder-2) as Box ," +
                "\$Спр.МаркировкаТовара.Товар as item " +
                "from \$Спр.МаркировкаТовара (nolock) " +
                "where (\$Спр.МаркировкаТовара.ДокПоступления = '${ss.extendID(naklAcc["ACiddoc"].toString(), "АдресПоступление")}' " +
                "or \$Спр.МаркировкаТовара.ДокПоступления = '${ss.extendID(naklAcc["iddoc"].toString(), "ПриходнаяКредит")}' )" +
                "and \$Спр.МаркировкаТовара.Товар = '${itemID}' "
        markItemDT = ss.executeWithReadNew(textQuery) ?: return
        refreshActivity()
        return
    }

    private fun reactionBarcode(Barcode: String): Boolean {
        val barcoderes = hh.disassembleBarcode(Barcode)
        val idd = barcoderes["IDD"].toString()
        val textQuery: String

        //если вместо Data-Matrix пикает ШК
        if (ss.isSC(idd, "Сотрудники")) {
            FExcStr.text = "Нет действий с данным ШК! Отсканируйте маркировку."
            badVoise()
            return false
        }
        val itemTemp = RefItem()
        if (itemTemp.foundBarcode(Barcode)) {
            FExcStr.text = "Это товар, сканируйте маркировку!"
            badVoise()
            return false
        }
        val testBatcode = Barcode.replace("'", "''")
        textQuery = "SELECT " +
                "-1*(isFolder-2) as Box ," +
                "SUBSTRING(\$Спр.МаркировкаТовара.Маркировка , 1 , 31) as Mark ," +
                "\$Спр.МаркировкаТовара.Товар as item , " +
                "ID as id " +
                "FROM \$Спр.МаркировкаТовара (nolock) " +
                "where \$Спр.МаркировкаТовара.Маркировка like ('%' +SUBSTRING('${testBatcode.trim()}',1,31) + '%') "

        val dtMark = ss.executeWithReadNew(textQuery) ?: return false
        if (dtMark.isNotEmpty()) {
            FExcStr.text = "Такая маркировка уже есть в базе!"
            badVoise()
            return false
        }
        val columnArray: MutableMap<String, String> = mutableMapOf()
        columnArray["Box"] = if(codeId == barcodeId) "0" else "1"
        columnArray["id"] = ""
        columnArray["Mark"] = barcode
        columnArray["item"] = itemID
        markItemDT.add(columnArray)
        FExcStr.text = "Маркировка добавлена."
        goodVoise()
        refreshActivity()
        return true
    }

    private fun reactionKey(keyCode: Int): Boolean {
        // нажали назад, выйдем
        if (keyCode == 4) {
            val itemCardInit = Intent(this, ItemCard::class.java)
            itemCardInit.putExtra("ParentForm", "AccMark")
            itemCardInit.putExtra("flagBarcode", flagBarcode)
            itemCardInit.putExtra("itemID", itemID)
            itemCardInit.putExtra("iddoc", idDoc)
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

    //заполнение таблицы
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
        boxes.text = "Уп."
        boxes.typeface = Typeface.SERIF
        boxes.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay * 0.1).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        boxes.gravity = Gravity.CENTER
        boxes.textSize = 20F
        boxes.setTextColor(-0x1000000)

        var mark = TextView(this)
        mark.text = "Маркировка"
        mark.typeface = Typeface.SERIF
        mark.gravity = Gravity.CENTER
        mark.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay * 0.8).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
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
            val rowTitl = TableRow(this)
            linearLayout = LinearLayout(this)
            val colorline = Color.WHITE
            rowTitl.setBackgroundColor(colorline)
            //добавим строки
            number = TextView(this)
            number.text = linenom.toString()
            number.typeface = Typeface.SERIF
            number.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay * 0.1).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            number.gravity = Gravity.CENTER
            number.textSize = 18F
            number.setTextColor(-0x1000000)
            boxes = TextView(this)
            boxes.text = rowDT["Box"].toString().trim()
            boxes.typeface = Typeface.SERIF
            boxes.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay * 0.1).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            boxes.gravity = Gravity.CENTER
            boxes.textSize = 18F
            boxes.setTextColor(-0x1000000)
            mark = TextView(this)
            mark.text = if (rowDT["Mark"].toString().trim().length > 31) rowDT["Mark"].toString().trim().substring(0,31) else rowDT["Mark"].toString().trim()       //в базу пишем полностью, на терминале покажем только 31 символ
            mark.typeface = Typeface.SERIF
            mark.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay * 0.8).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            mark.gravity = Gravity.CENTER
            mark.textSize = 14F
            mark.setTextColor(-0x1000000)
            linearLayout.addView(number)
            linearLayout.addView(boxes)
            linearLayout.addView(mark)

            rowTitl.addView(linearLayout)
            table.addView(rowTitl)
        }
        lblPlacer.visibility = View.VISIBLE
        lblPlacer.text = (naklAcc["DocNo"] + " (" + naklAcc["DateDoc"]?.let { hh.shortDate(it) } + ") " + item.invCode + " ВСЕГО МАРКИРОВОК " + linenom.toString())

    }

}