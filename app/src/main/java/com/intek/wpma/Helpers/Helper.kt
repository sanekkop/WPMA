package com.intek.wpma.Helpers

import java.math.BigInteger
import java.text.SimpleDateFormat
import java.util.*

class Helper {

    fun SuckDigits(Str:String):String    {
        val Numbers  = "01234567890"
        var result   = "";
        var str = Str
        while (str.length > 0)
        {
            if (Numbers.indexOf(str.substring(0, 1)) != -1)
            {
                result += str.substring(0, 1);
            }
            str = str.substring(1);
        }
        return result
    }

    fun GetShortFIO(FIO:String):String    {
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

    fun GetIDD(Barcode:String):String    {
        var IDD = ""
        if (Barcode.length == 18)
            IDD = "9999" + Barcode.substring(5, 18)
        else //13 symbols
            IDD = "99990" + Barcode.substring(2, 4) + "00" + Barcode.substring(4, 12)

        return IDD;
    }

    fun DisassembleBarcode (Barcode: String): MutableMap<String,String>    {
        var bigInteger: BigInteger // для перевода в др сс
        var result: MutableMap<String,String>  = mutableMapOf()
        result["Type"]  = "999"    //Код ошибки
        if (Barcode.length == 18)
        {
            result["Type"]  = "118"
            result["IDD"]   = GetIDD(Barcode)
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
                result["IDD"] = GetIDD(Barcode)
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

    fun ControlSymbolEAN(strBarcode:String):String    {
        var even = 0;
        var odd = 0;
        for (i in 0..5)
        {
            even += strBarcode.substring(2 * i + 1, 2 * i + 2).toInt()
            odd += strBarcode.substring(2 * i, 2 * i +1).toInt()
        }
        return ((10 - (even * 3 + odd) % 10) % 10).toString()
    }

    fun Pause(millisecond:Long)    {
        try {
            Thread.sleep(millisecond) //Приостанавливает поток
        }
        catch (e: java.lang.Exception) {
        }
    }

    fun StringToList(SourceStr: String, separator: String): MutableList<String>    {
        var SourceStr = SourceStr.replace(" ", "")
        var result: MutableList<String>
        result = mutableListOf()
        while (true)
        {
            var index: Int = SourceStr.indexOf(separator)
            index = if (index == -1) {
                0
            } else {
                index
            }

            val thispart: String = SourceStr.substring(0, index)
            if (thispart.isNotEmpty())
            {
                result.add(thispart)
            }
            if (index > 0)
            {
                SourceStr = SourceStr.substring(index + separator.length)
            }
            else
            {
                break
            }
        }
        if (SourceStr.isNotEmpty())
        {
            result.add(SourceStr)
        }
        return result
    }

    fun StringToList(SourceStr: String): MutableList<String>    {
        return StringToList(SourceStr, ",")
    }

    fun ListToStringWithQuotes(SourceList: MutableList<String>): String    {
        var result = ""
        for (element in SourceList)
        {
            result += ", '$element'"
        }
        result = result.substring(2)  //Убираем спедери запятые
        return result
    }

    fun WhatInt(keyCode:Int):Int    {
        when (keyCode)
        {
            //если нажата просто цифра
            7 -> return 0
            8 -> return 1
            9 -> return 2
            10 -> return 3
            11 -> return 4
            12 -> return 5
            13 -> return 6
            14 -> return 7
            15 -> return 8
            16 -> return 9
            //если нажата цифра с шифтом


        }
        return -1;
    }

    fun WhatDirection(keyCode:Int):String    {
        when (keyCode)
        {
            //если нажата просто цифра
            21 -> return "Left"
            19 -> return "Up"
            20 -> return "Down"
            22 -> return "Right"
        }
        return "null"
    }

    fun ReverseString(s:String):String    {
        return s.reversed();
    }

    fun GetPictureFileName(InvCode:String):String    {
        var invcode = InvCode.toLowerCase()
        var result = ""
        for ( i in 0.. invcode.length)
        {
            val symbol = invcode.substring(i, i+1)
            when (symbol)
            {
                "й" -> result += "iy"
                "ц" -> result += "cc"
                "у" -> result += "u"
                "к" -> result += "k"
                "е" -> result += "e"
                "н" -> result += "n"
                "г" -> result += "g"
                "ш" -> result += "h"
                "щ" -> result += "dg"
                "з" -> result += "z"
                "х" -> result += "x"
                "ъ" -> result += "dl"
                "ф" -> result += "f"
                "ы" -> result += "y"
                "в" -> result += "v"
                "а" -> result += "a"
                "п" -> result += "p"
                "р" -> result += "r"
                "о" -> result += "o"
                "л" -> result += "l"
                "д" -> result += "d"
                "ж" -> result += "j"
                "э" -> result += "w"
                "я" -> result += "ya"
                "ч" -> result += "ch"
                "с" -> result += "s"
                "м" -> result += "m"
                "и" -> result += "i"
                "т" -> result += "t"
                "ь" -> result += "zz"
                "б" -> result += "b"
                "ю" -> result += "q"
                "\\" -> result += "ls"
                "/" -> result += "ps"
                "1" -> result += "1"
                "2" -> result += "2"
                "3" -> result += "3"
                "4" -> result += "4"
                "5" -> result += "5"
                "6" -> result += "6"
                "7" -> result += "7"
                "8" -> result += "8"
                "9" -> result += "9"
                "0" -> result += "0"
            }
        }
        return result + ".gif"
    }

    fun IsGreenKey(Key:Int):Boolean    {
        return (Key == 1 || Key == 2 || Key == 3 || Key == 0)
    }

}