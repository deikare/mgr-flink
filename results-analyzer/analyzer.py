import json
import argparse
from matplotlib import pyplot as plt


def parse_args() -> str:
    parser = argparse.ArgumentParser("VFDT result analyzer")
    parser.add_argument("filename", help="Stat filepath")
    args = vars(parser.parse_args())
    filename = args["filename"]
    return filename


def read_data(filenames: list) -> list[dict]:
    result = []
    for filename in filenames:
        with open(filename) as f:
            data = json.load(f)
            result.append(data)

    return result


def plot_split_samples(data: list[dict], labels: list[str] = None):
    for data_dict in data:
        x = [1]
        y = [1]
        for sampleNumber in data_dict["samplesOnSplit"]:
            x.append(sampleNumber)
            y.append(y[-1] + 2)

        plt.step(x, y)

    if labels:
        plt.legend(labels)
    plt.title("Number of nodes in tree during stream processing")
    plt.ylabel("number of nodes")
    plt.xlabel("sample")
    plt.savefig('plots/splitsamples.png')
    plt.close(plt.gcf())


def plot_accuracy_per_batch(data: list[dict], labels: list[str] = None):
    for data_dict in data:
        x = [0]
        y = [0.0]

        for value in data_dict["batchStats"]:
            x.append(value["n"] + x[-1])
            y.append(value["correctClassifications"]
                     * 100 / value["n"])

        plt.plot(x, y)

    if labels:
        plt.legend(labels)
    plt.title("Classification accuracy per batch")
    plt.ylabel("accuracy [%]")
    plt.xlabel("sample")
    plt.ylim((0, 100))
    plt.savefig('plots/batch_accuracy.png')
    plt.close(plt.gcf())


def plot_walking_accuracy(data: list[dict], labels: list[str] = None):
    for data_dict in data:
        x = [0]
        y_total = 0
        percentage = [0.0]

        for value in data_dict["batchStats"]:
            x.append(value["n"] + x[-1])
            percentage.append(
                (value["correctClassifications"] + y_total) * 100 / x[-1])
            y_total += value["correctClassifications"]

        plt.plot(x, percentage)

    if labels:
        plt.legend(labels)

    plt.title("Classification: walking accuracy")
    plt.ylabel("accuracy [%]")
    plt.xlabel("sample")
    plt.ylim((0, 100))
    plt.savefig('plots/accuracy.png')
    plt.close(plt.gcf())


def plot_avg_values(data: list[dict], key_measurement_type: str, key: str, labels: list[str] = None):
    for data_dict in data:
        x = [0]
        y = [0]

        if key == _NODES_TRAVERSE_COUNT:
            subtitle = "Mean visited nodes count during traversal to leaf per batch"
            y_label = "count"
            prescaler = 1
        elif key == _LEAF_TRAVERSE_DURATION:
            subtitle = "Mean traversal to leaf duration per batch"
            y_label = "duration [\u03BCs]"
            prescaler = 1000
        else:
            subtitle = "Mean sample processing duration per batch"
            y_label = "duration [\u03BCs]"
            prescaler = 1000

        if key_measurement_type == _CLASSIFICATION:
            title = f"Classification: {subtitle}"
        else:
            title = f"Learning: {subtitle}"

        for value in data_dict["batchStats"]:
            x.append(value["n"] + x[-1])
            y.append(value[key_measurement_type][key] / prescaler)

        plt.plot(x, y)

    if labels:
        plt.legend(labels)
    plt.title(title)
    plt.xlabel("sample")
    plt.ylabel(y_label)
    plt.savefig(f'plots/avg_{key_measurement_type}_{key}.png')
    plt.close(plt.gcf())


_CLASSIFICATION = "classificationStats"
_LEARNING = "learningStats"

_NODES_TRAVERSE_COUNT = "nodesOnTraverseMeanCount"
_LEAF_TRAVERSE_DURATION = "toLeafTraverseMeanDuration"
_TOTAL_DURATION = "meanTotalDuration"

if __name__ == "__main__":
    # filename = parse_args()
    # filename = "results/stat_elec_r1_d0.05_t0.2_n50_b500.txt"
    filenames = [
        "results/stat_elec_r1_d0.05_t0.2_n50_b500.txt",
    ]

    labels = [
        # "nMin = 50",
        # "nMin = 500",
        # "nMin = 50, no h"

    ]

    data = read_data(filenames)

    plot_walking_accuracy(data, labels)
    plot_accuracy_per_batch(data, labels)
    plot_split_samples(data, labels)

    for key_measurement_type in [_CLASSIFICATION, _LEARNING]:
        for key in [_NODES_TRAVERSE_COUNT, _LEAF_TRAVERSE_DURATION, _TOTAL_DURATION]:
            plot_avg_values(data, key_measurement_type,
                            key, labels)
