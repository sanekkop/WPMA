package com.intek.wpma.ChoiseWork.Accept


import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import com.intek.wpma.Helpers.Helper
import com.intek.wpma.MainActivity
import com.intek.wpma.Model.Model
import com.intek.wpma.R
import com.intek.wpma.Ref.RefEmployer
import com.intek.wpma.Ref.RefItem
import kotlinx.android.synthetic.main.activity_search_acc.*


class ItemCard : Search() {

    var currentRowAcceptedItem: MutableMap<String, String> = mutableMapOf()
    var AcceptedItem: MutableMap<String, String> = mutableMapOf()
    var units: MutableList<MutableMap<String, String>> = mutableListOf()
    var fUnits: MutableList<MutableMap<String, String>> = mutableListOf()
    val newBarcodes: MutableList<String> = mutableListOf()
    var idDoc = ""
    var allCount = 0
    val model = Model()
    var item = RefItem()
    var bufferWarehouse = "" //переменка для товара на главном
    var flagBarcode = ""
    var isMoveButon = true
    var xCoor = 2
    var yCoor = 4

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_acc)

        flagBarcode = intent.extras!!.getString("flagBarcode")!!
        title = ss.title
        item.foundID(intent.extras!!.getString("itemID")!!)
        idDoc = intent.extras!!.getString("iddoc")!!

        btnPrinItem.setOnClickListener {
            //обработчик события при нажатии на кнопку принятия товара
            if (completeAccept()) {
                noneItem()
                yapItem()
                updateTableInfo()
                val backH = Intent(this, NoneItem::class.java)
                backH.putExtra("ParentForm", "ItemCard")
                backH.putExtra("flagBarcode", flagBarcode)
                backH.putExtra("itemID", item.id)
                backH.putExtra("iddoc", idDoc)
                startActivity(backH)
                finish()
            }
        }
        btnMark.setOnClickListener {
            //обработчик события при нажатии на кнопку принятия товара
           val backH = Intent(this, AccMark::class.java)
            backH.putExtra("ParentForm", "ItemCard")
            backH.putExtra("flagBarcode", flagBarcode)
            backH.putExtra("itemID", item.id)
            backH.putExtra("iddoc", idDoc)
            startActivity(backH)
            finish()
        }
        //
        if (!loadUnits()) {
            //облом выходим обратно
            val backH = Intent(this, NoneItem::class.java)
            startActivity(backH)
            finish()
            return
        }
        //БЛОКИРУЕМ ТОВАР
        if (!lockItem(item.id)) {
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
                    "period = @curdate " +
                    "and \$Рег.ОстаткиТоваров.Товар = :Item " +
                    "and \$Рег.ОстаткиТоваров.Склад in (:MainWarehouse, :BufferWarehouse) " +
                    "UNION ALL " +
                    "SELECT " +
                    ":MainWarehouse, :Item, 0 " +
                    ") as Main " +
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

    private fun getCurrentRowAcceptedItem() {

        for (dr in noneAccItem) {
            if (dr["id"] != item.id) {
                continue
            }
            if (dr["iddoc"].toString() == idDoc && currentRowAcceptedItem.isEmpty()) {
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

        if (currentRowAcceptedItem.isNotEmpty()) {

            AcceptedItem["id"] = currentRowAcceptedItem["id"].toString()
            AcceptedItem["Name"] = currentRowAcceptedItem["ItemName"].toString()
            AcceptedItem["InvCode"] = currentRowAcceptedItem["InvCode"].toString()
            AcceptedItem["Acticle"] = currentRowAcceptedItem["Article"].toString()
            AcceptedItem["Count"] = currentRowAcceptedItem["Count"].toString()
            AcceptedItem["AcceptCount"] = currentRowAcceptedItem["Count"].toString()
            AcceptedItem["Price"] = currentRowAcceptedItem["Price"].toString()
            AcceptedItem["Acceptance"] = "1"
            AcceptedItem["Details"] = currentRowAcceptedItem["Details"].toString()
            AcceptedItem["NowDetails"] = AcceptedItem["Details"].toString()
            AcceptedItem["ToMode"] = "Acceptance"
            AcceptedItem["BindingAdressFlag"] = "0"
            AcceptedItem["SeasonGroup"] = currentRowAcceptedItem["SeasonGroup"].toString()
            AcceptedItem["FlagFarWarehouse"] = currentRowAcceptedItem["FlagFarWarehouse"].toString()
            AcceptedItem["StoregeSize"] = currentRowAcceptedItem["StoregeSize"].toString()
            AcceptedItem["BaseUnitID"] = item.getAttribute("БазоваяЕдиницаШК").toString()
            AcceptedItem["MinParty"] = item.getAttribute("МинПартия").toString()
        } else {
            AcceptedItem["id"] = item.id
            AcceptedItem["Name"] = item.name
            AcceptedItem["InvCode"] = item.invCode
            AcceptedItem["Acticle"] = item.getAttribute("Артикул").toString()
            AcceptedItem["Count"] = "0"
            AcceptedItem["AcceptCount"] = "0"
            AcceptedItem["Price"] = item.getAttribute("Прих_Цена").toString()
            AcceptedItem["Acceptance"] = "1"
            AcceptedItem["Details"] = item.getAttribute("КоличествоДеталей").toString()
            AcceptedItem["NowDetails"] = AcceptedItem["Details"].toString()
            AcceptedItem["ToMode"] = "Acceptance"
            AcceptedItem["BindingAdressFlag"] = "0"
            AcceptedItem["SeasonGroup"] = ""
            AcceptedItem["FlagFarWarehouse"] = ""
            AcceptedItem["StoregeSize"] = "0"
            AcceptedItem["BaseUnitID"] = item.getAttribute("БазоваяЕдиницаШК").toString()
            AcceptedItem["MinParty"] = item.getAttribute("МинПартия").toString()
        }

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
        units = ss.executeWithReadNew(textQuery) ?: return false

        return true
    }

    //у нас давно все отрисованно, поэтому просто подтягиваем данные по товару дальше
    override fun refreshActivity() {
        //все подсчеты в reactionKey
        tbCoef1.text = "0"
        tbCoef2.text = "0"
        tbCoef3.text = "0"
        tbCount1.text = "0"
        tbCount2.text = "0"
        tbCount3.text = "0"
        var excStr = ""
        for (dr in acceptedItems) {
            if (dr["id"] == item.id && dr["iddoc"] == idDoc) {
                excStr = "ПОВТОРНАЯ приемка!!! "
                break
            }
        }
        //Добавляем что принимается не все
        var coef = 1
        if (allCount > AcceptedItem["AcceptCount"].toString().toInt()) {
            for (dr in units) {
                if (dr["OKEI"] == model.okeiPackage) {
                    coef = dr["Coef"].toString().toInt()
                    break
                }
            }
            excStr += " " + model.getStrPackageCount(
                AcceptedItem["AcceptCount"].toString().toInt(),
                coef
            ) + " из " + model.getStrPackageCount(allCount, coef)
        }

        storageSize.text =
            AcceptedItem["StoregeSize"]                                              //размер хранения
        if (AcceptedItem["StoregeSize"].toString().toFloat() > 0) {
            storageSize.visibility = View.VISIBLE
        } else {
            storageSize.visibility = View.INVISIBLE
        }
        itemName.text =
            AcceptedItem["Name"]                                                      //полное наименование товара
        details.text =
            AcceptedItem["Details"]                                                        //кол-во деталей товара, подтягиваем если есть, если нет заполняем, иначе 0
        shapka.text =
            (AcceptedItem["InvCode"].toString() + "Приемка товара")                         //код товара
        pricePrih.text =
            ("Цена: " + AcceptedItem["Price"].toString())                                //цена товара
        tbCount0.text = AcceptedItem["AcceptCount"].toString()
        tbCoef0.text = "1"
        zonaHand.text = (AcceptedItem["AdressMain"].toString()
            .trim() + ": " + AcceptedItem["BalanceMain"].toString() + " шт")
        zonaTech.text = (AcceptedItem["AdressBuffer"].toString()
            .trim() + ": " + AcceptedItem["BalanceBuffer"].toString() + " шт")
        if (units.isNotEmpty()) {
            for (dr in units) {
                var findUnits = false
                for (fdr in fUnits) {
                    if (fdr["ID"] == dr["ID"]) {
                        findUnits = true
                        break
                    }
                }
                if (!findUnits) {
                    //не нашли в табличке единицу, добавим
                    fUnits.add(dr)
                }
            }
        }
        if (fUnits.isNotEmpty()) {
            tbBarcode0.text = ""
            tbBarcode1.text = ""
            tbBarcode2.text = ""
            tbBarcode3.text = ""
            for (dr in fUnits) {
                when (dr["OKEI"]) {
                    model.okeiUnit -> {
                        if (dr["Barcode"].toString().trim() != "") {
                            tbBarcode0.text =
                                (tbBarcode0.text.toString() + " " + dr["Barcode"].toString().trim())
                        }
                    }
                    model.okeiPack -> {
                        tbCount1.text =
                            (AcceptedItem["AcceptCount"].toString().toInt() / dr["Coef"].toString()
                                .toInt()).toString()
                        if (dr["Barcode"].toString().trim() != "") {
                            tbBarcode1.text =
                                (tbBarcode1.text.toString().trim() + " " + dr["Barcode"].toString()
                                    .trim())
                        }
                        tbCoef1.text = dr["Coef"].toString()
                    }
                    model.okeiPackage -> {
                        tbCount2.text =
                            (AcceptedItem["AcceptCount"].toString().toInt() / dr["Coef"].toString()
                                .toInt()).toString()
                        if (dr["Barcode"].toString().trim() != "") {
                            tbBarcode2.text =
                                (tbBarcode2.text.toString().trim() + " " + dr["Barcode"].toString()
                                    .trim())
                        }
                        tbCoef2.text = dr["Coef"].toString()
                    }
                    model.okeiKit -> {
                        tbCount3.text =
                            (AcceptedItem["AcceptCount"].toString().toInt() / dr["Coef"].toString()
                                .toInt()).toString()
                        if (dr["Barcode"].toString().trim() != "") {
                            tbBarcode3.text =
                                (tbBarcode3.text.toString().trim() + " " + dr["Barcode"].toString()
                                    .trim())
                        }
                        tbCoef3.text = dr["Coef"].toString()
                    }
                }
            }
        }

        //обновим количество необходимое для приемки
        tbRes0.text = AcceptedItem["Count"].toString() //сумма принимаемого товара в общей сложности
        tbRes1.text = if (tbCoef1.text.toString().toInt() > 0) (AcceptedItem["Count"].toString()
            .toInt() / tbCoef1.text.toString().toInt()).toString() else "0"
        tbRes2.text = if (tbCoef2.text.toString().toInt() > 0) (AcceptedItem["Count"].toString()
            .toInt() / tbCoef2.text.toString().toInt()).toString() else "0"
        tbRes3.text = if (tbCoef3.text.toString().toInt() > 0) (AcceptedItem["Count"].toString()
            .toInt() / tbCoef3.text.toString().toInt()).toString() else "0"

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
        if (tbCoef2.text == "0" && yCoor == 4) xCoor = 1 else xCoor = 2

        refreshActivElement()
        FExcStr.text = excStr
    }

    fun refreshActivElement() {
        //для начала обнулим все цвета
        tbCoef0.setBackgroundColor(Color.WHITE)
        tbCoef1.setBackgroundColor(Color.WHITE)
        tbCoef2.setBackgroundColor(Color.WHITE)
        tbCoef3.setBackgroundColor(Color.WHITE)
        tbCount0.setBackgroundColor(Color.WHITE)
        tbCount1.setBackgroundColor(Color.WHITE)
        tbCount2.setBackgroundColor(Color.WHITE)
        tbCount3.setBackgroundColor(Color.WHITE)
        details.setBackgroundColor(Color.WHITE)
        tbBarcode0.setBackgroundColor(Color.LTGRAY)
        tbBarcode1.setBackgroundColor(Color.LTGRAY)
        tbBarcode2.setBackgroundColor(Color.LTGRAY)
        tbBarcode3.setBackgroundColor(Color.LTGRAY)
        when (yCoor) {
            1 -> details.setBackgroundColor(Color.LTGRAY)
            2 -> {
                if (xCoor == 1) tbCoef0.setBackgroundColor(Color.LTGRAY) //тут он не может ничего менять
                else {
                    tbCount0.setBackgroundColor(Color.LTGRAY)
                }
                tbBarcode0.setBackgroundColor(Color.BLUE)
            }
            3 -> {
                if (xCoor == 1) tbCoef1.setBackgroundColor(Color.LTGRAY)
                else {
                    tbCount1.setBackgroundColor(Color.LTGRAY)
                    //поменялось количесто меняем количество в основной
                    tbCount0.text =
                        if (tbCoef1.text.toString().toInt() > 0 && tbCoef1.text.toString().toInt() <= tbCount0.text.toString().toInt()) (tbCount1.text.toString()
                            .toInt() * tbCoef1.text.toString()
                            .toInt()).toString() else tbCount0.text
                }
                tbBarcode1.setBackgroundColor(Color.BLUE)
            }
            4 -> {
                if (xCoor == 1) tbCoef2.setBackgroundColor(Color.LTGRAY)
                else {
                    tbCount2.setBackgroundColor(Color.LTGRAY)
                    //поменялось количесто меняем количество в основной
                    tbCount0.text =
                        if (tbCoef2.text.toString().toInt() > 0 && tbCoef2.text.toString().toInt() <= tbCount0.text.toString().toInt()) (tbCount2.text.toString()
                            .toInt() * tbCoef2.text.toString()
                            .toInt()).toString() else tbCount0.text
                }
                tbBarcode2.setBackgroundColor(Color.BLUE)
            }
            5 -> {
                if (xCoor == 1) tbCoef3.setBackgroundColor(Color.LTGRAY)
                else {
                    tbCount3.setBackgroundColor(Color.LTGRAY)
                    //поменялось количесто меняем количество в основной
                    tbCount0.text =
                        if (tbCoef3.text.toString().toInt() > 0 && tbCoef3.text.toString().toInt() <= tbCount0.text.toString().toInt()) (tbCount3.text.toString()
                            .toInt() * tbCoef3.text.toString()
                            .toInt()).toString() else tbCount0.text
                }
                tbBarcode3.setBackgroundColor(Color.BLUE)
            }
        }
        //меняем все остальные значения исходя из новых значений
        tbCount1.text = if (tbCoef1.text.toString().toInt() > 0) (tbCount0.text.toString()
            .toInt() / tbCoef1.text.toString().toInt()).toString() else tbCount1.text
        tbCount2.text = if (tbCoef2.text.toString().toInt() > 0) (tbCount0.text.toString()
            .toInt() / tbCoef2.text.toString().toInt()).toString() else tbCount2.text
        tbCount3.text = if (tbCoef3.text.toString().toInt() > 0) (tbCount0.text.toString()
            .toInt() / tbCoef3.text.toString().toInt()).toString() else tbCount3.text
        tbRes1.text = if (tbCoef1.text.toString().toInt() > 0) (tbRes0.text.toString()
            .toInt() / tbCoef1.text.toString().toInt()).toString() else tbRes1.text
        tbRes2.text = if (tbCoef2.text.toString().toInt() > 0) (tbRes0.text.toString()
            .toInt() / tbCoef2.text.toString().toInt()).toString() else tbRes2.text
        tbRes3.text = if (tbCoef3.text.toString().toInt() > 0) (tbRes0.text.toString()
            .toInt() / tbCoef3.text.toString().toInt()).toString() else tbRes3.text
        //проверим а начело разделилось ли
        tbCount1.setTextColor(Color.BLACK)
        tbCount2.setTextColor(Color.BLACK)
        tbCount3.setTextColor(Color.BLACK)
        //не равны, значит будет у нас красной цифра
        if (tbCount1.text.toString().toInt() * tbCoef1.text.toString()
                .toInt() != tbCount0.text.toString().toInt() && tbCount1.text.toString()
                .toInt() * tbCoef1.text.toString().toInt() > 0
        ) tbCount1.setTextColor(Color.RED)
        if (tbCount2.text.toString().toInt() * tbCoef2.text.toString()
                .toInt() != tbCount0.text.toString().toInt() && tbCount2.text.toString()
                .toInt() * tbCoef2.text.toString().toInt() > 0
        ) tbCount2.setTextColor(Color.RED)
        if (tbCount3.text.toString().toInt() * tbCoef3.text.toString()
                .toInt() != tbCount0.text.toString().toInt() && tbCount3.text.toString()
                .toInt() * tbCoef3.text.toString().toInt() > 0
        ) tbCount3.setTextColor(Color.RED)
        AcceptedItem["AcceptCount"] = tbCount0.text.toString()
        AcceptedItem["NowDetails"] = details.text.toString()
    }

    override fun reactionBarcode(Barcode: String): Boolean {

        //для начала проверим этот ШК, может он уже у кого-то есть
        val itemTemp = RefItem()
        if (itemTemp.foundBarcode(Barcode) == true) {
            if (itemTemp.id != item.id) {
                badVoise()
                FExcStr.text = "Штрих-код от другой позиции " + itemTemp.invCode
                return false
            }
        }
        val helper = Helper()
        val barcoderes = helper.disassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if (typeBarcode == "113") {
            val idd = barcoderes["IDD"].toString()
            if (ss.isSC(idd, "Принтеры")||ss.isSC(idd, "Сотрудники")||ss.isSC(idd, "Секции")) {
                return super.reactionBarcode(Barcode)
            }
        }
        //не опознанный ШК, скорее всего позиция запомним ее
        return changeUnitBarcode(Barcode)
    }

    override fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        if (ss.helper.whatDirection(keyCode) in listOf("Left", "Right", "Up", "Down")) {
            val thisDirection = ss.helper.whatDirection(keyCode)
            clickVoise()
            when (thisDirection) {
                "Right" -> xCoor = if (xCoor == 2) 1 else 2
                "Left" -> xCoor = if (xCoor == 1) 2 else 1
                "Up" -> yCoor = if (yCoor == 1) 5 else yCoor - 1
                "Down" -> yCoor = if (yCoor == 5) 1 else yCoor + 1
            }
            isMoveButon = true
        }

        //вроде все как надо, но почему-то выдает ошибку при нажатии
        if (keyCode == 4) {
            //разблокируем обратно
            if (!ibsLockuot("int_ref_Товары_" + (item.id + "_unit"))) {
                //если не удалось разблокировать то просто напишем об этом
                FExcStr.text = "Не удалось разблокировать товар!"
            }
            val backH = Intent(this, NoneItem::class.java)
            backH.putExtra("ParentForm", "ItemCard")
            startActivity(backH)
            finish()
            return true
        }

        if (keyCode == 69) {
            // -  переход в режим маркирвоки
            val backH = Intent(this, AccMark::class.java)
            backH.putExtra("ParentForm", "ItemCard")
            backH.putExtra("flagBarcode", flagBarcode)
            backH.putExtra("itemID", item.id)
            backH.putExtra("iddoc", idDoc)
            startActivity(backH)
            finish()
            return true
        }

        if (ss.helper.whatInt(keyCode) >= 0) {
            val thisInt = ss.helper.whatInt(keyCode).toString()
            when (yCoor) {
                1 -> details.text =
                    (if (isMoveButon) thisInt else details.text.toString() + thisInt).toInt()
                        .toString()
                2 -> {
                    if (xCoor == 2) tbCount0.text =
                        (if (isMoveButon) thisInt else tbCount0.text.toString() + thisInt).toInt()
                            .toString()
                }
                3 -> {
                    if (xCoor == 1) tbCoef1.text =
                        (if (isMoveButon) thisInt else tbCoef1.text.toString() + thisInt).toInt()
                            .toString()
                    else tbCount1.text =
                        (if (isMoveButon) thisInt else tbCount1.text.toString() + thisInt).toInt()
                            .toString()
                }
                4 -> {
                    if (xCoor == 1) tbCoef2.text =
                        (if (isMoveButon) thisInt else tbCoef2.text.toString() + thisInt).toInt()
                            .toString()
                    else tbCount2.text =
                        (if (isMoveButon) thisInt else tbCount2.text.toString() + thisInt).toInt()
                            .toString()
                }
                5 -> {
                    if (xCoor == 1) tbCoef3.text =
                        (if (isMoveButon) thisInt else tbCoef3.text.toString() + thisInt).toInt()
                            .toString()
                    else tbCount3.text =
                        (if (isMoveButon) thisInt else tbCount3.text.toString() + thisInt).toInt()
                            .toString()
                }
            }
            isMoveButon = false
        }

        if (keyCode == 67) {
            //это делете, обнулим значение если это коэффициент
            isMoveButon = true
            var textForEdit = ""
            when (yCoor) {
                1 -> {
                    textForEdit = details.text.toString().trim()
                    if (textForEdit.count() == 1) details.text = "0" else details.text =
                        textForEdit.substring(0, textForEdit.count() - 1)
                }
                2 -> {
                    textForEdit = tbCount0.text.toString().trim()
                    if (xCoor == 2) if (textForEdit.count() == 1) tbCount0.text =
                        AcceptedItem["Count"].toString() else tbCount0.text =
                        textForEdit.substring(0, textForEdit.count() - 1)
                }
                3 -> {
                    if (xCoor == 1) {
                        textForEdit = tbCoef1.text.toString().trim()
                        if (textForEdit.count() == 1) tbCoef1.text = "0" else tbCoef1.text =
                            textForEdit.substring(0, textForEdit.count() - 1)
                    } else {
                        textForEdit = tbCount1.text.toString().trim()
                        if (textForEdit.count() == 1) tbCount1.text = "0" else tbCount1.text =
                            textForEdit.substring(0, textForEdit.count() - 1)
                    }
                }
                4 -> {
                    if (xCoor == 1) {
                        textForEdit = tbCoef1.text.toString().trim()
                        if (textForEdit.count() == 1) tbCoef2.text = "0" else tbCoef2.text =
                            textForEdit.substring(0, textForEdit.count() - 1)
                    } else {
                        textForEdit = tbCount2.text.toString().trim()
                        if (textForEdit.count() == 1) tbCount2.text = "0" else tbCount2.text =
                            textForEdit.substring(0, textForEdit.count() - 1)
                    }
                }
                5 -> {
                    if (xCoor == 1) {
                        textForEdit = tbCoef3.text.toString().trim()
                        if (textForEdit.count() == 1) tbCoef3.text = "0" else tbCoef3.text =
                            textForEdit.substring(0, textForEdit.count() - 1)
                    } else {
                        textForEdit = tbCount3.text.toString().trim()
                        if (textForEdit.count() == 1) tbCount3.text = "0" else tbCount3.text =
                            textForEdit.substring(0, textForEdit.count() - 1)
                    }
                }
            }
        }
        //тут надо расскрасить текущий активный элемент и пересчитать все колонки
        refreshActivElement()
        changeUnitCoef()
        if (keyCode == 61) {
            if (completeAccept()) {
                noneItem()
                yapItem()
                updateTableInfo()
                val backH = Intent(this, NoneItem::class.java)
                backH.putExtra("ParentForm", "ItemCard")
                startActivity(backH)
                finish()
            }
            return true
        }
        return false
    }

    private fun engineChangeBarcode(coef: Int, barcode: String, okei: String): Boolean {
        FExcStr.text = ("Штрихкод $barcode принят!")   //По умолчанию так
        //Поиск по ШК
        if (newBarcodes.contains(barcode)) {
            newBarcodes.remove(barcode)
        }
        newBarcodes.add(barcode)
        if (fUnits.isNotEmpty() && barcode != "") {
            for (dr in fUnits) {
                if (dr["Barcode"] == barcode) {
                    val currOKEI = dr["OKEI"].toString()
                    if (dr["Coef"].toString().toInt() == coef && currOKEI == okei) {
                        FExcStr.text = ("Штрихкод $barcode уже задан у данной единицы!")
                        return true //С этим коэффициентом и штрихкодом и ОКЕИ уже есть единица
                    } else if (currOKEI == model.okeiUnit || currOKEI == model.okeiPack || currOKEI == model.okeiPackage || currOKEI == model.okeiKit) {
                        //Из специальной - ЛЮБУЮ (нельзя менять коэффициент)
                        if (dr["ID"].toString() == ss.getVoidID()) {
                            dr["Barcode"] = ""
                            break
                        }
                        //Это уже существующая единица, нужно попробывать "пересосать" ШК из несуществующей
                        var yes = false
                        for (tmpdr in fUnits) {
                            if (tmpdr["ID"].toString() == ss.getVoidID() && tmpdr["Barcode"].toString()
                                    .trim() != ""
                            ) {
                                dr["Barcode"] = tmpdr["Barcode"].toString()
                                tmpdr["Barcode"] = ""
                                yes = true
                                break
                            }
                        }
                        if (!yes) {
                            dr["Barcode"] = ""
                        }
                        break
                    } else {
                        //Из НЕ специальной - ЛЮБУЮ
                        if (dr["Coef"].toString().toInt() == coef) {
                            //Коэффициенты совпадают - просто изменим ОКЕИ
                            dr["OKEI"] = okei
                            return true
                        }
                        //Если не совпадают, то нужно очистить баркод
                        //ТУТ ТУПОЧИСТИМ, Т.К. ВОЗМОЖНОСТИ ЗАВОДИТЬ НОВЫЕ ГОВНОЕДЕНИЦИ МЫ НЕ СДЕЛАЛИ ЕЩЕ
                        dr["Barcode"] = ""
                        break
                    }
                }
            }
        }
        //Поиск по коэффициенту (Среди НЕ новых единиц - они приоритетные)
        for (dr in fUnits) {
            if (dr["Coef"].toString() == coef.toString()) {
                val currOKEI = dr["OKEI"].toString();
                if (dr["Barcode"].toString()
                        .trim() == "" && currOKEI == okei && dr["ID"].toString() != ss.getVoidID()
                ) {
                    //есть единица с пустым штрихкодом - будем использовать ее
                    dr["Barcode"] = barcode
                    return true
                }
            }
        }
        //Поиск по коэффициенту
        for (dr in fUnits) {
            if (dr["Coef"].toString() == coef.toString()) {
                val currOKEI = dr["OKEI"].toString();
                if (dr["Barcode"].toString().trim() == "" && currOKEI == okei) {
                    //есть единица с пустым штрихкодом - будем использовать ее
                    dr["Barcode"] = barcode
                    return true
                }
            }
        }
        //Поиск по ОКЕИ (Среди НЕ новых единиц - они приоритетные)
        for (dr in fUnits) {
            if (dr["OKEI"].toString() == okei) {
                val currOKEI = dr["OKEI"].toString();
                if (dr["Barcode"].toString()
                        .trim() == "" && currOKEI != model.okeiUnit && dr["ID"].toString() != ss.getVoidID()
                ) {
                    //Есть с пустым ШК и другим коэффициентом, имзеним это...
                    dr["Barcode"] = barcode
                    dr["Coef"] = coef.toString()
                    return true
                }
            }
        }
        //Поиск по ОКЕИ
        for (dr in fUnits) {
            if (dr["OKEI"].toString() == okei) {
                val currOKEI = dr["OKEI"].toString();
                if (dr["Barcode"].toString().trim() == "" && currOKEI != model.okeiUnit) {
                    //Есть с пустым ШК и другим коэффициентом, имзеним это...
                    dr["Barcode"] = barcode
                    dr["Coef"] = coef.toString()
                    return true
                }
            }
        }
        //Нельзя использовать имеющуюся, будем создавать новую
        val newRow: MutableMap<String, String> = mutableMapOf()
        newRow["ID"] = ss.getVoidID()
        newRow["Coef"] = coef.toString()
        newRow["OKEI"] = okei
        newRow["Barcode"] = barcode
        fUnits.add(newRow)
        return true
    }

    private fun changeUnitBarcode(barcode: String): Boolean {
        if (yCoor == 1) {
            return false
        }
        var coef = 0
        var okei = model.okeiOthers
        when (yCoor) {
            2 -> {
                coef = tbCoef0.text.toString().toInt()
                okei = model.okeiUnit
            }
            3 -> {
                coef = tbCoef1.text.toString().toInt()
                okei = model.okeiPack
            }
            4 -> {
                coef = tbCoef2.text.toString().toInt()
                okei = model.okeiPackage
            }
            5 -> {
                coef = tbCoef3.text.toString().toInt()
                okei = model.okeiKit
            }
        }
        return changeUnitBarcode(coef, barcode, okei)
    }

    private fun changeUnitBarcode(coef: Int, barcode: String, okei: String): Boolean {

        if (coef == 0) {
            FExcStr.text = "Нельзя задать ШК единице с нулевым коэффициентом!"
            return false
        }

        val result = engineChangeBarcode(coef, barcode, okei)

        if (newBarcodes.isNotEmpty()) {
            flagBarcode = "1"
            //may bee flag even better?
            for (dr in fUnits) {
                if (dr["OKEI"].toString() == model.okeiPackage) {
                    if (newBarcodes.contains(dr["Barcode"].toString().trim())) {
                        flagBarcode = "2"
                        break
                    }
                }
            }
        }
        refreshActivity()
        return result
    }

    private fun changeUnitCoef() {
        if (xCoor == 2 || yCoor == 1) {
            return
        }
        var coef = 0
        var okei = model.okeiOthers
        when (yCoor) {
            2 -> {
                coef = tbCoef0.text.toString().toInt()
                okei = model.okeiUnit
            }
            3 -> {
                coef = tbCoef1.text.toString().toInt()
                okei = model.okeiPack
            }
            4 -> {
                coef = tbCoef2.text.toString().toInt()
                okei = model.okeiPackage
            }
            5 -> {
                coef = tbCoef3.text.toString().toInt()
                okei = model.okeiKit
            }
        }
        changeUnitCoef(coef, "", okei)
    }

    private fun changeUnitCoef(coef: Int, barcode: String, okei: String) {
        if (okei == model.okeiPack || okei == model.okeiPackage || okei == model.okeiKit) {
            //Это единица специального вида - меняем коэффициент у всех единиц такого вида
            if (fUnits.isNotEmpty()) {
                var findOkei = false
                for (dr in fUnits) {
                    if (dr["OKEI"] == okei) {
                        dr["Coef"] = coef.toString()
                        findOkei = true
                    }
                }
                if (findOkei) {
                    return
                }
            }
            //Не найдено...
            if (coef != 0) {
                val newRow: MutableMap<String, String> = mutableMapOf()
                newRow["ID"] = ss.getVoidID()
                newRow["Coef"] = coef.toString()
                newRow["OKEI"] = okei
                newRow["Barcode"] = barcode
                fUnits.add(newRow)
            }
        } else {
            //меняем коэффициент у всех говноединицс с таким ШК, по идее это всегда одна единица, т.к. ШК - уникален!
            if (fUnits.isNotEmpty() && barcode != "") {
                for (dr in fUnits) {
                    if (dr["Barcode"] == barcode && !(okei == model.okeiPack || okei == model.okeiPackage || okei == model.okeiKit)) {
                        dr["Coef"] = coef.toString()
                    }
                }
            }
            //Ввод новых для говноединиц пока не поддерживаем
        }
    }

    //тотальная ебань
    //класс для обработки события при нажатии на кнопку о принятии товара
    private fun completeAccept(): Boolean {

        if (!checkMark()) {
            //не указана маркировка
            FExcStr.text = "Маркированный товар! Сканируйте маркировку!"
            return false
        }
        val docForQuery = currentRowAcceptedItem["iddoc"].toString()
        var textQuery = ""
        //Сколько в накладной изначально
        if (AcceptedItem["AcceptCount"].toString().toInt() > currentRowAcceptedItem["Count"].toString().toInt()) {
            FExcStr.text =("Нельзя принять по данной накладной более " + currentRowAcceptedItem["Count"].toString() + " штук!")
            return false
        } else if (AcceptedItem["AcceptCount"].toString().toInt() == 0) {
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
        textQuery = ss.querySetParam(textQuery,"Doc", docForQuery)
        textQuery = ss.querySetParam(textQuery, "Item", item.id)
        textQuery = ss.querySetParam(textQuery,"LineNo_", currentRowAcceptedItem["LineNO_"].toString())
        val dTCurrState = ss.executeWithReadNew(textQuery)
        if (dTCurrState == null) {
            FExcStr.text = "Недопустимое количество! Повторите приемку позиции!"
            return false
        }
        if (dTCurrState[0]["Count"].toString().toInt() < AcceptedItem["AcceptCount"].toString().toInt()) {
            FExcStr.text = "Недопустимое количество! Повторите приемку позиции!"
            return false
        }
        //Скорректируем начальное количество
        val beginCount = dTCurrState[0]["Count"].toString().toInt()

        var needNew: Int = 0
        var coefPlace: Int = 0 //Коэффициент мест, по нему будет расчитывать количество этикеток
        val tmpDT: MutableList<MutableMap<String, String>> = mutableListOf()
        for (dr in fUnits) {
            if (dr["Coef"].toString().toInt() == 1 && dr["OKEI"].toString() != model.okeiUnit) {
                FExcStr.text = "Коэффициент 1 может быть только у штуки! Пожалуйста исправьте..."
                return false
            }
            if (dr["OKEI"].toString() == model.okeiPackage) {
                coefPlace = dr["Coef"].toString().toInt()
            }
            if (dr["ID"].toString() != ss.getVoidID()) {
                //Имеющаяся единица
                if (dr["Coef"].toString().toInt() == 0) {
                    textQuery = "UPDATE \$Спр.ЕдиницыШК " +
                            "SET " +
                            "ismark = 1 " +
                            "WHERE \$Спр.ЕдиницыШК .id = :ID ";
                    textQuery = ss.querySetParam(textQuery, "ID", dr["ID"].toString())
                    if (!ss.executeWithoutRead(textQuery)) {
                        return false
                    }
                } else {
                    textQuery = "UPDATE \$Спр.ЕдиницыШК " +
                            "SET " +
                            "\$Спр.ЕдиницыШК.Штрихкод = :Barcode, " +
                            "\$Спр.ЕдиницыШК.Коэффициент = :Coef, " +
                            "\$Спр.ЕдиницыШК.ОКЕИ = :OKEIPackage, " +
                            "\$Спр.ЕдиницыШК.ФлагРегистрацииМОД = 1 " +
                            "WHERE \$Спр.ЕдиницыШК .id = :ID "
                    textQuery = ss.querySetParam(textQuery, "Barcode", dr["Barcode"].toString())
                    textQuery = ss.querySetParam(textQuery, "Coef", dr["Coef"].toString())
                    textQuery = ss.querySetParam(textQuery, "ID", dr["ID"].toString())
                    textQuery = ss.querySetParam(textQuery, "OKEIPackage", dr["OKEI"].toString())
                    if (!ss.executeWithoutRead(textQuery)) {
                        return false
                    }
                }
            } else {
                var findDr = false
                if (tmpDT.isNotEmpty()) {
                    for (tmpdr in tmpDT) {
                        if (tmpdr["Coef"].toString() == dr["Coef"].toString() && tmpdr["OKEI"].toString() == dr["OKEI"].toString()) {
                            findDr = true
                            break
                        }
                    }
                }
                if (dr["Barcode"].toString().trim() != "" || !findDr) {
                    needNew++
                }
            }

            val tmpdr: MutableMap<String, String> = mutableMapOf()
            tmpdr["Coef"] = dr["Coef"].toString()
            tmpdr["OKEI"] = dr["OKEI"].toString()
            tmpDT.add(tmpdr)
        }
        tmpDT.clear()

        if (needNew > 0) {
            //Есть новые...
            var currentRow: Int = 0
            //Теперь также пишем новые
            for (dr in fUnits) {
                if (dr["Coef"].toString().toInt() == 0) {
                    continue
                }
                if (dr["ID"].toString() == ss.getVoidID()) {
                    var findDr = false
                    if (tmpDT.isNotEmpty()) {
                        for (tmpdr in tmpDT) {
                            if (tmpdr["Coef"].toString() == dr["Coef"].toString() && tmpdr["OKEI"].toString() == dr["OKEI"].toString()) {
                                findDr = true
                                break
                            }
                        }
                    }
                    if (dr["Barcode"].toString().trim() == "" && findDr) {
                        continue
                    }
                    if (!createUnit(
                            dr["OKEI"].toString(),
                            dr["Coef"].toString().toInt(),
                            dr["Barcode"].toString()
                        )
                    ) {
                        return false
                    }
                    currentRow++
                }
                val tmpdr: MutableMap<String, String> = mutableMapOf()
                tmpdr["Coef"] = dr["Coef"].toString()
                tmpdr["OKEI"] = dr["OKEI"].toString()
                tmpDT.add(tmpdr)
            }
        }
        //Запишем норму упаковки в товар
        var packNorm = ""
        packNorm = addPackNorm(packNorm, model.okeiKit)
        packNorm = addPackNorm(packNorm, model.okeiPack)
        packNorm = addPackNorm(packNorm, model.okeiPackage)
        if (packNorm == "") {
            packNorm = "1"
        } else {
            packNorm = packNorm.substring(1)  //Отрезаем первый символ "/"
        }

        if (AcceptedItem["MinParty"].toString().toInt() > 0)
            packNorm = ">" + AcceptedItem["MinParty"].toString() + "/" + packNorm

        textQuery = "UPDATE \$Спр.Товары " +
                "SET \$Спр.Товары.НормаУпаковки = :PackNorm , " +
                "\$Спр.Товары.КоличествоДеталей = :Details " +
                "WHERE \$Спр.Товары .id = :ItemID";
        textQuery = ss.querySetParam(textQuery, "ItemID", item.id)
        textQuery = ss.querySetParam(textQuery, "PackNorm", packNorm)
        textQuery = ss.querySetParam(textQuery, "Details", AcceptedItem["NowDetails"].toString())
        if (!ss.executeWithoutRead(textQuery)) {
            return false
        }

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
                "and RefE.\$Спр.ЕдиницыШК.ОКЕИ = :OKEI " +
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

        textQuery = ss.querySetParam(textQuery, "OKEI", model.okeiUnit)
        textQuery = ss.querySetParam(textQuery, "ItemID", item.id)
        if (!ss.executeWithoutRead(textQuery)) {
            return false
        }
        //Конец куска с заменой ШК у базовой единицы

        //ТЕПЕРЬ ПОЕХАЛА ЗАПИСЬ ДОКУМЕНТА
        //Расчитаем число этикеток
        var labelCount = 0
        if (flagBarcode != "0") {
            labelCount = 0
        } else {
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
        textQuery = ss.querySetParam(textQuery, "Doc", docForQuery)
        textQuery = ss.querySetParam(textQuery, "Item", item.id)
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        val alreadyDT = ss.executeWithReadNew(textQuery) ?: return false

        var allCountAccepted: Int = AcceptedItem["AcceptCount"].toString().toInt()
        if (alreadyDT.isEmpty() && allCountAccepted < beginCount) {
            //Нуно создать новую строку
            textQuery = "SELECT max(DT\$АдресПоступление .lineno_) + 1 as NewLineNo_ " +
                    "FROM DT\$АдресПоступление " +
                    "WHERE DT\$АдресПоступление .iddoc = :Doc"
            textQuery = ss.querySetParam(textQuery, "Doc", docForQuery)
            val datTab = ss.executeWithReadNew(textQuery) ?: return false
            val newLineNo_ = datTab[0]["NewLineNo_"].toString()

            textQuery = "INSERT INTO DT\$АдресПоступление VALUES " +
                    "(:Doc, :LineNo_, :Number, :Item, :Count, :EmptyID, :Coef, 1, :Employer, " +
                    ":Adress, :EmptyDate, :NowTime, 1, :LabelCount, :UnitID, 0, 0, :PalletID); " +
                    "UPDATE DT\$АдресПоступление " +
                    "SET \$АдресПоступление.Количество = :RemainedCount" +
                    "WHERE DT\$АдресПоступление .iddoc = :Doc and " +
                    "DT\$АдресПоступление .lineno_ = :RemainedLineNo_"
            textQuery = ss.querySetParam(textQuery, "Doc", docForQuery)
            textQuery = ss.querySetParam(textQuery, "LineNo_", newLineNo_)
            textQuery = ss.querySetParam(textQuery, "Number", currentRowAcceptedItem["Number"].toString())
            textQuery = ss.querySetParam(textQuery, "Item", item.id)
            textQuery = ss.querySetParam(textQuery, "Count", AcceptedItem["AcceptCount"].toString().toInt())
            textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
            textQuery = ss.querySetParam(textQuery, "Coef", 1)
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            textQuery = ss.querySetParam(textQuery, "Adress", ss.getVoidID())
            textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
            textQuery = ss.querySetParam(textQuery, "LabelCount", labelCount)
            textQuery = ss.querySetParam(textQuery, "UnitID", currentRowAcceptedItem["Unit"].toString())
            textQuery = ss.querySetParam(textQuery, "RemainedLineNo_", currentRowAcceptedItem["LineNO_"].toString())
            textQuery = ss.querySetParam(textQuery, "RemainedCount", beginCount - AcceptedItem["AcceptCount"].toString().toInt())
            textQuery = ss.querySetParam(textQuery, "PalletID", ss.FPallet.id)
            if (!ss.executeWithoutRead(textQuery)) {
                return false
            }
        } else if (alreadyDT.isEmpty() && AcceptedItem["AcceptCount"].toString().toInt() >= beginCount) {
            //Товар, будем писать в туже стоку
            textQuery = "UPDATE DT\$АдресПоступление " +
                    "SET " +
                    "\$АдресПоступление.Количество = :Count," +
                    "\$АдресПоступление.Сотрудник0 = :Employer," +
                    "\$АдресПоступление.Дата0 = :EmptyDate," +
                    "\$АдресПоступление.Время0 = :NowTime," +
                    "\$АдресПоступление.Состояние0 = 1," +
                    "\$АдресПоступление.КоличествоЭтикеток = :LabelCount," +
                    "\$АдресПоступление.ФлагПечати = 1," +
                    "\$АдресПоступление.Паллета = :PalletID " +
                    "WHERE " +
                    "DT\$АдресПоступление .iddoc = :Doc " +
                    "and DT\$АдресПоступление .lineno_ = :LineNo_"
            textQuery = ss.querySetParam(textQuery, "Count", AcceptedItem["AcceptCount"].toString().toInt())
            textQuery = ss.querySetParam(textQuery, "Doc", docForQuery)
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
            textQuery = ss.querySetParam(textQuery, "PalletID", ss.FPallet.id)
            textQuery = ss.querySetParam(textQuery, "LabelCount", labelCount)
            textQuery = ss.querySetParam(textQuery, "LineNo_", currentRowAcceptedItem["LineNO_"].toString())
            if (!ss.executeWithoutRead(textQuery)) {
                return false
            }
        } else if (alreadyDT.isNotEmpty() && AcceptedItem["AcceptCount"].toString().toInt() < beginCount) {
            //Нуно создать новую строку на новую паллету
            textQuery =
                "SELECT max(DT\$АдресПоступление .lineno_) + 1 as NewLineNo_ " +
                        "FROM DT\$АдресПоступление WHERE " +
                        "DT\$АдресПоступление .iddoc = :Doc"
            textQuery = ss.querySetParam(textQuery, "Doc", docForQuery)
            val daTab = ss.executeWithReadNew(textQuery) ?: return false
            val newLineNo_ = daTab[0]["NewLineNo_"].toString()

            textQuery = "INSERT INTO DT\$АдресПоступление VALUES " +
                    "(:Doc, :LineNo_, :Number, :Item, :Count, :EmptyID, :Coef, 1, :Employer, " +
                    ":Adress, :EmptyDate, :NowTime, 1, :LabelCount, :UnitID, 0, 0, :PalletID); " +
                    "UPDATE DT\$АдресПоступление " +
                    "SET \$АдресПоступление.Количество = :RemainedCount" +
                    "WHERE DT\$АдресПоступление .iddoc = :Doc and DT\$АдресПоступление .lineno_ = :RemainedLineNo_";
            textQuery = ss.querySetParam(textQuery, "Doc", docForQuery)
            textQuery = ss.querySetParam(textQuery, "LineNo_", newLineNo_)
            textQuery = ss.querySetParam(textQuery, "Number", currentRowAcceptedItem["Number"].toString())
            textQuery = ss.querySetParam(textQuery, "Item", item.id)
            textQuery = ss.querySetParam(textQuery, "Count",AcceptedItem["AcceptCount"].toString())
            textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
            textQuery = ss.querySetParam(textQuery, "Coef", 1)
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            textQuery = ss.querySetParam(textQuery, "Adress", ss.getVoidID())
            textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
            textQuery = ss.querySetParam(textQuery, "LabelCount", labelCount)
            textQuery = ss.querySetParam(textQuery, "UnitID", currentRowAcceptedItem["Unit"].toString())
            textQuery = ss.querySetParam(textQuery, "RemainedLineNo_", currentRowAcceptedItem["LineNO_"].toString())
            textQuery = ss.querySetParam(textQuery, "RemainedCount",beginCount - AcceptedItem["AcceptCount"].toString().toInt())
            textQuery = ss.querySetParam(textQuery, "PalletID", ss.FPallet.id)
            if (!ss.executeWithoutRead(textQuery)) {
                return false
            }
            //теперь еще обновим непринятые строки
            allCountAccepted = alreadyDT[0]["Count"].toString().toInt() + AcceptedItem["AcceptCount"].toString().toInt()
            textQuery = "UPDATE DT\$АдресПоступление " +
                    "SET " +
                    "\$АдресПоступление.Количество = :RemainedCount " +
                    "WHERE " +
                    "DT\$АдресПоступление .iddoc = :Doc " +
                    "and DT\$АдресПоступление .lineno_ = :RemainedLineNo_"
            textQuery = ss.querySetParam(textQuery, "Doc", docForQuery)
            textQuery = ss.querySetParam(textQuery, "RemainedLineNo_", currentRowAcceptedItem["LineNO_"].toString())
            textQuery = ss.querySetParam(textQuery, "RemainedCount", beginCount - AcceptedItem["AcceptCount"].toString().toInt())
            if (!ss.executeWithoutRead(textQuery)) {
                return false
            }
        } else {
            if (alreadyDT[0]["LineNo_"] == currentRowAcceptedItem["LineNO_"]) {
                FExcStr.text = "Состояние позиции изменилось! Повторите приемку!"
                return false
            }
            //Уже есть строка принятого, будем писать в изначальную (не принятую)
            textQuery = "UPDATE DT\$АдресПоступление " +
                    "SET " +
                    "\$АдресПоступление.Количество = :Count," +
                    "\$АдресПоступление.Сотрудник0 = :Employer," +
                    "\$АдресПоступление.Дата0 = :EmptyDate," +
                    "\$АдресПоступление.Время0 = :NowTime," +
                    "\$АдресПоступление.Состояние0 = 1," +
                    "\$АдресПоступление.КоличествоЭтикеток = :LabelCount," +
                    "\$АдресПоступление.ФлагПечати = 1," +
                    "\$АдресПоступление.Паллета = :PalletID " +
                    "WHERE " +
                    "DT\$АдресПоступление .iddoc = :Doc " +
                    "and DT\$АдресПоступление .lineno_ = :LineNo_; "
            textQuery = ss.querySetParam(textQuery, "Doc", docForQuery)
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
            textQuery = ss.querySetParam(textQuery, "PalletID", ss.FPallet.id)
            textQuery = ss.querySetParam(textQuery, "Count", AcceptedItem["AcceptCount"].toString().toInt())
            textQuery = ss.querySetParam(textQuery, "LabelCount", labelCount)
            textQuery = ss.querySetParam(textQuery, "LineNo_", currentRowAcceptedItem["LineNO_"].toString())
            if (!ss.executeWithoutRead(textQuery)) {
                return false
            }
        }

        //Выведем в строку состояния сколько мы приняли за этот раз
        var tmpCoef: Int = getCoefPackage()
        if (tmpCoef == 0) {
            tmpCoef = currentRowAcceptedItem["Coef"].toString().toInt()
        }
        FExcStr.text = (AcceptedItem["InvCode"].toString().trim() + " принят в количестве " + model.getStrPackageCount(
            AcceptedItem["AcceptCount"].toString().toInt(), tmpCoef
        ))
        //begin internal command
        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ss.extendID(ss.FEmployer.id, "Спр.Сотрудники")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход2"] = ss.extendID(item.id, "Спр.Товары")
        dataMapWrite["Спр.СинхронизацияДанных.ДокументВход"] = ss.extendID(docForQuery, "АдресПоступление")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = "AcceptItem (Принял товар)"
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход2"] = AcceptedItem["AcceptCount"].toString()
        execCommandNoFeedback("Internal", dataMapWrite)
        //end internal command
        if (!ibsLockuot("int_ref_Товары_" + (item.id + "_unit"))) {
            //если не удалось разблокировать то просто напишем об этом
            FExcStr.text = "Не удалось разблокировать товар!"
        }
        //обновим данные табличек и выйдем наружу
        return true
    }

    private fun createUnit(okei: String, coef: Int, barcode: String): Boolean {
        //Нужно создать новую единицу
        var textQuery =
            "UPDATE \$Спр.ЕдиницыШК " +
                    "SET " +
                    "\$Спр.ЕдиницыШК.Штрихкод = :Barcode, " +
                    "\$Спр.ЕдиницыШК.Коэффициент = :Coef, " +
                    "\$Спр.ЕдиницыШК.ОКЕИ = :OKEI, " +
                    "\$Спр.ЕдиницыШК.ФлагРегистрацииМОД = 1, " +
                    "parentext = :ItemID " +
                    "WHERE \$Спр.ЕдиницыШК .id in (" +
                    "SELECT top 1 Units.id FROM \$Спр.ЕдиницыШК as Units (nolock) " +
                    "WHERE Units.parentext = :ItemForUnits " +
                    "ORDER BY Units.ismark DESC)";
        textQuery = ss.querySetParam(textQuery, "Barcode", barcode)
        textQuery = ss.querySetParam(textQuery, "Coef", coef)
        textQuery = ss.querySetParam(textQuery, "ItemID", item.id)
        textQuery = ss.querySetParam(textQuery, "OKEI", okei)
        textQuery = ss.querySetParam(textQuery, "ItemForUnits", ss.Const.itemForUnits)
        return ss.executeWithoutRead(textQuery)
    }

    private fun addPackNorm(packNorm: String, okei: String): String {
        var packNorm = packNorm
        for (dr in fUnits) {
            if (dr["OKEI"].toString() == okei && dr["Coef"].toString().toInt() != 0) {
                packNorm += "/" + dr["Coef"].toString();
                if (okei == model.okeiKit) {
                    packNorm += "!";
                } else if (okei == model.okeiPackage) {
                    packNorm += "*";
                }
                break
            }
        }
        return packNorm
    }

    private fun getCoefPackage(): Int {
        var coef = 0;
        var textQuery =
            "SELECT TOP 1 " +
                    "Units.\$Спр.ЕдиницыШК.Коэффициент as Coef " +
                    "FROM " +
                    "\$Спр.ЕдиницыШК as Units (nolock) " +
                    "WHERE " +
                    "Units.\$Спр.ЕдиницыШК.ОКЕИ = :OKEI " +
                    "and Units.ismark = 0 " +
                    "and Units.parentext = :Item " +
                    "ORDER BY Units.\$Спр.ЕдиницыШК.Коэффициент "
        textQuery = ss.querySetParam(textQuery, "Item", item.id)
        textQuery = ss.querySetParam(textQuery, "OKEI", model.okeiPackage)

        val dtdt = ss.executeWithReadNew(textQuery) ?: return coef
        if (dtdt.isEmpty()) {
            coef = 1
        } else {
            coef = dtdt[0]["Coef"].toString().toFloat().toInt()
        }
        return coef
    }

    private fun checkMark(): Boolean {

        var textQuery = "SELECT " +
                "ISNULL(journ.iddoc, AC.iddoc ) as iddoc , " +
                "AC.iddoc as ACiddoc , " +
                "FROM " +
                " DH\$АдресПоступление as AC (nolock) " +
                "INNER JOIN _1sjourn as journAC (nolock) " +
                "     ON journAC.iddoc = AC.iddoc " +
                "LEFT JOIN _1sjourn as journ (nolock) " +
                "     ON journ.iddoc = right(AC.\$АдресПоступление.ДокументОснование , 9) " +
                "WHERE" +
                " AC.iddoc = '${idDoc}' "

        val naklAccTemp = ss.executeWithReadNew(textQuery)
        if (naklAccTemp == null || naklAccTemp.isEmpty())
        {
            //не вышли на накладную - косяк какой-то
           return false
        }
        //не пусто
        textQuery = "select id as ID , " +
                "\$Спр.МаркировкаТовара.Маркировка as Mark , " +
                "-1*(isFolder-2) as Box ," +
                "\$Спр.МаркировкаТовара.Товар as item " +
                "from \$Спр.МаркировкаТовара (nolock) " +
                "where (\$Спр.МаркировкаТовара.ДокПоступления = '${ss.extendID(naklAccTemp[0]["ACiddoc"].toString(), "АдресПоступление")}' " +
                "or \$Спр.МаркировкаТовара.ДокПоступления = '${ss.extendID(naklAccTemp[0]["iddoc"].toString(), "ПриходнаяКредит")}' )" +
                "and \$Спр.МаркировкаТовара.Товар = '${item.id}' "
        val markItemDT = ss.executeWithReadNew(textQuery) ?: return false
        if (markItemDT.isEmpty()) {
            return false
        }
        return true
    }
}