
import java.util.function.Supplier;

interface TerminalStreamOperation<T, R> extends StreamOperation<T>, Supplier<R> {
    @Override
    default void start(StreamCharacteristics upstreamCharacteristics) {
        if(upstreamCharacteristics.isChecked())
            throw new CheckedStreamException("Cannot process a checked exception stream with no error handling");
    }

    @Override
    default void finish() {
        // nothing
    }
}
