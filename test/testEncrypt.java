
import org.rmj.appdriver.Tokenize;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author mac
 */
public class testEncrypt {
    public static void main(String [] args){
        String lsValue = Tokenize.EncryptAuthToken("M00119002175", "09178911333", "1", "1");
        System.out.println(lsValue);
    }
}
