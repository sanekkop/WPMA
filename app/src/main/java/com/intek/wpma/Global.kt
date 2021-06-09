package com.intek.wpma

class Global {

    enum class Mode {
        Main, None, Waiting, Set, SetInitialization, SetComplete, SetCorrect, ChoiseDown, NewComplectation, NewComplectationComplete, ShowRoute, Down, DownComplete, FreeDownComplete, Acceptance, AcceptanceItem,
        AcceptanceNotAccepted, AcceptanceAccepted, TransferMode, TransferInit, TransferYep, TransferRefresh
    }

    enum class ActionSet {
        ScanAddress, ScanItem, EnterCount, ScanPart, ScanBox, ScanPallet, Waiting, ScanQRCode
    }
}