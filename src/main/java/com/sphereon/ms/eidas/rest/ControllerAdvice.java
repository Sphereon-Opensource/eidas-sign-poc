/*
 * Copyright (C) 2022 Sphereon BV
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.sphereon.ms.eidas.rest;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sphereon.ms.rest.response.error.ErrorResponse;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Created by niels on 17-9-16.
 */
@org.springframework.web.bind.annotation.ControllerAdvice
public class ControllerAdvice {
    private static final XLogger logger = XLoggerFactory.getXLogger(ControllerAdvice.class);

    @ResponseBody
    @ExceptionHandler(RestException.InvalidNameException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ErrorResponse imvalidNameExceptionHandler(RestException.InvalidNameException ex) {
        return handleError(ex);
    }


    @ResponseBody
    @ExceptionHandler(RestException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse storageExceptionHandler(RestException ex) {
        return handleError(ex);
    }


    @ResponseBody
    @ExceptionHandler(Throwable.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse fallbackExceptionHandler(Throwable t) {
        return handleError(t);
    }

    @ResponseBody
    @ExceptionHandler(JsonParseException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse fallbackExceptionHandler(JsonParseException jpe) {
        logger.error(jpe.getMessage(), jpe);
        return new ErrorResponse("400", jpe.getMessage());
    }

    private ErrorResponse handleError(Throwable ex) {
        logger.warn(ex.getMessage());
        if (logger.isDebugEnabled()) {
            logger.error(ex.getMessage(), ex);
        }
        return new ErrorResponse(ex);
    }
}
