package com.intek.wpma.ChoiseWork.Set


import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.*
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.intek.wpma.*
import com.intek.wpma.Helpers.Helper
import com.intek.wpma.Model.Model
import kotlinx.android.synthetic.main.activity_menu.*
import kotlinx.android.synthetic.main.activity_set.*
import kotlinx.android.synthetic.main.activity_set.terminalView
import java.math.BigDecimal
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*


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
    // количество принимаемой позиции
    var CountFact: Int = 0
    var CurrLine: Int = 0
    var PrinterPath = ""    //сюда будем запоминать принтер после завершения набора, чтобы постоянно не сканировать
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
            ToModeSetInicialization()
        } else if (ParentForm == "Correct" || ParentForm == "WatchTablePart") {
            try {
                PreviousAction.text = intent.extras!!.getString("PreviousAction")!!
                PrinterPath = intent.extras!!.getString("PrinterPath")!!
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
        } else if (ParentForm == "SetComplete") {
            try {
                PrinterPath = intent.extras!!.getString("PrinterPath")!!
                GetDocsSet()
                ToModeSetInicialization()
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
                correct.putExtra("PrinterPath", PrinterPath)
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
                    watchForm.putExtra("PrinterPath", PrinterPath)
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
                    "DocCC.SP3109 as ID, " +
                    "DocCC.lineno_ as LINENO_, " +
                    "Goods.descr as ItemName, " +
                    "Goods.SP1036 as InvCode, " +
                    "Goods.SP5086 as Details, " +
                    "DocCC.SP3110 as Count, " +
                    "DocCC.SP5508 as Adress, " +
                    "DocCC.SP3112 as Price, " +
                    "Sections.descr as AdressName, " +
                    "ISNULL(AOT.Balance, 0) as Balance, " +
                    //Реквизиты документа
                    "DocCC.iddoc as IDDOC, " +
                    "journForBill.docno as DocNo, " +
                    "CONVERT(char(8), CAST(LEFT(journForBill.date_time_iddoc, 8) as datetime), 4) as DateDoc, " +
                    "journForBill.iddoc as Bill, " +
                    "DocCCHead.SP2814 as Rows, " +
                    "Sector.descr as Sector, " +
                    "DocCCHead.SP3114 as Sum, " +
                    "DocCCHead.SP3595 as Number, " +
                    "DocCCHead.SP2841 as SelfRemovel, " +
                    "Clients.descr as Client, " +
                    "Bill.SP3094 as TypeNakl, " +
                    "isnull(DocCCHead.SP6525 , :EmptyID) as BoxID, " +
                    "AdressBox.descr as Box, " +
                    "DocCCHead.SP2764 as Sector, " +
                    "DocCCHead.SP2814 as Rows, " +
                    "DocCCHead.SP2773 as TypeSetter, " +
                    "DocCCHead.SP2841 as FlagDelivery " +
                    "from " +
                    "DT2776 as DocCC (nolock) " +
                    "LEFT JOIN DH2776 as DocCCHead (nolock) " +
                    "ON DocCCHead.iddoc = DocCC.iddoc " +
                    "LEFT JOIN SC33 as Goods (nolock) " +
                    "ON Goods.id = DocCC.SP3109 " +
                    "LEFT JOIN SC1141 as Sections (nolock) " +
                    "ON Sections.id = DocCC.SP5508 " +
                    "LEFT JOIN ( " +
                    "select " +
                    "RegAOT.SP4342 as item, " +
                    "RegAOT.SP4344 as adress, " +
                    "sum(RegAOT.SP4347 ) as balance " +
                    "from " +
                    "RG4350 as RegAOT (nolock) " +
                    "where " +
                    "period = @curdate " +
                    "and SP4343 = :Warehouse " +
                    "and SP4345 = 2 " +
                    "group by RegAOT.SP4342 , RegAOT.SP4344 " +
                    ") as AOT " +
                    "ON AOT.item = DocCC.SP3109 and AOT.adress = DocCC.SP5508 " +
                    "LEFT JOIN DH2763 as DocCB (nolock) " +
                    "ON DocCB.iddoc = DocCCHead.SP2771 " +
                    "LEFT JOIN DH196 as Bill (nolock) " +
                    "ON Bill.iddoc = DocCB.SP2759 " +
                    "LEFT JOIN _1sjourn as journForBill (nolock) " +
                    "ON journForBill.iddoc = Bill.iddoc " +
                    "LEFT JOIN SC1141 as Sector (nolock) " +
                    "ON Sector.id = DocCCHead.SP2764 " +
                    "LEFT JOIN SC46 as Clients (nolock) " +
                    "ON Bill.SP199 = Clients.id " +
                    "LEFT JOIN SC1141 as AdressBox (nolock) " +
                    "ON AdressBox.id = DocCCHead.SP6525 " +
                    "where " +
                    "DocCC.iddoc in (:Docs) " +
                    "and DocCC.SP5986 = :EmptyDate " +
                    "and DocCC.SP3116 = 0 " +
                    "and DocCC.SP3110 > 0 "

        if (AdressID != null) textQuery += "and DocCC.SP5508 = :Adress "
        if (iddoc != null) textQuery += "and DocCC.iddoc = :iddoc "
        textQuery += "order by " +
                "DocCCHead.SP2764 , Sections.SP5103 , LINENO_"
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
        textQuery =
            "SELECT " +
                    "Product.\$Спр.Товары.ИнвКод as ИнвКод , Product.descr as Name , Product.\$Спр.Товары.Категория as Категория " +
                    "FROM " +
                    "\$Спр.Товары  as Product (nolock)" +
                    "INNER JOIN \$Спр.КатегорииТоваров  as Categories (nolock) " +
                    "ON Categories.id = Product.\$Спр.Товары.Категория " +
                    "WHERE " +
                    "Product.id = '${CCItem!!.ID}' and Categories.\$Спр.КатегорииТоваров.Маркировка = 1 "
        var dt = SS.ExecuteWithRead(textQuery) ?: return false
        if (dt.isNotEmpty()) {
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
                "where iddoc = '${DocCC!!.ID}' and DocCC.\$КонтрольНабора.Корректировка = 0 and DocCC.\$КонтрольНабора.Дата5 != :EmptyDate "
            textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate())
            val dt = SS.ExecuteWithReadNew(textQuery)
            if(dt != null && dt.isNotEmpty()) {


                if (dt[0]["CountCC"] == "null") {        //потерянные маркировки есть, а в доке нет ни одной принятой позиции
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
                    watchForm.putExtra("PrinterPath", PrinterPath)
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
                    "sum(DocCC.SP3114) as Sum, " +
                    "count(*) Amount " +
                    "from " +
                    "DT2776 as DocCC (nolock) " +
                    "where " +
                    "DocCC.iddoc in (:Docs ) " +
                    "and DocCC.SP5986 = :EmptyDate " +
                    "and DocCC.SP3116 = 0 " +
                    "and DocCC.SP3110 > 0 "
        textQuery = textQuery.replace(":Docs", SS.helper.ListToStringWithQuotes(DocsSet))
        textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate())
        val dataTable = SS.ExecuteWithRead(textQuery) ?: return false
        if (dataTable.isNotEmpty()) {
            DocSetSum = dataTable[1][0].toBigDecimal()
            AllSetsRow = dataTable[1][1].toInt()
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
                "CAST(Units.SP2232 as int) as Coef, " +
                "ceiling(2/SP2232 ) as AmountOKEI2 " +
                "FROM " +
                "SC2237 as Units (nolock) " +
                "inner join SC2229 as OKEI (nolock)" +
                "on OKEI.id = Units.SP2230 " +
                "left join SC2229  as OKEI2 (nolock) " +
                "on OKEI2.id = Units.SP6584 " +
                "WHERE " +
                "Units.parentext = :CurrentItem " +
                "and OKEI.id <> :OKEIKit" +
                "and Units.ismark = 0 " +
                "and ceiling(2/SP2232 )*SP2232 = :amount " +
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

    fun LockDoc(IDDoc: String): Boolean {
        return IBS_Lock("int_doc_$IDDoc")
    }

    fun IBS_Lock(BlockText: String): Boolean {
        var textQuery =
            "set nocount on; " +
                    "declare @id bigint; " +
                    "exec IBS_Inicialize_with_DeviceID_new :Employer, :HostName, :DeviceID, @id output; " +
                    "select @id as ID;" +
                    "set nocount on; " +
                    "declare @result int; " +
                    "exec IBS_Lock :BlockText, @result output; " +
                    "select @result as result;"
        textQuery = SS.QuerySetParam(textQuery, "Employer", SS.FEmployer.ID)
        textQuery = SS.QuerySetParam(textQuery, "HostName", "Android - " + SS.terminal)
        textQuery = SS.QuerySetParam(textQuery, "DeviceID", SS.ANDROID_ID)
        textQuery = SS.QuerySetParam(textQuery, "BlockText", BlockText)
        var dataTable: Array<Array<String>>? = SS.ExecuteWithRead(textQuery) ?: return false
        if (dataTable!![1][0].toInt() > 0) {
            return true
        } else {
            FExcStr.text = "Объект заблокирован!" //Ответ по умолчанию
            //Покажем кто заблокировал
            textQuery =
                "SELECT " +
                        "rtrim(Collation.HostName) as HostName, " +
                        "rtrim(Collation.UserName) as UserName, " +
                        "convert(char(8), Block.date_time, 4) as Date, " +
                        "substring(convert(char, Block.date_time, 21), 12, 8) as Time " +
                        "FROM " +
                        "IBS_Block as Block " +
                        "INNER JOIN IBS_Collation as Collation " +
                        "ON Collation.ID = Block.ProcessID " +
                        "WHERE " +
                        "left(Block.BlockText, len(:BlockText)) = :BlockText "
            textQuery = SS.QuerySetParam(textQuery, "BlockText", BlockText)
            dataTable = SS.ExecuteWithRead(textQuery)
            if (dataTable!!.isNotEmpty()) {
                FExcStr.text =
                    "Объект заблокирован! " + dataTable[1][1] + ", " + dataTable[1][0] +
                            ", в " + dataTable[1][3] + " (" + dataTable[1][2] + ")"
            }
            return false
        }
    }

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
                    //BadDone();
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
                FExcStr.text = "Нет действий с этим штирхкодом в данном режиме!"; return false
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
                FExcStr.text = "Нет действий с данным справочником в данном режиме!"; false
            }
        }
    }

    private fun RBSet(Barcode: String): Boolean {
        if (SS.CurrentAction != Global.ActionSet.ScanItem && SS.CurrentAction != Global.ActionSet.ScanQRCode) {
            FExcStr.text = "Неверно! " + WhatUNeed()
            return false
        }
        var textQuery: String
        var dt: Array<Array<String>>


        if (SS.CurrentAction == Global.ActionSet.ScanQRCode && codeId == BarcodeId){//заодно проверим DataMatrix ли пришедший код
            //найдем маркировку в справочнике МаркировкаТовара
            var testBatcode = Barcode.replace("'","''")
            textQuery =
                "SELECT SP7271 " +
                        "FROM SC7277 " +
                        "where SC7277.SP7270 like ('%' +SUBSTRING('${testBatcode.trim()}',1,31) + '%') " +
                            "and SC7277.SP7271 = '${CCItem!!.ID}' " +
                            "and SC7277.SP7273 = 1 and SC7277.SP7275 = 0 and SC7277.SP7274 = '   0     0   ' "
            dt = SS.ExecuteWithRead(textQuery) ?: return false
            if (dt.isEmpty()){
                FExcStr.text = "Маркировка не найдена, либо товар уже набран/скорректирован!" + WhatUNeed()
                return false
            }
            else {
                //взведем фдаг отгрузки + проставим док отгрузки
                textQuery =
                    "UPDATE SC7277 " +
                            "SET SP7274 = '${SS.ExtendID(DocCC!!.ID,"КонтрольНабора")}', SP7275 = 1 " +
                    "WHERE " +
                            "SC7277.SP7270 like ('%' +SUBSTRING('${testBatcode.trim()}',1,31) + '%') " +
                            "and SC7277.SP7271 = '${CCItem!!.ID}' "
                if (!SS.ExecuteWithoutRead(textQuery)) {
                    FExcStr.text = "QR - code не распознан! Заново " + WhatUNeed()
                    return false
                }

                if (CCItem!!.Count > 1){
                    CountFact += 1
                    //набрали все позиции с маркировкой
                    if (CountFact == CCItem!!.Count){
                        EnterCountSet(CountFact)
                        return true
                    }

                    if (SS.isMobile){
                        PreviousAction.text = "Для завершения набора позиции с маркировкой нажмите ЗДЕСЬ!"
                    }
                    else PreviousAction.text = "Для завершения набора позиции с маркировкой нажмите 'ENTER'!"
                }
                else {
                    CountFact = 1
                    EnterCountSet(CountFact)
                }
                FExcStr.text = "Отобрано " + CCItem!!.InvCode.trim() + " - " + CountFact.toString() + " шт. (строка " + CCItem!!.CurrLine + ") " + WhatUNeed() + " - СЛЕДУЮЩИЙ!"
                count.text = (CCItem!!.Count - CountFact).toString() + " шт по 1"
            }
            return true
        }
        //нужно сканировать маркировку, а сканиуруют что-то другое
        if (SS.CurrentAction == Global.ActionSet.ScanQRCode && codeId != BarcodeId){
            FExcStr.text = "Неверно! " + WhatUNeed()
            return false
        }
        if (SS.CurrentAction == Global.ActionSet.ScanItem) {
            textQuery =
                "SELECT " +
                        "Units.parentext as ItemID, " +
                        "Goods.SP1036 as InvCode, " +
                        "Units.SP2230 as OKEI " +
                        "FROM SC2237 as Units (nolock) " +
                        "LEFT JOIN SC33 as Goods (nolock) " +
                        "ON Goods.id = Units.parentext " +
                        "WHERE Units.SP2233 = :Barcode "
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
            textQuery =
                "SELECT " +
                        "Product.SP1036, Product.descr, Product.SP1436 " +
                        "FROM " +
                        "SC33 as Product " +
                        "INNER JOIN SC1434 as Categories " +
                        "ON Categories.id = Product.SP1436 " +
                        "WHERE " +
                        "Product.id = '${CCItem!!.ID}' and Categories.SP7268 = 1"

            dt = SS.ExecuteWithRead(textQuery) ?: return false
            //есть маркировка, пусть сканируют QR-code
            if (dt.isNotEmpty()) {
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

        val textQuery: String =
            "SELECT SP3964, descr FROM SC1141 (nolock) WHERE SP1935='$IDD'"
        val result = SS.ExecuteWithRead(textQuery)
        Section = Model.Section(result!![1][0], IDD, result.get(1)[0], result[1][1].trim())

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
                    textQuery =
                        "SELECT ID, SP3964, descr FROM SC1141 (nolock) WHERE SP1935='$IDD'"
                    val result = SS.ExecuteWithRead(textQuery) ?: return false
                    Section = Model.Section(
                        result[1][0],
                        IDD,
                        result.get(1)[0],
                        result[1][1].trim()
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
                            "SELECT SP7271 " +
                            "FROM SC7277 " +
                            "WHERE " +
                                    "SC7277.SP7271 = '${CCItem!!.ID}' " +
                                    "and SC7277.SP7273 = 1 and SC7277.SP7275 = 0 and SC7277.SP7274 = '   0     0   ' "
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
            } else if (SS.IsSC(IDD, "Принтеры")) {
                //получим путь принтера
                textQuery =
                    "select descr, SP2461 " +
                            "from SC2459 " +
                            "where SP2465 = '$IDD'"
                val dataTable = SS.ExecuteWithRead(textQuery) ?: return false

                PrinterPath = dataTable[1][1]
                FExcStr.text = "Отсканирован принтер " + PrinterPath.trim() + "\n" + WhatUNeed()
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
                        "update DT2776 " +
                        "set SP3110 = :count, " +
                        "SP3114 =  :count * SP3112 " +
                        "where DT2776.iddoc = :iddoc and DT2776.lineno_ = :currline; " +
                        "if @@rowcount > 0 begin " +
                        "insert into DT2776 (SP3108 , SP3109 , SP3110 ," +
                        "SP3111 , SP3112 , SP3113 , SP3114 ," +
                        "SP3115 , SP3116 , SP3117 , SP4977 ," +
                        "SP5507 , SP5508 , SP5509 , SP5510 ," +
                        "SP5673 , SP5986 , SP5987 , SP5988 , " +
                        "lineno_, iddoc, SP6447 ) " +
                        "select SP3108 , SP3109 , :remaincount ," +
                        "SP3111 , SP3112 , SP3113 , :count * SP3112 ," +
                        "SP3115 , SP3116 , SP3117 , SP4977 ," +
                        "SP5507 , SP5508 , SP5509 , SP5510 ," +
                        "SP5673 , SP5986 , SP5987 , SP5988 , " +
                        "(select max(lineno_) + 1 from DT2776 where iddoc = :iddoc), iddoc, 0 " +
                        "from DT2776 as ForInst where ForInst.iddoc = :iddoc and ForInst.lineno_ = :currline; " +
                        "select max(lineno_) as newline from DT2776 where iddoc = :iddoc; " +
                        "if @@rowcount = 0 rollback tran else commit tran " +
                        "end " +
                        "else rollback"
            textQuery = SS.QuerySetParam(textQuery, "count", CountFact)
            textQuery =
                SS.QuerySetParam(textQuery, "remaincount", CCItem!!.Count - CountFact)
            textQuery = SS.QuerySetParam(textQuery, "iddoc", DocSet!!.ID)
            textQuery = SS.QuerySetParam(textQuery, "currline", CCItem!!.CurrLine)

            val dt = SS.ExecuteWithRead(textQuery) ?: return false
            //Писать будем в добавленную, так лучше! Поэтому обновляем корректную строчку
            CurrLine = dt[1][0].toInt()
        }
        //фиксируем строку
        var textQuery = "UPDATE DT2776 WITH (rowlock) " +
                "SET SP5986 = :Date5, " +
                "SP5987 = :Time5, " +
                "SP5988 = :id " +
                "WHERE DT2776.iddoc = :DocCC and DT2776.lineno_ = :lineno_; "


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
        if (SS.isMobile){  //спрячем клаву
            //val enterCount: EditText = findViewById(R.id.enterCount)
            //enterCount.imeOptions = EditorInfo.IME_ACTION_DONE


            //android:imeOptions="actionDone"
        }
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
            //свайп на просмотр
//            MotionEvent.ACTION_MOVE -> {
//                FExcStr.text = "Подгружаю список..."
//                //перейдем на форму просмотра
//                val WatchForm = Intent(this, WatchTablePart::class.java)
//                WatchForm.putExtra("Employer", Employer)
//                WatchForm.putExtra("EmployerIDD", EmployerIDD)
//                WatchForm.putExtra("EmployerFlags", EmployerFlags)
//                WatchForm.putExtra("EmployerID", EmployerID)
//                WatchForm.putExtra("iddoc", DocSet!!.ID)
//                WatchForm.putExtra("ItemCode", CCItem!!.InvCode)
//                WatchForm.putExtra("addressID", CCItem!!.AdressID)
//                WatchForm.putExtra("DocView", DocSet!!.View)
//                WatchForm.putExtra("terminalView",terminalView.text.trim())
//                WatchForm.putExtra("CountFact",CountFact.toString())
//                WatchForm.putExtra("PrinterPath", PrinterPath)
//                WatchForm.putExtra("isMobile",isMobile.toString())
//                startActivity(WatchForm)
//                finish()
//            }
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
            watchForm.putExtra("PrinterPath", PrinterPath)
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
            correct.putExtra("PrinterPath", PrinterPath)
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

    private fun ToModeSetComplete(): Boolean {
        //var empbtyBox = false
        //Проверим нет ли сборочного с пустой коробкой, его в первую очередь будем закрывать
        var textQuery =
            "SELECT " +
                    "journ.iddoc as IDDOC " +
                    "FROM " +
                    "_1sjourn as journ (nolock) " +
                    "INNER JOIN DH2776 as DocCC (nolock) " +
                    "ON DocCC.iddoc = journ.iddoc " +
                    "WHERE " +
                    "DocCC.SP2773 = :Employer " +
                    "and journ.iddocdef = 2776 " +
                    "and DocCC.SP2767 = :EmptyDate " +
                    "and not DocCC.SP2765 = :EmptyDate " +
                    "and DocCC.SP6525 = :EmptyID " +
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

        SS.CurrentMode = Global.Mode.SetComplete
        //перейдем на форму завершения набора
        val setComplete = Intent(this, SetComplete::class.java)
        setComplete.putExtra("PrinterPath", PrinterPath)
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
                    "DocCC.SP2841 as SelfRemovel, " +
                    "DocCC.SP2814 as Rows, " +
                    "journForBill.iddoc as Bill, " +
                    "Clients.descr as Client, DocCC.SP3114 as Sum, " +
                    "Bill.SP3094 as TypeNakl, AdressBox.descr as Box, isnull(DocCC.SP6525 , '     0   ') as BoxID " +
                    "FROM _1sjourn as journ (nolock) " +
                    "INNER JOIN DH2776 as DocCC (nolock) ON DocCC.iddoc = journ.iddoc" +
                    "LEFT JOIN DH2763 as DocCB (nolock) ON DocCB.iddoc = DocCC.SP2771 " +
                    "LEFT JOIN DH196 as Bill (nolock) ON Bill.iddoc = DocCB.SP2759 " +
                    "LEFT JOIN _1sjourn as journForBill (nolock) ON journForBill.iddoc = Bill.iddoc " +
                    "LEFT JOIN SC1141 as Section (nolock) ON Section.id = DocCC.SP2764" +
                    "LEFT JOIN SC46 as Clients (nolock) ON Bill.SP199 = Clients.id " +
                    "LEFT JOIN SC1141 as AdressBox (nolock) ON AdressBox.id = DocCC.SP6525" +
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
