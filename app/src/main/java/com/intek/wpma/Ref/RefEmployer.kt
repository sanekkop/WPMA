package com.intek.wpma.Ref

import java.math.BigInteger


class RefEmployer : ARef() {
    override val typeObj: String get() = "Сотрудники"
    private var settings: String = ""

    val canLoad: Boolean get() {getDataEmployer(); return (settings.substring(22,23) == "1")}
    val selfControl: Boolean get() {getDataEmployer(); return (settings.substring(20, 21) == "1")}
    val canRoute: Boolean get() {getDataEmployer(); return (settings.substring(19, 20) == "0")}
    val canHarmonization: Boolean get() {getDataEmployer(); return (settings.substring(17, 18) == "1")}
    val canSupply: Boolean get() {getDataEmployer(); return (settings.substring(14, 15) == "1")}
    val canCellInventory: Boolean get() {getDataEmployer(); return (settings.substring(13, 14) == "1")}
    val canDiffParty: Boolean get() {getDataEmployer(); return (settings.substring(12, 13) == "1")}
    val canAcceptance: Boolean get() {getDataEmployer(); return (settings.substring(11, 12) == "1")}
    val canTransfer: Boolean get() {getDataEmployer(); return (settings.substring(10, 11) == "1")}
    val canMultiadress: Boolean get() {getDataEmployer(); return (settings.substring(9, 10) == "1")}
    val canGiveSample: Boolean get() {getDataEmployer(); return (settings.substring(7, 8) == "1")}
    val canLayOutSample: Boolean get() {getDataEmployer(); return (settings.substring(6, 7) == "1")}
    val canInventory: Boolean get() {getDataEmployer(); return (settings.substring(5, 6) == "1")}
    val canComplectation: Boolean get() {getDataEmployer(); return (settings.substring(4, 5) == "1")}
    val canSet: Boolean get() {getDataEmployer(); return (settings.substring(1, 2) == "1")}
    val canDown: Boolean get() {getDataEmployer(); return (settings.substring(0, 1) == "1")}
    val idd:String get() {return getAttribute("IDD").toString()}

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
        haveName    = true
        haveCode    = true
    }


    private fun getDataEmployer(): Boolean {
        var result = false
        settings = "000000000000000000000000000000"
        val bigInteger: BigInteger = getAttribute("Настройки").toString().toBigInteger()
        //settings += Translation.DecTo2((long)(decimal)DataMap["Спр.Сотрудники.Настройки"])
        settings += bigInteger.toString(2)
        result = true
        settings = settings.substring(settings.length - 23)    //23 правых символов
        //settings = Helper.ReverseString(settings)              //Отразим, чтобы было удобнее добавлять новые флажки
        settings = settings.reversed() // должен отразить, проверить
        return result
    }
    override fun refresh()
    {
        super.refresh()
        settings = ""
        getDataEmployer()
    }

}