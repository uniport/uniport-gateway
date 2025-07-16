library identifier: 'portal-jenkinsfile-library@stable', retriever: modernSCM([
    $class: 'GitSCMSource', remote: 'git@github.com:uniport/jenkinsfile-library.git',
])

releasePipeline(
    releaseMvnArtifacts: true,
    releaseHelmCharts: false,
    releaseNpmPackages: false,
    releaseContainerImages: true,
    createGitTag: true,
    bumpVersion: true,
    updateChangelog: true,
    createJiraRelease: true,
    jiraReleasePrefix: "Uniport-Gateway",
    registerVersionWithArchetype: true,
    registerVersionWithArchetypeComponentName: "uniport-gateway"
)
