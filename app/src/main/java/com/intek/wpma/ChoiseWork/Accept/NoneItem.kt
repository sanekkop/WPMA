package com.intek.wpma.ChoiseWork.Accept

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.Model.Model
import com.intek.wpma.ParentForm
import com.intek.wpma.R
import com.intek.wpma.SQL.SQL1S.Const
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_none_item.*

class NoneItem : BarcodeDataReceiver() {

    var iddoc: String = ""
    var number: String = ""
    var barcode: String = ""
    private var currentLine:Int = 2
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_none_item)
        ParentForm = intent.extras!!.getString("ParentForm")!!
        iddoc = intent.extras!!.getString("Docs")!!
        val prIN = intent.extras!!.getString("FPrinter")!!
        val paLL = intent.extras!!.getString("FPallet")!!

        title = ss.title
        var oldx : Float = 0F
        FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                    val backHead = Intent(this, Search::class.java)
                    backHead.putExtra("ParentForm", "NoneItem")
                    startActivity(backHead)
                    finish()
                }
            }
            return true
        })

       /* FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x > oldx) {
                    val gotoItem = Intent(this, ItemCard::class.java)
                    gotoItem.putExtra("ParentForm", "NoneItem")
                    startActivity(gotoItem)
                    finish()
                }
            }
            return true
        }) */

        if (ss.isMobile){
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@NoneItem, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","NoneItem")
                startActivity(scanAct)
            }
        }

        printPal.text = "'принтер не выбран'"
        palletPal.text = "НЕТ ПАЛЛЕТЫ"

        printPal.text = prIN
        palletPal.text = paLL

        val linearLayout = LinearLayout(this)
        val rowTitle = TableRow(this)

        //добавим столбцы
        val number = TextView(this)
        number.text = "№"
        number.typeface = Typeface.SERIF
        number.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.05).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        number.gravity = Gravity.CENTER
        number.textSize = 18F
        number.setTextColor(-0x1000000)
        val docum = TextView(this)
        docum.text = "Накл."
        docum.typeface = Typeface.SERIF
        docum.gravity = Gravity.CENTER
        docum.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.21).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        docum.textSize = 18F
        docum.setTextColor(-0x1000000)
        val address = TextView(this)
        address.text = "Артикул"
        address.typeface = Typeface.SERIF
        address.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.24).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        address.gravity = Gravity.CENTER
        address.textSize = 18F
        address.setTextColor(-0x1000000)
        val boxes = TextView(this)
        boxes.text = "Арт. на"
        boxes.typeface = Typeface.SERIF
        boxes.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.24).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        boxes.gravity = Gravity.CENTER
        boxes.textSize = 18F
        boxes.setTextColor(-0x1000000)
        val boxesfact = TextView(this)
        boxesfact.text = "Кол-во"
        boxesfact.typeface = Typeface.SERIF
        boxesfact.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.13).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        boxesfact.gravity = Gravity.CENTER
        boxesfact.textSize = 18F
        boxesfact.setTextColor(-0x1000000)
        val kof = TextView(this)
        kof.text = "Коэф."
        kof.typeface = Typeface.SERIF
        kof.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.13).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        kof.gravity = Gravity.CENTER
        kof.textSize = 18F
        kof.setTextColor(-0x1000000)

        linearLayout.addView(number)
        linearLayout.addView(docum)
        linearLayout.addView(address)
        linearLayout.addView(boxes)
        linearLayout.addView(boxesfact)
        linearLayout.addView(kof)

        rowTitle.addView(linearLayout)
        rowTitle.setBackgroundColor(Color.GRAY)
        table.addView(rowTitle)

        noneItem()
     //   filter() //для поиска товара по артиклю
    }

    //непринятый товар, табличка
    @SuppressLint("ClickableViewAccessibility")
    fun noneItem() {

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
                //isnull(GS.\$Спр.ТоварныеСекции.РазмерХранения , 0) as StoregeSize " +
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
        textQuery = ss.querySetParam(textQuery, "Warehouse", Const.MainWarehouse)
        var dT = ss.executeWithReadNew(textQuery) ?: return
        var linenom = 2


        if (dT.isNotEmpty()) {

            for (DR in dT) {

                val linearLayout1 = LinearLayout(this)
                val rowTitle1 = TableRow(this)

                rowTitle1.isClickable = true
                rowTitle1.setOnTouchListener{  v, event ->
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
                    }
                    true
                }

                var colorline =  Color.WHITE
                if (linenom == currentLine)
                {
                    colorline = Color.GRAY
                }
                rowTitle1.setBackgroundColor(colorline)

                //добавим столбцы
                val number = TextView(this)
                number.text = DR["Number"]
                number.typeface = Typeface.SERIF
                number.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.05).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                number.gravity = Gravity.CENTER
                number.textSize = 18F
                number.setTextColor(-0x1000000)
                val docum = TextView(this)
                docum.text = DR["DOCNO"]
                docum.typeface = Typeface.SERIF
                docum.gravity = Gravity.CENTER
                docum.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.21).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                docum.textSize = 18F
                docum.setTextColor(-0x1000000)
                val address = TextView(this)
                address.text = DR["Article"].toString().trim()
                address.typeface = Typeface.SERIF
                address.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.24).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                address.gravity = Gravity.CENTER
                address.textSize = 18F
                address.setTextColor(-0x1000000)
                val boxes = TextView(this)
                boxes.text = DR["ArticleOnPack"].toString().trim()
                boxes.typeface = Typeface.SERIF
                boxes.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.24).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                boxes.gravity = Gravity.CENTER
                boxes.textSize = 18F
                boxes.setTextColor(-0x1000000)
                val boxesfact = TextView(this)
                boxesfact.text = DR["Count"]
                boxesfact.typeface = Typeface.SERIF
                boxesfact.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.13).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                boxesfact.gravity = Gravity.CENTER
                boxesfact.textSize = 18F
                boxesfact.setTextColor(-0x1000000)
                val kof = TextView(this)
                kof.text = ss.helper.byeTheNull(DR["Coef"].toString())
                kof.typeface = Typeface.SERIF
                kof.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.13).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                kof.gravity = Gravity.CENTER
                kof.textSize = 18F
                kof.setTextColor(-0x1000000)

                linearLayout1.addView(number)
                linearLayout1.addView(docum)
                linearLayout1.addView(address)
                linearLayout1.addView(boxes)
                linearLayout1.addView(boxesfact)
                linearLayout1.addView(kof)

                rowTitle1.addView(linearLayout1)
                rowTitle1.setBackgroundColor(Color.WHITE)
                table.addView(rowTitle1)


            }
        }
    }

    fun enterSearchArt() {
        enterSearchArt.visibility = View.VISIBLE
        enterSearchArt.setOnKeyListener { v: View, keyCode: Int, event ->
            enterSearchArt.isFocusable = true
            //enterSearchArt.showSoftInputOnFocus
            //val inputManager: InputMethodManager =  applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            /*inputManager.hideSoftInputFromWindow(this.currentFocus!!.windowToken,InputMethodManager.HIDE_NOT_ALWAYS)*/
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                if (ss.isMobile){  //спрячем клаву
                    val inputManager: InputMethodManager =  applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.hideSoftInputFromWindow(this.currentFocus!!.windowToken,InputMethodManager.HIDE_NOT_ALWAYS)
                }
                // сохраняем текст, введенный до нажатия Enter в переменную
                try {
                    val count = enterSearchArt.text.toString().toInt()
                    enterSearchArt.visibility = View.INVISIBLE
                    searchArt.text = "$count"
                    searchArt.visibility = View.VISIBLE
                    //FExcStr.text = "Ожидание команды"
                } catch (e: Exception) {
                }
            }
            false
        }
    }
    //тут поиск товара по артиклю
  // @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun filter() {
        val textQuery = "SELECT " +
                "Goods.\$Спр.Товары.Артикул as Article like  ('%' + ('${searchArt}') + '%') , " +   //эта шляпа должна сравнивать артикли с заданным, но это не точно
                "isnull(GS.\$Спр.ТоварныеСекции.РасчетныйРХ , 0) as StoregeSize " +
                "FROM DT\$АдресПоступление as Supply (nolock) " +
                "LEFT JOIN \$Спр.Товары as Goods (nolock) " +
                "ON Goods.ID = Supply.\$АдресПоступление.Товар " +
                "LEFT JOIN DH\$АдресПоступление as SypplyHeader (nolock) " +
                "ON SypplyHeader.iddoc = Supply.iddoc " +
                "LEFT JOIN _1sjourn as Journ (nolock) " +
                "ON Journ.iddoc = Right(SypplyHeader.\$АдресПоступление.ДокументОснование , 9) " +
                "LEFT JOIN ( SELECT Units.parentext as ItemID , min(Units.\$Спр.ЕдиницыШК.Коэффициент ) as Coef " +
                "FROM \$Спр.ЕдиницыШК as Units (nolock) " +
                "WHERE Units.\$Спр.ЕдиницыШК.ОКЕИ = :OKEIPackage and Units.ismark = 0 and not Units.\$Спр.ЕдиницыШК.Коэффициент = 0 " +
                "GROUP BY Units.parentext) as Package " +
                "ON Package.ItemID = Goods.ID " +
                "LEFT JOIN \$Спр.ТоварныеСекции as GS (nolock) " +
                "on GS.parentext = goods.id and gs.\$Спр.ТоварныеСекции.Склад = :Warehouse " +
                "WHERE Supply.IDDOC in ($iddoc) and Supply.\$АдресПоступление.Состояние0 = 0 " +
                "ORDER BY Journ.docno, Supply.LineNO_ "

        val dataTable = ss.executeWithRead(textQuery) ?: return

        if (dataTable.isNotEmpty()) {

            table.removeAllViewsInLayout()

            for (DR in dataTable) {

                val filSear = LinearLayout(this)
                val rowFil = TableRow(this)

                val findArt = TextView(this)
                findArt.text = "" //DR["Article"]
                findArt.typeface = Typeface.SERIF
                findArt.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.13).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                findArt.gravity = Gravity.CENTER
                findArt.textSize = 12F
                findArt.setTextColor(-0x1000000)


                filSear.addView(findArt)

                rowFil.addView(filSear)
                rowFil.setBackgroundColor(Color.GRAY)
                table.addView(rowFil)

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
            if(!ss.FPrinter.foundIDD(idd))
            {
                return false
            }
            printPal.text = ss.FPrinter.path
            return true
        }
        if (!ss.FPrinter.selected) {
            FExcStr.text = "Не выбран принтер!"
            return false
        }
        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?):Boolean {

        if (keyCode == 4){;
            val acBack = Intent(this, Search::class.java)
            acBack.putExtra("ParentForm", "NoneItem")
            startActivity(acBack)
            finish()
            return true
        }
        if (ss.helper.whatDirection(keyCode) == "Right") {
            val backHead = Intent(this, Search::class.java)
            backHead.putExtra("ParentForm", "NoneItem")
            startActivity(backHead)
            finish()
            return true
        }

        if (keyCode in 7..16) {
            enterSearchArt()
        }

        /* карточка товара
        if (ss.helper.whatDirection(keyCode) == "Left") {
            val goItCard = Intent(this, ItemCard::class.java)
            goItCard.putExtra(ParentForm, "NoneItem")
            startActivity(goItCard)
            finish()
            return true
        }*/
        return false
    }
}