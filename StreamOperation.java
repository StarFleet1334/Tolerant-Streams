
interface StreamOperation<T> {

    /**
     * Notifies the begin of consuming upstream elements and allows for
     * preparations.
     */
    void start(StreamCharacteristics upstreamCharacteristics);

    void acceptElement(StreamElement<T> element);

    /**
     * Notifies the end of new upstream elements and allows to pass accumulated
     * elements/state to be passed downstream
     */
    void finish();

    /**
     *
     * @return true if the StreamOperation wishes to receive more Elements
     */
    boolean needsMoreElements();
}
