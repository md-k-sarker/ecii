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

    /**
     * Java.util.properties does not accept multiline comment, weird!!
     */
    private static Properties prop;

    // properties needed
    /**
     * Configuration file path. For concept induction or similarity measurement lot's of settings are required.
     * So those are written in a config file and config file path are given as a parameter to the program.
     */
    public static String confFilePath;
    /**
     * For batch running we can provide the directory of config files  and the system will run for all config files.
     */
    public static String confFileDir;
    /**
     * Ontology reasoner name
     * Possible reasoners: hermit, elk, fact, jfact, pellet
     * Default: pellet
     */
    public static String reasonerName;
    /**
     * Type of metric to calculate accuracy/score of a solution.
     * Possible metrices: coverage, precision, recall, f_measure, hybrid (precision+recall+f_measure)
     * Default: f_measure
     */
    public static String scoreTypeNameRaw;
    /**
     * Ontology path, for batch running it will be different for multiple operation
     */
    public static String ontoPath;
    /*
     * outputResultPath will always be at the same directory of the confFile.
     */
    public static String outputResultPath;

    /**
     * default prefix.
     */
    public static String namespace;

    /**
     * all prefix:suffix map
     */
    public static HashMap<String, String> prefixes;

    /**
     * Default delimeter of ontology entity
     * Only allowables are #, : or /
     * Default #
     */
    public static String delimeter = "#";

    /**
     * Exension of result file.
     * Default: _results_ecii_v2.txt
     * result file for concept induction of similarity measure.
     */
    public static String resultFileExtension = "";

    /**
     * removeCommonTypes: Whether to remove those atomic types which appeared in both positive and negative inidviduals.
     * Also named as K7
     * Boolean
     * Default: true
     */
    public static boolean removeCommonTypes;

    /**
     * conceptLimitInNegExpr: Use only these number of top performing atomic concepts in the negative expression of a hornClause
     * Also named as K1
     * Integer
     * Default: 2
     */
    public static int conceptLimitInNegExpr;

    /**
     * hornClauseLimit: Number of allowable hornClause inside of a candidateClass.
     * Also named as K2
     * Integer
     * Default: 2
     */
    public static int hornClauseLimit;

    /**
     * objPropsCombinationLimit: Use this number of objectproperties in a single solution If we have more than 1 then it
     * will combine those objectProperties to make a solution.
     * Also named as K3.
     * Integer
     * Default: 2
     */
    public static int objPropsCombinationLimit;

    /**
     * conceptLimitInPosExpr: Limit the number of atomic concepts in the positive expression of a hornClause
     * it can be called as directTypeLimit or conceptLimitInPosExpr.
     * Also named as K4.
     * ecii-v0: not used.
     * ecii-v2: being used.
     * Integer
     * Default: 2
     */
    public static int conceptLimitInPosExpr;

    /**
     * posClassListMaxSize: Select these numbers of top performing positiveClasses, from the list of positiveClasses (if more exist)
     * to combine them.
     * We use this combination to make the positive expression of hornClause.
     * size of combination would be nCr or posClassListMaxSize--C--conceptLimitInPosExpr
     * Also named as K9
     * Integer
     * Default 20
     */
    public static int posClassListMaxSize;

    /**
     * negClassListMaxSize: Select these numbers of top performing negativeClasses, from the list of negativeClasses (if more exist)
     * to combine them.
     * We use this combination to make the negative expression of hornClause.
     * size would be nCr or negClassListMaxSize--C--conceptLimitInNegExpr
     * Also named as k10
     * Integer
     * Default: 20
     */
    public static int negClassListMaxSize;

    /**
     * hornClausesListMaxSize: Select these numbers of top performing hornClauses, to combine them in candidateClasses
     * size would be nCr or hornClausesListMaxSize--C--hornClauseLimit
     * Also named as K5
     * Integer
     * Default: 10
     */
    public static int hornClausesListMaxSize;

    /**
     * candidateClassesListMaxSize: Select these numbers of top performing candidateClasses, to combine them in candidateSolution
     * size would be nCr or candidateClassesListMaxSize--C--objPropsCombinationLimit
     * Also named as K6
     * Integer
     * Default: 10
     */
    public static int candidateClassesListMaxSize;

    /**
     * validateByReasonerSize: Validate the accuracy of top solutions by reasoner upto this number of solutions
     * Also named as k8
     * Integer
     * Default: 0
     */
    public static int validateByReasonerSize;

    /**
     * ascendingOfStringLength: To sort the solution whether use string length from small to large or large to small.
     * If true, use small to large, if false use large to small
     * Boolean
     * Default: false
     */
    public static boolean ascendingOfStringLength;

    /**
     * When printing solutions, use rdfs:label annotations instead of name
     * Boolean, Optional
     * Default: false
     */
    public static boolean printLabelInsteadOfName = false;

    //public static double combinationThreshold;
    public static boolean batch;
    public static String batchStartingPath;

    // used in CreateOWLFromADE20k.java class
    public static final String ontologyIRI = "http://www.daselab.org/ontologies/ADE20K/hcbdwsu/";

    /**
     * whether to measure the parwise similarity or not.
     * Will be overridden by the program parameter -m or -e
     */
    public static boolean runPairwiseSimilarity = false;

    /**
     * Experimental: instead of typeOfObjectsInPosIndivsMaxSize, posClassListMaxSize is multiplied by multiplicationConstant in limiting the positive types list.
     * This limits the solution size severely!! need to check. todo(zaman)
     * Integer
     * Default 1
     */
    public static int multiplicationConstant = 1;
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
     * This is being used inside: solution using multiple positive and multiple negative type
     * need to check. todo(zaman)
     */
    public static int typeOfObjectsInPosIndivsMinSize = 5;

    /**
     * Experimental
     * need to check. todo(zaman)
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

            logger.info("Given confFilePath: " + confFilePath);

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
            // delimeter
            delimeter = prop.getProperty("delimeter", "#");

            // score type
            // allowable names: coverage,precision,recall,f_measure,coverage_by_reasoner,
            // precision_by_reasoner,recall_by_reasoner,f_measure_by_reasoner
            // beware that properties are case sensitive
            scoreTypeNameRaw = prop.getProperty("scoreType", "f_measure");
            parseScoreTypes(scoreTypeNameRaw);

            // reasoner
            reasonerName = prop.getProperty("reasoner.reasonerImplementation", "pellet");

            // obj property
            SharedDataHolder.objProperties = Utility.readObjectPropsFromConf(SharedDataHolder.confFileFullContent, delimeter);
            // add none object property
            SharedDataHolder.objProperties.put(SharedDataHolder.noneOWLObjProp, 1.0);

            logger.info("Pos indivs---------");
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
            runPairwiseSimilarity = Boolean.parseBoolean(prop.getProperty("runPairwiseSimilarity", "false"));
            ascendingOfStringLength = Boolean.parseBoolean(prop.getProperty("ascendingOfStringLength", "false"));
            resultFileExtension = prop.getProperty("resultFileExtension", "_results_ecii_v2.txt");
            printLabelInsteadOfName = Boolean.parseBoolean(prop.getProperty("ascendingOfStringLength", "false"));

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
                    Score.defaultScoreType = ScoreType.F_MEASURE;
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
