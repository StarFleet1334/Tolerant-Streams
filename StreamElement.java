

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class StreamElement<T> {

    private final T element;
    private final List<Exception> exceptions;

    private StreamElement(T element, List<Exception> exceptions) {
        this.element = element;
        this.exceptions = exceptions;
    }

    private StreamElement(T element) {
        this(element, List.of());
    }

    boolean isNull() {
        return element == null;
    }

    T getElement() {
        if (hasExceptions())
            throw new UnsupportedOperationException();
        return element;
    }

    List<Exception> getExceptions() {
        return exceptions;
    }

    boolean hasExceptions() {
        return !exceptions.isEmpty();
    }

    <R> StreamElement<R> withExceptionAdded(Exception e) {
        Objects.requireNonNull(e);
        ArrayList<Exception> newExceptions = new ArrayList<>(exceptions);
        newExceptions.add(e);
        return new StreamElement<>(null, List.copyOf(newExceptions));
    }

    <R> StreamElement<R> tryAdapt() {
        if (!hasExceptions())
            throw new UnsupportedOperationException();
        return new StreamElement<>(null, exceptions); // cast wäre auch möglich
    }

    static <T> StreamElement<T> of(T t) {
        return new StreamElement<>(t);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(element);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof StreamElement))
            return false;
        StreamElement<?> other = (StreamElement<?>) obj;
        return Objects.equals(element, other.element);
    }

    @Override
    public String toString() {
        return String.format("StreamElement [%s, exceptions=%s]", element, exceptions);
    }

}
