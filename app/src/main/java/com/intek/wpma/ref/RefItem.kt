package com.intek.wpma.ref

class RefItem : ARef() {
    override val typeObj: String get() = "Товары"

    val pricePurchase:Double get() {return getAttribute("Прих_Цена").toString().toDouble()}
    val price:Double get() {return getAttribute("Опт_Цена").toString().toDouble()}
    val invCode:String get() {return getAttribute("ИнвКод").toString().trim()}
    val details:Int get() {return getAttribute("КоличествоДеталей").toString().toInt()}
    val zonaHand:RefGates get() {return getGatesProperty("ЗонаР")}
    val zonaTech:RefGates get() {return getGatesProperty("ЗонаТ")}

    fun foundBarcode(Barcode:String):Boolean    {
        var textQuery = "select top 1 PARENTEXT as ID from \$Спр.ЕдиницыШК (nolock) where \$Спр.ЕдиницыШК.Штрихкод = :barcode"
        textQuery = ss.querySetParam(textQuery, "barcode", Barcode)
        val dt  = ss.executeWithReadNew(textQuery) ?: return false
        if (dt.isEmpty())
        {
            return false
        }

        fID = dt[0]["ID"].toString()
        fName = ""
        refresh()
        return fName != ""
    } // FoundBarcode

    init {
        haveName    = true
        haveCode    = true
    }

}