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

import com.sphereon.ms.eidas.service.EidasDelegate;
import com.sphereon.ms.eidas.api.model.certificate.EidasCertificateImportRequest;
import com.sphereon.ms.eidas.api.model.signature.CadesSignatureRequest;
import com.sphereon.ms.eidas.api.model.signature.CadesSignatureResponse;
import com.sphereon.ms.eidas.api.model.signature.CadesSignatureVerifyRequest;
import com.sphereon.ms.eidas.api.model.signature.CadesSignatureVerifyResponse;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/eidas/1.0")
public class EidasController {
    private final EidasDelegate eidasDelegate;

    public EidasController(EidasDelegate eidasDelegate) {
        this.eidasDelegate = eidasDelegate;
    }

    @ApiOperation(nickname = "ImportCertificate", value = "Import certificate", notes = "Import a X509 certificate in base65 form", tags = {"Certificates"})
    @PostMapping(value = "/certificates")
    public ResponseEntity importCertificate(@RequestBody EidasCertificateImportRequest certificateImportRequest) {
        eidasDelegate.persistEidasCertificate(certificateImportRequest);
        return ResponseEntity.ok().build();
    }

    @ApiOperation(nickname = "Sign", value = "Sign input data", notes = "Create a signature using the named certificate", tags = {"Certificates"})
    @PostMapping(value = "/certificates/{name}/sign")
    public CadesSignatureResponse sign(
        @PathVariable String name,
        @RequestBody CadesSignatureRequest cadesSignatureRequest) {
        var signature = eidasDelegate.signWithCertificate(name, cadesSignatureRequest.getPassword(), cadesSignatureRequest.getContent());
        return new CadesSignatureResponse(signature);
    }

    @ApiOperation(nickname = "Verify", value = "Verify input data", notes = "Verifies a signature", tags = {"Signatures"})
    @PostMapping(value = "/signatures")
    public CadesSignatureVerifyResponse verifyCadesSignature(
        @RequestBody CadesSignatureVerifyRequest cadesSignatureVerifyRequest) {
        return eidasDelegate.verify(cadesSignatureVerifyRequest.getSignature());
    }
}
