package dev.vetra.api.shared.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

import java.util.List;

@Provider
public class GlobalExceptionHandler implements ExceptionMapper<Throwable> {

    private static final Logger LOG = Logger.getLogger(GlobalExceptionHandler.class);

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Throwable exception) {
        String path = uriInfo != null ? uriInfo.getPath() : "unknown";

        if (exception instanceof ForbiddenException fe) {
            LOG.warnf("Forbidden: path=%s, code=%s, message=%s", path, fe.errorCode(), fe.getMessage());
            return buildResponse(Response.Status.FORBIDDEN, fe.errorCode(), fe.getMessage(), path);
        }

        if (exception instanceof NotFoundException nfe) {
            LOG.warnf("Resource not found: path=%s, message=%s", path, nfe.getMessage());
            return buildResponse(Response.Status.NOT_FOUND, nfe.errorCode(), nfe.getMessage(), path);
        }

        if (exception instanceof DuplicateException de) {
            LOG.warnf("Duplicate resource: path=%s, message=%s", path, de.getMessage());
            return buildResponse(Response.Status.CONFLICT, de.errorCode(), de.getMessage(), path);
        }

        if (exception instanceof BusinessException be) {
            LOG.warnf("Business rule violation: path=%s, code=%s, message=%s", path, be.errorCode(), be.getMessage());
            ErrorResponse body = ErrorResponse.of(422, be.errorCode(), be.getMessage(), path);
            return Response.status(422).entity(body).build();
        }

        if (exception instanceof ConstraintViolationException cve) {
            LOG.warnf("Validation error: path=%s, violations=%d", path, cve.getConstraintViolations().size());
            List<ErrorResponse.FieldError> details = cve.getConstraintViolations().stream()
                    .map(v -> new ErrorResponse.FieldError(
                            extractFieldName(v.getPropertyPath().toString()),
                            v.getMessage()
                    ))
                    .toList();

            ErrorResponse body = ErrorResponse.of(
                    Response.Status.BAD_REQUEST.getStatusCode(),
                    "VALIDATION_ERROR",
                    "Invalid request",
                    path,
                    details
            );
            return Response.status(Response.Status.BAD_REQUEST).entity(body).build();
        }

        LOG.error("Unhandled exception on " + path, exception);
        return buildResponse(
                Response.Status.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "An unexpected error occurred",
                path
        );
    }

    private Response buildResponse(Response.Status status, String error, String message, String path) {
        ErrorResponse body = ErrorResponse.of(status.getStatusCode(), error, message, path);
        return Response.status(status).entity(body).build();
    }

    private String extractFieldName(String propertyPath) {
        if (propertyPath == null || propertyPath.isEmpty()) {
            return "unknown";
        }
        int lastDot = propertyPath.lastIndexOf('.');
        return lastDot >= 0 ? propertyPath.substring(lastDot + 1) : propertyPath;
    }
}
