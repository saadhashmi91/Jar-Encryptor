package com.veon.rafm.simbox.encryption;

import org.apache.commons.cli.*;
import org.apache.commons.io.IOUtils;
import org.mapdb.DB;
import org.mapdb.DBMaker;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.concurrent.ConcurrentMap;

public class JarEncryptor {
    private DB db;

    public JarEncryptor(){
    }

    final static String algorithm = "AES";

  public static  SecretKeySpec getKey(String password) throws Exception{

        byte[] salt = {
                (byte)0xc7, (byte)0x73, (byte)0x21, (byte)0x8c,
                (byte)0x7e, (byte)0xc8, (byte)0xee, (byte)0x99
        };



      ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
      outputStream.write( salt);
      outputStream.write( password.getBytes("UTF-8") );
      byte[] key = outputStream.toByteArray();
      MessageDigest sha = MessageDigest.getInstance("SHA-1");
      key = sha.digest(key);
      key = Arrays.copyOf(key, 16); // use only first 128 bit
      SecretKeySpec secret = new SecretKeySpec(key, "AES");
        return  secret;

      //  Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
       // cipher.init(Cipher.ENCRYPT_MODE, secret);

       // byte[] iv = cipher.getParameters().getParameterSpec(IvParameterSpec.class).getIV();
       // byte[] ciphertext = cipher.doFinal("Hello, World!".getBytes("UTF-8"));
    }


    public  boolean encrypt(String path, String algorithm, SecretKeySpec key) {
try {
    db = DBMaker.fileDB("encrypted")
            //TODO encryption API
            //.encryptionEnable("password")
            .make();

    Cipher encryption = Cipher.getInstance(algorithm);
    encryption.init(Cipher.ENCRYPT_MODE, key);

    java.util.jar.JarFile jar = new java.util.jar.JarFile(path);
    java.util.Enumeration enumeration = jar.entries();

    ConcurrentMap map = db.hashMap("classmap").createOrOpen();

    while (enumeration.hasMoreElements()) {

        java.util.jar.JarEntry file = (java.util.jar.JarEntry) enumeration.nextElement();

        if (file.isDirectory()) { // if its a directory, create it
             continue;
        } else {
            if(file.getName().contains(".class")) {
             String classpath = file.getName().replaceAll("/",".").replaceAll(".class","");
                java.io.InputStream is = jar.getInputStream(file);
                byte[] content = IOUtils.toByteArray(is);

                byte[] encryptedContent = encryption.doFinal(content);
                map.put(classpath, encryptedContent);
            }


        }

    }
} catch (Exception ex)
{
    System.out.println(ex);
 db.close();
    File file = new File("enrypted");
    file.delete();
    return false;
}
db.close();
return true;

 }


    public static void main(String[] args) throws Exception{

        Options options = new Options();

        Option input = new Option("i", "input-jar", true, "Input Jar Path");
        input.setRequired(true);
        options.addOption(input);

        Option password = new Option("k", "password", true, "Password for Key Generation");
        password.setRequired(true);
        options.addOption(password);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("jar-encryption", options);

            System.exit(1);
            return;
        }

        String inputFilePath = cmd.getOptionValue("input-jar");
        String pass = cmd.getOptionValue("password");

           if( new JarEncryptor().encrypt(inputFilePath, algorithm, JarEncryptor.getKey(pass)))
               System.out.println("Successfully Encrypted Jar");

        else   System.out.println("Error in  Encrypting Jar");


    }
}
