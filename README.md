# ecii
Learning description from examples: An efficient approach to analyzing big data

This repository contains the source code of ECII.


## How to run the program:
    Program runs in two mode. Batch mode and single mode. In single mode it will take a config file as input parameter and run the program as mentioned by the parameters in config file.
    
    In Batch mode it take directory as parameter and will run all the config files within that directory.

    #### For single mode:
        java -jar ecii.jar config_file

    #### For Batch mode:
        java -jar ecii.jar -b directory


## How to write Config file:
    Config file is a text file with user defined parameters and must end with .config
    Parameters are written as key, value pair.

    #### Parameters:
        // default namespace
        namespace : required 

        // K1/negExprTypeLimit, limit of number of concepts in a negative expression of a hornClause
        conceptLimitInNegExpr : integer, optional, default 3

        // K2/hornClauseLimit
        hornClauseLimit : integer, optional, default 3

        // K3/permutate/combination untill this number of objectproperties
        objPropsCombinationLimit: integer, optional, default 3

        // K5 select upto k5 hornClauses to make combination
        hornClausesListMaxSize: integer, optional, default 50

        // K6 select upto k6 candidate classes to make combination
        candidateClassesListMaxSize: integer, optional, default 50

        // declare some prefixes to use as abbreviations
        prefixes : map/dictionary, required 

        // knowledge source definition/owl_file_name
        ks.fileName : string, required

        // reasoner name. possible is hermit and pellet. default is pellet
        reasoner.reasonerImplementation : string, optional, default pellet

        // object properties to consider
        objectProperties : map/dictinary, optional

        // remove common atomic types. Those types appeared in both positive and negative individuals 
        removeCommonTypes: boolean, optional, default true.

        // positive examples
        lp.positiveExamples : array, required
        // negative examples
        lp.negativeExamples : array, required 


## Example of a config file:
    // default namespace
    namespace=http://www.daselab.org/ontologies/ADE20K/hcbdwsu#

    // K1/negExprTypeLimit, limit of number of concepts in a negative expression of a hornClause
    conceptLimitInNegExpr=3

    // K2/hornClauseLimit
    hornClauseLimit=3

    // K3/permutate/combination untill this number of objectproperties
    objPropsCombinationLimit=3

    // K5 select upto k5 hornClauses to make combination
    hornClausesListMaxSize=50

    // K6 select upto k6 candidate classes to make combination
    candidateClassesListMaxSize=50

    // declare some prefixes to use as abbreviations
    prefixes = [ ("ex","http://www.xyz.org/ontologies/ADE20K/hcbdwsu#") ]

    // knowledge source definition
    ks.type = "OWL File"
    ks.fileName = "ade20k_aligned_with_sumo.owl"

    // reasoner
    reasoner.reasonerImplementation=elk

    // object properties
    objectProperties={":imageContains"}

    // atomic types both appeared in positive and negative
    removeCommonTypes=true


    lp.positiveExamples = {"ex:highway_ADE_train_00008988","ex:highway_ADE_train_00008989","ex:highway_ADE_train_00008990","ex:highway_ADE_train_00008991","ex:highway_ADE_train_00008992" }

    lp.negativeExamples = { "ex:airport_terminal_ADE_train_00000001","ex:bathroom_ADE_train_00000006","ex:bedroom_ADE_train_00000192","ex:building_facade_ADE_train_00004593","ex:conference_room_ADE_train_00000570","ex:corridor_ADE_train_00000574","ex:dining_room_ADE_train_00006845","ex:hotel_room_ADE_train_00009520","ex:kitchen_ADE_train_00000594","ex:living_room_ADE_train_00000651","ex:mountain_snowy_ADE_train_00000932","ex:office_ADE_train_00000933","ex:street_ADE_train_00016858", "ex:skyscraper_ADE_train_00000954"}
