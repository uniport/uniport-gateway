library identifier: 'portal-jenkinsfile-library@0.3.2', retriever: modernSCM([
    $class: 'GitSCMSource', remote: 'git@github.com:uniport/jenkinsfile-library.git',
])

buildPipeline(
    // jenkins
    nodeLabels: "java17 && docker && !northport",
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
    enableDevDelployment: false,
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
