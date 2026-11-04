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

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.DeploymentConfiguration;
import org.jboss.arquillian.container.test.api.DeploymentConfiguration.DeploymentContentBuilder;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.impl.client.deployment.AbstractDeploymentScenarioGenerator;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.shrinkwrap.api.Archive;

/**
 * Creates plain deployments from the Maven project for deployments that the test class
 * does not declare with {@link Deployment} methods.
 * One deployment is created for each {@link OperateOnDeployment} name that has no
 * {@link Deployment} method, or a single default one if the test class references no deployments at all.
 * The archive type is determined from the POM's packaging,
 * see {@link ShrinkWrapManipulator#createDeployment()}.
 * Deployments that need customization still need an explicit {@link Deployment} method.
 * <p>
 * Registered by the Arquillian extension, so no explicit configuration is needed.
 */
public class MavenDeploymentScenarioGenerator extends AbstractDeploymentScenarioGenerator {
    @Override
    protected List<DeploymentConfiguration> generateDeploymentContent(TestClass testClass) {
        Set<String> declared = new HashSet<>();
        for (Method method : testClass.getMethods(Deployment.class)) {
            declared.add(method.getAnnotation(Deployment.class).name());
        }
        Set<String> missing = Stream.concat(
                        Arrays.stream(testClass.getJavaClass().getDeclaredMethods()),
                        Arrays.stream(testClass.getJavaClass().getDeclaredFields()))
                .map(element -> element.getAnnotation(OperateOnDeployment.class))
                .filter(Objects::nonNull)
                .map(OperateOnDeployment::value)
                .filter(name -> !declared.contains(name))
                .collect(Collectors.toSet());
        if (missing.isEmpty() && declared.isEmpty()) {
            return List.of(createConfiguration(null));
        }
        return missing.stream().map(MavenDeploymentScenarioGenerator::createConfiguration).toList();
    }

    private static DeploymentConfiguration createConfiguration(String name) {
        var builder = new DeploymentContentBuilder((Archive<?>) ShrinkWrapManipulator.createDeployment())
                .withDeployment();
        // without a name, Arquillian's default deployment name applies
        return (name == null ? builder : builder.withName(name)).build().get();
    }
}
