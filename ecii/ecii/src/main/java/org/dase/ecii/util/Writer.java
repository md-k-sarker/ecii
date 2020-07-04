package org.dase.ecii.util;

import java.io.BufferedWriter;
import java.io.FileWriter;

/**
 * This class will write stats in disk. This class acts like a static class.
 *
 * @author sarker
 */
public class Writer {


    private Writer() {

    }


    public static boolean writeInDisk(String path, String msgs, boolean append) {

        try (BufferedWriter br = new BufferedWriter(new FileWriter(path, append))) {
            br.write(msgs);
        } catch (Exception E) {
            return false;
        }
        return true;
    }

    /**
     * This is for unit testing
     *
     * @param args
     */
    public static void main(String[] args) {

    }

}
