package com.intek.wpma.SQL

import android.os.StrictMode
import java.sql.*

/// <summary>
/// Обеспечивает коннект, чтение/запись и т.п.
/// </summary>
///
open class SQLSynchronizer
{
    private val fServerName: Array<String> = arrayOf("192.168.8.4:57068","192.168.8.5:57068") //Наши серваки
    private val fDBName: String = "int9999001ad1" //База
    //private val fDBName: String? = "int9999001rab"
    private val fVers: String = "5.03"    //Номер версии
    private var myConnection: Connection? = null
    private var myComand: Statement? = null
    protected var myReader: ResultSet? = null
    protected var fExcStr: String? = null
    var permission: Boolean? = null

    // public delegate void OpenedEventHendler(object sender, EventArgs e);
    //public event OpenedEventHendler Opened;

    /// <summary>
    /// Строка исключения или ошибки
    /// </summary>
    var excStr: String
       get() = fExcStr.toString()
       set(value) {fExcStr  = value }

    val vers:String get() = fVers
    private fun sqlConnect(ServName: String): Boolean
    {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        var conIsOpen: Boolean
        try {
            val connection = DriverManager.getConnection("jdbc:jtds:sqlserver://$ServName/$fDBName","sa","1419176")
            myConnection = connection
            myComand = connection.createStatement()
            conIsOpen = !connection.isClosed
        }
        catch (e: SQLException) {
            conIsOpen = false
        }
       return conIsOpen
    }

    /// <summary>
    /// Конструктор класса
    /// </summary>
    init {
        openConnection()
    }

    /// <summary>
    /// Выполняет открытие соединения, или ничего не выполняет, если соединение уже открыто
    /// </summary>
    /// <returns></returns>
    private fun openConnection(): Boolean
    {
        //FExcStr = null;

        var conIsClosed = true
        if (myConnection != null)
        {
            conIsClosed = myConnection!!.isClosed
        }
        if (conIsClosed)
        {
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

    // <summary>
    // Выполняет команду на чтение или запись
    // </summary>
    // <param name="Query"></param>
    // <param name="Read"></param>
    // <returns></returns>
    protected fun executeQuery(Query: String, Read: Boolean): Boolean
    {
        fExcStr = null
        //Сохраним первоначальое состояние соединения
        val cs: Boolean = myConnection!!.isClosed
        if (!openConnection())
        {
            return false
        }
        try
        {
            if (Read)
            {
                myReader = myComand!!.executeQuery(Query)
            }
            else
            {
                myComand!!.execute(Query)
            }
        }
        catch (e: SQLException)
        {
            if (cs && myConnection!!.isClosed)
            {
                //Таким образом, если соединение до выполнения запроса было открыто,
                //а после выполнения обвалилось (см. выше). Это наверняка эффект "уснувшего" терминала!
                //От бесконечной рекурсии мы избавились так - был открыт, стал не открыт - повторяем,
                //при повторе CS полюбому - не открыт, значит дополнительный вызов не произойдет!
                return executeQuery(Query, Read)
            }
            fExcStr = e.toString()
            return false
        }
        return true
    }
    // <summary>
    // Выполняет команду на чтение
    // </summary>
    // <param name="Query"></param>
    // <returns></returns>
    fun executeQuery(Query: String): Boolean
    {
        return executeQuery(Query, true)
    }
}