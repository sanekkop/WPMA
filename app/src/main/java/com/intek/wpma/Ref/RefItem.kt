package com.intek.wpma.Ref

class RefItem(): ARef() {
    override val TypeObj: String get() = "Товары"

    val PricePurchase:Double get() {return GetAttribute("Прих_Цена").toString().toDouble()}
    val Price:Double get() {return GetAttribute("Опт_Цена").toString().toDouble()}
    val InvCode:String get() {return GetAttribute("ИнвКод").toString().trim()}
    val Details:Int get() {return GetAttribute("КоличествоДеталей").toString().toInt()}
    val ZonaHand:RefGates get() {return GetGatesProperty("ЗонаР")}
    val ZonaTech:RefGates get() {return GetGatesProperty("ЗонаТ")}

    fun FoundBarcode(Barcode:String):Boolean    {
        var textQuery = "select top 1 PARENTEXT as ID from \$Спр.ЕдиницыШК (nolock) where \$Спр.ЕдиницыШК.Штрихкод = :barcode"
        textQuery = SS.QuerySetParam(textQuery, "barcode", Barcode)
        val DT  = SS.ExecuteWithReadNew(textQuery) ?: return false
        if (DT.isEmpty())
        {
            return false
        }

        FID = DT[0]["ID"].toString()
        FName = "";
        Refresh();
        return FName != ""
    } // FoundBarcode

    init {
        HaveName    = true
        HaveCode    = true
    }

}