package com.example.watki;

public class FileDetails {


    private final int size;
    private final String type;
    private final boolean hasError;
    private final String errorMessage;

    //Kiedy nie ma errorw
    public FileDetails(int size, String type) {
        this.size = size;
        this.type = type;
        this.hasError = false;
        this.errorMessage = null;
    }

    //Kiedy jest error
    public FileDetails(int size, String type, boolean hasError, String errorMessage) {
        this.size = size;
        this.type = type;
        this.hasError = hasError;
        this.errorMessage = errorMessage;
    }
    public int getSize() {
        return size;
    }

    public String getType() {
        return type;
    }

    public boolean hasError() {
        return hasError;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

}
