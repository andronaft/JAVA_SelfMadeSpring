# mini-spring

[![build](https://github.com/andronaft/JAVA_SelfMadeSpring/actions/workflows/build.yml/badge.svg)](https://github.com/andronaft/JAVA_SelfMadeSpring/actions/workflows/build.yml)
![Java 17+](https://img.shields.io/badge/Java-17%2B-blue)

A minimal IoC / dependency-injection container written from scratch in plain Java, with no dependencies.

The goal isn't to replace Spring. I wanted to understand what Spring actually does between
`new AnnotationConfigApplicationContext(...)` and the first working bean: how classes are found,
how dependencies get injected, and when each lifecycle callback runs.

## Features

| Feature | How it works here |
|---|---|
| Component scanning | Finds `@Component` / `@Service` classes in a package and all its subpackages, in directories and inside JARs |
| Singleton registry | One instance per bean, looked up by name or by type (`getBean(ProductService.class)`) |
| Dependency injection | `@Autowired` fields are resolved by type, interfaces included, and set directly (no setter needed) |
| Clear errors | `NoSuchBeanDefinitionException`, `NoUniqueBeanDefinitionException`, `BeanCreationException` with the bean and field name |
| `BeanNameAware` | The container passes each bean its own name |
| `BeanPostProcessor` | Hooks before and after initialization; the returned object replaces the bean, so it can be a proxy |
| `InitializingBean` | `afterPropertiesSet()` runs once all dependencies are injected |
| `@PreDestroy` / `DisposableBean` | Cleanup callbacks when the container closes |
| `ApplicationContext` | Runs the whole lifecycle in one call, is `AutoCloseable`, and publishes `ContextClosedEvent` to `ApplicationListener`s |

## Bean lifecycle

```
            ApplicationContext(basePackage)
                          │
  1. instantiate          │  scan package + subpackages → find @Component/@Service → call no-arg constructor
                          ▼
  2. populateProperties   │  for each @Autowired field → find the one bean assignable to its type → set field
                          ▼
  3. injectBeanNames      │  BeanNameAware.setBeanName(name)
                          ▼
  4. initializeBeans      │  BeanPostProcessor.postProcessBeforeInitialization
                          │  InitializingBean.afterPropertiesSet
                          │  BeanPostProcessor.postProcessAfterInitialization  (result replaces the bean)
                          ▼
                  ── beans are ready ──
                          │
  5. close                │  ApplicationListener<ContextClosedEvent>.onApplicationEvent
                          │  @PreDestroy methods
                          │  DisposableBean.destroy
                          ▼
```

Spring follows the same order, and most of the steps have the same names in Spring.

## Usage

```java
@Service
public class PromotionsService implements BeanNameAware, InitializingBean {
    @Override public void setBeanName(String name) { /* ... */ }
    @Override public void afterPropertiesSet()     { /* ... */ }
}

@Component
public class ProductService {
    @Autowired
    private PromotionsService promotionsService;

    @PreDestroy
    public void shutdown() { /* ... */ }
}

try (ApplicationContext context = new ApplicationContext("com.zuk.demo")) {
    ProductService productService = context.getBean(ProductService.class);
}
```

## Running

Requires JDK 17+ and Maven.

```bash
mvn verify      # compile and run the tests
mvn exec:java   # run the demo (com.zuk.demo.Main)
```

The demo prints every lifecycle step as it happens:

```
---CustomPostProcessor Before PromotionsService
PromotionsService: afterPropertiesSet called
---CustomPostProcessor After PromotionsService
...
ProductService: @PreDestroy called
PromotionsService: received ContextClosedEvent
```

It also runs from a packaged JAR:

```bash
mvn package
java -cp target/mini-spring-1.0-SNAPSHOT.jar com.zuk.demo.Main
```

## Project structure

```
src/main/java/com/zuk/
├── minispring/
│   ├── annotation/   @Component, @Service, @Autowired, @PreDestroy
│   ├── beans/        BeanFactory, ClassPathScanner, BeanPostProcessor, Aware/Initializing/DisposableBean, exceptions
│   └── context/      ApplicationContext, ApplicationListener, ContextClosedEvent
└── demo/             Example application built on the container
src/test/java/        JUnit 5 tests for every lifecycle phase
```

## Differences from real Spring

Some parts are simplified on purpose. These are the main differences:

| mini-spring | Spring Framework |
|---|---|
| Loads every scanned class to check its annotations | Reads class files with ASM, so unannotated classes are never loaded |
| Bean name is the simple class name, so two classes with the same name in different packages conflict | Bean name is the decapitalized class name; conflicts can be resolved with `@Component("name")` |
| Needs a no-arg constructor | Constructor injection (the recommended style), factory methods, `@Bean` |
| Fails on several candidates for one `@Autowired` field | Picks one with `@Qualifier` / `@Primary`, or falls back to the field name |
| Singleton scope only | `singleton`, `prototype`, `request`, `session`, custom scopes |
| Injects all dependencies before any post-processor runs, so dependents get the raw bean, not the proxy | Creates beans on demand in dependency order, so dependents receive the proxy (`@Transactional`, `@Async`) |
| No detection of circular dependencies | Detects cycles; resolves setter-injection cycles through early references |
| Bean definitions and instances are the same thing | Uses a separate `BeanDefinition` metadata layer, so beans can be configured before they are created |
| Supports only `ContextClosedEvent` | A general event system (`ApplicationEventPublisher`, `@EventListener`) |

## Roadmap

- [ ] Constructor injection, with a dependency graph and circular-dependency detection
- [x] Injection by interface; clear `NoSuchBeanDefinitionException` / `NoUniqueBeanDefinitionException`
- [ ] `@Qualifier` and `@Primary`
- [x] Put the bean returned by `BeanPostProcessor` back into the registry
- [ ] A JDK dynamic-proxy based `@Timed` aspect, with dependents receiving the proxy
- [ ] `@Configuration` + `@Bean`, `@Value("${...}")` from `application.properties`
- [ ] `prototype` scope
- [x] Recursive package scanning that also works inside a JAR
