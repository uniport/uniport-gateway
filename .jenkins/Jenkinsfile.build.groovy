library identifier: 'portal-jenkinsfile-library@stable', retriever: modernSCM([
    $class: 'GitSCMSource', remote: 'git@github.com:uniport/jenkinsfile-library.git',
])

buildPipeline(
    // jenkins
    nodeLabels: "java21 && docker && !northport",
    jdkVersion: "OpenJDK-21",
    defaultBranch: "master",
    buildTimeoutMinutes: 60,
    notificationRecipientsConfigMapName: "default-email-recipients",
    // initialize
    initializeDocker: true,
    initializeHelm: true,
    initializeNPM: false,
    // build
    mavenProfiles: "!local,jenkins",
    // analysis
    enableTestEvaluation: true,
    testEvaluationResults: "**/target/surefire-reports/TEST-*.xml",
    enableCodeQualityEvaluation: true,
    enableNoGeneratedCodeCheck: true,
    // deployment
    enableDevDelployment: true,
    devDeploymentServiceName: "gateway",
    // register with archetype
    enableArchetypeRegistration: true,
    archetypeComponentName: "portal-gateway",
    // tag staging artifacts
    tagStagingArtifactsDocker: true,
    tagStagingArtifactsHelm: true,
    tagStagingArtifactsMvn: true,
    tagStagingArtifactsNPM: false,
    // post
    publishFrontendTestCoverageReport: false,
    publishFrontendTestCoverageReportDir: ""
)
