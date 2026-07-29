package framework.service;

import jakarta.servlet.http.Part;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class FileUpload {
    private Part part;
    private Map<String, byte[]> fileDataMap;  // Pour stocker nom + data
    
    public FileUpload(Part part) throws IOException {
        this.part = part;
        this.fileDataMap = new HashMap<>();
        
        // Stocker les données dans la map
        String filename = getFilename();
        byte[] data = part.getInputStream().readAllBytes();
        fileDataMap.put(filename, data);
    }
    
    // Getters de métadonnées
    public String getFilename() {
        String contentDisposition = part.getHeader("content-disposition");
        for (String content : contentDisposition.split(";")) {
            if (content.trim().startsWith("filename")) {
                return content.substring(content.indexOf('=') + 1).trim().replace("\"", "");
            }
        }
        return "unknown";
    }
    
    public String getContentType() {
        return part.getContentType();
    }
    
    public long getSize() {
        return part.getSize();
    }
    
    public String getExtension() {
        String filename = getFilename();
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }
    
    public boolean isEmpty() {
        return part.getSize() == 0;
    }
    
    // Accès aux données
    public byte[] getBytes() {
        return fileDataMap.get(getFilename());
    }
    
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(getBytes());
    }
    
    public Map<String, byte[]> getFileDataMap() {
        return fileDataMap;
    }
    
    // Sauvegarde simple
    public String saveTo(String directoryPath) throws IOException {
        String filename = getFilename();
        Path directory = Paths.get(directoryPath);
        
        // Créer le dossier s'il n'existe pas
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
        
        Path filePath = directory.resolve(filename);
        Files.write(filePath, getBytes());
        
        System.out.println(" Fichier sauvegardé : " + filePath.toAbsolutePath());
        return filePath.toString();
    }
    
    // Sauvegarde avec nom unique
    public String saveToWithUniqueName(String directoryPath) throws IOException {
        String originalFilename = getFilename();
        String extension = getExtension();
        String baseName = originalFilename.substring(0, 
            originalFilename.length() - extension.length());
        
        // Générer nom unique : nom_timestamp_random.ext
        String uniqueName = sanitizeFilename(baseName) + "_" + 
                           System.currentTimeMillis() + "_" + 
                           UUID.randomUUID().toString().substring(0, 8) + 
                           extension;
        
        Path directory = Paths.get(directoryPath);
        if (!Files.exists(directory)) {
            Files.createDirectories(directory);
        }
        
        Path filePath = directory.resolve(uniqueName);
        Files.write(filePath, getBytes());
        
        System.out.println(" Fichier sauvegardé (nom unique) : " + filePath.toAbsolutePath());
        return filePath.toString();
    }
    
    // Sanitize filename (supprimer caractères spéciaux)
    public static String sanitizeFilename(String filename) {
        // Remplacer espaces et caractères spéciaux
        return filename
            .replaceAll("[^a-zA-Z0-9._-]", "_")  // Remplacer par underscore
            .replaceAll("_+", "_")                // Consolider les underscores
            .toLowerCase();
    }
    
    // Validation
    public boolean isImage() {
        String contentType = getContentType();
        return contentType != null && contentType.startsWith("image/");
    }
    
    public boolean isPdf() {
        return "application/pdf".equals(getContentType());
    }
    
    public boolean hasExtension(String... extensions) {
        String ext = getExtension().toLowerCase();
        for (String allowed : extensions) {
            if (ext.equals(allowed.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
    
    @Override
    public String toString() {
        return "FileUpload{filename='" + getFilename() + "', size=" + getSize() + 
               ", contentType='" + getContentType() + "'}";
    }
}