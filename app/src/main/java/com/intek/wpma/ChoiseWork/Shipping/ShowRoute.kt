package com.intek.wpma.ChoiseWork.Shipping

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.LinearLayout
import android.widget.TableRow
import android.widget.TextView
import android.widget.Toast
import com.intek.wpma.*
import com.intek.wpma.Ref.RefEmployer
import com.intek.wpma.Ref.RefSection
import kotlinx.android.synthetic.main.activity_new_complectation.*
import kotlinx.android.synthetic.main.activity_show_route.*
import kotlinx.android.synthetic.main.activity_show_route.FExcStr
import kotlinx.android.synthetic.main.activity_show_route.btnScan

class ShowRoute: BarcodeDataReceiver() {

    private var ccrp: MutableList<MutableMap<String, String>> = mutableListOf()
    private var ccrpOld: MutableList<MutableMap<String, String>> = mutableListOf()
    private var docDown: MutableMap<String, String> = mutableMapOf()
    private var oldMode = ss.CurrentMode
    private var scaningBox = ""
    private var scaningBoxIddoc = ""
    private var needAdressComplete = ss.getVoidID()

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
        Log.d("IntentApiSample: ", "onResume")
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
        scanRes = null
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_route)
        title = ss.title
        oldMode = ss.CurrentMode
        docDown["ID"] = intent.extras?.getString("docDownID").toString()
        docDown["Sector"] = intent.extras?.getString("docDownSector").toString()

        //ss.CurrentMode = Global.Mode.ShowRoute
        if (ss.isMobile) {
            btnScan.visibility = View.VISIBLE
            btnScan!!.setOnClickListener {
                ss.CurrentMode = Global.Mode.ShowRoute
                val scanAct = Intent(this@ShowRoute, ScanActivity::class.java)
                scanAct.putExtra("ParentForm", "ShowRoute")
                startActivity(scanAct)
            }
        }
        var oldx = 0F
        FExcStr.setOnTouchListener(fun(v: View, event: MotionEvent): Boolean {
            if (event.action == MotionEvent.ACTION_DOWN) {
                oldx = event.x
                return true
            } else if (event.action == MotionEvent.ACTION_MOVE) {
                if (event.x < oldx) {
                    ss.CurrentMode = Global.Mode.ShowRoute
                    val shoiseWorkInit = Intent(this, NewComplectation::class.java)
                    shoiseWorkInit.putExtra("oldMode",oldMode.toString())
                    shoiseWorkInit.putExtra("scaningBox", scaningBox)

                    startActivity(shoiseWorkInit)
                    finish()
                }
            }
            return true
        })
        refreshRoute()
    }

    private fun refreshRoute() {
        var textQuery =
            "select " +
                    "right(min(journForBill.docno), 5) as Bill, " +
                    "rtrim(min(isnull(Sections.descr, 'Пу'))) + '-' + cast(min(DocCC.\$КонтрольНабора.НомерЛиста ) as char) as CC, " +
                    "max(AllTab.CountAllBox) as Boxes, " +
                    "rtrim(max(RefAdress9.descr)) as Adress, " +
                    "max(Gate.descr) as Gate " +
                    "from \$Спр.МестаПогрузки as Ref (nolock) " +
                    "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                    "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                    "left join \$Спр.Секции as Sections (nolock) " +
                    "on Sections.id = DocCC.\$КонтрольНабора.Сектор " +
                    "inner join DH\$КонтрольРасходной as DocCB (nolock) " +
                    "on DocCB .iddoc = DocCC.\$КонтрольНабора.ДокументОснование " +
                    "inner JOIN DH\$Счет as Bill (nolock) " +
                    "on Bill.iddoc = DocCB.\$КонтрольРасходной.ДокументОснование " +
                    "inner join _1sjourn as journForBill (nolock) " +
                    "on journForBill.iddoc = Bill.iddoc " +
                    "left join \$Спр.Секции as RefAdress9 (nolock) " +
                    "on RefAdress9.id = dbo.WMP_fn_GetAdressComplete(Ref.id) " +
                    "left join \$Спр.Ворота as Gate (nolock) " +
                    "on Gate.id = DocCB.\$КонтрольРасходной.Ворота " +
                    "inner join ( " +
                    "select " +
                    "DocCC.iddoc as iddoc, " +
                    "count(*) as CountAllBox " +
                    "from \$Спр.МестаПогрузки as Ref (nolock) " +
                    "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                    "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                    "where " +
                    "Ref.ismark = 0 " +
                    "and Ref.\$Спр.МестаПогрузки.Сотрудник8 = :Employer " +
                    "and Ref.\$Спр.МестаПогрузки.Дата9 = :EmptyDate " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата8 = :EmptyDate " +
                    "group by DocCC.iddoc ) as AllTab " +
                    "on AllTab.iddoc = DocCC.iddoc " +
                    "where " +
                    "Ref.ismark = 0 " +
                    "and Ref.\$Спр.МестаПогрузки.Сотрудник8 = :Employer " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата8 = :EmptyDate " +
                    "and Ref.\$Спр.МестаПогрузки.Дата9 = :EmptyDate " +
                    "group by DocCC.iddoc"

        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        ccrp = ss.executeWithReadNew(textQuery) ?: return

        textQuery =
            "select " +
                    "right(min(journForBill.docno), 5) as Bill, " +
                    "rtrim(min(isnull(Sections.descr, 'Пу'))) + '-' + cast(min(DocCC.\$КонтрольНабора.НомерЛиста ) as char) as CC, " +
                    "max(AllTab.CountAllBox) as Boxes, " +
                    "rtrim(max(RefAdress9.descr)) as Adress, " +
                    "max(Gate.descr) as Gate " +
                    "from \$Спр.МестаПогрузки as Ref (nolock) " +
                    "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                    "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                    "left join \$Спр.Секции as Sections (nolock) " +
                    "on Sections.id = DocCC.\$КонтрольНабора.Сектор " +
                    "inner join DH\$КонтрольРасходной as DocCB (nolock) " +
                    "on DocCB .iddoc = DocCC.\$КонтрольНабора.ДокументОснование " +
                    "inner JOIN DH\$Счет as Bill (nolock) " +
                    "on Bill.iddoc = DocCB.\$КонтрольРасходной.ДокументОснование " +
                    "inner join _1sjourn as journForBill (nolock) " +
                    "on journForBill.iddoc = Bill.iddoc " +
                    "left join \$Спр.Секции as RefAdress9 (nolock) " +
                    "on RefAdress9.id = dbo.WMP_fn_GetAdressComplete(Ref.id) " +
                    "left join \$Спр.Ворота as Gate (nolock) " +
                    "on Gate.id = DocCB.\$КонтрольРасходной.Ворота " +
                    "inner join ( " +
                    "select " +
                    "Ref.\$Спр.МестаПогрузки.НомерЗадания7 as NumberOfOrder " +
                    "from \$Спр.МестаПогрузки as Ref (nolock) " +
                    "where " +
                    "Ref.ismark = 0 " +
                    "and Ref.\$Спр.МестаПогрузки.Сотрудник8 = :Employer " +
                    "and Ref.\$Спр.МестаПогрузки.Дата9 = :EmptyDate " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата8 = :EmptyDate " +
                    "and not Ref.\$Спр.МестаПогрузки.НомерЗадания7 = 0 " +
                    "group by Ref.\$Спр.МестаПогрузки.НомерЗадания7 ) as NumberOfOrderBox " +
                    "on NumberOfOrderBox.NumberOfOrder = Ref.\$Спр.МестаПогрузки.НомерЗадания7 " +
                    "inner join ( " +
                    "select " +
                    "DocCC.iddoc as iddoc, " +
                    "count(*) as CountAllBox " +
                    "from \$Спр.МестаПогрузки as Ref (nolock) " +
                    "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                    "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                    "where " +
                    "Ref.ismark = 0 " +
                    "and Ref.\$Спр.МестаПогрузки.Сотрудник8 = :Employer " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата9 = :EmptyDate " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата8 = :EmptyDate " +
                    "group by DocCC.iddoc ) as AllTab " +
                    "on AllTab.iddoc = DocCC.iddoc " +
                    "where " +
                    "Ref.ismark = 0 " +
                    "and Ref.\$Спр.МестаПогрузки.Сотрудник8 = :Employer " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата8 = :EmptyDate " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата9 = :EmptyDate " +
                    "group by DocCC.iddoc"

        textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
        textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
        ccrpOld = ss.executeWithReadNew(textQuery) ?: return
        refreshActivity()
    }

    private fun reactionBarcode(Barcode: String): Boolean {
        val barcoderes = ss.helper.disassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if (typeBarcode == "6") {
            val id = barcoderes["ID"].toString()
            if (ss.isSC(id, "МестаПогрузки")) {
                if (oldMode == Global.Mode.NewComplectation) {

                    var textQuery =
                        "Select " +
                                "\$Спр.МестаПогрузки.Дата8 as Date, " +
                                "\$Спр.МестаПогрузки.КонтрольНабора as Doc, " +
                                "\$Спр.МестаПогрузки.Сотрудник8 as Employer " +
                                "from \$Спр.МестаПогрузки (nolock) where id = :id"
                    textQuery = ss.querySetParam(textQuery, "id", id)
                    val dt = ss.executeWithReadNew(textQuery) ?: return false
                    if (dt.isEmpty()) {
                        FExcStr.text = "Нет действий с данным штрихкодом!"
                        badVoise()
                        return false
                    }
                    if (dt[0]["Doc"].toString() != docDown["ID"]) {
                        FExcStr.text = "Место от другого сборочного!"
                        badVoise()
                        return false
                    }
                    if (!ss.isVoidDate(dt[0]["Date"].toString())) {
                        FExcStr.text = "Место уже отобрано!"
                        badVoise()
                        return false
                    }
                    if (dt[0]["Employer"].toString() != ss.FEmployer.id) {
                        FExcStr.text = "Этого места нет в задании!"
                        badVoise()
                        return false
                    }

                    //начинается!
                    textQuery =
                        "begin tran; " +
                                "UPDATE \$Спр.МестаПогрузки " +
                                "SET " +
                                "\$Спр.МестаПогрузки.Дата8 = :NowDate , " +
                                "\$Спр.МестаПогрузки.Время8 = :NowTime " +
                                //"$Спр.МестаПогрузки.Адрес9 = dbo.WMP_fn_GetAdressComplete(id) " +
                                "WHERE " +
                                "id = :itemid; " +
                                "if @@rowcount = 0 rollback tran " +
                                "else begin " +
                                "if exists ( " +
                                "select top 1 Ref.id from \$Спр.МестаПогрузки as Ref " +
                                "inner join \$Спр.Секции as SectionsCollect (nolock) " +
                                "ON SectionsCollect.id = Ref.\$Спр.МестаПогрузки.Адрес7 " +
                                "inner join \$Спр.Секции as RefSectionsParent (nolock) " +
                                "on left(SectionsCollect.descr, 2) = RefSectionsParent.descr " +
                                "where " +
                                "Ref.ismark = 0 " +
                                "and RefSectionsParent.descr = :NameParent " +
                                "and Ref.\$Спр.МестаПогрузки.КонтрольНабора = :iddoc " +
                                "and Ref.\$Спр.МестаПогрузки.Дата8 = :EmptyDate ) " +
                                "commit tran " +
                                "else begin " +
                                "declare @res int; " +
                                "exec WPM_GetOrderComplectationNew :Employer, :NameParent, 0, @res OUTPUT; " +
                                "if @res = 0 rollback tran else commit tran " +
                                "end " +
                                "end "

                    textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
                    textQuery = ss.querySetParam(textQuery, "itemid", id)
                    textQuery = ss.querySetParam(textQuery, "iddoc", docDown["ID"].toString())
                    textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())
                    textQuery = ss.querySetParam(
                        textQuery,
                        "NameParent",
                        docDown["Sector"].toString().trim()
                    )

                    if (!ss.executeWithoutRead(textQuery)) {
                        badVoise()
                        return false
                    }
                    return true

                }
                else {
                    var textQuery =
                        "Select " +
                                "isnull(Sections.descr, 'Пу') as Sector, " +
                                "DocCC.\$КонтрольНабора.НомерЛиста as Number, " +
                                "CONVERT(char(8), CAST(LEFT(journForBill.date_time_iddoc, 8) as datetime), 4) as datedoc, " +
                                "journForBill.docno as docno, " +
                                "Ref.\$Спр.МестаПогрузки.Дата9 as Date, " +
                                "Ref.\$Спр.МестаПогрузки.КонтрольНабора as Doc, " +
                                "Ref.\$Спр.МестаПогрузки.Сотрудник8 as Employer, " +
                                "isnull(dbo.WMP_fn_GetAdressComplete(Ref.id), :EmptyID ) as Adress9, " +
                                "isnull(Adress.Descr, 'нет адреса') as Adress, " +
                                "AllTab.CountAllBox as CountBox, " +
                                "Gate.descr as Gate " +
                                "from \$Спр.МестаПогрузки as Ref (nolock) " +
                                "left join \$Спр.Секции as Adress (nolock) " +
                                "on Adress.id = dbo.WMP_fn_GetAdressComplete(Ref.id) " +
                                "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                                "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                                "left join \$Спр.Секции as Sections (nolock) " +
                                "on Sections.id = DocCC.\$КонтрольНабора.Сектор " +
                                "inner join DH\$КонтрольРасходной as DocCB (nolock) " +
                                "on DocCB.iddoc = DocCC.\$КонтрольНабора.ДокументОснование " +
                                "inner JOIN DH\$Счет as Bill (nolock) " +
                                "on Bill.iddoc = DocCB.\$КонтрольРасходной.ДокументОснование " +
                                "INNER JOIN _1sjourn as journForBill (nolock) " +
                                "on journForBill.iddoc = Bill.iddoc " +
                                "left join \$Спр.Ворота as Gate (nolock) " +
                                "on Gate.id = DocCB.\$КонтрольРасходной.Ворота " +
                                "left join ( " +
                                "select " +
                                "DocCC.iddoc as iddoc, " +
                                "count(*) as CountAllBox " +
                                "from \$Спр.МестаПогрузки as Ref (nolock) " +
                                "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                                "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                                "where " +
                                "Ref.ismark = 0 " +
                                "and Ref.\$Спр.МестаПогрузки.Сотрудник8 = :Employer " +
                                "and Ref.\$Спр.МестаПогрузки.Дата9 = :EmptyDate " +
                                "and not Ref.\$Спр.МестаПогрузки.Дата8 = :EmptyDate " +
                                "group by DocCC.iddoc ) as AllTab " +
                                "on AllTab.iddoc = DocCC.iddoc " +
                                "where Ref.id = :id"
                    textQuery = ss.querySetParam(textQuery, "id", id)
                    textQuery = ss.querySetParam(textQuery, "EmptyID", ss.getVoidID())
                    textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
                    textQuery = ss.querySetParam(textQuery, "EmptyDate", ss.getVoidDate())

                    val dt = ss.executeWithReadNew(textQuery) ?: return false
                    if (dt.isEmpty()) {
                        FExcStr.text = "Нет действий с данным штрихкодом!"
                        badVoise()
                        return false
                    }
                    if (!ss.isVoidDate(dt[0]["Date"].toString())) {
                        FExcStr.text = "Место уже скомплектовано!"
                        badVoise()
                        return false
                    }
                    if (dt[0]["Employer"].toString() != ss.FEmployer.id) {
                        FExcStr.text = "Этого места нет в задании!"
                        badVoise()
                        return false
                    }

                    docDown["ID"] = dt[0]["Doc"].toString()
                    docDown["Boxes"] = dt[0]["CountBox"].toString()
                    docDown["View"] = dt[0]["Sector"].toString()
                        .trim() + "-" + dt[0]["Number"].toString() + " Заявка " + dt[0]["docno"].toString() + " (" + dt[0]["datedoc"].toString() + ")"
                    docDown["AdressCollect"] = dt[0]["Adress"].toString()
                    docDown["Sector"] = dt[0]["Gate"].toString().trim()
                    docDown["NumberBill"] = dt[0]["docno"].toString().trim()
                    docDown["NumberCC"] = dt[0]["Number"].toString()
                    docDown["MainSectorName"] = dt[0]["Sector"].toString()

                    scaningBox = id
                    scaningBoxIddoc = dt[0]["Doc"].toString()
                    needAdressComplete = dt[0]["Adress9"].toString()
                    FExcStr.text = "Отсканируйте адрес!"
                    refreshActivity()
                    return true

                }
            }
            else {
                FExcStr.text = "Нет действий с данным ШК в данном режиме!"
                badVoise()
                return false
            }

        }
        else if (typeBarcode == "113") {
            //справочники типовые
            val idd = barcoderes["IDD"].toString()
            if (ss.isSC(idd, "Сотрудники")) {
                ss.FEmployer = RefEmployer()
                val mainInit = Intent(this, MainActivity::class.java)
                mainInit.putExtra("ParentForm", "NewComplectation")
                startActivity(mainInit)
                finish()
            }
            else if (oldMode == Global.Mode.NewComplectationComplete && ss.isSC(idd, "Секции")) {
                if (scaningBox == "") {
                    FExcStr.text = "Отсканируйте место!"
                    badVoise()
                    return false
                }
                val section = RefSection()
                if (!section.foundIDD(idd)) {
                    FExcStr.text = "Не найден адрес!"
                    badVoise()
                    return false
                }
                val id = section.id
                if (needAdressComplete != ss.getVoidID()) {
                    if (id != needAdressComplete) {
                        FExcStr.text = "Неверный адрес!"
                        badVoise()
                        return false
                    }
                }
                else {
                    //нужно проверить зону ворот, к тем ли воротам он относится
                    var textQuery =
                        "Select " +
                                "Zone.\$Спр.ЗоныВорот.Секция as Section, " +
                                "Gate.descr as Gate " +
                                "from \$Спр.МестаПогрузки as Ref (nolock) " +
                                "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                                "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                                "inner join DH\$КонтрольРасходной as DocCB (nolock) " +
                                "on DocCB.iddoc = DocCC.\$КонтрольНабора.ДокументОснование " +
                                "inner join \$Спр.Ворота as Gate (nolock) " +
                                "on Gate.id = DocCB.\$КонтрольРасходной.Ворота " +
                                "inner join \$Спр.ЗоныВорот as Zone (nolock) " +
                                "on Gate.id = Zone.parentext " +
                                "where Ref.id = :id"
                    textQuery = ss.querySetParam(textQuery, "id", scaningBox)
                    val dt = ss.executeWithReadNew(textQuery) ?: return false
                    if (dt.isNotEmpty()) {
                        //зона задана, надо проверять адреса
                        var findAdres = false
                        for (dr in dt) {
                            if (id == dr["Section"].toString()) {
                                findAdres = true
                                break
                            }
                        }

                        if (!findAdres) {
                            //нет такого адреса в зоне
                            FExcStr.text = "Нужен адрес из зоны " + dt[0]["Gate"].toString().trim()
                            badVoise()
                            return false
                        }
                    }
                }
                var textQuery =
                    "declare @res int; " +
                            "begin tran; " +
                            "exec WPM_CompleteBox :box, :adress, @res output; " +
                            "if @res = 0 rollback tran else commit tran; " +
                            "select @res as result; "
                textQuery = ss.querySetParam(textQuery, "box", scaningBox)
                textQuery = ss.querySetParam(textQuery, "adress", id)
                val dt = ss.executeWithReadNew(textQuery) ?: return false
                if (dt.isEmpty()) return false
                if (dt[0]["result"] == "0") {
                    FExcStr.text = "Не удалось зафиксировать комплектацию места!"
                    badVoise()
                    return false
                }
                NewComplectation().checkFullNewComplete(scaningBox)
                ss.CurrentMode = Global.Mode.ShowRoute
                val newcomp = Intent(this, NewComplectation::class.java)
                newcomp.putExtra("lastGoodAdress", id)
                newcomp.putExtra("oldMode",oldMode.toString())
                newcomp.putExtra("scaningBox", scaningBox)
                startActivity(newcomp)
                finish()
                return true
            }
        }
        else {
            FExcStr.text = "Нет действий с данным ШК в данном режиме!"
            badVoise()
            return false
        }
        return true
    }

    private fun reactionKey(keyCode: Int, event: KeyEvent?): Boolean {

        // нажали назад, выйдем
        if (keyCode == 4|| ss.helper.whatDirection(keyCode) == "Left") {
            FExcStr.text = "Секунду..."
            ss.CurrentMode = Global.Mode.ShowRoute
            val shoiseWorkInit = Intent(this, NewComplectation::class.java)
            shoiseWorkInit.putExtra("oldMode",oldMode.toString())
            shoiseWorkInit.putExtra("scaningBox", scaningBox)
            startActivity(shoiseWorkInit)
            finish()
            return true
        }
        return false

    }

    private fun refreshActivity() {

        table.removeAllViewsInLayout()

        var cvet = Color.rgb(192, 192, 192)
        Shapka.text =
            """Комплектация в ${if (oldMode == Global.Mode.NewComplectation) "тележку" else "адрес"} (новая)"""
        var row = TableRow(this)
        var linearLayout = LinearLayout(this)

        var gate = TextView(this)
        gate.text = "Вр"
        gate.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.1).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        gate.gravity = Gravity.CENTER_HORIZONTAL
        gate.textSize = 20F
        gate.setTextColor(Color.BLACK)

        var num = TextView(this)
        num.text = "Заявка"
        num.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.25).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        num.gravity = Gravity.CENTER_HORIZONTAL
        num.textSize = 20F
        num.setTextColor(Color.BLACK)

        var sector = TextView(this)
        sector.text = "Лист"
        sector.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.15).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        sector.gravity = Gravity.START
        sector.textSize = 20F
        sector.setTextColor(Color.BLACK)

        var count = TextView(this)
        count.text = "М"
        count.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.1).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        count.gravity = Gravity.START
        count.textSize = 20F
        count.setTextColor(Color.BLACK)

        var address = TextView(this)
        address.text = "Адрес"
        address.layoutParams = LinearLayout.LayoutParams(
            (ss.widthDisplay * 0.4).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
        address.gravity = Gravity.START
        address.textSize = 20F
        address.setTextColor(Color.BLACK)


        linearLayout.setPadding(3, 3, 3, 3)
        linearLayout.addView(gate)
        linearLayout.addView(num)
        linearLayout.addView(sector)
        linearLayout.addView(count)
        linearLayout.addView(address)

        row.setBackgroundColor(Color.LTGRAY)
        row.addView(linearLayout)
        table.addView(row)

        if (ccrp.isEmpty() and ccrpOld.isEmpty()) return

        for (dr in ccrp) {
            linearLayout = LinearLayout(this)
            row = TableRow(this)

            gate = TextView(this)
            gate.text = dr["Gate"].toString().trim()
            gate.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.1).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gate.gravity = Gravity.CENTER_HORIZONTAL
            gate.textSize = 20F
            gate.setTextColor(Color.BLACK)

            num = TextView(this)
            num.text = dr["Bill"]
            num.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.25).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            num.gravity = Gravity.CENTER_HORIZONTAL
            num.textSize = 20F
            num.setTextColor(Color.BLACK)

            sector = TextView(this)
            sector.text = dr["CC"]
            sector.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.15).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            sector.gravity = Gravity.START
            sector.textSize = 20F
            sector.setTextColor(Color.BLACK)

            count = TextView(this)
            count.text = dr["Boxes"]
            count.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.1).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            count.gravity = Gravity.START
            count.textSize = 20F
            count.setTextColor(Color.BLACK)

            address = TextView(this)
            address.text = dr["Adress"]
            address.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.4).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            address.gravity = Gravity.START
            address.textSize = 20F
            address.setTextColor(Color.BLACK)


            linearLayout.setPadding(3, 3, 3, 3)
            linearLayout.addView(gate)
            linearLayout.addView(num)
            linearLayout.addView(sector)
            linearLayout.addView(count)
            linearLayout.addView(address)

            row.addView(linearLayout)

            table.addView(row)

        }
        for (dr in ccrpOld) {
            linearLayout = LinearLayout(this)
            row = TableRow(this)

            gate = TextView(this)
            gate.text = dr["Gate"].toString().trim()
            gate.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.1).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            gate.gravity = Gravity.CENTER_HORIZONTAL
            gate.textSize = 20F
            gate.setTextColor(Color.LTGRAY)

            num = TextView(this)
            num.text = dr["Bill"]
            num.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.25).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            num.gravity = Gravity.CENTER_HORIZONTAL
            num.textSize = 20F
            num.setTextColor(Color.LTGRAY)

            sector = TextView(this)
            sector.text = dr["CC"]
            sector.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.15).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            sector.gravity = Gravity.START
            sector.textSize = 20F
            sector.setTextColor(Color.LTGRAY)

            count = TextView(this)
            count.text = dr["Boxes"]
            count.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.1).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            count.gravity = Gravity.START
            count.textSize = 20F
            count.setTextColor(Color.LTGRAY)

            address = TextView(this)
            address.text = dr["Adress"]
            address.layoutParams = LinearLayout.LayoutParams(
                (ss.widthDisplay * 0.4).toInt(),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            address.gravity = Gravity.START
            address.textSize = 20F
            address.setTextColor(Color.LTGRAY)


            linearLayout.setPadding(3, 3, 3, 3)
            linearLayout.addView(gate)
            linearLayout.addView(num)
            linearLayout.addView(sector)
            linearLayout.addView(count)
            linearLayout.addView(address)

            row.addView(linearLayout)

            table.addView(row)

        }

    }

}