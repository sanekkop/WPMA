package com.intek.wpma.choiseWork.accept.transfer

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import com.intek.wpma.Global
import com.intek.wpma.MainActivity
import com.intek.wpma.R
import com.intek.wpma.helpers.Helper
import com.intek.wpma.ref.RefEmployer
import com.intek.wpma.ref.RefGates
import com.intek.wpma.ref.RefItem
import com.intek.wpma.ref.RefSection
import kotlinx.android.synthetic.main.activity_transfer_card.*
import kotlin.toString as toString

open class TransferCard : TransferMode() {

    private var itemID = ""
    private var bufferWarehouse = "" //переменка для товара на главном
    private var units: MutableList<MutableMap<String, String>> = mutableListOf()
    private var currentRowAcceptedItem: MutableMap<String, String> = mutableMapOf()
    private var fUnits: MutableList<MutableMap<String, String>> = mutableListOf()
    private var idDoc = ""
    private var allCount = 0
    private var firstAdd = ""
    private var lastAdd = ""
    private var countItemAcc = 0 //Сколько нужно переместить на полку

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_card)

        title = ss.title
        itemID = intent.extras?.getString("itemID").toString()
        countItemAcc = intent.extras?.getInt("count").toString().toInt()
        itm.foundID(itemID)

        itemName.text = transferItem["InvCode"].toString()
        FExcStr.text = ("НА ПОЛКУ! \n Отсканируйте адрес!")

        progressBar2.visibility = VISIBLE
        tableLayout.visibility = INVISIBLE
        tableLayout2.visibility = INVISIBLE
        tabCount.visibility = INVISIBLE

        if (!loadUnits()) {
            //облом выходим обратно
            val backH = Intent(this, TransferMode::class.java)
            startActivity(backH)
            finish()
            return
        }
        //БЛОКИРУЕМ ТОВАР
        if (!lockItem(itm.id)) {
            //облом выходим обратно
            val backH = Intent(this, TransferMode::class.java)
            startActivity(backH)
            finish()
            return
        }

        toModeTransferItem()
    }

    private fun toModeTransferItem() {
        getCurrentRowTransferItem()  //Подтянем данные о не разнесенной позиции
        buffWare()                   //подтянем склад буфер
        getCountAddress()            //подтянем остатки
        refreshItemProper()          //теперь все остальное
        refreshTransferCard()        //и покажем ка все это
    }

    private fun getCurrentRowTransferItem() {
        for (dr in itemOnPallet) {
            if (dr["id"] != itm.id) continue
            if (dr["iddoc"].toString() == idDoc && currentRowAcceptedItem.isEmpty()) currentRowAcceptedItem = dr
            allCount += dr["Count"].toString().toInt()
        }
    }

    //тут тянем инфу о товаре и его местоположение и кол-во на складе, если таковое есть
    //иначе адрес не задан и кол-во равно 0
    private fun getCountAddress() {
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
                    "Const.date DESC, Const.time DESC, Const.docid DESC), '<не задн>') as AdressMain, " +
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
        textQuery = ss.querySetParam(textQuery, "Item", itm.id)
        textQuery = ss.querySetParam(textQuery, "BufferWarehouse", bufferWarehouse)
        textQuery = ss.querySetParam(textQuery, "MainWarehouse", ss.Const.mainWarehouse)
        val datTab = ss.executeWithReadNew(textQuery) ?: return
        if (datTab.isNotEmpty()) {
            transferItem["BalanceMain"] = datTab[0]["BalanceMain"].toString()
            transferItem["BalanceBuffer"] = datTab[0]["BalanceBuffer"].toString()
            transferItem["AdressMain"] = datTab[0]["AdressMain"].toString()
            transferItem["AdressBuffer"] = datTab[0]["AdressBuffer"].toString()
            transferItem["IsRepeat"] = "0"
        }
    }

    //это приходный адрес, нужен для того, чтобы нормально подтягивалось кол-во товара, который нам надо принять
    private fun buffWare() {
        val textQuery = "SELECT VALUE as val FROM _1sconst (nolock) " +
                "WHERE ID = \$Константа.ОснЦентрСклад "
        val datTable = ss.executeWithReadNew(textQuery) ?: return
        if (datTable.isNotEmpty()) for (DR in datTable) bufferWarehouse = DR["val"].toString()
    }

    private fun refreshItemProper() {

        transferItem["id"] = itm.id
        transferItem["Name"] = itm.name
        transferItem["InvCode"] = itm.invCode
        transferItem["Acticle"] = itm.getAttribute("Артикул").toString()
        transferItem["Count"] = "0"
        transferItem["AcceptCount"] = if (countItemAcc > 0) countItemAcc.toString() else "0"
        transferItem["Price"] = itm.getAttribute("Прих_Цена").toString()
        transferItem["Acceptance"] = "1"
        transferItem["Details"] = itm.getAttribute("КоличествоДеталей").toString()
        transferItem["NowDetails"] = transferItem["Details"].toString()
        transferItem["ToMode"] = "Acceptance"
        transferItem["BindingAdressFlag"] = "0"
        transferItem["SeasonGroup"] = ""
        transferItem["FlagFarWarehouse"] = ""
        transferItem["StoregeSize"] = "0"
        transferItem["BaseUnitID"] = itm.getAttribute("БазоваяЕдиницаШК").toString()
        transferItem["MinParty"] = itm.getAttribute("МинПартия").toString()

        //begin internal command
        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ss.extendID(ss.FEmployer.id, "Спр.Сотрудники")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход2"] = ss.extendID(itm.id, "Спр.Товары")
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
        textQuery = ss.querySetParam(textQuery, "CurrentItem", itm.id)
        units = ss.executeWithReadNew(textQuery) ?: return false

        return true
    }

    private fun refreshTransferCard() {
        //все подсчеты в reactionKey
        tbCoef1.text = "0"
        tbCoef2.text = "0"
        tbCoef3.text = "0"
        tbCount1.text = "0"
        tbCount2.text = "0"
        tbCount3.text = "0"

        itemName.text = transferItem["Name"]            //полное наименование товара
        details.text = (transferItem["Details"] + " ")  //кол-во деталей товара, 0 по дефолту
        shapka.text = (transferItem["InvCode"].toString() + " Редактирование карточки") //код товара
        pricePrih.text = ("Цена: " + transferItem["Price"].toString())  //цена товара

        tbRes0.text = transferItem["Count"].toString() //сумма товара в общей сложности
        tbRes1.text = if (tbCoef1.text.toString().toInt() > 0) (transferItem["Count"].toString()
            .toInt() / tbCoef1.text.toString().toInt()).toString() else "0"
        tbRes2.text = if (tbCoef2.text.toString().toInt() > 0) (transferItem["Count"].toString()
            .toInt() / tbCoef2.text.toString().toInt()).toString() else "0"
        tbRes3.text = if (tbCoef3.text.toString().toInt() > 0) (transferItem["Count"].toString()
            .toInt() / tbCoef3.text.toString().toInt()).toString() else "0"

        tbCount0.text = transferItem["AcceptCount"].toString()
        tbCoef0.text = "1"
        zonaHand.text = (transferItem["AdressMain"].toString()
            .trim() + ": " + transferItem["BalanceMain"].toString() + " шт")
        zonaTech.text = (transferItem["AdressBuffer"].toString()
            .trim() + ": " + transferItem["BalanceBuffer"].toString() + " шт")

        if (units.isNotEmpty()) {
            for (dr in units) {
                var findUnits = false
                for (fdr in fUnits) if (fdr["ID"] == dr["ID"]) { findUnits = true; break }
                if (!findUnits) fUnits.add(dr) //не нашли в табличке единицу, добавим
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
                            (transferItem["AcceptCount"].toString().toInt() / dr["Coef"].toString()
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
                            (transferItem["AcceptCount"].toString().toInt() / dr["Coef"].toString()
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
                            (transferItem["AcceptCount"].toString().toInt() / dr["Coef"].toString()
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
        tbRes0.text = transferItem["Count"].toString() //сумма принимаемого товара в общей сложности

        progressBar2.visibility = INVISIBLE
        tableLayout.visibility = VISIBLE
        tableLayout2.visibility = VISIBLE
        tabCount.visibility = VISIBLE
    }

    override fun reactionBarcode(Barcode : String) : Boolean {
        val helper = Helper()
        val barcodeRes = helper.disassembleBarcode(Barcode)
        val typeBarcode = barcodeRes["Type"].toString()
        val idd = barcodeRes["IDD"].toString()
        if (typeBarcode == "113") {
            if (ss.isSC(idd, "Сотрудники")) {
                ss.FEmployer = RefEmployer()
                val mainInit = Intent(this, MainActivity::class.java)
                startActivity(mainInit)
                finish()
            }
            if (ss.isSC(idd, "Секции")) {
               if (enterAddress(idd)) {
                   deleteRowTransferItem(itemID, countItemAcc.toString())
                   ss.CurrentMode = Global.Mode.TransferRefresh
                   val soGood = Intent(this, TransferMode::class.java)
                   startActivity(soGood)
                   finish()
               }
            }
        } else {
            FExcStr.text = "Сканируйте адрес!"
            return false
        }
        return false
    }

    private fun enterAddress(IDD : String) : Boolean {

        val scanAddress = RefSection()
        if (!scanAddress.foundIDD(IDD)) return false
        //ОПРЕДЕЛИМ РОДИТЕЛЯ И ПРОВЕРИМ СКЛАД
        if (!validAddress(scanAddress.id)) return false

        val checkBind = (transferItem["BindingAdressFlag"].toString().trim().toInt() != 2 && scanAddress.type != 2) //ATDoc.ToWarehouseSingleAdressMode
        val bindingAddress = RefSection()
        checkAddressCard(itemID)
        if (transferItem["BindingAddress"].toString() != "null") {
            bindingAddress.foundID(getWareHouse(transferItem["BindingAddress"].toString(), "idByName"))
        }

        //Провем можно ли выставлять в зону товар, если это не машинная или не ручная зона (хорошие) то нихуя проверять не будем!
        val item = RefItem()
        item.foundID(itemID)
        val itemZone : RefGates = if (scanAddress.type == 2) item.zonaTech
        else item.zonaHand //Для всех остальных видов адреса будем считать ручную зону

        //На обязательный адрес
        if (checkBind && itemZone.selected && bindingAddress.selected)  {
            //у товара есть зона и у обязательного адреса есть зона и нужна выкладка в обязательный адрес
            if (itemZone.id == bindingAddress.id && scanAddress.id != bindingAddress.id) { //Goods.SP6690 zone id,
                //Зоны у товара и обязательного адреса совпадают, а обязательный не совпадает со сканированным, значит хуй вам!
                //иначе нам похуй, т.к. проверки ниже пропустят если все совпадает и ругаться не будут
                FExcStr.text = ("Неверный адрес! Обязателен: " + bindingAddress.name)
                return false
            }
        }
        //Проверка
        if (itemZone.selected && scanAddress.selected) {
            //У обоих задана зона              //задана может и задана, а что если у адреса нет адреса зоны
            if (itemZone.id != scanAddress.adressZone.id && !checkAddressInRange(scanAddress.name, itemZone.id)) {
                FExcStr.text = ("Нельзя! Товар: " + itemZone.name + ", адрес: " + scanAddress.name)
                return false
            }
        } else if (itemZone.selected) {
            //только товар задан
            FExcStr.text = ("Нельзя! Товар: " + itemZone.name)
            return false
        } else if (scanAddress.selected) {
            //Только у адреса задана зона
            FExcStr.text = ("Нельзя! Адрес: " + scanAddress.name)
            return false
        } else if (checkBind && scanAddress.id != bindingAddress.id && !bindingAddress.selected) {
            //нихуя не задано, проверяем по старинке обязательный адрес
            //дополнительно убедимся что у обязательного адреса не задана зона. Потому как если она задана, а у товара - нет,
            //  то этот обязательный адрес мы на хую вертим!
            FExcStr.text = ("Неверный адрес! Обязателен: " + bindingAddress.name)
            return false
        } //else в остальных случая - нам похуй

        //похуй то похуй, на всякий случай надо проверить входит ли отсканенный адрес в диапазон рекомендуемых
        else if (!checkAddressInRange(scanAddress.name, itemZone.id)) {
            FExcStr.text = ("Неверный адрес! Обязателен: " + bindingAddress.name)
            return false
        }
        //ну раз уж вообще ничего нет, положи хотя бы в диапозон
        else if (!scanAddress.adressZone.selected && !checkAddressInRange(scanAddress.name, itemZone.id)) {
            FExcStr.text = ("Неверный адрес! Положите товар в диапозоне $firstAdd..$lastAdd")
            return false
        }


        //--------------------------------------------
        //НИЖЕ НЕТ ПРОВЕРОК АДРЕСОВ И ПРОЧЕЙ ХУЙНИ
        //КОРОЧЕ НИЖЕ УЖЕ ЗАПИСЬ. ЕСЛИ ЧТО-ТО НАДО ПРОВЕРИТЬ, ДЕЛАЙ ЭТО ВЫШЕ

        //Подсосем таблицу с товаром
        var textQuery =
            "SELECT " +
                    "DocAT.lineno_ as lineno_, " +
                    "DocAT.\$АдресПеремещение.Количество as Count, " +
                    "DocAT.\$АдресПеремещение.Состояние1 as State1, " +
                    "DocAT.\$АдресПеремещение.Состояние0 as State0, " +
                    "DocAT.\$АдресПеремещение.Адрес0 as Adress0, " +
                    "DocAT.\$АдресПеремещение.Адрес1 as Adress1, " +
                    "DocAT.\$АдресПеремещение.Дата0 as Date0, " +
                    "DocAT.\$АдресПеремещение.Время0 as Time0, " +
                    "DocAT.\$АдресПеремещение.Док as ACID, " +
                    "DocAT.\$АдресПеремещение.НомерСтрокиДока as Number, " +
                    "DocAT.\$АдресПеремещение.ЕдиницаШК as UnitID, " +
                    "DocAT.\$АдресПеремещение.ФлагОбязательногоАдреса as BindingAdressFlag " +
                    "FROM " +
                    "DT\$АдресПеремещение as DocAT (nolock) " +
                    "WHERE " +
                    "DocAT.iddoc = :Doc " +
                    "and not DocAT.\$АдресПеремещение.Дата0 = :EmptyDate " +
                    "and DocAT.\$АдресПеремещение.Дата1 = :EmptyDate " +
                    "and DocAT.\$АдресПеремещение.Товар = :Item " +
                    "ORDER BY lineno_"
        textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        textQuery = ss.querySetParam(textQuery, "Item", itemID)
        val dt = ss.executeWithReadNew(textQuery) ?: return false

        var count : Int = countItemAcc   //Сколько нужно переместить на полку
        for (dr in dt) {
            val currCount = dr["Count"].toString().toInt()
            var updateCount : Int
            if (currCount <= count) {
                //Тупо апдейтем строчку
                count -= currCount
                updateCount = currCount
            }
            else {
                updateCount = count
                count = 0
            }

            textQuery =
                "UPDATE DT\$АдресПеремещение " +
                        "SET " +
                        "\$АдресПеремещение.Количество = :Count," +
                        "\$АдресПеремещение.Дата1 = :NowDate," +
                        "\$АдресПеремещение.Время1 = :NowTime," +
                        "\$АдресПеремещение.Состояние1 = :State, " +
                        "\$АдресПеремещение.Адрес1 = :Adress1 " +
                        "WHERE " +
                        "DT\$АдресПеремещение .iddoc = :Doc " +
                        "and DT\$АдресПеремещение .lineno_ = :LineNo_"
            textQuery = ss.querySetParam(textQuery, "Count", updateCount)
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            textQuery = ss.querySetParam(textQuery, "Adress1", scanAddress.id)
            textQuery = if (dr["State0"].toString().toInt() == -1 || dr["State0"].toString().toInt() == 0) {
                ss.querySetParam(textQuery, "State", -2)
            } else if (dr["State0"].toString().toInt() == 8) ss.querySetParam(textQuery, "State", 7)
            else ss.querySetParam(textQuery, "State", 2)

            textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
            textQuery = ss.querySetParam(textQuery, "LineNo_", dr["lineno_"].toString().toInt())
            if (!ss.executeWithoutRead(textQuery))  return false

            if (updateCount != currCount) {
                //Нуно создать новую строку
                textQuery =
                    "SELECT max(DT\$АдресПеремещение .lineno_) + 1 as NewLineNo_ " +
                            "FROM DT\$АдресПеремещение (nolock) WHERE  DT\$АдресПеремещение .iddoc = :Doc"
                textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
                val tmpDT = ss.executeWithReadNew(textQuery) ?: return false

                val newLineNo = tmpDT[0]["NewLineNo_"].toString().toInt()

                textQuery =
                    "INSERT INTO DT\$АдресПеремещение VALUES " +
                            "(:Doc, :LineNo_, :Item, :Count, :EmptyID, :Coef, :State0, :State1, :Employer, " +
                            ":Adress0, :Adress1, :NowDate, :EmptyDate, :NowTime, 0, :ACDoc, :Number, " +
                            "0, :BindingAdressFlag, :UnitID); "
                textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
                textQuery = ss.querySetParam(textQuery, "LineNo_", newLineNo)
                textQuery = ss.querySetParam(textQuery, "Item", itemID)
                textQuery = ss.querySetParam(textQuery, "Count", currCount - updateCount)
                textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
                textQuery = ss.querySetParam(textQuery, "Coef", 1)
                textQuery = ss.querySetParam(textQuery, "State0", dr["State0"].toString())
                textQuery = ss.querySetParam(textQuery, "State1", dr["State1"].toString())
                textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
                textQuery = ss.querySetParam(textQuery, "Adress0", dr["Adress0"].toString())
                textQuery = ss.querySetParam(textQuery, "Adress1", dr["Adress1"].toString())
                textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
                textQuery = ss.querySetParam(textQuery, "ACDoc", dr["ACID"].toString())
                textQuery = ss.querySetParam(textQuery, "Number", dr["Number"].toString())
                textQuery = ss.querySetParam(textQuery, "UnitID", dr["UnitID"].toString())
                textQuery = ss.querySetParam(textQuery, "BindingAdressFlag", dr["BindingAdressFlag"].toString())
                if (!ss.executeWithoutRead(textQuery)) return false
            }
            if (count == 0) break
        }

        //begin internal command
        val dataMapWrite : MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ss.extendID(ss.FEmployer.id, "Спр.Сотрудники")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход2"] = ss.extendID(itemID, "Спр.Товары")
        dataMapWrite["Спр.СинхронизацияДанных.ДокументВход"] = ss.extendID(iddoc, "АдресПеремещение")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = "PutItem (Выложил на полку)"
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход2"] = scanAddress.id
        if (!execCommandNoFeedback("Internal", dataMapWrite)) return false
        //end internal command

        return true
    }

    private fun validAddress(id : String) : Boolean {
        var textQuery =
            "DECLARE @parent varchar(9); " +
                    "DECLARE @curid varchar(9); " +
                    "SET @curid = :Adress1; " +
                    "WHILE not @curid = '     0   ' BEGIN " +
                    "SET @parent = @curid; " +
                    "SELECT @curid = parentid FROM \$Спр.Секции (nolock) WHERE id = @curid; " +
                    "END; " +
                    "SELECT \$Спр.Секции.Склад as Warehouse, \$Спр.Секции.ЗапретитьВыкладкуБезШК as StopWithoutBarcode " +
                    "FROM \$Спр.Секции (nolock) WHERE id = @parent; "
        textQuery = ss.querySetParam(textQuery, "Adress1", id)
        val datTab = ss.executeWithReadNew(textQuery) ?: return false

        if (inputWarehouse != datTab[0]["Warehouse"].toString()) { FExcStr.text = "Адрес другого склада!"; return false }

        if (datTab[0]["StopWithoutBarcode"].toString().toInt() == 1)  { FExcStr.text = "Нельзя выкладывать без ШК!"; return false }

        return true
    }

    private fun checkAddressInRange(urAdd : String, id : String) : Boolean {
        var textQuery =
            "select " +
                    "ref.descr name, " +
                    "(select top 1 \$Спр.Секции.ЗонаАдресов from \$Спр.Секции as ref2 (nolock)" +
                    " where ref2.descr > ref.descr and ref2.isfolder = 2" +
                    " and ref2.ismark = 0" +
                    " and ref2.\$Спр.Секции.ЗонаАдресов = :zone  order by descr) nextZone " +  //это изменённая строка при которой выводятся только начало и конец зоны
                    "from \$Спр.Секции as ref (nolock) " +
                    "where " +
                    "ref.ismark = 0 " +
                    "and ref.\$Спр.Секции.ЗонаАдресов = :zone " +
                    "order by ref.descr"
        textQuery = ss.querySetParam(textQuery, "zone", id)
        val dt = ss.executeWithReadNew(textQuery) ?: return false

        firstAdd = dt[0]["name"].toString().trim()
        val sizeDT = dt.count() - 1
        lastAdd = dt[sizeDT]["name"].toString().trim()

        for (dr in dt) {
            if (urAdd.trim() == dr["name"].toString().trim()) return true
        }


        return false
    }

    private fun checkAddressCard(itemID: String) {
        //ОПРЕДЕЛИМ РЕКОМЕНДУЕМЫЙ АДРЕС
        var textQuery =
            "SELECT top 1 " +
                    " left(const.value, 9) as Adress, " +
                    " section.descr as AdressName " +
                    "FROM _1sconst as const(nolock) " +
                    "LEFT JOIN \$Спр.Секции as Section (nolock) " +
                    "ON Section.id = left(value, 9) " +
                    "WHERE " +
                    "const.id = \$Спр.ТоварныеСекции.Секция " +
                    "and const.date <= :NowDate " +
                    "and const.OBJID in (" +
                    "SELECT id FROM \$Спр.ТоварныеСекции (nolock) " +
                    "WHERE " +
                    "\$Спр.ТоварныеСекции.Склад = :Warehouse " +
                    "and parentext = :Item) " +
                    "ORDER BY " +
                    "const.date DESC, const.time DESC, const.docid DESC "
        textQuery = ss.querySetParam(textQuery, "Item", itemID)//itm.id)
        textQuery = ss.querySetParam(textQuery, "Warehouse", inputWarehouse)
        val recTab = ss.executeWithReadNew(textQuery) ?: return

        if (recTab.isNotEmpty()) {
            transferItem["BindingAddress"] = recTab[0]["AdressName"].toString().trim()
        } else {
            FExcStr.text = "У товара не задана ручная зона!"
            FExcStr.setTextColor(Color.RED)
        }

    }

    override fun reactionKey(keyCode : Int, event: KeyEvent?) : Boolean {
        if (keyCode == 4) {
            if (!ibsLockOut("int_ref_Товары_" + (itm.id + "_unit"))) {
                //если не удалось разблокировать то просто напишем об этом
                FExcStr.text = "Не удалось разблокировать товар!"
            }
            val backTel = Intent(this, TransferMode::class.java)
            startActivity(backTel)
            finish()
            return true
        }
        if (ss.helper.whatDirection(keyCode) == "Right") {
            clickVoice()
            val moreWindow = Intent(this, TransferCardState::class.java)
            moreWindow.putExtra("itemID", itemID)
            moreWindow.putExtra("count", countItemAcc)
            startActivity(moreWindow)
            finish()
            return true
        }
        if (ss.helper.whatDirection(keyCode) == "Left") {
            clickVoice()
            val moreWindow = Intent(this, TransferCardRec::class.java)
            moreWindow.putExtra("itemID", itemID)
            moreWindow.putExtra("count", countItemAcc)
            startActivity(moreWindow)
            finish()
            return true
        }
        return false
    }
}