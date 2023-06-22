# bachelor_thesis

This repository contains the code for my bachelor thesis.

Under src/main/resources the OWL files for the ontologies can be found.
Under src/main/java/uniulm/aiinstitute the JAVA file containing the algorithm for the extraction of knowledge from the configuration ontology and the generation of HTN planning problem descriptions can be found.

Attention: The code in its current state is applied to the configuration ontology "config-leticia-ds", the running example. If you desire to apply the algorithm to a different configuration ontology, please consider adapting the code as follows:

1) Please change the file name of the configuration ontology in line 60 to the file name of the desired configuration ontology.

2) If you desire to apply the code to a configuration ontology that represents a recursive planning problem, marked with "rec", please swap line 204 with line 205.