package net.lazygun.micromuse;

/**
 * TODO: Write Javadocs for this class.
 * Created: 24/03/2014 12:56
 *
 * @author Ewan
 */
public class Link {

    private final Room from;
    private final String exit;
    private final Room to;

    public Link(Room from, String exit, Room to) {
        this.from = from;
        this.exit = exit;
        this.to = to;
    }

    public Room from() {
        return from;
    }

    public String exit() {
        return exit;
    }

    public Room to() {
        return to;
    }
}
