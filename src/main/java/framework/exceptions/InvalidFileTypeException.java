package framework.exceptions;

public class InvalidFileTypeException extends RuntimeException {
    private String actualType;
    private String[] allowedTypes;
    
    public InvalidFileTypeException(String actualType, String... allowedTypes) {
        super("Type de fichier non autorisé : " + actualType + 
              ". Types autorisés : " + String.join(", ", allowedTypes));
        this.actualType = actualType;
        this.allowedTypes = allowedTypes;
    }
    
    public String getActualType() { return actualType; }
    public String[] getAllowedTypes() { return allowedTypes; }
}
