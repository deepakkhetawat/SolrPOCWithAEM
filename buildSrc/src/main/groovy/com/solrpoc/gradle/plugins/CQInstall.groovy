package com.solrpoc.gradle.plugins

import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project


class CQInstall implements Plugin<Project> {

    @Override
    void apply(Project project) {
        final createCQPackage = project.tasks.findByPath("createCQPackage")
        final installTask = project.task([description: "Installs the CQ5 Package", group: "CQ Plugins", dependsOn: createCQPackage], 'installPackage') {
            it.mustRunAfter createCQPackage
        }
        final String installHost = System.getProperty("host", 'localhost')
        final String installPort = System.getProperty("port", '7502')
        installTask << {
            //get all CQ5 packages in the build/distributions build folder
            final projectPackages = project.fileTree(dir: "build/distributions", include: "**/*.zip")
            projectPackages.each { final pkg ->
                def outputBuffer = new StringBuffer()
                final process = "curl -u admin:admin -X POST -F file=@${pkg.name} http://${installHost}:${installPort}/crx/packmgr/service.jsp -F install=true".execute(null, pkg.parentFile)
                //Explicitly pass the process a Buffer so that it does not block due to a full Output Buffer
                process.consumeProcessOutputStream(outputBuffer)
                process.waitFor()
                println "----- Install Package ----"
                println process.err.text
                println "--------------------------"
            }    
        }

    }

}
