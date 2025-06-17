library identifier: 'portal-jenkinsfile-library@stable', retriever: modernSCM([
    $class: 'GitSCMSource', remote: 'git@github.com:uniport/jenkinsfile-library.git',
])

releasePipeline(
    releaseMvnArtifacts: true,
    releaseHelmCharts: true,
    releaseNpmPackages: false,
    releaseContainerImages: true,
    createGitTag: false,
    bumpVersion: false,
    updateChangelog: false,
    createJiraRelease: false,
    jiraReleasePrefix: "Portal-Gateway",
    registerVersionWithArchetype: false,
    registerVersionWithArchetypeComponentName: "portal-gateway"
)
