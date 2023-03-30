package vfdt;

import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProcessSink implements SinkFunction<String> {
    public final Logger logger = LoggerFactory.getLogger(ProcessSink.class);

    public ProcessSink() {
    }

    @Override
    public void invoke(String value, Context context) throws Exception {
        logger.info(value);
    }
}
