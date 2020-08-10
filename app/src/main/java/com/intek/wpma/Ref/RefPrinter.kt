package com.intek.wpma.Ref

class RefPrinter : ARef() {
    override val typeObj: String get() = "Принтеры"

    val path:String get() {return if(selected) getAttribute("Путь").toString() else "" }
    private val printerType:Int get() {return if(selected) getAttribute("ТипПринтера").toString().toInt() else -1}
    val description:String get() { return if(selected) (path.trim() + " " + (if(printerType == 1) "этикеток" else "обычный")) else "(принтер не выбран)"}
    init {
        haveName    = true
        haveCode    = true
    }
}