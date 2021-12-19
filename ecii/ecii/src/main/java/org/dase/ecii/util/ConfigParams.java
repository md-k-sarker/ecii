package org.dase.ecii.util;

import org.dase.ecii.core.ECIIVersion;
import org.dase.ecii.core.Score;
import org.dase.ecii.core.ScoreType;
import org.dase.ecii.core.SharedDataHolder;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
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
import java.util.HashSet;
import java.util.Properties;

public final class ConfigParams {

    private static String appConfigFileName = "app.properties";
    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    /**
     * Java.util.properties does not accept multiline comment, weird!!
     */
    private static Properties prop;

    // properties to run ecii algorithm
    /**
     * Ecii algorithm version.
     * There are 3 different versions of ecii algorithm
     * V0, V1 and V2.
     * String, Optional, Default: V2
     */
    public static ECIIVersion ECIIAlgorithmVersion;

    /**
     * Configuration file path. For concept induction or similarity measurement lot's of settings are required.
     * So those are written in a config file and config file path are given as a parameter to the program.
     */
    public static String confFilePath;

    /**
     * For batch running we can provide the directory of config files  and the system will run for all config files.
     * String,
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
     * String, Required
     */
    public static String ontoPath;

    /*
     * outputResultPath will always be at the same directory of the confFile.
     */
    public static String outputResultPath;

    /**
     * Default prefix.
     */
    public static String namespace;

    /**
     * all prefix:suffix map
     */
    public static HashMap<String, String> prefixes;

    /**
     * Default delimiter of ontology entity
     * Only allowables are #, : or /
     * Default #
     */
    public static String delimiterOntoEntityIRI = "#";

    /**
     * Exension of result file.
     * Default: _results_ecii_v2.txt
     * result file for concept induction of similarity measure.
     */
    public static String resultFileExtension = "";

    /**
     * removeCommonTypes: Whether to remove those atomic types which appeared in both positive and negative inidviduals.
     * Also named as K7
     * Boolean, Optional, Default: true
     */
    public static boolean removeCommonTypes;

    /**
     * removeCommonTypesFromOneSideOnly: remove common types from only pos or neg subset.
     * If the concept covers more individuals from positive side than negative side then it will remove the concept from negative side.
     * else from positive side.
     * removeCommonTypes: must also be true to activate this one
     * Boolean, Optional, Default: true
     */
    public static boolean removeCommonTypesFromOneSideOnly;

    /**
     * conceptLimitInNegExpr: Number of allowable atomic concepts in the negative expression of a hornClause
     * Limits the number of concepts in negative/disjunction part of hornClause
     * Also named as K1
     * Integer, Optional, Default: 2
     */
    public static int conceptLimitInNegExpr;

    /**
     * hornClauseLimit: Number of allowable hornClause inside of a candidateClass.
     * Also named as K2
     * Integer, Optional, Default: 2
     */
    public static int hornClauseLimit;

    /**
     * objPropsCombinationLimit: Use this number of objectproperties in a single solution If we have more than 1 then it
     * will combine those objectProperties to make a solution.
     * Specifically it determines how many candidateClass will be in a single solution.
     * <p>
     * If we have it's value as more than 1, we will get solution like:
     * ∃ :imageContains.((:Artifact ⊓ ¬ (:Substance) ⊓ (:Artifact ⊓ ¬ (:Plant)))
     * Setting this to 1 will fix this kind of problem.
     *
     * Thoughts...
     * Is this used to combine different object properties? or it also combine same object-property multiple times?
     *
     * Also named as K3.
     * Integer, Optional, Default: 1
     */
    public static int objPropsCombinationLimit;

    /**
     * conceptLimitInPosExpr: Limit the number of atomic concepts in the positive expression of a hornClause
     * it can be called as directTypeLimit or conceptLimitInPosExpr.
     * Also named as K4.
     * ecii-v0: not used.
     * ecii-v2: being used.
     * Integer, Optional, Default: 2
     */
    public static int conceptLimitInPosExpr;

    /**
     * limitPosTypes: Whether to limit the positive types or not.
     * After limiting the posTypes we will only use the limited number of posTypes and not the all posTypes.
     * Boolean, Optional, Default: False
     */
    public static boolean limitPosTypes;

    /**
     * posClassListMaxSize: Select these numbers of top performing positiveClasses (for a single objProperty), from the list of positiveClasses (if more exist)
     * <p>
     * It will be activate if and only if limitPosTypes == True
     * <p>
     * After limiting the posTypes we will only use these number of posTypes and not the all posTypes.
     * So the accuracy may decrease, as we are not using all posTypes, only a subset of them,
     * which essentially making some individuals uncoverable (individual not having any covering types in the posTypes list)
     * <p>
     * If we have m objectPorperty it will keep m*n posTypes.
     * <p>
     * We use these to make the positive expression of hornClause.
     * --Size of combination would be (m*n)Cr or (m*posClassListMaxSize)--C--conceptLimitInPosExpr
     * <p>
     * Also named as K9
     * Integer, Optional, Default 20
     */
    public static int posClassListMaxSize;

    /**
     * limitNegTypes: Whether to limit the negative types or not.
     * After limiting the negTypes we will only use the limited number of negTypes and not the all negTypes.
     * Boolean, Optional, Default: False
     */
    public static boolean limitNegTypes;

    /**
     * negClassListMaxSize: Select these numbers of top performing negativeClasses (for a single objProperty), from the list of negativeClasses (if more exist)
     * <p>
     * It will be activate if and only if limitNegTypes == True
     * <p>
     * After limiting the negTypes we will only use these number of negTypes and not the all negTypes.
     * So the accuracy may decrease, as we are not using all negTypes, only a subset of them,
     * which essentially making some individuals uncoverable (individual not having any covering types in the negTypes list)
     * <p>
     * If we have m objectPorperty it will keep m*n negTypes.
     * <p>
     * We use this combination to make the negative expression of hornClause.
     * size would be nCr or negClassListMaxSize--C--conceptLimitInNegExpr
     * <p>
     * Also named as k10
     * Integer, Optional, Default: 20
     */
    public static int negClassListMaxSize;

    /**
     * hornClausesListMaxSize: Select these numbers of top performing hornClauses, to combine them in candidateClasses
     * size would be nCr or hornClausesListMaxSize--C--hornClauseLimit
     * Also named as K5
     * Integer, Optional, Default: 10
     */
    public static int hornClausesListMaxSize;

    /**
     * candidateClassesListMaxSize: Select these numbers of top performing candidateClasses, to combine them in candidateSolution
     * size would be nCr or candidateClassesListMaxSize--C--objPropsCombinationLimit
     * Also named as K6
     * Integer, Optional, Default: 10
     */
    public static int candidateClassesListMaxSize;

    /**
     * validateByReasonerSize: Validate the accuracy of top n solutions by reasoner upto this number of solutions
     * Also named as k8
     * Integer, Optional, Default: 0
     */
    public static int validateByReasonerSize;

    /**
     * ascendingOfStringLength: To sort the solution whether use string length from small to large or large to small.
     * If true, use small to large, if false use large to small
     * Boolean, Optional, Default: false
     */
    public static boolean ascendingOfStringLength;

    /**
     * When printing solutions, use rdfs:label annotations instead of name.
     * if no rdfs label is given, it will print the gerShortNameWithPrefix
     * If this is true, it will override the gerShortNameWithPrefix options, will just print the rdfs label.
     * Boolean, Optional, Default: false
     */
    public static boolean printLabelInsteadOfName = false;


    /**
     * Run mutiple operations in batch or a single operation
     * Will be overridden by program parameter -b or .config
     * Boolean, Optional, Default: false
     */
    public static boolean batch;

    /**
     * Run all the config files in this directory.
     * It is required when batch operation is prescribed
     * String, Required
     */
    public static String batchStartingPath;


    /**
     * Whether to measure the parwise similarity or not.
     * Will be overridden by the program parameter -m or -e
     * Boolean, Optional, Default: false
     */
    public static boolean runPairwiseSimilarity = false;

    //@formatter:off
    /**
     * Experimental
     *
     * This filters the posTypes. It keeps only those postypes which covers at-least n individuals,
     * where n = posTypeMinCoverIndivsSize
     *
     * Only applies to ecii-v2 and
     * also only being used inside: solution using multiple positive and multiple negative type
     *
     * <p>
     * if (hashMap.size() > ConfigParams.typeOfObjectsInPosIndivsMaxSize) {
     * hashMap = new HashMap<>(hashMap.entrySet().stream().sorted(Collections.reverseOrder(Map.Entry.comparingByValue()))
     * .limit(ConfigParams.typeOfObjectsInPosIndivsMaxSize)
     * .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue())));
     * }
     * <p>
     *
     * If total positive individual is less than the provided value it will take the
     * minimum of half of SharedDataHolder.posIndivs.size() or provided value.
     * posTypeMinCoverIndivsSize = Math.min(posTypeMinCoverIndivsSize, SharedDataHolder.posIndivs.size()/3)
     *
     * Integer, Optional, Default: 1
     */
    public static int posTypeMinCoverIndivsSize = 1;

    /**
     * Experimental
     *
     * This filters the negTypes. It keeps only those negTypes which covers at-least n individuals,
     * where n = negTypeMinCoverIndivsSize
     *
     * Only applies to ecii-v2 and
     * also only being used inside: solution using multiple positive and multiple negative type
     *
     * If total negative individual is less than the provided value it will take the minimum of those.
     * negTypeMinCoverIndivsSize = Math.min(negTypeMinCoverIndivsSize, SharedDataHolder.negIndivs.size()/2)
     * for posTypeMinCoverIndivsSize it's 1/3 of the posIndivs size.
     * Integer, Optional, Default: 1
     */
    public static int negTypeMinCoverIndivsSize = 1;
    //@formatter:on

    /**
     * Parse the config file
     * <p>
     * Config file must end with .config
     * <p>
     * config file's comment must only contain #, can't start with // or /*
     * (weird!) java properties doesn't allow multiline comment
     *
     * @param _confFilePath
     */
    public static void parseConfigParams(String _confFilePath) {

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

            // ecii algorithm version
            String eciiVersionRawName = prop.getProperty("ECIIAlgorithmVersion", "v2");
            setECIIVersion(eciiVersionRawName);

            prefixes = Utility.extractPrefixesFromConf(SharedDataHolder.confFileFullContent);
            // default prefix is the namespace
            namespace = prop.getProperty("namespace");
            prefixes.put("", namespace);
            // delimiter
            delimiterOntoEntityIRI = prop.getProperty("delimiter", "#");

            // pos and neg indivs
            parsePosAndNegIndivTypes(prop, "lp.positiveExamples", "lp.negativeExamples");

            // score type
            // allowable names: coverage,precision,recall,f_measure,coverage_by_reasoner,
            // precision_by_reasoner,recall_by_reasoner,f_measure_by_reasoner
            // beware that properties are case sensitive
            scoreTypeNameRaw = prop.getProperty("scoreType", "f_measure");
            parseScoreTypes(scoreTypeNameRaw);

            // reasoner
            reasonerName = prop.getProperty("reasoner.reasonerImplementation", "pellet");

            // obj property
            SharedDataHolder.objProperties = Utility.readObjectPropsFromConf(SharedDataHolder.confFileFullContent, delimiterOntoEntityIRI);
            // add none object property
            SharedDataHolder.objProperties.put(SharedDataHolder.noneOWLObjProp, 1.0);

            conceptLimitInPosExpr = Integer.valueOf(prop.getProperty("conceptLimitInPosExpr", "2"));
            conceptLimitInNegExpr = Integer.valueOf(prop.getProperty("conceptLimitInNegExpr", "2"));
            hornClauseLimit = Integer.valueOf(prop.getProperty("hornClauseLimit", "2"));
            objPropsCombinationLimit = Integer.valueOf(prop.getProperty("objPropsCombinationLimit", "1"));
            hornClausesListMaxSize = Integer.valueOf(prop.getProperty("hornClausesListMaxSize", "10"));
            candidateClassesListMaxSize = Integer.valueOf(prop.getProperty("candidateClassesListMaxSize", "10"));
            removeCommonTypes = Boolean.parseBoolean(prop.getProperty("removeCommonTypes", "true"));
            removeCommonTypesFromOneSideOnly = Boolean.parseBoolean(prop.getProperty("removeCommonTypesFromOneSideOnly", "true"));
            validateByReasonerSize = Integer.valueOf(prop.getProperty("validateByReasonerSize", "0"));
            limitPosTypes = Boolean.parseBoolean(prop.getProperty("limitPosTypes", "false"));
            posClassListMaxSize = Integer.valueOf(prop.getProperty("posClassListMaxSize", "20"));
            posTypeMinCoverIndivsSize = Integer.valueOf(prop.getProperty("posTypeMinCoverIndivsSize", "1"));
            // override the min value
            posTypeMinCoverIndivsSize = Math.min(posTypeMinCoverIndivsSize, SharedDataHolder.posIndivs.size() / 3);
            limitNegTypes = Boolean.parseBoolean(prop.getProperty("limitNegTypes", "false"));
            negClassListMaxSize = Integer.valueOf(prop.getProperty("negClassListMaxSize", "20"));
            negTypeMinCoverIndivsSize = Integer.valueOf(prop.getProperty("negTypeMinCoverIndivsSize", "1"));
            // override the min value
            negTypeMinCoverIndivsSize = Math.min(negTypeMinCoverIndivsSize, SharedDataHolder.negIndivs.size()/2);
            runPairwiseSimilarity = Boolean.parseBoolean(prop.getProperty("runPairwiseSimilarity", "false"));
            ascendingOfStringLength = Boolean.parseBoolean(prop.getProperty("ascendingOfStringLength", "false"));
            resultFileExtension = prop.getProperty("resultFileExtension", "_results_ecii_" + ECIIAlgorithmVersion + ".txt");
            printLabelInsteadOfName = Boolean.parseBoolean(prop.getProperty("printLabelInsteadOfName", "false"));

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

    /**
     * This method should be used for large number of individuals processing.
     * <p>
     * As this function is executed before parsing the prefixes and namespaces, individuals must have full name.
     * It will just create individual by the name it finds there.
     * <p>
     * For example, input should't be like this......
     * lp.positiveExamples = {"ex:indi_GO_0005737","ex:indi_GO_0005739"}
     * input should be like this....
     * lp.positiveExamples = {"http://purl.obolibrary.org/obo/indi_GO_0005737","http://purl.obolibrary.org/obo/indi_GO_0005739"}
     *
     * @param prop
     * @param posIndivKey
     * @param negIndivKey
     * @return
     */
    private static boolean parsePosAndNegIndivTypes(Properties prop, String posIndivKey, String negIndivKey) {
        try {
            logger.info("Parsing positive and negative individuals............");
            // process positive
            String posIndivsStr = prop.getProperty(posIndivKey, "");

            if (null != posIndivsStr) {
                if (posIndivsStr.length() > 0) {
                    HashSet<String> posIndivsStrSet = getIndivsArray(posIndivsStr);
                    if (posIndivsStrSet.size() > 0) {
                        for (String s : posIndivsStrSet) {
                            OWLNamedIndividual eachIndi = OWLManager.getOWLDataFactory().getOWLNamedIndividual(IRI.create(s));
                            SharedDataHolder.posIndivs.add(eachIndi);
                        }
                    } else {
                        logger.error("ERROR!!!!!!Positive individuals are not given. Please provide at-least 1 positive individuals.\nProgram exiting");
                        System.exit(-1);
                    }
                } else {
                    logger.error("ERROR!!!!!!Positive individuals are not given. Please provide at-least 1 positive individuals.\nProgram exiting");
                    System.exit(-1);
                }
            } else {
                logger.error("ERROR!!!!!!!!, posIndivs portion can't be null, program exiting");
                System.exit(-1);
            }

            // process negative
            String negIndivsStr = prop.getProperty(negIndivKey, "");

            if (null != negIndivsStr) {
                if (negIndivsStr.length() > 0) {
                    HashSet<String> negIndivsStrSet = getIndivsArray(negIndivsStr);
                    negIndivsStrSet.forEach(s -> {
                        OWLNamedIndividual eachIndi = OWLManager.getOWLDataFactory().getOWLNamedIndividual(IRI.create(s));
                        SharedDataHolder.negIndivs.add(eachIndi);
                    });
                } else {

                }
            } else {
                logger.warn("Warning!!!!!!!!, negIndivs portion is empty.\tWill produce result using only positive individuals");
            }

            logger.info("Parsing positive and negative individuals finished");
            logger.info("Positive individuals size: " + SharedDataHolder.posIndivs.size());
            logger.info("Negative individuals size: " + SharedDataHolder.negIndivs.size());

            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * By taking a text portion convert it into set of raw indivs name
     * Expected text: {"http://purl.obolibrary.org/obo/indi_GO_0005737","http://purl.obolibrary.org/obo/indi_GO_0005739"}
     *
     * @param indivsPortion
     * @return HashSet<String>
     */
    private static HashSet<String> getIndivsArray(String indivsPortion) {
        HashSet<String> indivsName = new HashSet<>();
        if (null != indivsPortion) {
            StringBuilder stringBuilder = new StringBuilder(indivsPortion);
            String[] indivsStrArr = indivsPortion.trim().replace("{", "").replace("}", "").split(",");
            for (String eachIndivsStr : indivsStrArr) {
                if (eachIndivsStr.length() > 0) {
                    eachIndivsStr = eachIndivsStr.replace("\"", "").replace("\"", "");
                    if (eachIndivsStr.length() > 0) {
                        indivsName.add(eachIndivsStr);
                    }
                } else {

                }
            }
        } else {
            logger.warn("Warning!!!!!!! indivsPortion is null. If this happens for negative individual then it's okay, " +
                    "otherwise program will malfunction");
        }
        return indivsName;
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
     * Set ecii version
     * default is v2
     *
     * @param ecciAlgorithmVersionName
     */
    private static void setECIIVersion(String ecciAlgorithmVersionName) {
        if (null == ecciAlgorithmVersionName) {
            ECIIAlgorithmVersion = ECIIVersion.V2;
            return;
        }
        if (ecciAlgorithmVersionName.equalsIgnoreCase("v0")) {
            ECIIAlgorithmVersion = ECIIVersion.V0;
        } else if (ecciAlgorithmVersionName.equalsIgnoreCase("v1")) {
            ECIIAlgorithmVersion = ECIIVersion.V1;
        } else {
            ECIIAlgorithmVersion = ECIIVersion.V2;
        }
    }

    /**
     * Utility method to print configparams
     */
    private static void printConfigProperties() {
        logger.info("\tECIIAlgorithmVersion: " + ECIIAlgorithmVersion);
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
        logger.info("\tremoveCommonTypesFromOneSideOnly: " + removeCommonTypesFromOneSideOnly);
        logger.info("\tlimitPosTypes: " + limitPosTypes);
        logger.info("\tposTypeMinCoverIndivsSize: " + posTypeMinCoverIndivsSize);
        logger.info("\tlimitNegTypes: " + limitNegTypes);
        logger.info("\tnegTypeMinCoverIndivsSize: " + negTypeMinCoverIndivsSize);

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
