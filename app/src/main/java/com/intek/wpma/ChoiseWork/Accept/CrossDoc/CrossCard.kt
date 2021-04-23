package com.intek.wpma.ChoiseWork.Accept.CrossDoc

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.TextView
import com.intek.wpma.Helpers.Helper
import com.intek.wpma.R
import com.intek.wpma.Ref.RefItem
import kotlinx.android.synthetic.main.activity_cross_acc.*
import kotlinx.android.synthetic.main.activity_cross_acc.FExcStr
import kotlinx.android.synthetic.main.activity_cross_acc.btnMark
import kotlinx.android.synthetic.main.activity_cross_acc.btnPrinItem
import kotlinx.android.synthetic.main.activity_cross_acc.details
import kotlinx.android.synthetic.main.activity_cross_acc.itemName
import kotlinx.android.synthetic.main.activity_cross_acc.pricePrih
import kotlinx.android.synthetic.main.activity_cross_acc.shapka
import kotlinx.android.synthetic.main.activity_cross_acc.storageSize
import kotlinx.android.synthetic.main.activity_cross_acc.tbBarcode0
import kotlinx.android.synthetic.main.activity_cross_acc.tbBarcode1
import kotlinx.android.synthetic.main.activity_cross_acc.tbBarcode2
import kotlinx.android.synthetic.main.activity_cross_acc.tbBarcode3
import kotlinx.android.synthetic.main.activity_cross_acc.tbCoef0
import kotlinx.android.synthetic.main.activity_cross_acc.tbCoef1
import kotlinx.android.synthetic.main.activity_cross_acc.tbCoef2
import kotlinx.android.synthetic.main.activity_cross_acc.tbCoef3
import kotlinx.android.synthetic.main.activity_cross_acc.tbCount0
import kotlinx.android.synthetic.main.activity_cross_acc.tbCount1
import kotlinx.android.synthetic.main.activity_cross_acc.tbCount2
import kotlinx.android.synthetic.main.activity_cross_acc.tbCount3
import kotlinx.android.synthetic.main.activity_cross_acc.tbRes0
import kotlinx.android.synthetic.main.activity_cross_acc.tbRes1
import kotlinx.android.synthetic.main.activity_cross_acc.tbRes2
import kotlinx.android.synthetic.main.activity_cross_acc.tbRes3
import kotlinx.android.synthetic.main.activity_cross_acc.zonaHand
import kotlinx.android.synthetic.main.activity_cross_acc.zonaTech
import kotlinx.android.synthetic.main.activity_crossdoc.*
import kotlinx.android.synthetic.main.activity_search_acc.*

class CrossCard : CrossDoc() {

    private var units: MutableList<MutableMap<String, String>> = mutableListOf()
    private var fUnits: MutableList<MutableMap<String, String>> = mutableListOf()
    private val newBarcodes: MutableList<String> = mutableListOf()
    private var idDoc = ""
    private var allCount = 0
    private var bufferWarehouse = "" //переменка для товара на главном
    private var flagBarcode = ""
    private var isMoveButton = true
    private var xCoor = 2
    private var yCoor = 4


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cross_acc)

        flagBarcode = intent.extras!!.getString("flagBarcode")!!
        parentIDD = intent.extras!!.getString("parentIDD")!!
        title = ss.title
        item.foundID(intent.extras!!.getString("itemID")!!)
        idDoc = intent.extras!!.getString("iddoc")!!
        countMarkPac = if (intent.extras!!.getString("countMarkPac").isNullOrEmpty()) 0 else intent.extras!!.getString("countMarkPac").toString().toInt()
        countMarkUnit = if (intent.extras!!.getString("countMarkUnit").isNullOrEmpty()) 0 else intent.extras!!.getString("countMarkUnit").toString().toInt()
        countItemAcc = if (intent.extras!!.getString("countItemAcc").isNullOrEmpty()) 0 else intent.extras!!.getString("countItemAcc").toString().toInt()

        btnPrinItem.setOnClickListener {            //обработчик события при нажатии на кнопку принятия товара
            if (!ss.FPallet.selected) {
                badVoise()
                "Сначала отсканируйте паллету!!!".also { FExcStr.text = it }
                return@setOnClickListener
            } else {
                if (completeAccept(idDoc)) {
                    val backH = Intent(this, CrossNonItem::class.java)
                    backH.putExtra("ParentForm", "ItemCard")
                    backH.putExtra("parentIDD", parentIDD)
                    backH.putExtra("flagBarcode", flagBarcode)
                    backH.putExtra("itemID", item.id)
                    backH.putExtra("iddoc", idDoc)
                    startActivity(backH)
                    noneItem()
                    yapItem()
                    updateTableInfo()
                    finish()
                }
            }
        }

        btnMark.setOnClickListener {
            //обработчик события при нажатии на кнопку принятия товара
            if (!saveUnitItem()) {
                badVoise()
                FExcStr.text = "Не удалось записать позицию!"
                return@setOnClickListener
            }
            if (!ibsLockuot("int_ref_Товары_" + (item.id + "_unit"))) {
                //если не удалось разблокировать то просто напишем об этом
                FExcStr.text = "Не удалось разблокировать товар!"
                return@setOnClickListener
            }
            val backH = Intent(this, CrossMark::class.java)
            backH.putExtra("ParentForm", "CrossCard")
            backH.putExtra("flagBarcode", flagBarcode)
            backH.putExtra("itemID", item.id)
            backH.putExtra("iddoc", idDoc)
            backH.putExtra("countItemAcc", acceptedItem["AcceptCount"].toString())
            startActivity(backH)
            finish()
        }

        if (!loadUnits()) {         //облом выходим обратно
            val backH = Intent(this, CrossNonItem::class.java)
            backH.putExtra("parentIDD", parentIDD)
            startActivity(backH)
            finish()
            return
        }
        //БЛОКИРУЕМ ТОВАР
        if (!lockItem(item.id)) {       //облом выходим обратно
            val backH = Intent(this, CrossNonItem::class.java)
            backH.putExtra("parentIDD", parentIDD)
            startActivity(backH)
            finish()
            return
        }
        toModeAcceptedItem()
    }

    private fun toModeAcceptedItem() {
        getCurrentRowAcceptedItem()     //Подтянем данные о не принятой позиции
        buffWare()                      //подтянем склад буфер
        getCountAdress()                //подтянем остатки
        refreshItemProper()             //тепреь все остальное
        refreshActivity()               //рефрешим активити
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
            acceptedItem["BalanceMain"] = datTab[0]["BalanceMain"].toString()
            acceptedItem["BalanceBuffer"] = datTab[0]["BalanceBuffer"].toString()
            acceptedItem["AdressMain"] = datTab[0]["AdressMain"].toString()
            acceptedItem["AdressBuffer"] = datTab[0]["AdressBuffer"].toString()
            acceptedItem["IsRepeat"] = "0"
        }
    }

    private fun getCurrentRowAcceptedItem() {
        for (dr in noneAccItem) {
            if (dr["id"] != item.id) continue
            if (dr["iddoc"].toString() == idDoc && currentRowAcceptedItem.isEmpty()) currentRowAcceptedItem = dr

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

            acceptedItem["id"] = currentRowAcceptedItem["id"].toString()
            acceptedItem["Name"] = currentRowAcceptedItem["ItemName"].toString()
            acceptedItem["InvCode"] = currentRowAcceptedItem["InvCode"].toString()
            acceptedItem["Acticle"] = currentRowAcceptedItem["Article"].toString()
            acceptedItem["Count"] = currentRowAcceptedItem["Count"].toString()
            acceptedItem["AcceptCount"] = currentRowAcceptedItem["Count"].toString()
            acceptedItem["Price"] = currentRowAcceptedItem["Price"].toString()
            acceptedItem["Acceptance"] = "1"
            acceptedItem["Details"] = currentRowAcceptedItem["Details"].toString()
            acceptedItem["NowDetails"] = acceptedItem["Details"].toString()
            acceptedItem["ToMode"] = "Acceptance"
            acceptedItem["BindingAdressFlag"] = "0"
            acceptedItem["SeasonGroup"] = currentRowAcceptedItem["SeasonGroup"].toString()
            acceptedItem["FlagFarWarehouse"] = currentRowAcceptedItem["FlagFarWarehouse"].toString()
            acceptedItem["StoregeSize"] = currentRowAcceptedItem["StoregeSize"].toString()
            acceptedItem["BaseUnitID"] = item.getAttribute("БазоваяЕдиницаШК").toString()
            acceptedItem["MinParty"] = item.getAttribute("МинПартия").toString()
        } else {
            acceptedItem["id"] = item.id
            acceptedItem["Name"] = item.name
            acceptedItem["InvCode"] = item.invCode
            acceptedItem["Acticle"] = item.getAttribute("Артикул").toString()
            acceptedItem["Count"] = "0"
            acceptedItem["AcceptCount"] = "0"
            acceptedItem["Price"] = item.getAttribute("Прих_Цена").toString()
            acceptedItem["Acceptance"] = "1"
            acceptedItem["Details"] = item.getAttribute("КоличествоДеталей").toString()
            acceptedItem["NowDetails"] = acceptedItem["Details"].toString()
            acceptedItem["ToMode"] = "Acceptance"
            acceptedItem["BindingAdressFlag"] = "0"
            acceptedItem["SeasonGroup"] = ""
            acceptedItem["FlagFarWarehouse"] = ""
            acceptedItem["StoregeSize"] = "0"
            acceptedItem["BaseUnitID"] = item.getAttribute("БазоваяЕдиницаШК").toString()
            acceptedItem["MinParty"] = item.getAttribute("МинПартия").toString()
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
        if (allCount > acceptedItem["AcceptCount"].toString().toInt()) {
            for (dr in units) {
                if (dr["OKEI"] == model.okeiPackage) {
                    coef = dr["Coef"].toString().toInt()
                    break
                }
            }
            excStr += " " + model.getStrPackageCount(
                acceptedItem["AcceptCount"].toString().toInt(),
                coef
            ) + " из " + model.getStrPackageCount(allCount, coef)
        }

        storageSize.text = acceptedItem["StoregeSize"]                              //размер хранения
        if (acceptedItem["StoregeSize"].toString().toFloat() > 0) storageSize.visibility = View.VISIBLE
        else storageSize.visibility = View.INVISIBLE
        itemName.text = acceptedItem["Name"]                                        //полное наименование товара
        details.text = acceptedItem["Details"]                                      //кол-во деталей товара, подтягиваем если есть, если нет заполняем, иначе 0
        shapka.text =
            (acceptedItem["InvCode"].toString() + "Приемка товара")                 //код товара
        pricePrih.text =
            ("Цена: " + acceptedItem["Price"].toString())                           //цена товара
        tbCount0.text = acceptedItem["AcceptCount"].toString()
        tbCoef0.text = "1"
        zonaHand.text = (acceptedItem["AdressMain"].toString()
            .trim() + ": " + acceptedItem["BalanceMain"].toString() + " шт")
        zonaTech.text = (acceptedItem["AdressBuffer"].toString()
            .trim() + ": " + acceptedItem["BalanceBuffer"].toString() + " шт")
        if (units.isNotEmpty()) {
            for (dr in units) {
                var findUnits = false
                for (fdr in fUnits) {
                    if (fdr["ID"] == dr["ID"]) {
                        findUnits = true
                        break
                    }
                }
                if (!findUnits) fUnits.add(dr)  //не нашли в табличке единицу, добавим
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
                            (acceptedItem["AcceptCount"].toString().toInt() / dr["Coef"].toString()
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
                            (acceptedItem["AcceptCount"].toString().toInt() / dr["Coef"].toString()
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
                            (acceptedItem["AcceptCount"].toString().toInt() / dr["Coef"].toString()
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
        tbRes0.text = acceptedItem["Count"].toString() //сумма принимаемого товара в общей сложности
        tbRes1.text = if (tbCoef1.text.toString().toInt() > 0) (acceptedItem["Count"].toString()
            .toInt() / tbCoef1.text.toString().toInt()).toString() else "0"
        tbRes2.text = if (tbCoef2.text.toString().toInt() > 0) (acceptedItem["Count"].toString()
            .toInt() / tbCoef2.text.toString().toInt()).toString() else "0"
        tbRes3.text = if (tbCoef3.text.toString().toInt() > 0) (acceptedItem["Count"].toString()
            .toInt() / tbCoef3.text.toString().toInt()).toString() else "0"

        //определяем, как был найден товар, перед тем, как зайти в карточку
        when (flagBarcode) {
            "0" -> {
                excStr += (acceptedItem["InvCode"].toString() +
                        "найден в ручную!")
            }
            "1" -> {
                excStr += (acceptedItem["InvCode"].toString() +
                        "найден по штрихкоду!")
            }
            //пока не требуется, но пусть будет
            /*   "2" -> {
                excStr.text = (AcceptedItem["InvCode"].toString() +
                        "найден по ШК МЕСТА!")
            } */
        }
        xCoor = if (tbCoef2.text == "0" && yCoor == 4) 1 else 2

        refreshActivElement()
        FExcStr.text = excStr
    }

    private fun refreshActivElement() {
        //для начала обнулим все цвета
        val tbArr : Array<TextView> = arrayOf(tbCoef0, tbCoef1, tbCoef2, tbCoef3, tbCount0, tbCount1, tbCount2, tbCount3, details)
        for ((k, _) in tbArr.withIndex()) tbArr[k].setBackgroundColor(Color.WHITE)
        tbBarcode0.setBackgroundColor(Color.LTGRAY)
        tbBarcode1.setBackgroundColor(Color.LTGRAY)
        tbBarcode2.setBackgroundColor(Color.LTGRAY)
        tbBarcode3.setBackgroundColor(Color.LTGRAY)
        when (yCoor) {
            1 -> details.setBackgroundColor(Color.LTGRAY)
            2 -> {
                if (xCoor == 1) tbCoef0.setBackgroundColor(Color.LTGRAY) //тут он не может ничего менять
                else tbCount0.setBackgroundColor(Color.LTGRAY)

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
        //проверим а нацело разделилось ли
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
        acceptedItem["AcceptCount"] = tbCount0.text.toString()
        acceptedItem["NowDetails"] = details.text.toString()
    }

    override fun reactionBarcode(Barcode: String): Boolean {
        val itemTemp = RefItem()            //для начала проверим этот ШК, может он уже у кого-то есть
        if (itemTemp.foundBarcode(Barcode)) {
            if (itemTemp.id != item.id) {
                badVoise()
                FExcStr.text = ("Штрих-код от другой позиции " + itemTemp.invCode)
                return false
            }
        }
        val helper = Helper()
        val barcoderes = helper.disassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if (typeBarcode == "113") {
            val idd = barcoderes["IDD"].toString()
            if (ss.isSC(idd, "Принтеры")||ss.isSC(idd, "Сотрудники") || ss.isSC(idd, "Секции")) {
                return super.reactionBarcode(Barcode)
            }
        } else if (typeBarcode == "pallete") {
            return if(scanPalletBarcode(Barcode)) {
                FExcStr.text = ("Паллета " + ss.FPallet.pallete)
                goodVoise()
                true
            } else {
                FExcStr.text = "Паллета уже занята!"
                badVoise()
                false
            }
        }
        //не опознанный ШК, скорее всего позиция, запомним ее
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
            isMoveButton = true
        }

        //вроде все как надо, но почему-то выдает ошибку при нажатии
        if (keyCode == 4) {                   //разблокируем обратно
            // если не удалось разблокировать то просто напишем об этом
            if (!ibsLockuot("int_ref_Товары_" + (item.id + "_unit"))) FExcStr.text = "Не удалось разблокировать товар!"
            val backH = Intent(this, CrossNonItem::class.java)
            backH.putExtra("parentIDD", parentIDD)
            backH.putExtra("ParentForm", "ItemCard")
            startActivity(backH)
            finish()
            return true
        }

        if (keyCode == 69) {        // -  переход в режим маркирвоки
            if (!saveUnitItem()) {
                badVoise()
                FExcStr.text = "Не удалось записать позицию!"
                return false
            }
            if (!ibsLockuot("int_ref_Товары_" + (item.id + "_unit"))) {
                //если не удалось разблокировать то просто напишем об этом
                FExcStr.text = "Не удалось разблокировать товар!"
                return false
            }
            // -  переход в режим маркирвоки
            val backH = Intent(this, CrossMark::class.java)
            backH.putExtra("ParentForm", "ItemCard")
            backH.putExtra("flagBarcode", flagBarcode)
            backH.putExtra("itemID", item.id)
            backH.putExtra("iddoc", idDoc)
            backH.putExtra("countItemAcc", acceptedItem["AcceptCount"].toString())
            startActivity(backH)
            finish()
            return true
        }

        if (ss.helper.whatInt(keyCode) >= 0) {
            val thisInt = ss.helper.whatInt(keyCode).toString()
            when (yCoor) {
                1 -> details.text =
                    (if (isMoveButton) thisInt else details.text.toString() + thisInt).toInt()
                        .toString()
                2 -> {
                    if (xCoor == 2) tbCount0.text =
                        (if (isMoveButton) thisInt else tbCount0.text.toString() + thisInt).toInt()
                            .toString()
                }
                3 -> {
                    if (xCoor == 1) tbCoef1.text =
                        (if (isMoveButton) thisInt else tbCoef1.text.toString() + thisInt).toInt()
                            .toString()
                    else tbCount1.text =
                        (if (isMoveButton) thisInt else tbCount1.text.toString() + thisInt).toInt()
                            .toString()
                }
                4 -> {
                    if (xCoor == 1) tbCoef2.text =
                        (if (isMoveButton) thisInt else tbCoef2.text.toString() + thisInt).toInt()
                            .toString()
                    else tbCount2.text =
                        (if (isMoveButton) thisInt else tbCount2.text.toString() + thisInt).toInt()
                            .toString()
                }
                5 -> {
                    if (xCoor == 1) tbCoef3.text =
                        (if (isMoveButton) thisInt else tbCoef3.text.toString() + thisInt).toInt()
                            .toString()
                    else tbCount3.text =
                        (if (isMoveButton) thisInt else tbCount3.text.toString() + thisInt).toInt()
                            .toString()
                }
            }
            isMoveButton = false
        }

        if (keyCode == 67) {
            //это делете, обнулим значение если это коэффициент
            isMoveButton = true
            val textForEdit: String
            when (yCoor) {
                1 -> {
                    textForEdit = details.text.toString().trim()
                    if (textForEdit.count() == 1) details.text = "0" else details.text =
                        textForEdit.substring(0, textForEdit.count() - 1)
                }
                2 -> {
                    textForEdit = tbCount0.text.toString().trim()
                    if (xCoor == 2) if (textForEdit.count() == 1) tbCount0.text =
                        acceptedItem["Count"].toString() else tbCount0.text =
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
            if (completeAccept(idDoc)) {
                val backH = Intent(this, CrossNonItem::class.java)
                backH.putExtra("parentIDD", parentIDD)
                backH.putExtra("ParentForm", "ItemCard")
                startActivity(backH)
                noneItem()
                yapItem()
                updateTableInfo()
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
                val currOKEI = dr["OKEI"].toString()
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
                val currOKEI = dr["OKEI"].toString()
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
                val currOKEI = dr["OKEI"].toString()
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
                val currOKEI = dr["OKEI"].toString()
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

    private fun saveUnitItem():Boolean {
        var needNew = 0
        var textQuery: String
        var coefPlace = 0 //Коэффициент мест, по нему будет расчитывать количество этикеток
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
                            "WHERE \$Спр.ЕдиницыШК .id = :ID "
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
            var currentRow = 0
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
        packNorm = if (packNorm == "") {
            "1"
        } else {
            packNorm.substring(1)  //Отрезаем первый символ "/"
        }

        if (acceptedItem["MinParty"].toString().toInt() > 0)
            packNorm = ">" + acceptedItem["MinParty"].toString() + "/" + packNorm

        textQuery = "UPDATE \$Спр.Товары " +
                "SET \$Спр.Товары.НормаУпаковки = :PackNorm , " +
                "\$Спр.Товары.КоличествоДеталей = :Details " +
                "WHERE \$Спр.Товары .id = :ItemID"
        textQuery = ss.querySetParam(textQuery, "ItemID", item.id)
        textQuery = ss.querySetParam(textQuery, "PackNorm", packNorm)
        textQuery = ss.querySetParam(textQuery, "Details", acceptedItem["NowDetails"].toString())
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
        return true
    }

}