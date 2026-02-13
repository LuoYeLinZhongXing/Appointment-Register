package com.luoye.exception;

public class PhoneOrPasswordIsNullException extends  BaseException{
    public PhoneOrPasswordIsNullException() {
    }
    public PhoneOrPasswordIsNullException(String msg) {
        super(msg);
    }
}
