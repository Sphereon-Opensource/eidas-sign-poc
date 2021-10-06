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

package com.sphereon.commons.testing.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import javax.annotation.PreDestroy;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.multipart.MultipartFile;

/**
 * Created by sander on 10-7-2017.
 */
@Component
public class ResourceLoader {

    private final File tempDir;


    public ResourceLoader() {
        tempDir = new File(FileUtils.getTempDirectory(), UUID.randomUUID().toString());
        tempDir.mkdirs();
    }


    @PreDestroy
    public void cleanup() throws IOException {
        FileUtils.forceDelete(tempDir);
    }


    public File getResourceAsFile(String resourcePath) throws IOException {
        File tempFile = getTempFile(resourcePath);
        InputStream inputStream = ResourceLoader.class.getResourceAsStream(resourcePath);
        Assert.notNull(inputStream, "No resource found named " + resourcePath);
        FileUtils.copyInputStreamToFile(inputStream, tempFile);
        return tempFile;
    }


    public InputStream getResourceAsStream(String resourcePath) {
        InputStream inputStream = ResourceLoader.class.getResourceAsStream(resourcePath);
        Assert.notNull(inputStream, "No resource found named " + resourcePath);
        return inputStream;
    }


    public MultipartFile getResourceAsMultiPartFile(String resourcePath) throws IOException {
        String fileName = FilenameUtils.getName(resourcePath);
        InputStream inputStream = ResourceLoader.class.getResourceAsStream(resourcePath);
        Assert.notNull(inputStream, "No resource found named " + resourcePath);
        return new MockMultipartFile(fileName, fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE, inputStream);
    }


    private File getTempFile(String resourcePath) {
        File tempFile = new File(tempDir, FilenameUtils.getName(resourcePath));
        if (tempFile.exists()) {
            tempFile.delete();
        }
        return tempFile;
    }

}
