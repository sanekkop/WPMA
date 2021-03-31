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
import android.widget.Toast
import com.intek.wpma.*
import com.intek.wpma.Helpers.Helper
import com.intek.wpma.Model.Model
import com.intek.wpma.Ref.RefEmployer
import com.intek.wpma.Ref.RefPalleteMove
import com.intek.wpma.Ref.RefSection
import kotlinx.android.synthetic.main.activity_accept.*
import kotlinx.android.synthetic.main.activity_accept.FExcStr
import kotlinx.android.synthetic.main.activity_accept.palletPal
import kotlinx.android.synthetic.main.activity_accept.printPal
import kotlinx.android.synthetic.main.activity_accept.scroll
import kotlinx.android.synthetic.main.activity_accept.table
import kotlinx.android.synthetic.main.activity_yap_item.*

open class Search : BarcodeDataReceiver() {

    var oldx = 0F
    var oldMode:Global.Mode? = null
    private var currentLine:Int = 1

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
                        barcode = intent.getStringExtra("data")
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

    override fun onResume() {
        super.onResume()
        registerReceiver(barcodeDataReceiver, IntentFilter(ACTION_BARCODE_DATA))
        claimScanner()
        onWindowFocusChanged(true)
        Log.d("IntentApiSample: ", "onResume")
        if (ss.CurrentMode == Global.Mode.Waiting) {
            //значит вышлииз других страниц и надо просто обновить активити
            ss.CurrentMode == Global.Mode.Acceptance
            refreshActivity()
            return
        }
        if (scanRes != null) {
            try {
                barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(barcode)
            } catch (e: Exception) {
                val toast = Toast.makeText(
                    applicationContext,
                    "Ошибка! Возможно отсутствует соединение с базой!",
                    Toast.LENGTH_LONG
                )
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
        var iddoc: String = ""
        var consignmen: MutableList<MutableMap<String, String>> = mutableListOf()
        var noneAccItem : MutableList<MutableMap<String, String>> = mutableListOf()
        var acceptedItems: MutableList<MutableMap<String, String>> = mutableListOf()
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ss.CurrentMode in listOf(
                Global.Mode.AcceptanceNotAccepted,
                Global.Mode.AcceptanceAccepted
            )
        ) {
            return
        }
        setContentView(R.layout.activity_accept)
        title = ss.title
        FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                    val backAcc = Intent(this, YapItem::class.java)
                    startActivity(backAcc)
                } else if (event.x > oldx) {
                    val backAcc = Intent(this, NoneItem::class.java)
                    startActivity(backAcc)
                }
            }
            return true
        })

        if (ss.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@Search, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "Search")
                startActivity(scanAct)
            }
        }

        oldMode = ss.CurrentMode
        ss.CurrentMode = Global.Mode.Acceptance
        toModeAcceptance()

    }

    fun toModeAcceptance() {
        //если переход между экранами то просто обновим картинку
        if (oldMode == Global.Mode.Waiting){
            refreshActivity()
            return
        }
        //получаем задание
        getDocsAccept()

        //подтянем табличку накладных
        consign()
        //пробуем заблокировать
        for (dr in consignmen)
            if (!lockDocAccept(dr["ACID"].toString())) {
                consignmen.remove(dr)
            }
        iddoc = ""
        for (dr in consignmen)
            iddoc += ", '" + dr["ACID"].toString() + "'"
        if (iddoc.isNotEmpty()) {
            iddoc = iddoc.substring(2)   //Убираем спедери запятые
        }
        else {
            FExcStr.text = "Не удалось получить задание!"
            ss.excStr = "Не удалось получить задание!"
            val accMen = Intent(this, AccMenu::class.java)
            startActivity(accMen)
            finish()
            return
        }
        //подтянем не принятые товары
        noneItem()
        //тепреь принятые
        yapItem()
        //теперь обнговим информацию о не принятх позициях
        updateTableInfo()

        refreshActivity()
    }

    //подтягиваем данные для таблички
    private fun consign() {
        var textQuery = "SELECT " +
                "identity(int, 1, 1) as Number , " +
                "AC.iddoc as ACID , " +
                "journ.iddoc as ParentIDD , " +
                "Clients.descr as Client , " +
                "AC.\$АдресПоступление.КолСтрок as CountRow , " +
                "journ.docno as DocNo , " +
                "CAST(LEFT(journ.date_time_iddoc, 8) as datetime) as DateDoc , " +
                "CONVERT(char(8), CAST(LEFT(journ.date_time_iddoc,8) as datetime), 4) as DateDocText " +
                "into #temp " +
                "FROM" +
                " DH\$АдресПоступление as AC (nolock) " +
                "LEFT JOIN _1sjourn as journ (nolock) " +
                "     ON journ.iddoc = right(AC.\$АдресПоступление.ДокументОснование , 9) " +
                "LEFT JOIN DH\$ПриходнаяКредит as PK (nolock) " +
                "     ON journ.iddoc = PK.iddoc " +
                "LEFT JOIN \$Спр.Клиенты as Clients (nolock) " +
                "     ON PK.\$ПриходнаяКредит.Клиент = Clients.id " +
                "WHERE" +
                " AC.iddoc in ($iddoc) " +
                " select * from #temp " +
                " drop table #temp "

        consignmen = ss.executeWithReadNew(textQuery) ?: return
    }

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
        textQuery = ss.querySetParam(textQuery, "OKEIPackage", model.okeiPackage)
        textQuery = ss.querySetParam(textQuery, "Warehouse", ss.Const.mainWarehouse)
        noneAccItem = ss.executeWithReadNew(textQuery) ?: return
        //для удобного поиска подсосем сразу все цифры в названии и артикулах
        for (dr in noneAccItem){
            dr["ArticleFind"] = ss.helper.suckDigits(dr["Article"].toString())
            dr["ArticleOnPackFind"] = ss.helper.suckDigits(dr["ArticleOnPack"].toString())
            dr["ItemNameFind"] = ss.helper.suckDigits(dr["ItemName"].toString())
            dr["CoefView"] = if (dr["CoefView"].toString().trim() == "1") "?? " else "" + dr["Coef"].toString().trim()
        }


    }

    fun yapItem() {

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
        textQuery = ss.querySetParam(textQuery, "Warehouse", ss.Const.mainWarehouse)
        acceptedItems = ss.executeWithReadNew(textQuery) ?: return
    }

    fun updateTableInfo() {
        //обновим информацию по паллетам если она пустая
        if (ss.FPallets.isEmpty())
        {
            //Теперь принятые паллеты
            var textQuery =
                "SELECT " +
                        "Supply.\$АдресПоступление.Паллета as ID, " +
                        "min(Pallets.\$Спр.ПеремещенияПаллет.ШКПаллеты ) as Barcode, " +
                        "min(SUBSTRING(Pallets.\$Спр.ПеремещенияПаллет.ШКПаллеты ,8,4)) as Name, " +
                        "min(Pallets.\$Спр.ПеремещенияПаллет.Адрес0 ) as AdressID " +
                        "FROM DT\$АдресПоступление as Supply (nolock) " +
                        "INNER JOIN \$Спр.ПеремещенияПаллет as Pallets (nolock) " +
                        "ON Pallets.ID = Supply.\$АдресПоступление.Паллета " +
                        "WHERE Supply.IDDOC in ($iddoc) " +
                        "and Supply.\$АдресПоступление.Состояние0 = 1 " +
                        "and Supply.\$АдресПоступление.ФлагПечати = 1 " +
                        "and Supply.\$АдресПоступление.Сотрудник0 = :Employer " +
                        "GROUP BY " +
                        "Supply.\$АдресПоступление.Паллета " +
                        "ORDER BY " +
                        "Supply.\$АдресПоступление.Паллета ";
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id);
            val dt = ss.executeWithReadNew(textQuery) ?: return
            var palletID = ""
            for (dr in dt) {
                ss.FPallets.add(dr)
                palletID = dr["ID"].toString()
            }
            if (!ss.FPallet.foundID(palletID)) {
                ss.FPallet = RefPalleteMove()
            }
        }
        //Расчитываем строчечки
        for (dr in consignmen)
        {
            var countRow = 0;
            for (drNotAcc in noneAccItem) {
                if (drNotAcc["iddoc"].toString().trim() == dr["ACID"].toString().trim()) {
                    countRow ++
                }
            }
            dr["CountNotAcceptRow"] = countRow.toString()
        }
    }

    private fun getDocsAccept(): Boolean {
        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] =
            ss.extendID(ss.FEmployer.id, "Спр.Сотрудники")
        var dataMapRead: MutableMap<String, Any> = mutableMapOf()
        val fieldList: MutableList<String> = mutableListOf("Спр.СинхронизацияДанных.ДатаРез1")
        try {
            dataMapRead = execCommand("QuestAcceptance", dataMapWrite, fieldList, dataMapRead)
        } catch (e: Exception) {
            badVoise()
            val toast = Toast.makeText(
                applicationContext,
                "Не удалось получить задание!",
                Toast.LENGTH_SHORT
            )
            toast.show()
        }
        if ((dataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() == -3) {
            badVoise()
            FExcStr.text = "Заданий нет!"
            return false
        }
        if ((dataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() != 3) {
            badVoise()
            FExcStr.text = "Не известный ответ робота... я озадачен..."
            return false
        }
        iddoc = dataMapRead["Спр.СинхронизацияДанных.ДатаРез1"].toString()
        return true
    }

    open fun reactionBarcode(Barcode: String):Boolean {
        val helper = Helper()
        val barcoderes = helper.disassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if (typeBarcode == "113") {
            val idd = barcoderes["IDD"].toString()

            if (ss.isSC(idd, "Принтеры")) {
                if (!ss.FPrinter.foundIDD(idd)) {
                    return false
                }
                goodVoise()
                refreshActivity()
                return true
            }
            if (ss.isSC(idd, "Сотрудники")) {
                ss.FEmployer = RefEmployer()
                quitModeAcceptance()
                val mainInit = Intent(this, MainActivity::class.java)
                startActivity(mainInit)
                finish()
            } else if (ss.isSC(idd, "Секции")) {
                if (!ss.FPallet.selected) {
                    FExcStr.text = "Не выбрана паллета!"
                    badVoise()
                    return false
                }
                val sections = RefSection()
                sections.foundIDD(idd)

                var strPallets = ""
                for (dr in ss.FPallets) {
                    strPallets += ", '" + dr["ID"].toString() + "'"
                }
                if (strPallets.length > 2) {
                    strPallets = strPallets.substring(2)  //Убираем спедери запятые
                }

                var textQuery =
                    "UPDATE \$Спр.ПеремещенияПаллет SET \$Спр.ПеремещенияПаллет.Адрес0 = :ID, \$Спр.ПеремещенияПаллет.ФлагОперации = 2 WHERE \$Спр.ПеремещенияПаллет .id in ($strPallets)"
                textQuery = ss.querySetParam(textQuery, "ID", sections.id);
                if (!ss.executeWithoutRead(textQuery)) {
                    return false
                }
                //почистим табличку паллет от греха и почистим паллеты

                ss.FPallets = mutableListOf()
                ss.FPallet = RefPalleteMove()
                refreshActivity()
                return true
            } else {
                FExcStr.text = "Не верный тип справочника!"
                badVoise()
                return false
            }
        } else if (typeBarcode == "pallete") {
            if (scanPalletBarcode(Barcode)) {
                refreshActivity()
                goodVoise()
            } else {
                badVoise()
                return false
            }
            return true
        }
        else {
            FExcStr.text = "Не верный тип справочника!"
            badVoise()
            return false
        }
        return false
    }

    open fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == 4) {
            quitModeAcceptance()
            ss.excStr = "Выберите режим работы"
            val accMen = Intent(this, AccMenu::class.java)
            startActivity(accMen)
            finish()
            return true
        }

        if (ss.helper.whatDirection(keyCode) == "Left") {
            clickVoise()
            val backAcc = Intent(this, NoneItem::class.java)
            startActivity(backAcc)
            return true
        }

        if (ss.helper.whatDirection(keyCode) == "Right") {
            clickVoise()
            val backAcc = Intent(this, YapItem::class.java)
            startActivity(backAcc)
            return true
        }
        if (ss.helper.whatDirection(keyCode) in listOf("Down", "Up")) {

            table.getChildAt(currentLine).isFocusable = false
            table.getChildAt(currentLine).setBackgroundColor(Color.WHITE)
            if (ss.helper.whatDirection(keyCode) == "Down") {
                if (currentLine < consignmen.count()) {
                    currentLine++
                } else {
                    currentLine = 1
                }
            } else {
                if (currentLine > 1) {
                    currentLine--
                } else {
                    currentLine = consignmen.count()
                }

            }
            if (currentLine < 10) {
                scroll.fullScroll(View.FOCUS_UP)
            } else if (currentLine > consignmen.count() - 10) {
                scroll.fullScroll(View.FOCUS_DOWN)
            } else if (currentLine % 10 == 0) {
                scroll.scrollTo(0, 30 * currentLine - 1)
            }
            //теперь подкрасим строку серым
            table.getChildAt(currentLine).setBackgroundColor(Color.LTGRAY)
            table.getChildAt(currentLine).isActivated = false
            return true
        }

        if (keyCode == 67) {
            //делит удаляем текущую накладную
            //это делете, оотменим приемку если до этого двигались
            iddoc = iddoc.replace(consignmen[currentLine - 1]["ACID"].toString(), "")
            lockoutDocAccept(consignmen[currentLine - 1]["ACID"].toString())
            //подтянем табличку накладных
            consign()
            //подтянем не принятые товары
            noneItem()
            //тепреь принятые
            yapItem()
            //теперь обнговим информацию о не принятх позициях
            updateTableInfo()

            refreshActivity()
            return true
        }
        return false
    }

    fun scanPalletBarcode(strBarcode: String):Boolean {

        var textQuery =
            "declare @result char(9); exec WPM_GetIDNewPallet :Barcode, :Employer, @result out; select @result;"
        textQuery = ss.querySetParam(textQuery, "Barcode", strBarcode)
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        val palletID =  ss.executeScalar(textQuery) ?: return false
        ss.FPallet = RefPalleteMove()
        if (!ss.FPallet.foundID(palletID)) {
            return false
        }
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
        textQuery = ss.querySetParam(textQuery, "Pallet", ss.FPallet.id)
        if (!ss.executeWithoutRead(textQuery)) {
            return false
        }
        var findPallet = false
        for (dr in ss.FPallets) {
            if (dr["ID"].toString() == ss.FPallet.id) {
                findPallet = true
                break
            }
        }
        if (!findPallet) {
            val tmpdr:MutableMap<String,String> = mutableMapOf()
            tmpdr["ID"] = ss.FPallet.id
            tmpdr["Barcode"] = strBarcode;
            tmpdr["Name"] = strBarcode.substring(8,12)
            tmpdr["AdressID"] = ss.getVoidID()
            ss.FPallets.add(tmpdr)
        }
        return true
    }

    open fun refreshActivity() {

        if (ss.FPrinter.selected) {
            printPal.text = ss.FPrinter.path
        }
        else {
            printPal.text = "(принтер не выбран)"
        }
        if (ss.FPallet.selected) {
            palletPal.text = ss.FPallet.pallete
        }
        else {
            palletPal.text = "НЕТ ПАЛЛЕТЫ"
        }
        table.removeAllViewsInLayout()

        if (ss.CurrentMode in listOf(
                Global.Mode.AcceptanceNotAccepted,
                Global.Mode.AcceptanceAccepted
            )
        ) {
            return
        }

        var linearLayout = LinearLayout(this)
        val rowTitle = TableRow(this)

        //добавим столбцы
        var number = TextView(this)
        number.text = "№"
        number.typeface = Typeface.SERIF
        number.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.05).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        number.gravity = Gravity.CENTER
        number.textSize = 18F
        number.setTextColor(-0x1000000)
        var docum = TextView(this)
        docum.text = "Накл."
        docum.typeface = Typeface.SERIF
        docum.gravity = Gravity.CENTER
        docum.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.25).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        docum.textSize = 18F
        docum.setTextColor(-0x1000000)
        var datedoc = TextView(this)
        datedoc.text = "Дата"
        datedoc.typeface = Typeface.SERIF
        datedoc.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.22).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        datedoc.gravity = Gravity.CENTER
        datedoc.textSize = 18F
        datedoc.setTextColor(-0x1000000)
        var сountNotAcceptRow = TextView(this)
        сountNotAcceptRow.text = "Ост."
        сountNotAcceptRow.typeface = Typeface.SERIF
        сountNotAcceptRow.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.1).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        сountNotAcceptRow.gravity = Gravity.CENTER_HORIZONTAL
        сountNotAcceptRow.textSize = 18F
        сountNotAcceptRow.setTextColor(-0x1000000)
        var сlient = TextView(this)
        сlient.text = "Поставщик"
        сlient.typeface = Typeface.SERIF
        сlient.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.4).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        сlient.gravity = Gravity.CENTER
        сlient.textSize = 18F
        сlient.setTextColor(-0x1000000)

        linearLayout.addView(number)
        linearLayout.addView(docum)
        linearLayout.addView(datedoc)
        linearLayout.addView(сountNotAcceptRow)
        linearLayout.addView(сlient)

        rowTitle.addView(linearLayout)
        rowTitle.setBackgroundColor(Color.GRAY)
        table.addView(rowTitle)
        var lineNom = 0

        if (consignmen.isNotEmpty()) {

            for (DR in consignmen) {

                var linearLayout1 = LinearLayout(this)
                lineNom ++
                val rowTitle1 = TableRow(this)
                rowTitle1.isClickable = true
                rowTitle1.setOnTouchListener{ v, event ->  //выделение строки при таче
                    var i = 1
                    while (i < table.childCount) {
                        if (rowTitle1 != table.getChildAt(i)) {
                            table.getChildAt(i).setBackgroundColor(Color.WHITE)
                        } else {
                            currentLine = i
                            rowTitle1.setBackgroundColor(Color.LTGRAY)
                            kolEtik.text = consignmen[currentLine - 1]["LabelCount"].toString()
                        }
                        i++
                    }
                    true
                }

                //добавим столбцы
                number = TextView(this)
                number.text = DR["Number"]
                number.typeface = Typeface.SERIF
                number.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.05).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                number.gravity = Gravity.CENTER
                number.textSize = 18F
                number.setTextColor(-0x1000000)

                docum = TextView(this)
                docum.text = DR["DocNo"]
                docum.typeface = Typeface.SERIF
                docum.gravity = Gravity.CENTER
                docum.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.25).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                docum.textSize = 16F
                docum.setTextColor(-0x1000000)

                datedoc = TextView(this)
                //addr.text = DR["DateDoc"]
                datedoc.text = DR["DateDocText"].toString()
                datedoc.typeface = Typeface.SERIF
                datedoc.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.22).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                datedoc.gravity = Gravity.CENTER
                datedoc.textSize = 18F
                datedoc.setTextColor(-0x1000000)

                сountNotAcceptRow = TextView(this)
                сountNotAcceptRow.text = DR["CountNotAcceptRow"]
                сountNotAcceptRow.typeface = Typeface.SERIF
                сountNotAcceptRow.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.1).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                сountNotAcceptRow.gravity = Gravity.CENTER
                сountNotAcceptRow.textSize = 18F
                сountNotAcceptRow.setTextColor(-0x1000000)

                сlient = TextView(this)
                сlient.text = DR["Client"].toString().trim()
                сlient.typeface = Typeface.SERIF
                сlient.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.4).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                сlient.gravity = Gravity.CENTER
                сlient.textSize = 18F
                сlient.setTextColor(-0x1000000)

                linearLayout1.addView(number)
                linearLayout1.addView(docum)
                linearLayout1.addView(datedoc)
                linearLayout1.addView(сountNotAcceptRow)
                linearLayout1.addView(сlient)

                rowTitle1.addView(linearLayout1)
                var colorline =  Color.WHITE
                if (lineNom == currentLine) {
                    colorline = Color.LTGRAY
                }
                rowTitle1.setBackgroundColor(colorline)
                table.addView(rowTitle1)
            }
        }
    }

    fun quitModeAcceptance() {
        for (dr in iddoc.split(",")) {
            lockoutDocAccept(dr.replace("'", "").toString())
        }
    }

}

