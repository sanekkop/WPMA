package com.intek.wpma.ChoiseWork.Shipping

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import com.intek.wpma.BarcodeDataReceiver
import com.intek.wpma.Helpers.Helper
import com.intek.wpma.R
import com.intek.wpma.Ref.ARef
import com.intek.wpma.Ref.RefEmployer
import com.intek.wpma.Ref.RefSection
import com.intek.wpma.ScanActivity
import kotlinx.android.synthetic.main.activity_unloading.*


class UnLoading : BarcodeDataReceiver() {

    var CurrentAction: String = ""
    var Barcode: String = ""
    var codeId: String = ""             //показатель по которому можно различать типы штрих-кодов
    var AdressUnLoad:String = ""
    var BoxUnLoad:String = ""

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_unloading)

       terminalView.text = SS.terminal
        title = SS.helper.GetShortFIO(SS.FEmployer.Name)
        header.text  = "Свободная разргузка"
        CurrentAction = "ScanBox"

        if (SS.isMobile){
            btnScanUnloadingMode.visibility = View.VISIBLE
            btnScanUnloadingMode!!.setOnClickListener {
                val scanAct = Intent(this@UnLoading, ScanActivity::class.java)
                scanAct.putExtra("ParentForm","UnLoading")
                startActivity(scanAct)
            }
        }
    }

    companion object {
        var scanRes: String? = null
        var scanCodeId: String? = null
    }

    private fun reactionBarcode(Barcode: String): Boolean {
        val helper:Helper = Helper()
        val barcoderes = helper.DisassembleBarcode(Barcode)
        val typeBarcode = barcoderes["Type"].toString()
        if (typeBarcode == "113")
        {
            //это справочник типовой
            val idd = barcoderes["IDD"].toString()
            if (SS.IsSC(idd, "Секции")) {

                if ( CurrentAction != "ScanAdress")
                {
                    FExcStr.text = "Неверно! Отсканируйте адрес."
                    BadVoise()
                    return false
                }

                //получим данные адреса
                var adressScan:RefSection = RefSection()
                adressScan.FoundIDD(idd)
                AdressUnLoad = adressScan.ID
                var textQuery =
                "UPDATE \$Спр.МестаПогрузки " +
                        "SET " +
                        "\$Спр.МестаПогрузки.Адрес9 = :AdressID ," +
                        "\$Спр.МестаПогрузки.Сотрудник8 = :EmployerID ," +
                        "\$Спр.МестаПогрузки.Дата9 = :NowDate ," +
                        "\$Спр.МестаПогрузки.Время9 = :NowTime " +
                        "WHERE \$Спр.МестаПогрузки .id = :ID ";
                textQuery = SS.QuerySetParam(textQuery, "ID", BoxUnLoad);
                textQuery = SS.QuerySetParam(textQuery, "AdressID",      AdressUnLoad);
                textQuery = SS.QuerySetParam(textQuery, "EmployerID",    SS.FEmployer.ID);
                if (!SS.ExecuteWithoutRead(textQuery))
                {
                    FExcStr.text = "Не удалось зафиксировать! Отсканируйте адрес."
                    BadVoise()
                    return false
                }

                CurrentAction = "ScanBox"
             }
            else
            {
                FExcStr.text = "Неверно! " + if(CurrentAction == "ScanAdress") "Отсканируйте адрес." else "Отсканируйте коробку."
                BadVoise()
                return false
            }
        }
        else if(typeBarcode == "6")
        {
            val id = barcoderes["ID"].toString()
            if (SS.IsSC(id, "МестаПогрузки")) {
                if ( CurrentAction != "ScanBox")
                {
                    FExcStr.text = "Неверно! Отсканируйте адрес."
                    BadVoise()
                    return false
                }

                CurrentAction = "ScanAdress"
                AdressUnLoad = ""
                BoxUnLoad = id

            }
            else {
                FExcStr.text = "Неверно! Отсканируйте коробку."
                BadVoise()
                return false
            }
        }
        else {
            FExcStr.text = "Нет действий с данным ШК! " + if(CurrentAction == "ScanAdress") "Отсканируйте адрес." else "Отсканируйте коробку."
            BadVoise()
            return false
        }
        GoodVoise()
        RefreshActivity()
        return true
    }

    fun RefreshActivity()    {
        //пока не отсканировали место обновлять нечего
        if (BoxUnLoad == "")
        {
            lblInfo1.text = "";
            lblInfo2.text = "";
            return
        }
        var textQuery ="Select " +
                "isnull(Sections.descr, 'Пу') as Sector, " +
                "CONVERT(char(8), CAST(LEFT(journForBill.date_time_iddoc, 8) as datetime), 4) as DateDoc, " +
                "journForBill.docno as DocNo, " +
                "DocCC.\$КонтрольНабора.НомерЛиста as Number, " +
                "isnull(Adress.descr, 'Пу') as Adress, " +
                "TabBox.CountAllBox as CountAllBox, " +
                "Ref.\$Спр.МестаПогрузки.НомерМеста as NumberBox, " +
                "Gate.descr as Gate " +
                "from \$Спр.МестаПогрузки as Ref (nolock) " +
                "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                "left join \$Спр.Секции as Sections (nolock) " +
                "on Sections.id = DocCC.\$КонтрольНабора.Сектор " +
                "left join \$Спр.Секции as Adress (nolock) " +
                "on Adress.id = Ref.\$Спр.МестаПогрузки.Адрес9 " +
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
                "DocCB.iddoc as iddoc, " +
                "count(*) as CountAllBox " +
                "from \$Спр.МестаПогрузки as Ref (nolock) " +
                "inner join DH\$КонтрольНабора as DocCC (nolock) " +
                "on DocCC.iddoc = Ref.\$Спр.МестаПогрузки.КонтрольНабора " +
                "inner join DH\$КонтрольРасходной as DocCB (nolock) " +
                "on DocCB.iddoc = DocCC.\$КонтрольНабора.ДокументОснование " +
                "where " +
                "Ref.ismark = 0 " +
                "group by DocCB.iddoc ) as TabBox " +
                "on TabBox.iddoc = DocCB.iddoc " +
                "where Ref.id = :id"
        textQuery = SS.QuerySetParam(textQuery, "id", BoxUnLoad)
        val dataTable = SS.ExecuteWithRead(textQuery)
        if (dataTable!!.isEmpty()) {
            FExcStr.text = "Не найдено место! Отсканируйте коробку."
        }

        lblInfo1.text = dataTable[1][2].toString().substring(dataTable[1][2].toString().trim().length - 5, dataTable[1][2].toString().trim().length - 3) + " " +
        dataTable[1][2].toString().substring( dataTable[1][2].toString().trim().length - 3) +
        " сектор: " + dataTable[1][0].toString().trim() + "-" + dataTable[1][3].toString().trim() +
        " ворота: " + dataTable[1][7].toString().trim() + " адрес: " + dataTable[1][4].toString().trim()
        lblInfo2.text = "место № " + dataTable[1][6].toString().trim() + " из " + dataTable[1][5].toString().trim()
        if (AdressUnLoad != "")
        {

            var adressScan:RefSection = RefSection()
            adressScan.FoundID(AdressUnLoad)
            lblDocInfo.text = "Новый адрес: " + adressScan.Name;
        }
        FExcStr.text = if(CurrentAction == "ScanAdress") "Отсканируйте адрес." else "Отсканируйте коробку."

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
}
