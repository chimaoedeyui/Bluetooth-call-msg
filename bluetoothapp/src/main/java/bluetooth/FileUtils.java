package bluetooth;

import android.util.Log;

/**
 * Created by jk on 2015/10/23 0023.
 */
public class FileUtils {



    //java 合并两个byte数组
    public static byte[] byteMerger(byte[] byte_1, byte[] byte_2){
        if(byte_1==null)
            return byte_2;
        if(byte_2==null){
            return byte_1;
        }
        byte[] byte_3 = new byte[byte_1.length+byte_2.length];
        System.arraycopy(byte_1, 0, byte_3, 0, byte_1.length);
        System.arraycopy(byte_2, 0, byte_3, byte_1.length, byte_2.length);
        return byte_3;
    }

    //java 合并两个byte数组
    public static int[] Byte2Int(byte[] b1){
        if(b1==null)
            return null;

        int[] i = new int[b1.length];
        for(int j=0;j<b1.length;j++){
            i[j]=(int) b1[j];
            Log.i("lzk", "i:" + (i[j]&0xff));
        }
        return i;
    }

}
