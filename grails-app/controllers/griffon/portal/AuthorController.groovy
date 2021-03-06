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

import griffon.portal.values.ProfileTab

/**
 * @author Andres Almiray
 */
class AuthorController {
    static defaultAction = 'show'

    def show() {
        if (!params.id) {
            redirect(uri: '/')
            return
        }

        Author authorInstance = Author.get(params.id)

        if (!authorInstance) {
            flash.message = message(code: 'default.not.found.message', args: [message(code: 'author.label', default: 'Author'), params.id])
            redirect(uri: '/')
            return
        }

        params.tab = params.tab ?: ProfileTab.PLUGINS.name

        List<Plugin> pluginList = []
        if (params.tab == ProfileTab.PLUGINS.name) {
            pluginList = Plugin.withCriteria(sort: 'name', order: 'asc') {
                authors {
                    eq('email', authorInstance.email)
                }
            }
        }

        List<Archetype> archetypeList = []
        if (params.tab == ProfileTab.ARCHETYPES.name) {
            archetypeList = Archetype.withCriteria(sort: 'name', order: 'asc') {
                authors {
                    eq('email', authorInstance.email)
                }
            }
        }

        [
                authorInstance: authorInstance,
                pluginList: pluginList,
                archetypeList: archetypeList,
                tab: params.tab
        ]
    }
}
