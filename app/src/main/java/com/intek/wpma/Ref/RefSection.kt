package com.intek.wpma.Ref

import com.intek.wpma.SQL.SQL1S



class RefSection(): ARef() {
    override val TypeObj: String get() = "Секции"

    val Type:Int get() {return if (Selected) GetAttribute("ТипСекции").toString().toInt() else -1} // Type
    val AdressZone:RefGates get() { return GetGatesProperty("ЗонаАдресов")} //Зона адреса
    init {
        HaveName    = true
        HaveCode    = false
    }

}