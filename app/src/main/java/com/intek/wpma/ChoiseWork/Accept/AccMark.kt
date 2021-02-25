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
import com.intek.wpma.Ref.RefItem
import kotlinx.android.synthetic.main.activity_acc_mark.*
import kotlinx.android.synthetic.main.activity_acc_mark.FExcStr
import kotlinx.android.synthetic.main.activity_acc_mark.btnScan
import kotlinx.android.synthetic.main.activity_downing.*
import kotlinx.android.synthetic.main.activity_remark_mark.*
import kotlinx.android.synthetic.main.activity_set.*

class AccMark : BarcodeDataReceiver() {

    var idDoc = ""
    var flagBarcode = ""
    var itemID = ""
    var markItemDT : MutableList<MutableMap<String, String>> = mutableListOf()
    private var naklAcc: MutableMap<String,String> = mutableMapOf()
    private var markBox = ""  //ШК коробочной маркировки
    var item = RefItem()
    val hh = Helper()

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
            //если нажали финиш, значит переходим в режим погрузки
            completeAccMark()
        }

        toModeAccMarkInicialization()
    }


    private fun completeAccMark() {
        //нет еще такого будем создавать
        var markItemDTNew : MutableList<MutableMap<String, String>> = mutableListOf()
        for (rowDT in markItemDT) {
            if (rowDT["id"].toString() == "null" || rowDT["id"].toString() == "") {
                if (rowDT["Box"] == "0") {
                    val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
                    dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ss.extendID(itemID, "Спр.Товары")
                    dataMapWrite["Спр.СинхронизацияДанных.ДокументВход"] = ss.extendID(idDoc, "АдресПоступление")
                    dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = rowDT["Mark"].toString()
                    dataMapWrite["Спр.СинхронизацияДанных.ДатаВход2"] = rowDT["Box"].toString()
                    if (!execCommandNoFeedback("MarkInsert", dataMapWrite)) {
                        markItemDTNew.add(rowDT)
                    }
                }
                if (rowDT["Box"] == "1") {
                    val dataMapBox: MutableMap<String, Any> = mutableMapOf()
                    dataMapBox["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ""        //ss.extendID(itemID, "Спр.Товары")
                    dataMapBox["Спр.СинхронизацияДанных.ДокументВход"] = ""        //ss.extendID(idDoc, "АдресПоступление")
                    dataMapBox["Спр.СинхронизацияДанных.ДатаВход1"] = rowDT["Mark"].toString()
                    dataMapBox["Спр.СинхронизацияДанных.ДатаВход2"] = rowDT["Box"].toString()
                    if (!execCommandNoFeedback("MarkInsert", dataMapBox)) {
                        markItemDTNew.add(rowDT)
                    }
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
            itemCardInit.putExtra("iddoc", idDoc)
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
        val barcoderes = hh.disassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        val idd = barcoderes["IDD"].toString()
        var textQuery = ""

        //если вместо Data-Matrix пикает ШК
        if (ss.isSC(idd, "Сотрудники")) {
           FExcStr.text = "Нет действий с данным ШК! Отсканируйте маркировку."
           badVoise()
           return false
        }

        //если пикаем Data-Matrix
        if (codeId == barcodeId) {
            barcoderes["Box"] = "0"
            val testBatcode = Barcode.replace("'", "''")
            textQuery = "SELECT " +
                        "\$Спр.МаркировкаТовара.ФлагОтгрузки as Отгружен ," +
                        "SUBSTRING(\$Спр.МаркировкаТовара.ДокОтгрузки , 5 , 9) as ДокОтгрузки ," +
                        "SUBSTRING(\$Спр.МаркировкаТовара.Маркировка , 1 , 31) as Маркировка ," +
                        "\$Спр.МаркировкаТовара.Товар as Товар , " +
                        "ID as id " +
                        "FROM \$Спр.МаркировкаТовара (nolock) " +
                        "where \$Спр.МаркировкаТовара.Маркировка like ('%' +SUBSTRING('${testBatcode.trim()}',1,31) + '%') "

            val dtMark = ss.executeWithReadNew(textQuery) ?: return false

            //если мы натыкаемся на пустое значение, значит нужно его записать
            if (dtMark.isEmpty()) {
                FExcStr.text = "Маркировка не найдена. Давайте занесем её в базу."
                if (barcoderes["id"] == null) barcoderes["id"] = ""
                if (barcoderes["Mark"] == null) barcoderes["Mark"] = barcode
                //if (barcoderes["Box"] == null) barcoderes["Box"] = "0" //так как это DataMatrix, то каждый код уникален, значение всегда должно равняться единице
                dtMark.add(barcoderes)    //заполняем значениями пустой список
                markItemDT = dtMark       //и выкидываем это дальше
                }
            else {
                FExcStr.text = "Такая маркировка уже есть в базе!"
                badVoise()
                return false
                }
            }
        else {

            val itemTemp = RefItem()
            val dtMarkBox = ss.executeWithReadNew(textQuery) ?: return false
            barcoderes["Box"] = "1"
            //теперь случай, когда сканируется коробочная маркировка товара, то есть ШК в 20-21 символ
            if (barcode.length >= 20) {
                FExcStr.text = "Коробочная маркировка"
                markBox = barcode
                if (barcoderes["id"] == null) barcoderes["id"] = ""
                if (barcoderes["Mark"] == null) barcoderes["Mark"] = markBox
                dtMarkBox.add(barcoderes)
                goodVoise()
                refreshActivity()
                return true
            }
            if (itemTemp.foundBarcode(Barcode)) {
                FExcStr.text = "Товар найден, сканируйте маркировку"
                item = RefItem()
                item.foundID(itemTemp.id)
            }
            else {
                FExcStr.text = ("ВНИМАНИЕ! Товар не найден!")
                badVoise()
                return false
            }
        }
        goodVoise()
        refreshActivity()
        return true

       /*
        val textQuery: String
        val dt: Array<Array<String>>

        val qrCod = "010290001205009421tWp&YkCA+fZ;v91EE0692MB6116Q1iCFsD2BeI+6YKpLAhUAFVJajQx6CvM+ZQhA="

        if (ss.CurrentAction == Global.ActionSet.ScanQRCode && codeId == barcodeId) {//заодно проверим DataMatrix ли пришедший код
            //найдем маркировку в справочнике МаркировкаТовара

            val testBatcode = Barcode.replace("'", "''")
            textQuery =
                "SELECT \$Спр.МаркировкаТовара.ФлагОтгрузки as Отгружен " +
                        "FROM \$Спр.МаркировкаТовара (nolock) " +
                        "where \$Спр.МаркировкаТовара.Маркировка like ('%' +SUBSTRING('${testBatcode.trim()}',1,31) + '%') " +
                        "and \$Спр.МаркировкаТовара.Товар = '${ccItem!!.ID}' " +
                        "and \$Спр.МаркировкаТовара.ФлагПоступления = 1 " +
                        "and ((\$Спр.МаркировкаТовара.ФлагОтгрузки = 0 and \$Спр.МаркировкаТовара.ДокОтгрузки = '   0     0   ' )" +
                        "or (\$Спр.МаркировкаТовара.ФлагОтгрузки = 1 and \$Спр.МаркировкаТовара.ДокОтгрузки = '${ss.extendID(docCC!!.ID, "КонтрольНабора")}'))"
            val dtMark = ss.executeWithReadNew(textQuery) ?: return true
            if (barcode == qrCod) {
                FExcStr.text = ("Данный QR-code уже существует!")
                badVoise()
            }
            if (dtMark == null) {
                badVoise()
                return false
            }
        }
        return false*/
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

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
            val colorline = Color.WHITE
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
            boxes.text = rowDT["Box"].toString().trim()
            boxes.typeface = Typeface.SERIF
            boxes.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay * 0.3).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            boxes.gravity = Gravity.CENTER
            boxes.textSize = 20F
            boxes.setTextColor(-0x1000000)
            mark = TextView(this)
            mark.text = rowDT["Mark"].toString().trim().substring(0,31)       //в базу пишем полностью, на терминале покажем только 31 символ
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
        lblPlacer.text = (naklAcc["DocNo"] + " (" + naklAcc["DateDoc"]?.let { hh.shortDate(it) } + ") " + item.invCode + " ВСЕГО МАРКИРОВОК " + linenom.toString())

    }

}
