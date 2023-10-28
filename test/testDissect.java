
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.rmj.appdriver.MiscUtil;
import org.rmj.appdriver.StringHelper;
import org.rmj.appdriver.StringHelperMisc;
import org.rmj.appdriver.StringUtil;

public class testDissect {
    public static void main(String [] args){
        String lsMessagex = "HDEC Cuison, Michael Jr. Torres;M001;36.5;N;N;N;N;N;N;N;N;N;N";
        
        System.out.println(lsMessagex.substring(5));
        
        float lnValue = new Float("5.5.62");
        System.out.println(lnValue);
    }
}
