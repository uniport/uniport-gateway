# Customization

Diese Anleitung zeigt, wie die Docker Images einzelner Uniport Komponenten gemäss eigener Anforderungen angepasst werden können.

Bei der Verwendung einer Uniport Komponente in einem Projekt, werden dessen Docker Images jeweils spezifisch für das Projekt erstellt. Im einfachsten Fall geschieht dies durch eine einzige `FROM` Zeile im Dockerfile:

```Dockerfile
FROM ${docker.pull.registry}/com.inventage.portal.database.postgres:${portal-database.version}
```

Damit liegt ein projektspezifisches Image einer bestehenden Uniport Komponente vor, welches jedoch noch identisch mit dem Inhalt der Uniport Komponente ist.

Allfällige Anpassungen erfolgen durch Veränderungen beim Bau des Docker Images.

![Docker Image Custom](./data/IPS_Custom_Project_Customization.png)

Als Ausgangsbasis wird die Verwendung des `inventage-portal-solution` Archetypes, wie in [Getting Started](../getting-started/README.md) beschrieben, empfohlen.
