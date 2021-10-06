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

import static com.google.common.collect.Lists.newArrayList;
import static springfox.documentation.builders.RequestHandlerSelectors.basePackage;
import static springfox.documentation.schema.AlternateTypeRules.newRule;

import com.fasterxml.classmate.TypeResolver;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.Sets;
import com.sphereon.ms.rest.response.RequestHeaderAccess;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import javax.servlet.Filter;
import javax.servlet.ServletContext;
import org.apache.catalina.filters.RequestDumperFilter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.schema.WildcardType;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.ClientCredentialsGrant;
import springfox.documentation.service.GrantType;
import springfox.documentation.service.LoginEndpoint;
import springfox.documentation.service.OAuth;
import springfox.documentation.service.ResourceOwnerPasswordCredentialsGrant;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.service.SecurityScheme;
import springfox.documentation.service.Tag;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;


@Configuration
@EnableSwagger2
public class RestControllerConfigTemplate implements ServletContextAware {

    private ServletContext servletContext;


    @Override
    public void setServletContext(final ServletContext servletContext) {
        this.servletContext = servletContext;
    }


    public enum Mode {
        STORE("store"), SDK("default");
        Mode(String groupName) {
            this.groupName = groupName;
        }

        private final String groupName;
        public String getGroupName() {
            return groupName;
        }

    }


    @Bean
    Map<Mode, Docket> dockets(SimplifiedDocketConfigurator docketConfigurator,
        @Qualifier("sphereonDocketConfig") SphereonDocketConfig sphereonDocketConfig, SecurityScheme securitySchema,
        SecurityContext securityContext, TypeResolver typeResolver) {
        Map<Mode, Docket> docketMap = new HashMap<>();
        for (Mode mode : Mode.values()) {
            ApiInfo apiInfo = getApiInfo(docketConfigurator, mode);
            Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .groupName(mode.getGroupName())
                .apiInfo(apiInfo)
                .host(mode == Mode.SDK ? sphereonDocketConfig.getGatewayHostName() : "localhost")
                .protocols(mode == Mode.SDK ? Sets.newHashSet("https") : Sets.newHashSet("http"))
                .select()
                .apis(basePackage("com.sphereon.ms.eidas"))

//                .apis(requestHandler -> sphereonDocketConfig.getApiSelector().test(requestHandler))
                .paths(sphereonDocketConfig.getPathSelector())
                .build()
                .directModelSubstitute(LocalDate.class, java.sql.Date.class)
                .directModelSubstitute(OffsetDateTime.class, Date.class)
                // Add below substitution since models would be created otherwise
                // Bug in Springfox: since a list of bytearrays would be returned otherwise
                .directModelSubstitute(byte[].class, byte.class)
                .genericModelSubstitutes(ResponseEntity.class)
                .alternateTypeRules(
                    newRule(typeResolver.resolve(List.class, OffsetDateTime.class), typeResolver.resolve(List.class, Date.class)),
                    newRule(typeResolver.resolve(DeferredResult.class, typeResolver.resolve(ResponseEntity.class, WildcardType.class)),
                        typeResolver.resolve(WildcardType.class)))
                .securitySchemes(mode == Mode.STORE ? newArrayList() : newArrayList(securitySchema))
                .securityContexts(mode == Mode.STORE ? newArrayList() : newArrayList(securityContext))
                .enableUrlTemplating(false)
                .useDefaultResponseMessages(false)
                .tags(sphereonDocketConfig.getFirstTag(), sphereonDocketConfig.getAdditionalTags());
            docketMap.put(mode, docket);
        }
        return docketMap;
    }


    @Bean
    SecurityContext securityContext(@Qualifier("sphereonDocketConfig") SphereonDocketConfig sphereonSphereonDocketConfig,
        List<SecurityReference> defaultAuth) {
        return SecurityContext.builder()
            .securityReferences(defaultAuth)
            .forPaths(sphereonSphereonDocketConfig.getPathSelector())
            .build();
    }


    @Bean
    Docket docketStore(Map<Mode, Docket> dockets) {
        return dockets.get(Mode.STORE);
    }


    @Bean
    Docket docketSdk(Map<Mode, Docket> dockets) {
        return dockets.get(Mode.SDK);
    }


    @Bean
    FilterRegistrationBean corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
        bean.setOrder(0);
        return bean;
    }


    @Bean
    public RequestHeaderAccess requestHeaderAccess() {
        return new RequestHeaderAccess();
    }


    @Bean
    OAuth securitySchema(@Qualifier("sphereonDocketConfig") SphereonDocketConfig sphereonDocketConfig) {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        LoginEndpoint loginEndpoint = new LoginEndpoint("https://" + sphereonDocketConfig.getGatewayHostName() + "/token");
        GrantType ownerGrantType = new ResourceOwnerPasswordCredentialsGrant(loginEndpoint.getUrl());
        GrantType clientGrantType = new ClientCredentialsGrant(loginEndpoint.getUrl());
        return new OAuth("oauth2schema", newArrayList(authorizationScope), newArrayList(ownerGrantType, clientGrantType));
    }


    @Bean
    List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope
            = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return newArrayList(new SecurityReference("oauth2schema", authorizationScopes));
    }


    @Bean
    @ConditionalOnProperty(name = "sphereon.request-logging-filter.enable", havingValue = "true")
    Filter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(5120);
        return filter;
    }


    @Bean
    @ConditionalOnProperty(name = "sphereon.request-dumper-filter.enable", havingValue = "true")
    public FilterRegistrationBean requestDumperFilter() {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        Filter requestDumperFilter = new RequestDumperFilter();
        registration.setFilter(requestDumperFilter);
        registration.addUrlPatterns("/*");
        return registration;
    }


    @Bean
    MappingJackson2HttpMessageConverter mappingJackson2HttpMessageConverter(@Qualifier("primaryObjectMapper") ObjectMapper objectMapper) {
        MappingJackson2HttpMessageConverter jsonConverter = new MappingJackson2HttpMessageConverter();
        jsonConverter.setObjectMapper(objectMapper);
        List<MediaType> mediaTypes = new ArrayList<>(jsonConverter.getSupportedMediaTypes());
        mediaTypes.add(MediaType.APPLICATION_JSON);
        mediaTypes.add(MediaType.TEXT_HTML);
        mediaTypes.add(MediaType.valueOf("text/html;charset=utf-8"));
        jsonConverter.setSupportedMediaTypes(mediaTypes);
        return jsonConverter;
    }


    @Bean
    @Primary
    @Qualifier("primaryObjectMapper")
    public ObjectMapper primaryObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        objectMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
        objectMapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        return objectMapper;
    }


    //    @LoadBalanced
    @Bean
    RestTemplate restTemplate(@Qualifier("mappingJackson2HttpMessageConverter") MappingJackson2HttpMessageConverter httpMessageConverter) {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getMessageConverters().add(httpMessageConverter);
        return restTemplate;
    }


    public interface SimplifiedDocketConfigurator {

        void configureDocket(Builder docketBuilder);

        void configureApiInfo(ApiInfoBuilder apiInfoBuilder, Mode mode);


        final class Builder {
            final SphereonDocketConfig sphereonDocketConfig = new SphereonDocketConfig();


            public Builder withGatewayHostname(String gatewayHostName) {
                sphereonDocketConfig.setGatewayHostName(gatewayHostName);
                return this;
            }


            public Builder withPathMapping(Mode mode, String pathMapping) {
                sphereonDocketConfig.setPathMapping(mode, pathMapping);
                return this;
            }


            public Builder withApiSelector(Predicate<RequestHandler> apiSelector) {
                sphereonDocketConfig.setApiSelector(apiSelector);
                return this;
            }


            public Builder withApiSelector(String basePackage) {
                sphereonDocketConfig.setApiSelector(basePackage(basePackage));
                return this;
            }


            public Builder withPathSelector(Predicate<String> selector) {
                sphereonDocketConfig.setPathSelector(selector);
                return this;
            }


            public Builder withPathSelector(String regexSelector) {
                final java.util.function.Predicate<String> regexPredicate = PathSelectors.regex(regexSelector);
                sphereonDocketConfig.setPathSelector(regexPredicate);
                return this;
            }


            public Builder withTags(Tag first, Tag... remaining) {
                sphereonDocketConfig.firstTag = first;
                sphereonDocketConfig.additionalTags.addAll(Sets.newHashSet(remaining));
                return this;
            }


            public SphereonDocketConfig build() {
                return sphereonDocketConfig;
            }
        }
    }


    private static class SphereonDocketConfig {

        @Value("${sphereon.api.gateway-address:gw.api.cloud.sphereon.com}")
        private String gatewayHostName;

        private final Map<Mode, String> pathMappings = new HashMap<>();

        private final Set<Tag> additionalTags = Sets.newHashSet();

        private Tag firstTag;

        private Predicate<RequestHandler> apiSelector = RequestHandlerSelectors.any();

        private Predicate<String> pathSelector;


        public String getGatewayHostName() {
            return gatewayHostName;
        }


        public void setGatewayHostName(String gatewayHostName) {
            this.gatewayHostName = gatewayHostName;
        }


        public Map<Mode, String> getPathMappings() {
            return pathMappings;
        }


        public String getPathMapping(Mode mode) {
            return pathMappings.get(mode);
        }

        public void setPathMapping(Mode mode, String pathMapping) {
            pathMappings.put(mode, pathMapping);
        }


        public Predicate<RequestHandler> getApiSelector() {
            return apiSelector;
        }


        public void setApiSelector(Predicate<RequestHandler> apiSelector) {
            this.apiSelector = apiSelector;
        }


        public Predicate<String> getPathSelector() {
            return pathSelector;
        }


        public void setPathSelector(Predicate<String> pathSelector) {
            this.pathSelector = pathSelector;
        }


        public Tag getFirstTag() {
            return firstTag;
        }


        Tag[] getAdditionalTags() {
            return additionalTags.toArray(new Tag[0]);
        }

    }


    @Bean
    SphereonDocketConfig sphereonDocketConfig(SimplifiedDocketConfigurator docketConfigurator) {
        SimplifiedDocketConfigurator.Builder docketBuilder = new SimplifiedDocketConfigurator.Builder();
        docketConfigurator.configureDocket(docketBuilder);
        return docketBuilder.build();
    }


    private ApiInfo getApiInfo(SimplifiedDocketConfigurator docketConfigurator, Mode mode) {
        ApiInfoBuilder apiInfoBuilder = new ApiInfoBuilder();
        docketConfigurator.configureApiInfo(apiInfoBuilder, mode);
        return apiInfoBuilder.build();
    }


    @Bean
    public WebMvcConfigurer forwardToIndex() {
        return new WebMvcConfigurer() {
            @Override
            public void addViewControllers(ViewControllerRegistry registry) {
                // forward requests to /admin and /user to their index.html
                registry.addViewController("/").setViewName(
                    "forward:/docs/index.html");
                registry.addViewController("/docs/html").setViewName(
                    "forward:/docs/index.html");
                registry.addViewController("/docs").setViewName(
                    "forward:/docs/index.html");
                registry.addViewController("/docs/pdf").setViewName(
                    "forward:/docs/index.pdf");
            }
        };
    }
}
