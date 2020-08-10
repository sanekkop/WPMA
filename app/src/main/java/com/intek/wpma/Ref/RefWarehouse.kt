package com.intek.wpma.Ref

class RefWarehouse : ARef() {
    override val typeObj: String get() = "Склады"

    init {
        haveName    = true
        haveCode    = true
    }

}