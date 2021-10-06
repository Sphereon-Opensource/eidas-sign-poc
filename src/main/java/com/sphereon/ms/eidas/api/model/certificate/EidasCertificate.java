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

package com.sphereon.ms.eidas.api.model.certificate;

public class EidasCertificate {
    private final String owner;
    private final String name;
    private final String base64CertificateString;

    public EidasCertificate(String owner, String name, String base64CertificateString) {
        this.owner = owner;
        this.name = name;
        this.base64CertificateString = base64CertificateString;
    }

    public String getName() {
        return name;
    }

    public String getBase64CertificateString() {
        return base64CertificateString;
    }

    public String getOwner() {
        return owner;
    }
}
