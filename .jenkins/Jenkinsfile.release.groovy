library identifier: 'portal-jenkinsfile-library@0.3.0', retriever: modernSCM([
    $class: 'GitSCMSource', remote: 'git@github.com:uniport/jenkinsfile-library.git',
])

releasePipeline(
    releaseMvnArtifacts: true,
    releaseHelmCharts: true,
    releaseNpmPackages: false,
    releaseContainerImages: true,
    createGitTag: true,
    bumpVersion: true,
    updateChangelog: true,
    createJiraRelease: true,
    jiraReleasePrefix: "Portal-Gateway",
    registerVersionWithArchetype: true,
    registerVersionWithArchetypeComponentName: "portal-gateway"
)
