
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
public class testDecrypt {
    public static void main(String [] args){
        String fsToken = Tokenize.DecryptToken("43323235323635343230414235393735314433363745463443313438453643413036433236313833413434363333313643433341393541443839434439344643", "M00103001137");
        System.out.println(fsToken);
        //M00110018534:09175257115:1:1
    }
}
