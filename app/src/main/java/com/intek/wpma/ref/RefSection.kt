package com.intek.wpma.ref

class RefSection : ARef() {
    override val typeObj: String get() = "Секции"

    val type:Int get() {return if (selected) getAttribute("ТипСекции").toString().toInt() else -1} // Type
    val adressZone:RefGates get() { return getGatesProperty("ЗонаАдресов")} //Зона адреса
    init {
        haveName    = true
        haveCode    = false
    }

}