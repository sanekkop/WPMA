package com.intek.wpma.ChoiseWork.Accept

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.Model.Model
import com.intek.wpma.ParentForm
import com.intek.wpma.R
import com.intek.wpma.SQL.SQL1S.Const
import kotlinx.android.synthetic.main.activity_none_item.*


class NoneItem : BarcodeDataReceiver() {

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
        setContentView(R.layout.activity_none_item)
        ParentForm = intent.extras!!.getString("ParentForm")!!

        title = ss.title
        var oldx : Float = 0F
        FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                    val backHead = Intent(this, Search::class.java)
                    backHead.putExtra("ParentForm", "ShowInfoNewComp")
                    startActivity(backHead)
                    finish()
                }
            }
            return true
        })

        if (ss.FPrinter.path == null) {
            printPal.text = ss.FPrinter.path
        }
        else {
            printPal.text = "'принтер не выбран' НЕТ ПАЛЛЕТЫ"
        }

        noneItem()
    }

    fun noneItem() {

        /*
        * здесь
        * шапка
        * №, Накл., Артикул, Арт. на, Кол-во, Коэф.
        * таблицы
        */

        var textQuery = "SELECT " +
                "right(Journ.docno,5) as DOCNO," +
                "Supply.iddoc as iddoc," +
                "Goods.id as id," +
                "Goods.Descr as ItemName," +
                "Goods.\$Спр.Товары.ИнвКод as InvCode," +
                "Goods.\$Спр.Товары.Артикул as Article," +
                "Goods.\$Спр.Товары.АртикулНаУпаковке as ArticleOnPack," +
                "Goods.\$Спр.Товары.Прих_Цена as Price," +
                "Goods.\$Спр.Товары.КоличествоДеталей as Details, " +
                "CASE WHEN round(Supply.\$АдресПоступление.Количество " +
                "/ISNULL(Package.Coef, 1), 0)*ISNULL(Package.Coef, 1) = Supply.\$АдресПоступление.Количество " +
                "THEN ISNULL(Package.Coef, 1) ELSE 1 END as Coef, " +
                "CASE WHEN round(Supply.\$АдресПоступление.Количество " +
                "/ISNULL(Package.Coef, 1), 0)*ISNULL(Package.Coef, 1) = Supply.\$АдресПоступление.Количество " +
                "THEN round(Supply.\$АдресПоступление.Количество /ISNULL(Package.Coef, 1), 0) " +
                "ELSE Supply.\$АдресПоступление.Количество END as CountPackage, " +
                "Supply.\$АдресПоступление.Количество as Count," +
                "Supply.\$АдресПоступление.ЕдиницаШК as Unit," +
                "Supply.\$АдресПоступление.КоличествоЭтикеток as LabelCount," +
                "Supply.\$АдресПоступление.НомерСтрокиДока as Number," +
                "Supply.\$АдресПоступление.ГруппаСезона as SeasonGroup, " +
                "SypplyHeader.\$АдресПоступление.ДальнийСклад as FlagFarWarehouse," +
                " Supply.LineNO_ as LineNO_, " +
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
                "Units.parentext as ItemID, " +
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
                "ORDER BY Journ.docno, Supply.LineNO_"

        val model = Model()
        textQuery = ss.querySetParam(textQuery, "$iddoc", iddoc)
        textQuery = ss.querySetParam(textQuery, "OKEIPackage", model.okeiPackage)
        textQuery = ss.querySetParam(textQuery, "Warehouse", Const.MainWarehouse)
        var dT = ss.executeWithReadNew(textQuery) ?: return

        if (dT.isNotEmpty()) {

            for (DR in dT) {
                 /*
                 * здесь
                 * заполнение
                 * таблицы
                 * DR - Number, DOCNO, Article, ArticleOnPack, Count, Coef
                 */
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