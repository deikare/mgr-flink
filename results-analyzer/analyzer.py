import json
import argparse

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

if __name__ == "__main__":
    filename = parse_args()
    data = read_data(filename)
    print(data)
    pass
        