/*
 * Copyright 2012-2016 the original author or authors.
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
package org.grails.plugins.tomcat

import grails.util.BuildSettings
import grails.web.container.EmbeddableServer
import grails.web.container.EmbeddableServerFactory
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode

import org.codehaus.groovy.grails.cli.support.BuildSettingsAware
import org.grails.plugins.tomcat.fork.ForkedTomcatServer
import org.grails.plugins.tomcat.fork.TomcatExecutionContext

@CompileStatic
class TomcatServerFactory implements EmbeddableServerFactory, BuildSettingsAware {

	BuildSettings buildSettings

	EmbeddableServer createInline(String basedir, String webXml, String contextPath, ClassLoader classLoader) {
		final obj = buildSettings?.forkSettings?.get("run")
		if (obj) {
			return createForked(contextPath, obj)
		}

		new InlineExplodedTomcatServer(basedir, webXml, contextPath, classLoader)
	}

	protected ForkedTomcatServer createForked(String contextPath, forkConfig, boolean warMode = false) {
		final settings = buildSettings
		TomcatExecutionContext ec = new TomcatExecutionContext()
		final forkedTomcat = new ForkedTomcatServer(ec)
		ec.process = forkedTomcat

		ec.initialize settings
		ec.contextPath = contextPath
		ec.resourcesDir = settings.resourcesDir
		if (warMode) {
			ec.warPath = settings.projectWarFile.canonicalPath
		}

		if (forkConfig instanceof Map) {
			forkedTomcat.configure((Map)forkConfig)
		}

		def tomcatJvmArgs = getTomcatJvmArgs()
		if (tomcatJvmArgs instanceof List) {
			forkedTomcat.jvmArgs = (List<String>)tomcatJvmArgs
		}

		forkedTomcat
	}

	@CompileStatic(TypeCheckingMode.SKIP)
	protected getTomcatJvmArgs() {
		buildSettings.config?.grails?.tomcat?.jvmArgs
	}

	EmbeddableServer createForWAR(String warPath, String contextPath) {
		buildSettings.projectWarFile = new File(warPath)
		final forkConfig = buildSettings?.forkSettings?.get("war") ?: buildSettings?.forkSettings?.get("run") ?: [:]
		createForked contextPath, forkConfig, true
	}
}
