# Design Document

## 1. Introduction

In this project, we will design and build our own information retrieval system, implementing multiple retrieval models and testing their capabilities under various circumstances and in conjunction with additional techniques that we have studied throughout the course of the semester.

We will then be evaluating the effectiveness of this retrieval system in various ways, benchmarking against established relevance judgments.

## 2. Techniques to be Used

* In the first task of the first phase of the project, we'll be implementing the following three retrieval models: TF/IDF, a Query Likelihood model with Dirichlet smoothing, and BM25-- and utilizing Lucene’s default retrieval model as well.

    We have chosen to implement TF/IDF and Query Likelihood with Dirichlet smoothing because we feel that this gives us an opportunity to compare two models with significant differences-- both in terms of complexity and effectiveness. We feel this might help us to better appreciate the importance of choosing the best model for the job at hand. Generally speaking, the Query Likelihood Model is a sophistication on the Binary Independence Model, and Dirichlet smoothing is a sophistication on Jelinek-Mercer smoothing, so we thought the results should be more dramatic using models with less underlying similarity.

    Here are some of the scholarly articles that we will be referencing in our analysis for this task.

    1. http://maroo.cs.umass.edu/getpdf.php?id=694
    2. http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.58.8978&rep=rep1&type=pdf
    3. https://www.academia.edu/2788177/An_investigation_of_dirichlet_prior_smoothing_s_performance_advantage
    4. http://citeseerx.ist.psu.edu/viewdoc/download?doi=10.1.1.165.8690&rep=rep1&type=pdf

* In the second task of the first phase of the project, we will be using query-time stemming and pseudo-relevance feedback as query-expansion techniques. In general, to be effective, a thesaurus should be constructed specifically for the collection under consideration. Without manually creating such a thesaurus for this project, these (query-time stemming and pseudo-relevance feedback) would be the more effective (and scalable) approaches. For query-time stemming, since the corpus was not stemmed at index time (with a Porter stemmer, for example), we’ll be using Kstemming instead, which expands the query itself by adding additional real words that correlate with the underlying semantic meaning of the original query terms-- rather than stripping the suffixes off of words so that they no longer match any of the real (non-stemmed) terms in the index.

    Here are some of the scholarly articles we will be referencing for our analysis of this task.

    http://lexicalresearch.com/kstem-doc.txt
http://ciir.cs.umass.edu/pubfiles/ir-35.pdf

*   In the second phase of the project, we will be either using Lucene’s built-in Highlighter class for snippet generation and highlighting-- or learning from this functionality for our own Python implementation. Lucene is a well-established information-retrieval resource, used widely throughout industry. By understanding the approach Lucene takes to snippet generation and highlighting, we hope to better understand the current state of the art.

*   In the third phase of the project, we will be combining stopping with Kstemming rather than pseudo-relevance feedback. Our thinking here is that pseudo-relevance feedback can be unreliable if the original ranking is unreliable, so we feel it would be more effective to enhance a more reliable technique, such as Kstemming, with stopping rather than a less reliable technique, such as pseudo-relevance feedback.

*   We will also be implementing the following retrieval evaluation techniques: Mean Average Precision, Mean Reciprocal Rank, Precision at K, and Precision and Recall.

## 3. Design Choices

As a design choice, we will be utilizing Lucene for its default retrieval model and to implement Kstemming and pseudo-relevance feedback. As referenced above, Lucene is a well-established, well-tested industry standard, used by the likes of Solr and Elastic Search. Using Lucene in this way will allow us to focus on the analysis of the underlying techniques and strategies with confidence. Furthermore, studying the conceptual basis of information retrieval within the context of Lucene might better prepare us for a career in corporate software development. (More specifically, it’s quite possible that one of us has been explicitly presented with an opportunity for professional advancement by his current employer tied directly to his knowledge/understanding of Solr.)

Python will be used to augment as needed for various algorithmic implementations, as its straightforward yet powerful syntax is quite effective for such purposes.

For indexing purposes, we will implement a simple, document-frequency based indexer and disregard any relative positional information within a document. The retrieval models that we will be implementing will only need the document-frequency information, so this should be more efficient 
