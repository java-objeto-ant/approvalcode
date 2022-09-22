
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
        String fsToken = Tokenize.DecryptToken("33453434393631353036393533384643444241444535303334453432374633423146444638374239354338333337314436344237354343423532443245323836", "M00116002211");
        System.out.println(fsToken);
        //M00110018534:09175257115:1:1
    }
}
