public class SymbolInfo {
    private final String name;
    private final String type;
    private final String scope;
    private final String kind;  // 'variable' or 'function'

    public SymbolInfo(String name, String type, String scope, String kind) {
        this.name = name;
        this.type = type;
        this.scope = scope;
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getScope() {
        return scope;
    }

    public String getKind() {
        return kind;
    }

    @Override
    public String toString() {
        return "Name: " + name + ", Type: " + type + ", Scope: " + scope + ", Kind: " + kind;
    }
}
