Changelog
=========

All notable changes to this project will be documented in this file. The changes should be categorized under one of these sections: Added, Changed, Deprecated, Removed, Fixed or Security.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

[Git](https://git.inventage.com/projects/PORTAL/repos/portal-gateway/browse) - [JIRA](https://issue.inventage.com/browse/PORTAL-298?jql=project%20%3D%20PORTAL%20AND%20component%20%3D%20Portal-Gateway) - [Nexus2](https://nexus.inventage.com/content/repositories/inventage-portal/com/inventage/portal/gateway/) - [Nexus3](https://nexus3.inventage.com/#browse/browse:inventage-portal-docker)

2.2.0-[Unreleased]

### Added

- docker-compose.jar is built with and without Maven variable substitution ([PORTAL-360](https://issue.inventage.com/browse/PORTAL-360))
- health route and check

[2.1.0]-202108060813-181-e0baeec - 2021-08-09
------------

### Added

- Support for Refresh tokens in AuthorizationBearer middleware [PORTAL-298](https://issue.inventage.com/browse/PORTAL-298)
- BearerOnly middleware [PORTAL-341](https://issue.inventage.com/browse/PORTAL-341)
- Routing for microservice FileStorage added [PORTAL-352](https://issue.inventage.com/browse/PORTAL-352)
- docker-compose.jar is built with and without Maven variable substitution ([PORTAL-360](https://issue.inventage.com/browse/PORTAL-360))

[2.0.0]-202107261151-161-ad6799e - 2021-07-26
------------

# Added

- Build as Uber/fat jar artifact
- Build as native image
- Option to disable session management

# Changed

- Use proxy for each microservice

[1.3.0]-202107070850-141-913856c - 2021-07-07
------------

### Added

- Kubernetes pods defer their start until all dependencies are up and running.
- Pom module

### Fixed

- Version tiger

### Changed

- Path of default gateway configuration to `/etc/portal-gateway/default`
- Docker-compose artifacts contains only the base `docker-compose.yml` and `*.common.env` files.

### Removed

- Remove suffix `-public` from proxy hostnames

[1.2.0]-202106141557-113-657c581 - 2021-06-14
------------

### Added

- Session Bag ([PORTAL-194](https://issue.inventage.com/browse/PORTAL-194))
- Configuration documentation ([PORTAL-206](https://issue.inventage.com/browse/PORTAL-206))
- Helm charts ([PORTAL-216](https://issue.inventage.com/browse/PORTAL-216))
- Cookie configuration feature ([PORTAL-246](https://issue.inventage.com/browse/PORTAL-246))

### Changed

- Artifact `docker-compose.jar` contains the files from the `src/main/resources` folder with and without any variable substitution ([PORTAL-260](https://issue.inventage.com/browse/PORTAL-260))

[1.1.0]-202104301516-89-6ea3315 2021-04-30
------------

### Added

- Session information page for development/debug purposes [PORTAL-204](https://issue.inventage.com/browse/PORTAL-204)
- Tests

[1.0.0]-202104161309-72-2861749 - 2021-04-16
------------

### Added

- [PORTAL-89](https://issue.inventage.com/browse/PORTAL-89): Portal-Gateway providing reverse proxy functionality within the Inventage Portal Solution.

[Unreleased]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Fheads%2Fmaster&targetBranch=refs%2Ftags%2F2.1.0
[2.1.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Ftags%2F2.1.0&targetBranch=refs%2Ftags%2F2.0.0
[2.0.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Ftags%2F2.0.0&targetBranch=refs%2Ftags%2F1.3.0
[1.3.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Ftags%2F1.3.0&targetBranch=refs%2Ftags%2F1.2.0
[1.2.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Ftags%2F1.2.0&targetBranch=refs%2Ftags%2F1.1.0
[1.1.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/compare/commits?sourceBranch=refs%2Ftags%2F1.1.0&targetBranch=refs%2Ftags%2F1.0.0
[1.0.0]: https://git.inventage.com/projects/PORTAL/repos/portal-gateway/commits?until=1.0.0
