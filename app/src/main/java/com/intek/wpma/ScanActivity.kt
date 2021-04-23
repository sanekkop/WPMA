package com.intek.wpma

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.Result
import com.intek.wpma.ChoiseWork.Accept.Search
import com.intek.wpma.ChoiseWork.Revise.ReviseMark
import com.intek.wpma.ChoiseWork.Set.Correct
import com.intek.wpma.ChoiseWork.Set.SetComplete
import com.intek.wpma.ChoiseWork.Set.SetInitialization
import com.intek.wpma.ChoiseWork.Shipping.*
import com.intek.wpma.SQL.SQL1S
import me.dm7.barcodescanner.zxing.ZXingScannerView

var ParentForm: String = ""

class ScanActivity: AppCompatActivity(), ZXingScannerView.ResultHandler {
    private var mScannerView: ZXingScannerView? = null

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        // Programmatically initialize the scanner view
        mScannerView = ZXingScannerView(this)
        // Set the scanner view as the content view
        setContentView(mScannerView)
        ParentForm = intent.extras!!.getString("ParentForm")!!
    }

    override fun onResume() {
        super.onResume()
        onWindowFocusChanged(true)
        // Register ourselves as a handler for scan results.
        mScannerView!!.setResultHandler(this)
        // Start camera on resume
        mScannerView!!.flash = true
        mScannerView!!.startCamera()

    }

    override fun onPause() {
        super.onPause()
        // Stop camera on pause
        mScannerView!!.stopCamera()
    }

    override fun handleResult(rawResult: Result) { // Do something with the result here
        // Do something with the result here
        // Log.v("tag", rawResult.getText()); // Prints scan results
        // Log.v("tag", rawResult.getBarcodeFormat().toString()); // Prints the scan format (qrcode, pdf417 etc.)
        var codeId = ""
        when (rawResult.barcodeFormat.toString()){
            "DATA_MATRIX"   -> codeId = "w"
            "CODE_128"      -> codeId = "j"
            "EAN_13"        -> codeId = "d"
            "UPC_E"         -> codeId = "D"
            "QR_CODE"       -> codeId = "s"
        }
        SQL1S.CurrentMode
        when(ParentForm) {
            "MainActivity" -> {
                MainActivity.scanRes = rawResult.text
                MainActivity.scanCodeId = codeId
            }
            "SetInitialization" -> {
                SetInitialization.scanRes = rawResult.text
                SetInitialization.scanCodeId = codeId
            }
            "Correct" -> {
                Correct.scanRes = rawResult.text
                Correct.scanCodeId = codeId
            }
            "SetComplete" -> {
                SetComplete.scanRes = rawResult.text
                SetComplete.scanCodeId = codeId
            }
            "Downing" -> {
                Downing.scanRes = rawResult.text
                Downing.scanCodeId = codeId
            }
            "UnLoading" -> {
                UnLoading.scanRes = rawResult.text
                UnLoading.scanCodeId = codeId
            }
            "Loading" -> {
                Loading.scanRes = rawResult.text
                Loading.scanCodeId = codeId
            }
            "ChiseDown" -> {
                ChoiseDown.scanRes = rawResult.text
                ChoiseDown.scanCodeId = codeId
            }
            "FreeComplectation" -> {
                FreeComplectation.scanRes = rawResult.text
                FreeComplectation.scanCodeId = codeId
            }
            "NewComplectation" -> {
                NewComplectation.scanRes = rawResult.text
                NewComplectation.scanCodeId = codeId
            }
            "Search" -> {
                Search.scanRes = rawResult.text
                Search.scanCodeId = codeId
            }
            "YapItem" -> {
                Search.scanRes = rawResult.text
                Search.scanCodeId = codeId
            }
            "NoneItem" -> {
                Search.scanRes = rawResult.text
                Search.scanCodeId = codeId
            }
            "ReviseMark" -> {
                ReviseMark.scanRes = rawResult.text
                ReviseMark.scanCodeId = codeId
            }
            "ShowRoute" -> {
                NewComplectation.scanRes = rawResult.text
                NewComplectation.scanCodeId = codeId
            }
        }

        onBackPressed()

        // If you would like to resume scanning, call this method below:
        //mScannerView.resumeCameraPreview(this);
    }
}