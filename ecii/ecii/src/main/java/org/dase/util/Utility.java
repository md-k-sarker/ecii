/**
 *
 */
package org.dase.util;

import com.google.common.collect.Lists;
import com.wcohen.ss.Levenstein;
import org.apache.log4j.Level;
import org.dase.exceptions.MalFormedIRIException;
import org.semanticweb.HermiT.Configuration;
import org.semanticweb.HermiT.Reasoner;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.profiles.OWLProfileReport;
import org.semanticweb.owlapi.profiles.Profiles;
import org.semanticweb.owlapi.reasoner.*;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.manchester.cs.jfact.JFactFactory;
import com.clarkparsia.pellet.owlapiv3.PelletReasonerFactory;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import uk.ac.manchester.cs.factplusplus.owlapiv3.FaCTPlusPlusReasonerFactory;
import uk.ac.manchester.cs.jfact.JFactFactory;


import java.io.*;
import java.lang.invoke.MethodHandles;
import java.net.URLEncoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author sarker
 */
public class Utility {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // log level: ALL < DEBUG < INFO < WARN < ERROR < FATAL < OFF

    // "yyyy/MM/dd HH:mm:ss"
    public static final String DATE_TIME_FORMAT = "MM.dd.yyyy  HH.mm.ss a";


    /**
     * Save combined ontology to file system
     *
     * @param ontology
     * @param path     : this method don't make any attempt to fix the path. make sure your path is correct before calling this method. TODO.
     * @throws OWLOntologyStorageException
     * @throws OWLOntologyStorageException
     * @apiNote N.B: DO-NOT-SAVE- Each Combined Ontology do not save in disk for each
     * combined ontology. it takes too much space in disk. Each combined
     * ontology is being approximately 80MB. 80 MB *22000 = 2000,000 MB
     * = 2000 GB
     */
    public static void saveOntology(OWLOntology ontology, String path) throws OWLOntologyStorageException {

        String encodedPath = "";
        try {
            // encoded path not working with owlapi.
            encodedPath = URLEncoder.encode(path, "UTF-8");
            IRI owlDiskFileIRIForSave = IRI.create("file:" + path);
            ontology.getOWLOntologyManager().saveOntology(ontology, owlDiskFileIRIForSave);
        } catch (UnsupportedEncodingException ex) {
            logger.error("This path is not valid according to java: " + encodedPath);
            logger.error("UnsupportedEncodingException error. program exiting");
            logger.error(getStackTraceAsString(ex));
            System.exit(-1);
        }
    }


    /**
     * Load ontology from file system
     *
     * @param ontoFilePath
     * @return
     * @throws OWLOntologyCreationException
     * @throws IOException
     */
    public static OWLOntology loadOntology(String ontoFilePath) throws OWLOntologyCreationException, IOException {
        return loadOntology(Paths.get(ontoFilePath), null);
    }

    /**
     * Load ontology from file system
     *
     * @param ontologyPath
     * @param monitor
     * @return
     * @throws OWLOntologyCreationException
     * @throws IOException
     */
    public static OWLOntology loadOntology(String ontologyPath, Monitor monitor) throws OWLOntologyCreationException, IOException {
        Path ontoPath = Paths.get(ontologyPath).toAbsolutePath();
        return loadOntology(ontoPath, monitor);
    }

    /**
     * Load ontology from file system
     *
     * @throws OWLOntologyCreationException
     * @throws IOException
     */
    /**
     * @param ontologyPath
     * @param monitor
     * @return
     * @throws OWLOntologyCreationException
     * @throws IOException
     */
    public static OWLOntology loadOntology(Path ontologyPath, Monitor monitor) throws OWLOntologyCreationException, IOException {

        logger.info("Ontology Path before resolving: " + ontologyPath.toString());
        Path ontoPath = ontologyPath;

        File ontoFile = null;
        if (Files.isSymbolicLink(ontoPath)) {
            logger.info("Path is symbolic: " + ontoPath);
            ontoFile = Files.readSymbolicLink(ontoPath).toFile();
        } else {
            logger.info("Path is not symbolic: " + ontoPath);
            ontoFile = ontoPath.toFile();
        }
        ontoFile = ontoFile.toPath().toRealPath().toFile();
        logger.info("Ontology Path after resolving: " + ontoFile.getCanonicalPath());

        // We first need to obtain a copy of an OWLOntologyManager, which, as the name
        // suggests, manages a set of ontologies.
        OWLOntologyManager owlOntologyManager = OWLManager.createOWLOntologyManager();
        OWLDataFactory owlDataFactory = owlOntologyManager.getOWLDataFactory();

        // load the owlOntology.
        logger.info("\nOntology loading...");
        OWLOntology owlOntology = owlOntologyManager.loadOntologyFromOntologyDocument(ontoFile);

        // Report information about the owlOntology
        logger.info("Ontology Loaded");
        logger.info("Ontology path: " + ontoFile.getAbsolutePath());
        logger.info("Ontology id : " + owlOntology.getOntologyID());
        OWLProfileReport report = Profiles.OWL2_EL.checkOntology(owlOntology);

        if (report.isInProfile()) {
            logger.info("Is in OWL_EL: " + report.isInProfile());
        } else {
            logger.info("total violations: " + report.getViolations().size());
        }
        logger.info("Format : " + owlOntologyManager.getOntologyFormat(owlOntology));


        return owlOntology;
    }


    /**
     * Return Ontology IRI.
     *
     * @param ontology
     * @return
     */
    public static String getOntologyPrefix(OWLOntology ontology) {
        com.google.common.base.Optional<IRI> ontoIRI = ontology.getOntologyID().getDefaultDocumentIRI();
        String defaultOntologyIRIPrefix = ConfigParams.namespace;
        if (ontoIRI.isPresent()) {
            defaultOntologyIRIPrefix = ontoIRI.get().toString();
            logger.info("getDefaultDocumentIRI is present. DefaultIRIPrefix: " + defaultOntologyIRIPrefix);
        } else {
            ontoIRI = ontology.getOntologyID().getOntologyIRI();
            if (ontoIRI.isPresent()) {
                defaultOntologyIRIPrefix = ontoIRI.get().toString();
                logger.info("getDefaultDocumentIRI is not present. DefaultIRIPrefix: " + defaultOntologyIRIPrefix);
            } else {
                defaultOntologyIRIPrefix = "";
            }
        }
        // manually set defaultOntologyIRIPrefix
        // defaultOntologyIRIPrefix = "http://www.adampease.org/OP/SUMO.owl#";
        if (defaultOntologyIRIPrefix.length() > 0 && !defaultOntologyIRIPrefix.endsWith("#"))
            defaultOntologyIRIPrefix = defaultOntologyIRIPrefix + "#";
        logger.info("DefaultIRIPrefix: " + defaultOntologyIRIPrefix);

        return defaultOntologyIRIPrefix;
    }


    /**
     * Return the label if there is one. If not, just use the class URI short form
     *
     * @param owlEntity
     * @return
     */
    public static String labelFor(OWLEntity owlEntity) {
        /*
         * Use a visitor to extract label annotations
         */
        LabelExtractor le = new LabelExtractor();
        // Set<OWLAnnotation> annotations = owlEntity.getAnnotations(ontology);
        // for (OWLAnnotation anno : annotations) {
        // anno.accept(le);
        // }
        /* Print out the label if there is one. If not, just use the class URI */
        if (le.getResult() != null) {
            return le.getResult();
        } else {
            return owlEntity.getIRI().getShortForm();
        }
    }


    /**
     * Return the shortform of the owlentity. if shortname is empty then return empty string "".
     *
     * @param owlEntity
     * @return String : shortname of the entity
     */
    public static String getShortName(OWLEntity owlEntity) {
        if (null == owlEntity)
            return null;
        if (owlEntity.equals(SharedDataHolder.noneOWLObjProp))
            return SharedDataHolder.noneObjPropStr;
        String shortName = owlEntity.getIRI().getShortForm();
        if (shortName == null) {
            shortName = "";
        }
        return shortName;
    }

    /**
     * Get short form of each class.
     *
     * @param owlClassExpression: OWLClassExpression
     * @return
     */
    public static String shortFormForClassExpression(OWLClassExpression owlClassExpression) {

        StringBuilder sb = new StringBuilder();
        //logger.info("type of expr:" + owlClassExpression.getClassExpressionType());
        owlClassExpression.getNestedClassExpressions().forEach(eachClassExpr -> {
            if (!eachClassExpr.equals(owlClassExpression))
                sb.append(eachClassExpr + " ");
            // logger.info("nested classes: " + eachClassExpr);
        });

        return sb.toString();
    }

    /**
     * Compute confidence for mathing two entity by using levenstein distance
     *
     * @param labelA
     * @param labelB
     * @return
     */
    public static double computeConfidence(String labelA, String labelB) {
        double confidenceValue = 1 - (Math.abs(new Levenstein().score(labelA, labelB))
                / (double) Math.max(labelA.length(), labelB.length()));

        return confidenceValue;
    }


    /**
     * Create string of current date_time according to default date_time format.
     *
     * @return
     */
    public static String getCurrentDateTimeAsString() {
        return getCurrentDateTimeAsString(DATE_TIME_FORMAT);
    }

    /**
     * Create string of current date_time according to given date_time format.
     *
     * @return
     */
    public static String getCurrentDateTimeAsString(String format) {
        if (null == format)
            return null;
        String time = getDateTimeFormat(DATE_TIME_FORMAT).format(new Date());
        return time;
    }

    /**
     * Return the DateFormat using default DATE_TIME_FORMAT.
     *
     * @return DateFormat
     */
    public static DateFormat getDateTimeFormat() {
        return getDateTimeFormat(DATE_TIME_FORMAT);
    }

    /**
     * Return the DateFormat using the given DATE_TIME_FORMAT.
     *
     * @return DateFormat
     * @Input String : dateformat. example: "MM.dd.yyyy  HH.mm.ss a"
     */
    public static DateFormat getDateTimeFormat(String format) {
        if (null == format)
            return null;
        DateFormat dateFormat = new SimpleDateFormat(format);
        return dateFormat;
    }

    public static String getStackTraceAsString(Exception e) {

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String sStackTrace = sw.toString(); // stack trace as a string
        return sStackTrace;
    }


    /**
     * Precompute inference using owlreasoner
     *
     * @param owlReasoner
     * @return OWLReasoner
     */
    private static OWLReasoner precomputeInference(OWLReasoner owlReasoner) {
        // precompute inference
        owlReasoner.precomputeInferences(InferenceType.CLASS_ASSERTIONS);
        owlReasoner.precomputeInferences(InferenceType.CLASS_HIERARCHY);
        owlReasoner.precomputeInferences(InferenceType.DISJOINT_CLASSES);

        owlReasoner.precomputeInferences(InferenceType.OBJECT_PROPERTY_ASSERTIONS);
        owlReasoner.precomputeInferences(InferenceType.OBJECT_PROPERTY_HIERARCHY);

        owlReasoner.precomputeInferences(InferenceType.DIFFERENT_INDIVIDUALS);
        owlReasoner.precomputeInferences(InferenceType.SAME_INDIVIDUAL);

        owlReasoner.precomputeInferences(InferenceType.DATA_PROPERTY_ASSERTIONS);
        owlReasoner.precomputeInferences(InferenceType.DATA_PROPERTY_HIERARCHY);

        return owlReasoner;
    }

    /**
     * Initiate owlReasoner using reasoner name and the ontology.
     *
     * @param reasonerName: String
     * @param ontology:     owlOntology
     * @return OWLReasoner.
     */
    public static OWLReasoner initReasoner(String reasonerName, OWLOntology ontology, Monitor monitor) {

        if (null == reasonerName)
            throw new NullPointerException("Reasoner name is null.");
        if (null == ontology)
            throw new NullPointerException("Ontology is null.");

        ReasonerProgressMonitor progressMonitor = new NullReasonerProgressMonitor();
        FreshEntityPolicy freshEntityPolicy = FreshEntityPolicy.ALLOW;
        long timeOut = Integer.MAX_VALUE;
        IndividualNodeSetPolicy individualNodeSetPolicy = IndividualNodeSetPolicy.BY_NAME;
        OWLReasonerConfiguration reasonerConfig = new SimpleConfiguration(progressMonitor, freshEntityPolicy, timeOut, individualNodeSetPolicy);
        // OWLReasonerConfiguration config = new SimpleConfiguration(ConfigParams.timeOut);

        OWLReasoner owlReasoner = null;
        OWLReasonerFactory reasonerFactory = null;

        if (reasonerName.equals("hermit")) {
            // hermit version: 1.4.1.513
            //reasonerFactory = new Reasoner.ReasonerFactory();

            reasonerFactory = new org.semanticweb.HermiT.ReasonerFactory();
            Configuration c = new Configuration();
            c.ignoreUnsupportedDatatypes = true;
//			c.throwInconsistentOntologyException = false;
            reasonerConfig = c;
        } else if (reasonerName.equals("jfact")) {
            reasonerFactory = new JFactFactory();
        } else if (reasonerName.equals("fact")) {
            reasonerFactory = new FaCTPlusPlusReasonerFactory();
        } else if (reasonerName.equals("elk")) {
            reasonerFactory = new ElkReasonerFactory();
        } else {
            // default is pellet
            reasonerFactory = PelletReasonerFactory.getInstance();
            // change log level to WARN for Pellet, because otherwise log
            // output will be very large
            org.apache.log4j.Logger pelletLogger = org.apache.log4j.Logger.getLogger("org.mindswap.pellet");
            pelletLogger.setLevel(Level.WARN);

        }

        if (null != reasonerFactory) {
            owlReasoner = reasonerFactory.createNonBufferingReasoner(ontology, reasonerConfig);
        } else {
            logger.error("reasonerFacotry is null. Program exiting");
            monitor.stopSystem("reasonerFacotry is null. Program exiting", true);
        }

        if (null != owlReasoner) {
            //owlReasoner = new ThreadSafeOWLReasoner(owlReasoner, false);
            owlReasoner = precomputeInference(owlReasoner);
            logger.info("reasoner name: " + owlReasoner.getReasonerName());
        } else {
            logger.error("owl reasoner is null. Program exiting");
            monitor.stopSystem("owl reasoner is null. Program exiting", true);
        }

        return owlReasoner;
    }


    /**
     * extract prefixes from the conf file content. if the prefixes don't include new line then java properties
     * can extract this but it may have new line. so need regular expression.
     *
     * @return HashMap<String                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               ,                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               String>
     */
    public static HashMap<String, String> extractPrefixesFromConf(String confFileFullContent) throws IOException {

        HashMap<String, String> prefixes = new HashMap<>();

        String inputText = confFileFullContent;

        String regexText = "(prefixes){1}(\\s)*=(\\s)*\\[{1}([^]]|[\\s])*]{1}";
        Pattern pattern = Pattern.compile(regexText);
        Matcher matcher = pattern.matcher(inputText);

        //logger.info("inputText: " + inputText);
        // logger.debug("regexText: " + regexText);

        String prefixesPortion = "";
        if (matcher.find()) {
            prefixesPortion = matcher.group();
            //logger.debug("prefixesPortion: " + prefixesPortion);
        }


        // extract each prefix:suffix

        String regexForEachMapEntry = "[(]([^)]|[\\s])*[)]{1}";
        pattern = Pattern.compile(regexForEachMapEntry);
        matcher = pattern.matcher(prefixesPortion);

        //logger.info("prefixesPortion: " + prefixesPortion);
        //logger.debug("regexForEachMapEntry: " + regexForEachMapEntry);

        String eachMapEntry = "";
        while (matcher.find()) {
            eachMapEntry = matcher.group();
            logger.info("eachMapEntry: " + eachMapEntry);
            String[] parts = eachMapEntry.trim().replaceAll("[\"|\n|\\(|\\)]", "").split(",");
            String prefix = parts[0].trim();
            String suffix = parts[1].trim();
            logger.info("prefix: " + prefix);
            logger.info("suffix: " + suffix);
            prefixes.put(prefix, suffix);
        }

        return prefixes;
    }


    /**
     * Read object properties from the conf file.
     * Object properties must be written as: ex:ob1 or :ob1 or ob1 format and each object properties
     * must be seperated by comma or new line.
     *
     * @param confFileFullContent : confFile content
     * @return HashMap<OWLObjectProperty                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               ,                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                                               Double>
     */
    public static HashMap<OWLObjectProperty, Double> readObjectPropsFromConf(String confFileFullContent) throws IOException, MalFormedIRIException {

        //logger.debug("Reading object properties from full content: " + confFileFullContent);

        String inputText = confFileFullContent;

        String regexText = "(objectProperties){1}(\\s)*=(\\s)*\\{{1}([^}]|[\\s])*}{1}";
        Pattern pattern = Pattern.compile(regexText);
        Matcher matcher = pattern.matcher(inputText);

        //logger.debug("inputText: " + inputText);
        //logger.debug("regexText: " + regexText);

        String objPropsPortion = "";
        if (matcher.find()) {
            objPropsPortion = matcher.group();
            //logger.debug("objPropsPortion: " + objPropsPortion);
        }

        String regexEachEntity = "\"{1}([^\"])*\"{1}";
        ArrayList<IRI> objPropsIRI = extractEachEntityIRIFromTextPortion(objPropsPortion, regexEachEntity);

        HashMap<OWLObjectProperty, Double> objectPropertyFloatHashMap = new HashMap<>();
        objPropsIRI.forEach(iri -> {
            OWLObjectProperty objProp = OWLManager.getOWLDataFactory().getOWLObjectProperty(iri);
            // by default give all objectproperty same score. based on the score we may choose to use certain object properties or nor.
            objectPropertyFloatHashMap.put(objProp, 1.0);
        });
        return objectPropertyFloatHashMap;
    }

    /**
     * Create IRI from the given string. Given name must be ex:ob1 or :ob1 or ob1 format format.
     * if only ob1 is given then it must be full name. like www.http://hcbd.org#indi
     *
     * @param textPortion:    String.
     *                        Example1 :
     *                        objectProperties={":hasCar",":hasShape",":load",":loadCount",":wheels"}
     *                        Example2:
     *                        lp.positiveExamples = {
     *                        "kb:art",
     *                        "kb:calvin"
     *                        }
     * @param regexEachEntity : String
     * @return ArrayList<IRI>
     */
    public static ArrayList<IRI> extractEachEntityIRIFromTextPortion(String textPortion, String regexEachEntity) throws MalFormedIRIException {

        ArrayList<IRI> entityIRIs = new ArrayList<>();


        Pattern pattern = Pattern.compile(regexEachEntity);
        Matcher matcher = pattern.matcher(textPortion);

        String eachObjPropStr = "";
        // for each entity.
        while (matcher.find()) {
            eachObjPropStr = matcher.group();
            eachObjPropStr = eachObjPropStr.replaceAll("\"", "");
            //logger.debug("matched str for entity: " + eachObjPropStr);
            IRI iri = createEntityIRI(eachObjPropStr);
            if (null != iri) {
                entityIRIs.add(iri);
            } else {
                return null;
            }

        }
        return entityIRIs;
    }


    /**
     * Create an valid IRI based on the given string. Given name must be ex:ob1 or :ob1 or ob1 format format.
     * if only ob1 is given then it must be full name. like www.http://hcbd.org#indi
     *
     * @param entityFullName: String. format must be ex:indi or :indi or indi.
     * @return IRI
     */
    public static IRI createEntityIRI(String entityFullName) throws MalFormedIRIException {
        String prefix;
        String suffix;
        String[] parts;
        // parts[0] is prefix
        // parts[1] is actual name.

        if (entityFullName.contains(":") && !entityFullName.contains("#")) {
            parts = entityFullName.split(":");
            if (parts.length == 2) {
                // shortname.length must be greater than 0
                if (parts[1].length() == 0) {
                    logger.error("suffix length in entity name is zero. given name: " + entityFullName);
                    return null;
                } else {

                    suffix = parts[1];
                    // if parts[0] is empty or "" then it will get the default prefix from the prefixes hashmap.
                    prefix = ConfigParams.prefixes.get(parts[0]);
                    return createEntityIRI(prefix, suffix);
                }
            } else {
                // it is malformed syntax.
                logger.error("Malformed Syntax in entity name. given name: " + entityFullName);
                throw new MalFormedIRIException("Malformed Syntax in entity name. given name: " + entityFullName);
            }
        } else {
            // full name is given
            return IRI.create(entityFullName);
        }
    }


    /**
     * Create an valid IRI based on the given prefix and suffix.
     *
     * @param prefix
     * @param suffix
     * @return IRI
     */
    public static IRI createEntityIRI(String prefix, String suffix) {
        if (prefix.length() > 0 && suffix.length() > 0) {
            String validName = "";
            if (prefix.endsWith("#") && suffix.startsWith("#")) {
                suffix = suffix.substring(1, prefix.length());
                validName = prefix + suffix;
            } else if ((prefix.endsWith("#") && (!suffix.startsWith("#"))) || ((!prefix.endsWith("#")) && suffix.startsWith("#"))) {
                validName = prefix + suffix;
            } else {
                validName = prefix + "#" + suffix;
            }
            IRI iri = IRI.create(validName);
            return iri;
        } else {
            return null;
        }
    }


    /**
     * Read pos examples from the conf confFile
     *
     * @param confFileFullContent
     * @return HashSet of OWLNamedIndividual
     * @throws IOException
     */
    public static HashSet<OWLNamedIndividual> readPosExamplesFromConf(String confFileFullContent) throws IOException, MalFormedIRIException {
        //logger.debug("Reading posExamples from full content: " + confFileFullContent);
        HashSet<OWLNamedIndividual> posIndivs = new HashSet<OWLNamedIndividual>();

        String inputText = confFileFullContent;

        String regexText = "(lp.positiveExamples){1}(\\s)*=(\\s)*\\{{1}([^}]|[\\s])*}{1}";
        Pattern pattern = Pattern.compile(regexText);
        Matcher matcher = pattern.matcher(inputText);

        //logger.debug("inputText: " + inputText);
        //logger.debug("regexText: " + regexText);

        String posExamplesPortion = "";
        if (matcher.find()) {
            posExamplesPortion = matcher.group();
            //logger.debug("\n\n\nposExamplesPortion: " + posExamplesPortion);
        }


        String regexEachEntity = "\"{1}([^\"])*\"{1}";
        ArrayList<IRI> posExamplesIRI = extractEachEntityIRIFromTextPortion(posExamplesPortion, regexEachEntity);

        posExamplesIRI.forEach(iri -> {
            OWLNamedIndividual eachIndi = OWLManager.getOWLDataFactory().getOWLNamedIndividual(iri);
            posIndivs.add(eachIndi);
        });
        return posIndivs;
    }


    /**
     * Read neg examples from the conf confFile
     *
     * @param confFileFullContent
     * @return HashSet of OWLNamedIndividual
     * @throws IOException
     */
    public static HashSet<OWLNamedIndividual> readNegExamplesFromConf(String confFileFullContent) throws IOException, MalFormedIRIException {

        //logger.debug("Reading negExamples from full content: " + confFileFullContent);
        HashSet<OWLNamedIndividual> negIndivs = new HashSet<OWLNamedIndividual>();


        String inputText = confFileFullContent;

        String regexText = "(lp.negativeExamples){1}(\\s)*=(\\s)*\\{{1}([^}]|[\\s])*}{1}";
        Pattern pattern = Pattern.compile(regexText);
        Matcher matcher = pattern.matcher(inputText);

        //logger.debug("inputText: " + inputText);
        //logger.debug("regexText: " + regexText);

        String negExamplesPortion = "";
        if (matcher.find()) {
            negExamplesPortion = matcher.group();
            //logger.debug("negExamplesPortion: " + negExamplesPortion);
        }

        String regexEachEntity = "\"{1}([^\"])*\"{1}";
        ArrayList<IRI> negExamplesIRI = extractEachEntityIRIFromTextPortion(negExamplesPortion, regexEachEntity);

        negExamplesIRI.forEach(iri -> {
            OWLNamedIndividual eachIndi = OWLManager.getOWLDataFactory().getOWLNamedIndividual(iri);
            negIndivs.add(eachIndi);
        });
        return negIndivs;
    }


    /**
     * @param indivs:    array of string. example: ex:indi1, ex:indi2
     * @param indivsSet: the set where indivs should be added.
     * @return
     */
    private static HashSet<OWLNamedIndividual> addIndivsToList(String[] indivs, HashSet<OWLNamedIndividual> indivsSet) {
        for (String eachIndi : indivs) {
            eachIndi = eachIndi.trim();
            // remove "ex: and last "
            eachIndi = eachIndi.substring(eachIndi.indexOf(":") + 1, eachIndi.length() - 1);
            IRI iri;
            if (ConfigParams.namespace.endsWith("#")) {
                iri = IRI.create(ConfigParams.namespace + eachIndi);
            } else {
                iri = IRI.create(ConfigParams.namespace + "#" + eachIndi);
            }
            indivsSet.add(OWLManager.getOWLDataFactory().getOWLNamedIndividual(iri));
        }
        return indivsSet;
    }


    /**
     * Read ontology path from conf confFile
     *
     * @param confFileFullContent
     * @throws IOException
     */
    public static String readOntoPathFromConf(String confFileFullContent) {

        //logger.debug("Reading ontoPath from full text: " + confFileFullContent);
        Scanner scanner = new Scanner(confFileFullContent);
        String ontoPath = "";
        try {
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if (line.startsWith("ks.fileName")) {
                    // remove first { and last }
                    // TODO: we should use regex as it doesnt conver newline inside the string.
                    ontoPath = line.substring(line.indexOf("=") + 1, line.length() - 1).trim().replaceAll("\"", "");
                    break;
                }
            }
        } catch (Exception ex) {
            logger.error("Error reading ontoPath from confFile. program exiting");
            logger.error("Fatal Error", ex);
            System.exit(-1);
        }

        if (ontoPath.endsWith(".owl")) {
            logger.info("OntoPath: " + ontoPath + " is valid ontoPath.");
            return ontoPath;
        } else {
            logger.error("OntoPath " + ontoPath + " is not valid ontoPath. program exiting");
            System.exit(-1);
        }

        return ontoPath;
    }


    /**
     * Removes concepts which are direct subclassof owl:Thing and don't have any subclass.
     * TODO:
     *
     * @param combinedOntology
     * @param owlReasoner
     * @return
     */
    public static OWLOntology removeNonRelatedConceptsV1(OWLOntology combinedOntology, OWLReasoner owlReasoner) {

        OWLEntityRemover entityRemover = new OWLEntityRemover(Collections.singleton(combinedOntology));

        combinedOntology.getClassesInSignature().forEach(owlClass -> {
            Optional<OWLClass> supClass = owlReasoner.getSuperClasses(owlClass, true).getFlattened().stream().findFirst();
            if (supClass.isPresent()) {
                OWLClass superClass = supClass.get();
                if (superClass.isOWLThing()) {
                    if (owlReasoner.getSubClasses(owlClass, true).getFlattened().size() < 2) {
                        // logger.info("removing owlclass: "+ Utility.getShortName(owlClass));
                        entityRemover.visit(owlClass);
                    } else {
                        logger.info("can not remove owlclass: " + Utility.getShortName(owlClass));
                    }
                }
            }
        });

        ChangeApplied ca = combinedOntology.getOWLOntologyManager().applyChanges(entityRemover.getChanges());
        if (logger != null)
            logger.info("Removing " + ca.toString());

        return combinedOntology;
    }


    /**
     * keep those concepts which are type of positive or negative objects.
     * alternatively keep
     * those concepts which have at-least a single instance.
     * TODO: FIX
     *
     * @param combinedOntology
     * @param owlReasoner
     * @return
     */
    public static OWLOntology removeNonRelatedConceptsV2(OWLOntology combinedOntology, OWLReasoner owlReasoner) {

        OWLEntityRemover entityRemover = new OWLEntityRemover(Collections.singleton(combinedOntology));

        combinedOntology.getClassesInSignature().forEach(owlClass -> {
            if (owlReasoner.getInstances(owlClass, false).getFlattened().size() < 1) {
                entityRemover.visit(owlClass);
            }
        });

        ChangeApplied ca = combinedOntology.getOWLOntologyManager().applyChanges(entityRemover.getChanges());
        if (logger != null)
            logger.info("Removing " + ca.toString());

        return combinedOntology;
    }


    /**
     * Removes concepts which are not
     * 1. class type of positive objects or negative objects or
     * 2. subClass type of positive objects or negative objectsor
     * 3. superclass type of positive objects or negative objects .
     * <p>
     * TODO: FIX
     *
     * @param combinedOntology
     * @param owlReasoner
     * @return
     */
//    public static OWLOntology removeNonRelatedConceptsV3(OWLOntology combinedOntology, OWLReasoner owlReasoner) {
//
//        OWLEntityRemover entityRemover = new OWLEntityRemover(Collections.singleton(combinedOntology));
//
//        combinedOntology.classesInSignature().forEach(owlClass -> {
//            if (SharedDataHolder.typeOfObjectsInPosIndivs.containsKey(owlClass) || SharedDataHolder.typeOfObjectsInNegIndivs.containsKey(owlClass) || (owlClass.isOWLThing())) {
//
//            } else {
//
//            }
//        });
//
//        ChangeApplied ca = combinedOntology.getOWLOntologyManager().applyChanges(entityRemover.getChanges());
//        if (logger != null)
//            logger.info("Removing " + ca.toString());
//
//        return combinedOntology;
//    }


    /**
     * Removes non related properties.
     *
     * @param combinedOntology
     * @return
     */
    public static OWLOntology removeNonRelatedProperties(OWLOntology combinedOntology) {

        OWLEntityRemover entityRemover = new OWLEntityRemover(Collections.singleton(combinedOntology));

        combinedOntology.getObjectPropertiesInSignature().forEach(owlObjectProperty -> {
            if (!SharedDataHolder.objProperties.containsKey(owlObjectProperty)) {
                entityRemover.visit(owlObjectProperty);
            }
//            if (!owlObjectProperty.equals(SharedDataHolder.objPropImageContains)) {
//
//            }
        });

        ChangeApplied ca = combinedOntology.getOWLOntologyManager().applyChanges(entityRemover.getChanges());
        if (logger != null)
            logger.info("Removing " + ca.toString());

        return combinedOntology;
    }


    /**
     * Create named individual from each line of the text file.
     *
     * @return
     * @throws IOException
     * @deprecated
     */
    private static HashSet<OWLNamedIndividual> getIndivsFromTxtFile(String filePath) throws IOException, MalFormedIRIException {

        if (null != filePath) {
            String indiName;
            OWLNamedIndividual indi;
            HashSet<OWLNamedIndividual> indivs = new HashSet<OWLNamedIndividual>();
            BufferedReader br = new BufferedReader(new FileReader(new File(filePath)));

            while ((indiName = br.readLine()) != null) {
                IRI indiIRI = createEntityIRI(indiName);
                indi = OWLManager.getOWLDataFactory().getOWLNamedIndividual(indiIRI);
                indivs.add(indi);
            }
            return indivs;
        } else {
            throw new NullPointerException("Given filePath is null.");
        }
    }


    /**
     * Combination helper.
     * Take a list(n) and an integer r<n and create all combination of nCr.
     *
     * @param list
     * @param K1
     * @return ArrayList<ArrayList                                                                                                                                                                                                                                                               <                                                                                                                                                                                                                                                               OWLClassExpression>>
     */
    public static <T> ArrayList<ArrayList<T>> combinationHelper(ArrayList<T> list, int K1) {

        ArrayList<ArrayList<T>> result = null;

        try {
            if (list.size() <= K1) {
                result = new ArrayList<ArrayList<T>>();
                result.add(new ArrayList<>(list));
            } else if (K1 <= 0) {
                result = new ArrayList<ArrayList<T>>();
                result.add(new ArrayList<>());
            } else {
                ArrayList<T> sublist = new ArrayList<>(list.subList(1, list.size()));

                result = combinationHelper(sublist, K1);

                for (ArrayList<T> alist : combinationHelper(sublist, K1 - 1)) {
                    ArrayList<T> thelist = new ArrayList<>(alist);
                    thelist.add(list.get(0));
                    result.add(thelist);
                }
            }
        } catch (StackOverflowError ex) {
            ex.printStackTrace();
            logger.error("Memory overflow!!!!!!!!!!!!!!!!! ");
        }

        return result;
    }


    /**
     * Combines several collections of elements and create permutations of all of them, taking one element from each
     * collection, and keeping the same order in resultant lists as the one in original list of collections.
     *
     * <ul>Example
     * <li>Input  = { {a,b,c} , {1,2,3,4} }</li>
     * <li>Output = { {a,1} , {a,2} , {a,3} , {a,4} , {b,1} , {b,2} , {b,3} , {b,4} , {c,1} , {c,2} , {c,3} , {c,4} }</li>
     * </ul>
     *
     * @param collections Original list of collections which elements have to be combined.
     * @return Resultant collection of lists with all permutations of original list.
     */
    public static <T> Collection<List<T>> restrictedCombinationHelper(List<Collection<T>> collections) {
        if (collections == null || collections.isEmpty()) {
            return Collections.emptyList();
        } else {
            Collection<List<T>> res = Lists.newLinkedList();
            permutationsImpl(collections, res, 0, new LinkedList<T>());
            return res;
        }
    }

    /**
     * Utility method for Recursive implementation
     */
    private static <T> void permutationsImpl(List<Collection<T>> ori, Collection<List<T>> res, int d, List<T> current) {
        // if depth equals number of original collections, final reached, add and return
        if (d == ori.size()) {
            res.add(current);
            return;
        }

        // iterate from current collection and copy 'current' element N times, one for each element
        Collection<T> currentCollection = ori.get(d);
        for (T element : currentCollection) {
            List<T> copy = Lists.newLinkedList(current);
            copy.add(element);
            permutationsImpl(ori, res, d + 1, copy);
        }
    }


    /**
     * Return the correct path by resolving relative or absoluteness. root dir is user.dir
     *
     * @param path
     * @return
     */
    public static String getCorrectPath(String path) {

        if (path.startsWith("/")) {
            // path is absolute
            return path;
        } else {
            // path is relative
            return SharedDataHolder.programStartingDir.endsWith("/") ? SharedDataHolder.programStartingDir + path :
                    SharedDataHolder.programStartingDir + "/" + path;
        }
    }

    /**
     * Return the correct path by resolving relative or absoluteness. root dir is confdir
     *
     * @param confDir: the directory of the confPath
     * @param path
     * @return
     */
    public static String getCorrectPath(String confDir, String path) {

        if (path.startsWith("/")) {
            // path is absolute
            return path;
        } else {
            // path is relative
            return confDir.endsWith("/") ? confDir + path : confDir + "/" + path;
        }
    }


    /**
     * To test various methods
     *
     * @param args
     */
    public static void main(String[] args) {
        // TODO Auto-generated method stub

    }

    /**
     * OWLEntityRemover entityRemover = new
     * OWLEntityRemover(Collections.singleton(ontology)); for (OWLNamedIndividual
     * individual : ontology.getIndividualsInSignature(Imports.INCLUDED)) { if
     * (!individual.getIRI().getShortForm().contains("_Indi_")) {
     * entityRemover.visit(individual); } }
     * ontologyManager.applyChanges(entityRemover.getChanges());
     */

}
