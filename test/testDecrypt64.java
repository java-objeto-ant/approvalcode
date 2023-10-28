
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

public class testDecrypt64 {
    public static void main (String [] args){
        String pass = "eXlmLytZV3R1dzE1MkwydmIwVXRxZz09";
        String hash = "cfe92580c7aa3557ab49c54007c2e992d7c1ce4d1b51301607809e5721592493";
        
        
        try {
            String decryptedValue = appDecrypt(pass, hash);
            System.out.println("Decrypted Value: " + decryptedValue);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    public static String appDecrypt(String value, String salt) throws Exception {
        String method = "AES/CBC/PKCS5Padding";
        String iv = "0a6575be41b8e2b8";

        byte[] valueBytes = Base64.getDecoder().decode(value);
        byte[] saltBytes = salt.getBytes("UTF-8");
        byte[] ivBytes = iv.getBytes("UTF-8");

        SecretKeySpec secretKeySpec = new SecretKeySpec(saltBytes, "AES");
        Cipher cipher = Cipher.getInstance(method);
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(ivBytes));

        byte[] decryptedBytes = cipher.doFinal(valueBytes);
        return new String(decryptedBytes, "UTF-8");
    }
}
