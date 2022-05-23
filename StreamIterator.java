
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.OptionalLong;
import java.util.stream.Stream;

interface StreamIterator<T> {
    /**
     * Returns {@code true} if the stream has more elements.
     *
     * @return {@code true} if the stream has more elements
     */
    boolean hasNext();

    /**
     * Returns the next element of the stream
     *
     * @throws NoSuchElementException if the stream has no more elements
     */
    StreamElement<T> next();

    /**
     *
     * @return an OptionalLong with the exact size, or an empty one if the exact
     *         size is not known
     */
    OptionalLong getSize();

    static <T> StreamIterator<T> of(Collection<T> col) {
        return of(col.iterator(), OptionalLong.of(col.size()));
    }

    static <T> StreamIterator<T> of(Iterable<T> iterable) {
        return of(iterable.iterator(), OptionalLong.empty());
    }

    static <T> StreamIterator<T> of(T[] elements) {
        return of(Arrays.asList(elements));
    }

    static <T> StreamIterator<T> of(Stream<T> javaStream) {
        // could use Spliterator here which might have a size, but too advanced for PGdP
        return of(javaStream.iterator(), OptionalLong.empty());
    }

    private static <T> StreamIterator<T> of(Iterator<T> iterator, OptionalLong size) {
        return new StreamIterator<>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public StreamElement<T> next() {
                return StreamElement.of(iterator.next());
            }

            @Override
            public OptionalLong getSize() {
                return size;
            }
        };
    }
}
