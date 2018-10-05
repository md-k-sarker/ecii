package org.dase.util;

import org.dase.core.SharedDataHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Properties;

public final class ConfigParams {

    private static String appConfigFileName = "app.properties";
    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    private static Properties prop;

    // properties needed
    public static String confFilePath;
    public static String confFileDir;
    public static String reasonerName;
    public static String ontoPath;
    //outputResultPath will always be at the same directory of the confFile.
    public static String outputResultPath;
    // default prefix.
    public static String namespace;
    // all prefix:suffix map
    public static HashMap<String, String> prefixes;

    public static boolean removeCommonTypes;


    /**
     * This is also called as K1
     */
    public static int conceptLimitInNegExpr;
    /**
     * This is also called as K2
     */
    public static int hornClauseLimit;
    /**
     * This is also called as K3.
     */
    public static int objPropsCombinationLimit;
    /**
     * This is also called as K4. We can use it, but for simplicity do not use it now.
     */
    public static int directTypeLimit;

    /**
     * K5 select upto k5 hornClauses to make combination
     */
    public static int hornClausesListMaxSize;

    /**
     * K6 select upto k6 candidate classes to make combination
     */
    public static int candidateClassesListMaxSize;

    //public static double combinationThreshold;
    public static boolean batch;
    public static String batchStartingPath;

    // used in CreateOWLFromADE20k.java class
    public static final String ontologyIRI = "http://www.daselab.org/ontologies/ADE20K/hcbdwsu/";

    /**
     * Parse the default config.properties
     *
     * @return
     */
    public static void parseConfigParams(String _confFilePath) {

        confFilePath = Utility.getCorrectPath(SharedDataHolder.programStartingDir, _confFilePath);

        prop = new Properties();

        try (InputStream input = new BufferedInputStream(new FileInputStream(confFilePath))) {

            logger.info("given confFilePath: " + confFilePath);

            if (input == null) {
                logger.error("Error reading " + confFilePath + " file");
                System.exit(-1);
            }
            try {
                prop.load(input);
            } catch (IOException e) {
                e.printStackTrace();
                System.exit(-1);
            }

            logger.info("Printing config file before parsing: ");
            prop.forEach((k, v) -> {
                logger.info(k + ": " + v);
            });

            logger.info(System.lineSeparator() + "#############################" + System.lineSeparator());



            SharedDataHolder.confFileFullContent = readAllContent(confFilePath);

            prefixes = Utility.extractPrefixesFromConf(SharedDataHolder.confFileFullContent);
            // default prefix is the namespace
            namespace = prop.getProperty("namespace");
            prefixes.put("", namespace);

            reasonerName = prop.getProperty("reasoner.reasonerImplementation", "pellet");

            SharedDataHolder.objProperties = Utility.readObjectPropsFromConf(SharedDataHolder.confFileFullContent);
            // add none object property
            SharedDataHolder.objProperties.put(SharedDataHolder.noneOWLObjProp, 1.0);


            conceptLimitInNegExpr = Integer.valueOf(prop.getProperty("conceptLimitInNegExpr","3"));
            hornClauseLimit = Integer.valueOf(prop.getProperty("hornClauseLimit","3"));
            objPropsCombinationLimit = Integer.valueOf(prop.getProperty("objPropsCombinationLimit","3"));
            hornClausesListMaxSize = Integer.valueOf(prop.getProperty("hornClausesListMaxSize","50"));
            candidateClassesListMaxSize = Integer.valueOf(prop.getProperty("candidateClassesListMaxSize","50"));
            removeCommonTypes = Boolean.getBoolean(prop.getProperty("removeCommonTypes", "true"));

            confFileDir = Paths.get(confFilePath).getParent().toString();
            String replacement = "_concept_induction.txt";
            String resultFileName = Paths.get(confFilePath).getFileName().toString().replace(".config", replacement);
            outputResultPath = confFileDir + "/" + resultFileName;

            //ontoPath = prop.getProperty("ks.fileName");
            ontoPath = Utility.getCorrectPath(confFileDir, Utility.readOntoPathFromConf(SharedDataHolder.confFileFullContent));


            logger.info("Printing Config properties after parsing: ");
            printConfigProperties();


        } catch (Exception ex) {
            logger.error("Fatal Error", ex);
            System.exit(-1);
        }
    }

    /**
     * Utility method to print configparams
     */
    private static void printConfigProperties() {
        logger.info("\tconfFilePath: " + confFilePath);
        logger.info("\tontoPath: " + ontoPath);
        logger.info("\toutputResultPath: " + outputResultPath);
        logger.info("\tnamespace: " + namespace);
        logger.info("\tconceptLimitInNegExpr: " + conceptLimitInNegExpr);
        logger.info("\thornClauseLimit: " + hornClauseLimit);
        logger.info("\tobjPropsCombinationLimit: " + objPropsCombinationLimit);
        logger.info("\thornClausesListMaxSize: " + hornClausesListMaxSize);
        logger.info("\tcandidateClassesListMaxSize: " + candidateClassesListMaxSize);
    }

    /**
     * Utility method to read all content of a file
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    private static String readAllContent(String filePath) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(filePath));
        return new String(encoded, Charset.defaultCharset());
    }

    // private constructor, no instantiation
    private ConfigParams() {

    }

}
