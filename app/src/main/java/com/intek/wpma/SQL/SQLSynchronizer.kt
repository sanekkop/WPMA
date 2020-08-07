package com.intek.wpma.SQL

import android.app.Application
import android.os.StrictMode
import java.sql.*

/// <summary>
/// Обеспечивает коннект, чтение/запись и т.п.
/// </summary>
///
open class SQLSynchronizer
{
    protected val FServerName: Array<String> = arrayOf("192.168.8.4:57068","192.168.8.5:57068") //Наши серваки
    protected val FDBName: String = "int9999001ad1" //База
    //protected val FDBName: String? = "int9999001rab"
    protected val FVers: String = "5.02"    //Номер версии
    protected var MyConnection: Connection? = null
    protected var MyComand: Statement? = null
    protected var MyReader: ResultSet? = null
    protected var FExcStr: String? = null
    var permission: Boolean? = null

    // public delegate void OpenedEventHendler(object sender, EventArgs e);
    //public event OpenedEventHendler Opened;

    /// <summary>
    /// Строка исключения или ошибки
    /// </summary>
    var ExcStr: String
       get() = FExcStr.toString()
       set(value) {FExcStr  = value }

    val Vers:String get() = FVers
    fun SQLConnect(ServName: String): Boolean
    {
        val policy = StrictMode.ThreadPolicy.Builder().permitAll().build()
        StrictMode.setThreadPolicy(policy)
        Class.forName("net.sourceforge.jtds.jdbc.Driver")
        var conIsOpen: Boolean
        try {
            var connection = DriverManager.getConnection("jdbc:jtds:sqlserver://" + ServName + "/" + FDBName,"sa","1419176")
            MyConnection = connection
            MyComand = connection.createStatement()
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
        OpenConnection()
    }

    /// <summary>
    /// Выполняет открытие соединения, или ничего не выполняет, если соединение уже открыто
    /// </summary>
    /// <returns></returns>
    private fun OpenConnection(): Boolean
    {
        //FExcStr = null;

        var conIsClosed = true
        if (MyConnection != null)
        {
            conIsClosed = MyConnection!!.isClosed
        }
        if (conIsClosed)
        {
            //попытаемся подключиться к серверу
            if (!SQLConnect(FServerName[0])) {
                if (!SQLConnect(FServerName[1])) {
                    FExcStr = "Не удалось подключиться к серверу!"
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
    protected fun ExecuteQuery(Query: String, Read: Boolean): Boolean
    {
        FExcStr = null
        //Сохраним первоначальое состояние соединения
        val cs: Boolean = MyConnection!!.isClosed
        if (!OpenConnection())
        {
            return false
        }
        try
        {
            if (Read)
            {
                MyReader = MyComand!!.executeQuery(Query)
            }
            else
            {
                MyComand!!.execute(Query)
            }
        }
        catch (e: SQLException)
        {
            if (cs == true && MyConnection!!.isClosed != false)
            {
                //Таким образом, если соединение до выполнения запроса было открыто,
                //а после выполнения обвалилось (см. выше). Это наверняка эффект "уснувшего" терминала!
                //От бесконечной рекурсии мы избавились так - был открыт, стал не открыт - повторяем,
                //при повторе CS полюбому - не открыт, значит дополнительный вызов не произойдет!
                return ExecuteQuery(Query, Read)
            }
            FExcStr = e.toString()
            return false
        }
        return true
    }
    // <summary>
    // Выполняет команду на чтение
    // </summary>
    // <param name="Query"></param>
    // <returns></returns>
    fun ExecuteQuery(Query: String): Boolean
    {
        return ExecuteQuery(Query, true)
    }
}