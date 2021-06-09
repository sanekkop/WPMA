package com.intek.wpma.ref

class RefWarehouse : ARef() {
    override val typeObj: String get() = "Склады"

    init {
        haveName    = true
        haveCode    = true
    }

}