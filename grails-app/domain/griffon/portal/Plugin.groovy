/*
 * Copyright 2011-2012 the original author or authors.
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

package griffon.portal

import griffon.portal.values.Platform
import griffon.portal.values.Toolkit

/**
 * @author Andres Almiray
 */
class Plugin extends Artifact {
    String toolkits = ''
    String platforms = ''
    Map dependencies

    static constraints = {
        toolkits(nullable: false, blank: true)
        platforms(nullable: false, blank: true)
    }

    void toolkits(List<Toolkit> values) {
        toolkits = values.collect([]) {it.name.toLowerCase()}.join(',')
    }

    void platforms(List<Platform> values) {
        platforms = values.collect([]) {it.name.toLowerCase()}.join(',')
    }

    String toString() {
        super.toString() + [
                toolkits: toolkits,
                platforms: platforms,
                dependencies: dependencies
        ]
    }
}
