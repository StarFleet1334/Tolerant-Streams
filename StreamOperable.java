@FunctionalInterface
interface StreamOperable<T> {
    StreamOperation<T> getStreamOperation();
}
