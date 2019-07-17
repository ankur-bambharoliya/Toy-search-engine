Implementation:

 The code is implemented in two languages java 11 and python3. The evaluation and BM25 base model is implemented in python and rest of the project is implemented in java using lucene.

Python Libs:
    - nltk
    - plotly
    - jupyter

Java dependencies:
    - Lucene 8.0.0. However we are shipping those dependencies with the code in the java/libs folder.


Compile Java:
    - cd java
    -  javac -d ./build -cp libs/*: ./src/**/*.java ./src/*.java

How To Run:
    * To run pre processing
       - before running the code create the directory where you want to store the pre processed documents
       - cd java
       - java -cp libs/*:build/ Preprocess


    * To parse the stemmed document file
       - before running the code, create the directory where you want to store the parsed stemmed documents
       - cd java
       - java -cp libs/*:build/ StemFileParser

     * To run BM25 model
        -  create index by running python/BM25/index.py file. You can change the corpus path by changing the first               argument of the creatInvertedIndex function at the last line

        - create BM25 index by running python/BM25/BM25.py file

     * To run any other model

        - java -cp libs/*:build/ Main
        - Follow the instructions

     * To run the evaluation
        -run python/evaluate.py
        - it will create recall vs precision graph and python_rankings_eval.txt file

     * To run spell checker

        - run java -cp libs/*:build/ util/SpellCorrector


