/*
 * Copyright (C) 2011-2026 Flow Logix, Inc. All Rights Reserved.
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
package com.flowlogix.util;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.arquillian.config.descriptor.api.ArquillianDescriptor;
import org.jboss.arquillian.core.api.Instance;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.container.spi.client.deployment.DeploymentDescription;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.spi.client.deployment.DeploymentScenarioGenerator;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;
import static com.flowlogix.util.ShrinkWrapManipulator.addTestClasses;
import static com.flowlogix.util.ShrinkWrapManipulator.createDeployment;
import static com.flowlogix.util.ShrinkWrapManipulator.packageSlf4j;
import static java.util.function.Predicate.not;

/**
 * Creates plain deployments from the Maven project for deployments that the test class
 * does not declare with {@link Deployment} methods.
 * One deployment is created for each {@link OperateOnDeployment} name that has no
 * {@link Deployment} method, or a single default one if the test class references no deployments at all.
 * The archive type is determined from the POM's packaging unless overridden by
 * the {@code flowlogix/archiveType} descriptor property ({@code war}, {@code jar},
 * {@code ejb}, {@code ear}, or {@code rar}),
 * see {@link ShrinkWrapManipulator#createDeployment()}.
 * Compiled test classes are included so shared suite deployments can run tests in-container,
 * excluding client-only tests as described in {@link ShrinkWrapManipulator#addTestClasses(Archive)}.
 * Test resources and additional test dependencies are not imported.
 * Deployments that need customization still need an explicit {@link Deployment} method.
 * <p>
 * Registered by the Arquillian extension, so no explicit configuration is needed.
 */
public class MavenDeploymentScenarioGenerator implements DeploymentScenarioGenerator {
    @Inject
    Instance<ArquillianDescriptor> descriptor;

    @Override
    public List<DeploymentDescription> generate(TestClass testClass) {
        Set<String> declared = Arrays.stream(testClass.getMethods(Deployment.class))
                .map(method -> method.getAnnotation(Deployment.class).name())
                .collect(Collectors.toSet());
        Set<String> missing = Stream.concat(
                        Arrays.stream(testClass.getJavaClass().getDeclaredMethods()),
                        Arrays.stream(testClass.getJavaClass().getDeclaredFields()))
                .flatMap(element -> Stream.ofNullable(element.getAnnotation(OperateOnDeployment.class)))
                .map(OperateOnDeployment::value).filter(not(declared::contains))
                .collect(Collectors.toCollection(HashSet::new));
        if (missing.isEmpty() && declared.isEmpty()) {
            missing.add("_DEFAULT_");
        }
        if (missing.isEmpty()) {
            return List.of();
        }
        var configuration = descriptor.get().getExtensions().stream()
                .filter(extension -> "flowlogix".equals(extension.getExtensionName()))
                .findFirst();
        var packageSlf4j = configuration.map(extension -> extension.getExtensionProperty("packageSlf4j"))
                .map(Boolean::parseBoolean).orElse(false);
        var archiveType = configuration.map(extension -> extension.getExtensionProperty("archiveType")).orElse(null);
        return missing.stream()
                .map(name -> new DeploymentDescription(name, addTestClasses(createArchive(archiveType, packageSlf4j))))
                .toList();
    }

    private static <TT extends Archive<TT>> Archive<?> createArchive(String archiveType, boolean packageSlf4j) {
        @SuppressWarnings("unchecked")
        TT archive = archiveType == null ? createDeployment()
                : createDeployment((Class<TT>) ShrinkWrapManipulator.archiveType(archiveType));
        return packageSlf4j ? packageSlf4j(archive) : archive;
    }
}
