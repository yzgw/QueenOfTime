package queen;

import java.time.Duration;
import java.time.ZonedDateTime;

class Interval {
    private static final int REST_THRESHOLD_MIN = 30;

    private ZonedDateTime started;
    private ZonedDateTime ended;

    public Interval(ZonedDateTime started, ZonedDateTime ended) {
        this.started = started;
        this.ended = ended;
    }

    public ZonedDateTime getStarted() {
        return started;
    }

    public ZonedDateTime getEnded() {
        return ended;
    }

    public Duration getDuration() {
        return Duration.between(this.started, this.ended);
    }

    public boolean isRest() {
        Duration duration = Duration.between(this.started, this.ended);
        return Math.abs(duration.toMinutes()) > REST_THRESHOLD_MIN;
    }
}
