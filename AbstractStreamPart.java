import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

abstract class AbstractStreamPart<IN, OUT> implements Stream<OUT>, StreamOperable<IN> {

    private StreamOperable<OUT> next;

    StreamOperable<OUT> getNext() {
        return next;
    }

    void setNext(StreamOperable<OUT> next) {
        if (this.next != null)
            throw new IllegalStateException("Stream was already consumed or linked");
        this.next = Objects.requireNonNull(next, "next");
    }

    abstract SourcePart<?> getSource();

    private <R> R evaluate(TerminalStreamOperation<OUT, R> terminal) {
        setNext(() -> terminal);
        getSource().processStream();
        return terminal.get();
    }

    @Override
    public Stream<OUT> filter(Predicate<? super OUT> filter) {
        Objects.requireNonNull(filter, "filter");
        return filterImpl(filter::test, UpdateType.PRESERVE);
    }

    @Override
    public Stream<OUT> filterChecked(ThrowingPredicate<? super OUT> filter) {
        Objects.requireNonNull(filter, "filter");
        return filterImpl(filter, UpdateType.SET);
    }

    private Stream<OUT> filterImpl(ThrowingPredicate<? super OUT> filter, UpdateType updateChecked) {
        return new IntermediatePart<>(this) {
            @Override
            public StreamOperation<OUT> getStreamOperation() {
                return new ChainedStreamOperation<>(getNext()) {

                    @Override
                    public StreamCharacteristics modifyCharacteristics(StreamCharacteristics upstreamCharacteristics) {
                        return combineChecked(upstreamCharacteristics.withUnknownStreamSize(), updateChecked);
                    }

                    @Override
                    public void acceptElement(StreamElement<OUT> t) {
                        if (t.hasExceptions()) {
                            downstream().acceptElement(t);
                            return;
                        }
                        boolean passed;
                        try {
                            passed = filter.test(t.getElement());
                        } catch (Exception e) {
                            downstream().acceptElement(t.withExceptionAdded(e));
                            return;
                        }
                        if (passed)
                            downstream().acceptElement(t);
                    }
                };
            }
        };
    }

    @Override
    public <R> Stream<R> map(Function<? super OUT, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return mapImpl(mapper::apply, UpdateType.PRESERVE);
    }

    @Override
    public <R> Stream<R> mapChecked(ThrowingFunction<? super OUT, ? extends R> mapper) {
        Objects.requireNonNull(mapper, "mapper");
        return mapImpl(mapper, UpdateType.SET);
    }

    private <R> Stream<R> mapImpl(ThrowingFunction<? super OUT, ? extends R> mapper, UpdateType updateChecked) {
        return new IntermediatePart<>(this) {
            @Override
            public StreamOperation<OUT> getStreamOperation() {
                return new ChainedStreamOperation<>(getNext()) {

                    @Override
                    public StreamCharacteristics modifyCharacteristics(StreamCharacteristics upstreamCharacteristics) {
                        return combineChecked(upstreamCharacteristics.withDistinct(false), updateChecked);
                    }

                    @Override
                    public void acceptElement(StreamElement<OUT> t) {
                        if (t.hasExceptions()) {
                            downstream().acceptElement(t.tryAdapt());
                            return;
                        }
                        R r;
                        try {
                            r = mapper.apply(t.getElement());
                        } catch (Exception e) {
                            downstream().acceptElement(t.withExceptionAdded(e));
                            return;
                        }
                        downstream().acceptElement(StreamElement.of(r));
                    }
                };
            }
        };
    }

    @Override
    public Stream<OUT> distinct() {
        return new IntermediatePart<>(this) {
            @Override
            public StreamOperation<OUT> getStreamOperation() {
                return new ChainedStreamOperation<>(getNext()) {

                    boolean alreadyDistinct;
                    HashSet<StreamElement<OUT>> set;
                    ArrayList<StreamElement<OUT>> allDistinct;

                    @Override
                    public void start(StreamCharacteristics upstreamCharacteristics) {
                        super.start(upstreamCharacteristics);
                        alreadyDistinct = upstreamCharacteristics.isDistinct();
                        if (!alreadyDistinct) {
                            set = new HashSet<>();
                            allDistinct = new ArrayList<>();
                        }
                    }

                    @Override
                    public StreamCharacteristics modifyCharacteristics(StreamCharacteristics upstreamCharacteristics) {
                        return upstreamCharacteristics.withUnknownStreamSize();
                    }

                    @Override
                    public void acceptElement(StreamElement<OUT> t) {
                        if (alreadyDistinct) {
                            downstream().acceptElement(t);
                            return;
                        }
                        if (t.hasExceptions())
                            allDistinct.add(t);
                        else if (set.add(t))
                            allDistinct.add(t);
                    }

                    @Override
                    public void finish() {
                        if (!alreadyDistinct) {
                            Iterator<StreamElement<OUT>> it = allDistinct.iterator();
                            while (it.hasNext() && downstream().needsMoreElements())
                                downstream().acceptElement(it.next());
                        }
                        super.finish();
                    }
                };
            }
        };
    }

    @Override
    public long count() {
        return evaluate(new TerminalStreamOperation<>() {

            long count;
            boolean shortCircuit;

            @Override
            public void start(StreamCharacteristics upstreamCharacteristics) {
                TerminalStreamOperation.super.start(upstreamCharacteristics);
                count = 0L;
                var size = upstreamCharacteristics.getStreamSize();
                shortCircuit = size.isPresent();
                if (shortCircuit)
                    count = size.getAsLong();
            }

            @Override
            public boolean needsMoreElements() {
                return !shortCircuit;
            }

            @Override
            public void acceptElement(StreamElement<OUT> t) {
                checkElementForExceptions(t);
                count++;
            }

            @Override
            public Long get() {
                return count;
            }
        });
    }

    @Override
    public Optional<OUT> findFirst() {
        return evaluate(new TerminalStreamOperation<>() {

            Optional<OUT> value = Optional.empty();

            @Override
            public boolean needsMoreElements() {
                return value.isEmpty();
            }

            @Override
            public void acceptElement(StreamElement<OUT> t) {
                if (value.isPresent())
                    throw new IllegalStateException("findFirst() cannot accept more than one element");
                checkElementForExceptions(t);
                value = Optional.of(t.getElement());
            }

            @Override
            public Optional<OUT> get() {
                return value;
            }
        });
    }

    @Override
    public Optional<OUT> reduce(BinaryOperator<OUT> accumulator) {
        Objects.requireNonNull(accumulator, "accumulator");
        return evaluate(new TerminalStreamOperation<>() {
            boolean oneSeen = false;
            OUT current;

            @Override
            public boolean needsMoreElements() {
                return true;
            }

            @Override
            public void acceptElement(StreamElement<OUT> t) {
                checkElementForExceptions(t);
                if (oneSeen) {
                    current = accumulator.apply(current, t.getElement());
                } else {
                    current = t.getElement();
                    oneSeen = true;
                }
            }

            @Override
            public Optional<OUT> get() {
                if (oneSeen)
                    return Optional.of(current);
                return Optional.empty();
            }
        });
    }

    @Override
    public Collection<OUT> toCollection(Supplier<? extends Collection<OUT>> collectionGenerator) {
        Objects.requireNonNull(collectionGenerator, "collectionGenerator");
        return evaluate(new TerminalStreamOperation<>() {

            Collection<OUT> collection;

            @Override
            public void start(StreamCharacteristics upstreamCharacteristics) {
                TerminalStreamOperation.super.start(upstreamCharacteristics);
                collection = Objects.requireNonNull(collectionGenerator.get(), "collectionGenerator returned null");
            }

            @Override
            public boolean needsMoreElements() {
                return true;
            }

            @Override
            public void acceptElement(StreamElement<OUT> t) {
                checkElementForExceptions(t);
                collection.add(t.getElement());
            }

            @Override
            public Collection<OUT> get() {
                return collection;
            }
        });
    }

    @Override
    public Stream<OUT> onErrorFilter() {
        return new IntermediatePart<>(this) {
            @Override
            public StreamOperation<OUT> getStreamOperation() {
                return new ChainedStreamOperation<>(getNext()) {

                    @Override
                    public StreamCharacteristics modifyCharacteristics(StreamCharacteristics upstreamCharacteristics) {
                        return upstreamCharacteristics.withUnknownStreamSize();
                    }

                    @Override
                    public void acceptElement(StreamElement<OUT> t) {
                        if (!t.hasExceptions())
                            downstream().acceptElement(t);
                    }
                };
            }
        };
    }

    @Override
    public Stream<OUT> onErrorMap(Function<? super List<Exception>, ? extends OUT> errorMapper) {
        Objects.requireNonNull(errorMapper, "errorMapper");
        return onErrorMap(errorMapper::apply, UpdateType.CLEAR);
    }

    @Override
    public Stream<OUT> onErrorMapChecked(ThrowingFunction<? super List<Exception>, ? extends OUT> errorMapper) {
        Objects.requireNonNull(errorMapper, "errorMapper");
        return onErrorMap(errorMapper, UpdateType.SET);
    }

    private Stream<OUT> onErrorMap(ThrowingFunction<? super List<Exception>, ? extends OUT> errorMapper,
                                   UpdateType updateChecked) {
        return new IntermediatePart<>(this) {
            @Override
            public StreamOperation<OUT> getStreamOperation() {
                return new ChainedStreamOperation<>(getNext()) {

                    @Override
                    public StreamCharacteristics modifyCharacteristics(StreamCharacteristics upstreamCharacteristics) {
                        return combineChecked(upstreamCharacteristics.withDistinct(false), updateChecked);
                    }

                    @Override
                    public void acceptElement(StreamElement<OUT> t) {
                        if (!t.hasExceptions()) {
                            downstream().acceptElement(t);
                            return;
                        }
                        OUT replacement;
                        try {
                            replacement = errorMapper.apply(t.getExceptions());
                        } catch (Exception e) {
                            downstream().acceptElement(t.withExceptionAdded(e));
                            return;
                        }
                        downstream().acceptElement(StreamElement.of(replacement));
                    }
                };
            }
        };
    }

    private static StreamCharacteristics combineChecked(StreamCharacteristics characteristics, UpdateType updateType) {
        return characteristics.withChecked(updateType.getOperation().apply(characteristics.isChecked()));
    }

    private static void checkElementForExceptions(StreamElement<?> e) {
        if (e.hasExceptions())
            throw new ErrorsAtTerminalOperationException(
                    "Terminal operation encountered some Exceptions: " + e.getExceptions());
    }

    /**
     * Helps to reduce the code duplication and improve readability
     */
    private enum UpdateType {
        SET(old -> true),
        PRESERVE(old -> old),
        CLEAR(old -> false);

        final UnaryOperator<Boolean> updateOp;

        private UpdateType(UnaryOperator<Boolean> updateOp) {
            this.updateOp = updateOp;
        }

        UnaryOperator<Boolean> getOperation() {
            return updateOp;
        }
    }
}