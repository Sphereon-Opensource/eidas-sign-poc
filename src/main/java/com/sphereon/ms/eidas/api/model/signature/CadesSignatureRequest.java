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

package com.sphereon.ms.eidas.api.model.signature;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class CadesSignatureRequest {
    private final String content;
    private final String password;

    @JsonCreator
    public CadesSignatureRequest(@JsonProperty("content") String content, @JsonProperty("password") String password) {
        this.content = content;
        this.password = password;
    }

    public String getContent() {
        return content;
    }

    public String getPassword() {
        return password;
    }
}
