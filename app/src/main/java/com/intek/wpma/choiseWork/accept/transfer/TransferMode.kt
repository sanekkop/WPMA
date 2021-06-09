package com.intek.wpma.choiseWork.accept.transfer

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color.*
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.Global
import com.intek.wpma.MainActivity
import com.intek.wpma.R
import com.intek.wpma.choiseWork.Menu
import com.intek.wpma.helpers.Helper
import com.intek.wpma.model.Model
import com.intek.wpma.ref.*
import kotlinx.android.synthetic.main.activity_transfer_mode.*
import kotlinx.android.synthetic.main.activity_transfer_rec.*

open class TransferMode : BarcodeDataReceiver() {

    var currentLine : Int = 0
    val model = Model()
    val itm = RefItem()
    var fZone = RefSection()
    private var fName = ""
    private var fID = ""
    private var helper = Helper()
    private var pall = RefPalleteMove()
    private var ref = RefEmployer()
    private var idDocItm = ""
    private var count = 0
    private val widArr : Array<Double> = arrayOf(0.3, 0.2, 0.15, 0.35)
    private val strArr : Array<String> = arrayOf("Инв. код", "Кол-во", "Коэф", "Адрес")
    var transferItem : MutableMap<String, String> = mutableMapOf()
    private var rangItem : MutableList<MutableMap<String, String>> = mutableListOf()
    private var docEmployer : MutableList<MutableMap<String, String>> = mutableListOf()

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
                        barcode = intent.getStringExtra("data")!!
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
            //значит вышли из других страниц и надо просто обновить активити
            //ss.CurrentMode == Global.Mode.Acceptance
            //refreshActivity()
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
        var inputWarehouse : String = ""
        var outputWarehouse : String = ""
        var outputZone : MutableList<MutableMap<String, String>> = mutableListOf()
        var itemOnShelf : MutableList<MutableMap<String, String>> = mutableListOf()
        var itemOnPallet : MutableList<MutableMap<String, String>> = mutableListOf()
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transfer_mode)
        title = ss.title

        customTable(this, 3, strArr, widArr, headTab, "head")

        head.text = (" Разнос (ТЕЛЕЖКА) ")
        head.setTextColor(BLACK)

        when (ss.CurrentMode) {
            Global.Mode.TransferInit -> completeTransferInitialize(outputWarehouse, inputWarehouse)
            else -> transferInit()
        }
        //фигня чтобы скрол не скролился
        itemOn.setOnKeyListener { _, keyCode, event ->
            try {
                if (event.action == MotionEvent.ACTION_DOWN && ss.helper.whatDirection(keyCode) in listOf("Down", "Up") && itemOnPallet.isNotEmpty()) {
                    reactionKeyLocal1(keyCode)
                }
                else if (event.action == MotionEvent.ACTION_DOWN) reactionKey(keyCode, event) else true
            } catch (e: Exception) {
                true
            }
        }
    }

    private fun transferInit() {
        //смотрим, висит ли на нем документ (вдруг покурить вышел перед тем как разнести или вылетело чего)
        var textQuery = "SELECT " +
                "journ.iddoc as IDDOC, " +
                "FromWarehouse.descr as FromWarehouseName, " +
                "FromWarehouse.id as FromWarehouse, " +
                "ToWarehouse.descr as ToWarehouseName, " +
                "ToWarehouse.id as ToWarehouse " +
                "FROM " +
                "_1sjourn as journ (nolock) " +
                "LEFT JOIN DH\$АдресПеремещение as DocAT (nolock) " +
                "ON DocAT.iddoc = journ.iddoc " +
                "LEFT JOIN \$Спр.Склады as FromWarehouse (nolock) " +
            "ON FromWarehouse.id = DocAT.\$АдресПеремещение.Склад " +
                "LEFT JOIN \$Спр.Склады as ToWarehouse (nolock) " +
                "ON ToWarehouse.id = DocAT.\$АдресПеремещение.СкладПолучатель " +
                "WHERE " +
                "journ.date_time_iddoc < '19800101Z' " +
                "and journ.\$Автор = :Employer " +
                "and journ.iddocdef = \$АдресПеремещение " +
                "and DocAT.\$АдресПеремещение.ТипДокумента = 2 " +
                "and journ.ismark = 0 " +
                "and journ.iddoc in (" +
                "SELECT right(\$Спр.СинхронизацияДанных.ДокументВход , 9) " +
                "FROM \$Спр.СинхронизацияДанных (nolock INDEX=VI" + ss.getSync("Спр.СинхронизацияДанных.ДокументВход").substring(2) + ")" +
                " WHERE not descr = 'Internal' and \$Спр.СинхронизацияДанных.ФлагРезультата in (1,2))"
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        val docCheck = ss.executeWithReadNew(textQuery) ?: return

        //Тут сверху и снизу - два почти идентичных запроса. Один проверяет не в обработке ли документ,
        //а второй (ниже) - подтягивает его
        if (docCheck.isEmpty()) ss.excStr = "Подождите, обрабатывается предыдущий документ!"

        textQuery = "SELECT " +
                "journ.iddoc as IDDOC, " +
                "FromWarehouse.descr as FromWarehouseName, " +
                "FromWarehouse.id as FromWarehouse, " +
                "ToWarehouse.descr as ToWarehouseName, " +
                "ToWarehouse.id as ToWarehouse, " +
                "ToWarehouse.\$Спр.Склады.ОдноадресныйРежим as ToWarehouseSingleAdressMode " +
                "FROM " +
                "_1sjourn as journ (nolock) " +
                "LEFT JOIN DH\$АдресПеремещение as DocAT (nolock) " +
                "ON DocAT.iddoc = journ.iddoc " +
                "LEFT JOIN \$Спр.Склады as FromWarehouse (nolock) " +
                "ON FromWarehouse.id = DocAT.\$АдресПеремещение.Склад " +
                "LEFT JOIN \$Спр.Склады as ToWarehouse (nolock) " +
                "ON ToWarehouse.id = DocAT.\$АдресПеремещение.СкладПолучатель " +
                "WHERE " +
                "journ.date_time_iddoc < '19800101Z' " +
                "and journ.\$Автор = :Employer " +
                "and journ.iddocdef = \$АдресПеремещение " +
                "and DocAT.\$АдресПеремещение.ТипДокумента = 2 " +
                "and journ.ismark = 0 "
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        docEmployer = ss.executeWithReadNew(textQuery) ?: return

        //что-то есть, значит покажем
        if (docEmployer.isNotEmpty()) {

            iddoc = docEmployer[0]["IDDOC"].toString()

            outputWarehouse = docEmployer[0]["FromWarehouse"].toString()
            inputWarehouse = docEmployer[0]["ToWarehouse"].toString()

            getItem()
            //Подтянем нужную зону для разноса
            textQuery = "DECLARE @curdate DateTime; " +
                    "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock) ; " +
                    "SELECT TOP 1 " +
                    "RegAC.\$Рег.АдресОстаткиПоступления.Адрес as Adress1 , " +
                    "max(Gate.ID ) as GateID , " +
                    "max(Gate.Descr ) as GateName, " +
                    "max(Gate.\$Спр.Ворота.КоличествоСотрудников ) as CountEmployer " +
                    "FROM " +
                    "RG\$Рег.АдресОстаткиПоступления as RegAC (nolock) " +
                    "LEFT JOIN \$Спр.ЗоныВорот as ZoneGate (nolock) " +
                    "ON RegAC.\$Рег.АдресОстаткиПоступления.Адрес = ZoneGate.\$Спр.ЗоныВорот.Секция " +
                    "LEFT JOIN \$Спр.Ворота as Gate (nolock) " +
                    "ON (ZoneGate.ParentExt = Gate.ID) " +
                    "WHERE " +
                    "RegAC.period = @curdate " +
                    "and RegAC.\$Рег.АдресОстаткиПоступления.ТипДействия = 2 " +
                    "and RegAC.\$Рег.АдресОстаткиПоступления.Склад = :Warehouse " +
                    "and ((Gate.ID is NULL) OR (Gate.\$Спр.Ворота.ВРазносеСотрудников < Gate.\$Спр.Ворота.КоличествоСотрудников )) " +

                    "GROUP BY " +
                    "RegAC.\$Рег.АдресОстаткиПоступления.Адрес " +
                    "ORDER BY " +
                    "CASE WHEN (max(Gate.ID ) = :EmptyRef)OR(max(Gate.ID ) is NULL) THEN 999999 ELSE max(Gate.\$Спр.Ворота.Приоритет ) END , " +
                    "sum(1) "
            textQuery = ss.querySetParam(textQuery, "EmptyRef", ss.getVoidID())
            textQuery = ss.querySetParam(textQuery, "Warehouse", outputWarehouse)
            outputZone = ss.executeWithReadNew(textQuery) ?: return

            fZone.foundID(outputZone[0]["Adress1"].toString())
            fName = outputZone[0]["GateName"].toString()
            fID = outputZone[0]["GateID"].toString()

            head.text = (" Разнос (ТЕЛЕЖКА) "+ outputZone[0]["GateName"].toString().trim() + " " + fZone.name.trim())

            if (ss.CurrentMode == Global.Mode.TransferMode) {
                //теперь укажем, что мы в зоне работаем
                textQuery =
                    "UPDATE \$Спр.Ворота " +
                            "SET " +
                            "\$Спр.Ворота.ВРазносеСотрудников = (SELECT TOP 1 \$Спр.Ворота.ВРазносеСотрудников + 1 FROM \$Спр.Ворота WHERE id = :id) " +
                            "WHERE " +
                            "id = :id ; "
                textQuery = ss.querySetParam(textQuery, "id", fID)
                if (!ss.executeWithoutRead(textQuery)) return

                ss.CurrentMode = Global.Mode.TransferRefresh
            }
            refreshActivity()

        } else {
            ss.CurrentMode = Global.Mode.TransferInit
            //нихера нет, значит пусть идет выбирать склады
            val transferInit = Intent(this, TransferInitialize::class.java)
            startActivity(transferInit)
            finish()
            }
        }

    //выбрали склады, видим зону разноса, идем к палете и пикаем её
    private fun rbTransfer(barCode : String) : Boolean {
        ss.FEmployer.refresh()
        if (barCode == ss.FPallet.palleteBarcode) {
          //Паллету можно добавлять только когда весь товар разнесен
          if (itemOnPallet.count() > 0) {
              badVoice()
              FExcStr.text = "В телеге есть товар, добавлять нельзя!"
              return false
          }
          //подтянем все что принято и что лежит в этой зоне и на этой паллете
          var textQuery = "DECLARE @curdate DateTime; " +
          "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock) ; " +
          "SELECT " +
              "RegAC.\$Рег.АдресОстаткиПоступления.Товар as ItemID, " +
              "sum(DocAP.\$АдресПоступление.Количество ) as Count, " +
              "sum(RegAC.\$Рег.АдресОстаткиПоступления.Количество ) as ACount " +
          "FROM " +
              "RG\$Рег.АдресОстаткиПоступления as RegAC (nolock) " +
              "INNER JOIN DT\$АдресПоступление as DocAP (nolock) " +
                  "ON (RegAC.\$Рег.АдресОстаткиПоступления.Док = DocAP.IDDOC ) " +
                  "and (RegAC.\$Рег.АдресОстаткиПоступления.Товар = DocAP.\$АдресПоступление.Товар ) " +
              "INNER JOIN \$Спр.ПеремещенияПаллет as RefPallet (nolock) " +
                  "ON (DocAP.\$АдресПоступление.Паллета = RefPallet.ID) " +
          "WHERE " +
              "RegAC.period = @curdate " +
              "and RegAC.\$Рег.АдресОстаткиПоступления.Адрес = :AdressID " +
              "and RegAC.\$Рег.АдресОстаткиПоступления.Склад = :Warehouse " +
              "and RefPallet.\$Спр.ПеремещенияПаллет.ШКПаллеты = :BarcodePallet " +
              "and RegAC.\$Рег.АдресОстаткиПоступления.ТипДействия = 2 " +
              "and NOT DocAP.\$АдресПоступление.Дата0 = :EmptyDate " +
              "and RegAC.\$Рег.АдресОстаткиПоступления.Количество > 0 " +
          "GROUP BY " +
              "RegAC.\$Рег.АдресОстаткиПоступления.Товар "
          textQuery = ss.querySetParam(textQuery, "Warehouse", outputWarehouse)
          textQuery = ss.querySetParam(textQuery, "AdressID", fZone.id)
          textQuery = ss.querySetParam(textQuery, "BarcodePallet", barCode)
          textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
          val dataTab = ss.executeWithReadNew(textQuery) ?: return false

          if (dataTab.count() == 0) {
              badVoice()
              FExcStr.text = "С таким ШК паллета в зоне не найдена!"
              return false
          }
          //а теперь надо добавить весь товар с этой паллеты в телегу
          var lastNorm = dataTab.count()
          for (i in 0..dataTab.count()) {
              if (!addParty(dataTab[i]["ItemID"].toString(), dataTab[i]["Count"].toString().trim().toInt())) break
              lastNorm = i
          }
          if (lastNorm < dataTab.count()) {
              for (i in 0..lastNorm) {
                  deleteRowTransferItem(dataTab[i]["ItemID"].toString(), dataTab[i]["Count"].toString())
              }
              return false
          }
          return true
      }
        return true
  }

    //чтобы скуль знал, что весь товар с палеты теперь у нас в телеге
    private fun addParty(ItemID : String, InPartyCount : Int) : Boolean {
        var inPartyCount = InPartyCount
        //Подсосем инвентарный код, чтобы плевать его в сообщения
        var textQuery = "SELECT " +
                "Goods.\$Спр.Товары.ИнвКод as InvCode, " +
                "ISNULL(Package.Coef, 1) as Coef " +
                "FROM " +
                "\$Спр.Товары as Goods (nolock) " +
                "LEFT JOIN ( " +
                "SELECT " +
                "Units.parentext as id, " +
                "min(Units.\$Спр.ЕдиницыШК.Коэффициент ) as Coef " +
                "FROM " +
                "\$Спр.ЕдиницыШК as Units (nolock) " +
                "WHERE " +
                "Units.parentext = :ItemID " +
                "and Units.\$Спр.ЕдиницыШК.ОКЕИ = :OKEIPackage " +
                "and Units.ismark = 0 " +
                "GROUP BY " +
                "Units.parentext " +
                ") as Package " +
                "ON Package.id = Goods.id " +
                "WHERE " +
                "Goods.id = :ItemID "
        textQuery = ss.querySetParam(textQuery, "ItemID", ItemID)
        textQuery = ss.querySetParam(textQuery, "OKEIPackage", model.okeiPackage)
        val dateTab = ss.executeWithReadNew(textQuery) ?: return false

        val invCode = dateTab[0]["InvCode"].toString().trim()
        val coef = ss.helper.byeTheNull(dateTab[0]["Coef"].toString().trim()).toInt()

        //ПРОБИВАЕМ ОСТАТКИ ПО АДРЕС ПОСТУПЛЕНИЕ
        textQuery =
            "DECLARE @curdate DateTime; " +
                    "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem; " +
                    "SELECT " +
                    ":ItemID as ID, " +
                    "max(DocAC.lineno_) as lineno_, " +
                    "min(DocAC.\$АдресПоступление.ЕдиницаШК ) as UnitID, " +
                    "min(DocAC.\$АдресПоступление.Состояние0 ) as State0, " +
                    "min(DocAC.\$АдресПоступление.Адрес0 ) as Adress0, " +
                    "DocAC.iddoc as ACID, " +
                    "min(RegAC.Count) as Count " +
                    "FROM " +
                    "DT\$АдресПоступление as DocAC (nolock) " +
                    "INNER JOIN ( " +
                    "SELECT " +
                    "sum(RegAC.\$Рег.АдресОстаткиПоступления.Количество ) as Count, " +
                    "RegAC.\$Рег.АдресОстаткиПоступления.Док as ACID " +
                    "FROM " +
                    "RG\$Рег.АдресОстаткиПоступления as RegAC (nolock) " +
                    "WHERE " +
                    "RegAC.period = @curdate " +
                    "and RegAC.\$Рег.АдресОстаткиПоступления.Товар = :ItemID " +
                    "and RegAC.\$Рег.АдресОстаткиПоступления.Склад = :Warehouse " +
                    "GROUP BY " +
                    "RegAC.\$Рег.АдресОстаткиПоступления.Товар , " +
                    "RegAC.\$Рег.АдресОстаткиПоступления.Док " +
                    "HAVING " +
                    "sum(RegAC.\$Рег.АдресОстаткиПоступления.Количество ) <> 0 " +
                    ") as RegAC " +
                    "ON DocAC.iddoc = RegAC.ACID " +
                    "WHERE " +
                    "DocAC.\$АдресПоступление.Товар = :ItemID " +
                    "and DocAC.\$АдресПоступление.Состояние0 = 1 " +
                    "GROUP BY " +
                    "DocAC.iddoc "
        textQuery = ss.querySetParam(textQuery, "ItemID", ItemID)
        textQuery = ss.querySetParam(textQuery, "Warehouse", outputWarehouse)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        val addPartyDT = ss.executeWithReadNew(textQuery) ?: return false

        //ПРОБИВАЕМ СКОЛЬКО УЖЕ РАЗНОСИТСЯ
        //Для простоты будем считать что на каждого сотрудника максимум по одному документу на 80-ом,
        //  вообщем то, исходя из логике движка - так и есть
        textQuery = "SELECT " +
                "Employers.id as EmployerID, " +
                "Employers.descr as Employer, " +
                "DocAT.\$АдресПеремещение.Количество  as Count, " +
                "substring(DocAT.\$АдресПеремещение.Док , 5, 9) as ACID " +
                "FROM " +
                "_1sjourn as Journ (nolock) " +
                "INNER JOIN DT\$АдресПеремещение as DocAT (nolock) " +
                "ON DocAT.iddoc = Journ.iddoc " +
                "LEFT JOIN DH\$АдресПеремещение as DocATHeader (nolock) " +
                "ON DocATHeader.iddoc = Journ.iddoc " +
                "LEFT JOIN \$Спр.Сотрудники as Employers (nolock) " +
                "ON Employers.id = Journ.\$Автор " +
                "WHERE " +
                "Journ.date_time_iddoc < '19800101Z' " +
                "and not Journ.\$Автор = :EmptyID " +
                "and DocAT.\$АдресПеремещение.Товар = :ItemID " +
                "and DocATHeader.\$АдресПеремещение.ТипДокумента = 2 " +
                //"GROUP BY Journ.iddoc " +
                "ORDER BY Employers.id "
        textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
        textQuery = ss.querySetParam(textQuery, "ItemID", ItemID)
        val transferDT = ss.executeWithReadNew(textQuery) ?: return false

        var strTransfer = ""
        var countTransfer = 0
        if (transferDT.count() > 0) {
            var currEmployerID = transferDT[0]["EmployerID"].toString()
            var currEmployerFIO = helper.getShortFIO(transferDT[0]["Employer"].toString())
            var currCountTransfer = 0
            for (i in 0..transferDT.count()) {
                if (currEmployerID != transferDT[i]["EmployerID"].toString()) {
                    strTransfer = ", " + currEmployerFIO + " " + getStrPackageCount(currCountTransfer, coef)
                    currCountTransfer = 0
                    currEmployerID = transferDT[i]["EmployerID"].toString()
                    currEmployerFIO = helper.getShortFIO(transferDT[i]["Employer"].toString())
                }
                countTransfer += transferDT[i]["Count"].toString().trim().toInt()
                currCountTransfer += transferDT[i]["Count"].toString().trim().toInt()

                if (currEmployerID != ss.FEmployer.id && currCountTransfer > 0) {
                    //нельзя разносить никому, кроме самого авторизованного кента
                        badVoice()
                    FExcStr.text = ("$invCode нельзя взять! Уже разносится $currEmployerFIO")
                    return false
                }
            }
            strTransfer += (", " + currEmployerFIO + " " + getStrPackageCount(currCountTransfer, coef))
            strTransfer += ("Уже в разносе: " + strTransfer.substring(2))
        }

        //ПОДСОСЕМ ВАЛОВЫЕ ОСТАТКИ
        textQuery = "DECLARE @curdate DateTime; " +
                "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock); " +
                "SELECT " +
                ":ItemID as ID, " +
                "sum(RegAC.\$Рег.АдресОстаткиПоступления.Количество ) as Count, " +
                "sum(CASE WHEN " +
                "RegAC.\$Рег.АдресОстаткиПоступления.ТипДействия = 1 " +
                "THEN RegAC.\$Рег.АдресОстаткиПоступления.Количество ELSE 0 END) as NACount, " +
                "sum(CASE WHEN " +
                "RegAC.\$Рег.АдресОстаткиПоступления.ТипДействия = 2 " +
                "THEN RegAC.\$Рег.АдресОстаткиПоступления.Количество ELSE 0 END) as ACount " +
                "FROM " +
                "RG\$Рег.АдресОстаткиПоступления as RegAC (nolock) " +
                "WHERE " +
                "RegAC.period = @curdate " +
                "and RegAC.\$Рег.АдресОстаткиПоступления.Товар = :ItemID " +
                "and RegAC.\$Рег.АдресОстаткиПоступления.Склад = :Warehouse " +
                "GROUP BY " +
                "RegAC.\$Рег.АдресОстаткиПоступления.Товар "
        textQuery = ss.querySetParam(textQuery, "ItemID", ItemID)
        textQuery = ss.querySetParam(textQuery, "Warehouse", outputWarehouse)
        val tabDat = ss.executeWithReadNew(textQuery) ?: return false

        if (tabDat.count() == 0) {
            badVoice()
            FExcStr.text = ("$invCode: 0 м., нет в приемке. $strTransfer")
            return false
        }
        val allNACount = tabDat[0]["NACount"].toString().substringBefore(".").toInt()
        var allACount = tabDat[0]["ACount"].toString().substringBefore(".").toInt()
        //var allCount = tabDat[0]["Count"].toString().trim().toInt()

        //ПОЕХАЛИ ОПЯТЬ ПРИЕМКУ РАСЧИТЫВАТЬ
        if (allNACount > 0) {
            //часть товара не принята
                badVoice()
            FExcStr.text = (invCode + ": 0 м., принят неполностью - " +
                    getStrPackageCount(allACount, coef) + " из " +
                    getStrPackageCount(allNACount + allACount, coef) + ". " + strTransfer)
            return false
        }

        val countAdd = allACount - countTransfer //Осталось разнести
        if (countAdd <= 0) {
            FExcStr.text = ("$invCode: 0 м.. $strTransfer")
            return false
        }
        if (countAdd < InPartyCount) {
            FExcStr.text = ("Нет столько к разносу! Доступно " + getStrPackageCount(countAdd, coef) + " " + strTransfer)
            return false
        }

        //Весь товар принят! Можно добавлять его!
        /////////////////////////////////////////////////////
        //А ТЕПЕРЬ ПИШЕМ ЭТО ГОВНО В ДОКУМЕНТ

        //ОПРЕДЕЛЯЕМ ОСТАТКИ
        textQuery =
            "DECLARE @curdate DateTime; " +
                    "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock); " +
                    "SELECT " +
                    "CAST(sum(\$Рег.ОстаткиТоваров.ОстатокТовара ) as int) as Balance " +
                    "FROM " +
                    "RG\$Рег.ОстаткиТоваров (nolock) " +
                    "WHERE " +
                    "period = @curdate " +
                    "and \$Рег.ОстаткиТоваров.Товар = :Item " +
                    "and \$Рег.ОстаткиТоваров.Склад = :Warehouse " +
                    "GROUP BY \$Рег.ОстаткиТоваров.Товар "
        textQuery = ss.querySetParam(textQuery, "Item", addPartyDT[0]["ID"].toString())
        textQuery = ss.querySetParam(textQuery, "Warehouse", inputWarehouse)
        val dtReg = ss.executeWithReadNew(textQuery) ?: return false

        //этот запрос в принципе висит отдельной функцией для TransferCardRec, но пусть будет
        //ОПРЕДЕЛИМ РЕКОМЕНДУЕМЫЙ АДРЕС
        textQuery =
            "SELECT top 1 " +
                    " left(value, 9) as Adress1 " +
                    "FROM _1sconst (nolock) " +
                    "WHERE " +
                    "id = \$Спр.ТоварныеСекции.Секция " +
                    "and date <= :NowDate " +
                    "and OBJID in (" +
                    "SELECT id FROM \$Спр.ТоварныеСекции (nolock) " +
                    "WHERE " +
                    "\$Спр.ТоварныеСекции.Склад = :Warehouse " +
                    "and parentext = :Item)" +
                    "ORDER BY " +
                    "date DESC, time DESC, docid DESC "
        textQuery = ss.querySetParam(textQuery, "Item", ItemID)
        textQuery = ss.querySetParam(textQuery, "Warehouse", inputWarehouse)
        var tableDat = ss.executeWithReadNew(textQuery) ?: return false

        val address1 = if (tableDat.count() == 0) ss.getVoidID() else tableDat[0]["Adress1"].toString()

        //ДОБАВЛЯЕМ НОВУЮ или даже НОВЫЕ СТРОКИ
        textQuery =
            "SELECT ISNULL(max(DT\$АдресПеремещение .lineno_) + 1, 1) as LineNo_ " +
                    "FROM DT\$АдресПеремещение (nolock) " +
                    "WHERE DT\$АдресПеремещение .iddoc = :Doc"
        textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
        tableDat.clear()
        tableDat = ss.executeWithReadNew(textQuery) ?: return false
       // itemOnPallet = ss.executeWithReadNew(textQuery) ?: return false

        var lineNo = tableDat[0]["LineNo_"].toString().trim().toInt()

        var strAccepted = ""

        for (dr in addPartyDT) {
            if (allACount == 0) continue //тут должно быть break, но впадлу менять перед отпуском, т.к. 100% уверенности - нет

            var count = dr["Count"].toString().substringBefore(".").toInt()    //По данной строке документа висит столько
            if (count > allACount) {
                count = allACount; allACount = 0
            } else allACount -= count

            countTransfer = 0                            //Сколько разносится по данному документу
//            val tmpDR = transferDT.select("ACID = '" + dr["ACID"].toString() + "'")
//            for (tmpDr in tmpDR) countTransfer += tmpDr["Count"]

            if (count <= countTransfer) {
                countTransfer -= count; continue
            }   //Все разносится, добалвять не будем

            count -= countTransfer //Уменьшим на разносимое количество
            if (count >= inPartyCount) count = inPartyCount
            inPartyCount -= count

            //countTransfer = 0
            var balance = 0
            strAccepted += " + " + getStrPackageCount(count, coef) //Формируем строку вывода
            textQuery =
                "INSERT INTO DT\$АдресПеремещение VALUES " +
                        "(:Doc, :LineNo_, :Item, :Count, :EmptyID, :Coef, :State0, 2, :Employer, " +
                        ":Adress0, :Adress1, :NowDate, :EmptyDate, :NowTime, 0, :ACDoc, :Number, " +
                        ":SampleCount, :BindingAdressFlag, :UnitID); "
            textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
            textQuery = ss.querySetParam(textQuery, "LineNo_", lineNo)
            textQuery = ss.querySetParam(textQuery, "Item", dr["ID"].toString())
            textQuery = ss.querySetParam(textQuery, "Count", count)
            textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
            textQuery = ss.querySetParam(textQuery, "Coef", 1)
            textQuery = ss.querySetParam(textQuery, "State0", dr["State0"].toString())
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            textQuery = ss.querySetParam(textQuery, "Adress0", dr["Adress0"].toString())
            textQuery = ss.querySetParam(textQuery, "Adress1", address1)
            textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
            textQuery = ss.querySetParam(textQuery, "ACDoc", ss.extendID(dr["ACID"].toString(), "АдресПоступление"))
            textQuery = ss.querySetParam(textQuery, "Number", dr["lineno_"].toString())
            textQuery = ss.querySetParam(textQuery, "UnitID", dr["UnitID"].toString())
            textQuery = ss.querySetParam(textQuery, "SampleCount", 0)
            if (dtReg.count() > 0) {
                if (dtReg[0]["Balance"].toString().trim().toInt() != 0) balance = 1
                textQuery = ss.querySetParam(textQuery, "BindingAdressFlag", balance)
            } else textQuery = ss.querySetParam(textQuery, "BindingAdressFlag", 0)

            if (!ss.executeWithoutRead(textQuery)) return false
            lineNo++
            if (InPartyCount == 0) break
        }
        //strAccepted = strAccepted.substring(3)
        FExcStr.text = (invCode + ": " + getStrPackageCount(countAdd, coef))
        //+ ", ДОБАВЛЕНО: " + strAccepted + ". " + strTransfer;

        //begin internal command
        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ss.extendID(ss.FEmployer.id, "Спр.Сотрудники")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход2"] = ss.extendID(ItemID, "Спр.Товары")
        dataMapWrite["Спр.СинхронизацияДанных.ДокументВход"] = ss.extendID(iddoc, "АдресПеремещение")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = "LoadItem (Загрузил в тележку)"
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход2"] = countAdd.toString()  //Всего добавлено
        if (!execCommandNoFeedback("Internal", dataMapWrite)) return false
        //end internal command
        getItem()

        return refreshActivity()  //рефрешим табличку
    }

    private fun deleteRowTransferItem() : Boolean {
        //удалять из телеги нельзя, пикнул паллету мучийся
        FExcStr.text = "Нельзя удалять товар из телеги"
        return false
    }

    fun deleteRowTransferItem(itemID : String, delCount : String) {

        var textQuery = "DELETE FROM DT\$АдресПеремещение " +
        "WHERE DT\$АдресПеремещение .iddoc = :Doc " +
                "and DT\$АдресПеремещение .\$АдресПеремещение.Товар = :Item " +
                "and DT\$АдресПеремещение .\$АдресПеремещение.Дата1 = :EmptyDate "
        textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
        textQuery = ss.querySetParam(textQuery, "Item", itemID)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        if (!ss.executeWithoutRead(textQuery)) return
        //begin internal command
        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ss.extendID(ss.FEmployer.id, "Спр.Сотрудники")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход2"] = ss.extendID(itemID, "Спр.Товары")
        dataMapWrite["Спр.СинхронизацияДанных.ДокументВход"] = ss.extendID(iddoc, "АдресПеремещение")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = "RemovedItem (Удалил из тележки)"
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход2"] = delCount
        if (!execCommandNoFeedback("Internal", dataMapWrite)) return
        //end internal command
        getItem()  //рефрешим табличку
    }

    private fun getStrPackageCount(count: Int, coef: Int): String {
        var count0 = count
        var result = "$count0 ШТУК" //По умолчанию штуки
        if (coef > 1) {
            if ((count0 / coef) * coef == count0) {
                //Делится по коробкам
                count0 /= coef
                result = "$count0 м."
            }
        }
        return result
    }

    fun checkAddress() {
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
        textQuery = ss.querySetParam(textQuery, "Item", itm.id)
        textQuery = ss.querySetParam(textQuery, "Warehouse", inputWarehouse)
        val recTab = ss.executeWithReadNew(textQuery) ?: return

        if (recTab.isEmpty()) {
            stateRange.text = "У товара не задана ручная зона!"
            stateRange.setTextColor(RED)
        } else {
            stateRange.text = ("Ручная зона \n " + itm.zonaHand.fName)
            stateRange.setTextColor(BLACK)
        }
        rangItem = itm.zonaHand.ranges

        for ((i,_) in rangItem.withIndex()) {
            range.text = (
                    itm.zonaHand.ranges[i]["First"].toString().trim() + " .. " +
                            itm.zonaHand.ranges[i]["Last"].toString().trim()
                    )
        }
    }

    fun completeTransferInitialize(IDFrom : String, IDIn : String) : Boolean {
        //заполним склад выкладки
        val warehouseForAddressItem = RefWarehouse()
        warehouseForAddressItem.foundID(IDIn)

        var textQuery = "BEGIN TRAN; "+
                "DECLARE @iddoc varchar(9); " +
                "SELECT TOP 1 @iddoc=journ.iddoc FROM _1sjourn as journ (nolock)" +
                "   INNER JOIN DH\$АдресПеремещение as DocAT (nolock) " +
                "ON DocAT.iddoc = journ.iddoc " +
                "WHERE " +
                "DocAT.\$АдресПеремещение.ТипДокумента = 0 " +
                "and journ.date_time_iddoc < '19800101Z' " +
                "and journ.iddocdef = \$АдресПеремещение " +
                "and journ.\$Автор = :EmptyID " +
                "and journ.ismark = 0; " +
                "UPDATE _1sjourn WITH (rowlock) " +
                "SET _1sjourn.\$Автор = :Employer WHERE _1sjourn.iddoc = @iddoc; " +
                "if @@rowcount > 0 begin " +
                "UPDATE DH\$АдресПеремещение WITH (rowlock) " +
                "SET \$АдресПеремещение.Склад = :IDFrom, " +
                "\$АдресПеремещение.СкладПолучатель = :IDIn, " +
                "\$АдресПеремещение.ТипДокумента = 2 " +
                "WHERE DH\$АдресПеремещение .iddoc = @iddoc and DH\$АдресПеремещение .\$АдресПеремещение.ТипДокумента = 0; " +
                "if @@rowcount > 0 begin " +
                "COMMIT TRAN; " +
                "select 1 as result; " +
                "end else begin ROLLBACK TRAN; select 0 as result; end; " +
                "end else begin ROLLBACK TRAN; select 0 as result; end; "
        textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
        textQuery = ss.querySetParam(textQuery, "IDFrom", IDFrom)
        textQuery = ss.querySetParam(textQuery, "IDIn", IDIn)
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        val data = ss.executeWithReadNew(textQuery) ?: return false

        if (data[0]["result"]?.trim()?.toInt() == 0) {
            FExcStr.text = "Не удалось захватить документ. Жмакните повторно!"
            return false
        }

        //Подтянем нужную зону для разноса
        textQuery = "DECLARE @curdate DateTime; " +
                "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock) ; " +
                "SELECT TOP 1 " +
                "RegAC.\$Рег.АдресОстаткиПоступления.Адрес as Adress1 , " +
                "max(Gate.ID ) as GateID , " +
                "max(Gate.Descr ) as GateName, " +
                "max(Gate.\$Спр.Ворота.КоличествоСотрудников ) as CountEmployer " +
                "FROM " +
                "RG\$Рег.АдресОстаткиПоступления as RegAC (nolock) " +
                "LEFT JOIN \$Спр.ЗоныВорот as ZoneGate (nolock) " +
                "ON RegAC.\$Рег.АдресОстаткиПоступления.Адрес = ZoneGate.\$Спр.ЗоныВорот.Секция " +
                "LEFT JOIN \$Спр.Ворота as Gate (nolock) " +
                "ON (ZoneGate.ParentExt = Gate.ID) " +
                "WHERE " +
                "RegAC.period = @curdate " +
                "and RegAC.\$Рег.АдресОстаткиПоступления.ТипДействия = 2 " +
                "and RegAC.\$Рег.АдресОстаткиПоступления.Склад = :Warehouse " +
                "and ((Gate.ID is NULL) OR (Gate.\$Спр.Ворота.ВРазносеСотрудников < Gate.\$Спр.Ворота.КоличествоСотрудников )) " +
                "GROUP BY " +
                "RegAC.\$Рег.АдресОстаткиПоступления.Адрес " +
                "ORDER BY " +
                "CASE WHEN (max(Gate.ID ) = :EmptyRef)OR(max(Gate.ID ) is NULL) THEN 999999 ELSE max(Gate.\$Спр.Ворота.Приоритет ) END , " +
                "sum(1) "
        textQuery = ss.querySetParam(textQuery, "EmptyRef", ss.getVoidID())
        textQuery = ss.querySetParam(textQuery, "Warehouse", IDFrom)
        outputZone = ss.executeWithReadNew(textQuery) ?: return false

        fZone.foundID(outputZone[0]["Adress1"].toString())
        fName = outputZone[0]["GateName"].toString()
        fID = outputZone[0]["GateID"].toString()

        //теперь укажем, что мы в зоне работаем
        textQuery =
            "UPDATE \$Спр.Ворота " +
                    "SET " +
                    "\$Спр.Ворота.ВРазносеСотрудников = (SELECT TOP 1 \$Спр.Ворота.ВРазносеСотрудников + 1 FROM \$Спр.Ворота WHERE id = :id) " +
                    "WHERE " +
                    "id = :id ; "
        textQuery = ss.querySetParam(textQuery, "id", outputZone[0]["GateID"].toString())
        if (!ss.executeWithoutRead(textQuery)) return false

        head.text = (" Разнос (ТЕЛЕЖКА) "+ outputZone[0]["GateName"].toString().trim() + " " + fZone.name.trim())

        return true
    }

    fun quitModeTransfer() : Boolean {
        //теперь укажем, что мы из зоны вышли
        var textQuery = "UPDATE \$Спр.Ворота " +
                "SET " +
                "\$Спр.Ворота.ВРазносеСотрудников = (SELECT TOP 1 \$Спр.Ворота.ВРазносеСотрудников - 1 FROM \$Спр.Ворота WHERE id = :id) " +
                "WHERE id = :id ; "
        textQuery = ss.querySetParam(textQuery, "id", fZone.id)
        if (!ss.executeWithoutRead(textQuery)) return false
        lockoutDoc(iddoc)
        return true
    }

    private fun getItem() {

        var textQuery = "DECLARE @curdate DateTime; " +
                "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock); " +
                "SELECT " +
                "DocAT.\$АдресПеремещение.Товар as ID, " +
                "min(Goods.descr) as ItemName, " +
                "min(Goods.\$Спр.Товары.ИнвКод ) as InvCode, " +
                "min(Goods.\$Спр.Товары.Артикул ) as Article, " +
                "sum(DocAT.\$АдресПеремещение.Количество ) as Count, " +
                "DocAT.\$АдресПеремещение.Адрес0 as Adress0, " +
                "CASE WHEN round(sum(DocAT.\$АдресПеремещение.Количество )/ISNULL(min(Package.Coef), 1), 0)*ISNULL(min(Package.Coef), 1) = sum(DocAT.\$АдресПеремещение.Количество )" +
                "THEN ISNULL(min(Package.Coef), 1) ELSE 1 END as Coef, " +
                "CASE WHEN round(sum(DocAT.\$АдресПеремещение.Количество )/ISNULL(min(Package.Coef), 1), 0)*ISNULL(min(Package.Coef), 1) = sum(DocAT.\$АдресПеремещение.Количество )" +
                "THEN cast(round(sum(DocAT.\$АдресПеремещение.Количество )/ISNULL(min(Package.Coef), 1), 0) as int) " +
                "ELSE sum(DocAT.\$АдресПеремещение.Количество ) END as CountPackage, " +
                "CASE WHEN min(DocAT.\$АдресПеремещение.ФлагОбязательногоАдреса ) = 1" +
                "THEN min(Sections.descr) ELSE min(Sections.descr) END as AdressName " + //: "min(Sections.descr) as AdressName "
                "FROM " +
                "DT\$АдресПеремещение as DocAT (nolock) " +
                "LEFT JOIN \$Спр.Товары as Goods (nolock) " +
                "ON Goods.ID = DocAT.\$АдресПеремещение.Товар " +
                "LEFT JOIN \$Спр.Секции as Sections (nolock) " +
                "ON Sections.ID = DocAT.\$АдресПеремещение.Адрес1 " +
                "LEFT JOIN ( " +
                "SELECT " +
                "Units.parentext as ItemID, " +
                "min(Units.\$Спр.ЕдиницыШК.Коэффициент ) as Coef " +
                "FROM " +
                "\$Спр.ЕдиницыШК as Units (nolock) " +
                "WHERE " +
                "Units.\$Спр.ЕдиницыШК.ОКЕИ = :OKEIPackage " +
                "and Units.ismark = 0 " +
                "GROUP BY " +
                "Units.parentext ) as Package " +
                "ON Package.ItemID = Goods.ID " +
                "WHERE " +
                "DocAT.iddoc = :Doc " +
                "and not DocAT.\$АдресПеремещение.Дата0 = :EmptyDate " +
                "and DocAT.\$АдресПеремещение.Дата1 = :EmptyDate " +
                "GROUP BY DocAT.\$АдресПеремещение.Товар , DocAT.\$АдресПеремещение.Адрес0 " +
                "ORDER BY min(DocAT.\$АдресПеремещение.Дата0 ), min(DocAT.\$АдресПеремещение.Время0 )"
        textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        textQuery = ss.querySetParam(textQuery, "OKEIPackage", model.okeiPackage)
        textQuery = ss.querySetParam(textQuery, "Warehouse", inputWarehouse)
        itemOnPallet = ss.executeWithReadNew(textQuery) ?: return

        textQuery = "SELECT " +
                "DocAT.\$АдресПеремещение.Товар as ID, " +
                "min(Goods.descr) as ItemName, " +
                "min(Goods.\$Спр.Товары.ИнвКод ) as InvCode, " +
                "min(Goods.\$Спр.Товары.Артикул ) as Article, " +
                "sum(DocAT.\$АдресПеремещение.Количество ) as Count, " +
                "DocAT.\$АдресПеремещение.Адрес1 as Adress1, " +
                "CASE WHEN round(sum(DocAT.\$АдресПеремещение.Количество )/ISNULL(min(Package.Coef), 1), 0)*ISNULL(min(Package.Coef), 1) = sum(DocAT.\$АдресПеремещение.Количество )" +
                "THEN ISNULL(min(Package.Coef), 1) ELSE 1 END as Coef, " +
                "CASE WHEN round(sum(DocAT.\$АдресПеремещение.Количество )/ISNULL(min(Package.Coef), 1), 0)*ISNULL(min(Package.Coef), 1) = sum(DocAT.\$АдресПеремещение.Количество )" +
                "THEN cast(round(sum(DocAT.\$АдресПеремещение.Количество )/ISNULL(min(Package.Coef), 1), 0) as int) " +
                "ELSE sum(DocAT.\$АдресПеремещение.Количество ) END as CountPackage, " +
                "min(Sections.descr) as AdressName " +
                "FROM " +
                "DT\$АдресПеремещение as DocAT (nolock) " +
                "LEFT JOIN \$Спр.Товары as Goods (nolock) " +
                "ON Goods.ID = DocAT.\$АдресПеремещение.Товар " +
                "LEFT JOIN \$Спр.Секции as Sections (nolock) " +
                "ON Sections.ID = DocAT.\$АдресПеремещение.Адрес1 " +
                "LEFT JOIN ( " +
                "SELECT " +
                "Units.parentext as ItemID, " +
                "min(Units.\$Спр.ЕдиницыШК.Коэффициент ) as Coef " +
                "FROM " +
                "\$Спр.ЕдиницыШК as Units (nolock) " +
                "WHERE " +
                "Units.\$Спр.ЕдиницыШК.ОКЕИ = :OKEIPackage " +
                "and Units.ismark = 0 " +
                "GROUP BY " +
                "Units.parentext ) as Package " +
                "ON Package.ItemID = Goods.ID " +
                "WHERE " +
                "DocAT.iddoc = :Doc " +
                "and not DocAT.\$АдресПеремещение.Дата0 = :EmptyDate " +
                "and not DocAT.\$АдресПеремещение.Дата1 = :EmptyDate " +
                "GROUP BY DocAT.\$АдресПеремещение.Товар , DocAT.\$АдресПеремещение.Адрес1 " +
                "ORDER BY min(DocAT.\$АдресПеремещение.Дата0 ), min(DocAT.\$АдресПеремещение.Время0 )"
        textQuery = ss.querySetParam(textQuery, "Doc", iddoc)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        textQuery = ss.querySetParam(textQuery, "OKEIPackage", model.okeiPackage)
        itemOnShelf = ss.executeWithReadNew(textQuery) ?: return

        if (!lockDoc(iddoc)) {
            val accMen = Intent(this, Menu::class.java)
            startActivity(accMen)
            badVoice()
            finish()
            return
        }

       // refreshActivity()
    }

    private fun checkItem(itemCode : String) : Boolean {

        var textQuery = "SELECT " +
                "Units.parentext as ItemID " +
                "FROM \$Спр.ЕдиницыШК as Units (nolock) " +
                "WHERE Units.\$Спр.ЕдиницыШК.Штрихкод = :Barcode "
        textQuery = ss.querySetParam(textQuery, "Barcode", itemCode)
        val datTab = ss.executeWithReadNew(textQuery) ?: return false

        if (datTab.count() == 0) {
            FExcStr.text = "С таким штрихкодом товар не найден!"
            return false
        }
        //раньше было так
        //return checkParty(datTab[0]["ItemID"].toString())
        var count = 0
        for (dr in itemOnPallet) {
            count += dr["Coef"].toString().trim().toInt() * dr["CountPackage"].toString().trim().toInt()
        }
        if (count > 0) {
            FExcStr.text = (
                    itemOnPallet[0]["InvCode"].toString().trim() + " - есть в тележке " +
                            getStrPackageCount(count, datTab[0]["Coef"].toString().trim().toInt())
                    )
            //   return ToModeAcceptedItem(datTab[0]["ItemID"].toString(), "", CurrentMode, count, true)
        }

        //товара нет в телеге, проверим где он есть
        textQuery = "DECLARE @curdate DateTime; " +
                "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock) ; " +
                "SELECT " +
                "substring(RefPallet.\$Спр.ПеремещенияПаллет.ШКПаллеты ,9,4) as Pallet, " +
                "max(RegAC.\$Рег.АдресОстаткиПоступления.Адрес ) as AdressID " +
                "FROM " +
                "RG\$Рег.АдресОстаткиПоступления as RegAC (nolock) " +
                "INNER JOIN DT\$АдресПоступление as DocAP (nolock) " +
                "ON (RegAC.\$Рег.АдресОстаткиПоступления.Док = DocAP.IDDOC ) " +
                "and (RegAC.\$Рег.АдресОстаткиПоступления.Товар = DocAP.\$АдресПоступление.Товар ) " +
                "INNER JOIN \$Спр.ПеремещенияПаллет as RefPallet (nolock) " +
                "ON (DocAP.\$АдресПоступление.Паллета = RefPallet.ID) " +
                "WHERE " +
                "RegAC.period = @curdate " +
                "and RegAC.\$Рег.АдресОстаткиПоступления.Склад = :Warehouse " +
                "and RegAC.\$Рег.АдресОстаткиПоступления.Товар = :ItemID " +
                "and RegAC.\$Рег.АдресОстаткиПоступления.ТипДействия = 2 " +
                "and NOT DocAP.\$АдресПоступление.Дата0 = :EmptyDate " +
                "GROUP BY " +
                "RefPallet.\$Спр.ПеремещенияПаллет.ШКПаллеты "
        textQuery = ss.querySetParam(textQuery, "Warehouse", outputWarehouse)
        textQuery = ss.querySetParam(textQuery, "AdressID", fZone.adressZone.id)
        textQuery = ss.querySetParam(textQuery, "ItemID", datTab[0]["ItemID"].toString())
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        val dt = ss.executeWithReadNew(textQuery) ?: return false

        if (dt.count() == 0) FExcStr.text = "С таким ШК товара нет на паллетах!"
        else FExcStr.text = ("Товар с паллеты " + dt[0]["Pallet"].toString())
        return false

    }

    @SuppressLint("UseCompatLoadingForDrawables", "ClickableViewAccessibility")
    private fun refreshActivity() : Boolean {
        var lineNom = 0

        val countLocal = itemOnPallet.count()
        if (countLocal != itemOnPallet.count() && itemOnPallet.isNotEmpty()) {
            currentLine = 0  //сменилось количество, обнулим текущую строку
        }

        if (itemOnPallet.isNotEmpty()) {

            for (DR in itemOnPallet) {
                lineNom++

              //  if (DR["ClientName"].toString().trim() != clientCheck) continue

                val bodyRow = TableRow(this)
                val linearLayout1 = LinearLayout(this)
                bodyRow.isClickable = true
                bodyRow.setOnTouchListener{ _, _ ->  //выделение строки при таче
                    var i = 0
                    while (i < itemOn.childCount) {
                        if (bodyRow != itemOn.getChildAt(i))  itemOn.getChildAt(i).setBackgroundColor(WHITE)
                        else {
                            currentLine = i
                            itemName.text = DR["ItemName"].toString().trim()
                            address.text = DR["AdressName"].toString()
                            bodyRow.setBackgroundColor(LTGRAY)
                        }
                        i++
                    }
                    true
                }
                //добавим столбцы
                val stringArr : Array<String> = arrayOf(
                    DR["InvCode"].toString().trim(),
                    DR["CountPackage"].toString().trim(),
                    helper.byeTheNull(DR["Coef"].toString()),
                    DR["AdressName"].toString()
                )
                val bodyVal : MutableMap<String, TextView> = HashMap()
                for (i in 0..3) bodyVal["bodyVal$i"] = TextView(this)
                var s = 0

                for ((p,_) in bodyVal) {
                    bodyVal[p]?.apply {
                        text = (" " + stringArr[s])
                        typeface = Typeface.SERIF
                        layoutParams = LinearLayout.LayoutParams(
                            (ss.widthDisplay * widArr[s]).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                        background = getDrawable(R.drawable.cell_border)
                        gravity = Gravity.START
                        textSize = 18F
                        setTextColor(-0x1000000)
                    }
                    linearLayout1.background = getDrawable(R.drawable.cell_border)
                    linearLayout1.addView(bodyVal[p])
                    s++
                }

                var colorline = WHITE
                if (lineNom == currentLine) colorline = LTGRAY

                bodyRow.setBackgroundColor(colorline)
                bodyRow.addView(linearLayout1)
                itemOn.addView(bodyRow)
            }
            return true
        }
        return false
    }

    open fun reactionBarcode(Barcode: String):Boolean {
        val helper = Helper()
        val barcodeRes = helper.disassembleBarcode(Barcode)
        val idd = barcodeRes["IDD"].toString()
        val typeBarcode = barcodeRes["Type"].toString()
        if (typeBarcode == "pallete") {
            var textQuery =
                "declare @result char(9); exec WPM_GetIDNewPallet :Barcode, :Employer, @result out; select @result;"
            textQuery = ss.querySetParam(textQuery, "Barcode", Barcode)
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            val palletID =  ss.executeScalar(textQuery) ?: return false
            ss.FPallet = RefPalleteMove()
            if (!ss.FPallet.foundID(palletID)) {
                return false
            } else rbTransfer(Barcode)
        }
        if (typeBarcode == "113") {
            if (ss.isSC(idd, "Сотрудники")) {
                ss.FEmployer = RefEmployer()
                val mainInit = Intent(this, MainActivity::class.java)
                startActivity(mainInit)
                finish()
            }

            if (itm.foundBarcode(Barcode)) {
              //  if (!checkItem(Barcode)) {
                    var findItemInTable = false
                    for (DR in itemOnPallet) {
                        if (itm.id == DR["ID"].toString()) {
                            idDocItm = DR["iddoc"].toString()
                            count = DR["CountPackage"].toString().toInt() *
                                    helper.byeTheNull(DR["Coef"].toString()).toInt()
                            findItemInTable = true
                            break
                        }
                    }
                    if (findItemInTable) {
                        ss.CurrentMode = Global.Mode.TransferRefresh
                        FExcStr.text = "Получаю информацию о товаре..."
                        //если товар есть в списке, переходим в карточку
                        val gotoItem = Intent(this, TransferCard::class.java)
                        gotoItem.putExtra("itemID", itm.id)
                        gotoItem.putExtra("iddoc", idDocItm)
                        gotoItem.putExtra("count", count)
                        startActivity(gotoItem)
                        finish()
                        return true
                    }
             //   }
            } else if (itemOnPallet.isNotEmpty()) {
                FExcStr.text = "Сканируйте товар!"
                return false
            } else {
                FExcStr.text = "Сканируйте паллету!"
                return false
            }
        }
        return true
    }

    open fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == 4) {
            clickVoice()
            lockoutDoc(iddoc)
            ss.excStr = "Выберите режим работы"
            val accMen = Intent(this, Menu::class.java)
            startActivity(accMen)
            finish()
            return true
        }

        if (keyCode == 67) {
            if (itemOnPallet.isNotEmpty()) deleteRowTransferItem()
            else FExcStr.text = "Тут еще нечего удалять"
        }

        if (ss.helper.whatDirection(keyCode) == "Right") {
            clickVoice()
            val backAcc = Intent(this, TransferYepItem::class.java)
            startActivity(backAcc)
            finish()
            return true
        }
        if (ss.helper.whatDirection(keyCode) in listOf("Down", "Up")) {
            if (itemOnPallet.isNotEmpty()) reactionKeyLocal1(keyCode)
            else return false
        }
        return false
    }

    private fun reactionKeyLocal1(keyCode: Int) : Boolean {
        tickVoice()
        itemOn.getChildAt(currentLine).isFocusable = false
        itemOn.getChildAt(currentLine).setBackgroundColor(WHITE)

        if (ss.helper.whatDirection(keyCode) == "Down") {
            if (currentLine < itemOnPallet.count()) currentLine++ else currentLine = 0
            if (currentLine == itemOnPallet.count()) currentLine = 0
        } else {
            if (currentLine == 0) currentLine = itemOnPallet.count()
            if (currentLine > 0 || currentLine == itemOnPallet.count()) currentLine--
            else currentLine = itemOnPallet.count()
        }
        when {
            currentLine < 10 -> {
                scrollTab.fullScroll(View.FOCUS_UP)
            }
            currentLine > itemOnPallet.count() - 10 -> {
                scrollTab.fullScroll(View.FOCUS_DOWN)
            }
            currentLine % 10 == 0 -> {
                scrollTab.scrollTo(0, 30 * currentLine - 1)
            }
        }
        //теперь подкрасим строку серым
        itemName.text = itemOnPallet[currentLine]["ItemName"].toString().trim()
        address.text = (" " + itemOnPallet[currentLine]["AdressName"].toString())
        itemOn.getChildAt(currentLine).setBackgroundColor(LTGRAY)
        itemOn.getChildAt(currentLine).isActivated = false
        return true
    }
}