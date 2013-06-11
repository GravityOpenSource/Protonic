package ca.corefacility.bioinformatics.irida.web.controller.api.exception;

import ca.corefacility.bioinformatics.irida.exceptions.EntityExistsException;
import ca.corefacility.bioinformatics.irida.exceptions.EntityNotFoundException;
import ca.corefacility.bioinformatics.irida.exceptions.InvalidPropertyException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.google.gson.Gson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.*;

/**
 * Globally handles exceptions thrown by controllers.
 *
 * @author Franklin Bristow <franklin.bristow@phac-aspc.gc.ca>
 */
@ControllerAdvice
public class ControllerExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ControllerExceptionHandler.class);

    /**
     * Handle {@link Exception}.
     *
     * @param e the exception as thrown by the service.
     * @return an appropriate HTTP response.
     */
    @SuppressWarnings("unused")
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllOtherExceptions(Exception e) {
        logger.error("An exception happened at " + new Date() + ". The stack trace follows: ", e);
        return new ResponseEntity<>("Server error.", HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * Handle {@link ca.corefacility.bioinformatics.irida.exceptions.InvalidPropertyException}.
     *
     * @param e the exception as thrown by the service.
     * @return an appropriate HTTP response.
     */
    @ExceptionHandler(InvalidPropertyException.class)
    public ResponseEntity<String> handleInvalidPropertyException(InvalidPropertyException e) {
        logger.info("A client attempted to update a resource with an" +
                " invalid property at " + new Date() + ". The stack trace follows: ", e);
        return new ResponseEntity<>("Cannot update resource with supplied properties.", HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle {@link ca.corefacility.bioinformatics.irida.exceptions.EntityNotFoundException}.
     *
     * @param e the exception as thrown by the service.
     * @return an appropriate HTTP response.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<String> handleNotFoundException(EntityNotFoundException e) {
        logger.info("A client attempted to retrieve a resource with an identifier" +
                " that does not exist at " + new Date() + ". The stack trace follows: ", e);
        return new ResponseEntity<>("No such resource found.", HttpStatus.NOT_FOUND);
    }

    /**
     * Handle {@link javax.validation.ConstraintViolationException}.
     *
     * @param e the exception as thrown by the service.
     * @return an appropriate HTTP response.
     */
    @SuppressWarnings("unchecked")
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<String> handleConstraintViolations(ConstraintViolationException e) {
        Set<ConstraintViolation<?>> constraintViolations = new HashSet<>();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            constraintViolations.add(violation);
        }
        logger.info("A client attempted to create or update a resource with invalid values at " + new Date());
        return new ResponseEntity<>(validationMessages(constraintViolations), HttpStatus.BAD_REQUEST);
    }

    /**
     * Handle {@link ca.corefacility.bioinformatics.irida.exceptions.EntityExistsException}.
     *
     * @param e the exception as thrown by the service.
     * @return an appropriate HTTP response.
     */
    @SuppressWarnings("unused")
    @ExceptionHandler(EntityExistsException.class)
    public ResponseEntity<String> handleExistsException(EntityExistsException e) {
        logger.info("A client attempted to create a new resource with an identifier that exists, " +
                "or modify a resource to have an identifier that already exists at " + new Date());
        return new ResponseEntity<>("An entity already exists with that identifier.", HttpStatus.CONFLICT);
    }

    /**
     * Handle {@link com.fasterxml.jackson.core.JsonParseException}.
     *
     * @param e the exception as thrown by the JSON parser.
     * @return an appropriate HTTP response.
     */
    @SuppressWarnings("unused")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<String> handleInvalidJsonException(HttpMessageNotReadableException e) {
        String message = "Your request could not be parsed.";
        Throwable cause = e.getCause();
        if (cause instanceof UnrecognizedPropertyException) {
            // this is thrown when Jackson tries to de-serialize JSON into an object and the JSON object has a field that
            // doesn't exist
            UnrecognizedPropertyException unrecognizedProperty = (UnrecognizedPropertyException) cause;
            String propertyName = unrecognizedProperty.getUnrecognizedPropertyName();
            Collection<Object> acceptableProperties = unrecognizedProperty.getKnownPropertyIds();
            StringBuilder builder = new StringBuilder("Unrecognized property [");
            builder.append(propertyName).append("] in JSON request. The object that you were trying to create or update accepts the following fields: [\n");
            for (Object acceptableProperty : acceptableProperties) {
                // DON'T append the links entry
                if (!acceptableProperty.equals("links")) {
                    builder.append(acceptableProperty).append(",\n");
                }
            }
            builder.append("].");
            message = builder.toString();
        } else if (cause instanceof JsonParseException) {
            JsonParseException parseException = (JsonParseException) cause;
            if (parseException.getMessage().contains("double-quote")) {
                message = "Your request could not be parsed. Field names must be surrounded by double quotes (see: http://www.json.org/).";
            }
        }

        return new ResponseEntity<>(message, HttpStatus.BAD_REQUEST);
    }

    /**
     * Render a collection of constraint violations as a JSON object.
     *
     * @param failures the set of constraint violations.
     * @return the constraint violations as a JSON object.
     */
    private String validationMessages(Set<ConstraintViolation<?>> failures) {
        Map<String, List<String>> mp = new HashMap<>();
        for (ConstraintViolation<?> failure : failures) {
            logger.debug(failure.getPropertyPath().toString() + ": " + failure.getMessage());
            String property = failure.getPropertyPath().toString();
            if (mp.containsKey(property)) {
                mp.get(failure.getPropertyPath().toString()).add(failure.getMessage());
            } else {
                List<String> list = new ArrayList<>();
                list.add(failure.getMessage());
                mp.put(property, list);
            }
        }
        return new Gson().toJson(mp);
    }
}
