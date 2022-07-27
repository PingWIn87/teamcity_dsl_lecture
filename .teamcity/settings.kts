import jetbrains.buildServer.configs.kotlin.*
import jetbrains.buildServer.configs.kotlin.buildSteps.maven
import jetbrains.buildServer.configs.kotlin.buildSteps.script
import jetbrains.buildServer.configs.kotlin.triggers.finishBuildTrigger
import jetbrains.buildServer.configs.kotlin.vcs.GitVcsRoot

/*
The settings script is an entry point for defining a TeamCity
project hierarchy. The script should contain a single call to the
project() function with a Project instance or an init function as
an argument.

VcsRoots, BuildTypes, Templates, and subprojects can be
registered inside the project using the vcsRoot(), buildType(),
template(), and subProject() methods respectively.

To debug settings scripts in command-line, run the

    mvnDebug org.jetbrains.teamcity:teamcity-configs-maven-plugin:generate

command and attach your debugger to the port 8000.

To debug in IntelliJ Idea, open the 'Maven Projects' tool window (View
-> Tool Windows -> Maven Projects), find the generate task node
(Plugins -> teamcity-configs -> teamcity-configs:generate), the
'Debug' option is available in the context menu for the task.
*/

version = "2022.04"

project {
    val backend = GitVcsRoot{
        name = "backend"
        id = DslContext.createId("backend vcs dsl".replace("[\\s-]+".toRegex(), ""))
        url = "git@github.com:PingWIn87/backend-todo-list-maven.git"
        branch = "maven"
        authMethod = uploadedKey {
            uploadedKey = "github"
        }
    }
    vcsRoot(backend)
    val buildBackend = BuildType{
        name = "build backend"
        id = DslContext.createId("build backend dsl".replace("[\\s-]+".toRegex(), ""))
        artifactRules = "target/*.jar => build_result"
        vcs {
            root(backend)
        }
        steps {
            maven {
                name = "build"
                goals = "clean install"
                runnerArgs = "-DskipTests=true"
            }
        }
    }
    buildType(buildBackend)
    val renameArtifact = BuildType{
        name = "rename artifact"
        id = DslContext.createId("rename backend dsl".replace("[\\s-]+".toRegex(), ""))
        artifactRules = "result/*.jar"
        steps {
            script {
                scriptContent = """
                #!/bin/bash
                mkdir result
                mv build_result/backend-todo-list-0.0.1-SNAPSHOT.jar result/todo-%build.counter%.jar
            """.trimIndent()
            }
        }
        triggers {
            finishBuildTrigger {
                buildType = "${buildBackend.id}"
                successfulOnly = true
            }
        }
        dependencies {
            dependency(buildBackend) {
                snapshot {
                }

                artifacts {
                    artifactRules = "build_result => build_result"
                }
            }
        }
    }
    buildType(renameArtifact)
    buildTypesOrder = listOf(buildBackend, renameArtifact)

}




