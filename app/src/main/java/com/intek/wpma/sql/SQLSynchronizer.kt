package com.intek.wpma.sql

import android.os.StrictMode
import java.sql.*

/// Обеспечивает коннект, чтение/запись и т.п.
open class SQLSynchronizer {

    private val fServerName: Array<String> = arrayOf("192.168.8.4:57068","192.168.8.5:57068") //Наши серваки
    private val fDBName: String = "int9999001ad4" //База
    //private val fDBName: String = "int9999001rab"
    private val fVers: String = "5.03"    //Номер версии
    val fullVers = "$fVers.11"
    private var myConnection: Connection? = null
    private var myCommand: Statement? = null
    protected var myReader: ResultSet? = null
    private var myCommandForCoroutine: Statement? = null
    protected var myReaderForCoroutine: ResultSet? = null

    protected var fExcStr: String? = null
   // var permission: Boolean? = null

    // public delegate void OpenedEventHendler(object sender, EventArgs e);
    //public event OpenedEventHendler Opened;

    /// Строка исключения или ошибки
    var excStr: String
       get() = fExcStr.toString()
       set(value) {fExcStr  = value }

    val vers:String get() = fVers

    private fun sqlConnect(ServName: String): Boolean {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        var conIsOpen: Boolean
        try {
            val connection = DriverManager.getConnection("jdbc:jtds:sqlserver://$ServName/$fDBName","sa","1419176")
            myConnection = connection
            myCommand = connection.createStatement()
            myCommandForCoroutine = connection.createStatement()
            conIsOpen = !connection.isClosed
        }
        catch (e: SQLException) {
            conIsOpen = false
        }
       return conIsOpen
    }

    /// Конструктор класса
    init {
        openConnection()
    }

    /// Выполняет открытие соединения, или ничего не выполняет, если соединение уже открыто
    private fun openConnection(): Boolean {

        var conIsClosed = true
        if (myConnection != null) conIsClosed = myConnection!!.isClosed

        if (conIsClosed) {
            //попытаемся подключиться к серверу
            if (!sqlConnect(fServerName[0])) {
                if (!sqlConnect(fServerName[1])) {
                    fExcStr = "Не удалось подключиться к серверу!"
                    return false
                }
            }
        }
        return true
    }

    // Выполняет команду на чтение или запись
    protected fun executeQuery(Query: String, Read: Boolean): Boolean {
        //Сохраним первоначальое состояние соединения
        val cs: Boolean = myConnection!!.isClosed
        if (!openConnection()) return false

        try {
            if (Read) myReader = myCommand!!.executeQuery(Query)
            else myCommand!!.execute(Query)
        }
        catch (e: SQLException) {
            if (!cs && myConnection!!.isClosed) {
                //Таким образом, если соединение до выполнения запроса было открыто,
                //а после выполнения обвалилось (см. выше). Это наверняка эффект "уснувшего" терминала! или моргнувшей сети
                //От бесконечной рекурсии мы избавились так - был открыт, стал не открыт - повторяем,
                //при повторе CS полюбому - не открыт, значит дополнительный вызов не произойдет!

                //на всякий случай обождем секунду, может вайфай вернется быстро
                try {
                    Thread.sleep(1000)
                }
                catch (e: java.lang.Exception) { }
                return executeQuery(Query, Read)
            }
            fExcStr = e.toString()
            return false
        }
        return true
    }
    protected fun executeQueryForCoroutine(Query: String, Read: Boolean): Boolean {
        //Сохраним первоначальое состояние соединения
        val cs: Boolean = myConnection!!.isClosed
        if (!openConnection()) return false
        try {
            if (Read) myReaderForCoroutine = myCommandForCoroutine!!.executeQuery(Query)
            else myCommandForCoroutine!!.execute(Query)
        }
        catch (e: SQLException) {
            if (!cs && myConnection!!.isClosed) {
                //Таким образом, если соединение до выполнения запроса было открыто,
                //а после выполнения обвалилось (см. выше). Это наверняка эффект "уснувшего" терминала! или моргнувшей сети
                //От бесконечной рекурсии мы избавились так - был открыт, стал не открыт - повторяем,
                //при повторе CS полюбому - не открыт, значит дополнительный вызов не произойдет!

                //на всякий случай обождем секунду, может вайфай вернется быстро
                try {
                    Thread.sleep(1000)
                }
                catch (e: java.lang.Exception) { }
                return executeQueryForCoroutine(Query, Read)
            }
            fExcStr = e.toString()
            return false
        }
        return true
    }
    // Выполняет команду на чтение
    fun executeQuery(Query: String): Boolean {
        return executeQuery(Query, true)
    }
    fun executeQueryForCoroutine(Query: String): Boolean {
        return executeQueryForCoroutine(Query, true)
    }
}