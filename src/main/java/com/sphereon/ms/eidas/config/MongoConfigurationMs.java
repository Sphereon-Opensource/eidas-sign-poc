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

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.SimpleMongoClientDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

/**
 * Created by Sander on 27-2-2017.
 */
@Configuration("MongoConfiguration")
@EnableMongoRepositories(basePackages = {"com.sphereon.ms.eidas"}, mongoTemplateRef = "msMongoTemplate")
public class MongoConfigurationMs extends AbstractMongoConfigurationWithConverters {

    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MongoConfigurationMs.class);

    protected String dbHostAddress;

    protected int dbPort;

    protected String dbName;

    protected String userName;

    protected String password;

    protected boolean useSSL;

    private String connectionString;


    @Value("${sphereon.eidas.sign.poc.db.address:127.0.0.1}")
    public void setDBHostAddress(String value) {
        dbHostAddress = value;
    }


    @Value("${sphereon.eidas.sign.poc.db.port:27017}")
    public void setDBPort(int value) {
        dbPort = value;
    }


    @Value("${sphereon.edias-sign-poc.db.name:CRYPTOKEYS}")
    public void setDbName(String dbName) {
        this.dbName = dbName;
    }


    @Value("${sphereon.eidas-sign-poc.db.user-name:#{null}}")
    public void setUserName(String userName) {
        this.userName = userName;
    }


    @Value("${sphereon.eidas-sign-poc.db.password:#{null}}")
    public void setPassword(String password) {
        this.password = password;
    }


    @Value("${sphereon.eidas.sign.poc.db.use-ssl:false}")
    public void setUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
    }


    @Value("${sphereon.eidas-sign-poc.db.connection-string:#{null}}")
    public void setConnectionString(String connectionString) {
        this.connectionString = connectionString;
    }


    @Bean(name = "msMongoClient", destroyMethod = "close")
    public MongoClient msMongoClient() {
        logger.info("Initialising Mongo client from eidas-sign-poc MongoConfigurationMs.");
        if (StringUtils.isNoneBlank(connectionString)) {
            String connectStringWithDatabase = connectionString.replace("/?", '/' + dbName + '?');
            logger.info("The connection string is " + connectStringWithDatabase);
            return MongoClients.create(new ConnectionString(connectStringWithDatabase));
        } else {
            logger.info(String.format("The connection string is empty, using Mongo server on %s:%d", this.dbHostAddress, this.dbPort));
            return mongoClientFromProperties();
        }
    }


    private MongoClient mongoClientFromProperties() {
        final List<ServerAddress> serverAddresses = new ArrayList<>();
        serverAddresses.add(new ServerAddress(dbHostAddress, dbPort));

        final MongoClientSettings.Builder mongoClientSettings = MongoClientSettings.builder()
            .applyToClusterSettings(builder1 -> builder1.hosts(serverAddresses))
            .applyToSslSettings(sslSettings -> sslSettings.enabled(useSSL));
        if (userName != null && password != null) {
            mongoClientSettings.credential(MongoCredential.createCredential(userName, dbName, password.toCharArray()));
        }

        return MongoClients.create(mongoClientSettings.build());
    }


    @Bean(name = "msMongoDbFactory")
    public MongoDatabaseFactory msMongoDbFactory(@Qualifier("msMongoClient") MongoClient msMongoClient) {
        return new SimpleMongoClientDatabaseFactory(msMongoClient, dbName);
    }


    @Bean(name = {"msMongoTemplate", "mongoTemplate"})
    MongoTemplate msMongoTemplate(@Qualifier("msMongoDbFactory") MongoDatabaseFactory msMongoDbFactory,
        @Qualifier("msMappingMongoConverter") MappingMongoConverter msMappingMongoConverter) {
        return new MongoTemplate(msMongoDbFactory, msMappingMongoConverter);
    }


    @Bean(name = "msMappingMongoConverter")
    MappingMongoConverter msMappingMongoConverter(@Qualifier("msMongoDbFactory") MongoDatabaseFactory msMongoDbFactory) {
        return createMappingMongoConverter(msMongoDbFactory);
    }
}
