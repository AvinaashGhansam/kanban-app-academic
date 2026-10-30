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
- Replaced the old test libraries (JUnit 4, Mockito 1, JWebUnit, Selenium HtmlUnit, spring-mock) with JUnit 6,
  Mockito 5, AssertJ 3 and Hamcrest 3.

The Java packages (`com.metservice.kanban.*`) are unchanged.

## Testing

Tests go in `src/test/java` and run with `mvn test`. The stack is:

- [JUnit 6](https://docs.junit.org/) (Jupiter API: `org.junit.jupiter.api.*`)
- [Mockito 5](https://site.mockito.org/) with `mockito-junit-jupiter` (`@ExtendWith(MockitoExtension.class)`)
- [AssertJ 3](https://assertj.github.io/doc/) (`org.assertj.core.api.Assertions.assertThat`), the preferred
  assertion style
- [Hamcrest 3](https://hamcrest.org/JavaHamcrest/) (`org.hamcrest.MatcherAssert.assertThat`)

See [`TEST_PLAN.md`](TEST_PLAN.md) for what we are testing, who owns what, and the 7-week schedule.

The application code compiles at Java 8, but test code compiles at Java 17, so you need JDK 17 or newer to run the
tests.

## Building

Requirements: JDK 17 or newer and Maven 3.6 or newer.

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
