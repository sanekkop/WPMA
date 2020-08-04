package com.intek.wpma.ChoiseWork.Shipping

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
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.Helpers.Helper
import com.intek.wpma.Helpers.Translation
import com.intek.wpma.R
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_loading.*


class Loading : BarcodeDataReceiver() {

    var WayBill: MutableMap<String,String> = mutableMapOf()
    var WayBillDT: MutableList<MutableMap<String,String>> = mutableListOf()
    enum class Action {Inicialization,Loading}
    var CurentAction:Action = Action.Inicialization
    var Placer:String = ""
    val Trans = Translation()
    var CurrentLine:Int = 2 //информация начинается с 3 строки в таблице
    var CurrentLineWayBillDT:MutableMap<String,String> = mutableMapOf()
    var oldx:Float = 0F
    //region шапка с необходимыми функциями для работы сканеров перехватчиков кнопок и т.д.
    var Barcode: String = ""
    var codeId: String = ""             //показатель по которому можно различать типы штрих-кодов
    val barcodeDataReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            Log.d("IntentApiSample: ", "onReceive")
            if (ACTION_BARCODE_DATA == intent.action) {
                val version = intent.getIntExtra("version", 0)
                if (version >= 1) {
                    // ту прописываем что делать при событии сканирования
                    try {
                        Barcode = intent.getStringExtra("data")
                        reactionBarcode(Barcode)
                    }
                    catch(e: Exception) {
                        val toast = Toast.makeText(applicationContext, "Не удалось отсканировать штрихкод!", Toast.LENGTH_LONG)
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
        Log.d("IntentApiSample: ", "onResume")
        if(scanRes != null){
            try {
                Barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(Barcode)
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
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {

        return if (ReactionKey(keyCode, event)) true else super.onKeyDown(keyCode, event)
    }
    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }
    //endregion

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)
        title = SS.title

        if (SS.isMobile){
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@Loading, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","Loading")
                startActivity(scanAct)
            }
        }
        btnFinishLoadingMode!!.setOnClickListener {
            //еслинажали финиш, значит переходим в режим погрузки
            if (CurentAction == Action.Inicialization) {
                CompleteLoadingInicialization()
            }
            else
            {
                CompleteLodading()
            }
        }


        ToModeLoadingInicialization()
    }

    fun CompleteLoadingInicialization() {
        if (Placer == SS.FEmployer.ID)
        {
            FExcStr.text = "Пользователь совпадает с укладчиком! Извини друг, так нельзя!"
            return
        }

        if (WayBill.isEmpty())
        {
            FExcStr.text = "Не выбран путевой лист!"
            return
        }

        //проверим чек погрузки
        var textQuery =
        "select DocWayBill.\$ПутевойЛист.ЧекПогрузка as ЧекПогрузка " +
                "from DH\$ПутевойЛист as DocWayBill (nolock) " +
                "where " +
                "DocWayBill.iddoc = :iddoc ";
        textQuery = SS.QuerySetParam(textQuery, "iddoc", WayBill["ID"].toString());
        var dataTable = SS.ExecuteWithRead(textQuery)
        if (dataTable!![1][0].toInt() == 0 )
        {
            //погрузка не разрешена
            FExcStr.text = "Погрузка запрещена!"
            return;
        }

        //Проверим еще раз не засрал ли путевой кто-нибудь еще
        textQuery =
            "SELECT " +
                    "PL.iddoc as iddoc " +
                    "FROM DH\$ПутевойЛист as PL (nolock) " +
                    "INNER JOIN _1sjourn as journ (nolock) " +
                    "ON journ.iddoc = PL.iddoc " +
                    "WHERE " +
                    "PL.\$ПутевойЛист.Дата1 = :EmptyDate " +
                    "and journ.ismark = 0 " +
                    "and journ.iddoc = :iddoc " +
                    "ORDER BY journ.date_time_iddoc";
        textQuery = SS.QuerySetParam(textQuery, "EmptyDate",   SS.GetVoidDate());
        textQuery = SS.QuerySetParam(textQuery, "iddoc",       WayBill["ID"].toString());
        dataTable = SS.ExecuteWithRead(textQuery)

        if (dataTable == null){
            return
        }
        if (dataTable.isEmpty())
        {
            //уже кто-то взял поймем куда нас пихать если он еще не закрыт
            textQuery =
                "SELECT " +
                        "\$ПутевойЛист.Грузчик as Loader, " +
                        "\$ПутевойЛист.Укладчик as Placer, " +
                        "\$ПутевойЛист.Укладчик2 as Placer2, " +
                        "\$ПутевойЛист.Укладчик3 as Placer3 " +
                        "FROM DH\$ПутевойЛист as PL (nolock) " +
                        "INNER JOIN _1sjourn as journ (nolock) " +
                        "ON journ.iddoc = PL.iddoc " +
                        "WHERE " +
                        "PL.\$ПутевойЛист.Дата2 = :EmptyDate " +
                        "and journ.ismark = 0 " +
                        "and journ.iddoc = :iddoc " +
                        "and (\$ПутевойЛист.Грузчик = :EmptyLoader " +
                        "or \$ПутевойЛист.Укладчик = :EmptyLoader " +
                        "or \$ПутевойЛист.Укладчик2 = :EmptyLoader " +
                        "or \$ПутевойЛист.Укладчик3 = :EmptyLoader )" +
                        "ORDER BY journ.date_time_iddoc";
            textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate());
            textQuery = SS.QuerySetParam(textQuery, "iddoc", WayBill["ID"].toString());
            textQuery = SS.QuerySetParam(textQuery, "EmptyLoader", SS.GetVoidID());

            dataTable = SS.ExecuteWithRead(textQuery)
            if (dataTable == null){
                return
            }
            if (dataTable.isEmpty())
            {
                FExcStr.text = "Путевой закрыт, удален или укомплектован сотрудникаим!"
                return
            }

            textQuery =
                "UPDATE DH\$ПутевойЛист " +
                        "SET " +
                        "\$ПутевойЛист.Грузчик    = :Loader, " +
                        "\$ПутевойЛист.Укладчик   = :Placer_1, " +
                        "\$ПутевойЛист.Укладчик2  = :Placer_2, " +
                        "\$ПутевойЛист.Укладчик3  = :Placer_3 " +
                        "WHERE " +
                        "DH\$ПутевойЛист .iddoc = :iddoc;";
            var findeEmpty = false
            if (dataTable[1][0].toString() == SS.GetVoidID())
            {
                textQuery = SS.QuerySetParam(textQuery, "Loader", SS.FEmployer.ID);
                findeEmpty = true;
            }
            else
            {
                textQuery = SS.QuerySetParam(textQuery, "Loader", dataTable[1][0].toString());
            }
            if (dataTable[1][1].toString() == SS.GetVoidID() && !findeEmpty)
            {
                textQuery = SS.QuerySetParam(textQuery, "Placer_1", SS.FEmployer.ID);
                findeEmpty = true;
            }
            else
            {
                textQuery = SS.QuerySetParam(textQuery, "Placer_1", dataTable[1][1].toString());
            }
            if (dataTable[1][2].toString() == SS.GetVoidID() && !findeEmpty)
            {
                textQuery = SS.QuerySetParam(textQuery, "Placer_2", SS.FEmployer.ID);
                findeEmpty = true;
            }
            else
            {
                textQuery = SS.QuerySetParam(textQuery, "Placer2", dataTable[1][2].toString());
            }
            if (dataTable[1][3].toString() == SS.GetVoidID() && !findeEmpty)
            {
                textQuery = SS.QuerySetParam(textQuery, "Placer_3", SS.FEmployer.ID);
                findeEmpty = true;
            }
            else
            {
                textQuery = SS.QuerySetParam(textQuery, "Placer3", dataTable[1][3].toString());
            }
            textQuery = SS.QuerySetParam(textQuery, "iddoc", WayBill["ID"].toString());
            if (!findeEmpty)
            {
                //все строки заполнены и нас там нет облом
                FExcStr.text = "Этот путевой уже укомплектован сотрудникаим!"
                return
            }

        }
        else
        {
            textQuery =
                "UPDATE DH\$ПутевойЛист " +
                        "SET " +
                        "\$ПутевойЛист.Грузчик   = :Loader, " +
                        "\$ПутевойЛист.Укладчик  = :Placer, " +
                        "\$ПутевойЛист.Дата1     = :NowDate, " +
                        "\$ПутевойЛист.Время1    = :NowTime " +
                        "WHERE " +
                        "DH\$ПутевойЛист .iddoc = :iddoc;"
            textQuery = SS.QuerySetParam(textQuery, "Loader", SS.FEmployer.ID);
            textQuery = SS.QuerySetParam(textQuery, "Placer", if (Placer != "") Placer else SS.GetVoidID())
            textQuery = SS.QuerySetParam(textQuery, "iddoc", WayBill["ID"].toString());

        }

        dataTable = SS.ExecuteWithRead(textQuery)
        if (dataTable == null){
            return
        }
        ToModeLoading(WayBill["ID"].toString())
    }
    fun CompleteLodading() {
        var idSchet = "   " + Trans.DecTo36(SS.GetSynh("Счет"));
        idSchet = idSchet.substring(idSchet.length - 4);
        var idClaim = "   " + Trans.DecTo36(SS.GetSynh("ПретензияОтКлиента"));
        idClaim = idClaim.substring(idClaim.length - 4);

        //проверим чек погрузки
        var textQuery =
            "select DocWayBill.\$ПутевойЛист.ЧекПогрузка as ЧекПогрузка " +
                    "from DH\$ПутевойЛист as DocWayBill (nolock) " +
                    "where " +
                    "DocWayBill.iddoc = :iddoc ";
        textQuery = SS.QuerySetParam(textQuery, "iddoc", WayBill["ID"].toString());
        var dataTable = SS.ExecuteWithRead(textQuery)
        if (dataTable!![1][0].toInt() == 0 )
        {
            //погрузка не разрешена
            FExcStr.text = "Погрузка запрещена!"
            return;
        }

        textQuery =
            "SELECT " +
                    "Main.DocFull as DocFull " +
                    "FROM (" + GiveSomeOneQueryText() +
                    ") as Main " +
                    "INNER JOIN (" +
                    "SELECT " +
                    "Boxes.\$Спр.МестаПогрузки.Док as DocID " +
                    "FROM \$Спр.МестаПогрузки as Boxes (nolock) " +
                    "WHERE Boxes.ismark = 0 and Boxes.\$Спр.МестаПогрузки.Дата6 = :EmptyDate " +
                    "GROUP BY Boxes.\$Спр.МестаПогрузки.Док " +
                    ") as Boxes " +
                    "ON Boxes.DocID = Main.DocFull " +
                    "";
        textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate());
        textQuery = SS.QuerySetParam(textQuery, "EmptyID", SS.GetVoidID());
        textQuery = SS.QuerySetParam(textQuery, "iddoc", WayBill["ID"].toString());

        dataTable = SS.ExecuteWithRead(textQuery)
        if (dataTable == null){
            return
        }
        if (!dataTable.isEmpty())
        {
            FExcStr.text = "Не все погружено"
            return
        }

        textQuery =
            "UPDATE DH\$ПутевойЛист " +
                    "SET " +
                    "\$ПутевойЛист.Дата2 = :NowData, " +
                    "\$ПутевойЛист.Время2 = :NowTime " +
                    "WHERE " +
                    "DH\$ПутевойЛист .iddoc = :iddoc";

        textQuery = SS.QuerySetParam(textQuery, "iddoc", WayBill["ID"].toString());

        dataTable = SS.ExecuteWithRead(textQuery)
        if (dataTable == null){
            return
        }
        val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
        shoiseWorkInit.putExtra("ParentForm", "Loading")
        startActivity(shoiseWorkInit)
        finish()
    }

    fun GiveSomeOneQueryText():String {
        var idSchet = "   " + Trans.DecTo36(SS.GetSynh("Счет"))
        idSchet = idSchet.substring(idSchet.length - 4)
        var idClaim = "   " + Trans.DecTo36(SS.GetSynh("ПретензияОтКлиента"))
        idClaim = idClaim.substring(idClaim.length - 4)
        return "SELECT " +
                "PL.\$ПутевойЛист.ИндексРазгрузки as AdressCounter, " +
                "CASE " +
                "WHEN journ.iddocdef = \$Счет THEN '" + idSchet + "' + journ.iddoc " +
                "WHEN journ.iddocdef = \$ПретензияОтКлиента THEN '" + idClaim + "' + Claim.iddoc " +
                "WHEN journ.iddocdef = \$РасходнаяРеализ THEN '" + idSchet + "' + journProposal_RR.iddoc " +
                "WHEN journ.iddocdef = \$Перемещение THEN '" + idSchet + "' + journProposal_Per.iddoc " +
                "WHEN not journProposal.iddoc is null THEN '" + idSchet + "' + journProposal.iddoc " +
                "ELSE '   0' + :EmptyID END as DocFull, " +
                "CASE " +
                "WHEN journ.iddocdef = \$Счет THEN journ.iddoc " +
                "WHEN journ.iddocdef = \$ПретензияОтКлиента THEN Claim.iddoc " +
                "WHEN journ.iddocdef = \$РасходнаяРеализ THEN journProposal_RR.iddoc " +
                "WHEN journ.iddocdef = \$Перемещение THEN journProposal_Per.iddoc " +
                "WHEN not journProposal.iddoc is null THEN journProposal.iddoc " +
                "ELSE :EmptyID END as Doc, " +
                "ISNULL(RK.\$РасходнаяКредит.АдресДоставки , Bill.\$Счет.АдресДоставки ) as Adress, " +
                "PL.lineno_ as Number " +
                "FROM DT\$ПутевойЛист as PL (nolock) " +
                "LEFT JOIN _1sjourn as journ (nolock) " +
                "ON journ.iddoc = right(PL.\$ПутевойЛист.ДокументДоставки , 9) " +
                "LEFT JOIN DH\$РасходнаяКредит as RK (nolock) " +
                "ON RK.iddoc = journ.iddoc " +
                "LEFT JOIN DH\$РасходнаяРеализ as RR (nolock) " +
                "ON RR.iddoc = journ.iddoc " +
                "LEFT JOIN DH\$Перемещение as Per (nolock) " +
                "ON Per.iddoc = journ.iddoc " +
                "LEFT JOIN _1sjourn as journProposal (nolock) " +
                "ON right(RK.\$РасходнаяКредит.ДокументОснование , 9) = journProposal.iddoc " +
                "LEFT JOIN _1sjourn as journProposal_RR (nolock) " +
                "ON right(RR.\$РасходнаяРеализ.ДокументОснование , 9) = journProposal_RR.iddoc " +
                "LEFT JOIN _1sjourn as journProposal_Per (nolock) " +
                "ON right(Per.\$Перемещение.ДокументОснование , 9) = journProposal_Per.iddoc " +
                "LEFT JOIN DH\$Счет as Bill (nolock) " +
                "ON Bill.iddoc = journProposal.iddoc or Bill.iddoc = journ.iddoc " +
                "LEFT JOIN DH\$ПретензияОтКлиента as Claim (nolock) " +
                "ON Claim.iddoc = journ.iddoc " +
                "WHERE " +
                "PL.iddoc = :iddoc " +
                "and journ.iddocdef in (\$Счет , \$РасходнаяКредит , \$ПретензияОтКлиента , \$РасходнаяРеализ , \$Перемещение )"
    }

    fun ToModeLoadingInicialization() {
        var textQuery =
        "SELECT " +
                "PL.iddoc as iddoc " +
                "FROM DH\$ПутевойЛист as PL (nolock) " +
                "INNER JOIN _1sjourn as journ (nolock) " +
                "ON journ.iddoc = PL.iddoc " +
                "WHERE " +
                "(PL.\$ПутевойЛист.Грузчик = :Employer " +
                "OR PL.\$ПутевойЛист.Укладчик = :Employer " +
                "OR PL.\$ПутевойЛист.Укладчик2 = :Employer " +
                "OR PL.\$ПутевойЛист.Укладчик3 = :Employer )" +
                "and not PL.\$ПутевойЛист.Дата1 = :EmptyDate " +
                "and PL.\$ПутевойЛист.Дата2 = :EmptyDate " +
                "and journ.ismark = 0 " +
                "ORDER BY journ.date_time_iddoc";
        textQuery = SS.QuerySetParam(textQuery, "Employer", SS.FEmployer.ID);
        textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate());
        var DT = SS.ExecuteWithReadNew(textQuery)
        if (DT == null){
           return
        }

        if (!DT.isEmpty())
        {
            //существует документ!
            ToModeLoading(DT[0]["iddoc"].toString())
            return
        }
        RefreshActivity()
        return
    }
    fun ToModeLoading(iddoc:String) {
        //Если wayBill еще не выбран, то испавим это недоразумение

        if (WayBill.isEmpty() || WayBill["ID"] != iddoc)
        {
            var DataMap = SS.GetDoc(iddoc, true)

            if (DataMap == null)
            {
                return
            }
            WayBill = DataMap
            WayBill.put("View",DataMap["НомерДок"].toString() + " (" + DataMap["ДатаДок"].toString() + ")")
        }

        //проверим чек погрузки
        var textQuery =
            "select DocWayBill.\$ПутевойЛист.ЧекПогрузка as ЧекПогрузка " +
                    "from DH\$ПутевойЛист as DocWayBill (nolock) " +
                    "where " +
                    "DocWayBill.iddoc = :iddoc ";
        textQuery = SS.QuerySetParam(textQuery, "iddoc", WayBill["ID"].toString());
        var dataTable = SS.ExecuteWithRead(textQuery)
        if (dataTable!![1][0].toInt() == 0 )
        {
            //погрузка не разрешена
            FExcStr.text = "Погрузка запрещена!"
            return;
        }

        textQuery =
            "SELECT " +
                    "Main.AdressCounter as AdressCounter, " +
                    "Main.Adress as Adress, " +
                    "ISNULL(RefSection.descr, 'Нет адреса') as AdressCompl, " +
                    "Main.Doc as Doc, " +
                    "Journ.docno as ProposalNumber, " +
                    "CAST(LEFT(journ.date_time_iddoc,8) as DateTime) as ProposalDate, " +
                    "ISNULL(Boxes.CountBox, 0) as Boxes, " +
                    "ISNULL(BoxesComplete.CountBox, 0) as BoxesFact, " +
                    "Main.Number as Number " +
                    "FROM ( " + GiveSomeOneQueryText() +
                    ") as Main " +
                    "LEFT JOIN (" +
                    "SELECT " +
                    "Boxes.\$Спр.МестаПогрузки.Док as DocID, " +
                    "Boxes.\$Спр.МестаПогрузки.Адрес9  as AdressCompl, " +
                    "Count(*) as CountBox " +
                    "FROM \$Спр.МестаПогрузки as Boxes (nolock) " +
                    "WHERE Boxes.ismark = 0 " +
                    "GROUP BY Boxes.\$Спр.МестаПогрузки.Док , Boxes.\$Спр.МестаПогрузки.Адрес9 " +
                    ") as Boxes " +
                    "ON Boxes.DocID = Main.DocFull " +
                    "LEFT JOIN (" +
                    "SELECT " +
                    "Boxes.\$Спр.МестаПогрузки.Док as DocID, " +
                    "Boxes.\$Спр.МестаПогрузки.Адрес9 as AdressCompl, " +
                    "Count(*) as CountBox " +
                    "FROM \$Спр.МестаПогрузки as Boxes (nolock) " +
                    "WHERE Boxes.ismark = 0 and not Boxes.\$Спр.МестаПогрузки.Дата6 = :EmptyDate " +
                    "GROUP BY Boxes.\$Спр.МестаПогрузки.Док , Boxes.\$Спр.МестаПогрузки.Адрес9 " +
                    ") as BoxesComplete " +
                    "ON BoxesComplete.DocID = Main.DocFull " +
                    "and Boxes.AdressCompl = BoxesComplete.AdressCompl " +
                    "LEFT JOIN _1sjourn as journ (nolock) " +
                    "ON journ.iddoc = Main.Doc " +
                    "LEFT JOIN \$Спр.Секции as RefSection (nolock) " +
                    "ON RefSection.id = Boxes.AdressCompl OR RefSection.id = BoxesComplete.AdressCompl " +
                    "WHERE " +
                    "not ISNULL(BoxesComplete.CountBox, 0) = ISNULL(Boxes.CountBox, 0) " +
                    "and not ((ISNULL(journ.iddocdef,'') = \$ПретензияОтКлиента ) " +
                    "and (SUBSTRING(ISNULL(RefSection.descr, '80'),1,2) = '80')) " +
                    "ORDER BY Main.AdressCounter desc, Main.Number desc " +
                    "";

        textQuery = SS.QuerySetParam(textQuery, "EmptyDate", SS.GetVoidDate());
        textQuery = SS.QuerySetParam(textQuery, "EmptyID", SS.GetVoidID());
        textQuery = SS.QuerySetParam(textQuery, "iddoc", WayBill["ID"].toString());
        WayBillDT = SS.ExecuteWithReadNew(textQuery)!!
        CurrentLine = 2
        CurrentLineWayBillDT.put("ProposalNumber",WayBillDT[0]["ProposalNumber"].toString())
        CurrentLineWayBillDT.put("Doc", WayBillDT[0]["Doc"].toString())
        CurrentLineWayBillDT.put("AdressCounter",WayBillDT[0]["AdressCounter"].toString())
        CurentAction = Action.Loading
        RefreshActivity()
        return
    }

    private fun reactionBarcode(Barcode: String): Boolean {

        val helper: Helper = Helper()
        val barcoderes = helper.DisassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if(typeBarcode == "6")
        {
            val id = barcoderes["ID"].toString()
            if (SS.IsSC(id, "МестаПогрузки")) {
                //проверим что это то что нам нужно
                //проверим чек погрузки
                var textQuery =
                "select DocWayBill.\$ПутевойЛист.ЧекПогрузка " +
                        "from DH\$ПутевойЛист as DocWayBill (nolock) " +
                        "where " +
                        "DocWayBill.iddoc = :iddoc ";
                textQuery = SS.QuerySetParam(textQuery, "iddoc", WayBill["ID"].toString());
                var dataTable = SS.ExecuteWithRead(textQuery)
                if (dataTable!![1][0].toInt() == 0 )
                {
                    //погрузка не разрешена
                    FExcStr.text = "Погрузка запрещена!"
                    BadVoise()
                    return false
                }
                textQuery =
                    "Select " +
                            "\$Спр.МестаПогрузки.Дата6 as Date, " +
                            "right(\$Спр.МестаПогрузки.Док , 9) as Doc " +
                            "from \$Спр.МестаПогрузки (nolock) where id = :id";
                textQuery = SS.QuerySetParam(textQuery, "id", id);
                textQuery = SS.QuerySetParam(textQuery, "EmptyID", SS.GetVoidID());
                var DT = SS.ExecuteWithReadNew(textQuery)
                if (DT == null )
                {
                    //погрузка не разрешена
                    BadVoise()
                    return false
                }

                if (DT.isEmpty())
                {

                    FExcStr.text = "Нет действий с данным штрихкодом в этом режиме!";
                    //блокируем путевой
                    /*TextQuery =
                                   "UPDATE DH$ПутевойЛист " +
                                       "SET " +
                                           "$ПутевойЛист.ЧекПогрузка = 0 " +
                                   "WHERE " +
                                       "DH$ПутевойЛист .iddoc = :id";

                    QuerySetParam(ref TextQuery, "id", WayBill.ID);
                    ExecuteWithoutRead(TextQuery);
                    */
                    BadVoise()
                    return false
                }
                if (!SS.IsVoidDate(DT[0]["Date"].toString()))
                {
                    FExcStr.text = "Ошибка! Место уже погружено!";
                    //блокируем путевой
                    /*TextQuery =
                                   "UPDATE DH$ПутевойЛист " +
                                       "SET " +
                                           "$ПутевойЛист.ЧекПогрузка = 0 " +
                                   "WHERE " +
                                       "DH$ПутевойЛист .iddoc = :id";

                    QuerySetParam(ref TextQuery, "id", WayBill.ID);
                    ExecuteWithoutRead(TextQuery);
                    */
                    BadVoise()
                    return false
                }

                //теперь проверяем та ли это строка
                var indexWayBill = -1
                for (DR in WayBillDT)
                {
                    if (DR["Doc"].toString() == DT[0]["Doc"].toString())
                    {
                        //это наш документ, запомним индекс данной строки
                        indexWayBill = DR["AdressCounter"].toString().toInt()
                        break
                    }
                }

                if (indexWayBill == -1)
                {
                    //не нашли в путевом
                    FExcStr.text = "Не числится в данном путевом!";
                    //блокируем путевой
                    textQuery =
                        "UPDATE DH\$ПутевойЛист " +
                                "SET " +
                                "\$ПутевойЛист.ЧекПогрузка = 0 " +
                                "WHERE " +
                                "DH\$ПутевойЛист .iddoc = :id";

                    textQuery = SS.QuerySetParam(textQuery, "id", WayBill["ID"].toString())
                    SS.ExecuteWithoutRead(textQuery);
                    BadVoise()
                    return false
                }

                if (SS.Const.OrderControl) {
                    val currCounter = WayBillDT[0]["AdressCounter"].toString().toInt()
                    if (currCounter > indexWayBill) {
                        FExcStr.text = "Нарушена последовательность погрузки!";
                        //блокируем путевой
                        /*
                           textQuery =
                        "UPDATE DH\$ПутевойЛист " +
                                "SET " +
                                "\$ПутевойЛист.ЧекПогрузка = 0 " +
                                "WHERE " +
                                "DH\$ПутевойЛист .iddoc = :id";

                    textQuery = SS.QuerySetParam(textQuery, "id", WayBill["ID"].toString())
                    SS.ExecuteWithoutRead(textQuery);
                             */
                        BadVoise()
                        return false;
                    }

                }

                textQuery =
                    "UPDATE \$Спр.МестаПогрузки " +
                            "SET " +
                            "\$Спр.МестаПогрузки.Дата6 = :NowDate, " +
                            "\$Спр.МестаПогрузки.Время6 = :NowTime " +
                            "WHERE " +
                            "\$Спр.МестаПогрузки .id = :id";

                textQuery = SS.QuerySetParam(textQuery, "id", id);

                if (!SS.ExecuteWithoutRead(textQuery))
                {
                    FExcStr.text = "Ошибка фиксации погрузки";
                    BadVoise()
                    return false
                }

                FExcStr.text = "Погрузка МЕСТА зафиксирована";
                ToModeLoading(WayBill["ID"].toString())
            }
            else {
                FExcStr.text = "Неверно! Отсканируйте коробку."
                BadVoise()
                return false
            }
        }
        else {
            FExcStr.text = "Нет действий с данным ШК! Отсканируйте коробку."
            BadVoise()
            return false
        }
        GoodVoise()
        return true
    }

    private fun ReactionKey(keyCode: Int, event: KeyEvent?):Boolean {

        // нажали назад, выйдем
        if (keyCode == 4){
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            shoiseWorkInit.putExtra("ParentForm", "Loading")
            startActivity(shoiseWorkInit)
            finish()
            return true
        }
        if (SS.helper.WhatDirection(keyCode) == "Right")
        {
            //переход в просмотр состояния
            val showInfo = Intent(this, ShowInfo::class.java)
            showInfo.putExtra("ParentForm", "Loading")
            showInfo.putExtra("Number",CurrentLineWayBillDT["ProposalNumber"].toString())
            showInfo.putExtra("Doc",CurrentLineWayBillDT["Doc"].toString())
            startActivity(showInfo)
            finish()
            return true
        }
        else if(SS.helper.WhatDirection(keyCode) in listOf<String>("Down","Up")){
            table.getChildAt(CurrentLine).isFocusable = false
            table.getChildAt(CurrentLine).setBackgroundColor(Color.WHITE)
            if (SS.helper.WhatDirection(keyCode) == "Down")
            {
                if (CurrentLine -1 < WayBillDT.count()) CurrentLine++ else CurrentLine = 2
            }
            else {
                if (CurrentLine > 2) CurrentLine-- else CurrentLine = WayBillDT.count()+1
            }
            CurrentLineWayBillDT.put(
                    "ProposalNumber",
                    WayBillDT[CurrentLine-2]["ProposalNumber"].toString()
                )
                CurrentLineWayBillDT.put("Doc", WayBillDT[CurrentLine-2]["Doc"].toString())
                CurrentLineWayBillDT.put(
                    "AdressCounter",
                    WayBillDT[CurrentLine-2]["AdressCounter"].toString()
                )
            //теперь подкрасим строку серым
            table.getChildAt(CurrentLine).setBackgroundColor(Color.GRAY)
            table.getChildAt(CurrentLine).isFocusable = true
            return true
        }
        return false
    }


    @SuppressLint("ClickableViewAccessibility")
    fun RefreshActivity()    {
        //пока не отсканировали путевой обновлять нечего
        if (WayBill.isEmpty())
        {
            lblPlacer.text = "Путевой: <не выбран>"
            FExcStr.text = "Отсканируйте путевой лист"
            return
        }
        //путевой есть, надо подтянуть его в название
        lblPlacer.text = "Путевой: " + WayBill["НомерДок"] + " (" + WayBill["ДатаДок"] + ") Укладчик: "
        //теперь укладчик
        if (Placer == "")
        {
            lblPlacer.text = lblPlacer.text.toString() + "<не выбран>"
        }
        else
        {
            lblPlacer.text = lblPlacer.text.toString() + Placer
        }
        if (CurentAction == Action.Inicialization)
        {
            //это инициализация дальше ничего не надо делать пока
            lblPlacer.visibility = View.VISIBLE
            return
        }
        lblPlacer.visibility = View.INVISIBLE

        table.removeAllViewsInLayout()

        //строка с шапкой
        val linearLayoutDoc = LinearLayout(this)
        val rowTitleDoc = TableRow(this)

        val DocumName = TextView(this)
        DocumName.text = WayBill["НомерДок"] + " (" + WayBill["ДатаДок"] + ")"
        DocumName.gravity = Gravity.LEFT
        DocumName.textSize = 20F
        DocumName.setTextColor(-0x1000000)
        DocumName.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        linearLayoutDoc.addView(DocumName)
        rowTitleDoc.addView(linearLayoutDoc)
        table.addView(rowTitleDoc)

        val linearLayout = LinearLayout(this)
        val rowTitle = TableRow(this)

        //добавим столбцы
        val number = TextView(this)
        number.text = "№"
        number.typeface = Typeface.SERIF
        number.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.09).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        number.gravity = Gravity.CENTER
        number.textSize = 12F
        number.setTextColor(-0x1000000)
        val docum = TextView(this)
        docum.text = "Документ"
        docum.typeface = Typeface.SERIF
        docum.gravity = Gravity.CENTER
        docum.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.28).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        docum.textSize = 12F
        docum.setTextColor(-0x1000000)
        val address = TextView(this)
        address.text = "Адрес"
        address.typeface = Typeface.SERIF
        address.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.35).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        address.gravity = Gravity.CENTER
        address.textSize = 12F
        address.setTextColor(-0x1000000)
        val boxes = TextView(this)
        boxes.text = "Мест"
        boxes.typeface = Typeface.SERIF
        boxes.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.14).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        boxes.gravity = Gravity.CENTER
        boxes.textSize = 12F
        boxes.setTextColor(-0x1000000)
        val boxesfact = TextView(this)
        boxesfact.text = "Факт"
        boxesfact.typeface = Typeface.SERIF
        boxesfact.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.14).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
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
        var linenom = 2 //2 строки шапки не считаем

        for (rowDT in WayBillDT)
        {
            //строки теперь
            val rowTitle = TableRow(this)
            rowTitle.isClickable = true
            rowTitle.setOnTouchListener { v, event ->

                if (event.action == MotionEvent.ACTION_DOWN) {
                    oldx = event.x
                    var i = 0
                    while (i < table.childCount)
                    {
                        if (rowTitle != table.getChildAt(i))
                        {
                            table.getChildAt(i).setBackgroundColor(Color.WHITE)
                        }
                        else
                        {
                            CurrentLine = i
                            rowTitle.setBackgroundColor(Color.GRAY)
                            for (rowCDT in WayBillDT) {
                                if (((rowTitle.getChildAt(0) as ViewGroup).getChildAt(1) as TextView).text.toString() == rowCDT["ProposalNumber"]) {
                                    CurrentLineWayBillDT.put("ProposalNumber",rowCDT["ProposalNumber"].toString())
                                    CurrentLineWayBillDT.put("Doc", rowCDT["Doc"].toString())
                                    CurrentLineWayBillDT.put("AdressCounter", rowCDT["AdressCounter"].toString())
                                }
                            }
                        }
                        i++
                    }
                    true
                } else if (event.action == MotionEvent.ACTION_MOVE) {
                    if (event.x > oldx) {
                        val showInfo = Intent(this, ShowInfo::class.java)
                        showInfo.putExtra("ParentForm", "Loading")
                        showInfo.putExtra("Number",CurrentLineWayBillDT["ProposalNumber"].toString())
                        showInfo.putExtra("Doc",CurrentLineWayBillDT["Doc"].toString())
                        startActivity(showInfo)
                        finish()
                    }
                }
                true
            }
            val linearLayout = LinearLayout(this)
            var colorline =  Color.WHITE
            if (linenom == CurrentLine)
            {
                colorline = Color.GRAY
            }
            rowTitle.setBackgroundColor(colorline)
            //добавим столбцы
            val number = TextView(this)
            number.text = rowDT["AdressCounter"]
            number.typeface = Typeface.SERIF
            number.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.09).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            number.gravity = Gravity.CENTER
            number.textSize = 12F
            number.setTextColor(-0x1000000)
            val docum = TextView(this)
            docum.text = rowDT["ProposalNumber"]
            docum.typeface = Typeface.SERIF
            docum.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.28).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            docum.gravity = Gravity.CENTER
            docum.textSize = 12F
            docum.setTextColor(-0x1000000)
            val address = TextView(this)
            address.text = rowDT["AdressCompl"]?.trim()
            address.typeface = Typeface.SERIF
            address.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.35).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            address.gravity = Gravity.CENTER
            address.textSize = 12F
            address.setTextColor(-0x1000000)
            val boxes = TextView(this)
            boxes.text = rowDT["Boxes"]
            boxes.typeface = Typeface.SERIF
            boxes.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.14).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            boxes.gravity = Gravity.CENTER
            boxes.textSize = 12F
            boxes.setTextColor(-0x1000000)
            val boxesfact = TextView(this)
            boxesfact.text = rowDT["BoxesFact"]
            boxesfact.typeface = Typeface.SERIF
            boxesfact.layoutParams = LinearLayout.LayoutParams((SS.widthDisplay*0.14).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
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
            linenom++
        }

    }

}
