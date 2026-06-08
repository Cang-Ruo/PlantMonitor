package com.example.mygraduationproject.utils;

import android.util.Base64;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/** Base64缂栬В鐮佸伐鍏凤細鏂囦欢缂栬В鐮併€乁RL瀹夊叏缂栬В鐮併€丮D5鎽樿 */
public class Base64Utils {
    
    private static final int BUFFER_SIZE = 8192;
    
    public static String encodeFileToBase64(File file) throws IOException {
        if (file == null || !file.exists()) {
            throw new IOException("File does not exist");
        }
        
        InputStream inputStream = null;
        ByteArrayOutputStream outputStream = null;
        try {
            inputStream = new BufferedInputStream(new FileInputStream(file), BUFFER_SIZE);
            outputStream = new ByteArrayOutputStream();
            
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            
            byte[] bytes = outputStream.toByteArray();
            return Base64.encodeToString(bytes, Base64.NO_WRAP);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {}
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException ignored) {}
            }
        }
    }
    
    public static String encodeBytesToBase64(byte[] bytes) {
        if (bytes == null) return "";
        return Base64.encodeToString(bytes, Base64.NO_WRAP);
    }
    
    public static byte[] decodeBase64ToBytes(String base64String) {
        if (base64String == null) return new byte[0];
        return Base64.decode(base64String, Base64.NO_WRAP);
    }
    
    public static String encodeUrlSafe(String input) {
        if (input == null) return "";
        return Base64.encodeToString(input.getBytes(), Base64.URL_SAFE | Base64.NO_WRAP);
    }
    
    public static String decodeUrlSafe(String encoded) {
        if (encoded == null) return "";
        return new String(Base64.decode(encoded, Base64.URL_SAFE | Base64.NO_WRAP));
    }
    
    public static String md5(String input) {
        if (input == null) return "";
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            return "";
        }
    }
}
