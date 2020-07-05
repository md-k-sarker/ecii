package org.dase.ecii.ontofactory.create;

import org.dase.ecii.util.ConfigParams;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.ChangeApplied;
import org.semanticweb.owlapi.vocab.XSDVocabulary;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Create Ontology from the conf file.
 */
public class CreateOWLFromADE20k {

    // public static String ConfigParams.namespace =
    // "http://www.daselab.org/ontologies/ADE20K/hcbdwsu#";
    public static String rootPath = "/Users/sarker/Workspaces/ProjectHCBD/datas/ade20k_extended/training/";
    //public static String rootPathNingManual = "/home/sarker/MegaCloud/ProjectHCBD/datas/ning_manual/DL_tensorflow_save_v3_txts_as_dirs_owl_without_score_without_wordnet/";
    // "/home/sarker/MegaCloud/ProjectHCBD/datas/ADE20K/images/"

    public static String partLevelDataPropertyName = "partLevel";
    public static String isOccludedDataPropertyName = "isOccluded";
    public static String hasAttributeDataPropertyName = "hasAttribute";
    public static String imageContainsObjPropertyName = "imageContains";
    public static String imageContainsObjPropertyName0_100 = "imageContains0_100";
    public static String imageContainsObjPropertyName0_90 = "imageContains0_90";
    public static String imageContainsObjPropertyName0_80 = "imageContains0_80";
    public static String imageContainsObjPropertyName0_70 = "imageContains0_70";
    public static String imageContainsObjPropertyName0_60 = "imageContains0_60";
    public static String imageContainsObjPropertyName0_50 = "imageContains0_50";
    public static String imageContainsObjPropertyName0_40 = "imageContains0_40";
    public static String imageContainsObjPropertyName0_30 = "imageContains0_30";
    public static String imageContainsObjPropertyName0_20 = "imageContains0_20";
    public static String imageContainsObjPropertyName0_10 = "imageContains0_10";

    public static int counter = 0;

    /**
     * private constructor
     */
    public CreateOWLFromADE20k() {

    }

    /**
     * Main method
     *
     * @param args
     */
    public static void main(String[] args) {
        try {

            CreateOWLFromADE20k createOWLFromADE20k = new CreateOWLFromADE20k();

            Files.walk(Paths.get(rootPath)).
                    filter(f -> f.toFile().isFile() && f.toFile().getAbsolutePath().endsWith(".txt")).
                    forEach(f -> {
                        try {
                            SuperClassFacilitator superClassFacilitator = createOWLFromADE20k.createSuperClassName(f);
                            createOWLFromADE20k.createOWL(superClassFacilitator.path,
                                    superClassFacilitator.owlClassName,
                                    superClassFacilitator.shouldCreateSuperClass,
                                    superClassFacilitator.owlSuperClassName);
                            createOWLFromADE20k.printStatus(superClassFacilitator.path.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                            System.out.println("Error Occurred: " + e.toString());
                        }
                    });

            // createOWL(Paths.get(rootPathNingManual));
            // Files.walk(Paths.get(rootPath)).filter(f -> f.toFile().isFile())
            // .filter(f -> f.toFile().getAbsolutePath().endsWith(".txt")).forEach(f ->
            // createSuperClassName(f));
            // createOWL(
            // "D:/QQDownload/ADE20K_2016_07_26/ADE20K_2016_07_26/images/training/a/abbey/ADE_train_00000970_atr.txt",
            // "Abbey");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param path
     */
    /**
     * Create class name and super class name by taking the path name of a text file.
     * This is written for the ADE20K images atrribute files.
     *
     * @param path the path of the text file which has ade20k like directory structures. has attributes
     * @return SuperClassFacilitator
     */
    public SuperClassFacilitator createSuperClassName(Path path) {
        String parent = path.getParent().getFileName().toString();
        // added on may 13
        String[] parts = parent.split("_");
        parent = "";
        for (int i = 0; i < parts.length; i++) {
            parent = parent + parts[i].substring(0, 1).toUpperCase() + parts[i].substring(1).toLowerCase();
        }
        //parent = parent.substring(0, 1).toUpperCase() + parent.substring(1).toLowerCase();

        String grandParent = path.getParent().getParent().getFileName().toString();
        String[] gParts = grandParent.split("_");
        grandParent = "";
        for (int i = 0; i < gParts.length; i++) {
            grandParent = grandParent + gParts[i].substring(0, 1).toUpperCase() + gParts[i].substring(1).toLowerCase();
        }
        //grandParent = grandParent.substring(0, 1).toUpperCase() + grandParent.substring(1).toLowerCase();

        String owl_class_name = "";
        String owl_super_class_name = "";
        boolean shouldCreateSuperClass = false;

        // Condition
        // If parent name is misc, then parent folder name is misc
        // If grandparent is not a....z or outliers then class name should be
        // parent_name and grand_parent_name
        if (parent.equals("misc")) {
            owl_class_name = "misc";
        } else if ((grandParent.length() == 1 || grandParent.equals("outliers"))) {
            owl_class_name = parent;
        } else if ((parent.equalsIgnoreCase("Outdoor") || parent.equalsIgnoreCase("Indoor"))) {
            owl_class_name = grandParent;
        } else {
            owl_class_name = parent;
            owl_super_class_name = grandParent;
            shouldCreateSuperClass = true;
            System.out.println("else condition: " + "\n\towl_class_name: " + owl_class_name + "\n\towl_super_class_name: " + owl_super_class_name);
        }

        SuperClassFacilitator superClassFacilitator = new SuperClassFacilitator(
                path,
                owl_class_name,
                shouldCreateSuperClass,
                owl_super_class_name);

        return superClassFacilitator;
    }

    /**
     * Internal class to facilitate the parameter passing of different methods.
     */
    private class SuperClassFacilitator {
        public Path path;
        public String owlClassName;
        public boolean shouldCreateSuperClass;
        public String owlSuperClassName;

        public SuperClassFacilitator(Path path, String owlClassName, boolean shouldCreateSuperClass, String owlSuperClassName) {
            if (null != path && owlClassName != null && owlSuperClassName != null) {
                this.path = path;
                this.owlClassName = owlClassName;
                this.shouldCreateSuperClass = shouldCreateSuperClass;
                this.owlSuperClassName = owlSuperClassName;
            }
        }
    }

    /**
     * @param status
     */
    public void printStatus(String status) {
        try {
            counter++;
            System.out.println("creating owl from file: " + status + " is successfull");
            System.out.println("Processed " + counter + " files");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * @param score
     * @param owlDataFactory
     * @return
     */
    public static OWLObjectProperty getObjectProperty(Double score, OWLDataFactory owlDataFactory) {
        if (score < 0 || score > 1) {
            return null;
        } else {
            IRI iriObjectProp = null; // IRI.create(ConfigParams.namespace + imageContainsObjPropertyName);
            if (score > 0 && score <= 0.1) {
                iriObjectProp = IRI.create(ConfigParams.namespace + imageContainsObjPropertyName0_10);
            } else if (score > 0.1 && score <= 0.2) {
                iriObjectProp = IRI.create(ConfigParams.namespace + imageContainsObjPropertyName0_20);
            }
            if (score > 0.2 && score <= 0.3) {
                iriObjectProp = IRI.create(ConfigParams.namespace + imageContainsObjPropertyName0_30);
            } else if (score > 0.3 && score <= 0.4) {
                iriObjectProp = IRI.create(ConfigParams.namespace + imageContainsObjPropertyName0_40);
            }
            if (score > 0.4 && score <= 0.5) {
                iriObjectProp = IRI.create(ConfigParams.namespace + imageContainsObjPropertyName0_50);
            } else if (score > 0.5 && score <= 0.6) {
                iriObjectProp = IRI.create(ConfigParams.namespace + imageContainsObjPropertyName0_60);
            }
            if (score > 0.6 && score <= 0.7) {
                iriObjectProp = IRI.create(ConfigParams.namespace + imageContainsObjPropertyName0_70);
            } else if (score > 0.7 && score <= 0.8) {
                iriObjectProp = IRI.create(ConfigParams.namespace + imageContainsObjPropertyName0_80);
            }
            if (score > 0.8 && score <= 0.9) {
                iriObjectProp = IRI.create(ConfigParams.namespace + imageContainsObjPropertyName0_90);
            } else if (score > 0.9 && score <= 1.0) {
                iriObjectProp = IRI.create(ConfigParams.namespace + imageContainsObjPropertyName0_100);
            }

            OWLObjectProperty owlObjPropImageContains = owlDataFactory.getOWLObjectProperty(iriObjectProp);
            return owlObjPropImageContains;
        }
    }

    /*
     * Generate OWL File with with respect to score in annotations
     * Hard coded to work for atribute files matching with ADE20K images attributes
     */
    public static void createOWL(Path filePath, String ontoIRI) throws Exception {

        File f = filePath.toFile();
        String imageName = f.getName().replaceAll("_atr.txt", "");
        String folderName = filePath.getParent().getFileName().toString();
        if (folderName.endsWith("_")) {
            folderName = folderName.substring(0, folderName.length() - 1);
        }

        // if (!folderName.endsWith("_")) {
        //
        // }
        System.out.println("debug: imageTxtName: " + imageName);
        System.out.println("debug: folderName: " + folderName);

        // Create Ontology
        OWLOntologyManager owlManager = OWLManager.createOWLOntologyManager();

        OWLDataFactory owlDataFactory = owlManager.getOWLDataFactory();

        IRI ontologyIRI = IRI.create(ontoIRI + imageName);

        String temp = f.getAbsolutePath().replaceAll("_atr.txt", ".owl");
        // for windows os
        String diskFileName = temp.replace("\\", "/");
        IRI owlDiskFileIRI = IRI.create("file:" + diskFileName);
        OWLDeclarationAxiom dAxiom = null;
        OWLClassAssertionAxiom clsAAxiom = null;

        // System.out.println("debug: temp: "+ temp);
        // System.out.println("debug: diskFileName: "+ diskFileName);
        // System.out.println("debug: owlDiskFileIRI: " + owlDiskFileIRI);
        OWLOntology ontology = owlManager.createOntology(ontologyIRI);
        // System.out.println("created ontology: " + ontology);

        // create individual
        IRI iriIndi = IRI.create(ConfigParams.namespace + imageName);
        OWLNamedIndividual namedIndiImage = owlDataFactory.getOWLNamedIndividual(iriIndi);
        // add to ontology
        clsAAxiom = owlDataFactory.getOWLClassAssertionAxiom(owlDataFactory.getOWLThing(), namedIndiImage);
        owlManager.addAxiom(ontology, clsAAxiom);

        // create class from folderName
        IRI iriClass = IRI.create(ConfigParams.namespace + folderName);
        OWLClass owlClass = owlDataFactory.getOWLClass(iriClass);
        // add to ontology
        dAxiom = owlDataFactory.getOWLDeclarationAxiom(owlClass);
        owlManager.addAxiom(ontology, dAxiom);

        // OWLDataMinCardinality owlDataMinCardinality =
        // owlDataFactory.getOWLDataMinCardinality(0,
        // owlDataPropertyPartLevel);
        // OWLAxiom axiom =
        // owlDataFactory.getOWLDataPropertyRangeAxiom(owlDataPropertyPartLevel,
        // owlDataFactory.getIntegerOWLDatatype());
        // AddAxiom addAxiom = new AddAxiom(ontology, axiom);
        // owlManager.applyChange(addAxiom);

        // create data property
        IRI iriDataProperty = IRI.create(ConfigParams.namespace + partLevelDataPropertyName);
        OWLDataProperty owlDataPropertyPartLevel = owlDataFactory.getOWLDataProperty(iriDataProperty);
        OWLDataMinCardinality owlDataMinCardinality = owlDataFactory.getOWLDataMinCardinality(0,
                owlDataPropertyPartLevel);
        OWLAxiom axiom = owlDataFactory.getOWLDataPropertyRangeAxiom(owlDataPropertyPartLevel,
                owlDataFactory.getIntegerOWLDatatype());
        // AddAxiom addAxiom = new AddAxiom(ontology, axiom);
        owlManager.addAxiom(ontology, axiom);

        iriDataProperty = IRI.create(ConfigParams.namespace + isOccludedDataPropertyName);
        OWLDataProperty owlDataPropertyIsOccluded = owlDataFactory.getOWLDataProperty(iriDataProperty);
        axiom = owlDataFactory.getOWLDataPropertyRangeAxiom(owlDataPropertyIsOccluded,
                owlDataFactory.getBooleanOWLDatatype());
        // addAxiom = new AddAxiom(ontology, axiom);
        owlManager.addAxiom(ontology, axiom);

        iriDataProperty = IRI.create(ConfigParams.namespace + hasAttributeDataPropertyName);
        OWLDataProperty owlDataPropertyHasAttribute = owlDataFactory.getOWLDataProperty(iriDataProperty);
        axiom = owlDataFactory.getOWLDataPropertyRangeAxiom(owlDataPropertyHasAttribute,
                owlDataFactory.getOWLDatatype(XSDVocabulary.STRING.getIRI()));
        // addAxiom = new AddAxiom(ontology, axiom);
        owlManager.addAxiom(ontology, axiom);

        // assign individual to class
        // do not assign it the corresponding class, instead assign it to OWL:Thing
        System.out.println("Individual Name: " + namedIndiImage.getIRI().toString());
        OWLClassAssertionAxiom owlClassAssertionAxiom = owlDataFactory
                .getOWLClassAssertionAxiom(owlDataFactory.getOWLThing(), namedIndiImage);
        // addAxiom = new AddAxiom(ontology, owlClassAssertionAxiom);
        owlManager.addAxiom(ontology, owlClassAssertionAxiom);

        // Read files and Parse data
        FileReader reader = new FileReader(filePath.toString());
        BufferedReader bfr = new BufferedReader(reader);

        String line;
        // Set<String> terms = new HashSet<>();
        int lineCount = 0;

        while ((line = bfr.readLine()) != null) {
            lineCount++;
            // erase first 4 lines
            if (lineCount <= 4)
                continue;

            // this is a single line
            // example 017 # 0 # 0 # plant, flora, plant life # plants # ""
            String[] column = line.split("#");

            for (int i = 0; i < column.length; i++) {
                column[i] = column[i].trim();

            }

            // create namedIndividual
            // column[0]
            String rawClassesNamesForObject = column[4].trim().replace(",", "_");
            String instanceName = "obj_" + column[0] + "_" + column[1] + "_" + imageName + "_" + rawClassesNamesForObject;
            iriIndi = IRI.create(ConfigParams.namespace + instanceName);
            OWLNamedIndividual namedIndiObject = owlDataFactory.getOWLNamedIndividual(iriIndi);
            // column[6]
            // use score column , column[6] to get the score
            double objectScore = Double.valueOf(column[6]);
            // disregards objectScore.
            // as this is not giving good result.
            //OWLObjectProperty owlObjPropImageContains = getObjectProperty(objectScore, owlDataFactory);
            IRI iriObjectProp = IRI.create(ConfigParams.namespace + imageContainsObjPropertyName);
            OWLObjectProperty owlObjPropImageContains = owlDataFactory.getOWLObjectProperty(iriObjectProp);

            // Assign this objects to the image by using imagecontains object property
            OWLObjectPropertyAssertionAxiom owlObjectPropertyAssertionAxiom = owlDataFactory
                    .getOWLObjectPropertyAssertionAxiom(owlObjPropImageContains, namedIndiImage, namedIndiObject);
            owlManager.addAxiom(ontology, owlObjectPropertyAssertionAxiom);

            // create class and assign individual to class
            // Column[3] Wordnet
//			String[] classes = column[3].split(",");
//			for (String eachClass : classes) {
//
//				eachClass = eachClass.trim().replace(" ", "").replace("_", "");
//				eachClass = eachClass.substring(0, 1).toUpperCase() + eachClass.substring(1);
//				// create class
//				iriClass = IRI.create(ConfigParams.namespace + "WN_" + eachClass);
//				owlClass = owlDataFactory.getOWLClass(iriClass);
//				// assign individual to class
//				owlClassAssertionAxiom = owlDataFactory.getOWLClassAssertionAxiom(owlClass, namedIndiObject);
//				// addAxiom = new AddAxiom(ontology, owlClassAssertionAxiom);
//				owlManager.addAxiom(ontology, owlClassAssertionAxiom);
//			}

            // Column[4] Raw name
            String[] rawClasses = column[4].split(",");
            for (String eachClass : rawClasses) {

                eachClass = eachClass.trim().replace(" ", "").replace("_", "");
                eachClass = eachClass.substring(0, 1).toUpperCase() + eachClass.substring(1);
                // create class
                iriClass = IRI.create(ConfigParams.namespace + eachClass);
                owlClass = owlDataFactory.getOWLClass(iriClass);
                // assign individual to class
                owlClassAssertionAxiom = owlDataFactory.getOWLClassAssertionAxiom(owlClass, namedIndiObject);
                // addAxiom = new AddAxiom(ontology, owlClassAssertionAxiom);
                owlManager.addAxiom(ontology, owlClassAssertionAxiom);
            }

            // Column[1] part level
            // hasAttribute
            // this contains the part of relation
            // this can be merged without if-else
            // for our understanding keep this now.
            if (column[1].equals("0")) {
                axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(owlDataPropertyPartLevel, namedIndiObject,
                        Integer.parseInt(column[1]));
                // addAxiom = new AddAxiom(ontology, axiom);
                owlManager.addAxiom(ontology, axiom);
            } else {
                // column[1] = 1, 2, 3 or more
                // assign dataProperty
                axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(owlDataPropertyPartLevel, namedIndiObject,
                        Integer.parseInt(column[1]));
                // addAxiom = new AddAxiom(ontology, axiom);
                ChangeApplied ca = owlManager.addAxiom(ontology, axiom);
            }

            // Column[2]
            // isOccluded
            if (column[2].equals("0")) {
                axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(owlDataPropertyIsOccluded, namedIndiObject,
                        owlDataFactory.getOWLLiteral(false));
                // addAxiom = new AddAxiom(ontology, axiom);
                ChangeApplied ca = owlManager.addAxiom(ontology, axiom);

            } else if (column[2].equals("1")) {
                axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(owlDataPropertyIsOccluded, namedIndiObject,
                        owlDataFactory.getOWLLiteral(true));
                // addAxiom = new AddAxiom(ontology, axiom);
                ChangeApplied ca = owlManager.addAxiom(ontology, axiom);

            }

            // column[5] attributes
            column[5] = column[5].replaceAll("\"", "");
            if (column[5].length() > 0) {
                String[] attributes = column[5].split(",");
                for (String attribute : attributes) {
                    attribute = attribute.trim();
                    axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(owlDataPropertyHasAttribute,
                            namedIndiObject, owlDataFactory.getOWLLiteral(attribute));
                    // addAxiom = new AddAxiom(ontology, axiom);
                    ChangeApplied ca = owlManager.addAxiom(ontology, axiom);
                    // for (OWLOntologyChange eca : ca) {
                    // System.out.println("change: " + eca);
                    // }
                }
            }

        }

        // Save Ontology
        owlManager.saveOntology(ontology, owlDiskFileIRI);

        // System.out.println("saved on file: " + owlDiskFileIRI +
        // "\nSuccessfull");

    }

    /**
     * @param filePath
     * @param owl_class_name
     * @param shouldCreateSuperClass
     * @param owl_super_class_name
     * @throws Exception
     */
    public void createOWL(Path filePath, String owl_class_name, boolean shouldCreateSuperClass,
                          String owl_super_class_name) throws Exception {

        File f = filePath.toFile();
        String imageName = f.getName().replaceAll("_atr.txt", "");
        // System.out.println("debug: imageName: " + imageName);

        // Create Ontology
        OWLOntologyManager owlManager = OWLManager.createOWLOntologyManager();

        OWLDataFactory owlDataFactory = owlManager.getOWLDataFactory();

        IRI ontologyIRI = IRI.create(ConfigParams.namespace, imageName.replaceAll("_atr", ".owl"));

        String temp = f.getAbsolutePath().replaceAll("_atr.txt", ".owl");
        String diskFileName = temp.replace("\\", "/");
        IRI owlDiskFileIRI = IRI.create("file:" + diskFileName);

        // System.out.println("debug: temp: "+ temp);
        // System.out.println("debug: diskFileName: "+ diskFileName);
        // System.out.println("debug: owlDiskFileIRI: " + owlDiskFileIRI);
        OWLOntology ontology = owlManager.createOntology(ontologyIRI);
        // System.out.println("created ontology: " + ontology);

        // create individual
        IRI iriIndi = IRI.create(ConfigParams.namespace + owl_class_name + "_" + imageName);
        OWLNamedIndividual namedIndiImage = owlDataFactory.getOWLNamedIndividual(iriIndi);

        // create class
        IRI iriClass = IRI.create(ConfigParams.namespace + owl_class_name);
        OWLClass owlClass = owlDataFactory.getOWLClass(iriClass);
        OWLAxiom classAxiom = owlDataFactory.getOWLDeclarationAxiom(owlClass);
        AddAxiom classAddAxiom = new AddAxiom(ontology, classAxiom);
        owlManager.applyChange(classAddAxiom);

        // create super class
        if (shouldCreateSuperClass) {
            assert (owl_super_class_name.length() > 0);
            IRI iriSuperClass = IRI.create(ConfigParams.namespace + owl_super_class_name);
            OWLClass owlSuperClass = owlDataFactory.getOWLClass(iriSuperClass);
            classAxiom = owlDataFactory.getOWLDeclarationAxiom(owlSuperClass);
            classAddAxiom = new AddAxiom(ontology, classAxiom);
            owlManager.applyChange(classAddAxiom);

            OWLAxiom subClassOfAxiom = owlDataFactory.getOWLSubClassOfAxiom(owlClass, owlSuperClass);
            AddAxiom subClassAddAxiom = new AddAxiom(ontology, subClassOfAxiom);
            owlManager.applyChange(subClassAddAxiom);
        }

        // create object property
        IRI iriObjectProp = IRI.create(ConfigParams.namespace + imageContainsObjPropertyName);
        OWLObjectProperty owlObjPropImageContains = owlDataFactory.getOWLObjectProperty(iriObjectProp);
        OWLAxiom objPropAxiom = owlDataFactory.getOWLDeclarationAxiom(owlObjPropImageContains);
        AddAxiom objPropAddAxiom = new AddAxiom(ontology, objPropAxiom);
        owlManager.applyChange(objPropAddAxiom);
        // OWLDataMinCardinality owlDataMinCardinality =
        // owlDataFactory.getOWLDataMinCardinality(0,
        // owlDataPropertyPartLevel);
        // OWLAxiom axiom =
        // owlDataFactory.getOWLDataPropertyRangeAxiom(owlDataPropertyPartLevel,
        // owlDataFactory.getIntegerOWLDatatype());
        // AddAxiom addAxiom = new AddAxiom(ontology, axiom);
        // owlManager.applyChange(addAxiom);

        // create data property
        IRI iriDataProperty = IRI.create(ConfigParams.namespace + partLevelDataPropertyName);
        OWLDataProperty owlDataPropertyPartLevel = owlDataFactory.getOWLDataProperty(iriDataProperty);
        OWLDataMinCardinality owlDataMinCardinality = owlDataFactory.getOWLDataMinCardinality(0,
                owlDataPropertyPartLevel);
        OWLAxiom axiom = owlDataFactory.getOWLDataPropertyRangeAxiom(owlDataPropertyPartLevel,
                owlDataFactory.getIntegerOWLDatatype());
        AddAxiom addAxiom = new AddAxiom(ontology, axiom);
        owlManager.applyChange(addAxiom);

        iriDataProperty = IRI.create(ConfigParams.namespace + isOccludedDataPropertyName);
        OWLDataProperty owlDataPropertyIsOccluded = owlDataFactory.getOWLDataProperty(iriDataProperty);
        axiom = owlDataFactory.getOWLDataPropertyRangeAxiom(owlDataPropertyIsOccluded,
                owlDataFactory.getBooleanOWLDatatype());
        addAxiom = new AddAxiom(ontology, axiom);
        owlManager.applyChange(addAxiom);

        iriDataProperty = IRI.create(ConfigParams.namespace + hasAttributeDataPropertyName);
        OWLDataProperty owlDataPropertyHasAttribute = owlDataFactory.getOWLDataProperty(iriDataProperty);
        axiom = owlDataFactory.getOWLDataPropertyRangeAxiom(owlDataPropertyHasAttribute,
                owlDataFactory.getOWLDatatype(XSDVocabulary.STRING.getIRI()));
        addAxiom = new AddAxiom(ontology, axiom);
        owlManager.applyChange(addAxiom);

        // assign individual to class
        // do not assign it the corresponding class, instead assign it to OWL:Thing
        System.out.println("Individual Name: " + namedIndiImage.getIRI().toString());
        OWLClassAssertionAxiom owlClassAssertionAxiom = owlDataFactory
                .getOWLClassAssertionAxiom(owlDataFactory.getOWLThing(), namedIndiImage);
        addAxiom = new AddAxiom(ontology, owlClassAssertionAxiom);
        owlManager.applyChange(addAxiom);

        // Read files and Parse data
        FileReader reader = new FileReader(filePath.toString());
        BufferedReader bfr = new BufferedReader(reader);

        String line;
        // Set<String> terms = new HashSet<>();

        while ((line = bfr.readLine()) != null) {
            // this is a single line
            // example 017 # 0 # 0 # plant, flora, plant life # plants # ""
            String[] column = line.split("#");

            for (int i = 0; i < column.length; i++) {
                column[i] = column[i].trim();

            }

            // create object/namedIndividual
            // column[0]
            String rawClassesNamesForObject = column[4].trim().replace(",", "_");
            String instanceName = "obj_" + column[0] + "_" + column[1] + "_" + imageName + "_" + rawClassesNamesForObject;
            iriIndi = IRI.create(ConfigParams.namespace + instanceName);
            OWLNamedIndividual namedIndiObject = owlDataFactory.getOWLNamedIndividual(iriIndi);
            // Assign this objects to the image by using imagecontains object property
            OWLObjectPropertyAssertionAxiom owlObjectPropertyAssertionAxiom = owlDataFactory
                    .getOWLObjectPropertyAssertionAxiom(owlObjPropImageContains, namedIndiImage, namedIndiObject);
            addAxiom = new AddAxiom(ontology, owlObjectPropertyAssertionAxiom);
            owlManager.applyChange(addAxiom);

            // create class and assign individual to class
            // Column[3] Wordnet
            // skip column 3 for now. May 13. 2018
//			String[] classes = column[3].split(",");
//			for (String eachClass : classes) {
//
//				eachClass = eachClass.trim().replace(" ", "_");
//				eachClass = eachClass.substring(0, 1).toUpperCase() + eachClass.substring(1);
//				// create class
//				iriClass = IRI.create(ConfigParams.namespace + "WN_" + eachClass);
//				owlClass = owlDataFactory.getOWLClass(iriClass);
//				// assign individual to class
//				owlClassAssertionAxiom = owlDataFactory.getOWLClassAssertionAxiom(owlClass, namedIndiObject);
//				addAxiom = new AddAxiom(ontology, owlClassAssertionAxiom);
//				owlManager.applyChange(addAxiom);
//			}

            // Column[4] Raw name
            String[] rawClasses = column[4].split(",");
            for (String eachClass : rawClasses) {

                eachClass = eachClass.trim().replace(" ", "_");
                eachClass = eachClass.substring(0, 1).toUpperCase() + eachClass.substring(1);
                // create class
                iriClass = IRI.create(ConfigParams.namespace + eachClass);
                owlClass = owlDataFactory.getOWLClass(iriClass);
                // assign individual to class
                owlClassAssertionAxiom = owlDataFactory.getOWLClassAssertionAxiom(owlClass, namedIndiObject);
                addAxiom = new AddAxiom(ontology, owlClassAssertionAxiom);
                owlManager.applyChange(addAxiom);
            }

            // Column[1] part level
            // hasAttribute
            // this contains the part of relation
            // this can be merged without if-else
            // for our understanding keep this now.
            if (column[1].equals("0")) {
                axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(owlDataPropertyPartLevel, namedIndiObject,
                        Integer.parseInt(column[1]));
                addAxiom = new AddAxiom(ontology, axiom);
                owlManager.applyChange(addAxiom);
            } else {
                // column[1] = 1, 2, 3 or more
                // assign dataProperty
                axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(owlDataPropertyPartLevel, namedIndiObject,
                        Integer.parseInt(column[1]));
                addAxiom = new AddAxiom(ontology, axiom);
                owlManager.applyChange(addAxiom);
            }

            // Column[2]
            // isOccluded
            if (column[2].equals("0")) {
                axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(owlDataPropertyIsOccluded, namedIndiObject,
                        owlDataFactory.getOWLLiteral(false));
                addAxiom = new AddAxiom(ontology, axiom);
                ChangeApplied ca = owlManager.applyChange(addAxiom);

            } else if (column[2].equals("1")) {
                axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(owlDataPropertyIsOccluded, namedIndiObject,
                        owlDataFactory.getOWLLiteral(true));
                addAxiom = new AddAxiom(ontology, axiom);
                ChangeApplied ca = owlManager.applyChange(addAxiom);

            }

            // column[5] attributes
            column[5] = column[5].replaceAll("\"", "");
            if (column[5].length() > 0) {
                String[] attributes = column[5].split(",");
                for (String attribute : attributes) {
                    attribute = attribute.trim();
                    axiom = owlDataFactory.getOWLDataPropertyAssertionAxiom(owlDataPropertyHasAttribute,
                            namedIndiObject, owlDataFactory.getOWLLiteral(attribute));
                    addAxiom = new AddAxiom(ontology, axiom);
                    ChangeApplied ca = owlManager.applyChange(addAxiom);
                    // for (OWLOntologyChange eca : ca) {
                    // System.out.println("change: " + eca);
                    // }
                }
            }

        }

        // Save Ontology
        owlManager.saveOntology(ontology, new OWLXMLDocumentFormat(), owlDiskFileIRI);
        System.out.println("ontology has total " + ontology.getAxioms().size() + " axioms");
        System.out.println("saved on file: " + owlDiskFileIRI + "\nSuccessfull");
    }
}
