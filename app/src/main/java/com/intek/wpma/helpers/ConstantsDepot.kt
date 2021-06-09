package com.intek.wpma.helpers

import com.intek.wpma.sql.SQL1S
import java.text.SimpleDateFormat
import java.util.*

object ConstantsDepot {
    private var SS: SQL1S = SQL1S
    private var UpdateInterval  = 600
    private var FSettingsMOD:String = "0000000000000000000000000000000000000000000000000000000"    //default value
    private var FMainWarehouse:String = SS.getVoidID()
    private var FItemForUnits = SS.getVoidID()

    val OrderControl:Boolean get() {condRefresh(); return (FSettingsMOD.substring(13, 14) != "0")}
    val boxSetOn:Boolean get() { condRefresh(); return (FSettingsMOD.substring(30, 31) != "0") }
    val imageOn:Boolean get() { condRefresh(); return (FSettingsMOD.substring(24, 25) != "0") }
    //отключена
    val stopCorrect:Boolean get() { /*CondRefresh(); return (SS.SettingsMOD.substring(30, 31) == "0")  */ return false }
    val CarsCount:String get() { condRefresh(); return FSettingsMOD.substring(26, 27) }

    val mainWarehouse:String get() {return FMainWarehouse}
    /// Товар для единиц из подчинения которого будет подсасывать новые единицы
    val itemForUnits:String get() {return FItemForUnits}

    /// Штамп последнего обновления данных из конфы
    private var refreshTimestamp:Int = 0
    /// Обновляет значения, только если превышено время хранения
    private fun condRefresh() {

        val sdf = SimpleDateFormat("yyyyMMdd HH:mm:ss", Locale.US)
        val currentTime = SQL1S.timeStrToSeconds(sdf.format(Date()).substring(9, 17))

        if ((currentTime - refreshTimestamp) > UpdateInterval)
        {
            refresh(false)
        }
    }
    init {
        UpdateInterval = 600 //Раз в 10 минут
        FSettingsMOD = "0000000000000000000000000000000000000000000000000000000"    //default value
        refresh()
    }
    /// Обновляет данные, сосет из базы (все данные обновляются)
    fun refresh() {
        refresh(true)
    }
    /// непосредственно сосет из базы
    private fun refresh(RefreshAll:Boolean) {

        //Настройки обмена МОД
        var textQuery = "SELECT VALUE as val FROM _1sconst (nolock) WHERE ID = \$Константа.НастройкиОбменаМОД "
        var dt = SS.executeWithReadNew(textQuery)
        if (dt == null || dt.isEmpty())
        {
            return
        }
        val sdf = SimpleDateFormat("yyyyMMdd HH:mm:ss", Locale.US)
        val currentTime = SQL1S.timeStrToSeconds(sdf.format(Date()).substring(9, 17))

        refreshTimestamp =currentTime
        FSettingsMOD = dt[0]["val"].toString()

        //Эти обновляются только в принудиловку
        if (RefreshAll)
        {
            //а тут подсасываем констатну главного склада
            textQuery = "SELECT VALUE as val FROM _1sconst (nolock) WHERE ID = \$Константа.ОснСклад "
            dt.clear()
            dt = SS.executeWithReadNew(textQuery)
            if (dt == null || dt.isEmpty())
            {
                return
            }
            FMainWarehouse = dt[0]["val"].toString()

            //тут подсасываем константу товар для единиц
            textQuery = "SELECT VALUE as val FROM _1sconst (nolock) WHERE ID = \$Константа.ТоварДляЕдиниц "
            dt.clear()
            dt = SS.executeWithReadNew(textQuery)
            if (dt == null || dt.isEmpty())
            {
                return
            }
            FItemForUnits = dt[0]["val"].toString()
        }
    }
}