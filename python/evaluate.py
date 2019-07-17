import sys
import plotly.graph_objs as graph_objs
from plotly.offline import plot


# evaluates the given rankings for a retrieval model according to the given relevance judgements
def evaluate():
    # get file names from user
    relevance_reference_path = input("Please enter the full path of the relevance reference file: ").split()[0]
    model_rankings_path = input("Please enter the full path of the model rankings file: ").split()[0]
    start = model_rankings_path.rfind("/") + 1
    stop = model_rankings_path.rfind(".")
    model_rankings_identifier = model_rankings_path[start:stop]

    # get relevance reference and model rankings from files
    relevance_reference = get_relevance_reference(relevance_reference_path)
    relevant_doc_counts = get_relevant_doc_counts(relevance_reference)
    model_rankings = get_model_rankings_and_name(model_rankings_path)
    relevant_rankings = get_relevant_rankings(relevance_reference, model_rankings)

    # run algorithms, accumulating output along the way
    output = mean_average_precision(relevant_rankings, model_rankings_identifier)
    output += mean_reciprocal_rank(relevant_rankings, model_rankings_identifier)
    output += precision_at_k(relevant_rankings, 5, model_rankings_identifier)
    output += precision_at_k(relevant_rankings, 20, model_rankings_identifier)
    output += recall_and_precision(relevant_rankings, model_rankings_identifier, relevant_doc_counts)

    # write output to file
    output_file = open("{0}_eval.txt".format(model_rankings_identifier), "w")
    output_file.write(output)
    output_file.close()

    return


# returns a dictionary that maps query_id to a list of relevant doc_ids
def get_relevance_reference(relevance_reference_filename):
    relevance_reference_file = open(relevance_reference_filename, "r")
    lines = relevance_reference_file.readlines()
    relevancy_mapping = {}

    for line in lines:
        line = line.replace("\n", "")
        tokens = line.split()
        query_id = tokens[0]
        doc_id = tokens[2]

        if query_id not in relevancy_mapping:
            relevancy_mapping[query_id] = [doc_id]
        else:
            relevancy_mapping[query_id].append(doc_id)

    return relevancy_mapping


# returns a dictionary that maps query_id to relevant doc counts
def get_relevant_doc_counts(relevance_reference):
    doc_counts = {}

    for q in relevance_reference:
        doc_counts[q] = len(relevance_reference[q])

    return doc_counts


# returns a dictionary that maps query_id to a list of {doc_id: rank} dictionaries
# and the of the retrieval model used to generate these rankings
def get_model_rankings_and_name(model_rankings_filename):
    model_rankings_file = open(model_rankings_filename, "r")
    lines = model_rankings_file.readlines()
    model_rankings = {}

    for line in lines:
        line = line.replace("\n", "")
        tokens = line.split()
        query_id = tokens[0]
        doc_id = tokens[2]
        rank = tokens[3]

        if query_id not in model_rankings:
            doc_to_rank = {}
            doc_to_rank[doc_id] = rank
            model_rankings[query_id] = doc_to_rank
        else:
            model_rankings[query_id][doc_id] = rank

    return model_rankings


# returns a dictionary that maps query_id to a list of ranks of relevant documents
def get_relevant_rankings(relevance_reference, model_rankings):
    relevant_rankings = {}

    for q in relevance_reference.keys():
        query_rankings = model_rankings[q]
        rankings_list = []

        for d in relevance_reference[q]:
            if d in query_rankings:
                rankings_list.append(int(query_rankings[d]))

        relevant_rankings[q] = rankings_list

    return relevant_rankings


# returns a string that documents the mean average precision for given relevant rankings
def mean_average_precision(relevant_rankings, model_identifier):
    avg_precision_accumulator = 0

    for q in relevant_rankings.keys():
        avg_precision_accumulator += get_avg_precision(relevant_rankings[q])

    mean_avg_precision = avg_precision_accumulator / len(relevant_rankings)

    return "The mean average precision for the {0} model is {1}.\n\n".format(model_identifier, mean_avg_precision)


# returns the average precision of the given relevant rankings
def get_avg_precision(relevant_ranks):
    i = 0
    precision_accumulator = 0

    while i < len(relevant_ranks):
        precision_accumulator += (i + 1) / relevant_ranks[i]
        i += 1

    return precision_accumulator / len(relevant_ranks)


# returns a string that documents the mean reciprocal rank for the given relevant rankings
def mean_reciprocal_rank(relevant_rankings, model_identifier):
    reciprocal_rank_accumulator = 0

    for q in relevant_rankings.keys():
        reciprocal_rank_accumulator += get_reciprocal_rank(relevant_rankings[q])

    mean_rec_rank = reciprocal_rank_accumulator / len(relevant_rankings)

    return "The mean reciprocal rank for the {0} model is {1}.\n\n".format(model_identifier, mean_rec_rank)


# returns the reciprocal rank of the given relevant rankings
def get_reciprocal_rank(relevant_ranks):
    first_relevant_rank = min(relevant_ranks)

    return 1 / first_relevant_rank


# returns a string that documents the precision at rank k for the given relevant rankings
def precision_at_k(relevant_rankings, k, model_identifier):
    precision_accumulator = 0
    output = ""

    for q in relevant_rankings.keys():
        ranks = relevant_rankings[q]
        i = 0

        while i < len(ranks):
            if ranks[i] > k:
                prec_at_k = i / k
                precision_accumulator += prec_at_k
                output += "Precision at rank {0} for query \"{1}\" in model {2} is {3}.\n".format(k, q, model_identifier, prec_at_k)
                break
            elif ranks[i] == k:
                prec_at_k = (i + 1) / k
                precision_accumulator += prec_at_k
                output += "Precision at rank {0} for query \"{1}\" in model {2} is {3}.\n".format(k, q, model_identifier, prec_at_k)
                break
            else:
                i += 1

        if i >= len(ranks):
            prec_at_k = i / k
            precision_accumulator += prec_at_k
            output += "Precision at rank {0} for query \"{1}\" in model {2} is {3}.\n".format(k, q, model_identifier, prec_at_k)

    output += "\n"
    return output


# returns a string that represents a recall-precision table for each query from the given relevant rankings
# also creates an html Recall-Precision Graph for these queries
def recall_and_precision(relevant_rankings, model_identifier, relevant_doc_counts):
    output = ""
    max_recall_rank = 0

    for q in relevant_rankings:
        if relevant_rankings[q][-1] > max_recall_rank:
            max_recall_rank = relevant_rankings[q][-1]

    data = []

    for q in relevant_rankings.keys():
        relevant_ranks = relevant_rankings[q]
        relevant_docs = relevant_doc_counts[q]
        output += "Recall vs. Precision table for query {0} with model {1}.\n".format(q, model_identifier)
        ranks = [i + 1 for i in range(max_recall_rank)]
        r = []
        p = []
        hits = 0

        for i in ranks:
            if i in relevant_ranks:
                hits += 1
            recall = hits / relevant_docs
            precision = hits / i
            r.append(recall)
            p.append(precision)

        output += "rank\t".format()

        for i in ranks[:-1]:
            output += "{0}\t".format(i)

        output += "{0}\nrecall\t".format(ranks[-1])

        for x in r[:-1]:
            output += "{0}\t".format(x)

        output += "{0}\nprecision\t".format(r[-1])

        for y in p[:-1]:
            output += "{0}\t".format(y)

        output += "{0}\n\n".format(p[-1])

        data.append(graph_objs.Scatter(x=r, y=p, name="Query {0}".format(q)))

    # strategy for graph layout from the plotly documentation: https://plot.ly/python/figure-labels/
    layout = graph_objs.Layout(
        title=graph_objs.layout.Title(
            text="Recall vs. Precision for Model {0}".format(model_identifier),
            xref="paper",
            x=0
        ),
        xaxis=graph_objs.layout.XAxis(
            title=graph_objs.layout.xaxis.Title(
                text="Recall",
                font=dict(
                    family="Courier New, monospace",
                    size=18,
                    color="#7f7f7f"
                )
            )
        ),
        yaxis=graph_objs.layout.YAxis(
            title=graph_objs.layout.yaxis.Title(
                text="Precision",
                font=dict(
                    family="Courier New, monospace",
                    size=18,
                    color="#7f7f7f"
                )
            )
        )
    )

    figure = graph_objs.Figure(data=data, layout=layout)
    plot(figure, filename="recall_vs_precision_for_model_{0}.html".format(model_identifier))

    return output


if __name__ == "__main__":
    sys.tracebacklimit = 0
    evaluate()
