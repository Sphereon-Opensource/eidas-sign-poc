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


import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.github.dockerjava.api.model.Bind;
import com.sphereon.ms.eidas.api.model.certificate.EidasCertificateImportRequest;
import com.sphereon.ms.eidas.api.model.signature.CadesSignatureRequest;
import com.sphereon.ms.eidas.api.model.signature.CadesSignatureResponse;
import com.sphereon.ms.eidas.api.model.signature.CadesSignatureVerifyRequest;
import com.sphereon.ms.eidas.nosql.EidasCertificateRepository;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.restdocs.ManualRestDocumentation;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.MockMvcRestDocumentation;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.List;

import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessRequest;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.preprocessResponse;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith({RestDocumentationExtension.class, SpringExtension.class})
@SpringBootTest
@ActiveProfiles(value = {"test"})
@ContextConfiguration(initializers = DocumentationGenTest.MongoDbInitializer.class)
public class DocumentationGenTest {

    private static final XLogger logger = XLoggerFactory.getXLogger(DocumentationGenTest.class);

    static {
        System.setProperty("log4j.logger.com.sphereon", "DEBUG");
    }

    @Value(value = "${sphereon.api.version:${sphereon.application.version}}")
    private String apiVersion;

    private static MongoDBContainer mongoDbContainer;


    @Value(value = "http://localhost:21762")
    private String host;

    @Value(value = "${sphereon.api.gateway-protocol:http}")
    private String protocol;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private AuthenticationTestHandler authenticationTestHandler;


    @Autowired
    private EidasCertificateRepository certRepo;

    private final MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

    private static final String EXAMPLE_CERT = "{\n" +
            "  \n" +
            "  \"@context\": [\n" +
            "    \"https://www.w3.org/2018/credentials/v1\",\n" +
            "    \"https://www.w3.org/2018/credentials/examples/v1\"\n" +
            "  ],\n" +
            "  \n" +
            "  \"id\": \"http://example.edu/credentials/1872\",\n" +
            "  \n" +
            "  \"type\": [\"VerifiableCredential\", \"AlumniCredential\"],\n" +
            "  \n" +
            "  \"issuer\": \"https://example.edu/issuers/565049\",\n" +
            "  \n" +
            "  \"issuanceDate\": \"2010-01-01T19:23:24Z\",\n" +
            "  \n" +
            "  \"credentialSubject\": {\n" +
            "    \n" +
            "    \"id\": \"did:example:ebfeb1f712ebc6f1c276e12ec21\",\n" +
            "    \n" +
            "    \"alumniOf\": {\n" +
            "      \"id\": \"did:example:c276e12ec21ebfeb1f712ebc6f1\",\n" +
            "      \"name\": [{\n" +
            "        \"value\": \"Example University\",\n" +
            "        \"lang\": \"en\"\n" +
            "      }, {\n" +
            "        \"value\": \"Exemple d'Université\",\n" +
            "        \"lang\": \"fr\"\n" +
            "      }]\n" +
            "    }\n" +
            "  }" +
            "}";

    private final String OWNER = "test-owner";


    private ManualRestDocumentation restDocumentation;

    public DocumentationGenTest() throws NoSuchAlgorithmException {
    }

    @BeforeAll
    public static void startContainerAndPublicPortIsAvailable() {
        mongoDbContainer = new MongoDBContainer(DockerImageName.parse("mongo:4.0.10"));
        mongoDbContainer.setPortBindings(List.of("27017:27017"));

        mongoDbContainer.start();
    }

    @BeforeEach
    public void setUp(WebApplicationContext webApplicationContext,
                      RestDocumentationContextProvider restDocumentation) {
        this.mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .alwaysDo(document("{method-name}",
                        preprocessRequest(prettyPrint()), preprocessResponse(prettyPrint())))
                .apply(documentationConfiguration(restDocumentation).uris().withScheme(protocol).withHost(host)
                        .withPort(443)).build();
        objectMapper.configure(SerializationFeature.INDENT_OUTPUT, true)
                .configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Test
    public void createEndpointDocumentation() throws Exception {
        MockHttpServletResponse response;

        String accessToken = "Bearer " + authenticationTestHandler.getAccessToken();

        String CERT_BASEE64 = "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";
        String CERT_NAME = "test-cert";
        EidasCertificateImportRequest importRequest = new EidasCertificateImportRequest(CERT_NAME, CERT_BASEE64);


        /**
         * Import a certificate first
         */
        response = mockMvc.perform(
                        MockMvcRequestBuilders.post("/eidas/1.0/certificates").contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(importRequest))
                                .header("Authorization", accessToken))
                .andDo(MockMvcRestDocumentation.document("ImportCertificate", preprocessResponse(prettyPrint())))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        Assertions.assertNotNull(response);


        /**
         * Sign input data
         */
        String CERT_PASSWORD = "testpassword";
        final var signRequest = new CadesSignatureRequest(Base64.getEncoder().encodeToString(EXAMPLE_CERT.getBytes(StandardCharsets.UTF_8)), CERT_PASSWORD);
        response = mockMvc.perform(
                        MockMvcRequestBuilders.post(String.format("/eidas/1.0/certificates/%s/sign", CERT_NAME)).contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(signRequest))
                                .header("Authorization", accessToken))
                .andDo(MockMvcRestDocumentation.document("Sign", preprocessResponse(prettyPrint())))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        Assertions.assertNotNull(response);

        final var signatureResponse = objectMapper.readValue(response.getContentAsByteArray(), CadesSignatureResponse.class);
        Assertions.assertNotNull(signatureResponse.getSignature());


        /**
         * Verifies a signature
         */
        final var verifyRequest = new CadesSignatureVerifyRequest(signatureResponse.getSignature());
        response = mockMvc.perform(
                        MockMvcRequestBuilders.post("/eidas/1.0/signatures").contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsBytes(verifyRequest))
                                .header("Authorization", accessToken))
                .andDo(MockMvcRestDocumentation.document("Verify", preprocessResponse(prettyPrint())))
                .andExpect(status().isOk())
                .andReturn().getResponse();
        Assertions.assertNotNull(response);

    }



    public static class MongoDbInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {

            TestPropertyValues values = TestPropertyValues.of("spring.data.mongodb.host=" + mongoDbContainer.getContainerIpAddress(), "spring.data.mongodb.port=" + mongoDbContainer.getExposedPorts().get(0));
            values.applyTo(configurableApplicationContext);
        }
    }
}
