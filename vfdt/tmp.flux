from(bucket: "data")
  |> range(start: -inf)
  |> filter(fn: (r) => r["_measurement"] == "classifierResult")
  |> filter(fn: (r) => r["_field"] == "classificationDuration")
  |> sort(columns: ["_time"])
  |> filter(fn: (r) => r["experimentId"] == "dc4ad0f7-df5c-4633-aef5-e68a01ce7d1c")
  |> first()

from(bucket: "data")
    |> range(start: -inf)
    |> filter(fn: (r) => r["_measurement"] == "classifierResult")
    |> filter(fn: (r) => r["class"] == "0")
    |> filter(fn: (r) => r["dataset"] == "elec")
    |> filter(fn: (r) => r["experimentId"] == "366045e5-dbd2-4f85-b73b-2eca38959dd4")
    |> filter(fn: (r) => r["host"] == "f87870764851")
    |> filter(fn: (r) => r["name"] == "vfdt")
    |> filter(fn: (r) => r["params"] == "r1_d0.05_t0.2_n50")
    |> filter(fn: (r) => r["predicted"] == "0")
    |> yield(name: "mean")

//todo - able to compare few experiment Ids
//add global accuracy value as value in influx
//add experimentId sorting from earliest to latest
//add walking accuracy as query in influx

from(bucket: "data")
    |> range(start: -inf)
    |> filter(fn: (r) => r["_measurement"] == "classifierResult")
    |> filter(
        fn: (r) =>
            r["experimentId"] == "bf5c6d66-07ed-4927-9c03-c13dff0d8b31" and r["_field"]
                ==
                "classificationDuration",
    )
    |> group(columns: ["experimentId"])
    |> sort(columns: ["_time"])
    |> map(
        fn: (r) =>
            ({
                _time: r._time,
                experimentId: r.experimentId,
                _value: if r["class"] == r["predicted"] then 1.0 else 0.0,
                tmp: 1.0
            }),
    )
    |> group(columns: ["experimentId"])
    |> cumulativeSum(columns: ["tmp", "_value"])
    |> map(fn: (r) => ({_time: r._time, experimentId: r.experimentId, _value: r._value / r.tmp}))


//todo sorted experimentIds from earliest
from(bucket: "data")
    |> range(start: -inf)
    |> filter(fn: (r) => r["_measurement"] == "classifierResult" and r["_field"] == "classificationDuration")
    |> group(columns: ["experimentId"])
    |> keep(columns: ["_time", "experimentId"])
    |> sort(columns: ["_time"])
    |> limit(n: 1)
    |> group() //so it merges streams for each experimentId into one stream
    |> sort(columns: ["_time"])