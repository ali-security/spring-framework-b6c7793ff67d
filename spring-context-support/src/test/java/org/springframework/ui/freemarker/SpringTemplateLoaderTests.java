/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ui.freemarker;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringTemplateLoader}.
 *
 * @author Sébastien Deleuze
 */
class SpringTemplateLoaderTests {

	@Test
	void findTemplateSourceResolvesTemplateInsidePath(@TempDir Path tempDir) throws Exception {
		Path templates = Files.createDirectory(tempDir.resolve("templates"));
		Files.write(templates.resolve("hello.ftl"), "Hello".getBytes(StandardCharsets.UTF_8));
		SpringTemplateLoader loader = new SpringTemplateLoader(new DefaultResourceLoader(),
				"file:" + templates.toAbsolutePath() + File.separator);
		assertThat(loader.findTemplateSource("hello.ftl")).isNotNull();
	}

	@Test
	void findTemplateSourceRejectsBackslash(@TempDir Path tempDir) throws Exception {
		Path templates = Files.createDirectory(tempDir.resolve("templates"));
		Files.write(tempDir.resolve("other.txt"), "other".getBytes(StandardCharsets.UTF_8));
		SpringTemplateLoader loader = new SpringTemplateLoader(new DefaultResourceLoader(),
				"file:" + templates.toAbsolutePath() + File.separator);
		assertThat(loader.findTemplateSource("..\\other.txt")).isNull();
	}

	@Test
	void findTemplateSourceRejectsBackslashBeforeResourceResolution(@TempDir Path tempDir) throws Exception {
		Path templates = Files.createDirectory(tempDir.resolve("templates"));
		Files.write(tempDir.resolve("secret.txt"), "secret".getBytes(StandardCharsets.UTF_8));
		List<String> requestedLocations = new ArrayList<>();
		// A ResourceLoader which treats '\' as a path separator, like the file system does on Windows.
		ResourceLoader resourceLoader = new DefaultResourceLoader() {
			@Override
			public Resource getResource(String location) {
				requestedLocations.add(location);
				return super.getResource(location.replace('\\', '/'));
			}
		};
		SpringTemplateLoader loader = new SpringTemplateLoader(resourceLoader,
				"file:" + templates.toAbsolutePath() + "/");
		assertThat(loader.findTemplateSource("..\\secret.txt")).isNull();
		assertThat(requestedLocations).isEmpty();
	}

	@Test
	void findTemplateSourceRejectsNestedBackslashTraversal(@TempDir Path tempDir) throws Exception {
		Path templates = Files.createDirectory(tempDir.resolve("templates"));
		Path secrets = Files.createDirectory(tempDir.resolve("secrets"));
		Files.write(secrets.resolve("secret.txt"), "secret".getBytes(StandardCharsets.UTF_8));
		ResourceLoader resourceLoader = new DefaultResourceLoader() {
			@Override
			public Resource getResource(String location) {
				return super.getResource(location.replace('\\', '/'));
			}
		};
		SpringTemplateLoader loader = new SpringTemplateLoader(resourceLoader,
				"file:" + templates.toAbsolutePath() + "/");
		assertThat(loader.findTemplateSource("..\\secrets\\secret.txt")).isNull();
		assertThat(loader.findTemplateSource("sub\\..\\..\\secrets\\secret.txt")).isNull();
		assertThat(loader.findTemplateSource("..\\secrets/secret.txt")).isNull();
	}

}
