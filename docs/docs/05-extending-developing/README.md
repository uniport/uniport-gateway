# Extending & Developing

Diese Anleitung zeigt, wie ein neuer Microservice mithilfe eines Maven Archetypes entwickelt werden kann. Grundsätzlich kann ein neuer Microservice für Uniport auch mit einem eigenen Technologie-Stack entwickelt werden.

Die Inventage Portal Solution stellt für die Entwicklung eines neuen Microservices den Maven Archetype `portal-hasura-quarkus-postgres` bereit.

## Maven Archetype `portal-hasura-quarkus-postgres`

Damit der Uniport Archetype über den `mvn` Befehl genutzt werden kann, muss in den Maven Einstellungen unter `~/.m2/settings.xml` das `archetype` Repository konfiguriert werden. Die notwendigen Schritte dafür sind in der Anleitung [Getting Started](../02-getting-started/maven-archetype.md) aufgezeigt.

Anschliessend kann das Maven Archetype Plugin aufgerufen werden:

```shell
mvn archetype:generate
```

Es kann einige Zeit dauern bis die verfügbaren Archetypes zur Auswahl angezeigt werden:

```log
[INFO] --- maven-archetype-plugin:3.2.0:generate (default-cli) @ standalone-pom ---
[INFO] Generating project in Interactive mode
[INFO] No archetype defined. Using maven-archetype-quickstart (org.apache.maven.archetypes:maven-archetype-quickstart:1.0)
Choose archetype:
1: remote -> ru.circumflex:circumflex-archetype (-)
2: remote -> org.zkoss:zk-archetype-webapp (The ZK wepapp archetype)
3: remote -> org.zkoss:zk-archetype-component (The ZK Component archetype)

...

2141: remote -> com.inventage.maven2.plugins.archetypes:maven-archetype-jdocbook-book (-)
2142: remote -> com.inventage.maven2.plugins.archetypes:maven-archetype-jdocbook-article (-)
2143: remote -> com.inventage.launchpad:launchpad.archetype (Archetype - for a specific branded launchpad project)
2144: remote -> com.inventage.archetypes:portal-hasura-quarkus-postgres (-)
2145: remote -> com.inventage.archetypes:inventage-portal-solution (-)
Choose a number or apply filter (format: [groupId:]artifactId, case sensitive contains): 1508: 2144
```

Für die Verwendung des Uniport Archetypes für einen neuen Microservice muss die Nummer des `com.inventage.archetypes:portal-hasura-quarkus-postgres` Eintrags (2144 im obigen Fall) eingegeben werden.

Danach kann die zu verwendende Version des Archetypes ausgewählt werden:

```log
Choose com.inventage.archetypes:portal-hasura-quarkus-postgres version:
1: 1.2.0-202103171331-39-ecea73f
2: 1.2.0-202107131438-57-29f2f07
3: 1.3.0-SNAPSHOT
4: 1.3.0-202108261012-8-4491a79
5: 1.3.0-202108261250-9-4491a79
6: 1.3.0-202109021352-96-3b0c509
```

Es wird empfohlen die grösste Version oder allenfalls die zuletzt [releaste Version](https://wiki.inventage.com/display/PORTAL/Releases) zu verwenden.

Als Nächstes werden die Werte der typischen Maven Koordinaten und noch weitere Uniport spezifische für das neue Projekt abgefragt. Als `portPrefix` sollte eine 4-stellige Zahl eingegeben werden. Damit sind die ersten 4 Stellen des 5-stelligen Ports gemeint. D.h. wenn die Portal-Gateway-Komponente der Uniport Instanz den Port 30000 verwendet, so sollte hier für einen ersten eigenen Microservice innerhalb dieser Uniport Instanz 3001 eingegeben werden.

```log
[INFO] Using property: version = 1.0.0-SNAPSHOT
Define value for property 'microserviceName': Service1
Define value for property 'portPrefix': 2099
Define value for property 'groupId': com.inventage.portal.service1
Define value for property 'artifactId': service1
Define value for property 'package' com.inventage.portal.service1: :
Define value for property 'microserviceNameLowercase' service1: :
Define value for property 'microserviceNameUppercase' SERVICE1: :
Define value for property 'portPrefixUniport' 30: :
Confirm properties configuration:
version: 1.0.0-SNAPSHOT
microserviceName: Service1
portPrefix: 2099
groupId: com.inventage.portal.service1
artifactId: service1
package: com.inventage.portal.service1
microserviceNameLowercase: service1
microserviceNameUppercase: SERVICE1
portPrefixUniport: 30
Y: : Y
```

Nach der Bestätigung der Werte mit "Y" erfolgt die Generierung des Projekts:

```log
[INFO] Project created from Archetype in dir: /Users/esteiner/Workspaces/Inventage/InventagePortal/_examples_/portal-hasura-quarkus-postgres/1.3.0/service1
[INFO] ------------------------------------------------------------------------
[INFO] BUILD SUCCESS
[INFO] ------------------------------------------------------------------------
[INFO] Total time:  14:34 min
[INFO] Finished at: 2021-09-02T16:28:07+02:00
[INFO] ------------------------------------------------------------------------
```

## Generiertes Projekt

Das generierte Projekt basiert auf dem Uniport Microservice Blueprint #1. Dieser Blueprint umfasst zur Laufzeit die folgenden Prozesse:

1. Proxy
1. GraphQL Gateway
1. Frontend
1. Backend

Diese Prozesse werden als Docker Container ausgeführt.

Das generierte Projekt liegt als Maven Multi-Module-Projekt vor.

![Maven Multi-Module-Projekt](data/Maven_multi-module_Project.png)

Auf der top-level Stufe des Projekts befinden sich die folgenden Module:

- acceptance-tests: Tests, welche den ganzen Microservice abdecken
- api: Definitionen der Schnittstellen des Microservices
- backend: Implementation der Fachlogik
- docker-compose: Konfiguration für die Ausführung via Docker Compose
- frontend: Micro-Frontend des Microservice
- graphql: GraphQL Gateway und Verarbeitung lesender Zugriffe
- helm: Konfiguration für die Ausführung via Kubernetes
- pom: Parent POM für alle Module
- proxy: Reverse Proxy für das Request Routing innerhalb des Microservices

Für die Implementation von neuen Features werden folgende Schritte empfohlen:

1. backend: Erweiterung des Datenbankschemas via Liquibase Changelog (backend/src/main/resources/db/changeLog.xml)
1. backend: Implementation der Aggregates (backend/src/main/java/../domain/)
1. backend: Implementation der Services (backend/src/main/java/../application)
1. backend: Implementation des GraphQL APIs (backend/src/main/java/.../infrastructure/graphql) für schreibende Operationen
1. backend: Konfiguration der GraphQL API Metadaten für lesende Operationen

### Umsysteme

Der Microservice kommuniziert mit den folgenden Uniport Infrastruktur Komponenten und besitzt diese Ressourcen darin:

- Portal-Database: eigenes Datenbank Objekt und eigenen Datenbank Benutzer
- Portal-IAM: eigenen Client und eigenen Client Scope
- Portal-Messaging: 3 eigene Topics
- Portal-Gateway: eigene Routes Konfigurationen für die Weiterleitung von Requests

#### Portal-Database

Ein Microservice kommuniziert mit der Portal-Database für die Speicherung seines Domänen-Modells. Dafür besitzt jeder Microservice ein eigenes Datenbank-Objekt im zentralen Postgres Server. Die Konvention ist, dass der Name dieses Datenbank-Objekts den Prefix `portal-` und anschliessend den kleingeschriebenen Namen des Microservice hat (z.B. `portal-organisation` für den Namen des Organisation-Datenbank-Objekts). Für den Zugriff auf sein Datenbank-Objekt soll der Microservice einen dedizierten Datenbank Benutzer verwenden, welcher auch der Owner dieser Datenbank ist.

Das folgende Beispiel zeigt die SQL Befehle für das nachträgliche Hinzufügen eines Datenbank-Objekts für den Microservice mit dem Namen `Service1`:

```postgresql
CREATE USER "portal-service1" WITH REPLICATION PASSWORD 'portal-service1';
CREATE DATABASE "portal-service1" OWNER "portal-service1";
```

Als Referenz gilt das `01_create_portal_databases.sh` Skript im Docker Image von Portal-Database unter `/docker-entrypoint-initdb.d`.

Falls in dem Microservice auch Hasura als GraphQL Engine eingesetzt wird, so müssen noch die weiteren SQL Befehle in dem entsprechenden Datenbank-Objekt (z.B. `portal-service1`) ausgeführt werden:

```postgresql
-- switch to database portal-service1
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp"; -- (only necessary if you are using UUID)
CREATE SCHEMA IF NOT EXISTS hdb_catalog;
ALTER SCHEMA hdb_catalog OWNER TO "portal-service1";
GRANT SELECT ON ALL TABLES IN SCHEMA information_schema TO "portal-service1";
GRANT SELECT ON ALL TABLES IN SCHEMA pg_catalog TO "portal-service1";
```

Als Referenz gilt das `02_setup_hasura_graphql.sh` Skript im Docker Image von Portal-Database unter `/docker-entrypoint-initdb.d`

##### Technischer User für External Schema hinzufügen und konfigurieren

Einige Microservices enthalten ein `EXTERNAL` Schema, welches zur Erweiterung auf Datenstufe bei einem individuellen Projekt dient. Damit ein vom Projektteam entwickelter Service nicht die Daten von dem Microservice verändern kann, muss ein dedizierter technischer Benutzer erstellt werden. Die folgenden Postgres SQL Befehle zeigen, wie so ein technischer Benutzer erstellt wird und welche Rechte man ihm geben muss. Als Beispiel wird die `portal-conversation` DB verwendet, dessen "Owner" der `portal-conversation` Benutzer ist.

```postgresql
CREATE USER "portal-conversation-external" WITH REPLICATION PASSWORD 'portal-conversation-external';

-- switch to database portal-conversation

ALTER SCHEMA EXTERNAL OWNER TO "portal-conversation-external";
GRANT USAGE ON SCHEMA WRITE TO "portal-conversation-external";
GRANT REFERENCES ON ALL TABLES IN SCHEMA WRITE TO "portal-conversation-external";
```

#### Portal-Messaging

Ein Microservice kommuniziert mit den Teilen von Portal-Messaging:

![Microservice - Portal-Messaging](data/Microservice_Portal-Messaging.png)

Damit die Laufzeitabhängigkeit von einem Microservice auf Portal-Messaging komplett deaktiviert wird, müssen folgende Application Properties, Environment Variablen in Klammer, gesetzt werden:

- `outbox.connector.enabled=false` (OUTBOX_CONNECTOR_ENABLED)
- `schema.registry.enabled=false` (SCHEMA_REGISTRY_ENABLED)
- `messaging.incoming.enabled=false` (MESSAGING_INCOMING_ENABLED)
- `messaging.outgoing.enabled=false` (MESSAGING_OUTGOING_ENABLED)
- `quarkus.opentelemetry.tracer.exporter.otlp.enabled=false` (QUARKUS_OPENTELEMETRY_TRACER_EXPORTER_OTLP_ENDPOINT)

#### Portal-IAM

Ein Microservice kommuniziert mit dem Portal-IAM für den Bezug des Public Keys zur Verifikation der JWT Signatur.

#### Portal-Gateway

Siehe Anleitung [Customization](../customization/README.md) für die Anpassung der Portal-Gateway-Komponente, damit eingehende HTTP Requests auch an den neuen Microservice weitergeleitet werden.

#### Portal-Monitoring

### Modul: backend

Das `backend` Modul beinhaltet die fachspezifische Logik eines Microservices.

Es wird empfohlen die Implementation am Domain-Driven Design (DDD) auszurichten und auf eine hexagonale Architektur ([Wikipedia](<https://en.wikipedia.org/wiki/Hexagonal_architecture_(software)>), [Erklärung](https://web.archive.org/web/20220627085855/https://www.maibornwolff.de/blog/von-schichten-zu-ringen-hexagonale-architekturen-erklaert)) zu achten.

Die Package Struktur der Java Implementation orientiert sich an der [Onion Architecture](https://www.innoq.com/de/blog/ddd-mit-onion-architecture-umsetzen/) und dem Domain Driven Design (siehe auch `jug.ch` [Talk von Christian Stettler](https://www.jug.ch/events/slides/181122_OnionArchitecturesAndStereotypes.pdf)). Es werden folgende 3 Layer empfohlen:

- Domain Layer
- Application Layer
- Infrastructure Layer

![DDD Layers](./data/DDD_Layers.png)

Die Abhängigkeit zwischen diesen 3 Layern darf immer nur in dieser Richtung erfolgen:

Infrastructure --> Application --> Domain

Dies bedeutet z.B., dass es keine Abhängigkeit geben darf im Domain Layer auf Klassen aus dem Application Layer.

Zudem empfehlen zur Dokumentation des Codes Stereotypes einzusetzen. Die Stereotypes lassen sich einem der 3 obigen Layern zuordnen:

![DDD Layer Stereotypes](./data/DDD_Layers_Stereotypes.png)

In den folgenden Abschnitten werden die einzelnen Stereotypes näher erläutert.

Für die Überprüfung der Package Struktur wird jQAssistant eingesetzt.

#### Domain Layer

Der `domain` Layer stellt den innersten Layer dar. Er sollte möglichst frei von externen Abhängigkeiten zu technischen Implementationen sein, damit ein leichte Testbarkeit der Business-Logik sichergestellt werden kann und technische Abhängigkeiten auch später einfach angepasst werden können. Als Faustregel gilt, dass in den `import` Statements nur Klassen/Interfaces aus dem `domain` Layer selber und Klassen/Interfaces aus den folgenden Package Prefixes verwendet werden dürfen:

- `java.*`
- `javax.*`
- `org.eclipse.microprofile.*`
- `org.jqassistant.contrib.plugin.ddd.annotation`
- `io.debezium.outbox.quarkus.ExportedEvent`

Folgende DDD Klassen und Interface Typen sind im Domain Layer enthalten:

- AggregateRoot (`@DDD.AggregateRoot`)
- Entity (`@DDD.Entity`)
- ValueObject (`@DDD.ValueObject`)
- Factory (`@DDD.Factory`)
- Repository (Interface) (`@DDD.Repository`)
- DomainEvent (`@DDD.DomainEvent`)

Die `@DDD` Annotationen dienen als _executable documentation_. Denn anhand dieser wird die Einhaltung der Layer Strukturierung während des Builds überprüft.

Die DDD Klassen Typen `AggregateRoot` und `Entity` stellen eine `javax.persistence.Entity` dar und sind deshalb mit `@Entity(name = "<DATABASE_TABLe_NAME_LOWER_CASE>")` annotiert.

```java
@DDD.AggregateRoot
@Entity(name = "portaluser")
public class PortalUser extends AbstractEntity<Integer> {
  ...
}
```

Die Felder dieser Klassen werden in der Datenbank für diese Entität automatisch mit den gleichnamigen Spalten verbunden. Das Datenbank-Schema muss vor der ersten Ausführung via Liquibase definiert werden und dadurch während der Ausführung erstellt.

Die AggregateRoot Klasse stellt die Root von einem Aggregate Graphen dar. Innerhalb eines Aggregate-Graphen sind alle Entitäten existenzabhängig von der AggregateRoot Klasse (containment-relation).

Der DDD Klassen Typ `ValueObject` stellt ein `javax.persistence.Embeddable` dar und ist mit `@Embeddable` annotiert.

```java
@DDD.ValueObject
@Embeddable
public class PortalUserAttribute {
  ...
}
```

Die Klassen `AggregateRoot`, `Entity` und `ValueObject` können wie folgt strukturiert werden:

```java
    // --------------------------------------public getters------------------------------------------ //

    // TODO: hier werden die Attribute für das GraphQL Schema definiert

    // ---------------------------------public business interface------------------------------------ //

    // TODO: hier wird das Domain API definiert, welches hauptsächlich aus den Application Services angesprochen wird

    // ----------------------------------internal implementations------------------------------------ //

    // TODO: hier stehen z.B. Konstruktoren (public, protected) und private, protected Methoden

    // ---------------------------------------internal fields---------------------------------------- //

    // TODO: hier stehen die internen Properties, welche auch (teilweise) JPA Annotationen aufweisen
```

Der DDD Klassen Typ `Factory` dient der Instanziierung von `AggregateRoot` Typen. Instanzen von `Entity` Typen sollten immer durch die umgebende AggregateRoot Klasse geschehen. Typischerweise bietet die Klasse dafür eine `create` Methode mit dem JWT des Aufrufers und allen für die Instanziierung notwendigen Parameter. Diese Parameter sollten danach nicht mehr verändert werden können.

Factory Klassen können injected werden. Sie sind mit `@ApplicationScoped` annotiert.

Der DDD Interface Typ `Repository` stellt die Schnittstelle zum Persistenz-Layer (Infrastructure) dar. Die Implementation dieses Interface findet im Infrastructure Layer statt.

##### Persistente Klassen

Es wird empfohlen Quarkus Panache mit dem [Repository Pattern](https://quarkus.io/guides/hibernate-orm-panache#solution-2-using-the-repository-pattern) zu verwenden. Dadurch müssen die Domain Klassen von keiner Subklasse abgeleitet werden.

Für die Modellierung einer persistenten Klasse unterscheidet JPA [Value Typ](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#categorization-value)en und [Entity Typ](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#categorization-entity)en. Bei den Value Typen wird weiter unterschieden zwischen den [primitiven](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#basic), [eingebetteten](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#embeddables) und den [Collection-Typen](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#collections) ([Hibernate User Guide](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#domain-model)).

Eine persistente Klasse besitzt Felder und Beziehungen. Felder haben immer einen [Value Typ](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#categorization-value).

JPA kennt für Beziehungen folgende [Beziehungstypen](https://docs.jboss.org/hibernate/orm/current/userguide/html_single/Hibernate_User_Guide.html#associations) zwischen Entitäten:

- `@ManyToOne`
- `@OneToMany` (unidirectional oder bidirectional)
- `@OneToOne` (unidirectional oder bidirectional)
- `@ManyToMany` (unidirectional oder bidirectional)

Einige dieser Beziehungstypen können unidirectional (einseitig) oder bidirectional (beidseitig) sein. Wenn die Domain Klassen auch als GraphQL API Modell verwendet werden, so sind oft bidirectional Beziehungen gewünscht, damit je nach GraphQL Query von beiden Seiten navigiert werden kann.

Für eine bidirectional Beziehung muss in beiden persistenten Klassen eine JPA Annotation vorgenommen werden. Diese müssen jedoch miteinander verknüpft werden, was durch das `mappedBy` Attribut in der Annotation erfolgt. Denn auch für bidirectional Beziehungen wird in der Datenbank die Beziehung nur für eine Seite als Fremdschlüssel modelliert.

#### Application Layer

Folgende Klassen Typen sind im Application Layer enthalten:

- Service (`@DDD.Service`)

#### Infrastructure Layer

Folgende Klassen Typen sind im Infrastructure Layer enthalten:

- Implementation der Repository Interfaces
- Implementationen für externe Schnittstellen (z.B. GraphQL API, REST API, Kafka API)

Die Infrastructure Layer ist zuständig für die Kommunikation mit externen Systemen. Um die verschiedenen Bounded Contexts sauber voneinander zu trennen, werden die Interfaces bzw. Services aus dem Application Layer hier implementiert. In diesem Zusammenhang sollte man auch eine “isolierende” Schicht einführen, um die externe Schnittstelle vom eigenen Domain-Modell zu entkoppeln: das sogenannte Anti-Corruption Layer (ACL). Es sorgt dafür, dass keine fachlichen oder technischen Konzepte ungewollt vom externen System in die eigene Domäne durchsickern. Das wird erreicht, indem man zwischen den API-Objekten und den eigenen Domain-Objekten übersetzt (bei Bedarf in beide Richtungen).

Das ACL-Pattern kann man grob in drei Teile gliedern:

- Der Adapter ist die konkrete Implementierung des Service-Interfaces aus dem Application Layer.
- Der Translator übernimmt die Übersetzung zwischen externen Datenstrukturen und Domain-Objekten.
- Die Facade ist dann letztlich der Teil, der effektiv mit dem externen System interagiert (z.B. ein REST-Client).

Dadurch bleibt das eigene Domain-Modell klar abgegrenzt und wird nicht direkt von externen Änderungen beeinflusst.

![ACL](./data/DDD_ACL_Layer.png)

##### Repository Implementation

```java
@ApplicationScoped
public class PortalUserRepositoryImpl implements PortalUserRepository, AbstractIntegerIdRepository<PortalUser> {
  ...
}
```

#### Testing

### Modul: graphql

Die Tabellen Metadaten für Hasura (tables.yaml) können aus dem Liquibase ChangeSet abgeleitet werden. Das Uniport Tooling stellt den Hasura Metadata Generator bereit. Er basiert auf dem [JOOQ Maven Plugin], welches im `graphql/pom.xml` des `graphql` Modul verwendet und konfiguriert ist.

#### JWT Modus

#### Hasura Metadaten Generator

Der Hasura Metadata Generator erstellt, ausgehend von der `./backend/src/main/resources/db/changeLog.xml` die Metadaten für die Konfiguration des Hasura Servers. Weil für die Generierung das Root Element einer changeLog.xml Datei im Attribut `objectQuotingStrategy` den Wert `QUOTE_ALL_OBJECTS` aufweisen muss, wird vor der Generierung die originale changeLog.xml Datei kopiert und um den Wert angepasst. Die verwendete changeLog.xml Datei befindet sich dann unter `./graphql/target/db/`.

![Hasura Metadata Generator](data/hasura-metadata-generator.png)

Der Aufruf erfolgt über Maven aus dem Projektverzeichnis.

```shell
mvn compile -pl 'graphql' -Phasura-metadata
```

Als Resultat wird die Datei `./graphql/src/main/docker/hasura-metadata/tables.yaml` erstellt.

Über die Datei `metadata.properties` im Projektverzeichnis kann die Generierung beeinflusst werden. Es werden folgende Möglichkeiten unterstützt:

1. Umbenennung von Datenbank Schemata
1. Ausschluss von Beziehungen
1. Umbenennung von Beziehungen
1. Hinzufügen von Selektion-Berechtigungen

##### Umbenennung von Datenbank Schemata

```properties
<SCHEMA>.name=<NEUER-NAME>
```

##### Ausschluss von Beziehungen

`exclude=<SCHEMA>.<VIEW/TABLE>.<FOREIGNKEY_ON_TABLE>`

```properties
exclude=PUBLIC.MEMBER.PU_FKORGANISATIONUSERID
```

##### Umbenennung von Beziehungen

Der Name einer Beziehung wird als Erstes durch den Namen des Fremdschlüssels im Liquibase ChangeSet definiert.

Im unten dargestellten Screenshot in der Datei `changeLog.xml` lautet der Fremdschlüssel `invitation_Organisation` in der Tabelle`INVITATION`. Er stellt die Beziehung zwischen einer Invitation und ihrer Organisation dar.

![Benennung von Beziehungen](data/changeLog-metadata-tables-db.png)

Durch ein Property in der metadata.properties Datei kann der Name einer Beziehung überschrieben werden. Die Syntax für ein solches Property ist `[<SCHEMA>.]<VIEW/TABLE>.<FOREIGNKEY_ON_TABLE>.name=<NEUER-NAME>`.

So kann z.B. die Beziehung `OrganisationView ---> InvitationView` aus dem obigen Bild wie folgt nach "invitations" umbenannt werden:

```properties
OrganisationView.invitation_Organisation.name=invitations
```

##### Hinzufügen von Selektion-Berechtigungen

In der metadata.properties Datei kann über das Property `rules=<FILE>` eine Datei referenziert werden, welche die Selektion-Berechtigungen enthält.

#### Entwicklungszyklus

Als Vorgehen bei der Erweiterung des lesenden GraphQL APIs werden folgende Schritte empfohlen:

1. backend: via Liquibase Changelog die Definition des Datenbankschemas erweitern und für die Anwendung den Quarkus Prozess starten
2. backend: via Hasura Metadaten Generator die Hasura Dateien erstellen
3. graphql: Hasura starten
4. api-graphql: das GraphQL Schema von Hasura exportieren
5. api-graphql: das exportierte GraphQL Schema anpassen (`type Query` und `type Mutation` einfügen)
6. api-graphql: das GraphQL API als Java Klassen generieren

### Modul: frontend

Wir gehen für diese Anleitung davon aus, dass alle notwendigen Uniport Komponenten gemäss [Getting-Started](../02-getting-started/README.md) aufgesetzt sind und als Docker-Container laufen. Möchte man nun an einem Microservice am Frontend arbeiten, kann die Frontend-Konfiguration in einem Microservice entsprechend angepasst werden. Wir erläutern dies am Beispiel des Organisation-Microservice.

Im `frontend`-Modul eines Uniport Microservice wird jeweils ein Entwicklungsserver verwendet, um während der Entwicklung die Ressourcen (HTML, CSS, JS, etc.) für den Browser bereitzustellen und auszuliefern. In unserem Fall verwenden wir dazu den [Web Dev Server]. Die Konfiguration des [Web Dev Server] ist in der Datei `web-dev-server.config.mjs` erfasst.

Für den Organisation Microservice zeigt der folgende Ausschnitt den Inhalt der `organisation/frontend/src/main/web/web-dev-server.config.mjs` Datei:

```js
import dotenv from "dotenv";
dotenv.config();

import { webDevServerBaseConfig } from "@ips/build-rollup";
import proxy from "koa-proxies";
import { merge } from "lodash-es";

export default merge(webDevServerBaseConfig({ env: process.env }), {
    middleware: [
        proxy("/base", {
            target: process.env.USE_BASE_SERVICE_DEV || false ? process.env.BASE_SERVICE_DEV : process.env.BASE_SERVICE_LOCAL,
            logs: true,
            changeOrigin: true,
        }),
    ],
});
```

Wir gehen dieses File Zeile für Zeile durch, um gewisse Konzepte kurz zu erläutern.

```js
import dotenv from "dotenv";
dotenv.config();
```

Zu oberst wird die [dotenv] Bibliothek geladen und konfiguriert. Diese verwenden wir, um Environment-Variablen auch über Files für den [Web Dev Server]-Node-Prozess bereitzustellen. Die Datei `organisation/frontend/src/main/web/.env.example` zeigt ein Beispiel einer solchen Konfiguration. Eine benutzerspezifische Konfiguration kann erstellt werden, indem eine Datei `.env` neben `.env.example` angelegt wird. Diese Datei ist nicht unter Versionskontrolle. Im `.env.example` sind Variablen aufgeführt, die im `.env`-File mit benutzerspezifischen Werten verwendet werden können.

```js
import { webDevServerBaseConfig } from "@ips/build-rollup";
```

Hier wird die Basis-Konfiguration von [Web Dev Server] aus dem Uniport [portal-frontend-common]-Package importiert.

```js
import proxy from "koa-proxies";
import { merge } from "lodash-es";

export default merge(webDevServerBaseConfig({ env: process.env }), {
    middleware: [
        proxy("/base", {
            target: process.env.USE_BASE_SERVICE_DEV || false ? process.env.BASE_SERVICE_DEV : process.env.BASE_SERVICE_LOCAL,
            changeOrigin: true,
            logs: true,
        }),
    ],
});
```

Mittels der `merge` Funktion aus dem `lodash` Package können wir nun diese Basis-Konfiguration für unser Projekt spezifisch erweitern. [Web Dev Server] kennt das Konzept von ["Middlewares"](https://modern-web.dev/docs/dev-server/middleware/). Über Middlewares kann man sich in Requests, die vom Browser zum Server gelangen, reinhängen und z. B. [Proxies](https://modern-web.dev/docs/dev-server/middleware/#proxying-requests) einrichten.

Startet man nun den Server mit `$ npm start`, wird automatisch ein Browserfenster mit einer `localhost` URL geöffnet (in der Regel Port `8000`). Vermutlich wird jetzt noch nichts angezeigt, weil die Environment-Variablen `USE_BASE_SERVICE_DEV`, `BASE_SERVICE_DEV` und `BASE_SERVICE_LOCAL` im Node-Kontext des [Web Dev Server] nicht gesetzt sind.

Um dies zu beheben, legen wir ein `.env`-File (neben `.env.example`) mit dem folgenden Inhalt an und starten den Server neu:

```dotenv
USE_BASE_SERVICE_DEV=1
BASE_SERVICE_DEV=https://ips.inventage.dev
BASE_SERVICE_LOCAL=http://localhost:20010
```

Nun müsste unter `http://localhost:8080` die Navigations-Komponente der Uniport DEV-Umgebung zu sehen sein. Setzen wir den Wert `USE_BASE_SERVICE_DEV` im `.env`-File auf `0`:

```dotenv
USE_BASE_SERVICE_DEV=0
```

sollte nach einem erneuten Neustart des Servers der Wert von `BASE_SERVICE_LOCAL` angezogen werden. Dieser sollte auf das lokale Portal-Gateway zeigen. Der Port `20010` ist hier nur als Beispiel aufgeführt. Die Server-Logs des Portal-Gateways als auch des Base-Frontend-Service sollten dies wiederspiegeln. Wollen wir dieses "Switching" zwischen der Uniport DEV und der lokalen Umgebung gar nicht verwenden, können wir die Konfiguration des Servers wie folgt vereinfachen:

```js
export default merge(webDevServerBaseConfig({ env: process.env }), {
    middleware: [
        proxy("/base", {
            target: "http://ips.inventage.dev",
            changeOrigin: true,
            logs: true,
        }),
    ],
});
```

und die drei Einträge im `.env`-File löschen.

!!! important "Wichtig"

    Grundsätzlich gilt: **wir möchten lokal jeweils alle Requests vom Browser über den Portal-Gateway laufen lassen**, damit unsere Services hinter dem Gateway die richtige Authentisierung erhalten. Damit die restlichen Requests (z. B. auf die GraphQL API) funktionieren, müssen wir einen weiteren Eintrag in der `middleware`-Konfiguration des Servers und im `.env`-File erfassen:

```js
export default merge(webDevServerBaseConfig({ env: process.env }), {
    middleware: [
        proxy("/base", {
            target: "http://ips.inventage.dev",
            changeOrigin: true,
            logs: true,
        }),
        proxy("/v1/graphql", {
            target: "http://ips.inventage.dev",
            changeOrigin: true,
            logs: true,
            rewrite: (path) => `/organisation${path}`,
        }),
    ],
});
```

und im `.env`-File

```dotenv
FRONTEND_GRAPHQL_API_URL=/v1/graphql
```

Mit dieser Konfiguration haben wir nun sichergestellt, dass der Client alle GraphQL-Requests auf `/v1/graphql` absetzt, und dass der [Web Dev Server] diese Requests an den Portal-Gateway weiterleitet. Im `rewrite` Property wird noch ein Callback definiert für das Umschreiben der Pfade. Aus dem Request auf `/v1/graphql` im Browser wird beim Portal-Gateway `/organisation/v1/graphql`. Dieser Pfad sollte vom Gateway an den Organisation-Proxy weitergeleitet werden.

Wenn wir jetzt den Browser mit der URL des [Web Dev Server] öffnen, werden wir vermutlich noch Fehler beim GraphQL-Request erhalten. Der GraphQL-Service im Organisation Microservice ist geschützt. Damit der Browser für die `localhost` Domain ein entsprechendes Cookie im Portal-Gateway hält (eine Session hat), müssen wir noch einloggen. Dafür rufen wir die URL `http://ips.inventage.dev/organisation` auf und loggen ein. Indem man die Developer Tools des Browsers öffnet, kann man das erstellte Cookie ausfindig machen (vermutlich unter Storage oder ähnlichem). Darin sollte ein Eintrag sein mit Namen `uniport.session` (ab Gateway Version `8.0.0`, davor war es `inventage-portal-gateway.session`) und dem Wert der Session Id. Danach können wir wieder die URL des [Web Dev Server] (z. B. `http://localhost:8000`) aufrufen und dort (wieder über Developer Tools) ein Cookie mit einem Eintrag erstellen (oder falls schon ein solches vorhanden ist, den Wert entsprechend setzen). Man stelle sicher, dass Namen und Wert übereinstimmen. Nach einem Neuladen der Seite, sollte man nun "eingeloggt" sein.

Der GraphQL-Request sollte nun erfolgreich vom Browser, über den [Web Dev Server] zum Portal-Gateway und schlussendlich zum Organisation Microservice durchgehen. Die Navigations-Komponente sollte ebenfalls das Menu des eingeloggten Benutzers darstellen.

Diese Beispiel-Konfiguration, bei der die Requests im [Web Dev Server] zur Uniport DEV-Umgebung "proxied" werden kann man übertragen auf eine lokale Umgebung.

#### Anpassung der Base URL

Mit der bisherigen [Web Dev Server]-Konfiguration wird die SPA des Microservice unter dem Root-Pfad ausgeliefert. Wir können den [Web Dev Server] so konfigurieren, dass die SPA unter einer «Base URL» läuft (hier `/organisation`), damit z.B. in Kombination mit dem Menu aus dem Base-Microservice die aktiven Menüpunkte richtig gesetzt werden (und das Navigieren innerhalb der Menüpunkte des Organisation-Microservice über das Menu funktioniert).

Dafür müssen wir den Wert der `FRONTEND_BASE_HREF` Umgebungsvariable in unserem `.env` wie folgt setzen:

```dotenv
FRONTEND_BASE_HREF=/organisation/
```

Danach können wir den [Web Dev Server] neu starten und sollten unter `http://localhost:8000/organisation/` das Frontend unserer Applikation sehen.

### Modul: proxy

### Modul: api

Für jeden API-Typ (GraphQL, REST, Kafka) wird ein eigenes Submodule verwendet. In diesen Submodulen ist die jeweilige Interface-Beschreibung enthalten.

#### api-graphql

Für das Testen des GraphQL APIs und als Erleichterung für aufrufende Stellen ist es von Vorteil, wenn das vom Backend Modul angeboten GraphQL API auch als Java API zur Verfügung steht. Diesen Zweck übernimmt das `api-graphql` Modul. Ausgehend von einer laufenden Hasura Instanz kann das GraphQL Schema exportiert werden und daraus Java Klassen generiert werden.

##### GraphQL Schema exportieren

Das GraphQL Schema kann mit dem [graphqurl] Werkzeug aus [Hasura] exportiert und unter `src/main/resources/organisation_schema.graphqls` abgelegt werden:

```shell
gq http://localhost:20030/v1/graphql --header 'X-Hasura-Admin-Secret: admin' --introspect > src/main/resources/organisation_schema.graphqls
```

##### GraphQL Java Klassen generieren

Basierend auf dem GraphQL Schema wird mit dem [Netflix Graph-Data-Service (DGS)] ein Java Client API generiert. Allerdings werden die Queries nicht im `query_root` sondern im `Query` Type und die Mutationen nicht im `mutation_root` sondern im `Mutation` Type ausgelesen ([Bug DGS](https://github.com/Netflix/dgs-codegen/issues/375)). Aus diesem Grund müssen die Type Definitionen von `query_root` nach `Query` und von `mutation_root` nach `Mutation` manuell im Schema umbenannt werden.

```text
schema {
  query: Query
  mutation: Mutation
  subscription: subscription_root
}

...

type Query {
    """
    fetch data from the table: "DomainEventView"
    """
    DomainEventView(
        """distinct select on columns"""
        distinct_on: [DomainEventView_select_column!]

...

type Mutation {
  acceptJoinRequest(joinRequestId: JoinRequestId_IntInput!, organisationId: OrganisationId_IntInput!, roleIds: [RoleId_IntInput]): Member_Int
  activateMember(memberId: MemberId_IntInput!): Boolean!
  activateMemberFromUntil(activeFrom: DateTime, activeUntil: DateTime, memberId: MemberId_IntInput!): Boolean!
  activateOrganisation(organisationId: OrganisationId_IntInput!): Boolean!

...
```

Im `pom.xml` des `api-graphql` Moduls müssen unter `graphqlcodegen-maven-plugin` alle Queries und Mutationen, für welche Java Code generiert werden soll aufgeführt werden:

```text
<includeQueries>
    <param>DomainEventView</param>
    <param>InvitationRoleView</param>
    ...
</includeQueries>
<includeMutations>
    <param>acceptJoinRequest</param>
    <param>activateMember</param>
    ...
</includeMutations>
```

Dadurch kann auch die Anzahl der generierten Klassen leicht gesteuert werden.

Die Java Client Code Generierung ist durch das Maven Profil `graphql-generation` deaktiviert. Falls die Java Klassen generiert werden soll, so muss der Build mit `-P graphql-generation` aufgerufen werden:

```shell
mvn clean compile -P graphql-generation
```

Das [DGS GitHub] Repo und [DGS Code Generation Plugin] dienen als weitere Quellen.

#### Kafka

### Modul: docker-compose

### Modul: helm

### Modul: acceptance-tests

Für die Akzeptanztests können direkt die Uniport Infrastruktur Komponenten als Docker Container verwendet werden. Jede der Uniport Infrastruktur Komponenten stellt dafür ein Maven Artefakt mit den notwendigen Dateien für einen Start via docker-compose bereit.

```xml
    <dependency>
      <groupId>com.inventage.portal.messaging</groupId>
      <artifactId>docker-compose</artifactId>
      <version />
      <classifier>substituted</classifier>
    </dependency>
```

Es wird empfohlen im Parent POM unter `pom/pom.xml` sowohl die Versionen dieser Maven Artefakte als Property zu definieren als auch unter `<dependencyManagement>` die Abhängigkeiten zu deklarieren.

Im `acceptance-tests` Modul müssen diese Abhängigkeiten dann ebenfalls deklariert werden (ohne Angabe der Version) und können mithilfe des `maven-dependency-plugin` Maven Plugins aus dem Artefakt Archiv in den `${project.build.directory}/portal-messaging` Ordner extrahiert werden:

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <executions>
        <execution>
            <id>unpack-portal-messaging</id>
            <phase>generate-sources</phase>
            <goals>
                <goal>unpack</goal>
            </goals>
            <configuration>
                <artifactItems>
                    <artifactItem>
                        <groupId>com.inventage.portal.messaging</groupId>
                        <artifactId>docker-compose</artifactId>
                        <version>${portal-messaging-version}</version>
                        <classifier>substituted</classifier>
                        <outputDirectory>${portal-messaging.path}</outputDirectory>
                    </artifactItem>
                </artifactItems>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Damit kann in der AcceptanceTest Klasse dann ein DockerComposeContainer erstellt werden:

```java
protected static DockerComposeContainer portalMessaging = new DockerComposeContainer(
    new File("target/portal-messaging/docker-compose.yml"), new File("target/portal-messaging/docker-compose.override.yml"))
        .withLocalCompose(true)
        .withExposedService("portal-registry", 8081)
        .withExposedService("portal-connect", 8083)
        .withExposedService("portal-kafka", 9094);
```

Und vor den Tests gestartet und danach beendet werden:

```java
@BeforeAll
public static void setUp() throws InterruptedException {
    logOutputOfContainers();
    portalMessaging.start();
    TimeUnit.SECONDS.sleep(SECONDS_TO_WAIT_FOR_INIT);
}

@AfterAll
public static void cleanUp() {
    portalMessaging.stop();
}
```

### Insomnia Workspace

Jeder Microservice soll einen Insomnia Workspace bereitstellen. Dieser hilft bei den ersten Schritten von dessen Verwendung und beim explorativen Testen. Aus diesem Grund wird ein solcher Insomnia Workspace auch für jedes vom Archetype `portal-hasura-quarkus-postgres` erstellten Projekt generiert.

Der Insomnia Workspace beinhaltet mehrere Environments. Folgende Environments sind nützlich:

- Proxy@localhost
- Hasura@localhost
- Quarkus@localhost
- Proxy@ardbeg
- Portal-Gateway@ardbeg

In den obigen Insomnia Environments wird die Variable `navigationRootUrl` als Zieladresse unterschiedlich definiert. Damit können die gleichen Request Definitionen an unterschiedliche Instanzen von Navigation gerichtet werden, indem in der URL diese Variable mittels `{{navigationRootUrl}}` verwendet wird:

![Insomnia Environments](data/Insomnia_Environments.png)

[web dev server]: https://modern-web.dev/docs/dev-server/overview/
[dotenv]: https://github.com/motdotla/dotenv
[portal-frontend-common]: https://github.com/uniport/portal-frontend-common
[hasura]: https://hasura.io/
[graphqurl]: https://github.com/hasura/graphqurl
[netflix graph-data-service (dgs)]: https://netflix.github.io/dgs/
[dgs github]: https://github.com/netflix/dgs-framework/
[dgs code generation plugin]: https://github.com/deweyjose/graphqlcodegen
