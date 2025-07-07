# Configuration & Deployment

Diese Anleitung zeigt, wie eine Uniport Instanz konfiguriert und gestartet werden kann.

Als Ausgangsbasis wird ein Uniport Projekt verwendet, wie es vom Archetype `inventage-portal-solution` erstellt wird. Für Details zum Erstellen eine Uniport Projekts siehe [Getting Started](../getting-started/README.md).

Die Konfiguration ist in allen Uniport Komponenten gleich aufgebaut und erfolgt über Environment Variablen.

Für jede Uniport Komponente wird auf einer eigenen Seite dieser Anleitung auf deren Konfiguration eingegangen:

- [Portal-Gateway](./portal-gateway.md)
- [Portal-IAM](./portal-iam.md)
- [Portal-Database](./portal-database.md)
- [Portal-Messaging](./portal-messaging.md)
- [Portal-Monitoring](./portal-monitoring.md)
- [Organisation](./organisation.md)

Wir empfehlen zuerst die zwei folgenden Kapitel auf dieser Seite zu lesen, um den generellen Mechanismus für Konfiguration und Deployment zu verstehen.

## Konfiguration

Eine Uniport Komponente (= Kubernetes Pod) kann intern wiederum aus mehreren Subkomponenten (= Container) bestehen (z.B. Organisation besteht aus den Subkomponenten Proxy, Frontend, GraphQL, Backend und OPA). Jede dieser Subkomponenten kann Konfigurationsmöglichkeiten, in Form von Environment Variablen, für ein Projekt anbieten. Diese Environment Variablen sind jeweils mit der Subkomponenten als Prefix benannt (z.B. `BACKEND_LOG_LEVEL`, `FRONTEND_SERVER_PORT`) und tragen ebenfalls gemäss `semantic versioning` zur Version einer Komponente bei. Alle Environment Variablen, welche nicht einen Prefix einer Subkomponente besitzen, werden als Nicht-API betrachtet.

Weiter können innerhalb einer Komponente auch Werte genutzt werden, welche nicht durch die Komponente oder Subkomponente selber, sondern von einer fremden Komponente definiert wurden. Um eine redundante Definition dieser Werte zu verhindern, sollten diese Werte aus der Konfiguration der fremden Komponente importiert werden. So wird z.B. in den meisten Backend Subkomponenten (auf Basis von Quarkus) intern das Property `mp.jwt.verify.publickey.location` verwendet, um den Ort des öffentlichen Schlüssels für die Verifikation der JWTs anzugeben. Dieser Wert wird aber von der Portal-IAM Komponente definiert und sollte deshalb auch von dieser Komponente bezogen werden.

### Rückwärtskompatibilität

### Kubernetes Configuration

In einer Kubernetes Laufzeit Umgebung werden die Uniport Komponenten mit Helm Charts verwaltet. Jede Uniport Komponente wird mit einem `helm` Maven Modul ausgeliefert. Jede projektspezifische Uniport Instanz besitzt ein Modul für ihre instanz-spezifischen Uniport Komponenten (z.B. `portal-database`). In jedem Uniport Komponentenmodul findet sich ebenfalls ein `helm` Submodul. In diesem Modul erfolgen alle Konfigurationen für die verwendeten Kubernetes Laufzeit Umgebungen. Dieses projektspezifische `helm` Submodul besitzt im `src` Ordner die beiden Unterordner `base` und `main`.

Der Inhalt des `base` Ordners entsteht durch das Auspacken des `helm` Maven Moduls der jeweiligen Uniport Komponente. Bei jedem Build wird der Inhalt wieder aus der als Abhängigkeit definierten Uniport Komponente übernommen und neu extrahiert. Aus diesem Grund sind manuelle Anpassungen im `base` Ordner ohne Effekt, weil sie bei jedem Build überschrieben werden. Bei jedem Versionswechsel der Uniport Komponente kann sich der Inhalt dieses Ordners ändern.

![Helm](./data/helm.png)

Für jede Umgebung wird im `main` Ordner eine spezifische Datei verwendet, z.B. values.test.yaml für die `test` Umgebung.

## Deployment

Die Installation der folgende CLI Tools wird vorausgesetzt:

- yq
- jq
- kubectl
- helm
- terraform

### Lokales Kubernetes Deployment

Das lokale Kubernetes Deployment wird anhand einer [minikube] Kubernetes Installation für MacOS aufgezeigt. [minikube] macht es möglich mehrere Kubernetes Cluster zu verwenden. Sie werden Profile genannt und können je nach Bedarf gestartet und gestoppt werden.

In den folgenden Abschnitten werden die 4 Schritte erklärt, die es für das Deployment in einem neuen [minikube] Profil braucht.

Ein durch den Uniport Archetype generiertes Projekt beinhaltet im Projektverzeichnis das `buildNinstall.sh` Skript. Dieses führt alle der unten beschriebenen automatisch Schritte aus.

#### Initialize minikube

Beim erstmaligen [Start eines Profils] (= Kubernetes Cluster) in [minikube] wird dieses angelegt. Dabei werden die konfigurierten Einstellungen für die Grösse des Clusters verwendet. Wir empfehlen mindestens 4 CPUs (`minikube config set cpus 4`) und 16 GB RAM (`minikube config set memory 16g`).

```shell
.bin/kubernetes/minikube-setup.sh start
```

Ein neuer Cluster wird erstellt, gestartet und darin wird ein Namespace mit der `<artifactId>` als Namen angelegt und als aktueller Kontext für `kubectl` gesetzt.

#### Bauen der Container Images

Damit die Container Images der Uniport Instanz im neuen Cluster zur Verfügung stehen muss die Docker Einstellung auf den neuen Cluster zeigen und ein Build der Uniport Instanz gemacht werden. [minikube] bietet dafür den [docker-env] Befehl:

```shell
eval $(minikube -p <artifactId> docker-env)
mvn clean install
```

!!! note

    Mit dem Befehl `minikube profile list` kann die Liste der in Minikube vorhandenen Profile angezeigt werden.

Zur Überprüfung kann vor und nach der obigen Ausführung die Ausgabe von `docker images` verglichen werden.

#### Initialize Uniport

Im Kubernetes Cluster für Uniport werden `Image pull secrets`, `Operators` und `Secrets` benötigt. Diese werden mit dem folgenden Befehl angelegt:

```shell
.bin/kubernetes/uniport.sh initialize
```

#### Deploy Uniport

```shell
.bin/kubernetes/uniport.sh install
```

Bis alle Pods `ready` sind, kann einige Zeit vergehen. Am einfachsten lasst sich dies mit dem `watch` command überprüfen:

```shell
watch kubectl get pod --all-namespaces
```

Anschliessend kann zusätzlich noch [Mailhog] als Mock SMTP Server installiert werden:

```shell
.bin/kubernetes/uniport.sh mailhog
```

Damit auch einzelne Komponenten von Uniport installiert und deinstalliert werden können, bietet das `uniport.sh` Skript auch die Kommandos `install-service <SERVICE>` und `uninstall-service <SERVICE>` an.

##### Setzen des Passworts beim `ips@inventage.com` User

Initial wird ein User `ips@inventage.com` als Plattform-Administrator angelegt. Mit dem folgenden Befehl kann sein initiales Passwort auf das Standardpasswort (zurück)gesetzt werden:

```shell
.bin/kubernetes/uniport.sh set-password-ips
```

#### Direkter Zugriff auf einzelne Komponente

##### Portal-Gateway

Um auf die neue Uniport-Instanz zugreifen zu können wird ein Port-Forward benötigt der den im Archetype abgefragten Port Prefix als fünfstelliger Port auf localhost öffnet und an den Ingress Controller weiterleitet:

```shell
.bin/kubernetes/uniport.sh port-forward
```

Anschliessend kann via `http://<artifactId>.localdev.me:<portPrefix>000/` (z.B. http://portal.localdev.me:30000/) auf die neue Portal-Instanz zugegriffen werden.

##### Portal-IAM

Mit `port-forward-portal-iam` kann direkt auf das Portal-IAM unter `http://<artifactId>.localdev.me:<portPrefix>001` zugegriffen werden:

```shell
.bin/kubernetes/uniport.sh port-forward-portal-iam
```

##### Portal-Messaging / Kafka

Grundsätzlich ist es auch möglich auf Kafka von ausserhalb des Clusters zuzugreifen, benötigt allerdings ein wenig mehr Konfiguration. Grund dafür ist, dass Kafka ein Protokoll implementiert, dass auf TCP basiert. Die Kubernetes Ingress Controller unterstützen allerdings nur HTTP und TLS und daher muss via mutual TLS mit Strimzi kommuniziert werden (Aufgrund der High-Availability von Kafka ist ein simpler Port-Forward nicht möglich). Um mutual TLS sprechen zu können braucht der Client einen Private Key und ein Certificate. Zudem muss dem Server Certificate vertraut werden. Diese drei Secrets werden beim Ausführen des folgenden Commands aus dem Kubernetes Cluster heruntergeladen, unter `/tmp/uniport/secrets` abgelegt und können für die Kommunikation mit Kafka verwendet werden.

Zuerst muss eine weitere Konfiguration an Kafka selbst im `values.localdev.yaml` File vom `portal-messaging` Microservice vorgenommen werden:

```yaml
portal-kafka:
    externalAccess:
        enabled: true
        host: kafka.<artifactId>.localdev.me
        advertisedPort: 9093
```

Wichtig hier ist, dass die Ingresses die von Strimzi eingerichtet werden auch vom NGINX Ingress Controller adressierbar gemacht werden. Das kann überprüft werden, indem die Spalte `ADDRESS` nicht leer sein sollte in:

```bash
kubectl get ingress
```

```log
NAME                                        CLASS   HOSTS                              ADDRESS         PORTS     AGE
portal-messaging-kafka-external-0           nginx   broker-0.kafka.akkp.localdev.me    192.168.205.2   80, 443   60s
portal-messaging-kafka-external-bootstrap   nginx   bootstrap.kafka.akkp.localdev.me   192.168.205.2   80, 443   60s
portal-messaging-kafka-external-1           nginx   broker-1.kafka.akkp.localdev.me    192.168.205.2   80, 443   60s
portal-messaging-kafka-external-2           nginx   broker-2.kafka.akkp.localdev.me    192.168.205.2   80, 443   60s
```

Danach können die Secrets heruntergeladen werden und der Port-Forward aktiviert werden.

```shell
.bin/kubernetes/uniport.sh port-forward-portal-messaging-kafka
```

!!! note "Kafka Host und Port"

    Der in den folgenden Beispielen zu verwenden Kafka Host kann wie folgt heraus gefunden werden:

    ```bash
    kubectl get kafka portal-messaging -o=jsonpath='{.spec.kafka.listeners[?(@.type=="ingress")].configuration.bootstrap.host}'
    ```

    oder

    ```bash
    kubectl get ingress portal-messaging-kafka-external-bootstrap
    ```

    Fall über ein Port-Forward mit Kafka kommuniziert wird, dann muss der Port `9093` verwendet werden. Erfolgt die Kommunikation über den Ingress muss `443` verwendet werden.

Dann kann von einer Quarkus Application (z. B.Organisation-Service), die ausserhalb von Kubernetes läuft, darauf zugegriffen werden, indem der Output des folgenden Commands in das `application.properties` File eingetragen wird:

!!! warning "Kafka User"

    Mit dem nachfolgenden Command wird eine Konfiguration erstellt, die den `superuser` als Benutzer verwendet. Falls zusätzlich ACL Permissions getestet werden sollen, dann sollte der microservice-spezifische User dafür verwendet werden. Dieser kann z.B. für den `organisation` Microservice ebenfalls heruntergeladen und die Quarkus Konfiguration dementsprechend angepasst werden:

    ```bash
    $ KAFKA_USER=organisation-user .bin/kubernetes/rancher-desktop-setup.sh port-forward-portal-messaging-kafka
    ```

```bash
$ bash -c 'echo "
kafka.bootstrap.servers=bootstrap.kafka.<artifactId>.localdev.me:9093
kafka.security.protocol=SSL
kafka.ssl.truststore.type=PKCS12
kafka.ssl.truststore.location=/tmp/uniport/secrets/cluster-cert.p12
kafka.ssl.truststore.password=$(cat /tmp/uniport/secrets/cluster-cert-password.txt)
kafka.ssl.keystore.type=PKCS12
kafka.ssl.keystore.location=/tmp/uniport/secrets/superuser-keystore.p12
kafka.ssl.keystore.password=$(cat /tmp/uniport/secrets/superuser-keystore-password.txt)
"'
```

Fall die Kommunikation aus einem Grund nicht klappt, hilft [Troubleshooting](../troubleshooting/portal-messaging.md) weiter.

##### Portal-Messaging / Connect

Mit `port-forward-portal-messaging-connect` kann direkt auf das Portal-Connect unter `<artifactId>.localdev.me:8083` zugegriffen werden:

```shell
.bin/kubernetes/uniport.sh port-forward-portal-messaging-connect
```

##### Portal-Messaging / Registry

Mit `port-forward-portal-messaging-registry` kann direkt auf das Portal-Registry unter `<artifactId>.localdev.me:8081` zugegriffen werden:

```shell
.bin/kubernetes/uniport.sh port-forward-portal-messaging-registry
```

##### Portal-Database / Postgres

Mit `port-forward-portal-database-postgres` kann direkt auf Postgres unter `<artifactId>.localdev.me:5432` zugegriffen werden:

```shell
.bin/kubernetes/uniport.sh port-forward-database-postgres
```

### Cloud Kubernetes Deployment

Das Cloud Deployment basiert auf dem GitOps Prinzip. Dabei wird die ganze Konfiguration in Git in einem separaten Repository ("Deployment Config Repository") abgelegt.

![ArgoCD Deployment](./data/ArgoCD_Deployment.png)

Für das Cloud Deployment wird ArgoCD eingesetzt. Uniport stellt eine initiale Struktur unter `deployment/argocd` für die Verwaltung der ArgoCD Dateien und ihrer Strukturierung bereit. Als Cloud Anbieter unterstützt Uniport [DigitalOcean] und stellt [Terraform] Recipes für das Bootstrapping des Clusters (LoadBalancer, DNS, Standort, Hardwarespezifikationen) zur Verfügung.

#### Terraform

Das Terraform Recipe wird ein neues `Project` auf dem DigitalOcean provisionieren und verschieden Ressourcen erstellen.

!!! note "Beachte"

    Für die Konfiguration der Forwarding Regeln des Loadbalancers, werden die von den Ingress Services verwendeten Node Ports  auf `32080` für HTTP und `32443` für HTTPS gesetzt.

```sh
cd deployment/argocd/src/generated/terraform
terraform plan -var do_token='YOUR-PROJECT-DIGITAL-OCEAN-API-TOKEN' -var cloudns_password='YOUR-PROJECT-CLOUDNS-API-PASSWORD'
terraform apply -var do_token='YOUR-PROJECT-DIGITAL-OCEAN-API-TOKEN' -var cloudns_password='YOUR-PROJECT-CLOUDNS-API-PASSWORD'
```

#### ArgoCD

!!! important "Zeigt dein `kubectl` auf das richtige Cluster?"

    Bevor die nächsten Schritte ausgeführt werden, stelle sicher, dass dein `kubectl` auf das neu erstellte Cluster zeigt.

Uniport unterstützt ein automatisiertes Deployment mit ArgoCD. Dafür wird eine initiale Struktur unter `deployment/argocd/src/generated` erstellt. ArgoCD ist ein Kubernetes Operator, der dauernd den "Ist" mit dem "Soll"-Zustand vergleicht und gegebenenfalls Anpassung vornimmt. Dadurch ist stets bekannt, welche Konfiguration im Cluster deployt ist.

!!! important "Alle Credentials und Endpoints definiert?"

    Stelle sicher, dass im top-level `pom.xml` definiert sind. Üblicherweise sind diese mit `YOUR-PROJECT` geprefixt und müssen zuerst ersetzt werden.

Eine Uniport Instanz kann mit 3 einfachen Schritten durch ArgoCD installiert werden:

1. Installieren des [Operator Frameworks]:

    ```sh
    ./deployment/argocd/src/generated/bootstrap.sh install-olm
    ```

2. Installieren des [ArgoCD Operators]:

    ```sh
    ./deployment/argocd/src/generated/bootstrap.sh install-argo
    ```

3. Installieren der [App-of-Apps]:

    ```sh
    kubectl apply -f ./deployment/argocd/src/generated/20.argo-apps
    ```

Die ersten beiden Schritte dienen lediglich dazu, den ArgoCD Operator zu deployen. Spannend wird der 3. Schritt. Dabei werden sogenannt [App-of-Apps] installiert. ArgoCD kennt sogenannte Custom Resources mit dem Namen "Application" oder kurz "App". In einem "App" wird deklarative der "Soll"-Zustand beschrieben. In Uniport ist das meistens ein Helm-Chart in einem Git-Repo, das deployt werden soll. ArgoCD kann dann dieses Helm-Chart installieren und stellt sicher, dass auch in Zukunft stets genau das was im Helm-Chart definiert ist, deployt ist. Die [App-of-Apps] ist eine Cluster Bootstrapping Strategie, die in einem "App" andere "Apps" referenziert. Dies führt dazu, dass beim Installieren eines [App-of-Apps] zuerst das "App" installiert wird, welches wiederum "Apps" installiert, die die Helm-Charts installieren. Dies reduziert die von Hand installierten "Apps" und überlässt so viel wie möglich ArgoCD.

Uniport kennt 2 [App-of-Apps]: `cluster-config-root-application.yaml` und `root-application.yaml`. Ersteres installiert alle benötigten Cluster Services. Dazu gehören Ingress-Controller, Image Pull Secrets, Cert Manager, den Strimzi Operator und Weitere. Die Letztere installiert die Uniport Microservices.

Der aktuelle Zustand der "Apps" kann unter `argo.<PROJEKTNAME>.uniport.ch` eingesehen werden.

!!! info "ArgoCD Synchronisation pausieren"

    Da ArgoCD ständig "Ist" mit "Soll"-Zustand vergleicht, können keine Adhoc Konfigurationen ausprobiert werden. Allerdings kann die Synchronisation von einzelnen "Apps" über das UI pausiert werden. Wichtig, dabei ist, dass auch die Synchronisation der [App-of-Apps] pausiert wird, da sonst die [App-of-Apps] die Synchronisation in der "App" wieder fortsetzt.

#### Wie weiter?

!!! info "Dieser Schritt wird empfohlen, ist allerdings nicht erforderlich"

    Dieser Schritt dient dazu, die Fehlerquellen und den kognitiven Load zu minimieren.

ArgoCD braucht initial einige Konfiguration-Koordinaten, wie Git-Repo URL und Access Key sowie Zugriff auf ein Helm Repository. Da diese beim Instanzieren des Archetypes noch nicht bekannt sind, wird initial ein Maven Module unter `./deployment/argocd` erstellt. Es wird allerdings empfohlen, sobald die Konfiguration-Koordinaten bekannt sind, den zusätzlichen Schritt über Maven zu entfernen und unter `./deployment/argocd` die finalen "Apps" zu committen.

Dazu kann der Inhalt des `./deployment/argocd/src/generated` Order nach `./deployment/argocd` verschoben werden und danach kann der `./deployment/argocd/src` Order inklusive `pom.xml` (dazu gehört auch der `<module>argocd</module>` Eintrag im `deployment-parent`) gelöscht werden. Schlussendlich muss auch noch der Pfad in den "Apps" angepasst werden und die [App-of-Apps] deployt werden.

```bash
mv deployment/argocd/src/generated/* deployment/argocd
rm -r deployment/argocd/src deployment/argocd/target deployment/argocd/pom.xml
sed -i '/<module>argocd<\/module>/d' deployment/pom.xml # <modules> ist im Anschluss evtl. leer
rg "deployment/argocd/src/generated" -l | xargs sed 's/deployment\/argocd\/src\/generated/deployment\/argocd/g'
kubectl apply -f deployment/20.argo-apps
```

Falls vom Projekt gewünscht, kann der `deployment/argocd` natürlich auch in einem separaten Git-Repo geführt werden. Dazu müssen dann jeweils Git-Repo URL, Pfad und Access Key angepasst werden.

[minikube]: https://minikube.sigs.k8s.io/docs/
[start eines profils]: https://minikube.sigs.k8s.io/docs/commands/start/
[docker-env]: https://minikube.sigs.k8s.io/docs/commands/docker-env/
[mailhog]: https://github.com/mailhog/MailHog
[digitalocean]: https://www.digitalocean.com/
[terraform]: https://www.terraform.io/
[operator frameworks]: https://olm.operatorframework.io/
[argocd operators]: https://argo-cd.readthedocs.io/en/stable/
[app-of-apps]: https://argo-cd.readthedocs.io/en/stable/operator-manual/cluster-bootstrapping/
