
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
        String fsToken = Tokenize.DecryptToken("35304144353033423430424536303839333746333546413435343638413437394337373535374341444432314236443235354343434439384639393232323835", "M00103001137");
        System.out.println(fsToken);
        //M00110018534:09175257115:1:1
    }
}
