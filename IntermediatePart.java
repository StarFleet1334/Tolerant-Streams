import java.util.Objects;

abstract class IntermediatePart<IN, OUT> extends AbstractStreamPart<IN, OUT> {

    private AbstractStreamPart<?, IN> previous;

    IntermediatePart(AbstractStreamPart<?, IN> previous) {
        this.previous = Objects.requireNonNull(previous, "previous");
        this.previous.setNext(this);
    }

    @Override
    SourcePart<?> getSource() {
        return previous.getSource();
    }
}