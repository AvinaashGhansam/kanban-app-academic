# kanban-app-academic

> **For academic use only.** This repository is a copy of an existing open-source project, used for coursework
> (for example, practice with software testing and maintenance). It is not a product, it is not maintained for
> production, and it is not affiliated with or endorsed by the original authors.

## Credit

All credit for the original application goes to its creators:

- **Original project:** [kanban/kanban-app](https://github.com/kanban/kanban-app)
- **Original copyright:** Copyright (c) 2011 MetService (Meteorological Service of New Zealand Limited)
- **License:** MIT. See [`src/main/webapp/META-INF/LICENSE`](src/main/webapp/META-INF/LICENSE) and
  [`src/main/webapp/META-INF/licenses/mit-license-kanban.txt`](src/main/webapp/META-INF/licenses/mit-license-kanban.txt)
  for the original license and the third-party license notices.

The original copyright and license notices are kept as the MIT License requires.

## Changes from the original

- Renamed the project to `kanban-app-academic` and updated the Maven coordinates
  (`io.github.avinaashghansam:kanban-app-academic`).
- Removed the original unit and web tests (`src/test`). We will write our own test suite.
- Updated the build so it compiles on a modern JDK: Java 8 bytecode level, `maven-compiler-plugin` 3.13.0, and
  `maven-war-plugin` 3.4.0.
- Removed the original organization's private Nexus repositories and an unused plain-HTTP Maven repository.

The Java packages (`com.metservice.kanban.*`) are unchanged.

## Building

Requirements: JDK 8 or newer and Maven 3.6 or newer.

```sh
mvn clean package
```

This produces `target/kanban-app-academic-<version>.war`.

## Running

Deploy the WAR to a servlet container such as Tomcat or Jetty, then open `http://localhost:8080/kanban`
(the context path depends on how you deploy it). The first page offers to create a Kanban home directory with a
sample project. See [`kanban-app-set-up.html`](kanban-app-set-up.html) for the original setup and configuration guide.

## License

This repository is distributed under the same MIT License as the original project. Copyright (c) 2011 MetService.
