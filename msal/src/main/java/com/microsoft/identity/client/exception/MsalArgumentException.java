package com.microsoft.identity.client.exception;

import com.microsoft.identity.common.java.exception.ArgumentException;

public class MsalArgumentException extends MsalException {

    public static final String ACQUIRE_TOKEN_OPERATION_NAME =
            ArgumentException.ACQUIRE_TOKEN_OPERATION_NAME;
    public static final String ACQUIRE_TOKEN_SILENT_OPERATION_NAME =
            ArgumentException.ACQUIRE_TOKEN_SILENT_OPERATION_NAME;

    public static final String SCOPE_ARGUMENT_NAME = ArgumentException.SCOPE_ARGUMENT_NAME;
    public static final String IACCOUNT_ARGUMENT_NAME = ArgumentException.IACCOUNT_ARGUMENT_NAME;
    public static final String AUTHORITY_REQUIRED_FOR_SILENT =
            "Authority must be specified for acquireTokenSilent";

    private static final String ILLEGAL_ARGUMENT_ERROR_CODE = "illegal_argument_exception";

    private String mOperationName;
    private String mArgumentName;

    public MsalArgumentException(final String argumentName, final String message) {
        super(ILLEGAL_ARGUMENT_ERROR_CODE, message);
        mArgumentName = argumentName;
    }

    public MsalArgumentException(
            final String operationName, final String argumentName, final String message) {
        super(ILLEGAL_ARGUMENT_ERROR_CODE, message);
        mOperationName = operationName;
        mArgumentName = argumentName;
    }

    public MsalArgumentException(
            final String operationName,
            final String argumentName,
            final String message,
            final Throwable throwable) {
        super(ILLEGAL_ARGUMENT_ERROR_CODE, message, throwable);
        mOperationName = operationName;
        mArgumentName = argumentName;
    }

    public String getOperationName() {
        return mOperationName;
    }

    public String getArgumentName() {
        return mArgumentName;
    }
}
