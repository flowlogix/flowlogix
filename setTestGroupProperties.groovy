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

// combine individual test profiles into a single test group property
def groups = System.getProperty 'maven.test.property.profiles'
if (!groups) {
    groups = 'none(),'
    (project.activeProfiles).each{ profile ->
        def test_group = profile.properties.test_groups
        if (test_group) {
            groups += test_group + ","
        }
    }
    project.properties.groups = groups.substring(0, groups.length() - 1)
    System.setProperty 'maven.test.property.profiles', project.properties.groups
} else {
    project.properties.groups = groups
}
