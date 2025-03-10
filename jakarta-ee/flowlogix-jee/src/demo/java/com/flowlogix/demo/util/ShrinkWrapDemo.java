/*
 * Copyright (C) 2011-2025 Flow Logix, Inc. All Rights Reserved.
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
package com.flowlogix.demo.util;

import com.flowlogix.util.ShrinkWrapManipulator;
import com.flowlogix.util.ShrinkWrapManipulator.Action;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import static com.flowlogix.util.JakartaTransformerUtils.jakartify;
import static com.flowlogix.util.ShrinkWrapManipulator.getContextParamValue;

@SuppressWarnings({"HideUtilityClassConstructor", "unused"})
@Slf4j
public class ShrinkWrapDemo {
    // @Deployment
    // @start region="deployMaven"
    // tag::deployMaven[] // @replace regex='.*\n' replacement=""
    static WebArchive deployMaven() {
        return ShrinkWrapManipulator.logArchiveContents(
                ShrinkWrapManipulator.createDeployment(WebArchive.class),
                contents -> log.info("Archive Contents: {}", contents));
    }
    // end::deployMaven[] // @replace regex='.*\n' replacement=""
    // @end

    // @Deployment
    // @start region="deployMavenSuffix"
    // tag::deployMavenSuffix[] // @replace regex='.*\n' replacement=""
    static WebArchive deployMavenSuffix() {
        return ShrinkWrapManipulator.logArchiveContents(
                ShrinkWrapManipulator.createDeployment(WebArchive.class,
                        name -> name + "-prod"),
                contents -> log.info("Archive Contents: {}", contents));
    }
    // end::deployMavenSuffix[] // @replace regex='.*\n' replacement=""
    // @end

    // @Deployment
    // @start region="productionMode"
    // tag::productionMode[] // @replace regex='.*\n' replacement=""
    static WebArchive deployProductionMode() {
        var archive = ShrinkWrapManipulator.createDeployment(WebArchive.class);
        // add classes to the archive here
        var productionList = List.of(new Action(getContextParamValue(
                jakartify("javax.faces.PROJECT_STAGE")),
                node -> node.setTextContent("Production")));
        new ShrinkWrapManipulator().webXmlXPath(archive, productionList);
        return archive;
    }
    // end::productionMode[] // @replace regex='.*\n' replacement=""
    // @end

    // @Deployment
    // @start region="persistence"
    // tag::persistence[] // @replace regex='.*\n' replacement=""
    public static WebArchive deployPersistence() {
        var archive = ShrinkWrapManipulator.createDeployment(WebArchive.class);
        // add classes to the archive here
        String version = System.getProperty("project.version");
        new ShrinkWrapManipulator().persistenceXmlXPath(archive,
                List.of(new Action("//persistence/persistence-unit/jar-file",
                        node -> node.setTextContent(String.format("lib/entities-%s.jar", version)))));
        return archive;
    }
    // end::persistence[] // @replace regex='.*\n' replacement=""
    // @end
}
