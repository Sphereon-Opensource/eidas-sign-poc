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

/**
 * Created by niels on 17-9-16.
 */
public class RestException extends RuntimeException {

    private RestException(String error) {
        super(error);
    }


    private RestException(String error, Throwable cause) {
        super(error, cause);
    }


    private RestException(Throwable t) {
        super(t);
    }


    public static class UnknownException extends RestException {
        public UnknownException(String message) {
            super(message);
        }
    }

    public static class NotImplementedException extends RestException {
        public NotImplementedException(String message) {
            super(message);
        }
    }

    public static class InvalidNameException extends RestException {
        public InvalidNameException(String name) {
            super("Invalid id/name supplied: " + name);
        }
        public InvalidNameException(String name, String version) {
            super("Invalid id/name : " + name + " or version: " + version);
        }
    }

    public static class AzureAuthException extends RestException {
        public AzureAuthException(Throwable t) {
            super(t);
        }
    }

    public static class ConversionException extends RestException {
        public ConversionException(String message) {
            super("Conversion failed: " + message);
        }


        public ConversionException(Throwable t) {
            super(t);
        }
    }


    public static class DuplicateNameException extends RestException {
        public DuplicateNameException(String name) {
            super("Name already exists: " + name);
        }
    }

    public static class InvalidSignatureException extends RestException {
        public InvalidSignatureException(String msg) {
            super(msg);
        }
    }
}
