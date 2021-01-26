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
import com.intek.wpma.*
import com.intek.wpma.Helpers.Helper
import com.intek.wpma.Helpers.Translation
import com.intek.wpma.Ref.Doc
import com.intek.wpma.Ref.RefEmployer
import kotlinx.android.synthetic.main.activity_downing.*
import kotlinx.android.synthetic.main.activity_loading.*
import kotlinx.android.synthetic.main.activity_loading.FExcStr
import kotlinx.android.synthetic.main.activity_loading.btnScan
import kotlinx.android.synthetic.main.activity_loading.lblPlacer
import kotlinx.android.synthetic.main.activity_loading.table
import kotlinx.android.synthetic.main.activity_show_box.*
import kotlinx.android.synthetic.main.activity_unloading.*


class Loading : BarcodeDataReceiver() {

    private var wayBill: MutableMap<String,String> = mutableMapOf()
    private var wayBillDT: MutableList<MutableMap<String,String>> = mutableListOf()
    enum class Action {Inicialization,Loading}
    private var curentAction:Action = Action.Inicialization
    private var placer = RefEmployer()
    private val trans = Translation()
    private var currentLine:Int = 1 //информация начинается с 2 строки в таблице
    private var currentLineWayBillDT:MutableMap<String,String> = mutableMapOf()
    private var oldx:Float = 0F

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
                    }
                    catch(e: Exception) {
                        FExcStr.text = "Не удалось отсканировать штрихкод!" + e.toString()
                        badVoise()
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
        if(scanRes != null){
            try {
                barcode = scanRes.toString()
                codeId = scanCodeId.toString()
                reactionBarcode(barcode)
            }
            catch (e: Exception){
                FExcStr.text = e.toString()
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
    }
    //endregion

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_loading)
        title = ss.title

        if (ss.isMobile){
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                val scanAct = Intent(this@Loading, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","Loading")
                startActivity(scanAct)
            }
        }
        btnFinishLoadingMode!!.setOnClickListener {
            //еслинажали финиш, значит переходим в режим погрузки
            if (curentAction == Action.Inicialization) {
                completeLoadingInicialization()
            }
            else
            {
                completeLodading()
            }
        }
        FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x > oldx + 200) {
                    val showInfo = Intent(this, ShowInfo::class.java)
                    showInfo.putExtra("Number",currentLineWayBillDT["ProposalNumber"].toString())
                    showInfo.putExtra("Doc",currentLineWayBillDT["Doc"].toString())
                    startActivity(showInfo)
                    finish()
                }
                if (event.x < oldx - 200) {
                    val showInfo = Intent(this, ShowBox::class.java)
                    showInfo.putExtra("AdressCompl",currentLineWayBillDT["AdressCompl"].toString())
                    showInfo.putExtra("Doc",currentLineWayBillDT["Doc"].toString())
                    startActivity(showInfo)
                    finish()
                }
            }
            return true
        })

        toModeLoadingInicialization()
    }

    private fun completeLoadingInicialization() {
        if (placer.id == ss.FEmployer.id)
        {
            FExcStr.text = "Пользователь совпадает с укладчиком! Извини друг, так нельзя!"
            return
        }

        if (wayBill.isEmpty())
        {
            FExcStr.text = "Не выбран путевой лист!"
            return
        }

        //проверим чек погрузки
        var textQuery =
        "select DocWayBill.\$ПутевойЛист.ЧекПогрузка as ЧекПогрузка " +
                "from DH\$ПутевойЛист as DocWayBill (nolock) " +
                "where " +
                "DocWayBill.iddoc = :iddoc "
        textQuery = ss.querySetParam(textQuery, "iddoc", wayBill["ID"].toString())
        var dataTable = ss.executeWithRead(textQuery)
        if (dataTable!![1][0].toInt() == 0 )
        {
            //погрузка не разрешена
            FExcStr.text = "Погрузка запрещена!"
            return
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
                    "ORDER BY journ.date_time_iddoc"
        textQuery = ss.querySetParam(textQuery, "EmptyDate",   ss.getVoidDate())
        textQuery = ss.querySetParam(textQuery, "iddoc",       wayBill["ID"].toString())
        dataTable = ss.executeWithRead(textQuery)

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
                        "ORDER BY journ.date_time_iddoc"
            textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
            textQuery = ss.querySetParam(textQuery, "iddoc", wayBill["ID"].toString())
            textQuery = ss.querySetParam(textQuery, "EmptyLoader", ss.getVoidID())

            dataTable = ss.executeWithRead(textQuery)
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
                        "DH\$ПутевойЛист .iddoc = :iddoc"
            var findeEmpty = false
            if (dataTable[1][0] == ss.getVoidID())
            {
                textQuery = ss.querySetParam(textQuery, "Loader", ss.FEmployer.id)
                findeEmpty = true
            }
            else
            {
                textQuery = ss.querySetParam(textQuery, "Loader", dataTable[1][0])
            }
            if (dataTable[1][1] == ss.getVoidID() && !findeEmpty)
            {
                textQuery = ss.querySetParam(textQuery, "Placer_1", ss.FEmployer.id)
                findeEmpty = true
            }
            else
            {
                textQuery = ss.querySetParam(textQuery, "Placer_1", dataTable[1][1])
            }
            if (dataTable[1][2] == ss.getVoidID() && !findeEmpty)
            {
                textQuery = ss.querySetParam(textQuery, "Placer_2", ss.FEmployer.id)
                findeEmpty = true
            }
            else
            {
                textQuery = ss.querySetParam(textQuery, "Placer_2", dataTable[1][2])
            }
            if (dataTable[1][3] == ss.getVoidID() && !findeEmpty)
            {
                textQuery = ss.querySetParam(textQuery, "Placer_3", ss.FEmployer.id)
                findeEmpty = true
            }
            else
            {
                textQuery = ss.querySetParam(textQuery, "Placer_3", dataTable[1][3])
            }
            textQuery = ss.querySetParam(textQuery, "iddoc", wayBill["ID"].toString())
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
            textQuery = ss.querySetParam(textQuery, "Loader", ss.FEmployer.id)
            textQuery = ss.querySetParam(textQuery, "Placer", if (placer.selected) placer.id else ss.getVoidID())
            textQuery = ss.querySetParam(textQuery, "iddoc", wayBill["ID"].toString())

        }
        if (!ss.executeWithoutRead(textQuery)) {
            FExcStr.text = "Ошибка фиксации задания!"
            return
        }
        toModeLoading(wayBill["ID"].toString())
    }
    private fun completeLodading() {
        var idSchet = "   " + trans.decTo36(ss.getSynh("Счет"))
        idSchet = idSchet.substring(idSchet.length - 4)
        var idClaim = "   " + trans.decTo36(ss.getSynh("ПретензияОтКлиента"))
        idClaim = idClaim.substring(idClaim.length - 4)

        //проверим чек погрузки
        var textQuery =
            "select DocWayBill.\$ПутевойЛист.ЧекПогрузка as ЧекПогрузка " +
                    "from DH\$ПутевойЛист as DocWayBill (nolock) " +
                    "where " +
                    "DocWayBill.iddoc = :iddoc "
        textQuery = ss.querySetParam(textQuery, "iddoc", wayBill["ID"].toString())
        var dataTable = ss.executeWithRead(textQuery)
        if (dataTable!![1][0].toInt() == 0 )
        {
            //погрузка не разрешена
            FExcStr.text = "Погрузка запрещена!"
            return
        }

        textQuery =
            "SELECT " +
                    "Main.DocFull as DocFull " +
                    "FROM (" + giveSomeOneQueryText() +
                    ") as Main " +
                    "INNER JOIN (" +
                    "SELECT " +
                    "Boxes.\$Спр.МестаПогрузки.Док as DocID " +
                    "FROM \$Спр.МестаПогрузки as Boxes (nolock) " +
                    "WHERE Boxes.ismark = 0 and Boxes.\$Спр.МестаПогрузки.Дата6 = :EmptyDate " +
                    "GROUP BY Boxes.\$Спр.МестаПогрузки.Док " +
                    ") as Boxes " +
                    "ON Boxes.DocID = Main.DocFull " +
                    ""
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
        textQuery = ss.querySetParam(textQuery, "iddoc", wayBill["ID"].toString())

        dataTable = ss.executeWithRead(textQuery)
        if (dataTable == null){
            return
        }
        if (dataTable.isNotEmpty())
        {
            FExcStr.text = "Не все погружено"
            return
        }

        textQuery =
            "UPDATE DH\$ПутевойЛист " +
                    "SET " +
                    "\$ПутевойЛист.Дата2 = :NowDate, " +
                    "\$ПутевойЛист.Время2 = :NowTime " +
                    "WHERE " +
                    "DH\$ПутевойЛист .iddoc = :iddoc"

        textQuery = ss.querySetParam(textQuery, "iddoc", wayBill["ID"].toString())
        if (!ss.executeWithoutRead(textQuery))
        {
            FExcStr.text = "Ошибка фиксации погрузки"
            badVoise()
            return
        }
        val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
        shoiseWorkInit.putExtra("ParentForm", "Loading")
        startActivity(shoiseWorkInit)
        finish()
    }

    private fun giveSomeOneQueryText():String {
        var idSchet = "   " + trans.decTo36(ss.getSynh("Счет"))
        idSchet = idSchet.substring(idSchet.length - 4)
        var idClaim = "   " + trans.decTo36(ss.getSynh("ПретензияОтКлиента"))
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

    private fun toModeLoadingInicialization() {
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
                "ORDER BY journ.date_time_iddoc"
        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        val dt = ss.executeWithReadNew(textQuery) ?: return

        if (dt.isNotEmpty())
        {
            //существует документ!
            toModeLoading(dt[0]["iddoc"].toString())
            return
        }
        refreshActivity()
        return
    }
    private fun toModeLoading(iddoc:String) {
        //Если wayBill еще не выбран, то испавим это недоразумение

        if (wayBill.isEmpty() || wayBill["ID"] != iddoc)
        {
            val dataMap = ss.getDoc(iddoc, true) ?: return
            wayBill = dataMap
            wayBill["View"] = dataMap["НомерДок"].toString() + " (" + dataMap["ДатаДок"].toString() + ")"
        }

        //проверим чек погрузки
        var textQuery =
            "select DocWayBill.\$ПутевойЛист.ЧекПогрузка as ЧекПогрузка " +
                    "from DH\$ПутевойЛист as DocWayBill (nolock) " +
                    "where " +
                    "DocWayBill.iddoc = :iddoc "
        textQuery = ss.querySetParam(textQuery, "iddoc", wayBill["ID"].toString())
        val dataTable = ss.executeWithRead(textQuery)
        if (dataTable!![1][0].toInt() == 0 )
        {
            //погрузка не разрешена
            FExcStr.text = "Погрузка запрещена!"
            badVoise()
            return
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
                    "FROM ( " + giveSomeOneQueryText() +
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
                    ""

        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
        textQuery = ss.querySetParam(textQuery, "iddoc", wayBill["ID"].toString())
        wayBillDT = ss.executeWithReadNew(textQuery)!!
        currentLine = 1
        if (wayBillDT.isNotEmpty()) {
            //не кончились места
            currentLineWayBillDT["ProposalNumber"] = wayBillDT[0]["ProposalNumber"].toString()
            currentLineWayBillDT["Doc"] = wayBillDT[0]["Doc"].toString()
            currentLineWayBillDT["AdressCounter"] = wayBillDT[0]["AdressCounter"].toString()
            currentLineWayBillDT["AdressCompl"] = wayBillDT[0]["AdressCompl"].toString()
        }

        curentAction = Action.Loading
        refreshActivity()
        return
    }

    private fun reactionBarcode(Barcode: String): Boolean {

        val helper = Helper()
        val barcoderes = helper.disassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if(typeBarcode == "6" && curentAction == Action.Loading)
        {
            val id = barcoderes["ID"].toString()
            if (ss.isSC(id, "МестаПогрузки")) {
                //проверим что это то что нам нужно
                //проверим чек погрузки
                var textQuery =
                "select DocWayBill.\$ПутевойЛист.ЧекПогрузка " +
                        "from DH\$ПутевойЛист as DocWayBill (nolock) " +
                        "where " +
                        "DocWayBill.iddoc = :iddoc "
                textQuery = ss.querySetParam(textQuery, "iddoc", wayBill["ID"].toString())
                val dataTable = ss.executeWithRead(textQuery)
                if (dataTable!![1][0].toInt() == 0 )
                {
                    //погрузка не разрешена
                    FExcStr.text = "Погрузка запрещена!"
                    badVoise()
                    return false
                }
                textQuery =
                    "Select " +
                            "\$Спр.МестаПогрузки.Дата6 as Date, " +
                            "right(\$Спр.МестаПогрузки.Док , 9) as Doc " +
                            "from \$Спр.МестаПогрузки (nolock) where id = :id"
                textQuery = ss.querySetParam(textQuery, "id", id)
                val dt = ss.executeWithReadNew(textQuery)
                if (dt == null )
                {
                    //погрузка не разрешена
                    badVoise()
                    return false
                }

                if (dt.isEmpty())
                {

                    FExcStr.text = "Нет действий с данным штрихкодом в этом режиме!"
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
                    badVoise()
                    return false
                }
                if (!ss.isVoidDate(dt[0]["Date"].toString()))
                {
                    FExcStr.text = "Ошибка! Место уже погружено!"
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
                    badVoise()
                    return false
                }

                //теперь проверяем та ли это строка
                var indexWayBill = -1
                for (DR in wayBillDT)
                {
                    if (DR["Doc"].toString() == dt[0]["Doc"].toString())
                    {
                        //это наш документ, запомним индекс данной строки
                        indexWayBill = DR["AdressCounter"].toString().toInt()
                        break
                    }
                }

                if (indexWayBill == -1)
                {
                    //не нашли в путевом
                    FExcStr.text = "Не числится в данном путевом!"
                    //блокируем путевой
                    textQuery =
                        "UPDATE DH\$ПутевойЛист " +
                                "SET " +
                                "\$ПутевойЛист.ЧекПогрузка = 0 " +
                                "WHERE " +
                                "DH\$ПутевойЛист .iddoc = :id"

                    textQuery = ss.querySetParam(textQuery, "id", wayBill["ID"].toString())
                    ss.executeWithoutRead(textQuery)
                    badVoise()
                    return false
                }

                if (ss.Const.OrderControl) {
                    val currCounter = wayBillDT[0]["AdressCounter"].toString().toInt()
                    if (currCounter > indexWayBill) {
                        FExcStr.text = "Нарушена последовательность погрузки!"
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
                        badVoise()
                        return false
                    }

                }

                textQuery =
                    "UPDATE \$Спр.МестаПогрузки " +
                            "SET " +
                            "\$Спр.МестаПогрузки.Дата6 = :NowDate, " +
                            "\$Спр.МестаПогрузки.Время6 = :NowTime " +
                            "WHERE " +
                            "\$Спр.МестаПогрузки .id = :id"

                textQuery = ss.querySetParam(textQuery, "id", id)

                if (!ss.executeWithoutRead(textQuery))
                {
                    FExcStr.text = "Ошибка фиксации погрузки"
                    badVoise()
                    return false
                }

                FExcStr.text = "Погрузка МЕСТА зафиксирована"
                toModeLoading(wayBill["ID"].toString())
            }
            else {
                FExcStr.text = "Неверно! Отсканируйте коробку."
                badVoise()
                return false
            }
        }
        else if (curentAction == Action.Inicialization && typeBarcode == "113") {
            val idd = barcoderes["IDD"].toString()
            if (ss.isSC(idd, "Сотрудники")) {
                placer.foundIDD(idd)
            } else {
                val doc = ss.getDoc(idd, false)
                if (doc == null) {
                    FExcStr.text = "Нет действий с данным ШК!"
                    badVoise()
                    return false
                }
                if (doc["Тип"] == "ПутевойЛист") {
                    wayBill = doc
                    wayBill["View"] = doc["НомерДок"].toString() + " (" + doc["ДатаДок"].toString() + ")"
                } else {
                    FExcStr.text = "Неверный тип документа!"
                    badVoise()
                    return false
                }
            }
        }
        else {
            FExcStr.text = "Нет действий с данным ШК! Отсканируйте коробку."
            badVoise()
            return false
        }
        goodVoise()
        refreshActivity()
        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?):Boolean {

        // нажали назад, выйдем
        if (keyCode == 4){
            val shoiseWorkInit = Intent(this, ChoiseWorkShipping::class.java)
            shoiseWorkInit.putExtra("ParentForm", "Loading")
            startActivity(shoiseWorkInit)
            finish()
            return true
        }
        when {
            ss.helper.whatDirection(keyCode) == "Right" -> {
                //переход в просмотр состояния
                val showInfo = Intent(this, ShowInfo::class.java)
                showInfo.putExtra("ParentForm", "Loading")
                showInfo.putExtra("Number",currentLineWayBillDT["ProposalNumber"].toString())
                showInfo.putExtra("Doc",currentLineWayBillDT["Doc"].toString())
                startActivity(showInfo)
                finish()
                return true
            }
            ss.helper.whatDirection(keyCode) == "Left" -> {
                //переход в просмотр состояния
                val showInfo = Intent(this, ShowBox::class.java)
                showInfo.putExtra("AdressCompl",currentLineWayBillDT["AdressCompl"].toString())
                showInfo.putExtra("Doc",currentLineWayBillDT["Doc"].toString())
                startActivity(showInfo)
                finish()
                return true
            }
            ss.helper.whatDirection(keyCode) in listOf("Down","Up") -> {
                table.getChildAt(currentLine).isFocusable = false
                table.getChildAt(currentLine).setBackgroundColor(Color.WHITE)
                if (ss.helper.whatDirection(keyCode) == "Down")
                {
                    if (currentLine < wayBillDT.count()) currentLine++ else currentLine = 1
                }
                else {
                    if (currentLine > 1) currentLine-- else currentLine = wayBillDT.count()
                }
                currentLineWayBillDT["ProposalNumber"] = wayBillDT[currentLine-1]["ProposalNumber"].toString()
                currentLineWayBillDT["Doc"] = wayBillDT[currentLine-1]["Doc"].toString()
                currentLineWayBillDT["AdressCounter"] = wayBillDT[currentLine-1]["AdressCounter"].toString()
                currentLineWayBillDT["AdressCompl"] = wayBillDT[currentLine-1]["AdressCompl"].toString()
                //теперь подкрасим строку серым
                table.getChildAt(currentLine).setBackgroundColor(Color.GRAY)
                table.getChildAt(currentLine).isFocusable = true
                return false
            }
            //Если Нажали DEL
            keyCode == 67 -> {
                //еслинажали финиш, значит переходим в режим погрузки
                if (curentAction == Action.Inicialization) {
                    completeLoadingInicialization()
                }
                else
                {
                    completeLodading()
                }
                return true
            }
            else -> return false
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    fun refreshActivity()    {
        //пока не отсканировали путевой обновлять нечего
        if (wayBill.isEmpty())
        {
            lblPlacer.text = "Путевой: <не выбран>"
            FExcStr.text = "Отсканируйте путевой лист"
            return
        }
        //путевой есть, надо подтянуть его в название
        lblPlacer.text = "Путевой: " + wayBill["НомерДок"] + " (" + wayBill["ДатаДок"] + ") Укладчик: "
        //теперь укладчик
        if (!placer.selected)
        {
            lblPlacer.text = lblPlacer.text.toString() + "<не выбран>"
        }
        else
        {
            lblPlacer.text = lblPlacer.text.toString() + ss.helper.getShortFIO(placer.name)
        }
        if (curentAction == Action.Inicialization)
        {
            //это инициализация дальше ничего не надо делать пока
            lblPlacer.visibility = View.VISIBLE
            return
        }

        table.removeAllViewsInLayout()
        var linearLayout = LinearLayout(this)
        val rowTitle = TableRow(this)
        //добавим столбцы
        var number = TextView(this)
        number.text = "№"
        number.typeface = Typeface.SERIF
        number.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.09).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        number.gravity = Gravity.CENTER
        number.textSize = 20F
        number.setTextColor(-0x1000000)
        var docum = TextView(this)
        docum.text = "Документ"
        docum.typeface = Typeface.SERIF
        docum.gravity = Gravity.CENTER
        docum.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.32).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        docum.textSize = 20F
        docum.setTextColor(-0x1000000)
        var address = TextView(this)
        address.text = "Адрес"
        address.typeface = Typeface.SERIF
        address.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.33).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        address.gravity = Gravity.CENTER
        address.textSize = 20F
        address.setTextColor(-0x1000000)
        var boxes = TextView(this)
        boxes.text = "Мест"
        boxes.typeface = Typeface.SERIF
        boxes.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.13).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        boxes.gravity = Gravity.CENTER
        boxes.textSize = 20F
        boxes.setTextColor(-0x1000000)
        var boxesfact = TextView(this)
        boxesfact.text = "Факт"
        boxesfact.typeface = Typeface.SERIF
        boxesfact.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.13).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
        boxesfact.gravity = Gravity.CENTER
        boxesfact.textSize = 20F
        boxesfact.setTextColor(-0x1000000)

        linearLayout.addView(number)
        linearLayout.addView(docum)
        linearLayout.addView(address)
        linearLayout.addView(boxes)
        linearLayout.addView(boxesfact)

        rowTitle.addView(linearLayout)
        table.addView(rowTitle)
        var linenom = 1 //1 строку шапки не считаем
        var countBoxAll = 0
        for (rowDT in wayBillDT)
        {
            //строки теперь
            val rowTitle = TableRow(this)
            rowTitle.isClickable = true
            rowTitle.setOnTouchListener { v, event ->

                if (event.action == MotionEvent.ACTION_DOWN) {
                    var i = 0
                    while (i < table.childCount) {
                        if (rowTitle != table.getChildAt(i)) {
                            table.getChildAt(i).setBackgroundColor(Color.WHITE)
                        } else {
                            currentLine = i
                            rowTitle.setBackgroundColor(Color.GRAY)
                            for (rowCDT in wayBillDT) {
                                if (((rowTitle.getChildAt(0) as ViewGroup).getChildAt(1) as TextView).text.toString() == rowCDT["ProposalNumber"].toString()
                                        .substring(4)
                                ) {
                                    currentLineWayBillDT["ProposalNumber"] =
                                        rowCDT["ProposalNumber"].toString()
                                    currentLineWayBillDT["Doc"] = rowCDT["Doc"].toString()
                                    currentLineWayBillDT["AdressCounter"] =
                                        rowCDT["AdressCounter"].toString()
                                    currentLineWayBillDT["AdressCompl"] =
                                        rowCDT["AdressCompl"].toString()
                                }
                            }
                        }
                        i++
                    }

                }
                true
            }
            linearLayout = LinearLayout(this)
            var colorline =  Color.WHITE
            if (linenom == currentLine)
            {
                colorline = Color.GRAY
            }
            rowTitle.setBackgroundColor(colorline)
            //добавим столбцы
            number = TextView(this)
            number.text = rowDT["AdressCounter"]
            number.typeface = Typeface.SERIF
            number.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.09).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            number.gravity = Gravity.CENTER
            number.textSize = 20F
            number.setTextColor(-0x1000000)
            docum = TextView(this)
            docum.text = rowDT["ProposalNumber"].toString().substring(4)
            docum.typeface = Typeface.SERIF
            docum.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.32).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            docum.gravity = Gravity.CENTER
            docum.textSize = 20F
            docum.setTextColor(-0x1000000)
            address = TextView(this)
            address.text = rowDT["AdressCompl"]?.trim()
            address.typeface = Typeface.SERIF
            address.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.33).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            address.gravity = Gravity.CENTER
            address.textSize = 17F
            address.setTextColor(-0x1000000)
            boxes = TextView(this)
            boxes.text = rowDT["Boxes"]
            boxes.typeface = Typeface.SERIF
            boxes.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.14).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            boxes.gravity = Gravity.CENTER
            boxes.textSize = 20F
            boxes.setTextColor(-0x1000000)
            boxesfact = TextView(this)
            boxesfact.text = rowDT["BoxesFact"]
            boxesfact.typeface = Typeface.SERIF
            boxesfact.layoutParams = LinearLayout.LayoutParams((ss.widthDisplay*0.14).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
            boxesfact.gravity = Gravity.CENTER
            boxesfact.textSize = 20F
            boxesfact.setTextColor(-0x1000000)
            countBoxAll += rowDT["Boxes"].toString().toInt()
            linearLayout.addView(number)
            linearLayout.addView(docum)
            linearLayout.addView(address)
            linearLayout.addView(boxes)
            linearLayout.addView(boxesfact)

            rowTitle.addView(linearLayout)
            table.addView(rowTitle)
            linenom++
        }
        lblPlacer.visibility = View.VISIBLE
        lblPlacer.text = wayBill["НомерДок"] + " (" + wayBill["ДатаДок"] + ") ВСЕГО МЕСТ " + countBoxAll.toString()

    }

}
