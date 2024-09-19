package es.icp.pistola;

import android.util.Log;

import java.math.BigInteger;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class RfidUtils {



    public static String hexToAscii(String hexStr) {
        StringBuilder output = new StringBuilder("");

        for (int i = 0; i < hexStr.length(); i += 2) {
            String str = hexStr.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }

        return output.toString();
    }



    public  static  String convertFecha(String fromFormat, String toFormat, String dateToFormat){
        SimpleDateFormat inFormat = new SimpleDateFormat(fromFormat);
        Date date = null;
        try {
            date = inFormat.parse(dateToFormat);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        SimpleDateFormat outFormat = new SimpleDateFormat(toFormat);

        return outFormat.format(date);

    }

    public static String LeerEPC(String tagData) {
        String rfid_leido = "";
        try {
            if (tagData.length() == 24) {
                String tipo    = tagData.substring(0, 2);
                String company = tagData.substring(2, 8);
                String nSerie  = tagData.substring(8);

                String tipoString     = convertHexToString(tipo);
                String companyDecimal = convertHexToDecimal(company);
                String companyString  = "" + (char) Integer.parseInt(companyDecimal.substring(0, 2)) + (char) Long.parseLong(companyDecimal.substring(2, 4)) + (char) Long.parseLong(companyDecimal.substring(4));
                String nSerieString   = padLeftZeros(convertHexToDecimal(nSerie), 10);

                rfid_leido = tipoString + companyString + nSerieString;

            } else {
                //  MyLog.c(tagData + "; length: " + tagData.length());
                Log.d("ELSE_LEER_EPC", "Longitud " + tagData.length());
            }
        } catch (Exception ex) {
            // ex.printStackTrace();
            Log.d("CATCH_LEER_EPC", ex.getMessage());
        }

        return rfid_leido;
    }

    public static String convertHexToString(String hex) {
        StringBuilder sb   = new StringBuilder();
        StringBuilder temp = new StringBuilder();
        for (int i = 0; i < hex.length() - 1; i += 2) {
            String output  = hex.substring(i, (i + 2));
            long   decimal = Long.parseLong(output, 16);
            sb.append((char) decimal);
            temp.append(decimal);
        }
        return sb.toString();
    }

    public static String convertHexToDecimal(String hex) {

        return String.valueOf(Long.parseLong(hex, 16));
    }

    public static String padLeftZeros(String inputString, long length) {
        if (inputString.length() >= length) {
            return inputString;
        }
        StringBuilder sb = new StringBuilder();
        while (sb.length() < length - inputString.length()) {
            sb.append('0');
        }
        sb.append(inputString);

        return sb.toString();
    }

    public static ArrayList<Long> decodeEpc(String epc) {
        ArrayList<Long> ret = new ArrayList<>();
        String          bin = hexToBin(epc);
        bin = completeWithZeros(bin, 96);
        String[] bins = new String[6];
        bins[0] = bin.substring(0, 8);
        bins[1] = bin.substring(8, 11);
        bins[2] = bin.substring(11, 14);
        bins[3] = bin.substring(14, 34);
        bins[4] = bin.substring(34, 58);
        bins[5] = bin.substring(58);
        for (String b : bins) ret.add(binToDec(b));
        return ret;
    }

    public static String completeWithZeros(String code, long length) {
        while (code.length() < length) code = "0" + code;
        return code;
    }
    public static long binToDec(String s) {
        return Long.parseLong(s, 2);
    }

    public static String hexToBin(String s) {
        return new BigInteger(s, 16).toString(2);
    }
}
