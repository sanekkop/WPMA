package com.intek.wpma.Ref

class RefPrinter() : ARef() {
    override val TypeObj: String get() = "Принтеры"

    val Path:String get() {return if(Selected) GetAttribute("Путь").toString() else "" }
    val PrinterType:Int get() {return if(Selected) GetAttribute("ТипПринтера").toString().toInt() else -1}
    val Description:String get() { return if(Selected) (Path.trim() + " " + (if(PrinterType == 1) "этикеток" else "обычный")) else "(принтер не выбран)"}
    init {
        HaveName    = true
        HaveCode    = true
    }
}