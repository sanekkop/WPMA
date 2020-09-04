package com.intek.wpma

class Global {

    enum class Mode{
        Main, None, Waiting,Set, SetInicialization, SetComplete, ChoiseDown, NewComplectation, NewComplectationComplete, ShowRoute, Down,DownComplete, FreeDownComplete
    }

    enum class ActionSet{
        ScanAdress, ScanItem, EnterCount, ScanPart, ScanBox, ScanPallete, Waiting, ScanQRCode
    }
}