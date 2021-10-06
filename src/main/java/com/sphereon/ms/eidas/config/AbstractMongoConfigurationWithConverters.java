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

import com.mongodb.BasicDBObject;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.data.annotation.Persistent;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mapping.model.CamelCaseAbbreviatingFieldNamingStrategy;
import org.springframework.data.mapping.model.FieldNamingStrategy;
import org.springframework.data.mapping.model.PropertyNameFieldNamingStrategy;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.CustomConversions;
import org.springframework.data.mongodb.core.convert.DbRefResolver;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

public abstract class AbstractMongoConfigurationWithConverters {

    public MongoMappingContext mongoMappingContext() throws ClassNotFoundException {
        MongoMappingContext mappingContext = new MongoMappingContext();
        mappingContext.setInitialEntitySet(this.getInitialEntitySet());
        mappingContext.setSimpleTypeHolder(this.customConversions().getSimpleTypeHolder());
        mappingContext.setFieldNamingStrategy(this.fieldNamingStrategy());
        return mappingContext;
    }


    protected MappingMongoConverter createMappingMongoConverter(MongoDatabaseFactory mongoDbFactory) {
        try {
            DbRefResolver dbRefResolver = new DefaultDbRefResolver(mongoDbFactory);
            MappingMongoConverter converter = new MappingMongoConverter(dbRefResolver, this.mongoMappingContext());
            converter.setCustomConversions(this.customConversions());
            return converter;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Could not create MappingMongoConverter", e);
        }
    }


    protected FieldNamingStrategy fieldNamingStrategy() {
        return this.abbreviateFieldNames() ? new CamelCaseAbbreviatingFieldNamingStrategy() : PropertyNameFieldNamingStrategy.INSTANCE;
    }


    protected boolean abbreviateFieldNames() {
        return false;
    }


    protected Set<Class<?>> getInitialEntitySet() throws ClassNotFoundException {
        String basePackage = this.getMappingBasePackage();
        Set<Class<?>> initialEntitySet = new HashSet();
        if (StringUtils.hasText(basePackage)) {
            ClassPathScanningCandidateComponentProvider componentProvider = new ClassPathScanningCandidateComponentProvider(false);
            componentProvider.addIncludeFilter(new AnnotationTypeFilter(Document.class));
            componentProvider.addIncludeFilter(new AnnotationTypeFilter(Persistent.class));
            Iterator var4 = componentProvider.findCandidateComponents(basePackage).iterator();

            while (var4.hasNext()) {
                BeanDefinition candidate = (BeanDefinition) var4.next();
                initialEntitySet.add(ClassUtils.forName(candidate.getBeanClassName(), AbstractMongoConfigurationWithConverters.class.getClassLoader()));
            }
        }

        return initialEntitySet;
    }


    protected String getMappingBasePackage() {
        Package mappingBasePackage = this.getClass().getPackage();
        return mappingBasePackage == null ? null : mappingBasePackage.getName();
    }


    public CustomConversions customConversions() {
        return new CustomConversions(Arrays.asList(
                JSR310InstantToStringConverter.INSTANCE,
                StringToJSR310InstantConverter.INSTANCE,
                BasicDBObjectToJSR310InstantConverter.INSTANCE,
                DateToJSR310InstantConverter.INSTANCE));
    }


    @WritingConverter
    public enum JSR310InstantToStringConverter implements org.springframework.core.convert.converter.Converter<OffsetDateTime, String> {
        INSTANCE;

        @Nullable
        public String convert(OffsetDateTime source) {
            // We discard the nano seconds since it would not fit in the long
            return source == null ? null : source.toString();
        }
    }

    @ReadingConverter
    public enum StringToJSR310InstantConverter implements org.springframework.core.convert.converter.Converter<String, OffsetDateTime> {
        INSTANCE;

        @Nullable
        public OffsetDateTime convert(String source) {
            return source == null ? null : OffsetDateTime.parse(source);
        }
    }

    @ReadingConverter
    public enum BasicDBObjectToJSR310InstantConverter implements org.springframework.core.convert.converter.Converter<BasicDBObject, OffsetDateTime> {
        INSTANCE;

        @Nullable
        public OffsetDateTime convert(BasicDBObject source) {
            Date time = (Date) source.get("dateTime");
            String offset = (String) source.get("offset");
            return source == null ? null : OffsetDateTime.ofInstant(time.toInstant(), ZoneId.of(offset));
        }
    }

    @ReadingConverter
    public enum DateToJSR310InstantConverter implements org.springframework.core.convert.converter.Converter<Date, OffsetDateTime> {
        INSTANCE;

        @Nullable
        public OffsetDateTime convert(Date source) {
            return source == null ? null : OffsetDateTime.ofInstant(source.toInstant(), ZoneId.systemDefault());
        }
    }
}
