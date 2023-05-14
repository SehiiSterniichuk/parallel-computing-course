package lab4.config;

public enum Prefix {
    SIZE("size: "), THREADS("number-of-threads: "), ID("id: "), ERROR("error: "), TIME("execution-time: ");
    public final String v;

    Prefix(String v) {
        this.v = v;
    }

    @Override
    public String toString() {
        return v;
    }
}
