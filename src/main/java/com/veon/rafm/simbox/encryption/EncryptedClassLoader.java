package com.veon.rafm.simbox.encryption;

import org.mapdb.DB;
import org.mapdb.DBMaker;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.concurrent.ConcurrentMap;

public class EncryptedClassLoader extends ClassLoader {

    Cipher decryption;
   final String algorithm="AES";
    private DB db; /*DBMaker.fileDB("encrypted")
            //TODO encryption API
            //.encryptionEnable("password")
            .make();
            */
    ConcurrentMap map;// = db.hashMap("classmap").createOrOpen();

    public void closeDB()
    {
        db.close();
    }

    //= Cipher.getInstance(algorithm);
       // decryption.init(Cipher.DECRYPT_MODE, spec);

 public   EncryptedClassLoader(SecretKeySpec spec) throws Exception{

        decryption=Cipher.getInstance(algorithm);
        decryption.init(Cipher.DECRYPT_MODE,spec);
        db=DBMaker.fileDB("encrypted").make();
        map = db.hashMap("classmap").createOrOpen();

    }
    @Override
    public Class findClass(String name){
     try {
         byte[] b = loadClassData(name);

         return defineClass(name, b, 0, b.length);
     }
     catch (Exception ex){
         System.out.println(ex);
     }
     return null;
    }

    private byte[] loadClassData(String name) throws Exception {

        byte[] decryptedContent = decryption.doFinal((byte[])map.get(name));
        //System.out.println(new String(decryptedContent));
        return decryptedContent;
    }
}
