/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright 2020-2022 The JReleaser authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jreleaser.gradle.plugin.internal

import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.Directory
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildServiceSpec
import org.gradle.util.GradleVersion
import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.gradle.plugin.tasks.JReleaseAutoConfigReleaseTask
import org.jreleaser.gradle.plugin.tasks.JReleaserAnnounceTask
import org.jreleaser.gradle.plugin.tasks.JReleaserAssembleTask
import org.jreleaser.gradle.plugin.tasks.JReleaserChangelogTask
import org.jreleaser.gradle.plugin.tasks.JReleaserChecksumTask
import org.jreleaser.gradle.plugin.tasks.JReleaserConfigTask
import org.jreleaser.gradle.plugin.tasks.JReleaserDeployTask
import org.jreleaser.gradle.plugin.tasks.JReleaserDownloadTask
import org.jreleaser.gradle.plugin.tasks.JReleaserFullReleaseTask
import org.jreleaser.gradle.plugin.tasks.JReleaserPackageTask
import org.jreleaser.gradle.plugin.tasks.JReleaserPrepareTask
import org.jreleaser.gradle.plugin.tasks.JReleaserPublishTask
import org.jreleaser.gradle.plugin.tasks.JReleaserReleaseTask
import org.jreleaser.gradle.plugin.tasks.JReleaserSignTask
import org.jreleaser.gradle.plugin.tasks.JReleaserTemplateTask
import org.jreleaser.gradle.plugin.tasks.JReleaserUploadTask
import org.jreleaser.model.internal.JReleaserModel
import org.jreleaser.version.SemanticVersion
import org.kordamp.gradle.util.AnsiConsole

import static org.kordamp.gradle.util.StringUtils.isBlank

/**
 *
 * @author Andres Almiray
 * @since 0.1.0
 */
@CompileStatic
class JReleaserProjectConfigurer {
    private static final String JRELEASER_GROUP = 'JReleaser'

    static void configure(Project project) {
        JReleaserExtensionImpl extension = (JReleaserExtensionImpl) project.extensions.findByType(JReleaserExtension)

        boolean hasDistributionPlugin = configureDefaultDistribution(project, extension)

        Provider<Directory> outputDirectory = project.layout.buildDirectory
            .dir('jreleaser')

        Provider<JReleaserLoggerService> loggerProvider = project.gradle.sharedServices
            .registerIfAbsent('jreleaserLogger', JReleaserLoggerService.class) { BuildServiceSpec<JReleaserLoggerService.Params> spec ->
                spec.parameters.console.set(new AnsiConsole(project))
                spec.parameters.logLevel.set(project.gradle.startParameter.logLevel)
                spec.parameters.outputDirectory.set(outputDirectory)
            }

        project.tasks.named('clean', new Action<Task>() {
            @Override
            void execute(Task t) {
                t.doFirst(new Action<Task>() {
                    @Override
                    void execute(Task task) {
                        loggerProvider.get().close()
                    }
                })
            }
        })

        JReleaserModel model = extension.toModel(project, loggerProvider.get().logger)
        configureModel(project, model)

        project.tasks.register('jreleaserConfig', JReleaserConfigTask,
            new Action<JReleaserConfigTask>() {
                @Override
                void execute(JReleaserConfigTask t) {
                    t.group = JRELEASER_GROUP
                    t.description = 'Outputs current JReleaser configuration'
                    t.dryrun.set(extension.dryrun)
                    t.gitRootSearch.set(extension.gitRootSearch)
                    t.strict.set(extension.strict)
                    t.model.set(model)
                    t.jlogger.set(loggerProvider)
                    t.usesService(loggerProvider)
                    t.outputDirectory.set(outputDirectory)
                    if (hasDistributionPlugin) {
                        t.dependsOn('assembleDist')
                    }
                }
            })

        project.tasks.register('jreleaserTemplate', JReleaserTemplateTask,
            new Action<JReleaserTemplateTask>() {
                @Override
                void execute(JReleaserTemplateTask t) {
                    t.group = JRELEASER_GROUP
                    t.description = 'Generates templates for a specific packager/announcer'
                    t.jlogger.set(loggerProvider)
                    t.usesService(loggerProvider)
                    t.outputDirectory.set(project.layout
                        .projectDirectory
                        .dir('src/jreleaser'))
                }
            })

        project.tasks.register('jreleaserDownload', JReleaserDownloadTask,
            new Action<JReleaserDownloadTask>() {
                @Override
                void execute(JReleaserDownloadTask t) {
                    t.group = JRELEASER_GROUP
                    t.description = 'Downloads all artifacts'
                    t.dryrun.set(extension.dryrun)
                    t.gitRootSearch.set(extension.gitRootSearch)
                    t.strict.set(extension.strict)
                    t.model.set(model)
                    t.jlogger.set(loggerProvider)
                    t.usesService(loggerProvider)
                    t.outputDirectory.set(outputDirectory)
                }
            })

        project.tasks.register('jreleaserAssemble', JReleaserAssembleTask,
            new Action<JReleaserAssembleTask>() {
                @Override
                void execute(JReleaserAssembleTask t) {
                    t.group = JRELEASER_GROUP
                    t.description = 'Assemble all distributions'
                    t.dryrun.set(extension.dryrun)
                    t.gitRootSearch.set(extension.gitRootSearch)
                    t.strict.set(extension.strict)
                    t.model.set(model)
                    t.jlogger.set(loggerProvider)
                    t.usesService(loggerProvider)
                    t.outputDirectory.set(outputDirectory)
                }
            })

        project.tasks.register('jreleaserChangelog', JReleaserChangelogTask,
            new Action<JReleaserChangelogTask>() {
                @Override
                void execute(JReleaserChangelogTask t) {
                    t.group = JRELEASER_GROUP
                    t.description = 'Calculate changelogs'
                    t.dryrun.set(extension.dryrun)
                    t.gitRootSearch.set(extension.gitRootSearch)
                    t.strict.set(extension.strict)
                    t.model.set(model)
                    t.jlogger.set(loggerProvider)
                    t.usesService(loggerProvider)
                    t.outputDirectory.set(outputDirectory)
                }
            })

        project.tasks.register('jreleaserChecksum', JReleaserChecksumTask,
            new Action<JReleaserChecksumTask>() {
                @Override
                void execute(JReleaserChecksumTask t) {
                    t.group = JRELEASER_GROUP
                    t.description = 'Calculate checksums'
                    t.dryrun.set(extension.dryrun)
                    t.gitRootSearch.set(extension.gitRootSearch)
                    t.strict.set(extension.strict)
                    t.model.set(model)
                    t.jlogger.set(loggerProvider)
                    t.usesService(loggerProvider)
                    t.outputDirectory.set(outputDirectory)
                    if (hasDistributionPlugin) {
                        t.dependsOn('assembleDist')
                    }
                }
            })

        project.tasks.register('jreleaserSign', JReleaserSignTask,
            new Action<JReleaserSignTask>() {
                @Override
                void execute(JReleaserSignTask t) {
                    t.group = JRELEASER_GROUP
                    t.description = 'Signs a release'
                    t.dryrun.set(extension.dryrun)
                    t.gitRootSearch.set(extension.gitRootSearch)
                    t.strict.set(extension.strict)
                    t.model.set(model)
                    t.jlogger.set(loggerProvider)
                    t.usesService(loggerProvider)
                    t.outputDirectory.set(outputDirectory)
                    if (hasDistributionPlugin) {
                        t.dependsOn('assembleDist')
                    }
                }
            })

        project.tasks.register('jreleaserDeploy', JReleaserDeployTask,
            new Action<JReleaserDeployTask>() {
                @Override
                void execute(JReleaserDeployTask t) {
                    t.group = JRELEASER_GROUP
                    t.description = 'Deploys all artifacts'
                    t.dryrun.set(extension.dryrun)
                    t.gitRootSearch.set(extension.gitRootSearch)
                    t.strict.set(extension.strict)
                    t.model.set(model)
                    t.jlogger.set(loggerProvider)
                    t.usesService(loggerProvider)
                    t.outputDirectory.set(outputDirectory)
                }
            })

        project.tasks.register('jreleaserUpload', JReleaserUploadTask,
            new Action<JReleaserUploadTask>() {
                @Override
                void execute(JReleaserUploadTask t) {
                    t.group = JRELEASER_GROUP
                    t.description = 'Uploads all artifacts'
                    t.dryrun.set(extension.dryrun)
                    t.gitRootSearch.set(extension.gitRootSearch)
                    t.strict.set(extension.strict)
                    t.model.set(model)
                    t.jlogger.set(loggerProvider)
                    t.usesService(loggerProvider)
                    t.outputDirectory.set(outputDirectory)
                    if (hasDistributionPlugin) {
                        t.dependsOn('assembleDist')
                    }
                }
            })

        project.tasks.register('jreleaserRelease', JReleaserReleaseTask,
            new Action<JReleaserReleaseTask>() {
                @Override
                void execute(JReleaserReleaseTask t) {
                    t.group = JRELEASER_GROUP
                    t.description = 'Creates or updates a release'
                    t.dryrun.set(extension.dryrun)
                    t.gitRootSearch.set(extension.gitRootSearch)
                    t.strict.set(extension.strict)
                    t.model.set(model)
                    t.jlogger.set(loggerProvider)
                    t.usesService(loggerProvider)
                    t.outputDirectory.set(outputDirectory)
                    if (hasDistributionPlugin) {
                        t.dependsOn('assembleDist')
                    }
                }
            })

        project.tasks.register('jreleaserAutoConfigRelease', JReleaseAutoConfigReleaseTask,
            new Action<JReleaseAutoConfigReleaseTask>() {
                @Override
                void execute(JReleaseAutoConfigReleaseTask t) {
                    t.group = JRELEASER_GROUP
                    t.description = 'Creates or updates a release with auto-config enabled'
                    t.dryrun.set(extension.dryrun)
                    t.gitRootSearch.set(extension.gitRootSearch)
                    t.strict.set(extension.strict)
                    t.jlogger.set(loggerProvider)
                    t.usesService(loggerProvider)
                    t.outputDirectory.set(outputDirectory)
                }
            })

        project.tasks.register('jreleaserPrepare', JReleaserPrepareTask,
            new Action<JReleaserPrepareTask>() {
                @Override
                void execute(JReleaserPrepareTask t) {
                    t.group = JRELEASER_GROUP
                    t.description = 'Prepares all distributions'
                    t.dryrun.set(extension.dryrun)
                    t.gitRootSearch.set(extension.gitRootSearch)
                    t.strict.set(extension.strict)
                    t.model.set(model)
                    t.jlogger.set(loggerProvider)
                    t.usesService(loggerProvider)
                    t.outputDirectory.set(outputDirectory)
                    if (hasDistributionPlugin) {
                        t.dependsOn('assembleDist')
                    }
                }
            })

        project.tasks.register('jreleaserPackage', JReleaserPackageTask,
            new Action<JReleaserPackageTask>() {
                @Override
                void execute(JReleaserPackageTask t) {
                    t.group = JRELEASER_GROUP
                    t.description = 'Packages all distributions'
                    t.dryrun.set(extension.dryrun)
                    t.gitRootSearch.set(extension.gitRootSearch)
                    t.strict.set(extension.strict)
                    t.model.set(model)
                    t.jlogger.set(loggerProvider)
                    t.usesService(loggerProvider)
                    t.outputDirectory.set(outputDirectory)
                    if (hasDistributionPlugin) {
                        t.dependsOn('assembleDist')
                    }
                }
            })

        project.tasks.register('jreleaserPublish', JReleaserPublishTask,
            new Action<JReleaserPublishTask>() {
                @Override
                void execute(JReleaserPublishTask t) {
                    t.group = JRELEASER_GROUP
                    t.description = 'Publishes all distributions'
                    t.dryrun.set(extension.dryrun)
                    t.gitRootSearch.set(extension.gitRootSearch)
                    t.strict.set(extension.strict)
                    t.model.set(model)
                    t.jlogger.set(loggerProvider)
                    t.usesService(loggerProvider)
                    t.outputDirectory.set(outputDirectory)
                    if (hasDistributionPlugin) {
                        t.dependsOn('assembleDist')
                    }
                }
            })

        project.tasks.register('jreleaserAnnounce', JReleaserAnnounceTask,
            new Action<JReleaserAnnounceTask>() {
                @Override
                void execute(JReleaserAnnounceTask t) {
                    t.group = JRELEASER_GROUP
                    t.description = 'Announces a release'
                    t.dryrun.set(extension.dryrun)
                    t.gitRootSearch.set(extension.gitRootSearch)
                    t.strict.set(extension.strict)
                    t.model.set(model)
                    t.jlogger.set(loggerProvider)
                    t.usesService(loggerProvider)
                    t.outputDirectory.set(outputDirectory)
                    if (hasDistributionPlugin) {
                        t.dependsOn('assembleDist')
                    }
                }
            })

        project.tasks.register('jreleaserFullRelease', JReleaserFullReleaseTask,
            new Action<JReleaserFullReleaseTask>() {
                @Override
                void execute(JReleaserFullReleaseTask t) {
                    t.group = JRELEASER_GROUP
                    t.description = 'Invokes release, publish, and announce'
                    t.dryrun.set(extension.dryrun)
                    t.gitRootSearch.set(extension.gitRootSearch)
                    t.strict.set(extension.strict)
                    t.model.set(model)
                    t.jlogger.set(loggerProvider)
                    t.usesService(loggerProvider)
                    t.outputDirectory.set(outputDirectory)
                    if (hasDistributionPlugin) {
                        t.dependsOn('assembleDist')
                    }
                }
            })
    }

    private static boolean configureDefaultDistribution(Project project, JReleaserExtensionImpl extension) {
        return project.plugins.findPlugin('distribution')
    }

    private static void configureModel(Project project, JReleaserModel model) {
        String javaVersion = ''
        if (project.hasProperty('targetCompatibility')) {
            javaVersion = String.valueOf(project.findProperty('targetCompatibility'))
        }
        if (project.hasProperty('compilerRelease')) {
            javaVersion = String.valueOf(project.findProperty('compilerRelease'))
        }
        if (isBlank(javaVersion)) {
            javaVersion = JavaVersion.current().toString()
        }

        if (isBlank(model.project.java.version)) model.project.java.version = javaVersion
        if (isBlank(model.project.java.artifactId)) model.project.java.artifactId = project.name
        if (isBlank(model.project.java.groupId)) model.project.java.groupId = project.group.toString()
        if (!model.project.java.multiProjectSet) {
            model.project.java.multiProject = project.rootProject.childProjects.size() > 0
        }

        if (isBlank(model.project.java.mainClass)) {
            JavaApplication application = (JavaApplication) project.extensions.findByType(JavaApplication)
            if (application) {
                SemanticVersion gradleVersion = SemanticVersion.of(GradleVersion.current().getVersion())
                if (gradleVersion.major <= 6 && gradleVersion.minor < 4) {
                    model.project.java.mainClass = application.mainClassName
                } else {
                    model.project.java.mainClass = application.mainClass.orNull
                }

                if (gradleVersion.major <= 6 && gradleVersion.minor < 4) {
                    model.project.java.mainModule = ''
                } else {
                    model.project.java.mainModule = application.mainModule.orNull
                }
            }
        }
    }
}
