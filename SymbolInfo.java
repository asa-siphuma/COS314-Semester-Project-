public class SymbolInfo {
    String originalName;
    String uniqueName;
    String type;
    String scope;

    public SymbolInfo(String originalName, String uniqueName, String type, String scope) {
        this.uniqueName = uniqueName;
        this.type = type;
        this.scope = scope;
        this.originalName = originalName;
    }

    // Getter methods (optional) for accessing fields
    public String getUniqueName() {
        return uniqueName;
    }

    public String getOriginalName() {
        return originalName;
    }

    public String getType() {
        return type;
    }

    public String getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return "SymbolInfo [uniqueName=" + uniqueName + ", type=" + type + ", scope=" + scope + "]";
    }
}
