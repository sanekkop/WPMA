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
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.Model.Model
import com.intek.wpma.ParentForm
import com.intek.wpma.R
import com.intek.wpma.Ref.RefItem
import com.intek.wpma.Ref.RefPalleteMove
import com.intek.wpma.SQL.SQL1S
import com.intek.wpma.SQL.SQL1S.Const
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_none_item.*

class NoneItem : BarcodeDataReceiver() {

    var iddoc: String = ""
    var number: String = ""
    var barcode: String = ""
    var itemID = ""
    private var currentLine:Int = 2
    var codeId: String = ""  //показатель по которому можно различать типы штрих-кодов
    var noneAccItem : MutableList<MutableMap<String, String>> = mutableListOf()
    var artNeed : String = ""     //артикул, по которому можно искать
    var artSearch : String = ""   //а этот мы будем сравнивать
    private val pal = RefPalleteMove()
    private val itm = RefItem()
    var flagBarcode = ""
    var itemIDD = ""            //айдишник товара для поиска

    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    // ту прописываем что делать при событии сканирования

                    try {
                        barcode = intent.getStringExtra("data")                //.toString()
                        reactionBarcode(barcode)
                    }
                    catch(e: Exception) {
                        badVoise()
                        val toast = Toast.makeText(applicationContext, "Не удалось отсканировать штрихкод!", Toast.LENGTH_LONG)
                        toast.show()
                    }

                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_none_item)
        ParentForm = intent.extras!!.getString("ParentForm")!!
        iddoc = intent.extras!!.getString("Docs")!!
       // printPal.text = intent.extras!!.getString("FPrinter")!!
        title = ss.title

        if (ss.FPrinter.selected) {
            printPal.text = ss.FPrinter.path
        }
        if (ss.FPallet != "") {
            pal.foundID(ss.FPallet)
            palletPal.text = pal.pallete
        }

        var oldx = 0F
        FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                    val backHead = Intent(this, Search::class.java)
                    backHead.putExtra("ParentForm", "NoneItem")
                    backHead.putExtra("FPrint", printPal.text.toString())
                    backHead.putExtra("FPallet", palletPal.text.toString())
                    startActivity(backHead)
                    finish()
                }
            }
            return true
        })


        if (ss.isMobile){
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@NoneItem, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","NoneItem")
                startActivity(scanAct)
            }
        }

        noneItem()  //сначала все подтянем, потом нарисуем
    }

    //подтягиваем данные для таблички
    private fun noneItem() {
        var textQuery = "SELECT " +
                "right(Journ.docno,5) as DOCNO , " +
                "Supply.iddoc as iddoc , " +
                "Goods.id as id , " +
                "Goods.Descr as ItemName , " +
                "Goods.\$Спр.Товары.ИнвКод as InvCode , " +
                "Goods.\$Спр.Товары.Артикул as Article , " +
                "Goods.\$Спр.Товары.АртикулНаУпаковке as ArticleOnPack ," +
                "Goods.\$Спр.Товары.Прих_Цена as Price , " +
                "Goods.\$Спр.Товары.КоличествоДеталей as Details , " +
                "CASE WHEN round(Supply.\$АдресПоступление.Количество " +
                "/ISNULL(Package.Coef, 1), 0)*ISNULL(Package.Coef, 1) = Supply.\$АдресПоступление.Количество " +
                "THEN ISNULL(Package.Coef, 1) ELSE 1 END as Coef, " +
                "CASE WHEN round(Supply.\$АдресПоступление.Количество " +
                "/ISNULL(Package.Coef, 1), 0)*ISNULL(Package.Coef, 1) = Supply.\$АдресПоступление.Количество " +
                "THEN round(Supply.\$АдресПоступление.Количество /ISNULL(Package.Coef, 1), 0) " +
                "ELSE Supply.\$АдресПоступление.Количество END as CountPackage , " +
                "Supply.\$АдресПоступление.Количество as Count , " +
                "Supply.\$АдресПоступление.ЕдиницаШК as Unit , " +
                "Supply.\$АдресПоступление.КоличествоЭтикеток as LabelCount , " +
                "Supply.\$АдресПоступление.НомерСтрокиДока as Number , " +
                "Supply.\$АдресПоступление.ГруппаСезона as SeasonGroup , " +
                "SypplyHeader.\$АдресПоступление.ДальнийСклад as FlagFarWarehouse , " +
                " Supply.LineNO_ as LineNO_ , " +
                "isnull(GS.\$Спр.ТоварныеСекции.РасчетныйРХ , 0) as StoregeSize " +
                "FROM " +
                "DT\$АдресПоступление as Supply (nolock) " +
                "LEFT JOIN \$Спр.Товары as Goods (nolock) " +
                "ON Goods.ID = Supply.\$АдресПоступление.Товар " +
                "LEFT JOIN DH\$АдресПоступление as SypplyHeader (nolock) " +
                "ON SypplyHeader.iddoc = Supply.iddoc " +
                "LEFT JOIN _1sjourn as Journ (nolock) " +
                "ON Journ.iddoc = Right(SypplyHeader.\$АдресПоступление.ДокументОснование , 9) " +
                "LEFT JOIN ( " +
                "SELECT " +
                "Units.parentext as ItemID , " +
                "min(Units.\$Спр.ЕдиницыШК.Коэффициент ) as Coef " +
                "FROM \$Спр.ЕдиницыШК as Units (nolock) " +
                "WHERE " +
                "Units.\$Спр.ЕдиницыШК.ОКЕИ = :OKEIPackage " +
                "and Units.ismark = 0 " +
                "and not Units.\$Спр.ЕдиницыШК.Коэффициент = 0 " +
                "GROUP BY " +
                "Units.parentext) as Package " +
                "ON Package.ItemID = Goods.ID " +
                "LEFT JOIN \$Спр.ТоварныеСекции as GS (nolock) " +
                "on GS.parentext = goods.id and gs.\$Спр.ТоварныеСекции.Склад = :Warehouse " +
                "WHERE Supply.IDDOC in ($iddoc) " +
                "and Supply.\$АдресПоступление.Состояние0 = 0 " +
                "ORDER BY Journ.docno, Supply.LineNO_ "
        val model = Model()
        textQuery = ss.querySetParam(textQuery, "$iddoc", iddoc)
        textQuery = ss.querySetParam(textQuery, "OKEIPackage", model.okeiPackage)
        textQuery = ss.querySetParam(textQuery, "Warehouse", ss.Const.mainWarehouse)
        noneAccItem = ss.executeWithReadNew(textQuery) ?: return
        refreshActivity() //теперь рисуем
    }

    //а вот и сама табличка
    @SuppressLint("ClickableViewAccessibility") //уберешь SuppressLint, отъебнет скролл по таблице
    fun refreshActivity() {
        searchArt.setTextColor(Color.BLACK)
        searchArt.textSize = 18F
        artSearch = searchArt.text.toString()
        val lineNom = 2
        table.removeAllViewsInLayout()

        //шапочка
        val linearLayout = LinearLayout(this)
        val rowTitle = TableRow(this)

        val number = TextView(this)
        number.text = "№"
        number.typeface = Typeface.SERIF
        number.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.05).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        number.gravity = Gravity.CENTER
        number.textSize = 16F
        number.setTextColor(-0x1000000)
        number.setBackgroundResource(R.drawable.bg)
        number.setBackgroundColor(Color.GRAY)
        val docum = TextView(this)
        docum.text = "Накл."
        docum.typeface = Typeface.SERIF
        docum.gravity = Gravity.CENTER
        docum.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.21).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        docum.textSize = 16F
        docum.setTextColor(-0x1000000)
        docum.setBackgroundResource(R.drawable.bg)
        docum.setBackgroundColor(Color.GRAY)
        val address = TextView(this)
        address.text = "Артикул"
        address.typeface = Typeface.SERIF
        address.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.24).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        address.gravity = Gravity.CENTER
        address.textSize = 16F
        address.setTextColor(-0x1000000)
        address.setBackgroundResource(R.drawable.bg)
        address.setBackgroundColor(Color.GRAY)
        val boxes = TextView(this)
        boxes.text = "Арт. на"
        boxes.typeface = Typeface.SERIF
        boxes.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.24).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        boxes.gravity = Gravity.CENTER
        boxes.textSize = 16F
        boxes.setTextColor(-0x1000000)
        boxes.setBackgroundResource(R.drawable.bg)
        boxes.setBackgroundColor(Color.GRAY)
        val boxesfact = TextView(this)
        boxesfact.text = "Кол."
        boxesfact.typeface = Typeface.SERIF
        boxesfact.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.13).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        boxesfact.gravity = Gravity.CENTER
        boxesfact.textSize = 16F
        boxesfact.setTextColor(-0x1000000)
        boxesfact.setBackgroundResource(R.drawable.bg)
        boxesfact.setBackgroundColor(Color.GRAY)
        val kof = TextView(this)
        kof.text = "Коэф."
        kof.typeface = Typeface.SERIF
        kof.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.13).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        kof.gravity = Gravity.CENTER
        kof.textSize = 16F
        kof.setTextColor(-0x1000000)
        kof.setBackgroundResource(R.drawable.bg)
        kof.setBackgroundColor(Color.GRAY)

        linearLayout.addView(number)
        linearLayout.addView(docum)
        linearLayout.addView(address)
        linearLayout.addView(boxes)
        linearLayout.addView(boxesfact)
        linearLayout.addView(kof)

        rowTitle.addView(linearLayout)
        rowTitle.setBackgroundColor(Color.rgb(192,192,192))
        table.addView(rowTitle)

        //данные по товару
        if (noneAccItem.isNotEmpty()) {

            for (DR in noneAccItem) {

                artNeed = ss.helper.suckDigits(DR["Article"].toString().trim()) //убираем сранные буковки, которые мешают при поиске
                if (artNeed.indexOf(artSearch) == -1) continue //пока нет вхождений пропускаем, если есть рисуем

                val linearLayout1 = LinearLayout(this)
                val rowTitle1 = TableRow(this)

                rowTitle1.isClickable = true
                rowTitle1.setOnTouchListener{  v, event ->  //выделение строки при таче
                    var i = 0
                    while (i < table.childCount) {
                        if (rowTitle1 != table.getChildAt(i)) {
                            table.getChildAt(i).setBackgroundColor(Color.WHITE)
                        } else {
                            currentLine = i
                            rowTitle1.setBackgroundColor(Color.GRAY)
                        }
                        i++
                        ItemName.text = DR["ItemName"]
                        itemID = DR["id"].toString()
                    }
                    true
                }

                var colorline =  Color.WHITE
                if (lineNom == currentLine) {
                    colorline = Color.GRAY
                }
                rowTitle1.setBackgroundColor(colorline)

                //добавим столбцы
                val numBer = TextView(this)
                numBer.text = DR["Number"]
                numBer.typeface = Typeface.SERIF
                numBer.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.05).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                numBer.gravity = Gravity.CENTER
                numBer.textSize = 16F
                numBer.setTextColor(-0x1000000)
                val dcNum = TextView(this)
                dcNum.text = DR["DOCNO"]
                dcNum.typeface = Typeface.SERIF
                dcNum.gravity = Gravity.CENTER
                dcNum.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.21).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                dcNum.textSize = 16F
                dcNum.setTextColor(-0x1000000)
                val addRess = TextView(this)
                addRess.text = DR["Article"].toString().trim()
                addRess.typeface = Typeface.SERIF
                addRess.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.24).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                addRess.gravity = Gravity.START
                addRess.textSize = 16F
                addRess.setTextColor(-0x1000000)
                val boxES = TextView(this)
                boxES.text = DR["ArticleOnPack"].toString().trim()
                boxES.typeface = Typeface.SERIF
                boxES.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.24).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                boxES.gravity = Gravity.START
                boxES.textSize = 16F
                boxES.setTextColor(-0x1000000)
                val boxesFact = TextView(this)
                boxesFact.text = DR["Count"]
                boxesFact.typeface = Typeface.SERIF
                boxesFact.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.13).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                boxesFact.gravity = Gravity.CENTER
                boxesFact.textSize = 16F
                boxesFact.setTextColor(-0x1000000)
                val koEf = TextView(this)
                koEf.text = ss.helper.byeTheNull(DR["Coef"].toString()) //обрежем нулики и точку
                koEf.typeface = Typeface.SERIF
                koEf.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.13).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                koEf.gravity = Gravity.CENTER
                koEf.textSize = 16F
                koEf.setTextColor(-0x1000000)

                linearLayout1.addView(numBer)
                linearLayout1.addView(dcNum)
                linearLayout1.addView(addRess)
                linearLayout1.addView(boxES)
                linearLayout1.addView(boxesFact)
                linearLayout1.addView(koEf)

                rowTitle1.addView(linearLayout1)
                rowTitle1.setBackgroundColor(Color.WHITE)
                table.addView(rowTitle1)
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
            printPal.text = "'принтер не выбран'"
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
                return true
            }
        if (palletPal.text != pal.pallete){
            palletPal.text = "НЕТ ПАЛЛЕТЫ"
            FExcStr.text = "Не выбрана паллета!"
            badVoise()
        }

        //если таковой имеется, то присваеваем айдишник и ищем в списке непринятого
        if (itm.foundBarcode(Barcode) == true) {
            itemIDD = itm.fID.toString().trim()
            for (DR in noneAccItem) {
                if (itemIDD == DR["id"].toString().trim()) {
                    flagBarcode = "1"
                    break
                }
            }
            goodVoise()
            //если товар есть в списке, переходим в карточку
            val gotoItem = Intent(this, ItemCard::class.java)
            gotoItem.putExtra("ParentForm", "NoneItem")
            gotoItem.putExtra("itemI", itm.fID)
            gotoItem.putExtra("flagBarcode", flagBarcode)
            startActivity(gotoItem)
            finish()
        } else {
            FExcStr.text = "С таким штрихкодом товар не найден!"
            badVoise()
        }
        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?):Boolean {

        if (keyCode == 4){
            val acBack = Intent(this, Search::class.java)
            acBack.putExtra("ParentForm", "NoneItem")
            startActivity(acBack)
            finish()
            return true
        }

        if (ss.helper.whatDirection(keyCode) == "Right") {
            clickVoise()
            val backHead = Intent(this, Search::class.java)
            backHead.putExtra("ParentForm", "NoneItem")
            startActivity(backHead)
            finish()
            return true
        }

        if (ss.helper.whatInt(keyCode) != -1) {             //артикуля, ля, ля, ля
            searchArt.text = (searchArt.text.toString().trim() + ss.helper.whatInt(keyCode).toString())
            refreshActivity()
        }

        if (keyCode == 66 && palletPal.text == "НЕТ ПАЛЛЕТЫ") {    //нет паллеты, забудь про карточку
            FExcStr.text = "Не выбрана паллета!"
            badVoise()
            return false
        } else if (keyCode == 66 && palletPal.text == pal.pallete) {
            flagBarcode = "0"
            val gotoItem = Intent(this, ItemCard::class.java)
            gotoItem.putExtra("ParentForm", "NoneItem")
            gotoItem.putExtra("itemID", itemID)
            gotoItem.putExtra("flagBarcode", flagBarcode)
            startActivity(gotoItem)
            finish()
            return true
        }

        if (keyCode == 67) {                                //чистит артикуля(введенное)
            if (searchArt.text.toString().isNotEmpty()) {
                searchArt.text = searchArt.text
                    .toString()
                    .substring(0, searchArt.text.toString().length - 1)
                refreshActivity()
            } else refreshActivity()
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