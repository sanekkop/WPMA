package com.intek.wpma

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.Result
import com.intek.wpma.ChoiseWork.Set.Correct
import com.intek.wpma.ChoiseWork.Set.SetInitialization
import com.intek.wpma.ChoiseWork.Set.SetComplete
import com.intek.wpma.ChoiseWork.Shipping.Downing
import com.intek.wpma.ChoiseWork.Shipping.FreeComplectation
import com.intek.wpma.ChoiseWork.Shipping.Loading
import com.intek.wpma.ChoiseWork.Shipping.UnLoading
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
        // Register ourselves as a handler for scan results.
        mScannerView!!.setResultHandler(this)
        // Start camera on resume
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
        var codeId: String = ""
        when (rawResult.barcodeFormat.toString()){
            "DATA_MATRIX"   -> codeId = "w"
            "CODE_128"      -> codeId = "j"
            "EAN_13"        -> codeId = "d"
            "UPC_E"         -> codeId = "D"
            "QR_CODE"       -> codeId = "s"
        }

        when(ParentForm){
            "MainActivity"      -> {MainActivity.scanRes = rawResult.text
                                    MainActivity.scanCodeId = codeId}
            "SetInitialization" -> {SetInitialization.scanRes = rawResult.text
                                    SetInitialization.scanCodeId = codeId}
            "Correct"           -> {Correct.scanRes = rawResult.text
                                    Correct.scanCodeId = codeId}
            "SetComplete"       -> {SetComplete.scanRes = rawResult.text
                                    SetComplete.scanCodeId = codeId}
            "Downing"           -> {Downing.scanRes = rawResult.text
                                    Downing.scanCodeId = codeId}
            "UnLoading"         -> {UnLoading.scanRes = rawResult.text
                                    UnLoading.scanCodeId = codeId}
            "Loading"           -> {Loading.scanRes = rawResult.text
                                    Loading.scanCodeId = codeId}
            "FreeComplectation" -> {FreeComplectation.scanRes = rawResult.text
                                    FreeComplectation.scanCodeId = codeId}
        }


        onBackPressed()

        // If you would like to resume scanning, call this method below:
        //mScannerView.resumeCameraPreview(this);
    }
}