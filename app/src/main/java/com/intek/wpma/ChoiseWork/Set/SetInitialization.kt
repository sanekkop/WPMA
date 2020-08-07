package com.intek.wpma.ChoiseWork.Set


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.intek.wpma.*
import com.intek.wpma.Model.Model
import com.intek.wpma.Ref.RefSection
import kotlinx.android.synthetic.main.activity_set.*
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.*
import com.intek.wpma.ChoiseWork.Menu


class SetInitialization : BarcodeDataReceiver(), View.OnTouchListener {

    val primordial = Model()
    var ParentForm: String = "" // форма из которой пришли
    var AllSetsRow: Int = 0
    var DocSetSum: BigDecimal = "0.00".toBigDecimal()
    val MainWarehouse = "     D   "
    var DocSet: Model.StrictDoc? = null                     //текущий док набора
    var DocsSet: MutableList<String> = mutableListOf()      //незавершенные доки на сотруднике
    var DocCC: Model.DocCC? = null
    var Section: Model.Section? = null
    var CCItem: Model.StructItemSet? = null
    var Barcode: String = ""
    // количество набираемой  позиции
    var CountFact: Int = 0
    var CurrLine: Int = 0
    var codeId:String = ""  //показатель по которому можно различать типы штрих-кодов

    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    // ту прописываем что делать при событии сканирования
                    try {
                        Barcode = intent.getStringExtra("data")
                        codeId = intent.getStringExtra("codeId")
                        reactionBarcode(Barcode)
                    } catch (e: Exception) {
                        val toast = Toast.makeText(
                            applicationContext,
                            "Не удалось отсканировать штрихкод!",
                            Toast.LENGTH_LONG
                        )
                        toast.show()
                        BadVoise()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(com.intek.wpma.R.layout.activity_set)
        ParentForm = intent.extras!!.getString("ParentForm")!!
        SS.ANDROID_ID = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID)
        title = SS.title
        scanRes = null //занулим повторно для перехода между формами
        if (ParentForm == "Menu") {
            if (!ToModeSetInicialization())
            {
                BadVoise()
                val menu = Intent(this, Menu::class.java)
                startActivity(menu)
                finish()
                return
            }
        }
        else if (ParentForm == "Correct" || ParentForm == "WatchTablePart") {
            try {
                PreviousAction.text = intent.extras!!.getString("PreviousAction")!!
                //получим незаконченные задания по отбору
                GetDocsSet()
                //сообразим с какими параметрами нужно вызвать ToModeSet
                val docSetID = intent.extras!!.getString("DocSetID")!!
                val addressID = intent.extras!!.getString("AddressID")!!
                CountFact = intent.extras!!.getString("CountFact")!!.toInt()
                if ((docSetID != "") && (addressID == "")) {
                    ToModeSet(null, docSetID)
                } else if ((docSetID != "") && (addressID != "")) {
                    ToModeSet(addressID, docSetID)
                } else ToModeSet(null, null)
            } catch (e: Exception) {
                val toast = Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_LONG)
                toast.show()
            }
        }
        else if (ParentForm == "SetComplete") {
            try {
                GetDocsSet()
                if (!ToModeSetInicialization())
                {
                    //облом выходим обратно
                    BadVoise()
                    val menu = Intent(this, Menu::class.java)
                    startActivity(menu)
                    finish()
                }
            } catch (e: Exception) {
                val toast = Toast.makeText(applicationContext, e.toString(), Toast.LENGTH_LONG)
                toast.show()
            }
        }
        if (SS.isMobile){
            btnScanSetMode.visibility = View.VISIBLE
            btnScanSetMode!!.setOnClickListener {
                val scanAct = Intent(this@SetInitialization, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","SetInitialization")
                startActivity(scanAct)
            }
            if (SS.CurrentMode == Global.Mode.SetInicialization && SS.CurrentAction == Global.ActionSet.Waiting){
                mainView.text = "Для получения задания нажмите на ЭТО поле!"
            }
        }
        correct.setOnClickListener {
            if (SS.CurrentAction != Global.ActionSet.EnterCount && !DocSet!!.Special){
                // перейдем на форму корректировки
                val correct = Intent(this, Correct::class.java)
                correct.putExtra("iddoc", DocSet!!.ID)
                correct.putExtra("AddressID", CCItem!!.AdressID)
                correct.putExtra("CountFact",CountFact.toString())
                startActivity(correct)
                finish()
            }
        }
        mainView.setOnTouchListener(this)           //для запроса задания с телефона,чтобы кликали по этому полю
        var oldx : Float = 0F                      //для свайпа, чтобы посмотреть накладную
        FExcStr.setOnTouchListener{ v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                    FExcStr.text = "Подгружаю список..."
                    //перейдем на форму просмотра
                    val watchForm = Intent(this, WatchTablePart::class.java)
                    watchForm.putExtra("iddoc", DocSet!!.ID)
                    watchForm.putExtra("ItemCode", CCItem!!.InvCode)
                    watchForm.putExtra("addressID", CCItem!!.AdressID)
                    watchForm.putExtra("DocView", DocSet!!.View)
                    watchForm.putExtra("CountFact", CountFact.toString())
                    startActivity(watchForm)
                    finish()
                }
            }
            true
        }
        PreviousAction.setOnTouchListener(this)     //для завершения набора маркировок при неполно набранной строке
    }

    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }

    fun ToModeSetInicialization(): Boolean {
        SS.FEmployer.Refresh();    //Обновим данные сотрудника
        SS.Const.Refresh();        //Обновим константы

        //PreviousAction = "";

        //получим незаконченные задания по отбору
        GetDocsSet()
        if (DocsSet.isNotEmpty()) {
            return ToModeSet(null, null)
        }
        SS.CurrentMode = Global.Mode.SetInicialization
        SS.CurrentAction = Global.ActionSet.Waiting
        FExcStr.text = "Ожидание команды"
        return true

    } // ToModeSetInicialization

    fun GetDocsSet() {

        var textQuery =
        "SELECT " +
                "journ.iddoc as IDDOC " +
                "FROM " +
                "_1sjourn as journ (nolock) " +
                "INNER JOIN DH\$КонтрольНабора as DocCC (nolock) " +
                "ON DocCC.iddoc = journ.iddoc " +
                "WHERE " +
                "DocCC.\$КонтрольНабора.Наборщик = :Employer " +
                "and journ.iddocdef = \$КонтрольНабора " +
                "and DocCC.\$КонтрольНабора.Дата2 = :EmptyDate " +
                "and not DocCC.\$КонтрольНабора.Дата1 = :EmptyDate " +
                "and journ.ismark = 0 "

        textQuery = SS.QuerySetParam(textQuery, "Employer", SS.FEmployer.ID)
        textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate())
        val dataTable = SS.ExecuteWithReadNew(textQuery)

        if (dataTable!!.isNotEmpty()) {
            //если есть незаконченные задания по отбору

            for (i in 1 until dataTable.size) {
                DocsSet.add(dataTable[i]["IDDOC"].toString())
            }
        }
    }

    fun ToModeSet(AdressID: String?, iddoc: String?): Boolean {
        for (id in DocsSet) {
            if (!LockDoc(id)) {
                return false
            }
        }
        var textQuery =
            "DECLARE @curdate DateTime; " +
                    "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock); " +
                    "select top 1 " +
                    "DocCC.\$КонтрольНабора.Товар as ID, " +
                    "DocCC.lineno_ as LINENO_, " +
                    "Goods.descr as ItemName, " +
                    "Goods.\$Спр.Товары.ИнвКод as InvCode, " +
                    "Goods.\$Спр.Товары.КоличествоДеталей as Details, " +
                    "DocCC.\$КонтрольНабора.Количество as Count, " +
                    "DocCC.\$КонтрольНабора.Адрес0 as Adress, " +
                    "DocCC.\$КонтрольНабора.Цена as Price, " +
                    "Sections.descr as AdressName, " +
                    "ISNULL(AOT.Balance, 0) as Balance, " +  //Реквизиты документа
                    "DocCC.iddoc as IDDOC, " +
                    "journForBill.docno as DocNo, " +
                    "CONVERT(char(8), CAST(LEFT(journForBill.date_time_iddoc, 8) as datetime), 4) as DateDoc, " +
                    "journForBill.iddoc as Bill, " +
                    "DocCCHead.\$КонтрольНабора.КолСтрок as Rows, " +
                    "Sector.descr as Sector, " +
                    "DocCCHead.\$КонтрольНабора.Сумма as Sum, " +
                    "DocCCHead.\$КонтрольНабора.НомерЛиста as Number, " +
                    "DocCCHead.\$КонтрольНабора.ФлагСамовывоза as SelfRemovel, " +
                    "Clients.descr as Client, " +
                    "Bill.\$Счет.ТипНакладной as TypeNakl, " +
                    "isnull(DocCCHead.\$КонтрольНабора.Коробка , :EmptyID) as BoxID, " +
                    "AdressBox.descr as Box, " +
                    "DocCCHead.\$КонтрольНабора.Сектор as Sector, " +
                    "DocCCHead.\$КонтрольНабора.КолСтрок as Rows, " +
                    "DocCCHead.\$КонтрольНабора.Наборщик as TypeSetter, " +
                    "DocCCHead.\$КонтрольНабора.ФлагСамовывоза as FlagDelivery " +
                    "from " +
                    "DT\$КонтрольНабора as DocCC (nolock) " +
                    "LEFT JOIN DH\$КонтрольНабора as DocCCHead (nolock) " +
                    "ON DocCCHead.iddoc = DocCC.iddoc " +
                    "LEFT JOIN \$Спр.Товары as Goods (nolock) " +
                    "ON Goods.id = DocCC.\$КонтрольНабора.Товар " +
                    "LEFT JOIN \$Спр.Секции as Sections (nolock) " +
                    "ON Sections.id = DocCC.\$КонтрольНабора.Адрес0 " +
                    "LEFT JOIN ( " +
                    "select " +
                    "RegAOT.\$Рег.АдресОстаткиТоваров.Товар as item, " +
                    "RegAOT.\$Рег.АдресОстаткиТоваров.Адрес as adress, " +
                    "sum(RegAOT.\$Рег.АдресОстаткиТоваров.Количество ) as balance " +
                    "from " +
                    "RG\$Рег.АдресОстаткиТоваров as RegAOT (nolock) " +
                    "where " +
                    "period = @curdate " +
                    "and \$Рег.АдресОстаткиТоваров.Склад = :Warehouse " +
                    "and \$Рег.АдресОстаткиТоваров.Состояние = 2 " +
                    "group by RegAOT.\$Рег.АдресОстаткиТоваров.Товар , RegAOT.\$Рег.АдресОстаткиТоваров.Адрес " +
                    ") as AOT " +
                    "ON AOT.item = DocCC.\$КонтрольНабора.Товар and AOT.adress = DocCC.\$КонтрольНабора.Адрес0 " +
                    "LEFT JOIN DH\$КонтрольРасходной as DocCB (nolock) " +
                    "ON DocCB.iddoc = DocCCHead.\$КонтрольНабора.ДокументОснование " +
                    "LEFT JOIN DH\$Счет as Bill (nolock) " +
                    "ON Bill.iddoc = DocCB.\$КонтрольРасходной.ДокументОснование " +
                    "LEFT JOIN _1sjourn as journForBill (nolock) " +
                    "ON journForBill.iddoc = Bill.iddoc " +
                    "LEFT JOIN \$Спр.Секции as Sector (nolock) " +
                    "ON Sector.id = DocCCHead.\$КонтрольНабора.Сектор " +
                    "LEFT JOIN \$Спр.Клиенты as Clients (nolock) " +
                    "ON Bill.\$Счет.Клиент = Clients.id " +
                    "LEFT JOIN \$Спр.Секции as AdressBox (nolock) " +
                    "ON AdressBox.id = DocCCHead.\$КонтрольНабора.Коробка " +
                    "where " +
                    "DocCC.iddoc in (:Docs) " +
                    "and DocCC.\$КонтрольНабора.Дата5 = :EmptyDate " +
                    "and DocCC.\$КонтрольНабора.Корректировка = 0 " +
                    "and DocCC.\$КонтрольНабора.Количество > 0 " +
                    (if (AdressID != null) "and DocCC.\$КонтрольНабора.Адрес0 = :Adress " else "") +
                    (if (iddoc != null) "and DocCC.iddoc = :iddoc " else "") +
                    "order by " +
                    "DocCCHead.\$КонтрольНабора.Сектор , Sections.\$Спр.Секции.Маршрут , LINENO_"

        textQuery = SS.QuerySetParam(textQuery, "EmptyID", SS.GetVoidID())
        textQuery = textQuery.replace(":Docs", SS.helper.ListToStringWithQuotes(DocsSet))
        textQuery = SS.QuerySetParam(textQuery, "Warehouse", MainWarehouse)
        textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate())
        if (iddoc != null) {
            textQuery = SS.QuerySetParam(textQuery, "iddoc", iddoc)
        }
        if (AdressID != null) {
            textQuery = SS.QuerySetParam(textQuery, "Adress", AdressID)
        }

        val dataTable = SS.ExecuteWithRead(textQuery)
        //неотобранных строк больше нет
        if (dataTable!!.isEmpty()) {
            if (AdressID == null) {
                // завершение отбора
                return ToModeSetComplete()
            } else {
                FExcStr.text = "Нет такого адреса в сборочном!"
                return false
            }
            //FExcStr.text = "Нет доступных команд! Ошибка робота!"
        }

        RefreshRowSum()    //Подтянем циферки

        //представление документа
        val docView = if (dataTable[1][18].toInt() == 1) "(C) " else {
            ""
        } + dataTable[1][15].trim() + "-" +
                dataTable[1][17] + " Заявка " + dataTable[1][11] + " (" + dataTable[1][12] + ")"

        CCItem = Model.StructItemSet(
            dataTable[1][0],                            //ID
            dataTable[1][3],                            //InvCode
            dataTable[1][2].trim(),                     //Name
            dataTable[1][7].toBigDecimal(),             //Price
            dataTable[1][5].toBigDecimal().toInt(),     //Count
            dataTable[1][5].toBigDecimal().toInt(),     //CountFact
            dataTable[1][6],                            //AdressID
            dataTable[1][8].trim(),                     //AdressName
            dataTable[1][1].toInt(),                    //CurrLine
            dataTable[1][9].toBigDecimal().toInt(),     //Balance
            dataTable[1][4].toBigDecimal().toInt(),     //Details
            dataTable[1][5].toBigDecimal().toInt(),     //OKEI2Count
            "шт",                                //OKEI2
            1                                 //OKEI2Coef
        )
        CurrLine = dataTable[1][5].toBigDecimal().toInt()

        CCItem = MultiplesOKEI2(CCItem!!)

        //заявка
        DocSet = Model.StrictDoc(
            dataTable[1][10],                           //ID
            dataTable[1][18].toInt(),                   //SelfRemovel
            docView,                                    //View
            dataTable[1][14].toInt(),                   //Rows
            dataTable[1][13],                           //FromWarehouseID
            dataTable[1][19].trim(),                    //Client
            dataTable[1][16].toBigDecimal(),            //Sum
            dataTable[1][20].toInt() == 2,      //Special
            dataTable[1][22],                           //Box
            dataTable[1][21]                            //BoxID
        )
        //КонтрольНабора
        DocCC = Model.DocCC(
            dataTable[1][10],                           //ID
            dataTable[1][23],                           //Sector
            dataTable[1][24],                           //Rows
            dataTable[1][25],                           //TypeSetter
            dataTable[1][26].toInt()                    //FlagDelivery
        )

        //проверим есть ли маркировка
        if (IsMarkProduct(CCItem!!.ID)) {
            //проверим колво занятых маркировок с набранным колвом текущей позиции
            textQuery =
                "declare @count INT " +
                "SET @count = " +
                            "(select count(*) " +
                            "from \$Спр.МаркировкаТовара (nolock) " +
                            "where \$Спр.МаркировкаТовара.ФлагПоступления = 1 and \$Спр.МаркировкаТовара.ДокОтгрузки = '${SS.ExtendID(DocCC!!.ID, "КонтрольНабора")}' " +
                            "and \$Спр.МаркировкаТовара.ФлагОтгрузки = 1 and \$Спр.МаркировкаТовара.Товар = '${CCItem!!.ID}') " +
                "select SUM( DocCC.\$КонтрольНабора.Количество ) as CountCC, @count as CountMark " +
                "from DT\$КонтрольНабора as DocCC (nolock) " +
                "where iddoc = '${DocCC!!.ID}' and DocCC.\$КонтрольНабора.Корректировка = 0 and DocCC.\$КонтрольНабора.Дата5 != :EmptyDate and DocCC.\$КонтрольНабора.Товар = '${CCItem!!.ID}'"
            textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate())
            val dt = SS.ExecuteWithReadNew(textQuery)
            if(dt != null && dt.isNotEmpty()) {


                if (dt[0]["CountCC"] == "null") {        //потерянные маркировки есть, а в доке нет ни одной отобранной позиции
                    if (dt[0]["CountMark"].toString().toInt() > 0) {
                        CountFact = dt[0]["CountMark"].toString().toInt()
                    }
                } else {  //колво набранных маркировок не соответсвтует колву принятых в доке
                    if (dt[0]["CountMark"].toString().toInt() > dt[0]["CountCC"].toString()
                            .toInt()
                    ) {
                        CountFact =
                            dt[0]["CountMark"].toString().toInt() - dt[0]["CountCC"].toString()
                                .toInt()
                    }
                }
            }
        }

        if (CountFact ==0) {
            SS.CurrentAction = Global.ActionSet.ScanAdress
            FExcStr.text = WhatUNeed()
        }
        else if (CountFact > 0){
            //вернулись из корректировки/просмотра, лиюо заново зашли в режим с уже набранными маркировками
            SS.CurrentAction = Global.ActionSet.ScanQRCode
            if (SS.isMobile){
                PreviousAction.text = "Для завершения набора позиции с маркировкой нажмите ЗДЕСЬ!"
            }
            else PreviousAction.text = "Для завершения набора позиции с маркировкой нажмите 'ENTER'!"

            //набирали товар с маркировкой и не завершили набор
            FExcStr.text = "Отобрано " + CCItem!!.InvCode.trim() + " - " + CountFact.toString() + " шт. (строка " + CCItem!!.CurrLine + ")" + WhatUNeed()
            if (CCItem!!.Count == CountFact){
                //скорректировали последнюю, а предыдущие с маркировками висят
                FExcStr.text = "Позиция набрана, нажмите ENTER!"
            }
        }
        else {
            SS.CurrentAction = Global.ActionSet.ScanAdress
            FExcStr.text = WhatUNeed()
        }

        // заполним форму
        val price: TextView = findViewById(R.id.price)
        price.text = "Цена: " + dataTable[1][7]
        price.visibility = VISIBLE
        val balance: TextView = findViewById(R.id.balance)
        balance.text = "Ост-ок: " + CCItem!!.Balance
        balance.visibility = VISIBLE
        val address: TextView = findViewById(R.id.address)
        address.text = CCItem!!.AdressName
        address.visibility = VISIBLE
        val invCode: TextView = findViewById(R.id.InvCode)
        invCode.text = CCItem!!.InvCode
        invCode.visibility = VISIBLE
        val header: TextView = findViewById(R.id.header)
        header.text = "Строка " + CCItem!!.CurrLine + " из " + DocSet!!.Rows + " (ост " + AllSetsRow + ")"
        header.visibility = VISIBLE
        val item: TextView = findViewById(R.id.item)
        item.text = CCItem!!.Name
        item.visibility = VISIBLE

        var oldx : Float = 0F
        item.setOnTouchListener { v, event ->
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                    FExcStr.text = "Подгружаю список..."
                    //перейдем на форму просмотра
                    val watchForm = Intent(this, WatchTablePart::class.java)
                    watchForm.putExtra("iddoc", DocSet!!.ID)
                    watchForm.putExtra("ItemCode", CCItem!!.InvCode)
                    watchForm.putExtra("addressID", CCItem!!.AdressID)
                    watchForm.putExtra("DocView", DocSet!!.View)
                    watchForm.putExtra("CountFact", CountFact.toString())
                    startActivity(watchForm)
                    finish()
                }
            }
            true
        }

        val details: TextView = findViewById(R.id.details)
        details.text = "Деталей: " + CCItem!!.Details
        details.visibility = VISIBLE

        val count: TextView = findViewById(R.id.count)
        count.text = (CCItem!!.Count - CountFact).toString() + " шт по 1"
        count.visibility = VISIBLE

        correct.visibility = VISIBLE
        correct.isFocusable = false
        mainView.text = DocSet!!.View

        SS.CurrentMode = Global.Mode.Set
        return true
    }

    private fun RefreshRowSum(): Boolean {
        var textQuery =
            "select " +
                    "sum(DocCC.\$КонтрольНабора.Сумма ) as Sum, " +
                    "count(*) Amount " +
                    "from " +
                    "DT\$КонтрольНабора as DocCC (nolock) " +
                    "where " +
                    "DocCC.iddoc in (:Docs ) " +
                    "and DocCC.\$КонтрольНабора.Дата5 = :EmptyDate " +
                    "and DocCC.\$КонтрольНабора.Корректировка = 0 " +
                    "and DocCC.\$КонтрольНабора.Количество > 0 "
        textQuery = textQuery.replace(":Docs", SS.helper.ListToStringWithQuotes(DocsSet))
        textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate())
        val dataTable = SS.ExecuteWithReadNew(textQuery) ?: return false
        if (dataTable.isNotEmpty()) {
            DocSetSum = dataTable[0]["Sum"].toString().toBigDecimal()
            AllSetsRow = dataTable[0]["Amount"].toString().toInt()
        } else {
            DocSetSum = "0.00".toBigDecimal()
            AllSetsRow = 0
        }
        return true
    }

    private fun MultiplesOKEI2(CCItem: Model.StructItemSet): Model.StructItemSet {

        var item: Model.StructItemSet = CCItem
        var textQuery = "SELECT " +
                "isnull(OKEI2.descr, OKEI.descr) as Name, " +
                "CAST(Units.\$Спр.ЕдиницыШК.Коэффициент as int) as Coef, " +
                "ceiling(:amount/\$Спр.ЕдиницыШК.Коэффициент ) as AmountOKEI2 " +
                "FROM " +
                "\$Спр.ЕдиницыШК as Units (nolock) " +
                "inner join \$Спр.ОКЕИ as OKEI (nolock) " +
                "on OKEI.id = Units.\$Спр.ЕдиницыШК.ОКЕИ " +
                "left join \$Спр.ОКЕИ as OKEI2 (nolock) " +
                "on OKEI2.id = Units.\$Спр.ЕдиницыШК.ОКЕИ2 " +
                "WHERE " +
                "Units.parentext = :CurrentItem " +
                "and OKEI.id <> :OKEIKit" +
                "and Units.ismark = 0 " +
                "and ceiling(:amount/\$Спр.ЕдиницыШК.Коэффициент )*\$Спр.ЕдиницыШК.Коэффициент = :amount " +
                "order by AmountOKEI2"
        textQuery = SS.QuerySetParam(textQuery, "CurrentItem", CCItem.ID)
        textQuery = SS.QuerySetParam(textQuery, "amount", CCItem.Count)
        textQuery = SS.QuerySetParam(textQuery, "OKEIKit", primordial.OKEIKit)

        val dataTable = SS.ExecuteWithRead(textQuery)
        if (dataTable!!.isNotEmpty()) {
            item = Model.StructItemSet(
                CCItem.ID,
                CCItem.InvCode,
                CCItem.Name,
                CCItem.Price,
                CCItem.Count,
                CCItem.CountFact,
                CCItem.AdressID,
                CCItem.AdressName,
                CCItem.CurrLine,
                CCItem.Balance,
                CCItem.Details,
                dataTable[1][2].toBigDecimal().toInt(),
                dataTable[1][0].trim(),
                dataTable[1][1].toInt()
            )
        }
        return item
    }

    fun WhatUNeed(currAction: Global.ActionSet?): String {
        var result: String = ""
        when (currAction) {
            Global.ActionSet.ScanAdress -> result = "Отсканируйте адрес!"

            Global.ActionSet.ScanItem -> result = "Отсканируйте товар!"

            Global.ActionSet.EnterCount -> result = "Введите количество! (подтверждать - 'enter')"

            Global.ActionSet.ScanPart -> result =
                "Отсканируйте спец. ШК деталей! " + CCItem!!.InvCode.trim() +
                        (if (CCItem!!.Details == 99) " (особая)" else " (деталей: " + CCItem!!.Details.toString() + ")")

            Global.ActionSet.ScanBox -> result = "Отсканируйте коробку!"

            Global.ActionSet.ScanPallete -> result = "Отсканируйте паллету!"

            Global.ActionSet.ScanQRCode -> result = "Отсканируйте QR - код"
        }
        return result
    }

    private fun WhatUNeed(): String {
        return WhatUNeed(SS.CurrentAction)
    }  // WhatUNeed


    fun QuitModesSet(): Boolean {
        for (id in DocsSet) {
            if (!LockoutDoc(id)) {
                return false
            }
        }
        return true
    } // QuitModesSet

    private fun reactionBarcode(Barcode: String) {
        var isObject: Boolean = true
        var barcoderes: MutableMap<String, String> = SS.helper.DisassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        //если это не типовой справочник, то выходим
        val idd = barcoderes["IDD"].toString()
        if (SS.IsSC(idd, "Сотрудники")) {
            var intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
            return
        }
//        if (SS.IsSC(IDD, "Принтеры")) {
//            //получим путь принтера
//            val TextQuery =
//                "select descr, SP2461 " +
//                        "from SC2459 " +
//                        "where SP2465 = '$IDD'"
//            val DataTable = SS.ExecuteWithRead(TextQuery) ?: return
//
//            PrinterPath = DataTable!![1][1]
//            FExcStr.text = "Отсканирован принтер " + PrinterPath.trim() + "\n" + WhatUNeed()
//            return
//        }


        if (Barcode.substring(0, 2) == "25" && barcoderes["Type"] == "113") {
            if (barcoderes["IDD"]!! == "") {
                FExcStr.text = "Не удалось преобразовать штрихкод!"
                BadVoise()
                return
            }

            if (!SS.IsSC(barcoderes["IDD"]!!, "Сотрудники")) {
                if (!SS.IsSC(barcoderes["IDD"]!!, "Секции")) {
                    // вместо !SS.IsSC(dicBarcode["IDD"]!!, "Принтеры")
                    if (!SS.IsSC(barcoderes["IDD"]!!, "Принтеры")) {
                        isObject = false
                    }
                }
            }
            if (isObject) {
                if (ReactionSC(barcoderes["IDD"]!!)) {

                    if (SS.CurrentMode == Global.Mode.Set) {
                        GoodDone()
                        return
                    }
                }
                return
            }
        }
        if (barcoderes["Type"] == "part" && (SS.CurrentMode == Global.Mode.Set)) {
            ScanPartBarcode(barcoderes["count"]!!.toInt())

            return
        }
        if (ReactionBarcode(Barcode)) {

            if (SS.CurrentMode == Global.Mode.Set) {
                GoodDone()
                return
            } else {
                FExcStr.text = "Ожидание команды"
            }

        }
        else {
            if (barcoderes["Type"] == "6" && (SS.CurrentMode == Global.Mode.Set)) {
                if (ReactionSC(barcoderes["ID"]!!, true)) {
                    if (SS.CurrentMode == Global.Mode.Set) {
                        GoodDone()
                        return
                    } else {
                        //View()
                    }
                }
            }
            //if (SS.ExcStr == null) {
           //     FExcStr.text = "Ожидание команды"
           // } else {
                if (SS.CurrentMode == Global.Mode.Set) {
                    BadVoise()
                    return
                }
                FExcStr.text = SS.ExcStr
          //  }
        }

    }

    fun ReactionBarcode(Barcode: String): Boolean {
        when (SS.CurrentMode) {
            Global.Mode.Set -> return RBSet(Barcode)
            else -> {
                FExcStr.text = "Нет действий с этим штирхкодом в данном режиме!"
                BadVoise()
                return false
            }
        }
    }

    fun ReactionSC(IDD: String): Boolean {
        return ReactionSC(IDD, false)
    }

    fun ReactionSC(IDD: String, thisID: Boolean): Boolean {
        //FExcStr = null;
        return when (SS.CurrentMode) {

            Global.Mode.SetInicialization -> RSCSetInicialization(IDD)

            Global.Mode.Set -> RSCSet(IDD, thisID)

            //Global.Mode.SetComplete -> RSCSetComplete(IDD)


            else -> {
                FExcStr.text = "Нет действий с данным справочником в данном режиме!"
                BadVoise()
                false
            }
        }
    }

    private fun RBSet(Barcode: String): Boolean {
        if (SS.CurrentAction != Global.ActionSet.ScanItem && SS.CurrentAction != Global.ActionSet.ScanQRCode) {
            FExcStr.text = "Неверно! " + WhatUNeed()
            BadVoise()
            return false
        }
        var textQuery: String
        var dt: Array<Array<String>>


        if (SS.CurrentAction == Global.ActionSet.ScanQRCode && codeId == BarcodeId){//заодно проверим DataMatrix ли пришедший код
            //найдем маркировку в справочнике МаркировкаТовара
            var testBatcode = Barcode.replace("'","''")
            textQuery =
                "SELECT \$Спр.МаркировкаТовара.ФлагОтгрузки as Отгружен" +
                        "FROM \$Спр.МаркировкаТовара (nolock) " +
                        "where \$Спр.МаркировкаТовара.Маркировка like ('%' +SUBSTRING('${testBatcode.trim()}',1,31) + '%') " +
                        "and \$Спр.МаркировкаТовара.Товар = '${CCItem!!.ID}' " +
                        "and \$Спр.МаркировкаТовара.ФлагПоступления = 1 " +
                        "and ((\$Спр.МаркировкаТовара.ФлагОтгрузки = 0 and \$Спр.МаркировкаТовара.ДокОтгрузки = '   0     0   ' )" +
                        "or (\$Спр.МаркировкаТовара.ФлагОтгрузки = 1 and \$Спр.МаркировкаТовара.ДокОтгрузки = '${SS.ExtendID(DocCC!!.ID,"КонтрольНабора")}'))"
            val dt_mark = SS.ExecuteWithReadNew(textQuery) ?: return false
            if (dt_mark.isEmpty()){
                FExcStr.text = "Маркировка не найдена, либо товар уже отгружен!" + WhatUNeed()
                return false
            }
            else if (dt_mark[0]["Отгружен"].toString() == "0"){
                //взведем фдаг отгрузки + проставим док отгрузки
                textQuery =
                    "UPDATE \$Спр.МаркировкаТовара " +
                            "SET \$Спр.МаркировкаТовара.ДокОтгрузки = '${SS.ExtendID(DocCC!!.ID,"КонтрольНабора")}', \$Спр.МаркировкаТовара.ФлагОтгрузки = 1 " +
                    "WHERE " +
                            "\$Спр.МаркировкаТовара.Маркировка like ('%' +SUBSTRING('${testBatcode.trim()}',1,31) + '%') " +
                            "and \$Спр.МаркировкаТовара.Товар = '${CCItem!!.ID}' "
                if (!SS.ExecuteWithoutRead(textQuery)) {
                    FExcStr.text = "QR - code не распознан! Заново " + WhatUNeed()
                    BadVoise()
                    return false
                }

                if (CCItem!!.Count > 1){
                    CountFact += 1
                    //набрали все позиции с маркировкой
                    if (CountFact == CCItem!!.Count){
                        if (EnterCountSet(CountFact)){
                            //Если все норм, то выходим
                            return true
                        }
                        else{
                            //не прошло что-то отменим маркировку
                            textQuery =
                                "UPDATE \$Спр.МаркировкаТовара " +
                                        "SET \$Спр.МаркировкаТовара.ДокОтгрузки = '   0     0   ' , \$Спр.МаркировкаТовара.ФлагОтгрузки = 0 " +
                                        "WHERE " +
                                        "\$Спр.МаркировкаТовара.Маркировка like ('%' +SUBSTRING('${testBatcode.trim()}',1,31) + '%') " +
                                        "and \$Спр.МаркировкаТовара.Товар = '${CCItem!!.ID}' "
                            if (!SS.ExecuteWithoutRead(textQuery)) {
                                FExcStr.text = "QR - code не распознан! Заново " + WhatUNeed()
                                return false
                            }
                        }

                    }

                    if (SS.isMobile){
                        PreviousAction.text = "Для завершения набора позиции с маркировкой нажмите ЗДЕСЬ!"
                    }
                    else PreviousAction.text = "Для завершения набора позиции с маркировкой нажмите 'ENTER'!"
                }
                else {
                    CountFact = 1
                    if (!EnterCountSet(CountFact)){
                        //не прошло что-то отменим маркировку
                        textQuery =
                            "UPDATE \$Спр.МаркировкаТовара " +
                                    "SET \$Спр.МаркировкаТовара.ДокОтгрузки = '   0     0   ' , \$Спр.МаркировкаТовара.ФлагОтгрузки = 0 " +
                                    "WHERE " +
                                    "\$Спр.МаркировкаТовара.Маркировка like ('%' +SUBSTRING('${testBatcode.trim()}',1,31) + '%') " +
                                    "and \$Спр.МаркировкаТовара.Товар = '${CCItem!!.ID}' "
                        if (!SS.ExecuteWithoutRead(textQuery)) {
                            FExcStr.text = "QR - code не распознан! Заново " + WhatUNeed()
                            return false
                        }
                    }
                }
                FExcStr.text = "Отобрано " + CCItem!!.InvCode.trim() + " - " + CountFact.toString() + " шт. (строка " + CCItem!!.CurrLine + ") " + WhatUNeed() + " - СЛЕДУЮЩИЙ!"
                count.text = (CCItem!!.Count - CountFact).toString() + " шт по 1"
            }
            else {
                //маркировка уже есть, надо сверить количество которео уже собрали и которое отсканировали по Qr кодам
                //проверим колво занятых маркировок с набранным колвом текущей позиции
                textQuery =
                    "select SUM(Set_tabl.CountCC) as CountCC, " +
                            "SUM(Set_tabl.CountMark) as CountMark " +
                            "from ( select SUM(DocCC.\$КонтрольНабора.Количество ) as CountCC, " +
                            "0 as CountMark " +
                            "from DT\$КонтрольНабора as DocCC (nolock) " +
                            "where " +
                            "iddoc = '${DocCC!!.ID}' " +
                            "and DocCC.\$КонтрольНабора.Корректировка = 0 " +
                            "and DocCC.\$КонтрольНабора.Дата5 != :EmptyDate "
                "and DocCC.\$КонтрольНабора.Товар = '${CCItem!!.ID}'" +
                        "union all " +
                        "select 0 , count(*) " +
                        "from \$Спр.МаркировкаТовара (nolock) " +
                        "where \$Спр.МаркировкаТовара.ФлагПоступления = 1 " +
                        "and \$Спр.МаркировкаТовара.ДокОтгрузки = '${SS.ExtendID(
                            DocCC!!.ID,
                            "КонтрольНабора"
                        )}' " +
                        "and \$Спр.МаркировкаТовара.Товар = '${CCItem!!.ID}'"
                ") as Set_tabl "
                textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate())
                val dt_mark = SS.ExecuteWithReadNew(textQuery) ?: return false
                if (dt_mark.isEmpty()) {
                    //не может быть
                    return false
                }
                //надо чтобы маркировок было не больше чем набранных строк
                CountFact += dt_mark[0]["CountMark"].toString()
                    .toInt() - dt_mark[0]["CountCC"].toString().toInt()
            }
            return true
        }
        //нужно сканировать маркировку, а сканиуруют что-то другое
        if (SS.CurrentAction == Global.ActionSet.ScanQRCode && codeId != BarcodeId){
            FExcStr.text = "Неверно! " + WhatUNeed()
            BadVoise()
            return false
        }
        if (SS.CurrentAction == Global.ActionSet.ScanItem) {
            textQuery =
                "SELECT " +
                        "Units.parentext as ItemID, " +
                        "Goods.\$Спр.Товары.ИнвКод as InvCode, " +
                        "Units.\$Спр.ЕдиницыШК.ОКЕИ as OKEI " +
                        "FROM \$Спр.ЕдиницыШК as Units (nolock) " +
                        "LEFT JOIN \$Спр.Товары as Goods (nolock) " +
                        "ON Goods.id = Units.parentext " +
                        "WHERE Units.\$Спр.ЕдиницыШК.Штрихкод = :Barcode ";
            textQuery = SS.QuerySetParam(textQuery, "Barcode", Barcode)
            dt = SS.ExecuteWithRead(textQuery) ?: return false

            if (dt.isEmpty()) {
                FExcStr.text = "С таким штрихкодом товар не найден! " + WhatUNeed()
                return false
            }
            if (dt[1][0] != CCItem!!.ID) {
                FExcStr.text =
                    "Не тот товар! (отсканирован " + dt[1][1].trim() + ") " + WhatUNeed()
                return false
            }
            //проверим есть ли маркировка
           if (IsMarkProduct(CCItem!!.ID)) {
               //есть маркировка, пусть сканируют QR-code
               SS.CurrentAction = Global.ActionSet.ScanQRCode
               FExcStr.text = WhatUNeed()
               return false
           }
        }

        SS.CurrentAction = Global.ActionSet.EnterCount
        val enterCount: EditText = findViewById(R.id.enterCount)
        enterCount.visibility = VISIBLE
        enterCount?.setText("")
        enterCount.setOnKeyListener { v: View, keyCode: Int, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                // сохраняем текст, введенный до нажатия Enter в переменную
                try {
                    val count = enterCount.text.toString().toInt()
                    CountFact = count
                    EnterCountSet(CountFact)
                } catch (e: Exception) {

                }
            }
            false
        }

        FExcStr.text = WhatUNeed()
        return true
    }

    fun RSCSetInicialization(IDD: String): Boolean {

        val refSection = RefSection()
        if (!refSection.FoundIDD(IDD)) return false
        Section = Model.Section(
            refSection.ID,
            IDD,
            refSection.Type.toString(),
            refSection.Name.trim()
        )
        if (Section!!.Type != "12") {
            FExcStr.text = "Неверный тип адреса! Отсканируйте коробку!"
            return false
        } else {
            PreviousAction.text = Section!!.Descr
        }
        return true
    }

    fun RSCSet(IDD: String, thisID: Boolean): Boolean {

        if (!thisID) {

            var textQuery: String
            if (SS.IsSC(IDD, "Секции")) {
                if (SS.CurrentAction == Global.ActionSet.ScanAdress) {
                    val refSection = RefSection()
                    if (!refSection.FoundIDD(IDD)) return false
                    Section = Model.Section(
                        refSection.ID,
                        IDD,
                        refSection.Type.toString(),
                        refSection.Name.trim()
                    )

                    if (Section!!.Type == "12") {
                        FExcStr.text = "Неверно! " + WhatUNeed()
                        return false
                    }
                    if (Section!!.ID != CCItem!!.AdressID) {
                        //Переход на другую строку
                        return ToModeSet(Section!!.ID, null)
                    }
                    //&& Const.ImageOn
                    if (CCItem!!.Details > 0) {
                        SS.CurrentAction = Global.ActionSet.ScanPart
                    } else {
                        //проверим есть ли маркировка
                        textQuery =
                            "SELECT \$Спр.МаркировкаТовара.Товар " +
                            "FROM \$Спр.МаркировкаТовара (nolock) " +
                            "WHERE " +
                                    "\$Спр.МаркировкаТовара.Товар = '${CCItem!!.ID}' " +
                                    "and \$Спр.МаркировкаТовара.ФлагПоступления = 1 and \$Спр.МаркировкаТовара.ФлагОтгрузки = 0 and \$Спр.МаркировкаТовара.ДокОтгрузки = '   0     0   ' "
                        val dt = SS.ExecuteWithRead(textQuery) ?: return false
                        if (dt.isEmpty()){
                            SS.CurrentAction = Global.ActionSet.ScanItem
                        }
                        else SS.CurrentAction = Global.ActionSet.ScanQRCode
                    }
                    FExcStr.text = WhatUNeed()
                    return true
                } else if (SS.CurrentAction == Global.ActionSet.ScanBox) {
                    //СКАНИРОВАНИЕ КОРОБКИ
                    if (Section!!.Type.toInt() != 12) {
                        FExcStr.text = "Неверно! " + WhatUNeed()
                        return false
                    }
                    if (Section!!.ID != DocSet!!.BoxID) {
                        FExcStr.text = "Неверная коробка! " + WhatUNeed()
                        return false
                    }

                } else {
                    //Какой-то другой режим вероятно?
                    FExcStr.text = "Неверно! " + WhatUNeed()
                    return false
                }
            }
            else if (SS.IsSC(IDD, "Принтеры")) {
                //получим путь принтера
                SS.FPrinter.FoundIDD(IDD)
                if (!SS.FPrinter.Selected) return false
                FExcStr.text = "Отсканирован принтер " + SS.FPrinter.Path.trim() + "\n" + WhatUNeed()
                return true
            } else {
                FExcStr.text = "Неверно! " + WhatUNeed()
                return false
            }
            return true
        }
        else {
            FExcStr.text = "Нет действий с данным штрихкодом!"
            return false
        }
    }

    fun EnterCountSet(Count: Int): Boolean {

        //надо через попытку иначе если вылетет тут то будет косяк с маркировкой
        if (SS.isMobile){  //спрячем клаву
            //только если поле ввода количества активно
            if (enterCount.isFocused) {
                val inputManager: InputMethodManager =
                    applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputManager.hideSoftInputFromWindow(
                    this.currentFocus!!.windowToken,
                    InputMethodManager.HIDE_NOT_ALWAYS
                )
            }
        }

        if (SS.CurrentAction != Global.ActionSet.EnterCount && SS.CurrentAction != Global.ActionSet.ScanQRCode) {
            FExcStr.text = "Неверно! " + WhatUNeed()
            return false
        }
        if (Count <= 0 || (CCItem!!.Count < Count)) {
            FExcStr.text =
                "Количество указано неверно! (максимум " + CCItem!!.Count.toString() + ")"
            return false
        }

        return CompleteLineSet()
    }

    fun ScanPartBarcode(CountPart: Int): Boolean {
        if (SS.CurrentAction != Global.ActionSet.ScanPart) {
            FExcStr.text = "Неверно! " + WhatUNeed()
            return false
        } else if (CountPart != CCItem!!.Details) {
            FExcStr.text = "Количество деталей неверно! " + WhatUNeed()
            return false
        }
        SS.CurrentAction = Global.ActionSet.ScanItem
        FExcStr.text = WhatUNeed()
        return true
    } // ScanPartBarcode

    fun CompleteLineSet(): Boolean {
        //Заглушка, рефрешим позицию, чтобы не было проблем, если оборвется связь
//        if (!ToModeSet(CCItem!!.AdressID, DocSet!!.ID))
//        {
//            //CCItem!!.CountFact = CountFact;
//            CurrentAction = Global.ActionSet.ScanAdress   //Отключение по константе
//            return false
//        }
//        CurrentAction = Global.ActionSet.ScanAdress   //Отключение по константе
        //конец заглушки
        if (CCItem!!.Count > CountFact) {
//            if (Const.StopCorrect)
//            {
//                FExcStr = "Возможность дробить строку отключена!";
//                return false;
//            }
            //добавить строчку надо
            var textQuery =
                "begin tran; " +
                        "update DT\$КонтрольНабора " +
                        "set \$КонтрольНабора.Количество = :count, " +
                        "\$КонтрольНабора.Сумма =  :count*\$КонтрольНабора.Цена " +
                        "where DT\$КонтрольНабора .iddoc = :iddoc and DT\$КонтрольНабора .lineno_ = :currline; " +
                        "if @@rowcount > 0 begin " +
                        "insert into DT\$КонтрольНабора (\$КонтрольНабора.СтрокаИсх , \$КонтрольНабора.Товар , \$КонтрольНабора.Количество ," +
                        "\$КонтрольНабора.Единица , \$КонтрольНабора.Цена , \$КонтрольНабора.Коэффициент , \$КонтрольНабора.Сумма ," +
                        "\$КонтрольНабора.Секция , \$КонтрольНабора.Корректировка , \$КонтрольНабора.ПричинаКорректировки , \$КонтрольНабора.ЕдиницаШК ," +
                        "\$КонтрольНабора.Состояние0 , \$КонтрольНабора.Адрес0 , \$КонтрольНабора.СостояниеКорр , \$КонтрольНабора.АдресКорр ," +
                        "\$КонтрольНабора.ДокБлокировки , \$КонтрольНабора.Дата5 , \$КонтрольНабора.Время5 , \$КонтрольНабора.Контейнер , " +
                        "lineno_, iddoc, \$КонтрольНабора.Контроль ) " +
                        "select \$КонтрольНабора.СтрокаИсх , \$КонтрольНабора.Товар , :remaincount ," +
                        "\$КонтрольНабора.Единица , \$КонтрольНабора.Цена , \$КонтрольНабора.Коэффициент , :count*\$КонтрольНабора.Цена ," +
                        "\$КонтрольНабора.Секция , \$КонтрольНабора.Корректировка , \$КонтрольНабора.ПричинаКорректировки , \$КонтрольНабора.ЕдиницаШК ," +
                        "\$КонтрольНабора.Состояние0 , \$КонтрольНабора.Адрес0 , \$КонтрольНабора.СостояниеКорр , \$КонтрольНабора.АдресКорр ," +
                        "\$КонтрольНабора.ДокБлокировки , \$КонтрольНабора.Дата5 , \$КонтрольНабора.Время5 , \$КонтрольНабора.Контейнер , " +
                        "(select max(lineno_) + 1 from DT\$КонтрольНабора where iddoc = :iddoc), iddoc, 0 " +
                        "from DT\$КонтрольНабора as ForInst where ForInst.iddoc = :iddoc and ForInst.lineno_ = :currline; " +
                        "select max(lineno_) as newline from DT\$КонтрольНабора where iddoc = :iddoc; " +
                        "if @@rowcount = 0 rollback tran else commit tran " +
                        "end " +
                        "else rollback";
            textQuery = SS.QuerySetParam(textQuery, "count", CountFact)
            textQuery = SS.QuerySetParam(textQuery, "remaincount", CCItem!!.Count - CountFact)
            textQuery = SS.QuerySetParam(textQuery, "iddoc", DocSet!!.ID)
            textQuery = SS.QuerySetParam(textQuery, "currline", CCItem!!.CurrLine)

            val dt = SS.ExecuteWithRead(textQuery) ?: return false
            //Писать будем в добавленную, так лучше! Поэтому обновляем корректную строчку
            CurrLine = dt[1][0].toInt()
        }
        //фиксируем строку
        var textQuery =
            "UPDATE DT\$КонтрольНабора WITH (rowlock) " +
                    "SET \$КонтрольНабора.Дата5 = :Date5, " +
                    "\$КонтрольНабора.Время5 = :Time5, " +
                    "\$КонтрольНабора.Контейнер = :id " +
                    "WHERE DT\$КонтрольНабора .iddoc = :DocCC and DT\$КонтрольНабора .lineno_ = :lineno_; "

        val sdf = SimpleDateFormat("yyyyMMdd HH:mm:ss")
        val currentDate = sdf.format(Date()).substring(0, 8) + " 00:00:00.000"
        val currentTime = timeStrToSeconds(sdf.format(Date()).substring(9, 17))

        textQuery = SS.QuerySetParam(textQuery, "id", SS.GetVoidID())
        textQuery = SS.QuerySetParam(textQuery, "DocCC", DocSet!!.ID)
        textQuery = SS.QuerySetParam(textQuery, "Date5", currentDate)
        textQuery = SS.QuerySetParam(textQuery, "Time5", currentTime)
        textQuery = SS.QuerySetParam(textQuery, "lineno_", CCItem!!.CurrLine)

        if (!SS.ExecuteWithoutRead(textQuery)) {
            return false
        }
        //Запись прошла успешно
        SS.CurrentAction =
            Global.ActionSet.ScanAdress   //на всякий случай, если там что-нибудь накроется, то во вьюхе по крайней мере нельзя будет повторно ввести количество

        PreviousAction.text =
            "Отобрано " + CCItem!!.InvCode.trim() + " - " + CountFact.toString() + " шт. (строка " + CCItem!!.CurrLine + ")"
        //занулим,дабы не было ошибки с колвом
        CountFact = 0

        enterCount.visibility = INVISIBLE
        return ToModeSet(null, null)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        ReactionKey(keyCode, event)
        return super.onKeyDown(keyCode, event)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {

        when(event!!.action){
            // нажатие
            MotionEvent.ACTION_DOWN -> {
                //нажали на mainView для запроса задания
                if (SS.CurrentMode == Global.Mode.SetInicialization && SS.CurrentAction == Global.ActionSet.Waiting){
                    CompleteSetInicialization()
                    return true
                }
                //
                if (SS.CurrentMode == Global.Mode.Set && SS.CurrentAction == Global.ActionSet.ScanQRCode) {
                    if(CountFact != 0){
                        //пытаются завершить набор позиции, не набрав всю строку с маркировкой
                        EnterCountSet(CountFact)
                    }
                    return true
                }
            }
        }

        return true
    }

    fun ReactionKey(keyCode: Int, event: KeyEvent?) {

        when (SS.CurrentMode) {

            Global.Mode.Set -> {
                if (!enterCount.isVisible) {
                    RKSet(keyCode, event)
                }
            }

            else -> RKSetInicialization(keyCode, event)
        }
        // нажали назад, выйдем и разблокируем доки
        if (keyCode == 4){
            QuitModesSet()
            val menu = Intent(this, Menu::class.java)
            startActivity(menu)
            finish()
        }
    }

    fun RKSet(keyCode: Int, event: KeyEvent?) {

        if (keyCode == 22) //нажали вправо; просмотр табл. части
        {
//            if (!SS.Employer.CanRoute)
//            {
//                string appendix = " (нельзя посмотреть маршрут)";
//                if (lblAction.Text.IndexOf(appendix) < 0)
//                {
//                    lblAction.Text = lblAction.Text + appendix;
//                }
//                return;
//            }

            FExcStr.text = "Подгружаю список..."
            //перейдем на форму просмотра
            val watchForm = Intent(this, WatchTablePart::class.java)
            watchForm.putExtra("iddoc", DocSet!!.ID)
            watchForm.putExtra("ItemCode", CCItem!!.InvCode)
            watchForm.putExtra("addressID", CCItem!!.AdressID)
            watchForm.putExtra("DocView", DocSet!!.View)
            watchForm.putExtra("CountFact",CountFact.toString())
            startActivity(watchForm)
            finish()

        }

        if (keyCode == 16 && SS.CurrentAction != Global.ActionSet.EnterCount && !DocSet!!.Special) {
//            if (SS.Const.StopCorrect)
//            {
//                //StopCorrect - ВРЕМЕНННАЯ ЗАГЛУШКА
//                lblAction.Text = "Возможность корректировать отключена!";
//                return;
//            }
            // перейдем на форму корректировки
            val correct = Intent(this, Correct::class.java)
            correct.putExtra("iddoc", DocSet!!.ID)
            correct.putExtra("AddressID", CCItem!!.AdressID)
            correct.putExtra("CountFact",CountFact.toString())
            startActivity(correct)
            finish()
        }
        //нажали ENTER
        if (keyCode == 66){
            if (SS.CurrentAction == Global.ActionSet.ScanQRCode){
                if(CountFact != 0){
                    //пытаются завершить набор позиции, не набрав всю строку с маркировкой
                    EnterCountSet(CountFact)
                }
            }
        }

        /*
        if (Screan == 0 && (Key == Keys.Enter || Key == Keys.F14 || Key == Keys.F2 || Key == Keys.F1 || Key.GetHashCode() == 189) && SS.CurrentAction == ActionSet.EnterCount)
        {
            int tmpCount;
            try
            {
                string tmpTxt = pnlCurrent.GetTextBoxByName("tbCount").Text;
                if (tmpTxt.Substring(tmpTxt.Length - 1, 1) == "-")
                {
                    tmpTxt = tmpTxt.Substring(0, tmpTxt.Length - 1);
                }
                tmpCount = Convert.ToInt32(tmpTxt);
            }
            catch
            {
                tmpCount = 0;
            }
            if (SS.EnterCountSet(tmpCount))
            {
                View();
                GoodDone();
                pnlCurrent.GetTextBoxByName("tbCount").Text = ""
            }
            else
            {
                View();
                lblAction.Text = SS.ExcStr;
                BadDone();
            }
        }
        */

    }

    fun RKSetInicialization(keyCode: Int, event: KeyEvent?) {
        // нажали 1
        if (keyCode.toString() == "8") {
            CompleteSetInicialization()
        }
    }

    fun CompleteSetInicialization(): Boolean {
        // решили отказаться
//        if ( BoxSetView.text != "99-00-00-01")
//        {
//            val toast = Toast.makeText(applicationContext, "Коробка не указана!", Toast.LENGTH_SHORT)
//            toast.show()
//            return false
//        }
        // если коробка не указана. будем ставить ее по умолчанию

        var dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = SS.ExtendID(SS.FEmployer.ID, "Спр.Сотрудники")
        //ставим ID коробки по умолчанию
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход2"] = SS.ExtendID("  1IKX   ", "Спр.Секции")
        var dataMapRead: MutableMap<String, Any> = mutableMapOf()
        var fieldList: MutableList<String> = mutableListOf("Спр.СинхронизацияДанных.ДатаРез1")
        try {
            dataMapRead = ExecCommand("QuestPicing", dataMapWrite, fieldList, dataMapRead, "")
        }
        catch (e: Exception){
            val toast = Toast.makeText(applicationContext, "Не удалось получить задание!", Toast.LENGTH_SHORT)
            toast.show()
        }

        if ((dataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() == -3) {
            FExcStr.text = "Нет накладных к набору!"
            return false
        }
        if ((dataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() != 3) {
            FExcStr.text = "Не известный ответ робота... я озадачен..."
            return false
        }
        FExcStr.text = dataMapRead["Спр.СинхронизацияДанных.ДатаРез1"].toString()
        return ToModeSetInicialization()
    }

    fun ChekMarkProduct():Boolean {

        if (DocsSet == null || DocsSet.isEmpty()) {
            return true
        }
        var textQuery = "SELECT " +
                "SetTable.Item as Item , " +
                "min(Item.\$Спр.Товары.ИнвКод ) as InvCode , " +
                "SUM(SetTable.CountCC ) as CountCC, " +
                "SUM(SetTable.CountMark ) as CountMark " +
                "from ( " +
                "SELECT " +
                "DocCC.\$КонтрольНабора.Товар as Item , " +
                "DocCC.\$КонтрольНабора.Количество as CountCC , " +
                "0 as CountMark " +
                "from DT\$КонтрольНабора as DocCC (nolock) " +
                "where iddoc = '${DocsSet[0].toString()}' and DocCC.\$КонтрольНабора.Корректировка = 0 and DocCC.\$КонтрольНабора.Дата5 != :EmptyDate " +
                "union all " +
                "select " +
                "Mark.\$Спр.МаркировкаТовара.Товар ," +
                "0, count(*) " +
                "from \$Спр.МаркировкаТовара as Mark (nolock) " +
                "where Mark.\$Спр.МаркировкаТовара.ФлагПоступления = 1 and Mark.\$Спр.МаркировкаТовара.ДокОтгрузки = '${SS.ExtendID(
                    DocsSet[0].toString(),"КонтрольНабора")}'" +
                "and Mark.\$Спр.МаркировкаТовара.ФлагОтгрузки = 1 " +
                "group by Mark.\$Спр.МаркировкаТовара.Товар " +
                ") as SetTable " +
                "INNER JOIN \$Спр.Товары as Item (nolock) " +
                "on SetTable.Item = Item.ID " +
                "INNER JOIN \$Спр.КатегорииТоваров as Category (nolock) " +
                "on Item.\$Спр.Товары.Категория = Category.ID " +
                "where Category.\$Спр.КатегорииТоваров.Маркировка = 1 " +
                "group by SetTable.Item " +
                "having not SUM(SetTable.CountCC ) = SUM(SetTable.CountMark ) "
        val dt = SS.ExecuteWithReadNew(textQuery) ?: return true
        if (dt.isEmpty()) return true
        //не сходится маркировка
        FExcStr.text =
            "Не сходится маркировка " + dt[0]["InvCode"].toString().trim() + "! Обратитесь к администратору!"
        SS.ExcStr = FExcStr.text.toString().trim()
        return false
    }

    private fun ToModeSetComplete(): Boolean {
        //var empbtyBox = false
        //Проверим нет ли сборочного с пустой коробкой, его в первую очередь будем закрывать
        var textQuery = "SELECT " +
                "journ.iddoc as IDDOC " +
                "FROM " +
                "_1sjourn as journ (nolock) " +
                "INNER JOIN DH\$КонтрольНабора as DocCC (nolock) " +
                "ON DocCC.iddoc = journ.iddoc " +
                "WHERE " +
                "DocCC.\$КонтрольНабора.Наборщик = :Employer " +
                "and journ.iddocdef = \$КонтрольНабора " +
                "and DocCC.\$КонтрольНабора.Дата2 = :EmptyDate " +
                "and not DocCC.\$КонтрольНабора.Дата1 = :EmptyDate " +
                "and DocCC.\$КонтрольНабора.Коробка = :EmptyID " +
                "and journ.ismark = 0 "
        textQuery = SS.QuerySetParam(textQuery, "Employer", SS.FEmployer.ID)
        textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate())
        textQuery = SS.QuerySetParam(textQuery, "EmptyID", SS.GetVoidID())
        SS.ExecuteWithRead(textQuery) ?: return false
        // Пока закомментирую, тк как делаю отбор в ускоренном режиме без проверки коробки и тд
//        if (DT.Rows.Count > 0)
//        {
//            if (!ToModeSetCompleteAfrerBox(null))
//            {
//                return false;
//            }
//            OnChangeMode(new ChangeModeEventArgs(Mode.SetComplete));
//            return true;
//        }
//        BoxOk = false;
//        DocSet.ID = null;   //Нет конкретного документа
//        FExcStr = "Отсканируйте коробку!";
//        OnChangeMode(new ChangeModeEventArgs(Mode.SetComplete));
//        return true;

        //нужно сверить маркировку если она есть
        if (!ChekMarkProduct()){
            return false
        }

        SS.CurrentMode = Global.Mode.SetComplete
        //перейдем на форму завершения набора
        val setComplete = Intent(this, SetComplete::class.java)
        //закроем доки, висящие на сотруднике с уже набранными строчками
        for (id in DocsSet) {
            setComplete.putExtra("iddoc", id)
            startActivity(setComplete)
        }
        finish()
        return true
    }

    private fun LoadDocSet(iddoc: String): Boolean {
        var textQuery =
                "SELECT top 1 " +
                    "journ.iddoc as IDDOC, " +
                    "DocCC.\$КонтрольНабора.ФлагСамовывоза as SelfRemovel, " +
                    "DocCC.\$КонтрольНабора.КолСтрок as Rows, " +
                    "journForBill.iddoc as Bill, " +
                    "Clients.descr as Client, " +
                    "DocCC.\$КонтрольНабора.Сумма as Sum, " +
                    "Bill.\$Счет.ТипНакладной as TypeNakl, " +
                    "AdressBox.descr as Box, " +
                    "isnull(DocCC.\$КонтрольНабора.Коробка , '     0   ') as BoxID " +
                    "FROM _1sjourn as journ (nolock) " +
                    "INNER JOIN DH\$КонтрольНабора as DocCC (nolock) "+
                    "ON DocCC.iddoc = journ.iddoc" +
                    "LEFT JOIN DH\$КонтрольРасходной as DocCB (nolock) " +
                    "ON DocCB.iddoc = DocCC.\$КонтрольНабора.ДокументОснование " +
                    "LEFT JOIN DH\$Счет as Bill (nolock) " +
                    "ON Bill.iddoc = DocCB.\$КонтрольРасходной.ДокументОснование " +
                    "LEFT JOIN _1sjourn as journForBill (nolock) " +
                    "ON journForBill.iddoc = Bill.iddoc " +
                    "LEFT JOIN \$Спр.Секции as Section (nolock) " +
                    "ON Section.id = DocCC.\$КонтрольНабора.Сектор " +
                    "LEFT JOIN \$Спр.Клиенты as Clients (nolock) " +
                    "ON Bill.\$Счет.Клиент = Clients.id " +
                    "LEFT JOIN \$Спр.Секции as AdressBox (nolock) "+
                    "ON AdressBox.id = DocCC.\$КонтрольНабора.Коробка " +
                    "WHERE " +
                    "journ.iddoc = :iddoc "
        textQuery = SS.QuerySetParam(textQuery, "iddoc", iddoc)
        val dataTable = SS.ExecuteWithRead(textQuery) ?: return false

        if (dataTable.isNotEmpty()) {
            DocSet = Model.StrictDoc(
                dataTable[1][0],
                dataTable[1][1].toInt(),
                "",
                dataTable[1][2].toInt(),
                dataTable[1][3],
                dataTable[1][4].trim(),
                dataTable[1][5].toBigDecimal(),
                dataTable[1][6].toInt() == 2,
                dataTable[1][7],
                dataTable[1][8]
            )

            return true
        }

        return false
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(barcodeDataReceiver, IntentFilter(ACTION_BARCODE_DATA))
        claimScanner()
        Log.d("IntentApiSample: ", "onResume")
        if(scanRes != null){
            try {
                Barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(Barcode)
            }
            catch (e: Exception){
                val toast = Toast.makeText(applicationContext, "Не удалось отсканировать штрихкод!", Toast.LENGTH_LONG)
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

}
