package com.intek.wpma.ChoiseWork.Accept


import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import com.intek.wpma.Model.Model
import com.intek.wpma.R
import com.intek.wpma.Ref.RefItem
import kotlinx.android.synthetic.main.activity_search_acc.*


class ItemCard : Search() {

    var currentRowAcceptedItem: MutableMap<String, String> = mutableMapOf()
    var AcceptedItem: MutableMap<String, String> = mutableMapOf()
    var units: MutableList<MutableMap<String, String>> = mutableListOf()
    var idDoc = ""
    var allCount = 0
    var item = RefItem()
    var bufferWarehouse = "" //переменка для товара на главном
    var flagBarcode = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_acc)

        flagBarcode = intent.extras!!.getString("flagBarcode")!!
        title = ss.title
        item.foundID(intent.extras!!.getString("itemID")!!)
        idDoc = intent.extras!!.getString("iddoc")!!

        btnPrinItem.setOnClickListener {
            //обработчик события при нажатии на кнопку принятия товара
        }
        //
        if (!loadUnits())
        {
            //облом выходим обратно
            val backH = Intent(this, NoneItem::class.java)
            startActivity(backH)
            finish()
            return
        }
        //БЛОКИРУЕМ ТОВАР
        if (!lockItem(item.id))
        {
            //облом выходим обратно
            val backH = Intent(this, NoneItem::class.java)
            startActivity(backH)
            finish()
            return
        }

        toModeAcceptedItem()

    }

    fun toModeAcceptedItem() {
        //Подтянем данные о не принятой позиции
        getCurrentRowAcceptedItem()
        //подтянем склад буфер
        buffWare()
        //подтянем остатки
        getCountAdress()
        //тепреь все остальное
        refreshItemProper()
        //рефрешим активити
        refreshActivity()
    }
    //тут тянем инфу о товаре и его местоположение и кол-во на складе, если таковое есть
    //иначе адрес не задан и кол-во равно 0
    private fun getCountAdress() {
        var textQuery =
            "DECLARE @curdate DateTime; " +
            "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock); " +
            "SELECT " +
                "CAST(sum(CASE WHEN Main.Warehouse = :MainWarehouse THEN Main.Balance ELSE 0 END) as int) as BalanceMain, " +
                "CAST(sum(CASE WHEN Main.Warehouse = :BufferWarehouse THEN Main.Balance ELSE 0 END) as int) as BalanceBuffer, " +
                "ISNULL((" +
                    "SELECT top 1 " +
                        "Section.descr " +
                    "FROM _1sconst as Const (nolock) " +
                        "LEFT JOIN \$Спр.Секции as Section (nolock) " +
                            "ON Section.id = left(Const.value, 9) " +
                "WHERE " +
                    "Const.id = \$Спр.ТоварныеСекции.Секция " +
                    "and Const.date <= :NowDate " +
                    "and Const.OBJID in (" +
                        "SELECT id FROM \$Спр.ТоварныеСекции " +
                        "WHERE " +
                            "\$Спр.ТоварныеСекции.Склад = :MainWarehouse " +
                            "and parentext = :Item)" +
                "ORDER BY " +
                    "Const.date DESC, Const.time DESC, Const.docid DESC), '<не задан>') as AdressMain, " +
            "ISNULL((" +
                "SELECT top 1 " +
                    "Section.descr " +
                "FROM _1sconst as Const (nolock) " +
                    "LEFT JOIN \$Спр.Секции as Section (nolock) " +
                        "ON Section.id = left(Const.value, 9) " +
                "WHERE " +
                    "Const.id = \$Спр.ТоварныеСекции.Секция " +
                    "and Const.date <= :NowDate " +
                    "and Const.OBJID in (" +
                        "SELECT id FROM \$Спр.ТоварныеСекции " +
                        "WHERE " +
                            "\$Спр.ТоварныеСекции.Склад = :BufferWarehouse " +
                            "and parentext = :Item)" +
                "ORDER BY " +
                    "Const.date DESC, Const.time DESC, Const.docid DESC), '<не задан>') as AdressBuffer " +
            "FROM (" +
                    "SELECT " +
                        "\$Рег.ОстаткиТоваров.Склад as Warehouse, " +
                        "\$Рег.ОстаткиТоваров.Товар as Item, " +
                    "\$Рег.ОстаткиТоваров.ОстатокТовара as Balance " +
                "FROM " +
                    "RG\$Рег.ОстаткиТоваров (nolock) " +
                "WHERE " +
                    "period = @curdate "+
                    "and \$Рег.ОстаткиТоваров.Товар = :Item " +
                    "and \$Рег.ОстаткиТоваров.Склад in (:MainWarehouse, :BufferWarehouse) " +
                "UNION ALL " +
                "SELECT " +
                    ":MainWarehouse, :Item, 0 "+
                ") as Main "+
            "GROUP BY Main.Item"
        textQuery = ss.querySetParam(textQuery, "Item", item.id)
        textQuery = ss.querySetParam(textQuery, "BufferWarehouse", bufferWarehouse)
        textQuery = ss.querySetParam(textQuery, "MainWarehouse", ss.Const.mainWarehouse)
        val datTab = ss.executeWithReadNew(textQuery) ?: return
        if (datTab.isNotEmpty()) {
            AcceptedItem["BalanceMain"] = datTab[0]["BalanceMain"].toString()
            AcceptedItem["BalanceBuffer"] = datTab[0]["BalanceBuffer"].toString()
            AcceptedItem["AdressMain"] = datTab[0]["AdressMain"].toString()
            AcceptedItem["AdressBuffer"] = datTab[0]["AdressBuffer"].toString()
            AcceptedItem["IsRepeat"] = "0"
        }
    }

    private fun getCurrentRowAcceptedItem(){

        for (dr in noneAccItem) {
            if (dr["ID"] != item.id) {
                continue
            }
           if (dr["IDDOC"].toString() == idDoc && currentRowAcceptedItem.isEmpty()) {
               currentRowAcceptedItem = dr
           }
            allCount += dr["Count"].toString().toInt()
        }
    }

    //это приходный адрес, нужен для того, чтобы нормально подтягивалось кол-во товара, который нам надо принять
    private fun buffWare() {
        val textQuery = "SELECT VALUE as val FROM _1sconst (nolock) " +
                "WHERE ID = \$Константа.ОснЦентрСклад "
        val datTabl = ss.executeWithReadNew(textQuery) ?: return
        if (datTabl.isNotEmpty()) for (DR in datTabl) bufferWarehouse = DR["val"].toString()
    }

    //подтягиваем все остальное
    private fun refreshItemProper() {

        AcceptedItem["ID"] = currentRowAcceptedItem["ID"].toString()
        AcceptedItem["Name"] = currentRowAcceptedItem["ItemName"].toString()
        AcceptedItem["InvCode"] = currentRowAcceptedItem["InvCode"].toString()
        AcceptedItem["Acticle"] = currentRowAcceptedItem["Article"].toString()
        AcceptedItem["Count"] = currentRowAcceptedItem["Count"].toString()
        AcceptedItem["Price"] = currentRowAcceptedItem["Price"].toString()
        AcceptedItem["Acceptance"] = "1"
        AcceptedItem["Details"] = currentRowAcceptedItem["Details"].toString()
        AcceptedItem["NowDetails"] = AcceptedItem["Details"].toString()
        AcceptedItem["ToMode"] = "Acceptance"
        AcceptedItem["BindingAdressFlag"] = "0"
        AcceptedItem["SeasonGroup"] = currentRowAcceptedItem["SeasonGroup"].toString()
        AcceptedItem["FlagFarWarehouse"] = currentRowAcceptedItem["FlagFarWarehouse"].toString()
        AcceptedItem["StoregeSize"] = currentRowAcceptedItem["StoregeSize"].toString()

        //begin internal command
        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ss.extendID(ss.FEmployer.id, "Спр.Сотрудники")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход2"] = ss.extendID(item.id, "Спр.Товары")
        dataMapWrite["Спр.СинхронизацияДанных.ДокументВход"] = ss.extendID(idDoc, "АдресПеремещение")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = "OpenItemAccept (Открыл карточку для приемки)"
        execCommandNoFeedback("Internal", dataMapWrite)

    }

    //столбик ШК
    private fun loadUnits(): Boolean {
        //Загружает единицы товара в таблицу FUnits
        var textQuery = "SELECT Units.id as ID , " +
                "CAST(Units.\$Спр.ЕдиницыШК.Коэффициент as int) as Coef , " +
                "Units.\$Спр.ЕдиницыШК.Штрихкод as Barcode , " +
                "Units.\$Спр.ЕдиницыШК.ОКЕИ as OKEI " +
                "FROM " +
                "\$Спр.ЕдиницыШК as Units (nolock) " +
                "WHERE " +
                "Units.parentext = :CurrentItem and Units.ismark = 0 "
        textQuery = ss.querySetParam(textQuery, "CurrentItem", item.id)
        val units = ss.executeWithReadNew(textQuery) ?: return false

        return true
    }

    //у нас давно все отрисованно, поэтому просто подтягиваем данные по товару дальше
    override fun refreshActivity() {
        //все подсчеты в reactionKey
        numVod1.text = "0"
        numVod2.text = "0"
        numVod3.text = "0"
        wtfVod1.text = "0"
        wtfVod2.text = "0"
        wtfVod3.text = "0"
        val model = Model()
        var excStr = ""
        for (dr in acceptedItems){
            if (dr["ID"] == item.id && dr["IDDOC"] == idDoc) {
                excStr = "ПОВТОРНАЯ приемка!!! "
                break
            }
        }
        //Добавляем что принимается не все
        if (allCount > AcceptedItem["Count"].toString().toInt())
        {
            var coef = 1
            for (dr in units) {
                if (dr["OKEI"] == model.okeiPackage) {
                    coef = dr["Coef"].toString().toInt()
                    break
                }
            }
            excStr += " " + model.getStrPackageCount(AcceptedItem["Count"].toString().toInt(), coef) + " из " + model.getStrPackageCount(allCount, coef)
        }

        //заполнение шапки карточки товара
        zonaHand.text = (AcceptedItem["AdressBuffer"].toString().trim() +
                ": " + AcceptedItem["BalanceBuffer"].toString().trim() +
                " шт")                  //зона где товар есть
        /* zonaTech.text = (DR["AdressMain"].toString().trim() +
                ": " + DR["BalanceMain"].toString().trim() +
                " шт")                  //зона куда товар будут запихивать, изначально не задан
                */


        storageSize.text =
            AcceptedItem["BalanceBuffer"]                                              //кол-во товара дома как я понял

        if (units.isNotEmpty())
            for (dr in units)
                baseSHK.text = dr["Barcode"]                       //штрих-код товара
       //minParty.text = DR["BalanceBuffer"].toString().trim()
                shapka.text =
                    (AcceptedItem["InvCode"].toString() + "Приемка товара")                         //код товара
                itemName.text =
                    AcceptedItem["ItemName"]                                                      //полное наименование товара
                details.text =
                    AcceptedItem["Details"]                                                        //кол-во деталей товара, подтягиваем если есть, если нет заполняем, иначе 0
                pricePrih.text =
                    ("Цена: " + AcceptedItem["Price"].toString())                                //цена товара
                resOne.text = (
                        statVod1.text.toString().toInt() * minParty.text.toString().toInt()
                        ).toString()                                                                //сумма принимаемого товара в общей сложности

                //определяем, как был найден товар, перед тем, как зайти в карточку
                when (flagBarcode) {
                    "0" -> {
                        excStr += (AcceptedItem["InvCode"].toString() +
                                "найден в ручную!")
                    }
                    "1" -> {
                        excStr += (AcceptedItem["InvCode"].toString() +
                                "найден по штрихкоду!")
                    }
                    //пока не требуется, но пусть будет
                    /*   "2" -> {
                        FExcStr.text = (DR["InvCode"].toString() +
                                "найден по ШК МЕСТА!")
                        FExcStr.setTextColor(Color.RED)
                    } */
                }
            }

    override fun reactionBarcode(Barcode: String): Boolean {

        return true
    }

    override fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        if (ss.helper.whatDirection(keyCode) in listOf("Left", "Right", "Up", "Down")) clickVoise()

        //вроде все как надо, но почему-то выдает ошибку при нажатии
        if (keyCode == 4) {
            val backH = Intent(this, NoneItem::class.java)
            backH.putExtra("ParentForm", "ItemCard")
            startActivity(backH)
            finish()
            return true
        }

        val num1 = numVod1.text.toString().toInt()
        val num2 = numVod2.text.toString().toInt()
        val num3 = numVod3.text.toString().toInt()

        //при нажатии на кнопки идет полный пересчет чисел в табличке
        //substring нужен для удаления нуля, который висит в таблице изначально
        //без изначальных значений (с пустыми значениями) в ячейках крякает приложение при нажатии на стрелочки
        when (ss.helper.whatDirection(keyCode) in listOf("Left", "Right", "Up", "Down")) {
            num1 > 0 -> {
                clickVoise()
                wtfVod1.text = (
                        minParty.text.toString().toInt() / numVod1.text.toString().toInt()
                        ).toString().substring(0, -1)
                resTwo.text = (
                        minParty.text.toString().toInt() / numVod1.text.toString().toInt()
                        ).toString()
            }
            num2 > 0 -> {
                clickVoise()
                wtfVod2.text = (
                        minParty.text.toString().toInt() / numVod2.text.toString().toInt()
                        ).toString().substring(0, -1)
                resThird.text = (
                        minParty.text.toString().toInt() / numVod2.text.toString().toInt()
                        ).toString()
            }
            num3 > 0 -> {
                clickVoise()
                wtfVod3.text = (
                        minParty.text.toString().toInt() / numVod3.text.toString().toInt()
                        ).toString().substring(0, -1)
                resFor.text = (
                        minParty.text.toString().toInt() / numVod3.text.toString().toInt()
                        ).toString()
            }
        }
        /*  minParty.text = (
                        (numVod2.text.toString().toInt()) * wtfVod2.text.toString().toInt()
                        ).toString().substring(0,-1)
                minParty.text = (
                        (numVod1.text.toString().toInt()) * wtfVod1.text.toString().toInt()
                        ).toString().substring(0,-1)
                minParty.text = (
                        (numVod3.text.toString().toInt()) * wtfVod3.text.toString().toInt()
                        ).toString().substring(0,-1) */
        return false
    }

    //тотальная ебань
    //класс для обработки события при нажатии на кнопку о принятии товара
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
    } */
}