package vn.edu.crs.registrationservice.exception;

public class CourseServiceUnavailableException extends RuntimeException {

    public CourseServiceUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
