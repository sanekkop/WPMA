package com.intek.wpma.Ref

import com.intek.wpma.SQL.SQL1S
import java.math.BigInteger


class RefEmployer(): ARef() {
    override val TypeObj: String get() = "Сотрудники"
    var settings: String = ""

    val CanLoad: Boolean get() {GetDataEmployer(); return (settings.substring(22,23) == "1")}
    val SelfControl: Boolean get() {GetDataEmployer(); return (settings.substring(20, 21) == "1")}
    val CanRoute: Boolean get() {GetDataEmployer(); return (settings.substring(19, 20) == "0")}
    val CanHarmonization: Boolean get() {GetDataEmployer(); return (settings.substring(17, 18) == "1")}
    val CanSupply: Boolean get() {GetDataEmployer(); return (settings.substring(14, 15) == "1")}
    val CanCellInventory: Boolean get() {GetDataEmployer(); return (settings.substring(13, 14) == "1")}
    val CanDiffParty: Boolean get() {GetDataEmployer(); return (settings.substring(12, 13) == "1")}
    val CanAcceptance: Boolean get() {GetDataEmployer(); return (settings.substring(11, 12) == "1")}
    val CanTransfer: Boolean get() {GetDataEmployer(); return (settings.substring(10, 11) == "1")}
    val CanMultiadress: Boolean get() {GetDataEmployer(); return (settings.substring(9, 10) == "1")}
    val CanGiveSample: Boolean get() {GetDataEmployer(); return (settings.substring(7, 8) == "1")}
    val CanLayOutSample: Boolean get() {GetDataEmployer(); return (settings.substring(6, 7) == "1")}
    val CanInventory: Boolean get() {GetDataEmployer(); return (settings.substring(5, 6) == "1")}
    val CanComplectation: Boolean get() {GetDataEmployer(); return (settings.substring(4, 5) == "1")}
    val CanSet: Boolean get() {GetDataEmployer(); return (settings.substring(1, 2) == "1")}
    val CanDown: Boolean get() {GetDataEmployer(); return (settings.substring(0, 1) == "1")}
    val IDD:String get() {return GetAttribute("IDD").toString()}

    /*

    /// <summary>
        /// "Родной склад" сотрудника
        /// </summary>
        public RefWarehouse Warehouse
        {
            get
            {
                if (!Selected)
                {
                    return new RefWarehouse(SS);
                }
                string TextQuery = "select dbo.WPM_fn_GetNativeWarehouse(:employer)";
                SQL1S.QuerySetParam(ref TextQuery, "employer", ID);
                RefWarehouse result = new RefWarehouse(SS);
                result.FoundID(SS.ExecuteScalar(TextQuery).ToString());
                return result;
            }
        } // Warehouse

    */
    init {
        HaveName    = true
        HaveCode    = true
    }


    private fun GetDataEmployer(): Boolean {
        var result: Boolean = false
        settings = "000000000000000000000000000000"
        val bigInteger: BigInteger = GetAttribute("Настройки").toString().toBigInteger()
        //settings += Translation.DecTo2((long)(decimal)DataMap["Спр.Сотрудники.Настройки"])
        settings += bigInteger.toString(2)
        result = true
        settings = settings.substring(settings.length - 23)    //23 правых символов
        //settings = Helper.ReverseString(settings)              //Отразим, чтобы было удобнее добавлять новые флажки
        settings = settings.reversed() // должен отразить, проверить
        return result
    }
    override fun Refresh()
    {
        super.Refresh()
        settings = ""
        GetDataEmployer()
    }

}