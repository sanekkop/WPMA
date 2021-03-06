package com.intek.wpma

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.hardware.Camera
import android.net.ConnectivityManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TableLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.intek.wpma.sql.SQL1S
import kotlinx.android.synthetic.main.activity_set.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

abstract class BarcodeDataReceiver: AppCompatActivity() {

    val TAG = "IntentApiSample"
    val ACTION_BARCODE_DATA = "com.honeywell.sample.action.BARCODE_DATA"
    val ACTION_CLAIM_SCANNER = "com.honeywell.aidc.action.ACTION_CLAIM_SCANNER"
    val ACTION_RELEASE_SCANNER = "com.honeywell.aidc.action.ACTION_RELEASE_SCANNER"
    val EXTRA_SCANNER = "com.honeywell.aidc.extra.EXTRA_SCANNER"
    val EXTRA_PROFILE = "com.honeywell.aidc.extra.EXTRA_PROFILE"
    val EXTRA_PROPERTIES = "com.honeywell.aidc.extra.EXTRA_PROPERTIES"
    val EXTRA_CONTROL = "com.honeywell.aidc.action.ACTION_CONTROL_SCANNER"
    val EXTRA_SCAN = "com.honeywell.aidc.extra.EXTRA_SCAN"

    private val sdkVersion = Build.VERSION.SDK_INT
    val ss: SQL1S = SQL1S

    //для штрих-кода типа data matrix
    val barcodeId = "w"
    private val responseTime: Int = 60 //время ожидания отклика от 1С
    private fun sendImplicitBroadcast(ctxt: Context, i: Intent) {
        val pm = ctxt.packageManager
        val matches = pm.queryBroadcastReceivers(i, 0)

        for (resolveInfo in matches) {
            val explicit = Intent(i)
            val cn = ComponentName(
                resolveInfo.activityInfo.applicationInfo.packageName,
                resolveInfo.activityInfo.name
            )

            explicit.component = cn
            ctxt.sendBroadcast(explicit)
        }
    }

    private fun mySendBroadcast(intent: Intent) {
        if (sdkVersion < 26) sendBroadcast(intent)
        else {
            //for Android O above "gives W/BroadcastQueue: Background execution not allowed: receiving Intent"
            //either set targetSDKversion to 25 or use implicit broadcast
            sendImplicitBroadcast(applicationContext, intent)
        }

    }

    fun releaseScanner() {
        Log.d("IntentApiSample: ", "releaseScanner")
        mySendBroadcast(Intent(ACTION_RELEASE_SCANNER))
    }

    fun claimScanner() {
        Log.d("IntentApiSample: ", "claimScanner")
        val properties = Bundle()
        properties.putBoolean("DPR_DATA_INTENT", true)
        properties.putString("DPR_DATA_INTENT_ACTION", ACTION_BARCODE_DATA)

        properties.putInt("TRIG_AUTO_MODE_TIMEOUT", 2)
        properties.putString(
            "TRIG_SCAN_MODE",
            "readOnRelease"
        ) //This works for Hardware Trigger only! If scan is started from code, the code is responsible for a switching off the scanner before a decode

        mySendBroadcast(
            Intent(ACTION_CLAIM_SCANNER)
                .putExtra(EXTRA_SCANNER, "dcs.scanner.imager")
                .putExtra(EXTRA_PROFILE, "DEFAULT")// "MyProfile1")
                .putExtra(EXTRA_PROPERTIES, properties)
        )
    }

    fun badVoice() = ss.badVoice.play(1, 1F, 1F, 1, 0, 1F)

    fun goodVoice() = ss.goodVoice.play(1, 1F, 1F, 1, 0, 1F)

    fun clickVoice() = ss.clickVoice.play(1, 1F, 1F, 1, 0, 1F)

    fun tickVoice() = ss.tickVoice.play(1, 1F, 1F, 1, 0, 1F)

    /// формирует строку присвоений для инструкции SET в UPDATE из переданной таблицы
    /// Поддерживает типы - int, DateTime, string
    private fun toSetString(DataMap: MutableMap<String, Any>): String {
        var result = ""
        for (pair in DataMap) {
            result += ss.getSync(pair.key) + "=" + ss.valueToQuery(pair.value) + ","
        }
        //удаляем последнюю запятую
        if (result.isNotEmpty()) {
            result = result.substring(0, result.length - 1)
        }
        return result
    }

    fun timeStrToSeconds(str: String): Int {
        val parts = str.split(":")
        var result = 0
        for (part in parts) {
            val number = part.toInt()
            result = result * 60 + number
        }
        return result
    }

    ///  отсылает команду в 1С и не ждет ответа
    fun execCommandNoFeedback(Command: String, DataMapWrite: MutableMap<String, Any>): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val currentDate = sdf.format(Date()).substring(0, 10) + " 00:00:00.000"
        val currentTime = timeStrToSeconds(sdf.format(Date()).substring(11, 19))
        val query =
            "UPDATE " + ss.getSync("Спр.СинхронизацияДанных") +
                    " SET DESCR='" + Command + "'," + toSetString(DataMapWrite) + (if (DataMapWrite.isEmpty()) "" else ",") +
                    ss.getSync("Спр.СинхронизацияДанных.Дата") + " = '" + currentDate + "', " +
                    ss.getSync("Спр.СинхронизацияДанных.Время") + " = " + currentTime + ", " +
                    ss.getSync("Спр.СинхронизацияДанных.ФлагРезультата") + " = 1," +
                    ss.getSync("Спр.СинхронизацияДанных.ИДТерминала") + " = '" + ss.ANDROID_ID + "'" +
                    " WHERE ID = (SELECT TOP 1 ID FROM " + ss.getSync("Спр.СинхронизацияДанных") +
                    " WHERE " + ss.getSync("Спр.СинхронизацияДанных.ФлагРезультата") + "=0)"
        if (!ss.executeWithoutRead(query)) {
            return false
        }
        return true
    }

    fun execCommand(
        Command: String,
        DataMapWrite: MutableMap<String, Any>,
        FieldList: MutableList<String>,
        DataMapRead: MutableMap<String, Any>,
    ) : MutableMap<String, Any> = execCommand(Command, DataMapWrite, FieldList, DataMapRead, "")

    fun execCommand(
        Command: String,
        DataMapWrite: MutableMap<String, Any>,
        FieldList: MutableList<String>,
        DataMapRead: MutableMap<String, Any>,
        CommandID : String
    ): MutableMap<String, Any> {
        //тк в котлине нельзя переприсвоить значение переданному в фун параметру, создаю еще 1 перем
        var commandID = CommandID
        var beda = 0

        if (commandID == "") {
            commandID = sendCommand(Command, DataMapWrite)
        }
        //Ждем выполнения или отказа
        val query =
            "SELECT " + ss.getSync("Спр.СинхронизацияДанных.ФлагРезультата") + " as Flag" + (if (FieldList.size == 0) "" else "," + ss.toFieldString(
                FieldList
            )) +
                    " FROM " + ss.getSync("Спр.СинхронизацияДанных") + " (nolock)" +
                    " WHERE ID='" + commandID + "'"

        var waitRobotWork = false
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.US)
        var timeBegin: Int = timeStrToSeconds(sdf.format(Date()))
        while (kotlin.math.abs(timeBegin - timeStrToSeconds(sdf.format(Date()))) < responseTime) {

            val dataTable = ss.executeWithRead(query)
            //Ждем выполнения или отказа
            if (dataTable == null) {
                FExcStr.text = "Нет доступных команд! Ошибка робота!"
            }
            DataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] = dataTable!![1][0]
            if ((DataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() != 1) {
                if ((DataMapRead["Спр.СинхронизацияДанных.ФлагРезультата"] as String).toInt() == 2) {
                    if (!waitRobotWork) {
                        //1C получила команду, сбросим время ожидания
                        timeBegin = timeStrToSeconds(sdf.format(Date()))
                        waitRobotWork = true
                    }
                    continue
                }
                var i = 1
                while (i < dataTable[0].size) {
                    DataMapRead[FieldList[i - 1]] = dataTable[1][i]
                    i++
                }
                return DataMapRead
            } else {
                beda++
                continue   //Бред какой-то, попробуем еще раз
            }

            if (timeBegin + 1 < timeStrToSeconds(sdf.format(Date()))) {
                //Пауза в 1, после первой секунды беспрерывной долбежки!
                val tb: Int = timeStrToSeconds(sdf.format(Date()))
                while (kotlin.math.abs(tb - timeStrToSeconds(sdf.format(Date()))) < 1) {
                    //пустой цикл для паузы
                }
            }
        }
        ss.excStr = "1C не ответила! " + (if (beda == 0) "" else " Испарений: $beda")
        FExcStr.text = ss.excStr
        return DataMapRead

    }

    private fun sendCommand(Command: String, DataMapWrite: MutableMap<String, Any>): String {
        val commandID: String
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
        val currentDate = sdf.format(Date()).substring(0, 10) + " 00:00:00.000"
        val currentTime = timeStrToSeconds(sdf.format(Date()).substring(11, 19))

        val textQuery: String =
            "BEGIN TRAN; " +
                    "DECLARE @CommandID as varchar(9); " +
                    "SELECT TOP 1 @CommandID = ID FROM \$Спр.СинхронизацияДанных (tablockx) " +
                    "WHERE \$Спр.СинхронизацияДанных.ФлагРезультата = 0; " +
                    "UPDATE \$Спр.СинхронизацияДанных " +
                    " SET DESCR='" + Command + "'," + toSetString(DataMapWrite) + (if (DataMapWrite.isEmpty()) "" else ",") +
                    "\$Спр.СинхронизацияДанных.Дата = '" + currentDate + "', " +
                    "\$Спр.СинхронизацияДанных.Время  = " + currentTime + ", " +
                    "\$Спр.СинхронизацияДанных.ФлагРезультата = 1," +
                    "\$Спр.СинхронизацияДанных.ИДТерминала = '${ss.ANDROID_ID}'" +
                    " WHERE ID=@CommandID; " +
                    " SELECT @@rowcount as Rows, @CommandID as CommandID; " +
                    "COMMIT TRAN;"
        val dataTable = ss.executeWithReadNew(textQuery)
        if (dataTable == null) {
            FExcStr.text = "Нет доступных команд! Ошибка робота!"
        }
        commandID = dataTable!![0]["CommandID"].toString()
        return commandID
    }

    private fun ibsInitialization(EmployerID: String): Boolean {
        var textQuery =
            "set nocount on; " +
                    "declare @id bigint; " +
                    "exec IBS_Inicialize_with_DeviceID_new :Employer, :HostName, :DeviceID, @id output; " +
                    "select @id as ID;"
        textQuery = ss.querySetParam(textQuery, "Employer", EmployerID)
        textQuery = ss.querySetParam(textQuery, "HostName", "Android-"+ss.terminal.trim())
        textQuery = ss.querySetParam(textQuery, "DeviceID", ss.ANDROID_ID)
        val dt = ss.executeWithRead(textQuery) ?: return false
        if (dt.isEmpty()) {
            return false
        }
        return dt[1][0].toInt() > 0

    }

    fun lockoutDoc(IDDoc: String): Boolean {
        return ibsLockOut("int_doc_$IDDoc")
    }

    fun ibsLockOut(BlockText: String): Boolean {
        var textQuery = "exec IBS_Lockout :BlockText"
        textQuery = ss.querySetParam(textQuery, "BlockText", BlockText)
        if (!ss.executeWithoutRead(textQuery)) {
            return false
        }
        return true
    }

    fun lockDoc(IDDoc: String): Boolean {
        return ibsLock("int_doc_$IDDoc")
    }

    private fun ibsLock(BlockText: String): Boolean {

        var textQuery =
            "set nocount on; " +
                    "declare @result int; " +
                    "exec IBS_Lock :BlockText, @result output; " +
                    "select @result as result;"
        textQuery = ss.querySetParam(textQuery, "BlockText", BlockText)
        var dataTable: Array<Array<String>>? = ss.executeWithRead(textQuery) ?: return false
        if (dataTable!![1][0].toInt() > 0) {
            return true
        }
        else {
            ss.excStr = "Объект заблокирован!"
            FExcStr.text = "Объект заблокирован!" //Ответ по умолчанию
            //Покажем кто заблокировал
            textQuery =
                "SELECT " +
                        "ISNULL(rtrim(Collation.HostName),'') as HostName, " +
                        "ISNULL(rtrim(Collation.UserName),'') as UserName, " +
                        "convert(char(8), Block.date_time, 4) as Date, " +
                        "substring(convert(char, Block.date_time, 21), 12, 8) as Time " +
                        "FROM " +
                        "IBS_Block as Block " +
                        "INNER JOIN IBS_Collation as Collation " +
                        "ON Collation.ID = Block.ProcessID " +
                        "WHERE " +
                        "left(Block.BlockText, len(:BlockText)) = :BlockText "
            textQuery = ss.querySetParam(textQuery, "BlockText", BlockText)
            dataTable = ss.executeWithRead(textQuery)
            if (dataTable!!.isNotEmpty()) {
                ss.excStr = "Объект заблокирован! " + dataTable[1][1] + ", " + dataTable[1][0] +
                        ", в " + dataTable[1][3] + " (" + dataTable[1][2] + ")"
                FExcStr.text = ss.excStr

            }
            return false
        }
    }

    private fun ibsAbsoluteLock(BlockText: String):Boolean  {
        var textQuery =
        "set nocount on; " +
                "exec IBS_AbsoluteLock :BlockText;"
        textQuery = ss.querySetParam(textQuery, "BlockText", BlockText)
        return ss.executeWithoutRead(textQuery)
    }

    fun lockDocAccept(IDDoc: String): Boolean {
        val blockText1 = "int_doc_$IDDoc"
        val blockText2 = blockText1 + "_accept"
        val lock1 = ibsLock(blockText1)
        val lock2 = ibsAbsoluteLock(blockText2)
        return if (lock1 && lock2) {
            //Обе могут, ништяк! Снимем лишний и довольные выходим!
            ibsLockOut(blockText1)
            true
        } else {
            //Не могут обе, нужно снять свои блокировки
            if (lock1) {
                ibsLockOut(blockText1)
            }
            if (lock2) {
                ibsLockOut(blockText2)
            }
            false
        }
    }

    fun lockoutDocAccept(IDDoc: String): Boolean {
        return ibsLockOut("int_doc_" + IDDoc + "_accept")
    }

    fun lockItem(ItemID: String): Boolean {
        val blockText1 = "int_ref_Товары_$ItemID"
        val blockText2 = blockText1 + "_unit"
        val lock1 = ibsLock(blockText1)
        val lock2 = ibsLock(blockText2)
        //Только при отсутствии любой из этих блокировок
        return if (lock1 && lock2) {
            //Обе могут, ништяк! Снимем лишний и довольные выходим!
            ibsLockOut(blockText1)
            true
        } else {
            //Не могут обе, нужно снять свои блокировки
            if (lock1) {
                ibsLockOut(blockText1)
            }
            if (lock2) {
                ibsLockOut(blockText2)
            }
            false
        }
    }

    fun checkCameraHardware(context: Context): Boolean {

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
        } else {
            @Suppress("DEPRECATION") val numCameras: Int = Camera.getNumberOfCameras()
            numCameras > 0

        }
    }

    fun login(EmployerID: String): Boolean {
//        if (!SS.UpdateProgram())
//        {
//            return false
//        }
//        if (!SS.SynhDateTime())
//        {
//            return false
//        }
        if (!ibsInitialization(EmployerID)) {
            return false
        }

        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] =
            ss.extendID(EmployerID, "Спр.Сотрудники")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = ss.ANDROID_ID
        if (!execCommandNoFeedback("Login", dataMapWrite)) {
            return false
        }
        return true
    }

    fun logout(EmployerID: String): Boolean {
        val dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] =
            ss.extendID(EmployerID, "Спр.Сотрудники")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = ss.ANDROID_ID
        if (!execCommandNoFeedback("Logout", dataMapWrite)) {
            return false
        }
        ss.executeWithoutRead("exec IBS_Finalize")
        return true
    }

    fun isMarkProduct(ItemID: String): Boolean {
        val dt = getTableMark(ItemID) ?: return false
        return dt.isNotEmpty()
    }

    fun getTableMark(id: String): MutableList<MutableMap<String, String>>? {
        val textQuery =
            "SELECT " +
                    "min(Product.\$Спр.Товары.ИнвКод ) as ИнвКод , " +
                    "min(Product.descr ) as Name , " +
                    "isNull(max(TNVED.\$Спр.ТНВЭД.Маркировка ),isNull(max(Categories.\$Спр.КатегорииТоваров.Маркировка ),0)) as Маркировка  " +
                    "FROM " +
                    "\$Спр.Товары  as Product (nolock)" +
                    "LEFT JOIN \$Спр.КатегорииТоваров  as Categories (nolock) " +
                    "ON Categories.id = Product.\$Спр.Товары.Категория " +
                    "LEFT JOIN \$Спр.Сертификаты  as Certificate (nolock) " +
                    "ON ( select top 1" +
                            "left(Const.value, 9) " +
                        "from" +
                            "_1sconst as Const (nolock) " +
                        "where" +
                            "Const.id = 1336 " +
                            "and Const.objid = Product.ID " +
                            "and (Const.date <= :NowDate )" +
                        "order by Const.date desc, Const.time desc, Const.docid desc, Const.row_id desc " +
                    ") = Certificate.id " +
                    "LEFT JOIN \$Спр.ТНВЭД  as TNVED (nolock) " +
                    "ON Certificate.\$Спр.Сертификаты.ТНВЭДСпр = TNVED.id " +
                    "LEFT JOIN \$Спр.МаркировкаТовара  as Mark (nolock) " +
                    "ON Product.id = Mark.\$Спр.МаркировкаТовара.Товар " +

                "WHERE " +
                    "Product.id = '${id}' " +
                    "and (isNull(Categories.\$Спр.КатегорииТоваров.Маркировка , 0 ) > 0 " +
                    "OR isNull(TNVED.\$Спр.ТНВЭД.Маркировка , 0 ) > 0 " +
                    "OR not (Mark.id is null)) " +
                "GROUP BY Product.id "

        return ss.executeWithReadNew(textQuery)
    }

    fun checkOrder():String {
        val result = "Menu"
        //ЗАДАНИЕ СПУСКА
        if (ss.FEmployer.canDown) {
            var textQuery =
            "select top 1 " +
                    "Ref.id " +
                    "from \$Спр.МестаПогрузки as Ref (nolock) " +
                    "where " +
                    "Ref.\$Спр.МестаПогрузки.Сотрудник4 = :Employer " +
                    "and Ref.ismark = 0 " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата40 = :EmptyDate " +
                    "and not Ref.\$Спр.МестаПогрузки.Адрес3 = :EmptyID " +
                    "and Ref.\$Спр.МестаПогрузки.Дата5 = :EmptyDate "
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            val dt  = ss.executeWithRead(textQuery) ?: return result
            if (dt.isNotEmpty())
            {
                return "Down"
            }
        }
        //ЗАДАНИЕ СВОБОДНОГО СПУСКА/КОМПЛЕКТАЦИИ
        if (ss.FEmployer.canComplectation) {
            var textQuery =
            "select top 1 " +
                    "Ref.id " +
                    "from \$Спр.МестаПогрузки as Ref (nolock) " +
                    "where " +
                    "Ref.ismark = 0 " +
                    "and (" +
                    "Ref.\$Спр.МестаПогрузки.Сотрудник4 = :Employer " +
                    "and Ref.\$Спр.МестаПогрузки.Дата5 = :EmptyDate " +
                    "and Ref.\$Спр.МестаПогрузки.Адрес3 = :EmptyID " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата40 = :EmptyDate" +
                    " or " +
                    "Ref.\$Спр.МестаПогрузки.Сотрудник8 = :Employer " +
                    "and Ref.\$Спр.МестаПогрузки.Дата9 = :EmptyDate " +
                    "and Ref.\$Спр.МестаПогрузки.Адрес7 = :EmptyID " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата80 = :EmptyDate)"
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            val dt  = ss.executeWithRead(textQuery) ?: return result
            if (dt.isNotEmpty())
            {
                return "FreeDownComplete"
            }
        }
        //ЗАДАНИЕ ОТБОРА КОМПЛЕКТАЦИИ
        if (ss.FEmployer.canComplectation) {
            var textQuery =
            "select top 1 " +
                    "Ref.id " +
                    "from \$Спр.МестаПогрузки as Ref (nolock) " +
                    "where " +
                    "Ref.ismark = 0 " +
                    "and Ref.\$Спр.МестаПогрузки.Сотрудник8 = :Employer " +
                    "and Ref.\$Спр.МестаПогрузки.Дата9 = :EmptyDate " +
                    "and not Ref.\$Спр.МестаПогрузки.Адрес7 = :EmptyID " +
                    "and not Ref.\$Спр.МестаПогрузки.Дата80 = :EmptyDate"
            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
            val dt  = ss.executeWithRead(textQuery) ?: return result
            if (dt.isNotEmpty())
            {
                return "NewComplectation"
            }
        }

        return result
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_IMMERSIVE
               // or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                )
    }

    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    open fun isOnline(context: Context): Boolean {
        val cm = context.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }

    open fun getWareHouse(id : String, type : String) : String {

        if (type == "id") {
            val textQuery =
                "SELECT ID FROM \$Спр.Секции (nolock) WHERE \$Спр.Секции.IDD = '$id' "
            val dataTable = ss.executeWithReadNew(textQuery)

            return dataTable!![0]["ID"].toString()
        }
        if (type == "idByName") {
            val textQuery =
                "SELECT Warehouse.id as ID FROM \$Спр.Секции (nolock) as WareHouse (nolock) WHERE Warehouse.descr = '$id' "
            val dataTable = ss.executeWithReadNew(textQuery)

            return dataTable!![0]["ID"].toString()
        }
        if (type == "name") {
            var textQuery = "SELECT " +
                    "Warehouse.id as ID, " +
                    "Warehouse.descr as Name " +
                    "FROM " +
                    "\$Спр.Склады as Warehouse (nolock) " +
                    "WHERE " +
                    "Warehouse.id = :id " +
                    "ORDER BY Warehouse.descr"
            textQuery = ss.querySetParam(textQuery, "id", id)
            val ware = ss.executeWithReadNew(textQuery) ?: return ""

            return ware[0]["Name"].toString()
        }

        return ""
    }

    open fun customTable(context: Context, rows : Int,  data : Array<String>, table : TableLayout, type: String) {
        val size : Array<Double> = Array(data.size) { 0.25 }
        return customTable(context, rows, data, size, table, type)
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    open fun customTable(
        context: Context,
        rows : Int,
        data : Array<String>,
        size : Array<Double>,
        table : TableLayout,
        type : String) {

        var k = 0
        val linearLayout = LinearLayout(context)
        val placeValue : MutableMap<String, TextView> = HashMap()
        for (i in 0..rows) placeValue["plVal$i"] = TextView(context)

        for ((i,_) in placeValue) {

            placeValue[i]?.apply {
                text = (" " + data[k])
                typeface = Typeface.SERIF
                layoutParams = LinearLayout.LayoutParams(
                    (ss.widthDisplay * size[k]).toInt(), ViewGroup.LayoutParams.WRAP_CONTENT)
                textSize = 18F
                setTextColor(-0x1000000)

                if (type == "head") background = getDrawable(R.drawable.cell_border)
            }
            linearLayout.addView(placeValue[i])
            k++
        }
        if (type == "head") linearLayout.setBackgroundColor(Color.rgb(192, 192, 192))
        else linearLayout.setBackgroundColor(Color.rgb(255, 255, 255))
        table.addView(linearLayout)
    }


    fun updateInitialize(context: Context) {
        CoroutineScope(IO).launch {
            delay(10 * 1000L)
            if (ss.FEmployer.selected) {
                Handler(Looper.getMainLooper()).post {
                    var textQuery =
                        "set nocount on; " +
                                "declare @id bigint; " +
                                "exec IBS_update :DeviceID, @id output; " +
                                "select @id as ID;"
                    textQuery = ss.querySetParam(textQuery, "DeviceID", ss.ANDROID_ID)
                    val dt = ss.executeWithReadForCoroutine(textQuery)
                    if (dt != null && dt.isNotEmpty()) {
                        val result = dt[1][0].toInt()
                        if (result == 0 && ss.FEmployer.selected) {
                            //не нашел по девайс айди, значит надо инициировать
                            textQuery =
                                "set nocount on; " +
                                        "declare @id bigint; " +
                                        "exec IBS_Inicialize_with_DeviceID_new :Employer, :HostName, :DeviceID, @id output; " +
                                        "select @id as ID;"
                            textQuery = ss.querySetParam(textQuery, "Employer", ss.FEmployer.id)
                            textQuery = ss.querySetParam(textQuery, "HostName", "Android - " + ss.terminal.trim())
                            textQuery = ss.querySetParam(textQuery, "DeviceID", ss.ANDROID_ID)
                            ss.executeWithoutReadForCoroutine(textQuery)
                        }
                    }
                    //Toast.makeText(context, ss.FEmployer.name, Toast.LENGTH_SHORT).show()
                }
                updateInitialize(context)
            }
        }
    }
}