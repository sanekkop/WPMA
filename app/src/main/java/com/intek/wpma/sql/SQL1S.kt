package com.intek.wpma.sql

import android.media.AudioManager
import android.media.SoundPool
import com.intek.wpma.Global
import com.intek.wpma.helpers.ConstantsDepot
import com.intek.wpma.helpers.Helper
import com.intek.wpma.ref.RefEmployer
import com.intek.wpma.ref.RefPalleteMove
import com.intek.wpma.ref.RefPrinter
import net.sourceforge.jtds.jdbc.DateTime
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

/// <summary>
/// Класс организующий доступ и синхронизацию с базой данных компании
/// </summary>

object SQL1S : SQLSynchronizer() {

    private val SynhMap: MutableMap<String, String> =
        mutableMapOf()       //хеш-таблица, сопоставляет имена 1С с именами SQL
    private val ExclusionFields: MutableList<String> = mutableListOf()
    var helper: Helper = Helper()
    var terminal: String = ""
    var isMobile: Boolean = false
    var ANDROID_ID: String = "Android_ID"
    var FEmployer: RefEmployer = RefEmployer()
    var FPrinter: RefPrinter = RefPrinter()
    var FPallets:MutableList<MutableMap<String,String>> = mutableListOf()
    var FPallet = RefPalleteMove()
    val Const: ConstantsDepot = ConstantsDepot
    var widthDisplay: Int = 400
    var heightDisplay: Int = 800
    var title: String = vers
    var CurrentAction: Global.ActionSet? = null
    var CurrentMode: Global.Mode? = null
    val badVoice = SoundPool(1, AudioManager.STREAM_MUSIC,0)
    val goodVoice = SoundPool(1, AudioManager.STREAM_MUSIC,0)
    val clickVoice = SoundPool(1, AudioManager.STREAM_MUSIC, 0)
    val tickVoice = SoundPool(1, AudioManager.STREAM_MUSIC, 0)
    /*Конструктор класса

     */
    init {
        //стандартные поля, не будем извафлятся типа "Наименование" или "Код", дабы не ломать пальцы переключая раскладку
        ExclusionFields.add("ID")
        ExclusionFields.add("DESCR")
        ExclusionFields.add("CODE")
        ExclusionFields.add("ISMARK")
        ExclusionFields.add("DATE_TIME_IDDOC")
        ExclusionFields.add("IDDOCDEF")
        ExclusionFields.add("DOCNO")

        for (curr in ExclusionFields) {
            SynhMap[curr] = curr
        }
        loadAliases()
    }

    /*Функция по выполнению запроса с возвратом результата
     TextQuery - текст запроса, который надо выполнить
     Если не отработал запрос то возвращается null
     Если отработал то возвращается двумерный массив
     */
    fun executeWithRead(TextQuery: String): Array<Array<String>>? {
        var myArr: Array<Array<String>> = emptyArray()
        if (!executeQuery(queryParser(TextQuery))) return null

        if (myReader == null) return null

        while (myReader!!.next()) {
            var i = 1
            var columnArray: Array<String> = emptyArray()
            var rowsArray: Array<String> = emptyArray()
            while (i <= myReader!!.metaData.columnCount) {
                //заполним наименования колонок
                columnArray += myReader!!.metaData.getColumnName(i)
                //а теперь значение
                rowsArray += if (myReader!!.getString(myReader!!.metaData.getColumnName(i)) == null) {
                    "null"
                } else
                    myReader!!.getString(myReader!!.metaData.getColumnName(i))
                i++
            }
            if (myArr.isEmpty()) myArr += columnArray

            myArr += rowsArray
        }
        myReader!!.close()

        return myArr
    }
    fun executeWithReadForCoroutine(TextQuery: String): Array<Array<String>>? {
        var myArr: Array<Array<String>> = emptyArray()
        if (!executeQueryForCoroutine(queryParser(TextQuery))) return null

        if (myReaderForCoroutine == null) return null

        while (myReaderForCoroutine!!.next()) {
            var i = 1
            var columnArray: Array<String> = emptyArray()
            var rowsArray: Array<String> = emptyArray()
            while (i <= myReaderForCoroutine!!.metaData.columnCount) {
                //заполним наименования колонок
                columnArray += myReaderForCoroutine!!.metaData.getColumnName(i)
                //а теперь значение
                rowsArray += if (myReaderForCoroutine!!.getString(myReaderForCoroutine!!.metaData.getColumnName(i)) == null) {
                    "null"
                } else
                    myReaderForCoroutine!!.getString(myReaderForCoroutine!!.metaData.getColumnName(i))
                i++
            }
            if (myArr.isEmpty()) myArr += columnArray

            myArr += rowsArray
        }
        myReaderForCoroutine!!.close()

        return myArr
    }

    fun executeWithReadNew(TextQuery: String): MutableList<MutableMap<String, String>>? {
        val myArr: MutableList<MutableMap<String, String>> = mutableListOf()
        if (!executeQuery(queryParser(TextQuery))) return null

        if (myReader == null) return null

        try {
            while (myReader!!.next()) {
                var i = 1
                val columnArray: MutableMap<String, String> = mutableMapOf()
                while (i <= myReader!!.metaData.columnCount) {
                    //заполним наименования колонок
                    columnArray[myReader!!.metaData.getColumnName(i)] =
                        if (myReader!!.getString(myReader!!.metaData.getColumnName(i)) == null) {
                            "null"
                        } else
                            myReader!!.getString(myReader!!.metaData.getColumnName(i))
                    i++
                }
                myArr.add(columnArray)

            }
            myReader!!.close()

            return myArr
        }
        catch (e:Exception) {
            excStr = e.toString()
            return null
        }
    }

    /*Функция по выполнению запроса без возвращзаения
         TextQuery - текст запроса, торый надо выполнить
         Если не отработал запрос то возвращается false
         Если отработал то возвращается true
    */
    fun executeWithoutRead(TextQuery: String): Boolean {
        return executeQuery(queryParser(TextQuery), false)
    }
    fun executeWithoutReadForCoroutine(TextQuery: String): Boolean {
        return executeQueryForCoroutine(queryParser(TextQuery), false)
    }

    //Возвращает пустой ID
    fun getVoidID(): String {
        return "     0   "
    }

    //Пустая дата
    fun getVoidDate(): String {
        //return "17530101"
        //return "1/1/1753 12:00:00 AM"
        return "17530101 00:00:00.000"
    }

    /*
     reserved words:
         EmptyDate
         EmptyID
         NowDate
         NowTime
     */
    ///Устанавливает значение параметра в запросе
    fun querySetParam(TextQuery: String, NameParam: String, Value: Any): String {
        return TextQuery.replace(":$NameParam", valueToQuery(Value))
    }

    fun valueToQuery(Value: Any): String {
        var result = Value.toString()

        when (Value) {
            is Int -> {
                result = Value.toString()
            }
            is DateTime -> {
                result = "'" + dateTimeToSQL((Value)) + "'"
            }
            is String -> {
                result = "'$Value'"
            }
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
    ///
    /// </summary>
    /// <param name="TextQuery"></param>
    /// <returns></returns>
    private fun queryParser(TextQuery: String): String {

        var result = TextQuery
        result = querySetParam(result, "EmptyDate", getVoidDate())
        result = querySetParam(result, "EmptyID", getVoidID())
        val sdf = SimpleDateFormat("yyyyMMdd HH:mm:ss", Locale.US)
        val currentDate = sdf.format(Date()).substring(0, 8) + " 00:00:00.000"
        val currentTime = timeStrToSeconds(sdf.format(Date()).substring(9, 17))
        result = querySetParam(result, "NowDate", currentDate)
        result = querySetParam(result, "NowTime", currentTime)
        var curI = result.indexOf("$")
        while (curI != -1) {
            val endI = result.substring(curI + 1).indexOf(' ')
            val part = result.substring(curI + 1, curI + 1 + endI)
            result = result.replace("$$part ", getSync(part) + " ")
            curI = result.indexOf('$')
        }
        return result
    }

    private fun loadAliases(): Boolean {
        //Начальная загрузка псевдонимов. Лениво делать список...
        //в принципе метод - нахуй не нужный.
        val defaultAlies: MutableList<String> = mutableListOf()
        //Таблица синхронизации имен
        defaultAlies.add("Константа.ТоварДляЕдиниц")
        defaultAlies.add("Константа.ОснСклад")

        var result = "'"
        var i = 0
        while (i < defaultAlies.count()) {
            result += defaultAlies[i] + "','"
            i++
        }
        //удаляем последнюю запятую и ковычки "','"
        result = result.substring(0, result.length - 2)

        val textQuery =
            "select Name1C as Name1C, NameSQL as NameSQL from RT_Aliases (nolock) where Name1C in ($result)"
        val dataTable = executeWithRead(textQuery)

        if (dataTable!!.isNotEmpty()) for (dr in dataTable) SynhMap[dr[0]] = dr[1]  //если есть незаконченные задания по отбору
        else return false

        return true
    }

    fun addKnownAttributes(parent: String, AttributeList: MutableList<String>) {
        for (pair in SynhMap) {
            if (pair.key.length >= parent.length) {
                if (pair.key.substring(0, parent.length) == parent) {
                    val part: String = pair.key.substring(parent.length)
                    if (part.isNotEmpty()) AttributeList.add(pair.key)
                }
            }
        }
    }

    fun getSync(Alias: String): String {
        if (SynhMap.containsKey(Alias)) return SynhMap[Alias].toString()
        var textQuery =
            "select top 1 NameSQL as NameSQL from RT_Aliases (nolock) where Name1C = :Alias"
        textQuery = querySetParam(textQuery, "Alias", Alias)
        val dt: Array<Array<String>>
        if (executeWithRead(textQuery) == null) {
            throw Exception("Cant connect for load this KEY $Alias!")
        } else dt = executeWithRead(textQuery)!!

        if (dt.isEmpty()) throw Exception("Cant find this KEY $Alias!")
        //val result: String = DT[0]["NameSQL"]
        //ОТТЕСТИРОВАТЬ
        val result: String = dt[1][0].trim()
        SynhMap[Alias] = result
        return result
    }

    /*
                 /// <summary>
                 ///
                 /// </summary>
                 /// <param name="TextQuery"></param>
                 /// <param name="result"></param>
                 public void ExecuteWithReadNew(string TextQuery, out DataTable result)
                 {
                     result = new DataTable();
                     if (!ExecuteQuery(QueryParser(TextQuery)))
                     {
                         throw new TransportExcception(ExcStr);
                     }
                     for (int i = 0; i < MyReader.FieldCount; i++)
                     {
                         result.Columns.Add(MyReader.GetName(i), MyReader.GetFieldType(i));
                     }

                     while (MyReader.Read())
                     {
                         DataRow dr = result.NewRow();
                         for (int col = 0; col < MyReader.FieldCount; col++)
                         {
                             dr[col] = MyReader.GetValue(col);
                         }
                         result.Rows.Add(dr);
                     }
                     MyReader.Close();
                 } */// Обратная совместимость

    /// Возвращает просто конкретное значение без всякой таблицы
    fun executeScalar(textQuery: String): String? {
        var result: String? = null

        if (!executeQuery(queryParser(textQuery))) {
            return result
        }
        if (myReader == null) {
            return result
        }
        if (myReader!!.next()) {
            result = myReader!!.getString(myReader!!.metaData.getColumnName(1))
        }

        myReader!!.close()

        return result

    } // ExecuteScalar

    /// Преобразует имя поля или таблицы SQL в имя 1С
    fun to1CName(SQLName: String): String {
        val result: String
        for (pair in SynhMap) {
            if (pair.value == SQLName) {
                result = pair.key
                return result
            }
        }

        //нихуя не найдено, подсосем из базы!
        var textQuery =
            "select top 1 Name1C as Name1C from RT_Aliases (nolock) where NameSQL = :SQLName"
        textQuery = querySetParam(textQuery, "SQLName", SQLName)
        val dt = executeWithReadNew(textQuery)
            ?: throw Exception("Cant connect for load this SQL name! Sheet!")
        if (dt.isEmpty()) {
            throw Exception("Cant find this SQL name! Sheet!")
        }
        result = dt[0]["Name1C"].toString().trim()
        SynhMap[result] = SQLName   //add in dictionary
        return result
    }

    /// Преобразует дату из DateTime в формат в котором будем писать его в SQL, вот он: YYYY-DD-MM 05:20:00.000
    private fun dateTimeToSQL(DateTime: DateTime): String {
        // из-за отсутствия типа DateTime в kotlin функция нуждается в отладке
        //YYYYMMDD hh:mm:ss.nnn
        //return DateTime.Year.ToString() +
        //        DateTime.Month.ToString().PadLeft(2, '0') +
        //        DateTime.Day.ToString().PadLeft(2, '0') + " 00:00:00.000"
        return DateTime.toDate().toString() +
                DateTime.toTime().toString()

    }

    private fun sqlToDateTime(StrDateTime: String): String {
        //Пока что без времени
        return StrDateTime.substring(0, 10)
       /* return StrDateTime.substring(0, 4) + //"." +
                StrDateTime.substring(4, 7) + //"." +
                StrDateTime.substring(7, 10)*/
    }

    /// Get extend ID, include ID and 4 symbols determining the type (in 36-dimension system)
    fun extendID(ID: String, Type: String): String {
        val bigInteger: BigInteger
        return if (getSync(Type).substring(0, 2) == "SC") {
            bigInteger = getSync(Type).substring(2, getSync(Type).length).toBigInteger()
            val result = bigInteger.toString(36).toUpperCase(Locale.ROOT).padStart(4) + ID
            result
        } else {
            bigInteger = getSync(Type).toBigInteger()
            bigInteger.toString(36).toUpperCase(Locale.ROOT).padStart(4) + ID
        }
    }

    /*
     public bool GetColumns(string table_name, out string columns, string SQLfunc)
     {
         string separator = ",";
         string tail = "";   //В конце что добавим
         columns = "";
         if (SQLfunc != null)
         {
             separator = ")," + SQLfunc + "(";
             columns = SQLfunc + "(";
             tail = ")";
         }
         string TextQuery =
         "declare @ColumnList varchar(1000); " +
                 "select @ColumnList = COALESCE(@ColumnList + '" + separator + "', '') + column_name " +
                 "from INFORMATION_SCHEMA.Columns " +
                 "where table_name = :table_name; " +
                 "select @ColumnList as ColumnList";
         SQL1S.QuerySetParam(ref TextQuery, "table_name", table_name + " "); //Пробел в конце, чтобы парсер нормально отработал
         DataTable DT;
         ExecuteWithReadNew(TextQuery, out DT);
         if (DT.Rows.Count == 0)
         {
             return false;
         }

         columns += DT.Rows[0]["ColumnList"].ToString();
         columns += tail;
         return true;
     } // GetColumns
     public bool GetCollumns(string table_name, out string columns, string SQLfunc)
     {
         return GetColumns(table_name, out columns, null);
     } // GetCollumns
     */
    fun isVoidDate(DateTime: String): Boolean {
        //Тут можно и по красивей написать...
        return if (DateTime == "1753-01-01 00:00:00.0") {
            true
        } else DateTime == "17530101 00:00:00.000"
    }

    /*
     /// <summary>
     ///
     /// </summary>
     /// <param name="DateTime"></param>
     /// <returns></returns>
     static public string GetPeriodSQL(DateTime DateTime) {
         return "{d '" + DateTime.Year.ToString() + "-" + DateTime.Month.ToString().PadLeft(2, '0') + "-01'}";
     }*/
    /// Приводит передаваемый список строк в строку разделенную запятыми
    fun toFieldString(DataList: MutableList<String>): String {
        var result = ""
        for (item in DataList) {
            result += getSync(item) + " as " + (if ("." in item) item.replace(
                ".",
                ""
            ) else item.trim()) + " ,"
        }
        //удаляем последнюю запятую
        if (result.isNotEmpty()) {
            result = result.substring(0, result.length - 1)
        }
        return result
    }

    /*
                 /// <summary>
                 /// возвращает список данных элемента справочника по его IDD или же ID (регулируется параметром ThisID)
                 /// </summary>
                 /// <param name="IDDorID"></param>
                 /// <param name="SCType"></param>
                 /// <param name="FieldList"></param>
                 /// <param name="DataMap"></param>
                 /// <param name="ThisID"></param>
                 /// <returns></returns>
                 */
    //пока переписал эту функцию принимающую FieldList: String вместо FieldList: MutableList<String>
    // чтобы это исправить нужно поднять функцию StringToList из класса Helper
    fun getSCData(
        IDDorID: String,
        SCType: String,
        FieldList: MutableList<String>,
        DataMap: MutableMap<String, Any>,
        ThisID: Boolean
    ): MutableMap<String, Any>? {
        val scType = "Спр.$SCType"

        if (!executeQuery(
                "SELECT " + toFieldString(FieldList) + " FROM " + getSync(scType) + " (nolock)" +
                        " WHERE " + (if (ThisID) {
                    "ID"
                } else {
                    getSync("$scType.IDD")
                } + "='" + IDDorID + "'")
            )
        ) {
            return null
        }

        return if (myReader!!.next()) {
            var i = 1
            while (i <= myReader!!.metaData.columnCount) {
                DataMap[FieldList[i - 1]] =
                    myReader!!.getString(myReader!!.metaData.getColumnName(i))
                i++
            }
            myReader!!.close()
            DataMap
        } else {
            myReader!!.close()
            fExcStr = "Элемент справочника не найден!"
            null
        }
    }

    fun getSCData(
        IDD: String,
        SCType: String,
        ListStr: String,
        DataMap: MutableMap<String, Any>,
        ThisID: Boolean
    ): MutableMap<String, Any>? {
        val fieldList: MutableList<String> = helper.stringToList(ListStr)
        var i = 0
        while (i < fieldList.count()) {
            var curr = fieldList[i]
            if (!ExclusionFields.contains(curr)) {
                curr = "Спр.$SCType.$curr"
                fieldList.removeAt(i)
                fieldList.add(i, curr)
            }
            i++
        }
        return getSCData(IDD, SCType, fieldList, DataMap, ThisID)
    }

    fun isSC(IDD: String, SCType: String): Boolean {
        if (SCType == "Сотрудники") {
            val textQuery =
                "SELECT ID FROM \$Спр.Сотрудники (nolock) WHERE \$Спр.Сотрудники.IDD = '$IDD' "
            val dataTable = executeWithRead(textQuery)
            return dataTable!!.isNotEmpty()
        }
        if (SCType == "Секции") {
            val textQuery =
                "SELECT ID FROM \$Спр.Секции (nolock) WHERE \$Спр.Секции.IDD = '$IDD' "
            val dataTable = executeWithRead(textQuery)
            return dataTable!!.isNotEmpty()
        }
        if (SCType == "Принтеры") {
            val textQuery =
                "SELECT ID FROM \$Спр.Принтеры (nolock) WHERE \$Спр.Принтеры.IDD = '$IDD' "
            val dataTable = executeWithRead(textQuery)
            return dataTable!!.isNotEmpty()
        }
        if (SCType == "МестаПогрузки") {
            val textQuery =
                "SELECT ID FROM \$Спр.МестаПогрузки (nolock) WHERE ID = '$IDD' "
            val dataTable = executeWithRead(textQuery)
            return dataTable!!.isNotEmpty()
        }
        return true
    }

    /// возвращает список данных элемента справочника по его IDD
    fun getSCData(
        IDD: String,
        SCType: String,
        FieldList: MutableList<String>,
        DataMap: MutableMap<String, Any>
    ): MutableMap<String, Any>? {
        return getSCData(IDD, SCType, FieldList, DataMap, false)
    }

    fun getDoc(IDDorID: String, ThisID: Boolean): MutableMap<String, String>? {
        var iddocid = IDDorID
        val dataMap: MutableMap<String, String> = mutableMapOf()
        if (ThisID) {
            //Если ID - расширенный, то переведем его в обычный, 9-и символьный
            if (iddocid.length > 9) {
                iddocid = IDDorID.substring(4)
            }
        }
      /*  val textQuery = "SELECT IDDOC, IDDOCDEF, DATE_TIME_IDDOC, DOCNO, ISMARK, " + getSynh("IDD") + " as IDD" +
                " FROM _1SJOURN (nolock) WHERE ISMARK = 0 and " + if (ThisID) "IDDOC" else {
            getSynh("IDD")
        } + "='" + iddocid + "'"

        val dt = executeWithReadNew(textQuery)*/
        val dt = executeWithReadNew(
            "SELECT IDDOC, IDDOCDEF, DATE_TIME_IDDOC, DOCNO, ISMARK, " + getSync("IDD") + " as IDD" +
                    " FROM _1SJOURN (nolock) WHERE ISMARK = 0 and " + if (ThisID) "IDDOC" else {
                getSync("IDD")
            } + "='" + iddocid + "'"
        )

        if (dt == null || dt.isEmpty()) {
            return null
        }
        dataMap["ID"] = dt[0]["IDDOC"].toString()
        dataMap["IDD"] = dt[0]["IDD"].toString()
        dataMap["ПометкаУдаления"] = dt[0]["ISMARK"].toString()
        dataMap["НомерДок"] = dt[0]["DOCNO"].toString()
        dataMap["ДатаДок"] = sqlToDateTime(dt[0]["DATE_TIME_IDDOC"].toString())
        dataMap["Тип"] = to1CName(dt[0]["IDDOCDEF"].toString())
        return dataMap
    }

    fun getDocData(
        iddoc: String,
        docType: String,
        fieldList: MutableList<String>
    ): MutableMap<String, String>? {
        val dataMap: MutableMap<String, String> = mutableMapOf()
        if (!executeQuery("SELECT " + toFieldString(fieldList) + " FROM DH" + getSync(docType) + " (nolock) WHERE IDDOC='" + iddoc + "'")) {
            return null
        }
        return if (myReader!!.next()) {
            var i = 1
            while (i <= myReader!!.metaData.columnCount) {
                dataMap[fieldList[i - 1]] =
                    myReader!!.getString(myReader!!.metaData.getColumnName(i))
                i++
            }
            myReader!!.close()
            dataMap
        }
        else null
    }
}//class SQLSynhronizer

