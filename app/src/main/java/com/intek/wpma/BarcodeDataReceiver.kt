package com.intek.wpma

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.intek.wpma.SQL.SQL1S
import kotlinx.android.synthetic.main.activity_set.*
import java.text.SimpleDateFormat
import java.util.*

open abstract class BarcodeDataReceiver: AppCompatActivity() {

    val TAG = "IntentApiSample"
    val ACTION_BARCODE_DATA = "com.honeywell.sample.action.BARCODE_DATA"
    val ACTION_CLAIM_SCANNER = "com.honeywell.aidc.action.ACTION_CLAIM_SCANNER"
    val ACTION_RELEASE_SCANNER = "com.honeywell.aidc.action.ACTION_RELEASE_SCANNER"
    val EXTRA_SCANNER = "com.honeywell.aidc.extra.EXTRA_SCANNER"
    val EXTRA_PROFILE = "com.honeywell.aidc.extra.EXTRA_PROFILE"
    val EXTRA_PROPERTIES = "com.honeywell.aidc.extra.EXTRA_PROPERTIES"
    val EXTRA_CONTROL = "com.honeywell.aidc.action.ACTION_CONTROL_SCANNER"
    val EXTRA_SCAN = "com.honeywell.aidc.extra.EXTRA_SCAN"

    val sdkVersion = Build.VERSION.SDK_INT
    val SS:SQL1S = SQL1S
    val tsdVers: String = "5.02"
    //для штрих-кода типа data matrix
    val BarcodeId = "w"
    val ResponceTime: Int = 60 //время ожидания отклика от 1С

    fun sendImplicitBroadcast(ctxt: Context, i: Intent) {
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
    fun mysendBroadcast(intent: Intent) {
        if (sdkVersion < 26) {
            sendBroadcast(intent)
        } else {
            //for Android O above "gives W/BroadcastQueue: Background execution not allowed: receiving Intent"
            //either set targetSDKversion to 25 or use implicit broadcast
            sendImplicitBroadcast(applicationContext, intent)
        }

    }
    fun releaseScanner() {
        Log.d("IntentApiSample: ", "releaseScanner")
        mysendBroadcast(Intent(ACTION_RELEASE_SCANNER))
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

        mysendBroadcast(
            Intent(ACTION_CLAIM_SCANNER)
                .putExtra(EXTRA_SCANNER, "dcs.scanner.imager")
                .putExtra(EXTRA_PROFILE, "DEFAULT")// "MyProfile1")
                .putExtra(EXTRA_PROPERTIES, properties)
        )
    }

    fun GoodDone(){
        //GoodVoise()

    }
    fun BadVoise(){
        val bad = MediaPlayer.create(this,R.raw.bad)
        bad.start()
    }
    fun GoodVoise(){
        val good = MediaPlayer.create(this,R.raw.good)
        good.start()
    }
    /// <summary>
    /// формирует строку присвоений для инструкции SET в UPDATE из переданной таблицы
    /// Поддерживает типы - int, DateTime, string
    /// </summary>
    /// <param name="DataMap"></param>
    /// <returns></returns>
    fun ToSetString(DataMap: MutableMap<String, Any>): String {
        var result: String = ""
        for (pair in DataMap) {
            result += SS.GetSynh(pair.key) + "=" + SS.ValueToQuery(pair.value) + ","
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

    /// <summary>
    ///  отсылает команду в 1С и не ждет ответа
    /// </summary>
    /// <param name="Command"></param>
    /// <param name="DataMapWrite"></param>
    /// <returns></returns>
    fun ExecCommandNoFeedback(Command: String, DataMapWrite: MutableMap<String, Any>): Boolean
    {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val currentDate = sdf.format(Date()).substring(0, 10) + " 00:00:00.000"
        val currentTime = timeStrToSeconds(sdf.format(Date()).substring(11, 19))
        val query =
            "UPDATE " + SS.GetSynh("Спр.СинхронизацияДанных") +
                    " SET DESCR='" + Command + "'," + ToSetString(DataMapWrite) + (if(DataMapWrite.isEmpty())  "" else ",") +
                    SS.GetSynh("Спр.СинхронизацияДанных.Дата") + " = '" + currentDate + "', " +
                    SS.GetSynh("Спр.СинхронизацияДанных.Время") + " = " + currentTime + ", " +
                    SS.GetSynh("Спр.СинхронизацияДанных.ФлагРезультата") + " = 1," +
                    SS.GetSynh("Спр.СинхронизацияДанных.ИДТерминала") + " = '" + SS.ANDROID_ID + "'" +
                    " WHERE ID = (SELECT TOP 1 ID FROM " + SS.GetSynh("Спр.СинхронизацияДанных") +
                    " WHERE " + SS.GetSynh("Спр.СинхронизацияДанных.ФлагРезультата") + "=0)"
        if (!SS.ExecuteWithoutRead(query))
        {
            return false
        }
        return true
    }

    fun ExecCommand(
        Command: String,
        DataMapWrite: MutableMap<String, Any>,
        FieldList: MutableList<String>,
        DataMapRead: MutableMap<String, Any>,
        commandID: String
    ): MutableMap<String, Any> {
        //тк в котлине нельзя переприсвоить значение переданному в фун параметру, создаю еще 1 перем
        var commandID: String = ""
        var beda: Int = 0

        if (commandID == "") {
            commandID = SendCommand(Command, DataMapWrite, FieldList)
        }
        //Ждем выполнения или отказа
        val query =
            "SELECT " + SS.GetSynh("Спр.СинхронизацияДанных.ФлагРезультата") + " as Flag" + (if (FieldList.size == 0) "" else "," + SS.ToFieldString(
                FieldList
            )) +
                    " FROM " + SS.GetSynh("Спр.СинхронизацияДанных") + " (nolock)" +
                    " WHERE ID='" + commandID + "'"

        var waitRobotWork: Boolean = false
        val sdf = SimpleDateFormat("HH:mm:ss")
        var timeBegin: Int = timeStrToSeconds(sdf.format(Date()))
        while (kotlin.math.abs(timeBegin - timeStrToSeconds(sdf.format(Date()))) < ResponceTime) {

            val dataTable = SS.ExecuteWithRead(query)
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
                while (i < dataTable.size) {
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

                }
            }
        }
        FExcStr.text = "1C не ответила! " + (if (beda == 0) "" else " Испарений: $beda")
        return DataMapRead

    }

    fun SendCommand(
        Command: String,
        DataMapWrite: MutableMap<String, Any>,
        FieldList: MutableList<String>
    ): String {
        var commandID: String
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        val currentDate = sdf.format(Date()).substring(0, 10) + " 00:00:00.000"
        val currentTime = timeStrToSeconds(sdf.format(Date()).substring(11, 19))

        val textQuery: String =
            "BEGIN TRAN; " +
                    "DECLARE @CommandID as varchar(9); " +
                    "SELECT TOP 1 @CommandID = ID FROM " + SS.GetSynh("Спр.СинхронизацияДанных") + " (tablockx) " +
                    "WHERE " + SS.GetSynh("Спр.СинхронизацияДанных.ФлагРезультата") + "=0; " +
                    "UPDATE " + SS.GetSynh("Спр.СинхронизацияДанных") +
                    " SET DESCR='" + Command + "'," + ToSetString(DataMapWrite) + (if (DataMapWrite.isEmpty()) "" else ",") +
                    SS.GetSynh("Спр.СинхронизацияДанных.Дата") + " = '" + currentDate + "', " +
                    SS.GetSynh("Спр.СинхронизацияДанных.Время") + " = " + currentTime + ", " +
                    SS.GetSynh("Спр.СинхронизацияДанных.ФлагРезультата") + " = 1," +
                    SS.GetSynh("Спр.СинхронизацияДанных.ИДТерминала") + " = '${SS.ANDROID_ID}'" +
                    " WHERE ID=@CommandID; " +
                    " SELECT @@rowcount as Rows, @CommandID as CommandID; " +
                    "COMMIT TRAN;"
        val dataTable = SS.ExecuteWithRead(textQuery)
        if (dataTable == null) {
            FExcStr.text = "Нет доступных команд! Ошибка робота!"
        }
        commandID = dataTable!![1][1]
        return commandID
    }

    fun IBS_Inicialization(EmployerID: String): Boolean
    {
        var textQuery =
            "set nocount on; " +
                    "declare @id bigint; " +
                    "exec IBS_Inicialize_with_DeviceID_new :Employer, :HostName, :DeviceID, @id output; " +
                    "select @id as ID;"
        textQuery = SS.QuerySetParam(textQuery, "Employer", EmployerID)
        textQuery = SS.QuerySetParam(textQuery, "HostName", "Android")
        textQuery = SS.QuerySetParam(textQuery, "DeviceID", SS.ANDROID_ID)
        val dt = SS.ExecuteWithRead(textQuery) ?: return false
        if (dt.isEmpty())
        {
            return false
        }
        return dt[1][0].toInt() > 0

    }

    fun LockoutDoc(IDDoc: String): Boolean {
        return IBS_Lockuot("int_doc_$IDDoc")
    }

    fun IBS_Lockuot(BlockText: String): Boolean {
        var textQuery = "exec IBS_Lockout :BlockText"
        textQuery = SS.QuerySetParam(textQuery, "BlockText", BlockText)
        if (!SS.ExecuteWithoutRead(textQuery)) {
            return false
        }
        return true
    }

     fun checkCameraHardware(context: Context): Boolean {
         return context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)
    }

    fun Login(EmployerID: String): Boolean {
//        if (!SS.UpdateProgram())
//        {
//            return false
//        }
//        if (!SS.SynhDateTime())
//        {
//            return false
//        }
        if (!IBS_Inicialization(EmployerID)) {
            return false
        }

        var dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = SS.ExtendID(EmployerID, "Спр.Сотрудники")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = SS.ANDROID_ID
        if (!ExecCommandNoFeedback("Login", dataMapWrite)) {
            return false
        }
        return true
    }

    fun Logout(EmployerID: String): Boolean{
        var dataMapWrite: MutableMap<String, Any> = mutableMapOf()
        dataMapWrite["Спр.СинхронизацияДанных.ДатаСпрВход1"] = SS.ExtendID(EmployerID, "Спр.Сотрудники")
        dataMapWrite["Спр.СинхронизацияДанных.ДатаВход1"] = SS.ANDROID_ID
        if (!ExecCommandNoFeedback("Logout", dataMapWrite)) {
            return false
        }
        SS.ExecuteWithoutRead("exec IBS_Finalize")
        return true
    }


}