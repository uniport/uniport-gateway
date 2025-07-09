# Customization

To make new microservices accessible via the Portal-Gateway, a dedicated configuration must be created and integrated. The following sections describe the procedure for this.

## Creating a Custom Configuration

The configuration of the Portal-Gateway is twofold:

- **Static**
- **Dynamic**

The static configuration is provided in the form of a JSON file. It defines the objects for `entrypoints` and `providers`.

The second level depends on the `providers` from the static configuration. The dynamic configuration defines how the Portal-Gateway processes incoming requests.

### Static Configuration

--8<-- "content/04-customization/include-static-configuration.md"

### Dynamic Configuration

--8<-- "content/04-customization/include-dynamic-configuration.md"

### Implementation of Dynamic Configuration

--8<-- "content/04-customization/include-provider-configuration.md"

### JSON Schema

For both static and dynamic configurations, there are JSON schemas that define the structure of the configuration. Modern IDEs (e.g., IntelliJ) can use the JSON schema to assist developers in writing and validating the configuration.

Further information on using JSON schemas can be found in the following [chapter](../05-extending-developing/index.md#json-schemas-for-configuration-files).

## Adding Microservices

It is recommended to store your own configuration files under a different path in the Docker Image than `/etc/portal-gateway/default/`. Then, set the Environment Variable to the used value, e.g., `PORTAL_GATEWAY_JSON=/etc/portal-gateway/example/portal-gateway.json`, in the `portal-gateway.common.env` file under `./portal-gateway/docker-compose/src/main/resources/portal-gateway.common.env` for Docker, and in the `values.dev.yaml` file under `portal-gateway/helm/src/main/resources/values.dev.yaml` for Kubernetes.
