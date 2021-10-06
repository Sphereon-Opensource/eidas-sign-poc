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

package com.sphereon.ms.eidas;

import com.sphereon.ms.auth.jwt.JWTClaimAccess;
import com.sphereon.ms.auth.jwt.JWTContext;
import com.sphereon.ms.eidas.api.model.certificate.EidasCertificate;
import com.sphereon.ms.eidas.nosql.EidasCertificateRepository;
import com.sphereon.ms.eidas.service.EidasDelegate;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;

public class TestEidasDelegate {

    @Mock
    EidasCertificateRepository eidasCertificateRepository;

    @Mock
    JWTContext jwtContext;

    @InjectMocks
    EidasDelegate eidasDelegate;

    private final String testCertName = "test-cert";

    @BeforeEach
    public void init() {
        MockitoAnnotations.initMocks(this);
        String testCertBase64 = "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";
        String testOwner = "test-owner";
        EidasCertificate eidasCertificate = new EidasCertificate(
                testOwner,
            testCertName,
                testCertBase64
        );
        JWTClaimAccess jwtClaimAccess = Mockito.mock(JWTClaimAccess.class);
        Mockito.when(eidasCertificateRepository.findByOwnerAndName(any(), any()))
            .thenReturn(Optional.of(eidasCertificate));
        Mockito.when(jwtClaimAccess.getSphereonIdentifier(any(), any()))
            .thenReturn(testOwner);
        Mockito.when(jwtContext.claimAccess())
            .thenReturn(jwtClaimAccess);
    }

    @Test
    public void signatureTestShouldPass() {
        String testCertPassword = "testpassword";
        String signature = eidasDelegate.signWithCertificate(testCertName, testCertPassword,
            Base64.getEncoder().encodeToString("Hello".getBytes(StandardCharsets.UTF_8)));
        var response = eidasDelegate.verify(signature);
        Assertions.assertTrue(response.isVerified());
    }

    @Test
    public void signatureTestShouldFail() {
        String signature = "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";
        var verified = eidasDelegate.verify(signature);
        System.out.println(verified);
    }
}
