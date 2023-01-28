import json
import argparse
from matplotlib import pyplot as plt


def parse_args() -> str:
    parser = argparse.ArgumentParser("VFDT result analyzer")
    parser.add_argument("filename", help="Stat filepath")
    args = vars(parser.parse_args())
    filename = args["filename"]
    return filename


def read_data(filename: str):
    with open(filename) as f:
        data = json.load(f)

    return data


def plot_split_samples(data):
    x = [1]
    y = [1]
    for sampleNumber in data["samplesOnSplit"]:
        x.append(sampleNumber)
        y.append(y[-1] + 2)

    plt.step(x, y)
    plt.title("Number of nodes in tree during stream processing")
    plt.ylabel("number of nodes")
    plt.xlabel("sample")
    plt.show()


def plot_accuracy_per_batch(data):
    x = [0]
    y = [0.0]

    for value in data["batchStats"]:
        x.append(value["n"] + x[-1])
        y.append(value["correctClassifications"] * 100 / value["n"])

    plt.plot(x, y)
    plt.title("Classification accuracy")
    plt.ylabel("accuracy [%]")
    plt.xlabel("sample")
    plt.ylim((0, 100))
    plt.show()


def plot_walking_accuracy(data):
    x = [0]
    y_total = 0
    percentage = [0.0]

    for value in data["batchStats"]:
        x.append(value["n"] + x[-1])
        percentage.append(
            (value["correctClassifications"] + y_total) * 100 / x[-1])
        y_total += value["correctClassifications"]

    plt.plot(x, percentage)
    plt.title("Walking classification accuracy")
    plt.ylabel("accuracy [%]")
    plt.xlabel("sample")
    plt.ylim((0, 100))
    plt.show()


def plot_avg_values(data, key_measurement_type: str, key: str):
    x = [0]
    y = [0]

    if key == _NODES_TRAVERSE_COUNT:
        subtitle = "Mean nodes count during tree traverse to leaf"
        y_label = "count"
        prescaler = 1
    elif key == _LEAF_TRAVERSE_DURATION:
        subtitle = "Mean traverse to leaf duration"
        y_label = "duration [\u03BCs]"
        prescaler = 1000
    else:
        subtitle = "Mean node process duration"
        y_label = "duration [\u03BCs]"
        prescaler = 1000

    if key_measurement_type == _CLASSIFICATION:
        title = f"Classification: {subtitle}"
    else:
        title = f"Learning: {subtitle}"

    for value in data["batchStats"]:
        x.append(value["n"] + x[-1])
        y.append(value[key_measurement_type][key] / prescaler)

    plt.plot(x, y)

    plt.title(title)
    plt.xlabel("sample")
    plt.ylabel(y_label)
    plt.show()


_CLASSIFICATION = "classificationStats"
_LEARNING = "learningStats"

_NODES_TRAVERSE_COUNT = "nodesOnTraverseMeanCount"
_LEAF_TRAVERSE_DURATION = "toLeafTraverseMeanDuration"
_TOTAL_DURATION = "meanTotalDuration"

if __name__ == "__main__":
    # filename = parse_args()
    filename = "results/stat_elec_r1_d0.05_t0.1_n2_b1000.txt"
    data = read_data(filename)
    # print(data)
    plot_walking_accuracy(data)
    plot_accuracy_per_batch(data)
    plot_split_samples(data)
    for key_measurement_type in [_CLASSIFICATION, _LEARNING]:
        for key in [_NODES_TRAVERSE_COUNT, _LEAF_TRAVERSE_DURATION, _TOTAL_DURATION]:
            plot_avg_values(data, key_measurement_type,
                            key)
