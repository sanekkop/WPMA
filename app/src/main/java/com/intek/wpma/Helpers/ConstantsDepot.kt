package com.intek.wpma.Helpers

import com.intek.wpma.SQL.SQL1S
import java.text.SimpleDateFormat
import java.util.*


object ConstantsDepot {
    private var SS: SQL1S = SQL1S
    private var UpdateInterval  = 600
    private var FSettingsMOD:String = "0000000000000000000000000000000000000000000000000000000"    //default value
    private var FMainWarehouse:String = SS.GetVoidID()
    private var FItemForUnits = SS.GetVoidID()

    val OrderControl:Boolean get() {CondRefresh(); return (FSettingsMOD.substring(13, 14) == "0")}
    val BoxSetOn:Boolean get() { CondRefresh(); return (FSettingsMOD.substring(30, 31) == "0") }
    val ImageOn:Boolean get() { CondRefresh(); return (FSettingsMOD.substring(24, 25) == "0") }
    //отключена
    val StopCorrect:Boolean get() { /*CondRefresh(); return (SS.SettingsMOD.substring(30, 31) == "0")  */ return false }
    val CarsCount:String get() { CondRefresh(); return FSettingsMOD.substring(26, 27) }

    var MainWarehouse:String = FMainWarehouse
    /// Товар для единиц из подчинения которого будет подсасывать новые единицы
    var ItemForUnits =  FItemForUnits

    /// Штамп последнего обновления данных из конфы
    var RefreshTimestamp:Int = 0
    /// Обновляет значения, только если превышено время хранения
    fun CondRefresh()
    {

        val sdf = SimpleDateFormat("yyyyMMdd HH:mm:ss")
        val currentTime = SQL1S.timeStrToSeconds(sdf.format(Date()).substring(9, 17))

        if ((currentTime - RefreshTimestamp) > UpdateInterval)
        {
            Refresh(false)
        }
    }
    fun ConstantsDepot()
    {
        UpdateInterval = 600; //Раз в 10 минут
        FSettingsMOD = "0000000000000000000000000000000000000000000000000000000";    //default value
        Refresh();
    }
    /// Обновляет данные, сосет из базы (все данные обновляются)
    fun Refresh()
    {
        Refresh(true);
    }
    /// непосредственно сосет из базы
    fun Refresh(RefreshAll:Boolean)
    {

        //Настройки обмена МОД
        var textQuery = "SELECT VALUE as val FROM _1sconst (nolock) WHERE ID = \$Константа.НастройкиОбменаМОД ";
        var DT = SS.ExecuteWithReadNew(textQuery)
        if (DT == null || DT.isEmpty())
        {
            return
        }
        val sdf = SimpleDateFormat("yyyyMMdd HH:mm:ss")
        val currentTime = SQL1S.timeStrToSeconds(sdf.format(Date()).substring(9, 17))

        RefreshTimestamp =currentTime
        FSettingsMOD = DT[0]["val"].toString()

        //Эти обновляются только в принудиловку
        if (RefreshAll)
        {
            //а тут подсасываем констатну главного склада
            textQuery = "SELECT VALUE as val FROM _1sconst (nolock) WHERE ID = \$Константа.ОснСклад ";
            DT.clear()
            DT = SS.ExecuteWithReadNew(textQuery)
            if (DT == null || DT.isEmpty())
            {
                return
            }
            FMainWarehouse = DT[0]["val"].toString()

            //тут подсасываем константу товар для единиц
            textQuery = "SELECT VALUE as val FROM _1sconst (nolock) WHERE ID = \$Константа.ТоварДляЕдиниц "
            DT.clear()
            DT = SS.ExecuteWithReadNew(textQuery)
            if (DT == null || DT.isEmpty())
            {
                return
            }
            FItemForUnits = DT[0]["val"].toString();
        }
    }
}