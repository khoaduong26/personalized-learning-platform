package com.tlcn.backend.academic.service;

public class AcademicNotFoundException extends RuntimeException {
    public AcademicNotFoundException(String message) {
        super(message);
    }
}
