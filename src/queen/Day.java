package queen;

import java.time.ZonedDateTime;
import java.util.ArrayList;

class Day {
    private ZonedDateTime started;
    private ZonedDateTime ended;
    private ArrayList<Interval> rests;

    public Day(ZonedDateTime started, ZonedDateTime ended, ArrayList<Interval> rests) {
        this.started = started;
        this.ended = ended;
        this.rests = rests;
    }

    public ZonedDateTime getStarted() {
        return started;
    }

    public ZonedDateTime getEnded() {
        return ended;
    }

    public ArrayList<Interval> getRests() {
        return rests;
    }
}
