

import java.util.OptionalLong;

final class StreamCharacteristics {

    private static final StreamCharacteristics REGULAR = new StreamCharacteristics(false, false);

    private final OptionalLong streamSize;
    private final boolean isDistinct;
    private final boolean isChecked;

    StreamCharacteristics(OptionalLong streamSize, boolean isDistinct, boolean isChecked) {
        this.streamSize = streamSize;
        this.isDistinct = isDistinct;
        this.isChecked = isChecked;
    }

    StreamCharacteristics(long streamSize, boolean isDistinct, boolean isChecked) {
        this.streamSize = OptionalLong.of(streamSize);
        this.isDistinct = isDistinct;
        this.isChecked = isChecked;
    }

    StreamCharacteristics(boolean isDistinct, boolean isChecked) {
        this(OptionalLong.empty(), isDistinct, isChecked);
    }

    OptionalLong getStreamSize() {
        return streamSize;
    }

    boolean isDistinct() {
        return isDistinct;
    }

    boolean isChecked() {
        return isChecked;
    }

    StreamCharacteristics withStreamSize(OptionalLong streamSize) {
        return new StreamCharacteristics(streamSize, isDistinct, isChecked);
    }

    StreamCharacteristics withUnknownStreamSize() {
        return new StreamCharacteristics(isDistinct, isChecked);
    }

    StreamCharacteristics withDistinct(boolean isDistinct) {
        return new StreamCharacteristics(streamSize, isDistinct, isChecked);
    }

    StreamCharacteristics withChecked(boolean isChecked) {
        return new StreamCharacteristics(streamSize, isDistinct, isChecked);
    }

    static StreamCharacteristics regular() {
        return REGULAR;
    }
}
