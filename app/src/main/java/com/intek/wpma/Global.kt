package com.intek.wpma

class Global {

    enum class Mode {
        Main, None, Waiting, Set, SetInicialization, SetComplete, SetCorrect, ChoiseDown, NewComplectation, NewComplectationComplete, ShowRoute, Down, DownComplete, FreeDownComplete, Acceptance, AcceptanceItem,
        AcceptanceNotAccepted, AcceptanceAccepted
    }

    enum class ActionSet {
        ScanAdress, ScanItem, EnterCount, ScanPart, ScanBox, ScanPallete, Waiting, ScanQRCode
    }
}