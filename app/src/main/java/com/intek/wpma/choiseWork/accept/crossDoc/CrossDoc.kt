package com.intek.wpma.choiseWork.accept.crossDoc

import android.annotation.SuppressLint
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
import com.intek.wpma.choiseWork.accept.AccMenu
import com.intek.wpma.helpers.Helper
import com.intek.wpma.model.Model
import com.intek.wpma.ref.RefEmployer
import com.intek.wpma.ref.RefItem
import com.intek.wpma.ref.RefPalleteMove
import com.intek.wpma.ref.RefSection
import kotlinx.android.synthetic.main.activity_cross_yep.*
import kotlinx.android.synthetic.main.activity_crossdoc.*
import kotlinx.android.synthetic.main.activity_crossdoc.FExcStr
import kotlinx.android.synthetic.main.activity_crossdoc.printPal
import kotlinx.android.synthetic.main.activity_crossdoc.scroll
import kotlinx.android.synthetic.main.activity_crossdoc.table
import kotlinx.android.synthetic.main.activity_search_acc.*

open class CrossDoc : BarcodeDataReceiver() {

    open var oldx = 0F
    open var oldMode: Global.Mode? = null
    private var currentLine:Int = 1
    private var flagBarcode = ""
    private var fUnits: MutableList<MutableMap<String, String>> = mutableListOf()
    var currentRowAcceptedItem: MutableMap<String, String> = mutableMapOf()
    var acceptedItem: MutableMap<String, String> = mutableMapOf()
    var model = Model()
    var item = RefItem()
    var countMarkUnit = 0
    var countMarkPac = 0
    var countItemAcc = 0

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
        var parentIDD : String = ""
        var consignmen: MutableList<MutableMap<String, String>> = mutableListOf()
        var noneAccItem : MutableList<MutableMap<String, String>> = mutableListOf()
        var acceptedItems: MutableList<MutableMap<String, String>> = mutableListOf()
    }
    //endregion

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ss.CurrentMode in listOf(
                Global.Mode.AcceptanceNotAccepted,
                Global.Mode.AcceptanceAccepted)) {
            return
        }
        setContentView(R.layout.activity_crossdoc)
        title = ss.title
        FExcStr.setOnTouchListener(fun(_: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                    val backAcc = Intent(this, CrossYepItem::class.java)
                    startActivity(backAcc)
                } else if (event.x > oldx) {
                    val backAcc = Intent(this, CrossNonItem::class.java)
                    startActivity(backAcc)
                }
            }
            return true
        })

        if (ss.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@CrossDoc, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "CrossDoc")
                startActivity(scanAct)
            }
        }
        oldMode = ss.CurrentMode
        ss.CurrentMode = Global.Mode.Acceptance
        toModeAcceptance()
    }

    private fun toModeAcceptance() {
        //если переход между экранами то просто обновим картинку
        if (oldMode == Global.Mode.Waiting) { refreshActivity(); return }
        //обновим информацию о не принятых позициях
        updateTableInfo()

        refreshActivity()
    }

    //подтягиваем данные для таблички
    private fun consign(idd: String, parentIDD: String)  {
        var textQuery = "SELECT " +
                ":Number as Number , " +
                "AC.iddoc as ACID , " +
                ":ParentIDD as ParentIDD , " +
                "AC.\$АдресПоступление.КолСтрок as CountRow , " +
                "journ.docno as DocNo , " +
                "CAST(LEFT(journ.date_time_iddoc, 8) as datetime) as DateDoc , " +
                "CONVERT(char(8), CAST(LEFT(journ.date_time_iddoc,8) as datetime), 4) as DateDocText " +
                "FROM DH\$АдресПоступление as AC (nolock) " +
                "LEFT JOIN _1sjourn as journ (nolock) " +
                "ON journ.iddoc = right(AC.\$АдресПоступление.ДокументОснование , 9) " +
                "WHERE AC.iddoc = :Doc "
        textQuery = ss.querySetParam(textQuery, "Number", consignmen.count() + 1)
        textQuery = ss.querySetParam(textQuery, "Doc", parentIDD)                  //в шарпе этот ключ стоит на 3й строчке
        textQuery = ss.querySetParam(textQuery, "ParentIDD", idd)                  //а этот на 2й и это работает как надо (НЕ ТРОГАТЬ!!!)
        consignmen = ss.executeWithReadNew(textQuery) ?: return

        if (consignmen.isNotEmpty()) {        //если мы успешно подгрузили накладную
            //подтянем не принятые товары
            noneItem()
            //теперь принятые
            yapItem()
        }
    }

    fun noneItem() {
        var textQuery = "DECLARE @curdate DateTime; " +
                "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock); " +
                "SELECT " +
                "right(Journ.docno,5) as DOCNO , " +
                "Supply.iddoc as iddoc , " +
                "Goods.id as id , " +
                "Goods.Descr as ItemName , " +
                "Goods.\$Спр.Товары.ИнвКод as InvCode , " +
                "Goods.\$Спр.Товары.Артикул as Article , " +
                "Goods.\$Спр.Товары.АртикулНаУпаковке as ArticleOnPack , " +
                "Goods.\$Спр.Товары.Прих_Цена as Price , " +
                "Goods.\$Спр.Товары.КоличествоДеталей as Details , " +
                "CASE WHEN round(Supply.\$АдресПоступление.Количество " +
                "/ISNULL(Package.Coef, 1), 0)*ISNULL(Package.Coef, 1) = Supply.\$АдресПоступление.Количество " +
                "THEN ISNULL(Package.Coef, 1) ELSE 1 END as Coef, " +
                "CASE WHEN round( " +
                "(CASE WHEN Supply.\$АдресПоступление.Количество <= ISNULL(OrderOnClients.Balance,0) " +
                "THEN Supply.\$АдресПоступление.Количество ELSE ISNULL(OrderOnClients.Balance,0) END) " +
                "/ISNULL(Package.Coef, 1), 0)*ISNULL(Package.Coef, 1) = " +
                "(CASE WHEN Supply.\$АдресПоступление.Количество <= ISNULL(OrderOnClients.Balance,0) " +
                "THEN Supply.\$АдресПоступление.Количество ELSE ISNULL(OrderOnClients.Balance,0) END) " +
                "THEN round( " +
                "(CASE WHEN Supply.\$АдресПоступление.Количество <= ISNULL(OrderOnClients.Balance,0) " +
                "THEN Supply.\$АдресПоступление.Количество ELSE ISNULL(OrderOnClients.Balance,0) END) " +
                "/ISNULL(Package.Coef, 1), 0) " +
                "ELSE (CASE WHEN Supply.\$АдресПоступление.Количество <= ISNULL(OrderOnClients.Balance,0) " +
                "THEN Supply.\$АдресПоступление.Количество ELSE ISNULL(OrderOnClients.Balance,0) END) END as CountPackage , " +
                "(CASE WHEN Supply.\$АдресПоступление.Количество <= ISNULL(OrderOnClients.Balance,0) " +
                "THEN Supply.\$АдресПоступление.Количество ELSE ISNULL(OrderOnClients.Balance,0) END) as Count , " +
                "Supply.\$АдресПоступление.ЕдиницаШК as Unit , " +
                "Supply.\$АдресПоступление.КоличествоЭтикеток as LabelCount , " +
                "Supply.\$АдресПоступление.НомерСтрокиДока as Number , " +
                "Supply.\$АдресПоступление.ГруппаСезона as SeasonGroup , " +
                "SypplyHeader.\$АдресПоступление.ДальнийСклад as FlagFarWarehouse , " +
                "Supply.LineNO_ as LineNO_ , " +
                "isnull(GS.\$Спр.ТоварныеСекции.РасчетныйРХ , 0) as StoregeSize , " +
                "OrderOnClients.ClientID as ClientID , " +
                "OrderOnClients.ClientName as ClientName , " +
                "OrderOnClients.OrderID as OrderID , " +
                "OrderOnClients.OrderName as OrderName , " +
                "OrderOnClients.AddressID as AddressID , " +
                "OrderOnClients.AddressName as AddressName , " +
                "OrderOnClients.PalletID as PalletID , " +
                "OrderOnClients.PalletName as PalletName , " +
                "isNull(OrderOnClients.BoxCount,0) as BoxCount , " +
                "Supply.\$АдресПоступление.Количество as CountAll " +
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
                "LEFT JOIN ( " +
                "SELECT " +
                "\$Рег.ЗаказыНаКлиентов.Клиент as ClientID , " +
                "min(Clients.Descr ) as ClientName , " +
                "\$Рег.ЗаказыНаКлиентов.Товар as Item , " +
                "\$Рег.ЗаказыНаКлиентов.Склад as Warehouse , " +
                "sum(OrderOnClientItem.\$ЗаказНаКлиента.Количество ) - sum(OrderOnClientItem.\$ЗаказНаКлиента.Принято ) as Balance , " +
                "min(Journ.IDDOC) as OrderID , " +
                "min(right(Journ.docno,5)) as OrderName , " +
                "min(Sections.ID ) as AddressID , " +
                "min(\$ЗаказНаКлиента.КолМест ) as BoxCount , " +
                "min(Pallets.ID ) as PalletID , " +
                "min(SUBSTRING(Pallets.\$Спр.ПеремещенияПаллет.ШКПаллеты ,9,4) ) as PalletName , " +
                "min(Sections.Descr ) as AddressName , " +
                "min(OrderOnClient.\$ЗаказНаКлиента.ДокументОснование ) as OrderDocOsn " +
                "FROM " +
                "RG\$Рег.ЗаказыНаКлиентов as RegOrderOnClient (nolock) " +
                "LEFT JOIN \$Спр.Клиенты as Clients (nolock) " +
                "on Clients.ID = \$Рег.ЗаказыНаКлиентов.Клиент " +
                "LEFT JOIN DH\$ЗаказНаКлиента as OrderOnClient (nolock) " +
                "on OrderOnClient.IDDOC = Right(\$Рег.ЗаказыНаКлиентов.Док , 9) " +
                "LEFT JOIN DT\$ЗаказНаКлиента as OrderOnClientItem (nolock) " +
                "on OrderOnClient.IDDOC = OrderOnClientItem.IDDOC " +
                "and OrderOnClientItem.\$ЗаказНаКлиента.Товар = \$Рег.ЗаказыНаКлиентов.Товар " +
                "LEFT JOIN \$Спр.Секции as Sections (nolock) " +
                "on Sections.ID = OrderOnClient.\$ЗаказНаКлиента.Адрес " +
                "LEFT JOIN \$Спр.ПеремещенияПаллет as Pallets (nolock) " +
                "on Pallets.ID = OrderOnClient.\$ЗаказНаКлиента.Паллета " +
                "LEFT JOIN _1sjourn as Journ (nolock) " +
                "on Journ.IDDOC = Right(\$Рег.ЗаказыНаКлиентов.Док , 9) " +
                "WHERE " +
                "RegOrderOnClient.period = @curdate " +
                "GROUP BY " +
                "\$Рег.ЗаказыНаКлиентов.Клиент , \$Рег.ЗаказыНаКлиентов.Товар , " +
                "\$Рег.ЗаказыНаКлиентов.Склад , \$Рег.ЗаказыНаКлиентов.Док " +
                "HAVING " +
                "sum(OrderOnClientItem.\$ЗаказНаКлиента.Количество ) - sum(OrderOnClientItem.\$ЗаказНаКлиента.Принято ) > 0 " +
                "and sum(RegOrderOnClient.\$Рег.ЗаказыНаКлиентов.Количество ) > 0 " +
                ") as OrderOnClients " +
                "on Goods.ID = OrderOnClients.Item " +
                "and SypplyHeader.\$АдресПоступление.Склад = OrderOnClients.Warehouse " +
                "and SypplyHeader.\$АдресПоступление.ДокументОснование = OrderOnClients.OrderDocOsn " +
                "WHERE " +
                "Supply.IDDOC in (:Docs) " +
                "and Supply.\$АдресПоступление.Состояние0 = 0 " +
                "ORDER BY " +
                "Journ.docno, OrderOnClients.OrderID, Supply.LineNO_ "
        val model = Model()
        textQuery = ss.querySetParam(textQuery, "OKEIPackage", model.okeiPackage)
        textQuery = ss.querySetParam(textQuery, "Warehouse", ss.Const.mainWarehouse)
        textQuery = ss.querySetParam(textQuery, "Docs", parentIDD.substring(4,10))
        noneAccItem = ss.executeWithReadNew(textQuery) ?: return
        //для удобного поиска подсосем сразу все цифры в названии и артикулах
        for (dr in noneAccItem){
            dr["Client"] = ss.helper.getShortFIO(dr["Client"].toString())
            dr["ArticleFind"] = ss.helper.suckDigits(dr["Article"].toString())
            dr["ArticleOnPackFind"] = ss.helper.suckDigits(dr["ArticleOnPack"].toString())
            dr["ItemNameFind"] = ss.helper.suckDigits(dr["ItemName"].toString())
            dr["Count"] = dr["Count"].toString().trim()
        }
    }

    fun yapItem() {
        var textQuery = "DECLARE @curdate DateTime; " +
                "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock); " +
                "SELECT " +
                "right(Journ.docno,5) as DOCNO , " +
                "Supply.iddoc as iddoc , " +
                "Goods.id as id , " +
                "Goods.Descr as ItemName , " +
                "Goods.\$Спр.Товары.ИнвКод as InvCode , " +
                "Goods.\$Спр.Товары.Артикул as Article , " +
                "Goods.\$Спр.Товары.АртикулНаУпаковке as ArticleOnPack , " +
                "Goods.\$Спр.Товары.Прих_Цена as Price , " +
                "Goods.\$Спр.Товары.КоличествоДеталей as Details , " +
                "CASE WHEN round(Supply.\$АдресПоступление.Количество " +
                "/ISNULL(Package.Coef, 1), 0)*ISNULL(Package.Coef, 1) = Supply.\$АдресПоступление.Количество " +
                "THEN ISNULL(Package.Coef, 1) ELSE 1 END as Coef , " +
                "CASE WHEN round( " +
                "(CASE WHEN Supply.\$АдресПоступление.Количество <= ISNULL(OrderOnClients.Balance,0) " +
                "THEN Supply.\$АдресПоступление.Количество ELSE ISNULL(OrderOnClients.Balance,0) END) " +
                "/ISNULL(Package.Coef, 1), 0)*ISNULL(Package.Coef, 1) = " +
                "(CASE WHEN Supply.\$АдресПоступление.Количество <= ISNULL(OrderOnClients.Balance,0) " +
                "THEN Supply.\$АдресПоступление.Количество ELSE ISNULL(OrderOnClients.Balance,0) END) " +
                "THEN round( " +
                "(CASE WHEN Supply.\$АдресПоступление.Количество <= ISNULL(OrderOnClients.Balance,0) " +
                "THEN Supply.\$АдресПоступление.Количество ELSE ISNULL(OrderOnClients.Balance,0) END) " +
                "/ISNULL(Package.Coef, 1), 0) " +
                "ELSE (CASE WHEN Supply.\$АдресПоступление.Количество <= ISNULL(OrderOnClients.Balance,0) " +
                "THEN Supply.\$АдресПоступление.Количество ELSE ISNULL(OrderOnClients.Balance,0) END) END as CountPackage , " +
                "(CASE WHEN Supply.\$АдресПоступление.Количество <= ISNULL(OrderOnClients.Balance,0) " +
                "THEN Supply.\$АдресПоступление.Количество ELSE ISNULL(OrderOnClients.Balance,0) END) as Count , " +
                "Supply.\$АдресПоступление.ЕдиницаШК as Unit , " +
                "Supply.\$АдресПоступление.КоличествоЭтикеток as LabelCount , " +
                "Supply.\$АдресПоступление.НомерСтрокиДока as Number , " +
                "Supply.\$АдресПоступление.ГруппаСезона as SeasonGroup , " +
                "SypplyHeader.\$АдресПоступление.ДальнийСклад as FlagFarWarehouse , " +
                "Supply.LineNO_ as LineNO_, " +
                "isnull(GS.\$Спр.ТоварныеСекции.РасчетныйРХ , 0) StoregeSize , " +
                "OrderOnClients.ClientID as ClientID , " +
                "OrderOnClients.ClientName as ClientName , " +
                "OrderOnClients.OrderID as OrderID , " +
                "OrderOnClients.OrderName as OrderName , " +
                "OrderOnClients.AddressID as AddressID , " +
                "OrderOnClients.AddressName as AddressName , " +
                "OrderOnClients.PalletID as PalletID , " +
                "OrderOnClients.PalletName as PalletName , " +
                "isNull(OrderOnClients.BoxCount,0) as BoxCount , " +
                "Supply.\$АдресПоступление.Количество as CountAll " +
                "FROM DT\$АдресПоступление as Supply (nolock) " +
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
                "LEFT JOIN ( " +
                "SELECT " +
                "\$Рег.ЗаказыНаКлиентов.Клиент as ClientID , " +
                "min(Clients.Descr) as ClientName , " +
                "\$Рег.ЗаказыНаКлиентов.Товар as Item , " +
                "\$Рег.ЗаказыНаКлиентов.Склад as Warehouse , " +
                "sum(OrderOnClientItem.\$ЗаказНаКлиента.Принято ) as Balance , " +
                "min(Journ.IDDOC) as OrderID , " +
                "min(right(Journ.docno,5)) as OrderName , " +
                "min(Sections.ID ) as AddressID , " +
                "min(\$ЗаказНаКлиента.КолМест ) as BoxCount , " +
                "min(Pallets.ID ) as PalletID , " +
                "min(SUBSTRING(Pallets.\$Спр.ПеремещенияПаллет.ШКПаллеты ,9,4) ) as PalletName , " +
                "min(Sections.Descr ) as AddressName , " +
                "min(OrderOnClient.\$ЗаказНаКлиента.ДокументОснование ) as OrderDocOsn " +
                "FROM " +
                "RG\$Рег.ЗаказыНаКлиентов as RegOrderOnClient (nolock) " +
                "LEFT JOIN \$Спр.Клиенты as Clients (nolock) " +
                "on Clients.ID = \$Рег.ЗаказыНаКлиентов.Клиент " +
                "LEFT JOIN DH\$ЗаказНаКлиента as OrderOnClient (nolock) " +
                "on OrderOnClient.IDDOC = Right(\$Рег.ЗаказыНаКлиентов.Док , 9) " +
                "LEFT JOIN DT\$ЗаказНаКлиента as OrderOnClientItem (nolock) " +
                "on OrderOnClient.IDDOC = OrderOnClientItem.IDDOC " +
                "and OrderOnClientItem.\$ЗаказНаКлиента.Товар = \$Рег.ЗаказыНаКлиентов.Товар " +
                "LEFT JOIN \$Спр.Секции as Sections (nolock) " +
                "on Sections.ID = OrderOnClient.\$ЗаказНаКлиента.Адрес " +
                "LEFT JOIN \$Спр.ПеремещенияПаллет as Pallets (nolock) " +
                "on Pallets.ID = OrderOnClient.\$ЗаказНаКлиента.Паллета " +
                "LEFT JOIN _1sjourn as Journ (nolock) " +
                "on Journ.IDDOC = Right(\$Рег.ЗаказыНаКлиентов.Док , 9) " +
                "WHERE " +
                "RegOrderOnClient.period = @curdate " +
                "GROUP BY " +
                "\$Рег.ЗаказыНаКлиентов.Клиент , " +
                "\$Рег.ЗаказыНаКлиентов.Товар , " +
                "\$Рег.ЗаказыНаКлиентов.Склад , " +
                "\$Рег.ЗаказыНаКлиентов.Док " +
                "HAVING " +
                "sum(OrderOnClientItem.\$ЗаказНаКлиента.Принято ) > 0 " +
                "and sum(RegOrderOnClient.\$Рег.ЗаказыНаКлиентов.Количество ) > 0 " +
                ") as OrderOnClients " +
                "on Goods.ID = OrderOnClients.Item " +
                "and SypplyHeader.\$АдресПоступление.Склад = OrderOnClients.Warehouse " +
                "and SypplyHeader.\$АдресПоступление.ДокументОснование = OrderOnClients.OrderDocOsn " +
                "WHERE Supply.IDDOC in (:Docs) " +
                "and Supply.\$АдресПоступление.Состояние0 = 1 " +
                "and Supply.\$АдресПоступление.ФлагПечати = 1 " +
                "and Supply.\$АдресПоступление.Сотрудник0 = :Employer " +
                "ORDER BY Journ.docno, OrderOnClients.OrderID, Supply.\$АдресПоступление.Дата0 , Supply.\$АдресПоступление.Время0 "
        val model = Model()
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id) //ss.EmployerID)
        textQuery = ss.querySetParam(textQuery, "OKEIPackage", model.okeiPackage)
        textQuery = ss.querySetParam(textQuery, "Warehouse", ss.Const.mainWarehouse)
        textQuery = ss.querySetParam(textQuery, "Docs", parentIDD.substring(4,10))
        acceptedItems = ss.executeWithReadNew(textQuery) ?: return
    }

    //тотальная ебань
    //класс для обработки события при нажатии на кнопку о принятии товара
    fun completeAccept(idDoc:String): Boolean {

        if (checkMark(idDoc) == 0) {
            //не указана маркировка
            if (ss.excStr == "") {
                FExcStr.text = "Маркированный товар! Сканируйте маркировку!"
            }
            else {
                FExcStr.text = ss.excStr
            }
            return false
        }
        val docForQuery = currentRowAcceptedItem["iddoc"].toString()
        var textQuery: String
        //Сколько в накладной изначально
        if (acceptedItem["AcceptCount"].toString().toInt() > currentRowAcceptedItem["Count"].toString().toInt()) {
            FExcStr.text =("Нельзя принять по данной накладной более " + currentRowAcceptedItem["Count"].toString() + " штук!")
            return false
        } else if (acceptedItem["AcceptCount"].toString().toInt() == 0) {
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
        if (dTCurrState[0]["Count"].toString().toInt() < acceptedItem["AcceptCount"].toString().toInt()) {
            FExcStr.text = "Недопустимое количество! Повторите приемку позиции!"
            return false
        }
        //Скорректируем начальное количество
        val beginCount = dTCurrState[0]["Count"].toString().toInt()

        var needNew = 0
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
        packNorm = if (packNorm == "") "1" else packNorm.substring(1)  //Отрезаем первый символ "/"

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

        //ТЕПЕРЬ ПОЕХАЛА ЗАПИСЬ ДОКУМЕНТА
        //Расчитаем число этикеток
        val labelCount: Int = if (flagBarcode != "0")  0 else 1

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

        var allCountAccepted: Int = acceptedItem["AcceptCount"].toString().toInt()
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
            textQuery = ss.querySetParam(textQuery, "Count", acceptedItem["AcceptCount"].toString().toInt())
            textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
            textQuery = ss.querySetParam(textQuery, "Coef", 1)
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            textQuery = ss.querySetParam(textQuery, "Adress", ss.getVoidID())
            textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
            textQuery = ss.querySetParam(textQuery, "LabelCount", labelCount)
            textQuery = ss.querySetParam(textQuery, "UnitID", currentRowAcceptedItem["Unit"].toString())
            textQuery = ss.querySetParam(textQuery, "RemainedLineNo_", currentRowAcceptedItem["LineNO_"].toString())
            textQuery = ss.querySetParam(textQuery, "RemainedCount", beginCount - acceptedItem["AcceptCount"].toString().toInt())
            textQuery = ss.querySetParam(textQuery, "PalletID", ss.FPallet.id)
            if (!ss.executeWithoutRead(textQuery)) {
                return false
            }
        }
        else if (alreadyDT.isEmpty() && acceptedItem["AcceptCount"].toString().toInt() >= beginCount) {
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
            textQuery = ss.querySetParam(textQuery, "Count", acceptedItem["AcceptCount"].toString().toInt())
            textQuery = ss.querySetParam(textQuery, "Doc", docForQuery)
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
            textQuery = ss.querySetParam(textQuery, "PalletID", ss.FPallet.id)
            textQuery = ss.querySetParam(textQuery, "LabelCount", labelCount)
            textQuery = ss.querySetParam(textQuery, "LineNo_", currentRowAcceptedItem["LineNO_"].toString())
            if (!ss.executeWithoutRead(textQuery)) {
                return false
            }
        }
        else if (alreadyDT.isNotEmpty() && acceptedItem["AcceptCount"].toString().toInt() < beginCount) {
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
                    "WHERE DT\$АдресПоступление .iddoc = :Doc and DT\$АдресПоступление .lineno_ = :RemainedLineNo_"
            textQuery = ss.querySetParam(textQuery, "Doc", docForQuery)
            textQuery = ss.querySetParam(textQuery, "LineNo_", newLineNo_)
            textQuery = ss.querySetParam(textQuery, "Number", currentRowAcceptedItem["Number"].toString())
            textQuery = ss.querySetParam(textQuery, "Item", item.id)
            textQuery = ss.querySetParam(textQuery, "Count", acceptedItem["AcceptCount"].toString())
            textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
            textQuery = ss.querySetParam(textQuery, "Coef", 1)
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            textQuery = ss.querySetParam(textQuery, "Adress", ss.getVoidID())
            textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
            textQuery = ss.querySetParam(textQuery, "LabelCount", labelCount)
            textQuery = ss.querySetParam(textQuery, "UnitID", currentRowAcceptedItem["Unit"].toString())
            textQuery = ss.querySetParam(textQuery, "RemainedLineNo_", currentRowAcceptedItem["LineNO_"].toString())
            textQuery = ss.querySetParam(textQuery, "RemainedCount",beginCount - acceptedItem["AcceptCount"].toString().toInt())
            textQuery = ss.querySetParam(textQuery, "PalletID", ss.FPallet.id)
            if (!ss.executeWithoutRead(textQuery)) {
                return false
            }
            //теперь еще обновим непринятые строки
            allCountAccepted = alreadyDT[0]["Count"].toString().toInt() + acceptedItem["AcceptCount"].toString().toInt()
            textQuery = "UPDATE DT\$АдресПоступление " +
                    "SET " +
                    "\$АдресПоступление.Количество = :RemainedCount " +
                    "WHERE " +
                    "DT\$АдресПоступление .iddoc = :Doc " +
                    "and DT\$АдресПоступление .lineno_ = :RemainedLineNo_"
            textQuery = ss.querySetParam(textQuery, "Doc", docForQuery)
            textQuery = ss.querySetParam(textQuery, "RemainedLineNo_", currentRowAcceptedItem["LineNO_"].toString())
            textQuery = ss.querySetParam(textQuery, "RemainedCount", beginCount - acceptedItem["AcceptCount"].toString().toInt())
            if (!ss.executeWithoutRead(textQuery)) {
                return false
            }
        }
        else {
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
            textQuery = ss.querySetParam(textQuery, "Count", acceptedItem["AcceptCount"].toString().toInt())
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
        FExcStr.text = (acceptedItem["InvCode"].toString().trim() + " принят в количестве " + model.getStrPackageCount(
            acceptedItem["AcceptCount"].toString().toInt(), tmpCoef
        ))
        //begin internal command
        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ss.extendID(ss.FEmployer.id, "Спр.Сотрудники")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход2"] = ss.extendID(item.id, "Спр.Товары")
        dataMapWrite["Спр.СинхронизацияДанных.ДокументВход"] = ss.extendID(docForQuery, "АдресПоступление")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = "AcceptItem (Принял товар)"
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход2"] = acceptedItem["AcceptCount"].toString()
        execCommandNoFeedback("Internal", dataMapWrite)
        //end internal command
        if (!ibsLockOut("int_ref_Товары_" + (item.id + "_unit"))) {
            //если не удалось разблокировать то просто напишем об этом
            FExcStr.text = "Не удалось разблокировать товар!"
        }
        //обновим данные табличек и выйдем наружу
        return true
    }

    fun createUnit(okei: String, coef: Int, barcode: String): Boolean {
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
                    "ORDER BY Units.ismark DESC)"
        textQuery = ss.querySetParam(textQuery, "Barcode", barcode)
        textQuery = ss.querySetParam(textQuery, "Coef", coef)
        textQuery = ss.querySetParam(textQuery, "ItemID", item.id)
        textQuery = ss.querySetParam(textQuery, "OKEI", okei)
        textQuery = ss.querySetParam(textQuery, "ItemForUnits", ss.Const.itemForUnits)
        return ss.executeWithoutRead(textQuery)
    }

    fun addPackNorm(pacNorm: String, okei: String): String {
        var packNorm = pacNorm
        for (dr in fUnits) {
            if (dr["OKEI"].toString() == okei && dr["Coef"].toString().toInt() != 0) {
                packNorm += "/" + dr["Coef"].toString()
                if (okei == model.okeiKit) {
                    packNorm += "!"
                } else if (okei == model.okeiPackage) {
                    packNorm += "*"
                }
                break
            }
        }
        return packNorm
    }

    private fun getCoefPackage(): Int {
        var coef = 0
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
        coef = if (dtdt.isEmpty()) 1 else dtdt[0]["Coef"].toString().toFloat().toInt()
        return coef
    }

    private fun checkMark(idDocItm:String): Int {

        ss.excStr = ""
        var textQuery = "SELECT " +
                "Category.\$Спр.КатегорииТоваров.Маркировка as Маркировка " +
                "from \$Спр.Товары as Item (nolock) " +
                "INNER JOIN \$Спр.КатегорииТоваров as Category (nolock) " +
                "on Item.\$Спр.Товары.Категория = Category.ID " +
                "where Category.\$Спр.КатегорииТоваров.Маркировка > 0 " +
                "and Item.id = '${item.id}' "
        val dt = ss.executeWithReadNew(textQuery)
        if (dt == null)
        {
            ss.excStr = "Не удалось выполнить запрос флага маркировки"
            return 0
        }
        if (dt.isEmpty()) return 1

        textQuery = "SELECT " +
                "ISNULL(journ.iddoc, AC.iddoc ) as iddoc , " +
                "AC.iddoc as ACiddoc " +
                "FROM " +
                " DH\$АдресПоступление as AC (nolock) " +
                "INNER JOIN _1sjourn as journAC (nolock) " +
                "     ON journAC.iddoc = AC.iddoc " +
                "LEFT JOIN _1sjourn as journ (nolock) " +
                "     ON journ.iddoc = right(AC.\$АдресПоступление.ДокументОснование , 9) " +
                "WHERE" +
                " AC.iddoc = '${idDocItm}' "

        val naklAccTemp = ss.executeWithReadNew(textQuery)
        if (naklAccTemp == null || naklAccTemp.isEmpty()) {
            //не вышли на накладную - косяк какой-то
            ss.excStr = "Не удалось найти накладную"
            return 0
        }

        //проверим сканировали ли маркировки в экране для маркировок
        if (countMarkPac == 0 && countMarkUnit == 0) {
            FExcStr.text = "Маркированный товар! Сканируйте маркировку!"
            return 0
        }
        if (countMarkPac == 0 && countMarkUnit < acceptedItem["AcceptCount"].toString().toInt()) {
            FExcStr.text = "Недостаточно штучных маркировок!"
            return 0
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
        val markItemDT = ss.executeWithReadNew(textQuery)
        if (markItemDT == null) {
            ss.excStr = "Не удалось выполнить запрос маркировок"
            return 0
        }

        if (markItemDT.isEmpty()) {
            //раз сюда пришли, значит там что-то сканировали, просто еще не создалось
            return countMarkPac+countMarkUnit
        }
        return markItemDT.count()

    }

    fun updateTableInfo() {
        //обновим информацию по паллетам если она пустая
        if (ss.FPallets.isEmpty()) {
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
                        "Supply.\$АдресПоступление.Паллета "
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
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
            var countRow = 0
            for (drNotAcc in noneAccItem) {
                if (drNotAcc["iddoc"].toString().trim() == dr["ACID"].toString().trim()) {
                    countRow ++
                }
            }
            dr["CountNotAcceptRow"] = countRow.toString()
        }
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
                goodVoice()
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
                    "UPDATE \$Спр.ПеремещенияПаллет " +
                            "SET \$Спр.ПеремещенияПаллет.Адрес0 = :ID, \$Спр.ПеремещенияПаллет.ФлагОперации = 2 " +
                            "WHERE \$Спр.ПеремещенияПаллет .id in ($strPallets)"
                textQuery = ss.querySetParam(textQuery, "ID", sections.id)
                if (!ss.executeWithoutRead(textQuery)) {
                    return false
                }
                //почистим табличку паллет от греха и почистим паллеты

                ss.FPallets = mutableListOf()
                ss.FPallet = RefPalleteMove()
                refreshActivity()
                return true
            }
            if (ss.isSC(idd, "")) {      //если это ни один из типов справочников, значит скорее всего нужный нам документ
                parentIDD = ss.getDoc(idd, false).toString()
                if (parentIDD == "" || parentIDD == "null") {   //если это совсем левый шк
                    FExcStr.text = "Не верный тип справочника!"
                    badVoice()
                    return false
                }
                consign(idd, parentIDD.substring(4, 10))
                goodVoice()
                refreshActivity()
                return true
            } else {
                FExcStr.text = "Не верный тип справочника!"
                badVoice()
                return false
            }
        } else if (typeBarcode == "pallete") {
            badVoice()
            FExcStr.text = "Паллета только в карточке товара!"
            return false
        }
        else {
            FExcStr.text = "Не верный тип справочника!"
            badVoice()
            return false
        }
    }

    open fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        if (keyCode == 4) {
            clickVoice()
            quitModeAcceptance()
            ss.excStr = "Выберите режим работы"
            val accMen = Intent(this, AccMenu::class.java)
            startActivity(accMen)
            finish()
            return true
        }

        if (ss.helper.whatDirection(keyCode) == "Left") {
            clickVoice()
            val backAcc = Intent(this, CrossNonItem::class.java)
            backAcc.putExtra("parentIDD", parentIDD)
            startActivity(backAcc)
            return true
        }

        if (ss.helper.whatDirection(keyCode) == "Right") {
            clickVoice()
            val backAcc = Intent(this, CrossYepItem::class.java)
            startActivity(backAcc)
            return true
        }
        if (ss.helper.whatDirection(keyCode) in listOf("Down", "Up")) {
            tickVoice()
            table.getChildAt(currentLine).isFocusable = false
            table.getChildAt(currentLine).setBackgroundColor(Color.WHITE)
            if (ss.helper.whatDirection(keyCode) == "Down") {
                if (currentLine < consignmen.count()) currentLine++
                else currentLine = 1
            } else {
                if (currentLine > 1) currentLine--
                else currentLine = consignmen.count()
            }
            when {
                currentLine < 10 -> {
                    scroll.fullScroll(View.FOCUS_UP)
                }
                currentLine > consignmen.count() - 10 -> {
                    scroll.fullScroll(View.FOCUS_DOWN)
                }
                currentLine % 10 == 0 -> {
                    scroll.scrollTo(0, 30 * currentLine - 1)
                }
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
            //consign(barcode)
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
            val tmpdr:MutableMap<String, String> = mutableMapOf()
            tmpdr["ID"] = ss.FPallet.id
            tmpdr["Barcode"] = strBarcode
            tmpdr["Name"] = strBarcode.substring(8, 12)
            tmpdr["AdressID"] = ss.getVoidID()
            ss.FPallets.add(tmpdr)
        }
        return true
    }

    @SuppressLint("ClickableViewAccessibility")
    open fun refreshActivity() {

        if (ss.FPrinter.selected) printPal.text = ss.FPrinter.path
        else printPal.text = "(принтер не выбран)"

        table.removeAllViewsInLayout()

        if (ss.CurrentMode in listOf(
                Global.Mode.AcceptanceNotAccepted,
                Global.Mode.AcceptanceAccepted
            )
        ) return

        val linearLayout = LinearLayout(this)
        val rowTitle = TableRow(this)
        var k = 0

        val widArr : Array<Double> = arrayOf(0.05, 0.45, 0.32, 0.1)   //длина ячеек в таблице
        val strArr : Array<String> = arrayOf("№", "Накл.", "Дата", "Ост.")
        val hatTab: MutableMap<String, TextView> = HashMap()
        for (i in 0..3) hatTab["hatTab$i"] = TextView(this) //создадим массив ячеек в шапке таблицы
        //добавим столбцы
        for ((i, _) in hatTab) {
            hatTab[i]?.text = strArr[k]         //заполним ячейки значениями
            hatTab[i]?.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay * widArr[k]).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            hatTab[i]?.gravity = Gravity.CENTER
            hatTab[i]?.textSize = 18F
            hatTab[i]?.setTextColor(-0x1000000)
            hatTab[i]?.typeface = Typeface.SERIF
            linearLayout.addView(hatTab[i])      //добавляем ячейки в линию
            k++
        }
        k = 0
        rowTitle.addView(linearLayout)
        rowTitle.setBackgroundColor(Color.GRAY)
        table.addView(rowTitle)
        var lineNom = 0

        if (consignmen.isNotEmpty()) {

            for (DR in consignmen) {

                val linearLayout1 = LinearLayout(this)
                lineNom ++
                val rowTitle1 = TableRow(this)
                rowTitle1.isClickable = true
                rowTitle1.setOnTouchListener{ _, _ ->  //выделение строки при таче
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

                val bodyCross: MutableMap<String, TextView> = HashMap()
                for (i in 0..3) bodyCross["bodyCross$i"] = TextView(this)

                val valueArr : Array<String> = arrayOf(
                    DR["Number"].toString(), DR["DocNo"].toString(), DR["DateDocText"].toString(),
                    DR["CountRow"].toString()
                )           //массив значений
                //добавим столбцы
                for ((i, _) in bodyCross) {
                    bodyCross[i]?.text = valueArr[k]       //закидываем значения в ячейки строки
                    bodyCross[i]?.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay * widArr[k]).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                    bodyCross[i]?.gravity = Gravity.CENTER
                    bodyCross[i]?.textSize = 18F
                    bodyCross[i]?.setTextColor(-0x1000000)
                    bodyCross[i]?.typeface = Typeface.SERIF
                    linearLayout1.addView(bodyCross[i])
                    k++
                }

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

    private fun quitModeAcceptance() {
        for (dr in iddoc.split(",")) {
            lockoutDocAccept(dr.replace("'", ""))
        }
    }

}