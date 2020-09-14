# ECII

Learning description from examples: An efficient approach for contextual data analysis.

Originally concept induction algorithm, where other functionality is added to provide:

- Contextual data analysis
- Similarity between natural language (sentence, paragraph, tweet etc).
- Provide insights of machine learning decisions.
- Identify new complex entities for knowledge graph.  

## Program options

1. Contextual data analysis (concept induction)
2. Measure similarity between ontology entities
3. Strip down ontology or keeping entities of interest while discarding others
4. Create ontology
5. Combine multiple ontologies

This repository contains the source code of ECII. 
Source code is in ecii/ecii directory.
Sample/example files is in examples directory.

## How to run the program

    Program can be run from source code directly or from jar. Running from source code is preferable, 
    as running from jar may produce memory limit exception for very big knowledge graph.  

## Tutorials

Contextual data analysis: https://github.com/md-k-sarker/ecii/wiki/Contextual-data-analysis-using-ECII

Strip down ontology: https://github.com/md-k-sarker/ecii/wiki/Strip-down-ontology

Create ontology: https://github.com/md-k-sarker/ecii/wiki/Create-Ontology-or-Knowledge-Graph

Combine ontologies: https://github.com/md-k-sarker/ecii/wiki/Combine-Ontology

## Details of the parameters

https://github.com/md-k-sarker/ecii/wiki/ECII-parameters


### Details of the algorithm

```latex
@inproceedings{sarker2019efficient,
  title={Efficient concept induction for description logics},
  author={Sarker, Md Kamruzzaman and Hitzler, Pascal},
  booktitle={Proceedings of the AAAI Conference on Artificial Intelligence},
  volume={33},
  pages={3036--3043},
  year={2019}
}
```
