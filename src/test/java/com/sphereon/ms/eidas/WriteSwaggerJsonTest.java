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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sphereon.ms.eidas.config.RestControllerConfigTemplate;
import io.swagger.models.Swagger;
import io.swagger.parser.SwaggerParser;
import io.swagger.util.Json;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = EidasCertApplication.class)
@ActiveProfiles("write-swagger")
public class WriteSwaggerJsonTest {

    @Autowired
    private ObjectMapper objectMapper;


    @Value("${local.server.port}")
    private String port;

    @Test
    public void createSwaggerJsons() throws Exception {
        createSwaggerJson(RestControllerConfigTemplate.Mode.STORE, "swagger.store.json");
        createSwaggerJson(RestControllerConfigTemplate.Mode.SDK, "swagger.sdk.json");
    }


    private void createSwaggerJson(RestControllerConfigTemplate.Mode mode, String jsonFile) throws Exception {
        File swaggerDir = new File("src/main/resources/swagger");
        swaggerDir.mkdirs();

        File swaggerFile = new File(swaggerDir.getAbsolutePath() + File.separator + jsonFile);
        if (swaggerFile.exists()) {
            Assertions.assertTrue(swaggerFile.delete());
        }

        CloseableHttpClient client = HttpClients.createDefault();
        HttpGet get = new HttpGet(getServiceUrl(mode));
        CloseableHttpResponse response = client.execute(get);
        Assertions.assertNotNull(response);
        Assertions.assertEquals(200, response.getStatusLine().getStatusCode());
        SwaggerParser swaggerParser = new SwaggerParser();
        Swagger swagger = swaggerParser.read(getServiceUrl(mode));
        if (mode == RestControllerConfigTemplate.Mode.SDK) {
            swagger.setBasePath("/crypto/keys/0.9");
        }

        swaggerFile.setWritable(true);

        Files.write(swaggerFile.toPath(), Json.pretty(swagger).getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE);
        if (swaggerFile.length() <= 10) {
            Assertions.fail("Swagger file too small");
        }
        response.close();
        client.close();
    }


    private String getServiceUrl(RestControllerConfigTemplate.Mode mode) {
        return String.format("http://localhost:%s/v2/api-docs?group=%s", port, mode.getGroupName());
    }


}
