package org.dase.ecii.util;
/*
Written by sarker.
Written at 6/8/18.
*/

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Class to copy image/any file from one directory to another directory.
 * This is primarily used to copy image files of ade20k dataset.
 *
 * @deprecated, we are using now python copier
 */
public class CopyImageFiles {

    public CopyImageFiles() {

    }

    static String srcTraversePath = "/Users/sarker/Workspaces/ProjectHCBD/experiments/Jun_08/neuron_activation_tracing/without_score/ning_v3/";
    static String dstTraversePath = "/Users/sarker/Workspaces/ProjectHCBD/datas/ade20k/training/";

    public static void copySingleFile(String src, String dest) {
        try {
            FileUtils.copyFile(new File(src), new File(dest));
        } catch (Exception ex) {

            System.out.println("Error occurred");
            ex.printStackTrace();
            System.exit(-1);
        }
    }

    public static void processPaths(File copyTo) {
        String copyToFileName = copyTo.getName();
        String parentClassname = copyTo.toPath().getParent().getFileName().toString();
        String copyToFolder = copyTo.getAbsolutePath().replaceAll(copyToFileName, "");

        //System.out.println("parentClassname: " + parentClassname);
        String imageFileName = copyToFileName.replaceAll(parentClassname.toLowerCase() + "_", "").replaceAll(".owl", ".jpg");

        //System.out.println("imageFileName: " + imageFileName);


        File dir = new File(dstTraversePath);

        try {
            Files.walk(dir.toPath()).filter(f -> f.toFile().isFile() && f.toFile().getAbsolutePath().endsWith(imageFileName)).forEach(f -> {
                System.out.println("imgFile found: " + f.toFile());

                String dstPath = copyToFolder + parentClassname + "_" + imageFileName;

                System.out.println("copyFile: " + f.toAbsolutePath());
                System.out.println("copyToFolder: " + copyToFolder);

                copySingleFile(f.toString(), dstPath);
            });
        } catch (Exception ex) {
            ex.printStackTrace();
        }


    }

    public static void main(String[] args) {
        try {
            Files.walk(Paths.get(srcTraversePath)).filter(f -> f.toFile().isFile() && f.toString().endsWith(".owl")).forEach(f -> {
                processPaths(f.toFile());
            });
        } catch (Exception ex) {
            System.out.println("Error occurred");
            ex.printStackTrace();
            System.exit(-1);
        }
    }
}
