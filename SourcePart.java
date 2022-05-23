

import java.util.Objects;

public final class SourcePart<T> extends AbstractStreamPart<T, T> {

    private final StreamIterator<T> source;
    private final StreamCharacteristics characteristics;

    SourcePart(StreamIterator<T> source, StreamCharacteristics characteristics) {
        this.source = Objects.requireNonNull(source, "source");
        this.characteristics = Objects.requireNonNull(characteristics, "characteristics");
    }

    SourcePart(StreamIterator<T> source) {
        this(source, StreamCharacteristics.regular().withStreamSize(source.getSize()));
    }

    @Override
    SourcePart<?> getSource() {
        return this;
    }

    @Override
    public StreamOperation<T> getStreamOperation() {
        // could also be made a shortcut for getNext().getStreamOperation()
        throw new UnsupportedOperationException();
    }

    void processStream() {
        StreamOperation<T> firstOp = getNext().getStreamOperation();
        firstOp.start(characteristics);
        while (firstOp.needsMoreElements() && source.hasNext()) {
            firstOp.acceptElement(source.next());
        }
        firstOp.finish();
    }
}