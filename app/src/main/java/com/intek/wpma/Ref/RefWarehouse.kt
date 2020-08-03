package com.intek.wpma.Ref

class RefWarehouse(): ARef() {
    override val TypeObj: String get() = "Склады"

    init {
        HaveName    = true
        HaveCode    = true
    }

}