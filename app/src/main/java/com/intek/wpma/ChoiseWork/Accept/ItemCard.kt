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
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.ParentForm
import com.intek.wpma.R
import com.intek.wpma.Model.Model
import com.intek.wpma.Ref.RefItem
import com.intek.wpma.SQL.SQL1S.Const
import kotlinx.android.synthetic.main.activity_search_acc.*

class ItemCard : BarcodeDataReceiver() {
    var iddoc: String = ""
    var number: String = ""
    var barcode: String = ""
    var codeId: String = ""  //показатель по которому можно различать типы штрих-кодов
    var cardItem = RefItem()
    var itemCardInfo : MutableList<MutableMap<String, String>> = mutableListOf()
    var item = RefItem()
    var bufferWarehouse = ""
    var flagBarcode = ""
    val model = Model()

    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    // ту прописываем что делать при событии сканирования

                    try {
                        barcode = intent.getStringExtra("data").toString()
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
        flagBarcode = intent.extras!!.getString("flagBarcode")!!
        title = ss.title

        when (flagBarcode) {
            "0" -> item.foundID(intent.extras!!.getString("itemID")!!)
            "1" -> item.foundID(intent.extras!!.getString("itemI")!!)
            //"2" -> этот флаг должен передаваться в случае нахождения товара по ШК места
        }

        btnPrinItem.setOnClickListener {

        }


        numVod1.text = "0"
        numVod2.text = "0"
        numVod3.text = "0"
        wtfVod1.text = "0"
        wtfVod2.text = "0"
        wtfVod3.text = "0"

        itemCard()
    }

    //тут тянем инфу о товаре и его местоположение и кол-во на складе, если таковое есть
    //иначе адрес не задан и кол-во равно 0
    private fun getDoc() {
        buffWare()        //вызываем тут, иначе негде будет искать товар
        whatIsIt()
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
                "SELECT :MainWarehouse, :Item, 0 ) as Main GROUP BY Main.Item"
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        textQuery = ss.querySetParam(textQuery, "Item", item.id)
        textQuery = ss.querySetParam(textQuery, "BufferWarehouse", bufferWarehouse)
        textQuery = ss.querySetParam(textQuery, "MainWarehouse", Const.MainWarehouse)
        val datTab = ss.executeWithReadNew(textQuery) ?: return

        if (datTab.isNotEmpty()) {
            for (DR in datTab) {
                //заполнение шапки карточки товара
                zonaHand.text = (DR["AdressMain"].toString().trim() +
                         ": " + DR["BalanceMain"].toString().trim() +
                         " шт")          //зона где товар есть
                zonaTech.text = (DR["AdressBuffer"].toString().trim() +
                         ": " + DR["BalanceBuffer"].toString().trim() +
                         " шт")          //зона куда товар будут запихивать, изначально не задан
                minParty.text = DR["BalanceBuffer"]
                }
        }
    }

    //нам нужен только наш склад, поэтому его и подтянем
    private fun buffWare() {
        val textQuery = "SELECT " +
                "VALUE as val " +
                "FROM _1sconst (nolock) " +
                "WHERE ID = \$Константа.ОснЦентрСклад "
        val datTabl = ss.executeWithReadNew(textQuery) ?: return
        if (datTabl.isNotEmpty()) {
            for (DR in datTabl) {
                bufferWarehouse = DR["val"].toString()
            }
        }
    }

    //подтягиваем все остальное
    private fun nadBl() {
        getDoc()                //собственно исходя из этого и тянем
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

    //Запрос Подсосем остатки в разрезе адресов и состояний
    private fun whatIsIt() {
        /*Если бы мы знали, что это такое,
        * То мы бы знали, что это такое,
        * Но мы не знаем, что это такое
        * */
        var textQuery = "DECLARE @curdate DateTime; " +
                    "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock); " +
                    "SELECT min(Section.descr) as Adress, " +
                    "CASE " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = -10 THEN '-10 Автокорректировка' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = -2 THEN '-2 В излишке' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = -1 THEN '-1 В излишке (пересчет)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 0 THEN '00 Не существует' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 1 THEN '01 Приемка' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 2 THEN '02 Хороший на месте' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 3 THEN '03 Хороший (пересчет)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 4 THEN '04 Хороший (движение)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 7 THEN '07 Бракованный на месте' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 8 THEN '08 Бракованный (пересчет)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 9 THEN '09 Бракованный (движение)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 12 THEN '12 Недостача' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 13 THEN '13 Недостача (пересчет)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 14 THEN '14 Недостача (движение)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 17 THEN '17 Недостача подтвержденная' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 18 THEN '18 Недостача подт.(пересчет)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 19 THEN '19 Недостача подт.(движение)' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 22 THEN '22 Пересорт излишек' " +
                    "WHEN RegAOT.\$Рег.АдресОстаткиТоваров.Состояние = 23 THEN '23 Пересорт недостача' " +
                    "ELSE rtrim(cast(RegAOT.\$Рег.АдресОстаткиТоваров.Состояние as char)) + ' <неизвестное состояние>' END as Condition, " +
                    "cast(sum(RegAOT.\$Рег.АдресОстаткиТоваров.Количество ) as int) as Count " +
                    "FROM " +
                    "RG\$Рег.АдресОстаткиТоваров as RegAOT (nolock) " +
                    "LEFT JOIN \$Спр.Секции as Section (nolock) " +
                    "ON Section.id = RegAOT.\$Рег.АдресОстаткиТоваров.Адрес " +
                    "WHERE " +
                    "RegAOT.period = @curdate " +
                    "and RegAOT.\$Рег.АдресОстаткиТоваров.Товар = :ItemID " +
                    "and RegAOT.\$Рег.АдресОстаткиТоваров.Склад = :Warehouse " +
                    "GROUP BY " +
                    "RegAOT.\$Рег.АдресОстаткиТоваров.Адрес , " +
                    "RegAOT.\$Рег.АдресОстаткиТоваров.Товар , " +
                    "RegAOT.\$Рег.АдресОстаткиТоваров.Состояние " +
                    "HAVING sum(RegAOT.\$Рег.АдресОстаткиТоваров.Количество ) <> 0 " +
                    "ORDER BY Adress, Condition"
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        textQuery = ss.querySetParam(textQuery, "ItemID", item.id)
        textQuery = ss.querySetParam(textQuery, "Warehouse", Const.MainWarehouse)
        val adressConditionItem = ss.executeWithReadNew(textQuery) ?: return
        if (adressConditionItem.isNotEmpty()) {
            for (DR in adressConditionItem) {

            }
        }
    }

 /*   private fun loadUnits(itemID : String) : Boolean {
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
    }*/

    //у нас давно все отрисованно, поэтому просто подтягиваем данные по товару
    @RequiresApi(Build.VERSION_CODES.O)
    private fun itemCard() {       //тут вакханали ебейшая какая-то происходит, как-то должно считать
        nadBl()
        if (itemCardInfo.isNotEmpty()) {
            for (DR in itemCardInfo) {
                shapka.text = (DR["InvCode"].toString() + "Приемка товара")                         //код товара
                itemName.text = DR["ItemName"]                                                      //полное наименование товара
                details.text = DR["Details"]                                                        //кол-во деталей товара, подтягиваем если есть, если нет заполняем, иначе 0
                storageSize.text = DR["StoregeSize"]                                                //кол-во товара дома как я понял
                pricePrih.text = ("Цена: " + DR["Price"].toString())                                //цена товара
                baseSHK.text = cardItem.foundBarcode(DR["BaseUnitID"].toString()).toString()        //штрих-код товара

                //определяем, как был найден товар, перед тем, как зайти в карточку
                when (flagBarcode) {
                    "0" -> {
                        FExcStr.text =  (DR["InvCode"].toString() +
                                "найден в ручную!")
                        FExcStr.setTextColor(Color.RED)
                    }
                    "1" -> {
                        FExcStr.text =  (DR["InvCode"].toString() +
                                "найден по штрихкоду!")
                        FExcStr.setTextColor(Color.RED)
                    }
                    //пока не требуется, но пусть будет
                 /*   "2" -> {
                        FExcStr.text =  (DR["InvCode"].toString() +
                                "найден по ШК МЕСТА!")
                        FExcStr.setTextColor(Color.RED)
                    } */
                }
            }}
                resOne.text = (
                        statVod1.text.toString().toInt() * minParty.text.toString().toInt()
                        ).toString()                                                                //сумма принимаемого товара в общей сложности
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

        if (keyCode == 4) {
            val backH = Intent(this, NoneItem::class.java)
            startActivity(backH)
            finish()
            return true
        }
        if (ss.helper.whatDirection(keyCode) in listOf("Left","Right", "Up", "Down")) {  //StateListDrawable == true) {
            itemCard()

               minParty.text = (
                       (numVod1.text.toString().toInt()) * wtfVod1.text.toString().toInt()
                       ).toString().substring(0,-1)

                minParty.text = (
                        (numVod2.text.toString().toInt()) * wtfVod2.text.toString().toInt()
                        ).toString().substring(0,-1)

                minParty.text = (
                        (numVod1.text.toString().toInt()) * wtfVod1.text.toString().toInt()
                        ).toString().substring(0,-1)

                minParty.text = (
                        (numVod1.text.toString().toInt()) * wtfVod1.text.toString().toInt()
                        ).toString().substring(0,-1)

                minParty.text = (
                        (numVod2.text.toString().toInt()) * wtfVod2.text.toString().toInt()
                        ).toString().substring(0,-1)

                minParty.text = (
                        (numVod3.text.toString().toInt()) * wtfVod3.text.toString().toInt()
                        ).toString().substring(0,-1)

       /*     try {
                resTwo.text = (
                        minParty.text.toString().toInt() / numVod1.text.toString().toInt()
                        ).toString()
                resThird.text = (
                        minParty.text.toString().toInt() / numVod2.text.toString().toInt()
                        ).toString()
                resFor.text = (
                        minParty.text.toString().toInt() / numVod3.text.toString().toInt()
                        ).toString()


                minParty.text = (
                        (numVod2.text.toString().toInt()) * wtfVod2.text.toString().toInt()
                        ).toString().substring(0,-1)
                wtfVod1.text = (
                        minParty.text.toString().toInt() / numVod1.text.toString().toInt()
                        ).toString().substring(0,-1)
                wtfVod2.text = (
                        minParty.text.toString().toInt() / numVod2.text.toString().toInt()
                        ).toString().substring(0,-1)
                return true
            } catch (e : Exception) {
                numVod1.text = "0"
                numVod2.text = "0"
                numVod3.text = "0"
                wtfVod1.text = "0"
                wtfVod2.text = "0"
                wtfVod3.text = "0"

            } */
        }
        return false
    }

    //тотальная ебань
  /* private fun completeAccept() : Boolean {
        FExcStr.text = null
        var textQuery = ""
       // var beginCount = 0

            beginCount = CurrentRowAcceptedItem["Count"]  //Сколько в накладной изначально

            if (AcceptedItem.AcceptCount > CurrentRowAcceptedItem["Count"]) {
                FExcStr.text = ("Нельзя принять по данной накладной более " + CurrentRowAcceptedItem["Count"].ToString() + " штук!")
                return false
            }
            else if (AcceptedItem.AcceptCount == 0) {
                FExcStr.text = "Нельзя принять нулевое количество!"
                return false
            }

            //Теперь проверим не поменялась ли ситуация в самой накладной, пока мы курили бамбук!
            textQuery = "SELECT " +
                        "ACDT.\$АдресПоступление.Количество as Count " +
                        "FROM " +
                        "DT\$АдресПоступление as ACDT (nolock) " +
                        "WHERE " +
                        "ACDT.iddoc = :Doc " +
                        "and ACDT.\$АдресПоступление.Товар = :Item " +
                        "and ACDT.\$АдресПоступление.Состояние0 = 0 " +
                        "and ACDT.lineno_ = :LineNo_ "
            textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
            textQuery = ss.querySetParam(textQuery, "Item", item.id)
            var dTCurr = ss.executeWithReadNew(textQuery)
            if (dTCurr == null) {
                FExcStr.text = "Недопустимое количество! Повторите приемку позиции!"
                return false
            }
            if (dTCurr[0]["Count"] < AcceptedItem.AcceptCount) {
                FExcStr.text = "Недопустимое количество! Повторите приемку позиции!"
                return false
            }
            //Скорректируем начальное количество
            var beginCount = dTCurr[0]["Count"]

        var NeedNew : Int = 0
        var CoefPlace : Int  = 0 //Коэффициент мест, по нему будет расчитывать количество этикеток
        var tmpDT = new DataTable()
        tmpDT.Columns.Add("Coef") //, Type.GetType("System.Int32"))
        tmpDT.Columns.Add("OKEI") //, Type.GetType("System.String"))
        for (dr in FUnits.Rows) {
            if (dr["Coef"] == 1 && dr["OKEIPackage"].ToString() != model.okeiUnit) {
                FExcStr.text = "Коэффициент 1 может быть только у штуки! Пожалуйста исправьте..."
                return false
            }
            if (dr["OKEIPackage"].ToString() == model.okeiPackage) {
                CoefPlace = dr["Coef"]
            }
            if (dr["ID"].ToString() != ss.getVoidID()) {
                //Имеющаяся единица
                if (dr["Coef"] == 0) {
                    textQuery = "UPDATE \$Спр.ЕдиницыШК " +
                                "SET " +
                                "ismark = 1 " +
                                "WHERE \$Спр.ЕдиницыШК .id = :ID ";
                    textQuery = ss.querySetParam(textQuery, "ID", dr["ID"])
                    var datTab = ss.executeWithReadNew(textQuery)
                }
                else {
                    textQuery = "UPDATE \$Спр.ЕдиницыШК " +
                                "SET " +
                                "\$Спр.ЕдиницыШК.Штрихкод = :Barcode, " +
                                "\$Спр.ЕдиницыШК.Коэффициент = :Coef, " +
                                "\$Спр.ЕдиницыШК.ОКЕИ = :OKEIPackage, " +
                                "\$Спр.ЕдиницыШК.ФлагРегистрацииМОД = 1 " +
                                "WHERE \$Спр.ЕдиницыШК .id = :ID "
                    textQuery = ss.querySetParam(textQuery, "Barcode", dr["Barcode"])
                    textQuery = ss.querySetParam(textQuery, "Coef", dr["Coef"])
                    textQuery = ss.querySetParam(textQuery, "ID", dr["ID"])
                    textQuery = ss.querySetParam(textQuery, "OKEIPackage", dr["OKEIPackage"])
                    var datTab = ss.executeWithReadNew(textQuery)
                }
            }
            else {
                var tmpDR = tmpDT.Select("Coef = " + dr["Coef"].ToString() + " and OKEIPackage = '" + dr["OKEIPackage"] + "'")
                if (dr["Barcode"].ToString().Trim() != "" || tmpDR.Length == 0) {
                    NeedNew++
                }
            }
            var tmpdr = tmpDT.NewRow()
            tmpdr["Coef"] = dr["Coef"]
            tmpdr["OKEIPackage"] = dr["OKEIPackage"].ToString()
            tmpDT.Rows.Add(tmpdr)
        }
        tmpDT.Clear()

        if (NeedNew > 0) {
            //Есть новые...
            var CurrentRow : Int = 0
            //Теперь также пишем новые
            for (dr in FUnits.Rows) {
                if (dr["Coef"] == 0) {
                    continue
                }
                if (dr["ID"].ToString() == ss.getVoidID()) {
                    var tmpDR = tmpDT.Select("Coef = " + dr["Coef"].ToString() + " and OKEIPackage = '" + dr["OKEIPackage"] + "'")
                    if (!(dr["Barcode"].ToString().Trim() != "" || tmpDR.Length == 0)) {
                        continue
                    }
                    if (!CreateUnit(item.id, dr["OKEIPackage"].ToString(), dr["Coef"], dr["Barcode"].ToString())) {
                        return false
                    }
                    CurrentRow++
                }
                var tmpdr = tmpDT.NewRow()
                tmpdr["Coef"] = dr["Coef"]
                tmpdr["OKEIPackage"] = dr["OKEIPackage"].ToString()
                tmpDT.Rows.Add(tmpdr)
            }
        }
        //Запишем норму упаковки в товар
        var packNorm : String = (model.okeiKit + model.okeiKit + model.okeiPackage).toString()
        if (packNorm == "") packNorm = "1"
        else packNorm = packNorm.substring(0, -1)  //Отрезаем первый символ "/"

        if (AcceptedItem.MinParty > 0) packNorm = ">" + AcceptedItem.MinParty.ToString() + "/" + packNorm

        textQuery = "UPDATE \$Спр.Товары " +
                    "SET \$Спр.Товары.НормаУпаковки = :PackNorm , " +
                    "\$Спр.Товары.КоличествоДеталей = :Details " +
                    "WHERE \$Спр.Товары .id = :ItemID";
        textQuery = ss.querySetParam(textQuery, "ItemID", item.id)
        textQuery = ss.querySetParam(textQuery, "PackNorm", packNorm)
        var datTab = ss.executeWithReadNew(textQuery)

            //Если ШК с базовой единицы слетел, то поставим его назад этим чудовищным запросом
        textQuery = "declare @idbase char(9) " +
                    "declare @idbar char(9) " +
                    "declare @barcode varchar(50) " +
                    "select @idbase = RefE.id from \$Спр.Товары as RefG (nolock) " +
                    "inner join \$Спр.ЕдиницыШК as RefE (nolock) " +
                    "on RefE.id = RefG.\$Спр.Товары.БазоваяЕдиницаШК " +
                    "where RefG.id = :ItemID and RefE.\$Спр.ЕдиницыШК.Штрихкод = ''; " +
                    "if @idbase is not null begin " +
                    "select @idbar = RefE.id, @barcode = RefE.\$Спр.ЕдиницыШК.Штрихкод from \$Спр.ЕдиницыШК as RefE (nolock) " +
                    "where " +
                    "RefE.parentext = :ItemID " +
                    "and RefE.\$Спр.ЕдиницыШК.ОКЕИ = :OKEIPackage " +
                    "and not RefE.\$Спр.ЕдиницыШК.Штрихкод = ''; " +
                    "if @idbar is not null begin " +
                    "begin tran; " +
                    "update \$Спр.ЕдиницыШК " +
                    "set \$Спр.ЕдиницыШК.Штрихкод = @barcode " +
                    "where id = @idbase; " +
                    "if @@rowcount = 0 " +
                    "rollback tran " +
                    "else begin " +
                    "update \$Спр.ЕдиницыШК " +
                    "set \$Спр.ЕдиницыШК.Штрихкод = '' " +
                    "where id = @idbar; " +
                    "if @@rowcount = 0 " +
                    "rollback tran " +
                    "else " +
                    "commit tran " +
                    "end " +
                    "end " +
                    "end"

        textQuery = ss.querySetParam(textQuery, "OKEIPackage",  model.okeiUnit)
        textQuery = ss.querySetParam(textQuery, "ItemID", item.id)
        var datTab = ss.executeWithReadNew(textQuery) ?: return
            //Конец хуйни с заменой ШК у базовой единицы

        Dictionary<string, object> DataMapWrite = new Dictionary<string, object>()

        //ТЕПЕРЬ ПОЕХАЛА ЗАПИСЬ ДОКУМЕНТА
        //Расчитаем число этикеток
        var labelCount = 0
        if (flagBarcode != "0") {
            labelCount = 0
        }
        else {
            labelCount = 1
        }

        //Для начала подсосем есть ли уже принятые и не напечатанные строки в накладной
        textQuery =
            "SELECT " +
                    "ACDT.LineNo_ as LineNo_, " +
                    "ACDT.\$АдресПоступление.Количество as Count, " +
                    "ACDT.\$АдресПоступление.КоличествоЭтикеток as LabelCount " +
                    "FROM " +
                    "DT\$АдресПоступление as ACDT (nolock) " +
                    "WHERE " +
                    "ACDT.iddoc = :Doc " +
                    "and ACDT.\$АдресПоступление.Товар = :Item " +
                    "and ACDT.\$АдресПоступление.ФлагПечати = 1 " +
                    "and ACDT.\$АдресПоступление.Сотрудник0 = :Employer"
        textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
        textQuery = ss.querySetParam(textQuery, "Item", item.id)
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        var datTab = ss.executeWithReadNew(textQuery) ?: return

        var AllCountAccepted : Int = AcceptedItem.AcceptCount
        if (AlreadyDT.Rows.Count == 0 && AcceptedItem.AcceptCount < BeginCount) {
            //Нуно создать новую строку
            textQuery = "SELECT max(DT\$АдресПоступление .lineno_) + 1 as NewLineNo_ " +
                        "FROM DT\$АдресПоступление " +
                        "WHERE DT\$АдресПоступление .iddoc = :Doc"
            textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
            var datTab = ss.executeWithReadNew(textQuery) ?: return
                val newLineNo_ = datTab[0]["NewLineNo_"]

            textQuery = "INSERT INTO DT\$АдресПоступление VALUES " +
                        "(:Doc, :LineNo_, :Number, :Item, :Count, :EmptyID, :Coef, 1, :Employer, " +
                        ":Adress, :EmptyDate, :TimeNow, 1, :LabelCount, :UnitID, 0, 0, :PalletID); " +
                        "UPDATE DT\$АдресПоступление " +
                        "SET \$АдресПоступление.Количество = :RemainedCount" +
                        "WHERE DT\$АдресПоступление .iddoc = :Doc and " +
                        "DT\$АдресПоступление .lineno_ = :RemainedLineNo_"
            textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
            textQuery = ss.querySetParam(textQuery, "LineNo_", newLineNo_)
            textQuery = ss.querySetParam(textQuery, "Number", number)
            textQuery = ss.querySetParam(textQuery, "Item", item.id)
            textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
            textQuery = ss.querySetParam(textQuery, "Coef",1)
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            textQuery = ss.querySetParam(textQuery, "Adress", ss.getVoidID())
            textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
            textQuery = ss.querySetParam(textQuery, "UnitID", CurrentRowAcceptedItem["Unit"])
            textQuery = ss.querySetParam(textQuery, "RemainedLineNo_", CurrentRowAcceptedItem["LINENO_"])
            textQuery = ss.querySetParam(textQuery, "RemainedCount",BeginCount - AcceptedItem.AcceptCount)
            textQuery = ss.querySetParam(textQuery, "PalletID",ss.FPallet)
            var datTab = ss.executeWithReadNew(textQuery) ?: return
        }
        else if (AlreadyDT.Rows.Count == 0 && AcceptedItem.AcceptCount >= BeginCount) {
            //Товар, будем писать в туже стоку
            textQuery = "UPDATE DT\$АдресПоступление " +
                        "SET " +
                        "\$АдресПоступление.Количество = :Count," +
                        "\$АдресПоступление.Сотрудник0 = :Employer," +
                        "\$АдресПоступление.Дата0 = :EmptyDate," +
                        "\$АдресПоступление.Время0 = :TimeNow," +
                        "\$АдресПоступление.Состояние0 = 1," +
                        "\$АдресПоступление.КоличествоЭтикеток = :LabelCount," +
                        "\$АдресПоступление.ФлагПечати = 1," +
                        "\$АдресПоступление.Паллета = :PalletID " +
                        "WHERE " +
                        "DT\$АдресПоступление .iddoc = :Doc " +
                        "and DT\$АдресПоступление .lineno_ = :LineNo_"
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
            textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
            textQuery = ss.querySetParam(textQuery, "PalletID", ss.FPallet)
            var datTab = ss.executeWithReadNew(textQuery) ?: return
        }
        else if (AlreadyDT.Rows.Count > 0 && AcceptedItem.AcceptCount < BeginCount) {
            //Нуно создать новую строку на новую паллету
            textQuery =
                "SELECT max(DT\$АдресПоступление .lineno_) + 1 as NewLineNo_ " +
                        "FROM DT\$АдресПоступление WHERE " +
                        "DT\$АдресПоступление .iddoc = :Doc"
            textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
            val daTab = ss.executeWithReadNew(textQuery)
            val newLineNo_ = daTab[0]["NewLineNo_"]

            textQuery = "INSERT INTO DT\$АдресПоступление VALUES " +
                        "(:Doc, :LineNo_, :Number, :Item, :Count, :EmptyID, :Coef, 1, :Employer, " +
                        ":Adress, :EmptyDate, :TimeNow, 1, :LabelCount, :UnitID, 0, 0, :PalletID); " +
                        "UPDATE DT\$АдресПоступление " +
                        "SET \$АдресПоступление.Количество = :RemainedCount" +
                        "WHERE DT\$АдресПоступление .iddoc = :Doc and DT\$АдресПоступление .lineno_ = :RemainedLineNo_";
            textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
            textQuery = ss.querySetParam(textQuery, "LineNo_", newLineNo_)
            textQuery = ss.querySetParam(textQuery, "Number", number)
            textQuery = ss.querySetParam(textQuery, "Item", item.id)
            textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
            textQuery = ss.querySetParam(textQuery, "Coef", 1)
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            textQuery = ss.querySetParam(textQuery, "Adress", ss.getVoidID())
            textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
            textQuery = ss.querySetParam(textQuery, "RemainedLineNo_", CurrentRowAcceptedItem["LINENO_"])
            textQuery = ss.querySetParam(textQuery, "RemainedCount", BeginCount - AcceptedItem.AcceptCount)
            textQuery = ss.querySetParam(textQuery, "PalletID", ss.FPallet)
            var alReady = ss.executeWithReadNew(textQuery) ?: return
            //теперь еще обновим непринятые строки
            AllCountAccepted = alReady[0]["Count"].toString() + AcceptedItem.AcceptCount
            textQuery = "UPDATE DT\$АдресПоступление " +
                        "SET " +
                        "\$АдресПоступление.Количество = :RemainedCount " +
                        "WHERE " +
                        "DT\$АдресПоступление .iddoc = :Doc " +
                        "and DT\$АдресПоступление .lineno_ = :RemainedLineNo_"
            textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
            textQuery = ss.querySetParam(textQuery, "RemainedLineNo_", CurrentRowAcceptedItem["LINENO_"])
            textQuery = ss.querySetParam(textQuery, "RemainedCount",   BeginCount - AcceptedItem.AcceptCount)
            var datTab = ss.executeWithReadNew(textQuery) ?: return
        }
        else
        {
            if (AlreadyDT.Rows[0]["LineNo_"] == CurrentRowAcceptedItem["LINENO_"])
            {
                FExcStr.text = "Состояние позиции изменилось! Повторите приемку!"
                return false
            }
            //Уже есть строка принятого, будем писать в изначальную (не принятую)
            textQuery = "UPDATE DT\$АдресПоступление " +
                        "SET " +
                        "\$АдресПоступление.Количество = :Count," +
                        "\$АдресПоступление.Сотрудник0 = :Employer," +
                        "\$АдресПоступление.Дата0 = :EmptyDate," +
                        "\$АдресПоступление.Время0 = :TimeNow," +
                        "\$АдресПоступление.Состояние0 = 1," +
                        "\$АдресПоступление.КоличествоЭтикеток = :LabelCount," +
                        "\$АдресПоступление.ФлагПечати = 1," +
                        "\$АдресПоступление.Паллета = :PalletID " +
                        "WHERE " +
                        "DT\$АдресПоступление .iddoc = :Doc " +
                        "and DT\$АдресПоступление .lineno_ = :LineNo_; "
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
            textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
            textQuery = ss.querySetParam(textQuery, "PalletID", ss.FPallet)
            var datTab = ss.executeWithReadNew(textQuery)
        }

        //Выведем в строку состояния сколько мы приняли за этот раз
        var tmpCoef : Int = 0
        if (!GetCoefPackage(item.id, out tmpCoef)) tmpCoef = CurrentRowAcceptedItem["Coef"]

        FExcStr.text = (AcceptedItem.InvCode.Trim() + " принят в количестве " + GetStrPackageCount(AcceptedItem.AcceptCount, tmpCoef))
        //begin internal command
        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ss.extendID(ss.FEmployer.id, "Спр.Сотрудники")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход2"] = ss.extendID(item.id, "Спр.Товары")
        dataMapWrite["Спр.СинхронизацияДанных.ДокументВход"] = ss.extendID(iddoc, "АдресПоступление")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = "AcceptItem (Принял товар)"
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход2"] = AcceptedItem.AcceptCount

        var dataMapRead: MutableMap<String, Any> = mutableMapOf()
        val fieldList: MutableList<String> = mutableListOf("Спр.СинхронизацияДанных.ДатаРез1")
        try {
            dataMapRead = execCommand("Internal", dataMapWrite, fieldList, dataMapRead)
        }
        catch (e: Exception) {
            return false
        }
        //end internal command
    }*/
}