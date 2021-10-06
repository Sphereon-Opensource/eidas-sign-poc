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

package com.sphereon.ms.eidas.config;

import com.sphereon.ms.eidas.config.RestControllerConfigTemplate.Mode;
import com.sphereon.ms.eidas.config.RestControllerConfigTemplate.SimplifiedDocketConfigurator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Tag;


@Component
public class DocketConfigurator implements SimplifiedDocketConfigurator {

  @Value(value = "${sphereon.api.version:${sphereon.application.version}}")
  private String apiVersion;

  @Value("${sphereon.api.doc-basepath:https://docs.sphereon.com/api/eidas-sign-poc/${sphereon.api.version}/html}")
  private String docsURL;


  @Override
  public void configureDocket(Builder docketBuilder) {
    docketBuilder.withPathMapping(Mode.SDK, "/eidas/" + apiVersion)
        .withPathSelector("^/(?!error).*")
        .withTags(
            new Tag("Certificates", "Certificates and signing"),
            new Tag("Signatures", "Signature verification"))
    ;

    docketBuilder.withPathMapping(Mode.STORE, "")
        .withPathSelector("^/(?!error).*")
        .withTags(
                new Tag("Certificates", "Certificates and signing"),
                new Tag("Signatures", "Signature verification")
        );
  }


  @Override
  public void configureApiInfo(ApiInfoBuilder apiInfoBuilder, Mode mode) {
    apiInfoBuilder.title("Eidas Sign API")
        .description(description(mode))

        .termsOfServiceUrl(docsURL)
        .contact(new Contact("Sphereon", "https://sphereon.com", "dev@sphereon.com"))
        .license("Apache License Version 2.0")
        .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0")
        .version(apiVersion);
  }


  private String description(Mode mode) {
    String desc ;

    desc = "The eIdas signature API is a PoC API that allows you to sign objects using X509 / eidas compliant signatures.";

    if (mode == Mode.STORE) {
      desc = desc.replaceAll("<[^>]+>", "");
    }
    return desc;

  }

}
