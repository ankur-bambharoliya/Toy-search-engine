import os
import re
import nltk
import collections

# Tuple to represent simple term frequency posting
class Posting(collections.namedtuple('Posting', ['docID', 'count'])):
    __slots__ = ()

    def __str__(self):
        return "{} {}".format(self.docID, self.count)


'''
Creates an inverted index with simple term frequency  postings

params:

 corpus_path : path the corpus folder. Corpus folder contains preprocessed text documents
 output_path : path to index file
 term_count_path : path file where term count table needs to be stored
 case_folding: ignore cases if True; Default True
 allow_special_char: allow special characters if set to True; Default False
 n : size of n-gram; Default n=1 so unigram
'''


def creatInvertedIndex(corpus_path, output_path, term_count_path, case_folding=True, allow_special_char=False, n=1):
    # dict to store inverted index
    invertIntex = {};
    print("Creating Index....")
    # read list of files in corpus
    for root, dirs, files in os.walk(corpus_path):
        docID = 0;
        term_count_table = {}
        id_to_file = {}

        # iterate through each processed text file to create an index.
        for filename in files:
            term_count = 0
            print(docID, filename)
            text_file = open(os.path.join(corpus_path, filename), "r")
            # split file content by new line so that trigrams are calculated with the paragraph
            for line in re.split(r'\n', text_file.read()):

                if case_folding:
                    line = line.lower()

                # replace special characters
                if not allow_special_char:
                    line = re.sub(r'[^a-zA-Z0-9\-\s\t]+', " ", line)

                token = nltk.word_tokenize(line)
                ngrams = nltk.ngrams(token, n)

                for ngram in ngrams:
                    term_count += 1
                    # create entry for the term if it not created before
                    if ngram not in invertIntex:
                        invertIntex[ngram] = []

                    # add the posting for current document if it is not already added
                    if len(invertIntex[ngram]) == 0 or invertIntex[ngram][-1].docID is not docID:
                        invertIntex[ngram].append(Posting(docID, 0))

                    # if posting is already added, then increase the count by 1
                    invertIntex[ngram][-1] = Posting(docID, invertIntex[ngram][-1].count + 1)

            # store the document term count in term count table
            term_count_table[docID] = term_count
            id_to_file[docID] = filename
            docID += 1

    # delete the old index file
    if os.path.exists(output_path):
        os.remove(output_path)

    # write new index file
    with open(output_path, 'a') as file:
        for term, postingList in invertIntex.items():
            file.write(" ".join(map(str, term)) + " " + " ".join(map(str, postingList)) + "\n")

    # write the document term count table into file
    with open(term_count_path, 'w') as file:
        file.write(str(term_count_table))

    with open("id_to_file.txt", 'w') as file:
        file.write(str(id_to_file))
    return invertIntex

creatInvertedIndex("../../test-collection/cacm", "unigram.txt", "unigram_term_count.txt", n=1)
