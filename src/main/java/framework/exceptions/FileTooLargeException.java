package framework.exceptions;

public class FileTooLargeException extends RuntimeException {
    private long maxSize;
    private long actualSize;
    
    public FileTooLargeException(long maxSize, long actualSize) {
        super("Fichier trop grand : " + actualSize + " octets (max : " + maxSize + " octets)");
        this.maxSize = maxSize;
        this.actualSize = actualSize;
    }
    
    public long getMaxSize() { return maxSize; }
    public long getActualSize() { return actualSize; }
}