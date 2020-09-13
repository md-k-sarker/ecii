package org.dase.test;
/*
Written by sarker.
Written at 7/30/18.
*/

import org.dase.ecii.exceptions.MalFormedIRIException;
import org.dase.ecii.core.SharedDataHolder;
import org.dase.ecii.util.ConfigParams;
import org.dase.ecii.util.Utility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestConfParsing {

    final static Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
    // log level: ALL < DEBUG < INFO < WARN < ERROR < FATAL < OFF

    /**
     * Test whether the parsing is working or not
     */
    public static void testReadExamplesFromConf() {
        String inputText = "// reasoner\n" +
                "reasoner.type = \"closed world reasoner\"\n" +
                "reasoner.sources = { ks }\n" +
                "\n" +
                "// learning problem\n" +
                "lp.type = \"posNegStandard\"\n" +
                "lp.positiveExamples = {\n" +
                "\"kb:art\",\n" +
                "\"kb:calvin\",\n" +
                "\"kb:carlos\",\n" +
                "\"kb:david\",\n" +
                "\"kb:eric\",\n" +
                "\"kb:fred\",\n" +
                "\"kb:frederick\",\n" +
                "\"kb:george\"\n" +
                "}\n" +
                "lp.negativeExamples = {\n" +
                "\"kb:alfred\",\n" +
                "\"kb:alice\",\n" +
                "\"kb:angela\",\n" +
                "\"kb:bob\",\n" +
                "\"kb:carl\",\n" +
                "\"kb:christy\",\n" +
                "\"kb:karl\"\n" +
                "}\n" +
                "\n" +
                "alg.type = \"ocel\"";

        String inputTextWithoutNewLine = "// reasoner\n" +
                "reasoner.type = \"closed world reasoner\"\n" +
                "reasoner.sources = { ks }\n" +
                "\n" +
                "// learning problem\n" +
                "lp.type = \"posNegStandard\"\n" +
                "lp.positiveExamples = {" +
                "\"kb:art\"," +
                "\"kb:calvin\"," +
                "\"kb:carlos\"," +
                "\"kb:david\"," +
                "\"kb:eric\"," +
                "\"kb:fred\"," +
                "\"kb:frederick\"," +
                "\"kb:george\"" +
                "}\n" +
                "lp.negativeExamples = {" +
                "\"kb:alfred\"," +
                "\"kb:alice\"," +
                "\"kb:angela\"," +
                "\"kb:bob\"," +
                "\"kb:carl\"," +
                "\"kb:christy\"," +
                "\"kb:karl\"" +
                "}\n" +
                "\n" +
                "alg.type = \"ocel\"";

        String regex = "(lp.positiveExamples){1}(\\s)*=(\\s)*\\{{1}([^}]|[\\s])*}{1}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(inputTextWithoutNewLine);

        logger.info("inputTextWithoutNewLine: " + inputTextWithoutNewLine);
        logger.info("regex: " + regex);

        String examplesPortion = "";
        while (matcher.find()) {
            examplesPortion = matcher.group();
            logger.info("examplesPortion: " + examplesPortion);
        }

        String regexEachExample = "\"{1}([^\"])*\"{1}";
        pattern = Pattern.compile(regexEachExample);
        matcher = pattern.matcher(examplesPortion);

        String eachIri = "";
        while (matcher.find()) {
            eachIri = matcher.group();
            eachIri = eachIri.replaceAll("\"", "");
            logger.info("matched indi: " + eachIri);
        }
    }

    private static void testParsing(){
        try {
            logger.debug("ObjectProperties:");
            Utility.readObjectPropsFromConf(SharedDataHolder.confFileFullContent, ConfigParams.delimiterOntoEntityIRI).forEach((owlObjectProperty, aDouble) -> {
                logger.debug(owlObjectProperty.getIRI().toString() + " " + aDouble.toString());
            });

            logger.debug("Positive Individuals:");
            Utility.readPosExamplesFromConf(SharedDataHolder.confFileFullContent, ConfigParams.delimiterOntoEntityIRI).forEach(owlNamedIndividual -> {
                logger.debug(owlNamedIndividual.getIRI().toString());
            });

            logger.debug("Negative Individuals:");
            Utility.readNegExamplesFromConf(SharedDataHolder.confFileFullContent, ConfigParams.delimiterOntoEntityIRI).forEach(owlNamedIndividual -> {
                logger.debug(owlNamedIndividual.getIRI().toString());
            });
        }catch (MalFormedIRIException ex){
            logger.error("failed ", ex);
        }catch (IOException ex){
            logger.error("failed ", ex);
        }
    }


}
