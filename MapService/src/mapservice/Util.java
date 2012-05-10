/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package mapservice;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Jason
 */
public class Util {

    public static Object readObject(byte[] data) {
        ByteArrayInputStream stream = null;
        ObjectInputStream objStream = null;
        try {
            stream = new ByteArrayInputStream(data);
            objStream = new ObjectInputStream(stream);
            Object output = objStream.readObject();
            objStream.close();
            stream.close();
            return output;

        } catch (Exception ex) {
            Logger.getLogger(SlaveNode.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                stream.close();
                objStream.close();
            } catch (IOException ex) {
                Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return null;
    }

    public static byte[] writeObject(Object o) {
        byte[] output = null;
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objWriter = null;
        try {
            objWriter = new ObjectOutputStream(byteStream);
            objWriter.writeObject(o);

            output = byteStream.toByteArray();
            byteStream.close();
            objWriter.close();
        } catch (IOException ex) {
            Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            try {
                byteStream.close();
                objWriter.close();
            } catch (IOException ex) {
                Logger.getLogger(Util.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return output;
    }
}
