package com.intek.wpma.ChoiseWork.Accept

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.ParentForm
import com.intek.wpma.R
import kotlinx.android.synthetic.main.activity_accept.*
import kotlinx.android.synthetic.main.activity_accept.table
import android.view.KeyEvent
import com.intek.wpma.ScanActivity

class Search : BarcodeDataReceiver() {

    var iddoc: String = ""
    var iddocControl: String = ""
    var number: String = ""
    var barcode: String = ""
    var codeId: String = ""  //показатель по которому можно различать типы штрих-кодов

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
                    }
                    catch(e: Exception) {
                        val toast = Toast.makeText(applicationContext, "Не удалось отсканировать штрихкод!", Toast.LENGTH_LONG)
                        toast.show()
                    }

                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accept)
        iddoc = intent.extras!!.getString("Doc")!!
        number = intent.extras!!.getString("Number")!!
        ParentForm = intent.extras!!.getString("ParentForm")!!

        title = ss.title

        if (ss.isMobile){
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@Search, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","SetComplete")
                startActivity(scanAct)
            }
        }

        val linearLayout = LinearLayout(this)
        val rowTitle = TableRow(this)

        //добавим столбцы
        val number = TextView(this)
        number.text = "№"
        number.typeface = Typeface.SERIF
        number.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.09).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        number.gravity = Gravity.CENTER
        number.textSize = 12F
        number.setTextColor(-0x1000000)
        val docum = TextView(this)
        docum.text = "Накладная"
        docum.typeface = Typeface.SERIF
        docum.gravity = Gravity.CENTER
        docum.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.28).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        docum.textSize = 12F
        docum.setTextColor(-0x1000000)
        val address = TextView(this)
        address.text = "Дата"
        address.typeface = Typeface.SERIF
        address.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.35).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        address.gravity = Gravity.CENTER
        address.textSize = 12F
        address.setTextColor(-0x1000000)
        val boxes = TextView(this)
        boxes.text = "Осталось"
        boxes.typeface = Typeface.SERIF
        boxes.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.14).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        boxes.gravity = Gravity.CENTER
        boxes.textSize = 12F
        boxes.setTextColor(-0x1000000)
        val boxesfact = TextView(this)
        boxesfact.text = "Поставщик"
        boxesfact.typeface = Typeface.SERIF
        boxesfact.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.14).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT)
        boxesfact.gravity = Gravity.CENTER
        boxesfact.textSize = 12F
        boxesfact.setTextColor(-0x1000000)

        linearLayout.addView(number)
        linearLayout.addView(docum)
        linearLayout.addView(address)
        linearLayout.addView(boxes)
        linearLayout.addView(boxesfact)

        rowTitle.addView(linearLayout)
        table.addView(rowTitle)

        alitem()
    }


    @SuppressLint("ClickableViewAccessibility")
    fun alitem() {

        var textQuery = "SELECT " +
                "identity(int, 1, 1) as Number, " +
                "AC.iddoc as ACID," +
                "journ.iddoc as ParentIDD," +
                "Clients.descr as Client," +
                "AC.\$АдресПоступление.КолСтрок as CountRow," +
                "journ.docno as DocNo," +
                "CAST(LEFT(journ.date_time_iddoc, 8) as datetime) as DateDoc," +
                "CONVERT(char(8), CAST(LEFT(journ.date_time_iddoc,8) as datetime), 4) as DateDocText " +
                "into #temp " +
                "FROM DH\$АдресПоступление as AC (nolock) " +
                "LEFT JOIN _1sjourn as journ (nolock) " +
                "     ON journ.iddoc = right(AC.\$АдресПоступление.ДокументОснование , 9) " +
                "LEFT JOIN DH\$ПриходнаяКредит as PK (nolock) " +
                "     ON journ.iddoc = PK.iddoc " +
                "LEFT JOIN \$Спр.Клиенты as Clients (nolock) " +
                "     ON PK.\$ПриходнаяКредит.Клиент = Clients.id " +
                "WHERE AC.iddoc in (:Docs) " +
                " select * from #temp " +
                " drop table #temp "

        textQuery = ss.querySetParam(textQuery, "Number", number)
        textQuery = ss.querySetParam(textQuery, "iddoc", iddoc)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        val dataTable = ss.executeWithReadNew(textQuery) ?: return

        if (dataTable.isNotEmpty()) {

            for (DR in dataTable) {
                val linearLayout = LinearLayout(this)
                val rowTitle = TableRow(this)

                //добавим столбцы
                val numb = TextView(this)
                numb.text = DR["Number"]
                numb.typeface = Typeface.SERIF
                numb.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.09).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                numb.gravity = Gravity.CENTER
                numb.textSize = 12F
                numb.setTextColor(-0x1000000)
                val doc = TextView(this)
                doc.text = DR["ACID"]
                doc.typeface = Typeface.SERIF
                doc.gravity = Gravity.CENTER
                doc.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.28).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                doc.textSize = 12F
                doc.setTextColor(-0x1000000)
                val addr = TextView(this)
                addr.text = DR["ParentIDD"]
                addr.typeface = Typeface.SERIF
                addr.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.35).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                addr.gravity = Gravity.CENTER
                addr.textSize = 12F
                addr.setTextColor(-0x1000000)
                val box = TextView(this)
                box.text = DR["CountRow"]
                box.typeface = Typeface.SERIF
                box.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.14).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                box.gravity = Gravity.CENTER
                box.textSize = 12F
                box.setTextColor(-0x1000000)
                val boxf = TextView(this)
                boxf.text = DR["Client"]
                boxf.typeface = Typeface.SERIF
                boxf.layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * 0.14).toInt(),
                    ViewGroup.LayoutParams.WRAP_CONTENT)
                boxf.gravity = Gravity.CENTER
                boxf.textSize = 12F
                boxf.setTextColor(-0x1000000)

                linearLayout.addView(numb)
                linearLayout.addView(doc)
                linearLayout.addView(addr)
                linearLayout.addView(box)
                linearLayout.addView(boxf)

                rowTitle.addView(linearLayout)
                table.addView(rowTitle)
            }
        }
    }


    override fun onResume() {
        super.onResume()
        registerReceiver(barcodeDataReceiver, IntentFilter(ACTION_BARCODE_DATA))
        claimScanner()
        Log.d("IntentApiSample: ", "onResume")

        if(scanRes != null){
            try {
                barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(barcode)
            }
            catch (e: Exception){
                val toast = Toast.makeText(applicationContext, "Ошибка! Возможно отсутствует соединение с базой!", Toast.LENGTH_LONG)
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

/*

private bool ToModeAcceptance()
{
    if (FConsignment == null)
    {
        FConsignment = new DataTable();
        FConsignment.Columns.Add("Number", Type.GetType("System.Int32"));
        FConsignment.Columns.Add("ACID", Type.GetType("System.String"));
        FConsignment.Columns.Add("ParentIDD", Type.GetType("System.String"));
        FConsignment.Columns.Add("DOCNO", Type.GetType("System.String"));
        FConsignment.Columns.Add("DateDoc", Type.GetType("System.DateTime"));
        FConsignment.Columns.Add("DateDocText", Type.GetType("System.String"));
        FConsignment.Columns.Add("Client", Type.GetType("System.String"));
        FConsignment.Columns.Add("CountRow", Type.GetType("System.Int32"));
        FConsignment.Columns.Add("CountNotAcceptRow", Type.GetType("System.Int32"));
    }

    if (FNotAcceptedItems == null)
    {
        FNotAcceptedItems = new DataTable();
        FNotAcceptedItems.Columns.Add("ID", Type.GetType("System.String"));
        FNotAcceptedItems.Columns.Add("LINENO_", Type.GetType("System.Int32"));
        FNotAcceptedItems.Columns.Add("Number", Type.GetType("System.Int32")); //Номер строки документа
        FNotAcceptedItems.Columns.Add("ItemName", Type.GetType("System.String"));
        FNotAcceptedItems.Columns.Add("IDDOC", Type.GetType("System.String"));
        FNotAcceptedItems.Columns.Add("DOCNO", Type.GetType("System.String"));
        FNotAcceptedItems.Columns.Add("InvCode", Type.GetType("System.String"));
        FNotAcceptedItems.Columns.Add("Article", Type.GetType("System.String"));
        FNotAcceptedItems.Columns.Add("ArticleOnPack", Type.GetType("System.String"));
        FNotAcceptedItems.Columns.Add("ArticleFind", Type.GetType("System.String"));
        FNotAcceptedItems.Columns.Add("ArticleOnPackFind", Type.GetType("System.String"));
        FNotAcceptedItems.Columns.Add("ItemNameFind", Type.GetType("System.String"));
        FNotAcceptedItems.Columns.Add("Count", Type.GetType("System.Int32"));
        FNotAcceptedItems.Columns.Add("CountPackage", Type.GetType("System.Int32"));
        FNotAcceptedItems.Columns.Add("Coef", Type.GetType("System.Int32"));
        FNotAcceptedItems.Columns.Add("CoefView", Type.GetType("System.String"));
        FNotAcceptedItems.Columns.Add("LabelCount", Type.GetType("System.Int32"));
        FNotAcceptedItems.Columns.Add("Unit", Type.GetType("System.String"));
        FNotAcceptedItems.Columns.Add("Price", Type.GetType("System.Decimal"));
        FNotAcceptedItems.Columns.Add("Details", Type.GetType("System.Int32"));
        FNotAcceptedItems.Columns.Add("SeasonGroup", Type.GetType("System.Int32"));
        FNotAcceptedItems.Columns.Add("FlagFarWarehouse", Type.GetType("System.Int32"));
        FNotAcceptedItems.Columns.Add("StoregeSize", Type.GetType("System.Int32"));
        FAcceptedItems = FNotAcceptedItems.Clone();
    }

        //Непринятый товар
        var textQuery = "SELECT " +
                "right(Journ.docno,5) as DOCNO," +
                "Supply.iddoc as iddoc," +
                "Goods.id as id," +
                "Goods.Descr as ItemName," +
                "Goods.\$Спр.Товары.ИнвКод as InvCode," +
                "Goods.\$Спр.Товары.Артикул as Article," +
                "Goods.\$Спр.Товары.АртикулНаУпаковке as ArticleOnPack," +
                "Goods.\$Спр.Товары.Прих_Цена as Price," +
                "Goods.\$Спр.Товары.КоличествоДеталей as Details, " +
                "CASE WHEN round(Supply.\$АдресПоступление.Количество " +
                "/ISNULL(Package.Coef, 1), 0)*ISNULL(Package.Coef, 1) = Supply.\$АдресПоступление.Количество " +
                "THEN ISNULL(Package.Coef, 1) ELSE 1 END as Coef, " +
                "CASE WHEN round(Supply.\$АдресПоступление.Количество " +
                "/ISNULL(Package.Coef, 1), 0)*ISNULL(Package.Coef, 1) = Supply.\$АдресПоступление.Количество " +
                "THEN round(Supply.\$АдресПоступление.Количество /ISNULL(Package.Coef, 1), 0) " +
                "ELSE Supply.\$АдресПоступление.Количество END as CountPackage, " +
                "Supply.\$АдресПоступление.Количество as Count," +
                "Supply.\$АдресПоступление.ЕдиницаШК as Unit," +
                "Supply.\$АдресПоступление.КоличествоЭтикеток as LabelCount," +
                "Supply.\$АдресПоступление.НомерСтрокиДока as Number," +
                "Supply.\$АдресПоступление.ГруппаСезона as SeasonGroup, " +
                "SypplyHeader.\$АдресПоступление.ДальнийСклад as FlagFarWarehouse," +
                "Supply.LineNO_ as LineNO_, " +
                //"isnull(GS.$Спр.ТоварныеСекции.РазмерХранения , 0) as StoregeSize " +
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
                "Units.parentext as ItemID, " +
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
                "WHERE Supply.IDDOC in (:Docs) " +
                "and Supply.\$АдресПоступление.Состояние0 = 0 " +
                "ORDER BY Journ.docno, Supply.LineNO_ ";
        TextQuery = TextQuery.Replace(":Docs", Docs);
        QuerySetParam(ref TextQuery, "OKEIPackage", OKEIPackage);
        QuerySetParam(ref TextQuery, "Warehouse", Const.MainWarehouse);
        DataTable DT;
        if (!ExecuteWithRead(TextQuery, out DT))
        {
            return false;
        }
        DT.Columns.Add("ArticleFind", Type.GetType("System.String"));
        DT.Columns.Add("ArticleOnPackFind", Type.GetType("System.String"));
        DT.Columns.Add("ItemNameFind", Type.GetType("System.String"));
        DT.Columns.Add("CoefView", Type.GetType("System.String"));
        for (int row = 0; row < DT.Rows.Count; row++)
        {
            DT.Rows[row]["ArticleFind"] = Helper.SuckDigits(DT.Rows[row]["Article"].ToString().Trim());
            DT.Rows[row]["ArticleOnPackFind"] = Helper.SuckDigits(DT.Rows[row]["ArticleOnPack"].ToString().Trim());
            DT.Rows[row]["ItemNameFind"] = Helper.SuckDigits(DT.Rows[row]["ItemName"].ToString().Trim());
            DT.Rows[row]["CoefView"] = ((int)(decimal)DT.Rows[row]["Coef"] == 1 ? "??  " : "")
            + ((int)(decimal)DT.Rows[row]["Coef"]).ToString();
        }
        FNotAcceptedItems.Merge(DT, false, MissingSchemaAction.Ignore);

        //Теперь принятый товар
        TextQuery =
            "SELECT " +
                    "right(Journ.docno,5) as DOCNO," +
                    "Supply.iddoc as iddoc," +
                    "Goods.id as id," +
                    "Goods.Descr as ItemName," +
                    "Goods.$Спр.Товары.ИнвКод as InvCode," +
                    "Goods.$Спр.Товары.Артикул as Article," +
                    "Goods.$Спр.Товары.АртикулНаУпаковке as ArticleOnPack," +
                    "Goods.$Спр.Товары.Прих_Цена as Price," +
                    "Goods.$Спр.Товары.КоличествоДеталей as Details, " +
                    "CASE WHEN round(Supply.$АдресПоступление.Количество /ISNULL(Package.Coef, 1), 0)*ISNULL(Package.Coef, 1) = Supply.$АдресПоступление.Количество " +
                    "THEN ISNULL(Package.Coef, 1) ELSE 1 END as Coef, " +
                    "CASE WHEN round(Supply.$АдресПоступление.Количество /ISNULL(Package.Coef, 1), 0)*ISNULL(Package.Coef, 1) = Supply.$АдресПоступление.Количество " +
                    "THEN round(Supply.$АдресПоступление.Количество /ISNULL(Package.Coef, 1), 0) " +
                    "ELSE Supply.$АдресПоступление.Количество END as CountPackage, " +
                    "Supply.$АдресПоступление.Количество as Count," +
                    "Supply.$АдресПоступление.ЕдиницаШК as Unit," +
                    "Supply.$АдресПоступление.КоличествоЭтикеток as LabelCount," +
                    "Supply.$АдресПоступление.НомерСтрокиДока as Number," +
                    "Supply.$АдресПоступление.ГруппаСезона as SeasonGroup," +
                    "SypplyHeader.$АдресПоступление.ДальнийСклад as FlagFarWarehouse," +
                    "Supply.LineNO_ as LineNO_, " +
                    //"isnull(GS.$Спр.ТоварныеСекции.РазмерХранения , 0) StoregeSize " +
                    "isnull(GS.$Спр.ТоварныеСекции.РасчетныйРХ , 0) StoregeSize " +
                    "FROM DT$АдресПоступление as Supply (nolock) " +
                    "LEFT JOIN $Спр.Товары as Goods (nolock) " +
                    "ON Goods.ID = Supply.$АдресПоступление.Товар " +
                    "LEFT JOIN DH$АдресПоступление as SypplyHeader (nolock) " +
                    "ON SypplyHeader.iddoc = Supply.iddoc " +
                    "LEFT JOIN _1sjourn as Journ (nolock) " +
                    "ON Journ.iddoc = Right(SypplyHeader.$АдресПоступление.ДокументОснование , 9) " +
                    "LEFT JOIN ( " +
                    "SELECT " +
                    "Units.parentext as ItemID, " +
                    "min(Units.$Спр.ЕдиницыШК.Коэффициент ) as Coef " +
                    "FROM " +
                    "$Спр.ЕдиницыШК as Units (nolock) " +
                    "WHERE " +
                    "Units.$Спр.ЕдиницыШК.ОКЕИ = :OKEIPackage " +
                    "and Units.ismark = 0 " +
                    "and not Units.$Спр.ЕдиницыШК.Коэффициент = 0 " +
                    "GROUP BY " +
                    "Units.parentext " +
                    ") as Package " +
                    "ON Package.ItemID = Goods.ID " +
                    "LEFT JOIN $Спр.ТоварныеСекции as GS (nolock) " +
                    "on GS.parentext = goods.id and gs.$Спр.ТоварныеСекции.Склад = :Warehouse " +
                    "WHERE Supply.IDDOC in (:Docs) " +
                    "and Supply.$АдресПоступление.Состояние0 = 1 " +
                    "and Supply.$АдресПоступление.ФлагПечати = 1 " +
                    "and Supply.$АдресПоступление.Сотрудник0 = :Employer " +
                    "ORDER BY Journ.docno, Supply.$АдресПоступление.Дата0 , Supply.$АдресПоступление.Время0 ";
        TextQuery = TextQuery.Replace(":Docs", Docs);
        QuerySetParam(ref TextQuery, "Employer", Employer.ID);
        QuerySetParam(ref TextQuery, "OKEIPackage", OKEIPackage);
        QuerySetParam(ref TextQuery, "Warehouse", Const.MainWarehouse);
        DT.Clear();
        if (!ExecuteWithRead(TextQuery, out DT))
        {
            return false;
        }
        DT.Columns.Add("CoefView", Type.GetType("System.String"));
        for (int row = 0; row < DT.Rows.Count; row++)
        {
            DT.Rows[row]["CoefView"] = ((int)(decimal)DT.Rows[row]["Coef"] == 1 ? "??  " : "")
            + ((int)(decimal)DT.Rows[row]["Coef"]).ToString();
        }
        FAcceptedItems.Merge(DT, false, MissingSchemaAction.Ignore);
        if (FPallets.Rows.Count == 0)
        {
            //Теперь принятые паллеты
            TextQuery =
                "SELECT " +
                        "Supply.$АдресПоступление.Паллета as ID, " +
                        "min(Pallets.$Спр.ПеремещенияПаллет.ШКПаллеты ) as Barcode, " +
                        "min(SUBSTRING(Pallets.$Спр.ПеремещенияПаллет.ШКПаллеты ,8,4)) as Name, " +
                        "min(Pallets.$Спр.ПеремещенияПаллет.Адрес0 ) as AdressID " +
                        "FROM DT$АдресПоступление as Supply (nolock) " +
                        "INNER JOIN $Спр.ПеремещенияПаллет as Pallets (nolock) " +
                        "ON Pallets.ID = Supply.$АдресПоступление.Паллета " +
                        "WHERE Supply.IDDOC in (:Docs) " +
                        "and Supply.$АдресПоступление.Состояние0 = 1 " +
                        "and Supply.$АдресПоступление.ФлагПечати = 1 " +
                        "and Supply.$АдресПоступление.Сотрудник0 = :Employer " +
                        "GROUP BY " +
                        "Supply.$АдресПоступление.Паллета " +
                        "ORDER BY " +
                        "Supply.$АдресПоступление.Паллета ";
            TextQuery = TextQuery.Replace(":Docs", Docs);
            QuerySetParam(ref TextQuery, "Employer", Employer.ID);
            DT.Clear();
            if (!ExecuteWithRead(TextQuery, out DT))
            {
                return false;
            }
            FPallets.Merge(DT, false, MissingSchemaAction.Ignore);
            if (DT.Rows.Count > 0)
            {
                FPalletID = FPallets.Rows[DT.Rows.Count - 1]["ID"].ToString();
                FBarcodePallet = FPallets.Rows[DT.Rows.Count - 1]["Barcode"].ToString();
            }
            else
            {
                FPalletID = "";
                FBarcodePallet = "";
            }
        }
        //Расчитываем строчечки
        DR = FConsignment.Select();
        foreach (DataRow dr in DR)
        {
            DataRow[] tmpDR = FNotAcceptedItems.Select("IDDOC = '" + dr["ACID"].ToString() + "'");
            dr["CountNotAcceptRow"] = tmpDR.Length;
        }
    }

    FCurrentMode = Mode.Acceptance;
    return true;
} // ToModeAcceptance
private bool ToModeAcceptedItem(string ItemID, string IDDoc, Mode ToMode)
{
    return ToModeAcceptedItem(ItemID, IDDoc, ToMode, 0, false);
}
private bool ToModeAcceptedItem(string ItemID, string IDDoc, Mode ToMode, int InPartyCount, bool OnShelf)
{
    //проверяем наличие отсканированной паллеты
    if ((FPalletID == "") && (ToMode == Mode.Acceptance))
    {
        FExcStr = "Не выбрана паллета";
        return false;
    }
    AdressConditionItem = null;
    //Если был дисконнект, то это проявиться после нижеследующего запроса
    //и дальше будет, не приемка, а редактирование карточки, для этого запрос и помещен в начало
    if (!LoadUnits(ItemID))
    {
        return false;
    }

    //FExcStr - несет смысл
    AcceptedItem = new StructItem();
    AcceptedItem.GenerateBarcode = false;
    if (NewBarcodes == null)
    {
        NewBarcodes = new List<string>();
    }
    else
    {
        NewBarcodes.Clear();
    }

    //Определяем имеется ли данный товар в списке принимаемых
    CurrentRowAcceptedItem = null;
    int AllCount = 0;
    DataRow[] DR;

    if (ToMode == Mode.Acceptance)
    {
        DR = FNotAcceptedItems.Select("ID = '" + ItemID + "'");
        if (DR.Length > 1 && IDDoc != "")
        {
            foreach (DataRow dr in DR)
            {
                if (dr["IDDOC"].ToString() == IDDoc)
                {
                    if (CurrentRowAcceptedItem == null)
                    {
                        CurrentRowAcceptedItem = dr;
                    }
                }
                AllCount += (int)dr["Count"];
            }
        }
        else if (DR.Length > 0) //Один товар или не указана строка документа
        {
            CurrentRowAcceptedItem = DR[0];
            foreach (DataRow dr in DR)
            {
                AllCount += (int)dr["Count"];
            }
        }
        //иначе это скан товара не из списка!
    }
    else if ( ToMode == Mode.AcceptanceCross)
    {
        DR = FNotAcceptedItems.Select("ID = '" + ItemID + "'");
        if (DR.Length > 1 && IDDoc != "")
        {
            foreach (DataRow dr in DR)
            {
                if (dr["IDDOC"].ToString() == IDDoc && dr["OrderID"].ToString() == FOrderID)
                {
                    if (CurrentRowAcceptedItem == null)
                    {
                        CurrentRowAcceptedItem = dr;
                    }
                }
                AllCount += (int)dr["Count"];
            }
        }
        else if (DR.Length > 0) //Один товар или не указана строка документа
        {
            CurrentRowAcceptedItem = DR[0];
            foreach (DataRow dr in DR)
            {
                AllCount += (int)dr["Count"];
            }
        }
        //иначе это скан товара не из списка!
    }
    //БЛОКИРУЕМ ТОВАР
    if (!LockItem(ItemID))
    {
        return false;
    }
    RefWarehouse WorkWarehouse = new RefWarehouse(this);

    if (WarehouseForAddressItem == null)
    {
        WorkWarehouse.FoundID(Const.MainWarehouse);
    }
    else if (WarehouseForAddressItem.Selected)
    {
        WorkWarehouse.FoundID(WarehouseForAddressItem.ID);
    }
    else
    {
        WorkWarehouse.FoundID(Const.MainWarehouse);
    }
    //ОПРЕДЕЛЯЕМ ОСТАТКИ И АДРЕСА
    string TextQuery =
    "DECLARE @curdate DateTime; " +
            "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock); " +
            "SELECT " +
            "CAST(sum(CASE WHEN Main.Warehouse = :MainWarehouse THEN Main.Balance ELSE 0 END) as int) as BalanceMain, " +
            "CAST(sum(CASE WHEN Main.Warehouse = :BufferWarehouse THEN Main.Balance ELSE 0 END) as int) as BalanceBuffer, " +
            "ISNULL((" +
            "SELECT top 1 " +
            "Section.descr " +
            "FROM _1sconst as Const (nolock) " +
            "LEFT JOIN $Спр.Секции as Section (nolock) " +
            "ON Section.id = left(Const.value, 9) " +
            "WHERE " +
            "Const.id = $Спр.ТоварныеСекции.Секция " +
            "and Const.date <= :DateNow " +
            "and Const.OBJID in (" +
            "SELECT id FROM $Спр.ТоварныеСекции " +
            "WHERE " +
            "$Спр.ТоварныеСекции.Склад = :MainWarehouse " +
            "and parentext = :Item)" +
            "ORDER BY " +
            "Const.date DESC, Const.time DESC, Const.docid DESC), '<не задан>') as AdressMain, " +
            "ISNULL((" +
            "SELECT top 1 " +
            "Section.descr " +
            "FROM _1sconst as Const (nolock) " +
            "LEFT JOIN $Спр.Секции as Section (nolock) " +
            "ON Section.id = left(Const.value, 9) " +
            "WHERE " +
            "Const.id = $Спр.ТоварныеСекции.Секция " +
            "and Const.date <= :DateNow " +
            "and Const.OBJID in (" +
            "SELECT id FROM $Спр.ТоварныеСекции " +
            "WHERE " +
            "$Спр.ТоварныеСекции.Склад = :BufferWarehouse " +
            "and parentext = :Item)" +
            "ORDER BY " +
            "Const.date DESC, Const.time DESC, Const.docid DESC), '<не задан>') as AdressBuffer " +
            "FROM (" +
            "SELECT " +
            "$Рег.ОстаткиТоваров.Склад as Warehouse, " +
            "$Рег.ОстаткиТоваров.Товар as Item, " +
            "$Рег.ОстаткиТоваров.ОстатокТовара as Balance " +
            "FROM " +
            "RG$Рег.ОстаткиТоваров (nolock) " +
            "WHERE " +
            "period = @curdate " +
            "and $Рег.ОстаткиТоваров.Товар = :Item " +
            "and $Рег.ОстаткиТоваров.Склад in (:MainWarehouse, :BufferWarehouse) " +
            "UNION ALL " +
            "SELECT " +
            ":MainWarehouse, :Item, 0 " +
            ") as Main " +
            "GROUP BY Main.Item";
    QuerySetParam(ref TextQuery, "DateNow", DateTime.Now);
    QuerySetParam(ref TextQuery, "Item", ItemID);
    QuerySetParam(ref TextQuery, "BufferWarehouse", BufferWarehouse);
    QuerySetParam(ref TextQuery, "MainWarehouse", WorkWarehouse.ID);

    DataTable DT;
    if (!ExecuteWithRead(TextQuery, out DT))
    {
        return false;
    }
    AcceptedItem.BalanceMain = (int)DT.Rows[0]["BalanceMain"];
    AcceptedItem.BalanceBuffer = (int)DT.Rows[0]["BalanceBuffer"];
    AcceptedItem.AdressMain = DT.Rows[0]["AdressMain"].ToString();
    AcceptedItem.AdressBuffer = DT.Rows[0]["AdressBuffer"].ToString();
    AcceptedItem.IsRepeat = false;

    Dictionary<string, object> DataMapWrite = new Dictionary<string, object>();
    if (CurrentRowAcceptedItem == null)
    {
        //Подсосем остатки в разрезе адресов и состояний
        TextQuery =
            "DECLARE @curdate DateTime; " +
                    "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock); " +
                    "SELECT " +
                    "min(Section.descr) as Adress, " +
                    "CASE " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = -10 THEN '-10 Автокорректировка' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = -2 THEN '-2 В излишке' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = -1 THEN '-1 В излишке (пересчет)' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = 0 THEN '00 Не существует' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = 1 THEN '01 Приемка' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = 2 THEN '02 Хороший на месте' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = 3 THEN '03 Хороший (пересчет)' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = 4 THEN '04 Хороший (движение)' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = 7 THEN '07 Бракованный на месте' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = 8 THEN '08 Бракованный (пересчет)' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = 9 THEN '09 Бракованный (движение)' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = 12 THEN '12 Недостача' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = 13 THEN '13 Недостача (пересчет)' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = 14 THEN '14 Недостача (движение)' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = 17 THEN '17 Недостача подтвержденная' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = 18 THEN '18 Недостача подт.(пересчет)' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = 19 THEN '19 Недостача подт.(движение)' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = 22 THEN '22 Пересорт излишек' " +
                    "WHEN RegAOT.$Рег.АдресОстаткиТоваров.Состояние = 23 THEN '23 Пересорт недостача' " +
                    "ELSE rtrim(cast(RegAOT.$Рег.АдресОстаткиТоваров.Состояние as char)) + ' <неизвестное состояние>' END as Condition, " +
                    "cast(sum(RegAOT.$Рег.АдресОстаткиТоваров.Количество ) as int) as Count " +
                    "FROM " +
                    "RG$Рег.АдресОстаткиТоваров as RegAOT (nolock) " +
                    "LEFT JOIN $Спр.Секции as Section (nolock) " +
                    "ON Section.id = RegAOT.$Рег.АдресОстаткиТоваров.Адрес " +
                    "WHERE " +
                    "RegAOT.period = @curdate " +
                    "and RegAOT.$Рег.АдресОстаткиТоваров.Товар = :ItemID " +
                    "and RegAOT.$Рег.АдресОстаткиТоваров.Склад = :Warehouse " +
                    "GROUP BY " +
                    "RegAOT.$Рег.АдресОстаткиТоваров.Адрес , " +
                    "RegAOT.$Рег.АдресОстаткиТоваров.Товар , " +
                    "RegAOT.$Рег.АдресОстаткиТоваров.Состояние " +
                    "HAVING sum(RegAOT.$Рег.АдресОстаткиТоваров.Количество ) <> 0 " +
                    "ORDER BY Adress, Condition";
        QuerySetParam(ref TextQuery, "DateNow", DateTime.Now);
        QuerySetParam(ref TextQuery, "ItemID", ItemID);
        QuerySetParam(ref TextQuery, "Warehouse", WorkWarehouse.ID);
        if (!ExecuteWithRead(TextQuery, out AdressConditionItem))
        {
            AdressConditionItem = null;
        }

        //Я не знаю что это...
        TextQuery =
            "SELECT " +
                    "Goods.Descr as ItemName," +
                    "Goods.$Спр.Товары.ИнвКод as InvCode, " +
                    "Goods.$Спр.Товары.Артикул as Article, " +
                    "Goods.$Спр.Товары.КоличествоДеталей as Details, " +
                    "Goods.$Спр.Товары.БазоваяЕдиницаШК as BaseUnitID, " +
                    "Goods.$Спр.Товары.МинПартия as MinParty, " +
                    "Goods.$Спр.Товары." + (ToMode == Mode.Acceptance ? "Прих_Цена" : "Опт_Цена") + " as Price,  " +
        //"isnull(RefSections.$Спр.ТоварныеСекции.РазмерХранения , 0) as StoregeSize " +
        "isnull(RefSections.$Спр.ТоварныеСекции.РасчетныйРХ , 0) as StoregeSize " +
                "FROM $Спр.Товары as Goods (nolock) " +
                "left join $Спр.ТоварныеСекции as RefSections (nolock) " +
                "on RefSections.parentext = Goods.id and RefSections.$Спр.ТоварныеСекции.Склад = :warehouse " +
                "WHERE Goods.id = :Item ";
        QuerySetParam(ref TextQuery, "Item", ItemID);
        QuerySetParam(ref TextQuery, "warehouse", WorkWarehouse.ID);
        DT.Clear();
        if (!ExecuteWithRead(TextQuery, out DT))
        {
            return false;
        }
        AcceptedItem.Details = (int)(decimal)DT.Rows[0]["Details"];
        AcceptedItem.NowDetails = AcceptedItem.Details;

        AcceptedItem.ID = ItemID;
        AcceptedItem.Name = DT.Rows[0]["ItemName"].ToString();
        AcceptedItem.InvCode = DT.Rows[0]["InvCode"].ToString();
        AcceptedItem.Acticle = DT.Rows[0]["Article"].ToString();
        AcceptedItem.BaseUnitID = DT.Rows[0]["BaseUnitID"].ToString();
        AcceptedItem.MinParty = (int)(decimal)DT.Rows[0]["MinParty"];
        AcceptedItem.Count = 0;
        AcceptedItem.Price = (decimal)DT.Rows[0]["Price"];
        AcceptedItem.Acceptance = false;
        AcceptedItem.ToMode = ToMode;
        AcceptedItem.BindingAdressFlag = false;
        AcceptedItem.StoregeSize = (int)(decimal)DT.Rows[0]["StoregeSize"];
        if (AcceptedItem.ToMode == Mode.AcceptanceCross)
        {
            CurrentPalletAcceptedItem = new RefPalleteMove(this);

        }

        //Если это необходимо, то определяем количество товара для склада инвентаризации
        if (AcceptedItem.ToMode != Mode.Inventory || InventoryWarehouse.ID == Const.MainWarehouse)
        {
            AcceptedItem.CurrentBalance = AcceptedItem.BalanceMain;
        }
        else if (InventoryWarehouse.ID == BufferWarehouse)
        {
            AcceptedItem.CurrentBalance = AcceptedItem.BalanceBuffer;
        }
        else
        {
            //Остатков этого склада нет!
            TextQuery =
                "DECLARE @curdate DateTime; " +
                        "SELECT @curdate = DATEADD(DAY, 1 - DAY(curdate), curdate) FROM _1ssystem (nolock); " +
                        "SELECT sum(Main.Balance) as Balance " +
                        "FROM " +
                        "(SELECT " +
                        "$Рег.ОстаткиТоваров.Товар as Item, " +
                        "$Рег.ОстаткиТоваров.ОстатокТовара as Balance " +
                        "FROM " +
                        "RG$Рег.ОстаткиТоваров (nolock) " +
                        "WHERE " +
                        "period = @curdate " +
                        "and $Рег.ОстаткиТоваров.Товар = :ItemID " +
                        "and $Рег.ОстаткиТоваров.Склад = :Warehouse " +
                        "UNION ALL " +
                        "SELECT :ItemID, 0 " +
                        ") as Main " +
                        "GROUP BY Main.Item;";
            QuerySetParam(ref TextQuery, "DateNow", DateTime.Now);
            QuerySetParam(ref TextQuery, "ItemID", ItemID);
            QuerySetParam(ref TextQuery, "Warehouse", InventoryWarehouse.ID);
            if (!ExecuteWithRead(TextQuery, out DT))
            {
                return false;
            }
            AcceptedItem.CurrentBalance = (int)(decimal)DT.Rows[0]["Balance"];
        }
        //А теперь имя склада!
        if (AcceptedItem.ToMode != Mode.Inventory)
        {
            TextQuery =
                "SELECT descr as Name FROM $Спр.Склады (nolock) WHERE ID = :Warehouse";
            QuerySetParam(ref TextQuery, "Warehouse", WorkWarehouse.ID);
            if (!ExecuteWithRead(TextQuery, out DT))
            {
                return false;
            }
            AcceptedItem.CurrentWarehouseName = DT.Rows[0]["Name"].ToString();
        }
        else
        {
            AcceptedItem.CurrentWarehouseName = InventoryWarehouse.Name;
        }

        //
        if (ToMode == Mode.Transfer)
        {
            if (ATDoc.ToWarehouseSingleAdressMode)
            {
                DR = FTransferReadyItems.Select("ID = '" + AcceptedItem.ID + "'");
                if (DR.Length > 0)
                {
                    AcceptedItem.BindingAdress = DR[0]["Adress1"].ToString();
                    AcceptedItem.BindingAdressName = DR[0]["AdressName"].ToString();
                    AcceptedItem.BindingAdressFlag = true;
                }
                else if (!Employer.CanMultiadress && AcceptedItem.CurrentBalance > 0)
                {
                    //ОПРЕДЕЛИМ РЕКОМЕНДУЕМЫЙ АДРЕС
                    TextQuery =
                        "SELECT top 1 " +
                                " left(const.value, 9) as Adress, " +
                                " section.descr as AdressName " +
                                "FROM _1sconst as const(nolock) " +
                                "LEFT JOIN $Спр.Секции as Section (nolock) " +
                                "ON Section.id = left(value, 9) " +
                                "WHERE " +
                                "const.id = $Спр.ТоварныеСекции.Секция " +
                                "and const.date <= :DateNow " +
                                "and const.OBJID in (" +
                                "SELECT id FROM $Спр.ТоварныеСекции (nolock) " +
                                "WHERE " +
                                "$Спр.ТоварныеСекции.Склад = :Warehouse " +
                                "and parentext = :Item) " +
                                "ORDER BY " +
                                "const.date DESC, const.time DESC, const.docid DESC ";
                    QuerySetParam(ref TextQuery, "DateNow", DateTime.Now);
                    QuerySetParam(ref TextQuery, "Item", ItemID);
                    QuerySetParam(ref TextQuery, "Warehouse", ATDoc.ToWarehouseID);
                    if (!ExecuteWithRead(TextQuery, out DT))
                    {
                        return false;
                    }
                    if (DT.Rows.Count == 1)
                    {
                        AcceptedItem.BindingAdress = DT.Rows[0]["Adress"].ToString();
                        AcceptedItem.BindingAdressName = DT.Rows[0]["AdressName"].ToString();
                        AcceptedItem.BindingAdressFlag = true;
                    }
                }
            }
        }


        if (AcceptedItem.ToMode == Mode.Acceptance || AcceptedItem.ToMode == Mode.AcceptanceCross)
        {
            FExcStr = "РЕДАКТИРОВАНИЕ КАРТОЧКИ! ТОВАРА НЕТ В СПИСКЕ ПРИНИМАЕМЫХ!";
        }
        else if (AcceptedItem.ToMode == Mode.Inventory)
        {
            FExcStr = "РЕДАКТИРОВАНИЕ КАРТОЧКИ!";
        }
        else if (AcceptedItem.ToMode == Mode.SampleInventory)
        {
            FExcStr = "ОБРАЗЕЦ! " + FExcStr;
        }
        else if (AcceptedItem.ToMode == Mode.SamplePut)
        {
            FExcStr = "ОБРАЗЕЦ (выкладка)! " + FExcStr;
        }
        else if (AcceptedItem.ToMode == Mode.Transfer || AcceptedItem.ToMode == Mode.NewInventory || AcceptedItem.ToMode == Mode.Harmonization || AcceptedItem.ToMode == Mode.HarmonizationPut)
        {
            if (OnShelf)
            {
                RefItem Item = new RefItem(this);
                Item.FoundID(AcceptedItem.ID);
                RefSection BindingAdress = new RefSection(this);
                BindingAdress.FoundID(AcceptedItem.BindingAdress);

                if (AcceptedItem.BindingAdressFlag)
                {
                    FExcStr = "НА ПОЛКУ! Отсканируйте адрес!"; // по умолчинию так ставим, а ниже условия которые могут этот текст поменять

                    if (!Item.ZonaHand.Selected && !BindingAdress.AdressZone.Selected)
                    {
                        FExcStr = "НА ПОЛКУ! Отсканируйте адрес: " + AcceptedItem.BindingAdressName;
                    }
                    else if (Item.ZonaHand.Selected && BindingAdress.AdressZone.Selected)
                    {
                        if (Item.ZonaHand.ID == BindingAdress.AdressZone.ID)
                        {
                            FExcStr = "НА ПОЛКУ! Отсканируйте адрес: " + AcceptedItem.BindingAdressName;
                        }
                    }
                }
                else if (AcceptedItem.ToMode == Mode.Harmonization)
                {
                    //ну не пиздец ли это???
                    FExcStr = "В ТЕЛЕЖКУ! Отсканируйте адрес!";
                }
                else
                {
                    FExcStr = "НА ПОЛКУ! Отсканируйте адрес!";
                }
            }
            else
            {
                DR = FTransferReadyItems.Select("ID = '" + AcceptedItem.ID + "'");
                if (DR.Length == 0)
                {
                    FExcStr = "В ТЕЛЕЖКУ!";
                }
                else
                {
                    AcceptedItem.IsRepeat = true;
                    FExcStr = "ВНИМАНИЕ! УЖЕ СЧИТАЛСЯ! (В ТЕЛЕЖКУ)";
                }
            }
            AcceptedItem.Count = InPartyCount;
            AcceptedItem.OnShelf = OnShelf;
        }

        FCurrentMode = Mode.AcceptedItem;
        //begin internal command
        DataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ExtendID(Employer.ID, "Спр.Сотрудники");
        DataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход2"] = ExtendID(ItemID, "Спр.Товары");
        DataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = "OpenItem (Открыл карточку)";
        if (!ExecCommandNoFeedback("Internal", DataMapWrite))
        {
            return false;
        }
        //end internal command
        return true;
    }

    AcceptedItem.ID = CurrentRowAcceptedItem["ID"].ToString();
    AcceptedItem.Name = CurrentRowAcceptedItem["ItemName"].ToString();
    AcceptedItem.InvCode = CurrentRowAcceptedItem["InvCode"].ToString();
    AcceptedItem.Acticle = CurrentRowAcceptedItem["Article"].ToString();
    AcceptedItem.Count = (int)CurrentRowAcceptedItem["Count"];
    AcceptedItem.Price = (decimal)CurrentRowAcceptedItem["Price"];
    AcceptedItem.Acceptance = true;
    AcceptedItem.Details = (int)CurrentRowAcceptedItem["Details"];
    AcceptedItem.NowDetails = AcceptedItem.Details;
    AcceptedItem.ToMode = ToMode == Mode.AcceptanceCross ? Mode.AcceptanceCross : Mode.Acceptance;
    AcceptedItem.BindingAdressFlag = false;
    AcceptedItem.SeasonGroup = (int)CurrentRowAcceptedItem["SeasonGroup"];
    AcceptedItem.FlagFarWarehouse = (int)CurrentRowAcceptedItem["FlagFarWarehouse"];
    AcceptedItem.StoregeSize = (int)CurrentRowAcceptedItem["StoregeSize"];
    if (AcceptedItem.ToMode == Mode.AcceptanceCross)
    {
        CurrentPalletAcceptedItem = new RefPalleteMove(this);
        if (CurrentRowAcceptedItem["PalletID"].ToString() != "")
        {
            CurrentPalletAcceptedItem.FoundID(CurrentRowAcceptedItem["PalletID"].ToString());
        }
    }

    if (FlagBarcode == 0)
    {
        FExcStr = AcceptedItem.InvCode.Trim() + " найден в ручную!";
    }
    else if (FlagBarcode == 1)
    {
        FExcStr = AcceptedItem.InvCode.Trim() + " найден по штрихкоду!";
    }
    else //FlagBarcode == 2
    {
        FExcStr = AcceptedItem.InvCode.Trim() + " найден по ШК МЕСТА!";
    }
    DataRow[] DRAI = FAcceptedItems.Select("ID = '" + ItemID + "' and IDDOC = '" + IDDoc + "'");
    if (DRAI.Length > 0)
    {
        FExcStr = "ПОВТОРНАЯ приемка!!! " + FExcStr;
    }
    //Добавляем что принимается не все
    if (AllCount > AcceptedItem.Count)
    {
        int Coef = 1;
        DR = FUnits.Select("OKEI = '" + OKEIPackage + "'");
        foreach (DataRow dr in DR)
        {
            Coef = (int)dr["Coef"];
            break;
        }
        FExcStr += " " + GetStrPackageCount(AcceptedItem.Count, Coef) + " из " + GetStrPackageCount(AllCount, Coef);
    }
    //begin internal command
    DataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = ExtendID(Employer.ID, "Спр.Сотрудники");
    DataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход2"] = ExtendID(ItemID, "Спр.Товары");
    DataMapWrite["Спр.СинхронизацияДанных.ДокументВход"] = ExtendID(IDDoc, "АдресПеремещение");
    DataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = "OpenItemAccept (Открыл карточку для приемки)";
    if (!ExecCommandNoFeedback("Internal", DataMapWrite))
    {
        return false;
    }
    //end internal command
    FCurrentMode = Mode.AcceptedItem;
    return true;
} // ToModeAcceptedItem
private bool ToModeAcceptedItem(string ItemID, string IDDoc)
{
    return ToModeAcceptedItem(ItemID, IDDoc, FCurrentMode == Mode.AcceptanceCross ? Mode.AcceptanceCross : Mode.Acceptance);
}

 */

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        return if (reactionKey(keyCode, event)) true else super.onKeyDown(keyCode, event)
    }

    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }
    //endregion

    private fun reactionBarcode(Barcode: String): Boolean {

        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?):Boolean {

        // нажали назад, выйдем и разблокируем доки
        if (keyCode == 4){
            return true
        }
        return false
    }

}

