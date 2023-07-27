library identifier: 'portal-lib@master', retriever: modernSCM(
        [$class: 'GitSCMSource',
         remote: 'ssh://git@git.inventage.com:2048/portal/jenkinsfile-library.git',
        ])

pipeline {
    agent {
        node {
            label 'docker'
        }
    }

    options {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        ansiColor('xterm')
        skipDefaultCheckout()
    }

    parameters {
        string(name: 'GIT_RELEASE_BRANCH_NAME', description: 'Name of the branch in Git which should be tagged')
        string(name: 'NOTIFICATIONS_RECIPIENT', description: 'Build notifications recipient(s)')
        string(name: 'PACKAGE_VERSION', description: 'Package version from the upstream build')
        string(name: 'PROMOTION_TAG_DOCKER', description: 'Staged docker artefacts version')
        string(name: 'PROMOTION_TAG_HELM', description: 'Name of the Nexus 3 tag containing the helm artifacts to be moved into the release repository')
        string(name: 'PROMOTION_TAG_NPM', description: 'Staged npm artefacts version')
        string(name: 'STAGING_REPOSITORY_ID', description: 'Nexus 2 staging repository ID')
        string(name: 'UPSTREAM_BUILD_URL', description: 'URL of the upstream build')

    }

    environment {
        IPS_CREDENTIALS = credentials('inventage-portal')

        CI_NEXUS2_BITBUCKET_CREDS = credentials('inventage-portal')
        CI_REGISTRY_CREDS = credentials('portal-registry-creds')

        DOCKER_CONFIG = "${env.WORKSPACE}/.docker"
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    Map checkoutCfg = checkout([
                            $class           : 'GitSCM',
                            branches         : [[name: params.GIT_RELEASE_BRANCH_NAME]],
                            extensions       : scm.extensions + [
                                    [$class: 'CleanBeforeCheckout'],
                                    [$class: 'LocalBranch']
                            ],
                            userRemoteConfigs: scm.userRemoteConfigs
                    ])
                    env.GIT_URL = checkoutCfg.GIT_URL
                    echo "--> Building from remote: ${env.GIT_URL}"
                }

                setNotificationsRecipient()

                // Display important variables
                sh("env | sort")

                script {
                    if (!params.PROMOTION_TAG_DOCKER) {
                        error('Required parameter are missing, cannot proceed.')
                    }

                    if (params.PACKAGE_VERSION) {
                        currentBuild.description = "Version: ${params.PACKAGE_VERSION}"
                    }

                    if (params.UPSTREAM_BUILD_URL) {
                        currentBuild.description = currentBuild.description + "<br /><a href=\"${params.UPSTREAM_BUILD_URL}\">Upstream Build</a>"
                    }
                }
            }
        }
        stage('Nexus 2') {
            when {
                expression { params.STAGING_REPOSITORY_ID }
            }
            steps {
                mvn("org.sonatype.plugins:nexus-staging-maven-plugin:1.6.8:rc-release -DserverId=inventage-portal-group -DnexusUrl=https://nexus.inventage.com -DstagingRepositoryId=${params.STAGING_REPOSITORY_ID}")
            }
        }

        stage('Nexus 3 (helm)') {
            when {
                expression { params.PROMOTION_TAG_HELM }
            }
            steps {
                moveComponents(
                        'nexusInstanceId': 'nexus3.inventage.com',
                        'destination': 'inventage-portal-helm',
                        'tagName': params.PROMOTION_TAG_HELM
                )
            }
        }

        stage('Nexus 3 (npm)') {
            when {
                expression { params.PROMOTION_TAG_NPM }
            }
            steps {
                moveComponents(
                        'nexusInstanceId': 'nexus3.inventage.com',
                        'destination': 'inventage-portal-npm',
                        'tagName': params.PROMOTION_TAG_NPM
                )
            }
        }

        stage('Nexus 3 (docker)') {
            when {
                expression { params.PROMOTION_TAG_DOCKER }
            }
            steps {
                promoteContainerImages(
                        tagName: params.PROMOTION_TAG_DOCKER,
                        credentialsId: 'inventage-portal'
                )
            }
        }

        stage('Tag commit') {
            steps {
                script {
                    String[] versionParts = params.PACKAGE_VERSION.split("-")
                    String COMMIT_ID = versionParts[3]
                    String TAG_MSG = versionParts[0]

                    createAndPushTag(
                            commitId: COMMIT_ID,
                            tag: TAG_MSG,
                            tagMsg: params.PACKAGE_VERSION
                    )
                }
            }
        }

        stage('Bump version') {
            steps {
                setVersion(
                        currentVersion: params.PACKAGE_VERSION,
                        branchName: params.GIT_RELEASE_BRANCH_NAME
                )
            }
        }

        stage('Update Changelog') {
            steps {
                updateChangelog(
                        packageVersion: params.PACKAGE_VERSION,
                        branchName: params.GIT_RELEASE_BRANCH_NAME
                )
            }
        }

        stage('Jira Release') {
            steps {
                script {
                    token = sh(
                            script: "echo -n ${IPS_CREDENTIALS_USR}:${IPS_CREDENTIALS_PSW} | base64 - ",
                            returnStdout: true
                    )
                    jiraRelease(
                            componentName: "Portal-Gateway",
                            releaseVersion: params.PACKAGE_VERSION,
                            authToken: "$token"
                    )
                }
            }
        }
    }

    post {
        always {
            sendNotifications()
        }
    }
}

def mvn(cmd) {
    sh("mvn -s '${env.WORKSPACE}/.jenkins/settings.xml' -B ${cmd}")
}

/**
 * Sets the notifications recipient environment variable, which is either the parameter passed to this pipeline OR the default recipient.
 *
 * @return
 */
def setNotificationsRecipient() {
    env.NOTIFICATIONS_RECIPIENT = "${params.NOTIFICATIONS_RECIPIENT}"
    if (!env.NOTIFICATIONS_RECIPIENT) {
        configFileProvider([configFile(fileId: 'default-email-recipients', variable: 'DEFAULT_EMAIL_RECIPIENTS_FILE')]) {
            env.NOTIFICATIONS_RECIPIENT = sh(script: "cat '${env.DEFAULT_EMAIL_RECIPIENTS_FILE}'", returnStdout: true, encoding: 'UTF-8').trim()
        }
    }
}

/**
 * Sends notifications about the build status.
 *
 * @return
 */
def sendNotifications() {
    step([
            $class                  : 'Mailer',
            notifyEveryUnstableBuild: true,
            recipients              : env.NOTIFICATIONS_RECIPIENT
    ])
}
