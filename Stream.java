import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Exception-fangende Stream Implementierung
 * <p>
 * Auf einer Stream-Instanz kann einmalig eine Stream-Methode aufgerufen werden,
 * danach ist diese Stream-Instanz an die entsprechende Operation gebunden bzw.
 * im fall einer terminalen Stream-Operation wird der Stream ausgewertet und
 * dabei konsumiert. Benutze Stream-Instanzen können daher nicht wiederverwendet
 * werden, es wird eine {@link IllegalStateException} geworfen.
 *
 * @param <T> der Typ der Elemente im Stream
 */
public interface Stream<T> {

    /**
     * Transformiert alle Elemente des Streams von T nach R mithilfe der übergeben
     * Function. Die Transformation hat auf fehlerhafte Elemente keinen Einfluss,
     * sie werden einfach weitergereicht.
     *
     * @param <R>    der Typ der Elemente des Streams nach map
     * @param mapper die Funktion, die die Elemente des Streams von Typ T (oder
     *               einer Oberklasse davon) zu Typ R (oder eine Unterklasse davon)
     *               umwandelt. Tritt dabei ein Fehler aus, so wird dieser als
     *               fehlerhaftes Element durch den Stream gereicht.
     * @return einen Stream vom Typ R
     * @throws NullPointerException falls mapper null ist
     */
    <R> Stream<R> map(Function<? super T, ? extends R> mapper);

    /**
     * Filtert die Elemente aus dem Stream, für die das übergebene Predicate false
     * zurückgibt. Der Filter hat auf fehlerhafte Elemente keinen Einfluss, sie
     * werden einfach weitergereicht.
     *
     * @param filter die Bedingung, nach der Elemente im Stream weitergereicht (test
     *               liefert true) werden oder herausgefiltert (test liefert false).
     *               Tritt dabei ein Fehler aus, so wird dieser als fehlerhaftes
     *               Element durch den Stream gereicht und nicht herausgefiltert.
     * @return einen Stream vom Typ T
     * @throws NullPointerException falls filter null ist
     */
    Stream<T> filter(Predicate<? super T> filter);

    /**
     * Siehe {@link #map(Function)}, jedoch kann mapper beliebige Exceptions werfen.
     * <p>
     * <b>Diese Stream Operation verwandelt den Stream in einen "Checked
     * Stream".</b>
     *
     * @throws NullPointerException falls mapper null ist
     * @see #map(Function)
     */
    <R> Stream<R> mapChecked(ThrowingFunction<? super T, ? extends R> mapper);

    /**
     * Siehe {@link #filter(Predicate)}, jedoch kann filter beliebige Exceptions
     * werfen.
     * <p>
     * <b>Diese Stream Operation verwandelt den Stream in einen "Checked
     * Stream".</b>
     *
     * @throws NullPointerException falls filter null ist
     * @see #filter(Predicate)
     */
    Stream<T> filterChecked(ThrowingPredicate<? super T> filter);

    /**
     * Eliminiert Duplikate aus dem Stream, fehlerhafte Elemente sind davon nicht
     * betroffen. Die Reihenfolge der Elemente verändert sich dabei nicht, bei
     * gleichen Elementen wird nur das erste behalten; fehlerhafte Elemente werden
     * einfach in weitergereicht. Enthält der Stream ein null-Elemente, so werden
     * diese nicht anders als normale Elemente behandelt, der Ergebnis-Stream
     * enthält maximal ein null Element.
     * <p>
     * Beispiel: aus
     *
     * <pre>
     * [1, 3, 2, 2, null, 1, {fehlerhaft}, 3, {fehlerhaft}, 4]
     * </pre>
     *
     * wird
     *
     * <pre>
     * [1, 3, 2, null, {fehlerhaft}, {fehlerhaft}, 4]
     * </pre>
     *
     * @return einen Stream selben Typs, der frei von Duplikaten ist
     */
    Stream<T> distinct();

    /**
     * Gibt die Anzahl an Elementen im Stream zurück. Ist die Größe des Streams
     * bekannt, wird das Ergebnis sofort zurückgegeben, ohne das Elemente den Stream
     * durchlaufen.
     * <p>
     * Ist die Größe nicht bekannt, werden alle Elemente im Stream gezählt. Falls
     * Elemente davon fehlerhaft sind, wird eine ErrorsAtTerminalOperationException
     * geworfen. (Da z.B. nicht herausgefunden werden kann, ob diese Elemente bei
     * einer filter-Operation gefiltert werden müssten).
     * <p>
     * <b>Dies ist eine terminale Stream-Operation.</b>
     *
     * @return die Anzahl der Elemente im Stream
     * @throws ErrorsAtTerminalOperationException falls die exakte Anzahl unbekannt
     *                                            ist und ein fehlerhaftes Element
     *                                            verarbeitet werden müsste
     * @throws CheckedStreamException             falls der Stream an dem Punkt ein
     *                                            "Checked Stream" ist
     */
    long count();

    /**
     * Gibt das erste Element im Stream zurück.Ist der Stream leer, so wird
     * Optional.empty() zurückgegeben.
     * <p>
     * <b>Dies ist eine terminale Stream-Operation.</b>
     *
     * @return das erste Element im Stream, falls vorhanden.
     * @throws NullPointerException               falls das erste Element null ist
     * @throws ErrorsAtTerminalOperationException falls das erste Element fehlerhaft
     *                                            ist
     * @throws CheckedStreamException             falls der Stream an dem Punkt ein
     *                                            "Checked Stream" ist
     * @implSpec sobald ein Element gefunden wurde, werden keine weiteren Elemente
     *           von dem Stream verarbeitet.
     */
    Optional<T> findFirst();

    /**
     * Akkumuliert alle Elemente in dem Stream mit dem gegebenen accumulator. Ist
     * der Stream leer, so wird Optional.empty() zurückgegeben. Ist mindestens ein
     * Element vorhanden, so wird dieses als Startwert benutzt und alle darauf
     * folgenden werden mit dem Wert von davor kombiniert, woraus sich wieder ein
     * neuer Wert ergibt. Der akkumulierte Wert wird jeweils als erster Parameter
     * übergeben, der hinzuzufügende Wert an zweiter Stelle.
     * <p>
     * <b>Dies ist eine terminale Stream-Operation.</b>
     *
     * @param accumulator ein Akkumulator für Elemente vom Typ T
     * @return das Resultat aus der Akkumulation, falls vorhanden.
     * @throws NullPointerException               falls accumulator oder das
     *                                            Resultat null ist
     * @throws ErrorsAtTerminalOperationException falls irgendein Element fehlerhaft
     *                                            ist
     * @throws CheckedStreamException             falls der Stream an dem Punkt ein
     *                                            "Checked Stream" ist
     */
    Optional<T> reduce(BinaryOperator<T> accumulator);

    /**
     * Sammelt alle Elemente in einer Collection, die aus dem übergebenen
     * collectionGenerator neu erzeugt bzw. genommen wird.
     * <p>
     * <b>Dies ist eine terminale Stream-Operation.</b>
     *
     * @return das Resultat aus der Akkumulation, falls vorhanden.
     * @throws NullPointerException               falls der collectionGenerator oder
     *                                            die daraus kommende Collection
     *                                            null ist
     * @throws ErrorsAtTerminalOperationException falls irgendein Element fehlerhaft
     *                                            ist
     * @throws CheckedStreamException             falls der Stream an dem Punkt ein
     *                                            "Checked Stream" ist
     */
    Collection<T> toCollection(Supplier<? extends Collection<T>> collectionGenerator);

    /**
     * Transformiert alle fehlerhaften Elemente des Streams zu regulären, nicht
     * fehlerhaften Elementen mithilfe der übergebenen Function. Die Transformation
     * hat auf bereits nicht fehlerhafte Elemente keinen Einfluss, sie werden
     * einfach weitergereicht. Tritt bei errorMapper selbst ein Fehler auf, so
     * bleibt das Element fehlerhaft, und die aufgetretene Exception wird der Liste
     * hinzugefügt.
     * <p>
     * <b>Falls der Stream ein "Checked Stream" ist, ist es es danach nicht mehr</b>
     *
     * @param errorMapper die Funktion, die fehlerhafte Elemente des Streams zu
     *                    regulären umwandelt. Tritt bei errorMapper selbst ein
     *                    Fehler auf, so bleibt das Element fehlerhaft, und die neu
     *                    aufgetretene Exception wird dem Element hinzugefügt.
     * @return ein Stream selben Typs, garantiert kein "Checked Stream"
     * @throws NullPointerException falls errorMapper null ist
     */
    Stream<T> onErrorMap(Function<? super List<Exception>, ? extends T> errorMapper);

    /**
     * Siehe {@link #onErrorMap(Function)}, jedoch kann errorMapper beliebige
     * Exceptions werfen und falls der Stream ein "Checked Stream" ist, ändert sich
     * daran nichts.
     * <p>
     * <b>Diese Stream Operation verwandelt den Stream in einen "Checked
     * Stream".</b>
     *
     * @throws NullPointerException falls errorMapper null ist
     * @see #onErrorMap(Function)
     */
    Stream<T> onErrorMapChecked(ThrowingFunction<? super List<Exception>, ? extends T> errorMapper);

    /**
     * Filtert alle fehlerhaften Elemente aus dem Stream heraus.
     * <p>
     * <b>Falls der Stream ein "Checked Stream" ist, ist es es danach nicht mehr</b>
     *
     * @return ein Stream selben Typs, ohne fehlerhafte Elemente und garantiert kein
     *         "Checked Stream"
     */
    Stream<T> onErrorFilter();

    /**
     * Erzeugt einen neuen Stream, der den übergebenen Stream nutzt
     */
    static <T> Stream<T> of(java.util.stream.Stream<T> javaStream) {
        Objects.requireNonNull(javaStream, "javaStream");
        return new SourcePart<>(StreamIterator.of(javaStream));
    }

    /**
     * Erzeugt einen neuen Stream, der die Elemente der Collection enthält
     *
     * @throws NullPointerException falls die Collection null ist
     * @implSpec die Größe des erzeugten Streams ist bekannt und davon abhängige
     *           Operationen nutzten diese Information zur Optimierung
     */
    static <T> Stream<T> of(Collection<T> col) {
        Objects.requireNonNull(col, "col");
        return new SourcePart<>(StreamIterator.of(col));
    }

    /**
     * Erzeugt einen neuen Stream, der die Elemente des Sets enthält
     *
     * @throws NullPointerException falls das Set null ist
     * @implSpec die Größe des erzeugten Streams und dessen Duplikat-Freiheit ist
     *           bekannt und davon abhängige Operationen nutzten diese Information
     *           zur Optimierung
     */
    static <T> Stream<T> of(Set<T> set) {
        Objects.requireNonNull(set, "set");
        return new SourcePart<>(StreamIterator.of(set), new StreamCharacteristics(set.size(), true, false));
    }

    /**
     * Erzeugt einen neuen Stream, der die übergebenen Elemente enthält
     *
     * @throws NullPointerException falls das Array null ist
     * @implSpec die Größe des erzeugten Streams ist bekannt und davon abhängige
     *           Operationen nutzten diese Information zur Optimierung
     */
    @SafeVarargs
    static <T> Stream<T> of(T... elements) {
        Objects.requireNonNull(elements, "elements");
        return new SourcePart<>(StreamIterator.of(elements));
    }
}