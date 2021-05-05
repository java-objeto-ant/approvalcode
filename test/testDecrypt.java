
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
        String fsToken = Tokenize.DecryptToken("34443935443441424246423230363231334536454534393739313537453131363841423745353030334445343839363038434335444135303945444245364646", "M00114000092");
        System.out.println(fsToken);
        //M00110018534:09175257115:1:1
    }
}
