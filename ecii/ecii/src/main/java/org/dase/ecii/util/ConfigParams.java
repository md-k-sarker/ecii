package org.dase.ecii.util;

import org.dase.ecii.core.Score;
import org.dase.ecii.core.ScoreType;
import org.dase.ecii.core.SharedDataHolder;
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
    public static String scoreTypeNameRaw;
    public static String ontoPath;
    //outputResultPath will always be at the same directory of the confFile.
    public static String outputResultPath;
    // default prefix.
    public static String namespace;
    // all prefix:suffix map
    public static HashMap<String, String> prefixes;

    /**
     *
     */
    public static String resultFileExtension = "";
    /**
     * K7/atomic types both appeared in positive and negative
     */
    public static boolean removeCommonTypes;

    /**
     * This is also called as K1
     * K1/negExprTypeLimit, limit of number of concepts in a negative expression of a hornClause
     */
    public static int conceptLimitInNegExpr;
    /**
     * Number of allowable hornClause inside of a candidateClass.
     * This is also called as K2
     */
    public static int hornClauseLimit;
    /**
     * This is also called as K3.
     * K3/permutate/combination untill this number of objectproperties
     */
    public static int objPropsCombinationLimit;
    /**
     * it can be called as directTypeLimit or conceptLimitInPosExpr. This is also called as K4.
     * ecii-v0: not being used.
     * ecii-v2: we are using this one.
     * <p>
     * limit of number of concepts in a positive expression of a hornClause
     */
    public static int conceptLimitInPosExpr;

    /**
     * k9/ maximum posclasses (top scoring) to do the combination.
     * size of combination would be nCr or posClassListMaxSize--C--conceptLimitInPosExpr
     */
    public static int posClassListMaxSize;

    /**
     * Experimental: instead of typeOfObjectsInPosIndivsMaxSize, posClassListMaxSize is multiplied by multiplicationConstant in limiting the positive types list.
     */
    public static int multiplicationConstant = 1;

    /**
     * k10/ maximum negclasses (top scoring) to do the combination.
     * size would be nCr or negClassListMaxSize--C--conceptLimitInNegExpr
     */
    public static int negClassListMaxSize;

    /**
     * K5 select upto k5 hornClauses to make combination
     */
    public static int hornClausesListMaxSize;

    /**
     * K6 select upto k6 candidate classes to make combination
     */
    public static int candidateClassesListMaxSize;

    /**
     * K8/Validate the accuracy of top solutions by reasoner upto this number of solutions
     */
    public static int validateByReasonerSize;

    public static boolean ascendingOfStringLength;

    //public static double combinationThreshold;
    public static boolean batch;
    public static String batchStartingPath;

    // used in CreateOWLFromADE20k.java class
    public static final String ontologyIRI = "http://www.daselab.org/ontologies/ADE20K/hcbdwsu/";

    public static boolean runPairwiseSimilarity = false;

    //@formatter:off
    /**
     * Experimental, maximum positive individuals (positive objects connected through some property) size, it was used to limit
     *
     *             if (hashMap.size() > ConfigParams.typeOfObjectsInPosIndivsMaxSize) {
     *                 hashMap = new HashMap<>(hashMap.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
     *                         .limit(ConfigParams.typeOfObjectsInPosIndivsMaxSize)
     *                         .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
     */
//    public static int typeOfObjectsInPosIndivsMaxSize = 100;

    /**
     * Experimental
     */
    public static int typeOfObjectsInPosIndivsMinSize = 5;

    /**
     * Experimental
     */
    public static int typeOfObjectsInNegIndivsMinSize = 5;
    //@formatter:on

    /**
     * Parse the default config.properties
     *
     * @return
     */
    public static void parseConfigParams(String _confFilePath) {

        // config file's comment must only contain #, can't start with // or /*
        // java properties doesn't allow multiline comment
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

            // score type
            // allowable names: coverage,precision,recall,f_measure,coverage_by_reasoner,
            // precision_by_reasoner,recall_by_reasoner,f_measure_by_reasoner
            // beware that properties are case sensitive
            scoreTypeNameRaw = prop.getProperty("scoreType", "precision");
            parseScoreTypes(scoreTypeNameRaw);

            // reasoner
            reasonerName = prop.getProperty("reasoner.reasonerImplementation", "pellet");

            // obj property
            SharedDataHolder.objProperties = Utility.readObjectPropsFromConf(SharedDataHolder.confFileFullContent);
            // add none object property
            SharedDataHolder.objProperties.put(SharedDataHolder.noneOWLObjProp, 1.0);

            logger.info("pos indivs---------");
            logger.info(prop.get("lp.positiveExamples").toString());

            conceptLimitInPosExpr = Integer.valueOf(prop.getProperty("conceptLimitInPosExpr", "2"));
            conceptLimitInNegExpr = Integer.valueOf(prop.getProperty("conceptLimitInNegExpr", "2"));
            hornClauseLimit = 1; //Integer.valueOf(prop.getProperty("hornClauseLimit", "2"));
            objPropsCombinationLimit = Integer.valueOf(prop.getProperty("objPropsCombinationLimit", "2"));
            hornClausesListMaxSize = Integer.valueOf(prop.getProperty("hornClausesListMaxSize", "10"));
            candidateClassesListMaxSize = Integer.valueOf(prop.getProperty("candidateClassesListMaxSize", "10"));
            removeCommonTypes = Boolean.parseBoolean(prop.getProperty("removeCommonTypes", "true"));
            validateByReasonerSize = Integer.valueOf(prop.getProperty("validateByReasonerSize", "0"));
            posClassListMaxSize = Integer.valueOf(prop.getProperty("posClassListMaxSize", "20"));
            negClassListMaxSize = Integer.valueOf(prop.getProperty("negClassListMaxSize", "20"));
            runPairwiseSimilarity = Boolean.parseBoolean(prop.getProperty("removeCommonTypes", "false"));
            ascendingOfStringLength = Boolean.parseBoolean(prop.getProperty("ascendingOfStringLength", "false"));
            resultFileExtension = prop.getProperty("resultFileExtension","_results_ecii_v2.txt");

            confFileDir = Paths.get(confFilePath).getParent().toString();
            String replacement = ConfigParams.resultFileExtension;
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

    private static boolean parseScoreTypes(String scoreTypeNameRaw) {
        try {
            switch (scoreTypeNameRaw) {
                case "hybrid":
                    Score.defaultScoreType = ScoreType.HYBRID;
                    break;
                case "precision":
                    Score.defaultScoreType = ScoreType.PRECISION;
                    break;
                case "recall":
                    Score.defaultScoreType = ScoreType.RECALL;
                    break;
                case "f_measure":
                    Score.defaultScoreType = ScoreType.F_MEASURE;
                    break;
                case "coverage":
                    Score.defaultScoreType = ScoreType.COVERAGE;
                    break;
                case "precision_by_reasoner":
                    Score.defaultScoreType = ScoreType.PRECISION_by_REASONER;
                    break;
                case "recall_by_reasoner":
                    Score.defaultScoreType = ScoreType.RECALL_by_REASONER;
                    break;
                case "f_measure_by_reasoner":
                    Score.defaultScoreType = ScoreType.F_MEASURE_by_REASONER;
                    break;
                case "coverage_by_reasoner":
                    Score.defaultScoreType = ScoreType.COVERAGE_by_REASONER;
                    break;
                default:
                    Score.defaultScoreType = ScoreType.PRECISION;
            }
            return true;
        } catch (Exception ex) {
            logger.error(Utility.getStackTraceAsString(ex));
            return false;
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
        logger.info("\tconceptLimitInPosExpr: " + conceptLimitInPosExpr);
        logger.info("\tconceptLimitInNegExpr: " + conceptLimitInNegExpr);
        logger.info("\tposClassListMaxSize: " + posClassListMaxSize);
        logger.info("\tnegClassListMaxSize: " + negClassListMaxSize);
        logger.info("\thornClauseLimit: " + hornClauseLimit);
        logger.info("\tobjPropsCombinationLimit: " + objPropsCombinationLimit);
        logger.info("\thornClausesListMaxSize: " + hornClausesListMaxSize);
        logger.info("\tcandidateClassesListMaxSize: " + candidateClassesListMaxSize);
        logger.info("\tremoveCommonTypes: " + removeCommonTypes);
        logger.info("\tscoreTypeNameRaw: " + scoreTypeNameRaw);
        logger.info("\tvalidateByReasonerSize: " + validateByReasonerSize);
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
