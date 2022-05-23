
import java.util.Objects;

abstract class ChainedStreamOperation<T, R> implements StreamOperation<T> {

    private final StreamOperation<R> downstream;

    ChainedStreamOperation(StreamOperation<R> downstream) {
        this.downstream = Objects.requireNonNull(downstream);
    }

    ChainedStreamOperation(StreamOperable<R> downstream) {
        this(downstream.getStreamOperation());
    }

    @Override
    public void start(StreamCharacteristics upstreamCharacteristics) {
        downstream().start(modifyCharacteristics(upstreamCharacteristics));
    }

    StreamCharacteristics modifyCharacteristics(StreamCharacteristics upstreamCharacteristics) {
        return upstreamCharacteristics;
    }

    @Override
    public boolean needsMoreElements() {
        return downstream().needsMoreElements();
    }

    @Override
    public void finish() {
        downstream().finish();
    }

    StreamOperation<R> downstream() {
        return downstream;
    }
}