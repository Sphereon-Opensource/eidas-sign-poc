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

package com.sphereon.commons.assertions;

/**
 * Created by sande on 28-6-2017.
 */
public class Assert extends org.springframework.util.Assert {

    public static void isExpected(String fieldName, Object valueFound, Object valueExpected) {
        if (valueFound == null && valueExpected == null) {
            return;
        }

        boolean isEqual;
        if (valueFound == null) {
            isEqual = false;
        } else {
            isEqual = valueFound.equals(valueExpected);
        }
        if (!isEqual) {
            throw new IllegalArgumentException(String.format("Field %s equals %s while %s was expected.", fieldName, valueFound, valueExpected));
        }

    }

}
