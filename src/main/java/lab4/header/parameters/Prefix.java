package lab4.header.parameters;

public enum Prefix {
    SIZE("size: "), THREADS("number-of-threads: "), ID("id: ");
    public final String v;

    Prefix(String v) {
        this.v = v;
    }

    @Override
    public String toString() {
        return v;
    }
}
