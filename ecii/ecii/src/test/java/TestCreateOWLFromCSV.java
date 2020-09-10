/*
Written by sarker.
Written at 4/22/20.
*/

import org.dase.ecii.ontofactory.create.CreateOWLFromCSV;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Paths;

public class TestCreateOWLFromCSV {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static void main(String[] args) {

//        String csvPath = "/Users/sarker/Workspaces/Jetbrains/residue/data/7_IFPs/Entities/Entities_274.csv";

        try {

            Files.walk(Paths.get("/Users/sarker/Workspaces/Jetbrains/residue/experiments/7_IFP/Entities_With_Ontology/tmp"))
                    .filter(path -> path.toFile().isFile() && path.toString().endsWith(".csv"))
                    .forEach(path -> {
                        CreateOWLFromCSV createOWLFromCSV = null;
                        try {
                            logger.info("processing csv file: " + path);
                            createOWLFromCSV = new CreateOWLFromCSV(path.toString(),
                                    "talksAbout",
                                    "http://www.daselab.com/residue/analysis",
                                    true,
                                    "#");
                        } catch (OWLOntologyCreationException e) {
                            e.printStackTrace();
                        }

                        createOWLFromCSV.parseCSVToCreateIndivAndTheirTypes(
                                "local_onto_resource",
                                "local_onto_types",
                                true,
                                "indiv_");
                    });

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
