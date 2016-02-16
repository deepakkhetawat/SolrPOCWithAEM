package com.solrpoc.gradle.plugins

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Zip

class CQPackage implements Plugin<Project> {

    @Override
    void apply(Project project) {
        //Create the notProvided configuration
        project.configurations.create 'notProvided'
        //Create the task
        final packageTask = project.task([type: Zip, description: "Packages the CQ5 package", group: "CQ Plugins"], 'createCQPackage') {
            from "src/main/content"
            from(project.configurations.notProvided, {
                into "jcr_root/apps/solrpoc/install"
            })
            from(project.tasks.jar, {
                into "jcr_root/apps/solrpoc/install"
            })

        }

        //Set the dependencies for the plugin
        project.tasks.build.dependsOn += "cleanCreateCQPackage"
        project.tasks.build.dependsOn += packageTask

        packageTask.dependsOn project.tasks.jar
        packageTask.mustRunAfter project.tasks.jar
        
        project.tasks.clean.dependsOn += "cleanCreateCQPackage"
    }

}
