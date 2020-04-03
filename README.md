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
| Component scanning | Finds classes annotated with `@Component` / `@Service` in a package through the class loader |
| Singleton registry | One instance per bean, stored in a `Map<String, Object>` |
| Dependency injection | `@Autowired` fields are resolved by type and injected through their setter |
| `BeanNameAware` | The container passes each bean its own name |
| `BeanPostProcessor` | Hooks that run before and after bean initialization |
| `InitializingBean` | `afterPropertiesSet()` runs once all dependencies are injected |
| `@PreDestroy` / `DisposableBean` | Cleanup callbacks when the container closes |
| `ApplicationContext` | Runs the whole lifecycle in one call and publishes `ContextClosedEvent` to `ApplicationListener`s |

## Bean lifecycle

```
            ApplicationContext(basePackage)
                          │
  1. instantiate          │  scan package → find @Component/@Service → call no-arg constructor
                          ▼
  2. populateProperties   │  for each @Autowired field → find bean of that type → call setter
                          ▼
  3. injectBeanNames      │  BeanNameAware.setBeanName(name)
                          ▼
  4. initializeBeans      │  BeanPostProcessor.postProcessBeforeInitialization
                          │  InitializingBean.afterPropertiesSet
                          │  BeanPostProcessor.postProcessAfterInitialization
                          ▼
                  ── beans are ready ──
                          │
  5. close                │  @PreDestroy methods
                          │  DisposableBean.destroy
                          │  ApplicationListener<ContextClosedEvent>.onApplicationEvent
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

    public void setPromotionsService(PromotionsService promotionsService) {
        this.promotionsService = promotionsService;
    }

    @PreDestroy
    public void shutdown() { /* ... */ }
}

ApplicationContext context = new ApplicationContext("com.zuk.demo");
ProductService productService = (ProductService) context.getBean("ProductService");
context.close();
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

## Project structure

```
src/main/java/com/zuk/
├── minispring/
│   ├── annotation/   @Component, @Service, @Autowired, @PreDestroy
│   ├── beans/        BeanFactory, BeanPostProcessor, BeanNameAware, InitializingBean, DisposableBean
│   └── context/      ApplicationContext, ApplicationListener, ContextClosedEvent
└── demo/             Example application built on the container
src/test/java/        JUnit 5 tests for every lifecycle phase
```

## Differences from real Spring

Some parts are simplified on purpose. These are the main differences:

| mini-spring | Spring Framework |
|---|---|
| Scans one package, without subpackages, and only from the file system (no JARs) | Recursive classpath scanning, including JARs, via ASM without loading classes |
| Needs a public no-arg constructor | Constructor injection (the recommended style), factory methods, `@Bean` |
| Matches `@Autowired` by exact class and needs a setter | Matches by type, including interfaces and subclasses, plus `@Qualifier` / `@Primary`; can inject straight into fields |
| Singleton scope only | `singleton`, `prototype`, `request`, `session`, custom scopes |
| The bean a `BeanPostProcessor` returns is ignored | The returned object replaces the bean, which is how AOP proxies (`@Transactional`, `@Async`) work |
| No detection of circular dependencies | Detects cycles; resolves setter-injection cycles through early references |
| Bean definitions and instances are the same thing | Uses a separate `BeanDefinition` metadata layer, so beans can be configured before they are created |
| Supports only `ContextClosedEvent` | A general event system (`ApplicationEventPublisher`, `@EventListener`) |

## Roadmap

- [ ] Constructor injection, with a dependency graph and circular-dependency detection
- [ ] Injection by interface, plus `@Qualifier` and `@Primary`
- [ ] Put the bean returned by `BeanPostProcessor` back into the registry, then build a JDK dynamic-proxy based `@Timed` aspect on it
- [ ] `@Configuration` + `@Bean`, `@Value("${...}")` from `application.properties`
- [ ] `prototype` scope
- [ ] Recursive package scanning that also works inside a JAR
