package com.intek.wpma.Helpers

import android.os.Build
import androidx.annotation.RequiresApi
import java.math.BigInteger
import java.text.SimpleDateFormat
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor
import java.util.*

class Helper {
    /*
    /// <summary>
    ///
    /// </summary>
    /// <param name="str"></param>
    /// <returns></returns>
    static public string SuckDigits(string str)
    {
        string Numbers  = "01234567890";
        string result   = "";
        while (str.Length > 0)
        {
            if (Numbers.IndexOf(str.Substring(0, 1)) != -1)
            {
                result += str.Substring(0, 1);
            }
            str = str.Substring(1);
        }
        return result;
    }
    */
    /// <summary>
    /// делает из полного имени формат типа: Иванов И.И.
    /// </summary>
    /// <param name="FIO"></param>
    /// <returns></returns>
    fun GetShortFIO(FIO:String):String
    {
        var result = ""
        var fio = FIO.trim()
        var space = false
        var surname = false
        for (i in 0..fio.length-1)
        {
            var ch = fio.substring(i, i+1);
            if (!surname)
            {
                result += ch
            }
            if (space)
            {
                result += ch + "."
            }
            surname = if (ch == " ") true else surname
            space = ch == " "
        }
        return result;
    }
/*
    /// <summary>
    ///
    /// </summary>
    /// <param name="Barcode"></param>
    /// <returns></returns>
    static public string GetIDD(string Barcode)
    {
        string IDD = "";

        if (Barcode.Length == 18)
            IDD = "9999" + Barcode.Substring(5, 13);
        else //13 symbols
            IDD = "99990" + Barcode.Substring(2, 2) + "00" + Barcode.Substring(4, 8);

        return IDD;
    }
    */
    /// <summary>
    ///
    /// </summary>
    /// <param name="Barcode"></param>
    /// <returns></returns>
    fun DisassembleBarcode (Barcode: String): MutableMap<String,String>
    {
        var bigInteger: BigInteger // для перевода в др сс
        var result: MutableMap<String,String>  = mutableMapOf()
        result["Type"]  = "999"    //Код ошибки
        if (Barcode.length == 18)
        {
            result["Type"]  = "118"
            result["IDD"]   = "9999" + Barcode.substring(5, 18)
        }
        else if (Barcode.length == 13)
        {
            if (Barcode.substring(0, 4) == "2599")
            {
                result["Type"] = "6"
                //В следующих 8 разрядах - закодированный ИД справочника
                bigInteger = Barcode.substring(4, 12).toBigInteger()
                var encodedID: String   = bigInteger.toString(36)
                encodedID = "      $encodedID"
                encodedID = encodedID.substring(encodedID.length - 6) + "   "
                result["ID"] = encodedID.toUpperCase()
            }
            else if (Barcode.substring(0, 9) == "259000000")
            {
                result["Type"]  = "part"
                result["count"] = Barcode.substring(9, 12)
            }
            else if (Barcode.substring(0, 4) == "2580")
            {
                result["Type"] = "pallete"
                result["number"] = Barcode.substring(4, 12)
            }
            else
            {
                result["Type"] = "113"
                result["IDD"] = "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12)
            }
        }
        else
        {
            //128-Code (поехали, будем образать задние разряды полсе их обработки
            try{
            bigInteger = Barcode.toBigInteger()
            var firstChange: String   = bigInteger.toString(10)
            var binaryBar: String = "00000" + firstChange.toBigInteger().toString(2)
            //Последние пять разрядов - тип
            val type: String = binaryBar.substring(binaryBar.length - 6).toBigInteger().toString(10).toString()
            if (type == "5")
            {
                result["Type"]      = type
                //В следующие 34 разряда - закодированный ИДД документа
                binaryBar              = binaryBar.substring(0, binaryBar.length - 6)
                var encodedIDD: String = binaryBar.substring(binaryBar.length - 34).toBigInteger().toString(10).toString()
                encodedIDD          = "000000000$encodedIDD"
                encodedIDD          = encodedIDD.substring(encodedIDD.length - 10) //получаем 10 правых символов
                result["IDD"]       = "99990" + encodedIDD.substring(0, 2) + "00" + encodedIDD.substring(2)
                //В следующих 20 разрядах кроется строка документа (вот так с запасом взял)
                binaryBar           = "0000000000000000000" + binaryBar.substring(0, binaryBar.length - 34)
                result["LineNo"]    = binaryBar.substring(binaryBar.length - 20).toBigInteger().toString(10).toString()
                }
            }
            catch (e: Exception){
                result["IDD"] = ""
            }
        }
        return result
    }

    /// <summary>
    /// Преобразует число секунд в строку вида ЧЧ:ММ
    /// </summary>
    /// <param name="Sec">Seconds since the beginning of the day</param>
    /// <returns></returns>


     fun timeToString(sec : Any) : String {
         sec as Int
         val Hours = sec / 3600
         val Minutes = (sec -(Hours * 3600)) / 60
         return "$Hours:$Minutes"
     }

    fun ShortDate(dat : Any) : String {

        val datFormat:SimpleDateFormat  = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH)
        val date2 = datFormat.parse(dat.toString())

        return "(" + date2.date.toString() + "." + (date2.month + 1).toString() + ")"
    }

    /*
    /// <summary>
    ///
    /// </summary>
    /// <param name="Barcode"></param>
    /// <returns></returns>
    static public int ControlSymbolEAN(string strBarcode)
    {
        int even = 0;
        int odd = 0;
        for (int i = 0; i < 6; i++)
        {
            even += Convert.ToInt32(strBarcode.Substring(2 * i + 1, 1));
            odd += Convert.ToInt32(strBarcode.Substring(2 * i, 1));
        }
        return (10 - (even * 3 + odd) % 10) % 10;
    }
    /// <summary>
    /// Pause. Empty cycle
    /// </summary>
    /// <param name="millisecond">how long</param>
    static public void Pause(int millisecond)
    {
        int Start = Environment.TickCount;
        while (Environment.TickCount - Start < millisecond)
        {
        }
    }
    /// <summary>
    ///
    /// </summary>
    /// <param name="SourceStr"></param>
    /// <param name="separator"></param>
    /// <returns></returns>
    */
    fun  StringToList(SourceStr: String, separator: String): MutableList<String>
    {
        var sourceStr = SourceStr.replace(" ", "")
        var result: MutableList<String>
        result = emptyList<String>() as MutableList<String>
        while (true)
        {
            var index: Int = sourceStr.indexOf(separator)
            index = if (index == -1) {
                0
            } else {
                index
            }

            val thispart: String = sourceStr.substring(0, index)
            if (thispart.isNotEmpty())
            {
                result.add(thispart)
            }
            if (index > 0)
            {
                sourceStr = sourceStr.substring(index + separator.length)
            }
            else
            {
                break
            }
        }
        if (sourceStr.isNotEmpty())
        {
            result.add(sourceStr)
        }
        return result
    }

    fun StringToList(SourceStr: String): MutableList<String>
    {
        return StringToList(SourceStr, ",")
    }

    fun ListToStringWithQuotes(SourceList: MutableList<String>): String
    {
        var result = ""
        for (element in SourceList)
        {
            result += ", '$element'"
        }
        result = result.substring(2)  //Убираем спедери запятые
        return result
    }
    /*
    static public int WhatInt(Keys Key)
    {
        int result = -1;
        switch (Key)
        {
            case Keys.D0:
            result = 0;
            break;
            case Keys.D1:
            result = 1;
            break;
            case Keys.D2:
            result = 2;
            break;
            case Keys.D3:
            result = 3;
            break;
            case Keys.D4:
            result = 4;
            break;
            case Keys.D5:
            result = 5;
            break;
            case Keys.D6:
            result = 6;
            break;
            case Keys.D7:
            result = 7;
            break;
            case Keys.D8:
            result = 8;
            break;
            case Keys.D9:
            result = 9;
            break;
        }
        return result;
    }
    static public string ReverseString(string s)
    {
        char[] arr = s.ToCharArray();
        Array.Reverse(arr);
        return new string(arr);
    }
    static public string GetPictureFileName(string InvCode)
    {
        InvCode = InvCode.ToLower();
        string result = "";
        for (int i = 0; i < InvCode.Length; i++)
        {
            string symbol = InvCode.Substring(i, 1);
            switch (symbol)
            {
                case "й"
                :result += "iy";break;
                case "ц"
                :result += "cc";break;
                case "у"
                :result += "u";break;
                case "к"
                :result += "k";break;
                case "е"
                :result += "e";break;
                case "н"
                :result += "n";break;
                case "г"
                :result += "g";break;
                case "ш"
                :result += "h";break;
                case "щ"
                :result += "dg";break;
                case "з"
                :result += "z";break;
                case "х"
                :result += "x";break;
                case "ъ"
                :result += "dl";break;
                case "ф"
                :result += "f";break;
                case "ы"
                :result += "y";break;
                case "в"
                :result += "v";break;
                case "а"
                :result += "a";break;
                case "п"
                :result += "p";break;
                case "р"
                :result += "r";break;
                case "о"
                :result += "o";break;
                case "л"
                :result += "l";break;
                case "д"
                :result += "d";break;
                case "ж"
                :result += "j";break;
                case "э"
                :result += "w";break;
                case "я"
                :result += "ya";break;
                case "ч"
                :result += "ch";break;
                case "с"
                :result += "s";break;
                case "м"
                :result += "m";break;
                case "и"
                :result += "i";break;
                case "т"
                :result += "t";break;
                case "ь"
                :result += "zz";break;
                case "б"
                :result += "b";break;
                case "ю"
                :result += "q";break;
                case @"\"
                :result += "ls";break;
                case @"/"
                :result += "ps";break;
                case "1"
                :result += "1";break;
                case "2"
                :result += "2";break;
                case "3"
                :result += "3";break;
                case "4"
                :result += "4";break;
                case "5"
                :result += "5";break;
                case "6"
                :result += "6";break;
                case "7"
                :result += "7";break;
                case "8"
                :result += "8";break;
                case "9"
                :result += "9";break;
                case "0"
                :result += "0";break;
            }
        }
        return result + ".gif";
    }
    static public bool IsGreenKey(Keys Key)
    {
        return (Key == Keys.F14 || Key == Keys.F2 || Key == Keys.F1 || Key.GetHashCode() == 189) ? true : false;
    }
     */
}