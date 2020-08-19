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
import android.view.inputmethod.InputMethodManager
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.ParentForm
import com.intek.wpma.R
import com.intek.wpma.SQL.SQL1S.Const
import com.intek.wpma.Model.Model
import com.intek.wpma.Ref.RefPalleteMove
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_yap_item.*
import kotlinx.android.synthetic.main.activity_yap_item.FExcStr

class YapItem : BarcodeDataReceiver() {

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
        setContentView(R.layout.activity_yap_item)

        ParentForm = intent.extras!!.getString("ParentForm")!!
        iddoc = intent.extras!!.getString("Docs")!!
        title = ss.title
        var oldx : Float = 0F
        FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x > oldx) {
                    val backHead = Intent(this, Search::class.java)
                    backHead.putExtra("ParentForm", "ShowInfoNewComp")
                    startActivity(backHead)
                    finish()
                }
            }
            return true
        })

        kolEtik.setOnKeyListener { v: View, keyCode: Int, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                if (ss.isMobile){  //спрячем клаву
                    val inputManager: InputMethodManager =  applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.hideSoftInputFromWindow(this.currentFocus!!.windowToken,
                        InputMethodManager.HIDE_NOT_ALWAYS)
                }
                // сохраняем текст, введенный до нажатия Enter в переменную
                try {
                    val count = kolEtik.text.toString().toInt()
                    //places = count
                    kolEtik.visibility = View.INVISIBLE
                } catch (e: Exception) {
                }
            }
            false
        }

        val palet = RefPalleteMove()

        if (ss.FPrinter.path == null) { // && palet.pallete == "") {
            printPal.text = ss.FPrinter.path //+ palet.pallete
        }
        else {
            printPal.text = "'принтер не выбран' НЕТ ПАЛЛЕТЫ"
        }

        yapItem()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun yapItem() {

        val linearLayout1 = LinearLayout(this)
        val rowTitle1 = TableRow(this)

        //добавим столбцы
        val num = TextView(this)
        num.text = "№"
        num.typeface = Typeface.SERIF
        num.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.05).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        num.gravity = Gravity.CENTER
        num.textSize = 12F
        num.setTextColor(-0x1000000)
        val doc = TextView(this)
        doc.text = "Накл."
        doc.typeface = Typeface.SERIF
        doc.gravity = Gravity.CENTER
        doc.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.15).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        doc.textSize = 12F
        doc.setTextColor(-0x1000000)
        val add = TextView(this)
        add.text = "Инв.Код"
        add.typeface = Typeface.SERIF
        add.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.15).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        add.gravity = Gravity.CENTER
        add.textSize = 12F
        add.setTextColor(-0x1000000)
        val box = TextView(this)
        box.text = "Наименование"
        box.typeface = Typeface.SERIF
        box.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.25).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        box.gravity = Gravity.CENTER
        box.textSize = 12F
        box.setTextColor(-0x1000000)
        val boxf = TextView(this)
        boxf.text = "Кол-во"
        boxf.typeface = Typeface.SERIF
        boxf.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.15).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        boxf.gravity = Gravity.CENTER
        boxf.textSize = 12F
        boxf.setTextColor(-0x1000000)
        val kof = TextView(this)
        kof.text = "Коэф."
        kof.typeface = Typeface.SERIF
        kof.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.1).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        kof.gravity = Gravity.CENTER
        kof.textSize = 12F
        kof.setTextColor(-0x1000000)
        val etik = TextView(this)
        etik.text = "Этикетка"
        etik.typeface = Typeface.SERIF
        etik.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.15).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        etik.gravity = Gravity.CENTER
        etik.textSize = 12F
        etik.setTextColor(-0x1000000)

        linearLayout1.addView(num)
        linearLayout1.addView(doc)
        linearLayout1.addView(add)
        linearLayout1.addView(box)
        linearLayout1.addView(boxf)
        linearLayout1.addView(kof)
        linearLayout1.addView(etik)

        rowTitle1.addView(linearLayout1)
        rowTitle1.setBackgroundColor(Color.GRAY)
        table.addView(rowTitle1)

        var textQuery = "SELECT " +
                "right(Journ.docno,5) as DOCNO , " +
                "Supply.iddoc as iddoc ," +
                "Goods.id as id ," +
                "Goods.Descr as ItemName ," +
                "Goods.\$Спр.Товары.ИнвКод as InvCode ," +
                "Goods.\$Спр.Товары.Артикул as Article ," +
                "Goods.\$Спр.Товары.АртикулНаУпаковке as ArticleOnPack ," +
                "Goods.\$Спр.Товары.Прих_Цена as Price ," +
                "Goods.\$Спр.Товары.КоличествоДеталей as Details ," +
                "CASE WHEN round(Supply.\$АдресПоступление.Количество /ISNULL(Package.Coef, 1), 0)*ISNULL(Package.Coef, 1) = Supply.\$АдресПоступление.Количество " +
                "THEN ISNULL(Package.Coef, 1) ELSE 1 END as Coef," +
                "CASE WHEN round(Supply.\$АдресПоступление.Количество /ISNULL(Package.Coef, 1), 0)*ISNULL(Package.Coef, 1) = Supply.\$АдресПоступление.Количество " +
                "THEN round(Supply.\$АдресПоступление.Количество /ISNULL(Package.Coef, 1), 0) " +
                "ELSE Supply.\$АдресПоступление.Количество END as CountPackage, " +
                "Supply.\$АдресПоступление.Количество as Count," +
                "Supply.\$АдресПоступление.ЕдиницаШК as Unit," +
                "Supply.\$АдресПоступление.КоличествоЭтикеток as LabelCount," +
                "Supply.\$АдресПоступление.НомерСтрокиДока as Number," +
                "Supply.\$АдресПоступление.ГруппаСезона as SeasonGroup," +
                "SypplyHeader.\$АдресПоступление.ДальнийСклад as FlagFarWarehouse, " +
                "Supply.LineNO_ as LineNO_, " +
                //isnull(GS.$Спр.ТоварныеСекции.РазмерХранения , 0) StoregeSize " +
                "isnull(GS.\$Спр.ТоварныеСекции.РасчетныйРХ , 0) StoregeSize " +
                "FROM DT\$АдресПоступление as Supply (nolock) " +
                "LEFT JOIN \$Спр.Товары as Goods (nolock) " +
                "ON Goods.ID = Supply.\$АдресПоступление.Товар " +
                "LEFT JOIN DH\$АдресПоступление as SypplyHeader (nolock) " +
                "ON SypplyHeader.iddoc = Supply.iddoc " +
                "LEFT JOIN _1sjourn as Journ (nolock) " +
                "ON Journ.iddoc = Right(SypplyHeader.\$АдресПоступление.ДокументОснование , 9) " +
                "LEFT JOIN ( " +
                "SELECT " +
                "Units.parentext as ItemID, " +
                "min(Units.\$Спр.ЕдиницыШК.Коэффициент ) as Coef " +
                "FROM " +
                "\$Спр.ЕдиницыШК as Units (nolock) " +
                "WHERE " +
                "Units.\$Спр.ЕдиницыШК.ОКЕИ = :OKEIPackage " +
                "and Units.ismark = 0 " +
                "and not Units.\$Спр.ЕдиницыШК.Коэффициент = 0 " +
                "GROUP BY " +
                "Units.parentext " +
                ") as Package " +
                "ON Package.ItemID = Goods.ID " +
                "LEFT JOIN \$Спр.ТоварныеСекции as GS (nolock) " +
                "on GS.parentext = goods.id and gs.\$Спр.ТоварныеСекции.Склад = :Warehouse " +
                "WHERE Supply.IDDOC in ($iddoc) " +
                "and Supply.\$АдресПоступление.Состояние0 = 1" +
                "and Supply.\$АдресПоступление.ФлагПечати = 1" +
                "and Supply.\$АдресПоступление.Сотрудник0 = :Employer" +
                "ORDER BY Journ.docno, Supply.\$АдресПоступление.Дата0 , Supply.\$АдресПоступление.Время0 "

        val model = Model()
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id) //ss.EmployerID)
        textQuery = ss.querySetParam(textQuery, "OKEIPackage", model.okeiPackage)
        textQuery = ss.querySetParam(textQuery, "Warehouse", Const.MainWarehouse)
        val datT = ss.executeWithReadNew(textQuery) ?: return

        if (datT.isNotEmpty()) {

            for (DR in datT) {

                val linearLayout2 = LinearLayout(this)
                val rowTitle2 = TableRow(this)

                //добавим столбцы
                val num = TextView(this)
                num.text = DR["DOCNO"]
                num.typeface = Typeface.SERIF
                num.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.05).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                num.gravity = Gravity.CENTER
                num.textSize = 12F
                num.setTextColor(-0x1000000)
                val doc = TextView(this)
                doc.text = DR["iddoc"]
                doc.typeface = Typeface.SERIF
                doc.gravity = Gravity.CENTER
                doc.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.15).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                doc.textSize = 12F
                doc.setTextColor(-0x1000000)
                val address = TextView(this)
                address.text = DR["InvCode"]
                address.typeface = Typeface.SERIF
                address.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.15).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                address.gravity = Gravity.CENTER
                address.textSize = 12F
                address.setTextColor(-0x1000000)
                val boxes = TextView(this)
                boxes.text = DR["ItemName"]
                boxes.typeface = Typeface.SERIF
                boxes.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.25).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                boxes.gravity = Gravity.CENTER
                boxes.textSize = 12F
                boxes.setTextColor(-0x1000000)
                val boxesfact = TextView(this)
                boxesfact.text = DR["Count"]
                boxesfact.typeface = Typeface.SERIF
                boxesfact.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.15).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                boxesfact.gravity = Gravity.CENTER
                boxesfact.textSize = 12F
                boxesfact.setTextColor(-0x1000000)
                val koef = TextView(this)
                koef.text = DR["Coef"]
                koef.typeface = Typeface.SERIF
                koef.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.1).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                koef.gravity = Gravity.CENTER
                koef.textSize = 12F
                koef.setTextColor(-0x1000000)
                val etiks = TextView(this)
                etiks.text = DR["LabelCount"]
                etiks.typeface = Typeface.SERIF
                etiks.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.15).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                etiks.gravity = Gravity.CENTER
                etiks.textSize = 12F
                etiks.setTextColor(-0x1000000)

                linearLayout2.addView(num)
                linearLayout2.addView(doc)
                linearLayout2.addView(address)
                linearLayout2.addView(boxes)
                linearLayout2.addView(boxesfact)
                linearLayout2.addView(koef)
                linearLayout2.addView(etiks)

                rowTitle2.addView(linearLayout2)
                rowTitle2.setBackgroundColor(Color.WHITE)
                table.addView(rowTitle2)
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

        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?):Boolean {

        // нажали назад, выйдем и разблокируем доки
        if (keyCode == 4){
            val accMen = Intent(this, AccMenu::class.java)
            startActivity(accMen)
            finish()
        }
        return false
    }
}