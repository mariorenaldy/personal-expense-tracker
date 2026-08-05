package com.mariorenaldy.expensetracker.exception;

public class DuplicateCategoryException extends RuntimeException {
    public DuplicateCategoryException(String message){
        super(message);
    }
}
