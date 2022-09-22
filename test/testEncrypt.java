
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
        String lsValue = Tokenize.EncryptAuthToken("M00116002211", "09175243089", "1", "1");
        System.out.println(lsValue);
        
        
        if ("33453434393631353036393533384643444241444535303334453432374633423146444638374239354338333337314436344237354343423532443245323836".equals(lsValue)){
            System.out.println("tama");
        } else{
            System.out.println("mali");
        }
    }
}
