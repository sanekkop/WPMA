package com.intek.wpma.ChoiseWork.Accept

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View.OnFocusChangeListener
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.ParentForm
import com.intek.wpma.R
import com.intek.wpma.Ref.RefItem
import com.intek.wpma.SQL.SQL1S.Const
import kotlinx.android.synthetic.main.activity_search_acc.*


class ItemCard : BarcodeDataReceiver() {

    var iddoc: String = ""
    var number: String = ""
    var barcode: String = ""
    var codeId: String = ""  //показатель по которому можно различать типы штрих-кодов
    var cardItem = RefItem()
   // var noNe = NoneItem()
    var itemCardInfo : MutableList<MutableMap<String, String>> = mutableListOf()
    var item = RefItem()
    var bufferWarehouse = ""
    //var inPut = InputConnectionWrapper()

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

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_acc)

        ParentForm = intent.extras!!.getString("ParentForm")!!
        item.foundID(intent.extras!!.getString("itemID")!!)
        title = ss.title

        val myTextViewFocus = OnFocusChangeListener { view, hasFocus ->
                if (hasFocus) {
                    (view as TextView).setBackgroundColor(Color.WHITE)
                } else {
                    (view as TextView).setBackgroundColor(Color.GRAY)
                }
            }
        itemCard()
    }

    private fun getDoc() {
        buffWare()
        var textQuery = "DECLARE @curdate DateTime; " +
                "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock); " +
                "SELECT " +
                "CAST(sum(CASE WHEN Main.Warehouse = :MainWarehouse THEN Main.Balance ELSE 0 END) as int) as BalanceMain, " +
                "CAST(sum(CASE WHEN Main.Warehouse = :BufferWarehouse THEN Main.Balance ELSE 0 END) as int) as BalanceBuffer, " +
                "ISNULL((SELECT top 1 Section.descr " +
                "FROM _1sconst as Const (nolock) " +
                "LEFT JOIN \$Спр.Секции as Section (nolock) " +
                "ON Section.id = left(Const.value, 9) " +
                "WHERE Const.id = \$Спр.ТоварныеСекции.Секция " +
                "and Const.date <= :NowDate and Const.OBJID in (" +
                "SELECT id FROM \$Спр.ТоварныеСекции " +
                "WHERE \$Спр.ТоварныеСекции.Склад = :MainWarehouse " +
                "and parentext = :Item)" +
                "ORDER BY " +
                "Const.date DESC, Const.time DESC, Const.docid DESC), '<не задан>') as AdressMain, " +
                "ISNULL((" +
                "SELECT top 1 Section.descr " +
                "FROM _1sconst as Const (nolock) " +
                "LEFT JOIN \$Спр.Секции as Section (nolock) " +
                "ON Section.id = left(Const.value, 9) " +
                "WHERE Const.id = \$Спр.ТоварныеСекции.Секция " +
                "and Const.date <= :NowDate and Const.OBJID in (" +
                "SELECT id FROM \$Спр.ТоварныеСекции " +
                "WHERE " +
                "\$Спр.ТоварныеСекции.Склад = :BufferWarehouse and parentext = :Item)" +
                "ORDER BY " +
                "Const.date DESC, Const.time DESC, Const.docid DESC), '<не задан>') as AdressBuffer " +
                "FROM (SELECT \$Рег.ОстаткиТоваров.Склад as Warehouse, " +
                "\$Рег.ОстаткиТоваров.Товар as Item, " +
                "\$Рег.ОстаткиТоваров.ОстатокТовара as Balance " +
                "FROM RG\$Рег.ОстаткиТоваров (nolock) " +
                "WHERE " +
                "period = @curdate and \$Рег.ОстаткиТоваров.Товар = :Item " +
                "and \$Рег.ОстаткиТоваров.Склад in (:MainWarehouse, :BufferWarehouse) " +
                "UNION ALL " +
                "SELECT :MainWarehouse, :Item, 0 ) as Main GROUP BY Main.Item";
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate());
        textQuery = ss.querySetParam(textQuery, "Item", item.id);
        textQuery = ss.querySetParam(textQuery, "BufferWarehouse", bufferWarehouse);
        textQuery = ss.querySetParam(textQuery, "MainWarehouse", Const.MainWarehouse);
        val datTab = ss.executeWithReadNew(textQuery) ?: return

        if (datTab.isNotEmpty()) {
            for (DR in datTab) {
                //заполнение шапки карточки товара
                zonaHand.text = DR["AdressMain"].toString().trim() +
                         ": " + DR["BalanceMain"].toString().trim() +
                         " шт"          //зона где товар есть
                zonaTech.text = DR["AdressBuffer"].toString().trim() +
                         ": " + DR["BalanceBuffer"].toString().trim() +
                         " шт"          //зона куда товар будут запихивать, изначально не
                minParty.text = DR["BalanceBuffer"]
                }
        }
    }

    private fun buffWare() {
        var textQuery = "SELECT " +
                "VALUE as val " +
                "FROM _1sconst (nolock) " +
                "WHERE ID = \$Константа.ОснЦентрСклад "
        var datTabl = ss.executeWithReadNew(textQuery) ?: return
        if (datTabl.isNotEmpty()) {
            for (DR in datTabl) {
                bufferWarehouse = DR["val"].toString()
            }
        }
    }

    private fun nadBl() {
        getDoc()
        var textQuery = "SELECT " +
                "Goods.Descr as ItemName , " +
                "Goods.\$Спр.Товары.ИнвКод as InvCode , " +
                "Goods.\$Спр.Товары.Артикул as Article , " +
                "Goods.\$Спр.Товары.КоличествоДеталей as Details , " +
                "Goods.\$Спр.Товары.БазоваяЕдиницаШК as BaseUnitID , " +
                "Goods.\$Спр.Товары.МинПартия as MinParty , " +
                "Goods.\$Спр.Товары.Прих_Цена as Price ,  " +
                "isnull(RefSections.\$Спр.ТоварныеСекции.РасчетныйРХ , 0) as StoregeSize " +
                "FROM \$Спр.Товары as Goods (nolock) " +
                "left join \$Спр.ТоварныеСекции as RefSections (nolock) " +
                "on RefSections.parentext = Goods.id and RefSections.\$Спр.ТоварныеСекции.Склад = :warehouse " +
                "WHERE Goods.id = :Item "
        textQuery = ss.querySetParam(textQuery, "Item", item.id)
        textQuery = ss.querySetParam(textQuery, "warehouse", Const.MainWarehouse)
        itemCardInfo = ss.executeWithReadNew(textQuery) ?: return
    }

    private fun loadUnits(itemID : String) : Boolean {
        //Загружает единицы товара в таблицу FUnits
        var textQuery = "SELECT " +
                "Units.id as ID , " +
                "CAST(Units.\$Спр.ЕдиницыШК.Коэффициент as int) as Coef , " +
                "Units.\$Спр.ЕдиницыШК.Штрихкод as Barcode , " +
                "Units.\$Спр.ЕдиницыШК.ОКЕИ as OKEI " +
                "FROM " +
                "\$Спр.ЕдиницыШК as Units (nolock) " +
                "WHERE " +
                "Units.parentext = :CurrentItem and Units.ismark = 0"
        textQuery = ss.querySetParam(textQuery, "CurrentItem", itemID)
        val dT = ss.executeWithReadNew(textQuery) ?: return false
        return true
    }

    @RequiresApi(Build.VERSION_CODES.O)   //у нас давно все отрисованно, поэтому просто подтягиваем данные по товару
    private fun itemCard() {       //тут вакханали ебейшая какая-то происходит, как-то должно считать
        nadBl()
        if (itemCardInfo.isNotEmpty()) {
            for (DR in itemCardInfo) {
                shapka.text = DR["InvCode"].toString() + " Приемка товара"                          //код товара
                itemName.text = DR["ItemName"]                                                      //полное наименование товара
                details.text = DR["Details"]                                                        //кол-во деталей товара, подтягиваем если есть, если нет заполняем, иначе 0
                storageSize.text = DR["StoregeSize"]                                                //кол-во товара дома как я понял
                pricePrih.text = "Цена: " + DR["Price"].toString()                                  //цена товара
                baseSHK.text = cardItem.foundBarcode(DR["BaseUnitID"].toString()).toString()        //штрих-код товара
                resOne.text = (
                        statVod1.text.toString().toInt() * minParty.text.toString().toInt()
                        ).toString()                                                                //сумма принимаемого товара в общей сложности

                // minParty.text = DR["MinParty"]                                                     //я не ебу что это, но пусть будет так //не пашет, подтянем в другом месте
                resTwo.text = "0"
                resThird.text = "0"
                resFor.text = "0"
                numVod1.text = "0"
                numVod2.text = "0"
                numVod3.text = "0"
                wtfVod1.text = "0"
                wtfVod2.text = "0"
                wtfVod3.text = "0"
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

    @RequiresApi(Build.VERSION_CODES.O)
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun reactionKey(keyCode: Int, event: KeyEvent?):Boolean {

        if (keyCode == 4){
            val backH = Intent(this, NoneItem::class.java)
            backH.putExtra("ParentForm", "ItemCard")
            startActivity(backH)
            finish()
        }
        if (ss.helper.whatDirection(keyCode) in listOf("Left","Right", "Up", "Down") ) { // || inPut.finishComposingText()) {
            itemCard()
            resTwo.text = (
                    minParty.text.toString().toInt() / numVod1.text.toString().toInt()
                    ).toString()
            resThird.text = (
                    minParty.text.toString().toInt() / numVod2.text.toString().toInt()
                    ).toString()
            resFor.text = (
                    minParty.text.toString().toInt() / numVod3.text.toString().toInt()
                    ).toString()
            /*
                minParty.text = (
                        numVod2.text.toString().toInt() / wtfVod2.text.toString().toInt()
                        ).toString() */
                wtfVod1.text = (
                        minParty.text.toString().toInt() / numVod1.text.toString().toInt()
                        ).toString()
                wtfVod2.text = (
                        minParty.text.toString().toInt() / numVod2.text.toString().toInt()
                        ).toString()
        }
        return false
    }
}