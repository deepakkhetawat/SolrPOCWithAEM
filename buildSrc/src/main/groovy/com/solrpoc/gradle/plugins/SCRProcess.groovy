package com.solrpoc.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project


class SCRProcess implements Plugin<Project> {

    @Override
    void apply(Project project) {
        project.ant.properties.src = "build/classes/main"
        project.ant.properties.classes = "build/classes/main"

        project.task([description: "Processes SCR annoations from source", group: "CQ Plugins", dependsOn: "compileGroovy"], 'processSCRAnnotations') << {
            if (!(new File("build/classes/main").isDirectory())) {
                println "No Java/Groovy classes found"
                return
            }
            ant.taskdef(resource: "scrtask.properties", classpath: project.configurations.compile.asPath)
            ant.scr(srcdir: project.ant.properties.src, destdir: project.ant.properties.classes, classpath: project.configurations.compile.asPath, scanClasses: true)
        }

        final packageSCRTask = project.task([description: "Injects SCR metafiles into package's OSGI-INF", group: "CQ Plugins", dependsOn: "processSCRAnnotations"], 'packageSCRAnnotations') << {
            def tree = project.fileTree(dir: ant.properties.classes + "/OSGI-INF", include: "**/*.xml", exclude: "**/metatype/**")
            def serviceComponents = ""
            if (tree.isEmpty()) {
                println "No SCR Annotations found"
                project.extensions.add("serviceComponents", serviceComponents)
                return
            }

            tree.each { File file ->
                def index = file.path.indexOf("OSGI-INF")
                serviceComponents += file.path.substring(index).replace("\\", "/") + ", "
            }
            serviceComponents = serviceComponents.substring(0, serviceComponents.length() - 2)//remove final ", "
            //Register to an extension
            project.extensions.add("serviceComponents", serviceComponents)
        }

        final jarTask = project.tasks.findByPath("jar").dependsOn packageSCRTask
    }
}
