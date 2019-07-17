import collections
import operator
import re
import nltk
from math import log

# Tuple to represent simple term frequency posting
class Posting(collections.namedtuple('Posting', ['docID', 'count'])):
    __slots__ = ()

    def __str__(self):
        return "{} {}".format(self.docID, self.count)

'''
Provide the API to access index.    
'''
class index_loader:
    def __init__(self, inverted_index_file_path, document_term_count_file):
        # map of posting lists
        self.invert_intex = {}
        # map of term document frequency
        self.doc_freq = {}
        # map of term frequency
        self.term_freq = {}
        # total number of documents in index
        self.total_docs = 0
        # total number of terms in index
        self.total_terms = 0
        # map document term count
        self.doc_term_count = {}
        self.load_index(inverted_index_file_path)
        self.load_document_term_count(document_term_count_file)

        with open('id_to_file.txt', 'r') as id_file:
            self.id_to_filename = eval(id_file.read())
    '''
    load index from the given inverted_index_file_path
    '''
    def load_index(self, inverted_index_file_path):
        invert_intex = self.invert_intex

        with open(inverted_index_file_path, 'r') as index_file:
            line = index_file.readline()

            # read the inverted index line by line. One line is for one term
            while line:
                postings_list = []
                segments = line.split(" ")
                term = segments[0]
                self.term_freq[term] = 0

                # sum the number of occurrence in each document in the terms postings list
                for i in range(1,len(segments), 2):
                    docID = int(segments[i])
                    term_count = int(segments[i+1])
                    postings_list.append(Posting(docID, term_count))

                    self.term_freq[term] += term_count
                    self.total_terms += term_count
                self.doc_freq[term] = len(postings_list)

                invert_intex[term] = postings_list;
                line = index_file.readline()

        return invert_intex;

    def load_document_term_count(self, document_term_count_file):
        with open(document_term_count_file, 'r') as file:
            self.doc_term_count = eval(file.read())
            self.total_docs = len(self.doc_term_count)

'''
Search the given terms in  the corpus represented by the given inverted index

params:
    inverted_index: Dictionary structure of inverted_index  
    terms: query terms and frequency map
'''

def search(loader, terms, k_1=1.2, k_2=100, b=0.75):
    document_score = {}
    N = loader.total_docs
    inverted_index = loader.invert_intex

    # calculate average document length
    avdl = loader.total_terms/loader.total_docs

    # iterate over query terms
    for term in terms:
        if term in inverted_index:
            postings_list = inverted_index[term]

            n_i = loader.doc_freq[term]
            # calculate idf for the query term
            idf = log((N-n_i+0.5)/(n_i+0.5))

            # iterate over the posting of the term
            for posting in postings_list:

                dl = loader.doc_term_count[posting.docID]
                K = k_1*((1-b)+b*(dl/avdl))

                # calculate BM25 for one query term
                score = idf*(k_1+1)*(k_2+1)*posting.count*terms[term]/((K+posting.count)*(k_2+terms[term]))
                if posting.docID in document_score:
                    document_score[posting.docID] += score
                else:
                    document_score[posting.docID] = score

    # sorted the documents by score and return top 100 documents
    return sorted(document_score.items(), reverse=True, key=operator.itemgetter(1))[:100]

'''
Parse the query string and returns map of terms and frequency.
'''
def parse(queryString, case_folding = True, allow_special_char=False):
    # frequency of trigrams
    if case_folding:
        queryString = queryString.lower()

    # replace special characters
    if not allow_special_char:
        queryString = re.sub(r'[^a-zA-Z0-9\-\s\t]+', " ", queryString)
    token = nltk.word_tokenize(queryString);
    ngrams =  nltk.ngrams(token, 1)
    map = {}
    for ngram in ngrams:
        if ngram in map:
            map[ngram[0]] +=1
        else:
            map[ngram[0]] = 1
    return map

'''
Search the given query and store the query result in file.
'''
def performSearch(query, index_loader, query_ID, output_file):
    parsedQuery = parse(query)
    hits = search(index_loader, parsedQuery)
    rank = 1
    results = []
    for (doc_ID, score) in hits:
        file_name = index_loader.id_to_filename[doc_ID]
        results.append("%d Q0 %s %d %f BM25"%(query_ID, file_name[:file_name.rfind('.')],rank, score))
        rank +=1

    with open(output_file, 'a') as file:
        file.write("\n".join(results))
        file.write("\n")


def getQueryList():

    query_list = []
    query_file = open("../../test-collection/cacm.query.txt")
    line = query_file.readline()
    while line:
        while not re.match('<DOCNO>.*</DOCNO>', line.strip()):
            line = query_file.readline();

        line = query_file.readline().strip()
        query = '';
        while not re.match('</DOC>', line):
            query += line
            line = query_file.readline().strip()
        query_list.append(query)

        while not re.match('<DOC>', line.strip()):
            if not line:
                break
            line = query_file.readline();
        line = query_file.readline();
        print(query)
    return query_list

loader = index_loader("unigram.txt", "unigram_term_count.txt")
# print(loader.term_freq["what"])

for i, query in enumerate(getQueryList()):
    performSearch(query, loader, i+1, "BM25.txt")



