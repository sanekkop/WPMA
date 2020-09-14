package com.intek.wpma.ChoiseWork.Set

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.isVisible
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.Model.Model
import com.intek.wpma.R
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_correct.*


class Correct : BarcodeDataReceiver() {

    var iddoc: String = ""
    private var addressID: String = ""
    private val mainWarehouse = "     D   "
    private var ccItem: Model.StructItemSet? = null
    private var docSet: Model.StrictDoc? = null
    var barcode: String = ""
    private var choiseCorrect: Int = 0          //тип корректировки
    private var countFact: Int = 0              //при наборе маркировок, чтобы не сбились уже отсканированные QR-коды
    private var enterCount: Int = 0             //колво позиций для корректировки (вводится вручную)
    private var enterCountWithoutQRCode = 0     //колво позиций без QR - кода (вводится вручную)
    private var countWithoutQRCode: Int = 0     //колво уже скорректированных позиций без QR - кода
    private var countCorrect: Int = 0           //общее колво уже скорректированных позиций (с QR - кодом и без)
    var codeId: String = ""             //показатель по которому можно различать типы штрих-кодов
    private var flagBtn = 0
    private var flagMark = 0                    //флаг маркировки

    //region шапка с необходимыми функциями для работы сканеров перехватчиков кнопок и т.д.
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
    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }
    override fun onResume() {
        super.onResume()
        //        IntentFilter intentFilter = new IntentFilter("hsm.RECVRBI");
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
                val toast = Toast.makeText(applicationContext, "Отсутствует соединение с базой!", Toast.LENGTH_LONG)
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

        reactionKey(keyCode, event)
        return super.onKeyDown(keyCode, event)
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_correct)

        iddoc = intent.extras!!.getString("iddoc")!!
        addressID = intent.extras!!.getString("AddressID")!!
        title = ss.title
        countFact = intent.extras!!.getString("CountFact")!!.toInt()
        //заполним заново товар и док
        getItemAndDocSet()
        val label: TextView = findViewById(R.id.label)

        label.text = "Корректировка позиции ${ccItem!!.InvCode}"
        val enterCountCorrect: EditText = findViewById(R.id.enterCountCorrect)
        NoQRCode.setOnClickListener {
            FExcStr.text = "Введите колво товара без QR-кода"
            enterCountCorrect.setText("")
            enterCountCorrect.visibility = View.VISIBLE
            NoQRCode.isFocusable = false
            flagBtn = 1
        }

        //дабы не дублировать код будем эмулировать нажатие кнопки
        btnDefect.setOnClickListener {
            btnShortage.visibility = View.INVISIBLE
            btnRejection.visibility = View.INVISIBLE
            btnDefect.isFocusable = false
            choiseCorrect = 1
            enterCountCorrect()
        }
        btnShortage.setOnClickListener {
            btnDefect.visibility = View.INVISIBLE
            btnRejection.visibility = View.INVISIBLE
            btnShortage.isFocusable = false
            choiseCorrect = 2
            enterCountCorrect()
        }
        btnRejection.setOnClickListener {
            btnDefect.visibility = View.INVISIBLE
            btnShortage.visibility = View.INVISIBLE
            btnRejection.isFocusable = false
            choiseCorrect = 3
            enterCountCorrect()
        }
        if (ss.isMobile){
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@Correct, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","SetCorrect")
                startActivity(scanAct)
            }
        }

    }

    private fun reactionBarcode(Barcode: String) {
        if (countCorrect < enterCount) {
            if (codeId == barcodeId) {//проверим DataMatrix ли пришедший код
                //проверим, был ли уже принят этот товар с маркировкой
                val testBatcode = Barcode.replace("'","''")
                var textQuery =
                    "SELECT \$Спр.МаркировкаТовара.Товар, \$Спр.МаркировкаТовара.ДокОтгрузки, \$Спр.МаркировкаТовара.ФлагОтгрузки " +
                            "FROM \$Спр.МаркировкаТовара " +
                            "WHERE \$Спр.МаркировкаТовара.Маркировка like ('%' + SUBSTRING('${testBatcode.trim()}',1,31) + '%') " +
                            "and \$Спр.МаркировкаТовара.Товар = '${ccItem!!.ID}' "
                val dt = ss.executeWithRead(textQuery) ?: return
                if (dt.isEmpty()){
                    FExcStr.text = "Маркировка не найдена, либо товар уже набран/скорректирован! Отсканируйте QR - код"
                    return
                }
                if (dt[1][2].toInt() == 1 && dt[1][1] == ss.extendID(iddoc,"КонтрольНабора")) {
                    //корректируют позицию, которую только что набрали
                    countFact -= 1
                }
                //найдем маркировку в справочнике МаркировкаТовара, занулим флаг
                textQuery =
                    "UPDATE \$Спр.МаркировкаТовара " +
                            "SET \$Спр.МаркировкаТовара.ДокОтгрузки = '${ss.extendID(iddoc,"КонтрольНабора")}', \$Спр.МаркировкаТовара.ФлагОтгрузки = 0 " +
                            "where \$Спр.МаркировкаТовара.Маркировка like ('%' +SUBSTRING('${testBatcode.trim()}',1,31) + '%') " +
                            "and \$Спр.МаркировкаТовара.Товар = '${ccItem!!.ID}' "
                if (!ss.executeWithoutRead(textQuery)) {
                    FExcStr.text = "Не удалось освободить маркировку!"
                    return
                }
                countCorrect += 1
                FExcStr.text = "Корректировка принята " + ccItem!!.InvCode.trim() + " - " + countCorrect.toString() + " шт. ( Осталось: " + (enterCount - countCorrect).toString() + ") Отсканируйте QR - код!"
                if (countCorrect == enterCount) { // скорректировали задданное колво позиций
                    completeCorrect(choiseCorrect, countCorrect)
                }
            } else {
                FExcStr.text = "Неправильный тип QR - кода"
            }
        }
    }

    private fun getItemAndDocSet(): Boolean {
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
                    "AdressBox.descr as Box " +
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
                    "DocCC.iddoc = :iddoc " +
                    "and DocCC.\$КонтрольНабора.Дата5 = :EmptyDate " +
                    "and DocCC.\$КонтрольНабора.Корректировка = 0 " +
                    "and DocCC.\$КонтрольНабора.Количество > 0 " +
                    "and DocCC.\$КонтрольНабора.Адрес0 = :AddressID " +
                    "order by " +
                    "DocCCHead.\$КонтрольНабора.Сектор , Sections.\$Спр.Секции.Маршрут , LINENO_"

        textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
        textQuery = ss.querySetParam(textQuery, "Warehouse", mainWarehouse)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        textQuery = ss.querySetParam(textQuery, "iddoc", iddoc)
        textQuery = ss.querySetParam(textQuery, "AddressID", addressID)
        val dataTable = ss.executeWithRead(textQuery) ?: return false

        ccItem = Model.StructItemSet(
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

        docSet = Model.StrictDoc(
            dataTable[1][10],                           //ID
            dataTable[1][18].toInt(),                   //SelfRemovel
            "",                                   //View
            dataTable[1][14].toInt(),                   //Rows
            dataTable[1][13],                           //FromWarehouseID
            dataTable[1][19].trim(),                    //Client
            dataTable[1][16].toBigDecimal(),            //Sum
            dataTable[1][20].toInt() == 2,      //Special
            dataTable[1][22],                           //Box
            dataTable[1][21]                            //BoxID
        )


        return true

    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?) {

        // нажали назад, вернемся на форму набора
        if (keyCode == 4) {
            if (choiseCorrect == 0) {
                FExcStr.text = ""
                val setInitialization = Intent(this, SetInitialization::class.java)
                setInitialization.putExtra("ParentForm", "Correct")
                setInitialization.putExtra("DocSetID", docSet!!.id)  //вернемся на определенную, так как что-то еще осталось
                setInitialization.putExtra("AddressID", ccItem!!.AdressID)
                setInitialization.putExtra("PreviousAction", FExcStr.text.toString())
                setInitialization.putExtra("CountFact", countFact.toString())
                setInitialization.putExtra("isMobile",ss.isMobile.toString())
                startActivity(setInitialization)
                finish()
            }
        }

        if (keyCode in 8..10) {

            choiseCorrect = 0
            // нажали 1 - брак
            if (keyCode.toString() == "8") {
                btnShortage.visibility = View.INVISIBLE
                btnRejection.visibility = View.INVISIBLE
                btnDefect.isFocusable = false
                choiseCorrect = 1
            }
            // нажали 2 - недостача
            if (keyCode.toString() == "9") {
                btnDefect.visibility = View.INVISIBLE
                btnRejection.visibility = View.INVISIBLE
                btnShortage.isFocusable = false
                choiseCorrect = 2
            }
            // нажали 3 - отказ
            if (keyCode.toString() == "10") {
                btnDefect.visibility = View.INVISIBLE
                btnShortage.visibility = View.INVISIBLE
                btnRejection.isFocusable = false
                choiseCorrect = 3
            }
            enterCountCorrect()
        }
    }

    private fun enterCountCorrect() {
        enterCountCorrect.visibility = View.VISIBLE
        FExcStr.text = "Укажите количество в штуках"
        enterCountCorrect.setOnKeyListener { v: View, keyCode: Int, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                if (ss.isMobile){  //спрячем клаву
                    val inputManager: InputMethodManager =  applicationContext.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    inputManager.hideSoftInputFromWindow(this.currentFocus!!.windowToken,InputMethodManager.HIDE_NOT_ALWAYS)
                    //фокус на поле ввода при открытии активити не пашет, надо сделать
                    val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.showSoftInput(this.enterCountCorrect, InputMethodManager.SHOW_IMPLICIT)
                }
                try {
                    if (flagBtn == 0) {
                        enterCount = enterCountCorrect.text.toString().toInt()
                        if (enterCount > ccItem!!.Count || enterCount > (ccItem!!.Count - countFact)) {
                            FExcStr.text = "Нельзя скорректировать столько! "
                            if (enterCount > (ccItem!!.Count - countFact)) {
                                FExcStr.text =
                                    "Нельзя скорректировать столько! Возможно: " + (ccItem!!.Count - countFact).toString() + " шт"
                            }
                        } else {
                            //проверим есть ли маркировка
                            val textQuery = "SELECT " +
                                    "Product.\$Спр.Товары.ИнвКод as ИнвКод , Product.descr as Name , Product.\$Спр.Товары.Категория as Категория " +
                                    "FROM " +
                                    "\$Спр.Товары  as Product (nolock)" +
                                    "INNER JOIN \$Спр.КатегорииТоваров  as Categories (nolock) " +
                                    "ON Categories.id = Product.\$Спр.Товары.Категория " +
                                    "WHERE " +
                                    "Product.id = '${ccItem!!.ID}' and Categories.\$Спр.КатегорииТоваров.Маркировка = 1 "
                            val dt = ss.executeWithRead(textQuery)

                            //есть маркировка, пусть сканируют QR-code
                            if (dt!!.isNotEmpty()) {
                                flagMark = 1
                                FExcStr.text = "Отсканируйте QR - код! (Осталось: " + (enterCount - countCorrect).toString() + " шт)"
                                enterCountCorrect.visibility = View.INVISIBLE
                                //без QR - кода можно корректировать только при недостачи
                                if(btnShortage.isVisible) {
                                    NoQRCode.visibility = View.VISIBLE
                                }

                            } else {
                                //if (countCorrect == EnterCount) {
                                countCorrect = enterCount
                                completeCorrect(choiseCorrect, countCorrect)
                                // }
                            }
                        }
                    } else {
                        //нажали кн "без QR - кода "
                        enterCountWithoutQRCode = enterCountCorrect.text.toString().toInt()
                        if (enterCountWithoutQRCode > enterCount - countCorrect) {
                            FExcStr.text =
                                "Нельзя скорректировать столько! Возможно: " + (enterCount - countCorrect).toString() + " шт"

                        } else {
                            flagBtn = 0
                            enterCountCorrect.visibility = View.INVISIBLE
                            countWithoutQRCode += enterCountWithoutQRCode
                            countCorrect += enterCountWithoutQRCode
                            if (countCorrect == enterCount) {
                                //все позиций скорректированы, завершим корректировку
                                completeCorrect(choiseCorrect, countCorrect)
                            }
                            FExcStr.text = "Корректировка принята " + ccItem!!.InvCode.trim() + " - " + countCorrect.toString() + " шт. ( Осталось: " + (enterCount - countCorrect).toString() + ") Отсканируйте QR - код!"
                        }
                    }

                }
                catch (e: Exception) {

                }
            }
            false
        }
    }

    private fun completeCorrect(Choise: Int, CountCorrect: Int): Boolean {
        //Заглушка, рефрешим позицию, чтобы не было проблем, если оборвется связь
//        if (!ToModeSet(CCItem.AdressID, DocSet.ID))
//        {
//            FCurrentMode = Mode.SetCorrect;
//            return false;
//        }
//        FCurrentMode = Mode.SetCorrect;
        //конец заглушки

        if (CountCorrect <= 0 || CountCorrect > ccItem!!.Count) {
            FExcStr.text = "Нельзя скорректировать столько!"
            return false
        }

        val adressCode:Int
        val correctReason: String
        val what: String
        when (Choise) {
            1 -> {
                adressCode = 7
                correctReason = "   2EU   "
                what = "брак"

            }

            2 -> {
                adressCode = 12
                correctReason = "   2EV   "
                what = "недостача"
            }

            3 -> {
                adressCode = 2
                correctReason = "   2EW   "
                what = "отказ"
            }

            4 -> {
                adressCode = 2
                correctReason = "   4MG   "
                what = "отказ по ШК"
            }

            else -> {
                FExcStr.text = "Неясная причина корректировки!"
                return false
            }
        }

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
                    "select \$КонтрольНабора.СтрокаИсх , \$КонтрольНабора.Товар , :CountCorrect ," +
                    "\$КонтрольНабора.Единица , \$КонтрольНабора.Цена , \$КонтрольНабора.Коэффициент , :CountCorrect*\$КонтрольНабора.Цена ," +
                    "\$КонтрольНабора.Секция , :CountCorrect , :Reason, , \$КонтрольНабора.ЕдиницаШК ," +
                    "\$КонтрольНабора.Состояние0 , \$КонтрольНабора.Адрес0 , :AdressCode , \$КонтрольНабора.АдресКорр ," +
                    "\$КонтрольНабора.ДокБлокировки , \$КонтрольНабора.Дата5 , \$КонтрольНабора.Время5 , \$КонтрольНабора.Контейнер , " +
                    "(select max(lineno_) + 1 from DT\$КонтрольНабора where iddoc = :iddoc), iddoc, 0 " +
                    "from DT\$КонтрольНабора as ForInst where ForInst.iddoc = :iddoc and ForInst.lineno_ = :currline; " +
                    "if @@rowcount = 0 rollback tran else commit tran " +
                    "end " +
                    "else rollback"

        textQuery = ss.querySetParam(textQuery, "count", ccItem!!.Count - CountCorrect)
        textQuery = ss.querySetParam(textQuery, "CountCorrect", CountCorrect)
        textQuery = ss.querySetParam(textQuery, "iddoc", docSet!!.id)
        textQuery = ss.querySetParam(textQuery, "currline", ccItem!!.CurrLine)
        textQuery = ss.querySetParam(textQuery, "Reason", correctReason)
        textQuery = ss.querySetParam(textQuery, "AdressCode", adressCode)

        if (!ss.executeWithoutRead(textQuery)) {
            return false
        }
        FExcStr.text =
            "Корректировка принята " + ccItem!!.InvCode.trim() + " - " + CountCorrect.toString() + " шт. (" + what + ")"

        // переходим обратно на форму отбора и завершаем корректировку
        val setInitialization = Intent(this, SetInitialization::class.java)
        if (CountCorrect == ccItem!!.Count) {
            setInitialization.putExtra("ParentForm", "Correct")
            setInitialization.putExtra("DocSetID", "")  //скорректировали полностью
            setInitialization.putExtra("AddressID", "")
        } else {
           setInitialization.putExtra("ParentForm", "Correct")
            setInitialization.putExtra("DocSetID", docSet!!.id)  //вернемся на определенную, так как что-то еще осталось
            if (CountCorrect == ccItem!!.Count) {
                setInitialization.putExtra("AddressID", "")
            } else setInitialization.putExtra("AddressID", ccItem!!.AdressID)
        }
        setInitialization.putExtra("PreviousAction", FExcStr.text.toString())
        setInitialization.putExtra("isMobile",ss.isMobile.toString())
        setInitialization.putExtra("CountFact", countFact.toString())
        startActivity(setInitialization)
        finish()


        return true
    } // CompleteCorrect

}

