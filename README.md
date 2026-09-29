# mini-spring

[![build](https://github.com/andronaft/JAVA_SelfMadeSpring/actions/workflows/build.yml/badge.svg)](https://github.com/andronaft/JAVA_SelfMadeSpring/actions/workflows/build.yml)
![Java 17+](https://img.shields.io/badge/Java-17%2B-blue)

A minimal IoC / dependency-injection container written from scratch in plain Java, with no dependencies.

The goal isn't to replace Spring. I wanted to understand what Spring actually does between
`new AnnotationConfigApplicationContext(...)` and the first working bean: how classes are found,
how dependencies get injected, how circular dependencies are resolved, where AOP proxies come
from, and when each lifecycle callback runs.

## Features

| Feature | How it works here |
|---|---|
| Component scanning | Finds `@Component`, `@Service` and `@Configuration` classes in a package and all its subpackages, in directories and inside JARs |
| Bean definitions | Scanning registers a `BeanDefinition` per bean; instances are created from them on demand, dependencies first |
| Constructor injection | The only constructor, else the `@Autowired` one, else the no-arg one. Errors name the parameter |
| Field injection | `@Autowired` fields, including ones declared in superclasses, set directly (no setter needed) |
| Choosing between beans | `@Qualifier` (bean name or qualifier), then `@Primary`, then the field or parameter name |
| `List<T>` injection | Every bean of type `T`, in registration order; a composite doesn't receive itself |
| Circular dependencies | Field cycles are resolved through Spring's three singleton caches; constructor cycles fail with the full path, e.g. `alpha -> beta -> gamma -> alpha` |
| `@Configuration` + `@Bean` | Methods that produce beans, with injected parameters; `static` ones don't need the configuration instance |
| `@Value` | `${key}`, `${key:default}` from `application.properties` and system properties, converted to `String`, numbers, `boolean` or enums |
| Scopes | `singleton` (default) and `@Scope("prototype")`: a new instance for every request and injection point |
| `BeanPostProcessor` | Hooks before and after initialization; the returned object replaces the bean, and dependents receive it |
| AOP: `@Timed` | `TimedBeanPostProcessor` wraps beans in JDK dynamic proxies that measure `@Timed` methods, cycles included |
| Lifecycle callbacks | `BeanNameAware`, `InitializingBean`, `@PreDestroy`, `DisposableBean` (on the object behind a proxy, in reverse creation order) |
| `ApplicationContext` | Registers post-processor beans first, creates all singletons at startup, destroys what it created if startup fails, is `AutoCloseable`, publishes `ContextClosedEvent` |
| Clear errors | `NoSuchBeanDefinitionException`, `NoUniqueBeanDefinitionException`, `BeanCurrentlyInCreationException`, `BeanNotOfRequiredTypeException`, each naming the bean and the field or parameter |

## Bean lifecycle

```
ApplicationContext(basePackage)
 │
 ├─ 1. scan                        a BeanDefinition per @Component/@Service/@Configuration class and @Bean method
 ├─ 2. registerBeanPostProcessors  create the BeanPostProcessor beans first, so they see every other bean
 ├─ 3. preInstantiateSingletons    getBean(name) for every singleton
 │
 │     getBean(name)
 │      ├─ singleton already there?     return it; inside a cycle, return its early reference
 │      ├─ instantiate                  constructor or @Bean method; each argument is getBean(...) (recursion)
 │      ├─ expose early reference       a factory in the 3rd-level cache, for cycles through fields
 │      ├─ populate                     @Autowired and @Value fields, superclasses first
 │      ├─ BeanNameAware.setBeanName
 │      ├─ BeanPostProcessor.postProcessBeforeInitialization
 │      ├─ InitializingBean.afterPropertiesSet
 │      ├─ BeanPostProcessor.postProcessAfterInitialization   (may return a proxy, which replaces the bean)
 │      └─ cache the singleton          prototypes are returned without caching
 │
 │   ── beans are ready ──
 │
 └─ close()                        ApplicationListener<ContextClosedEvent>.onApplicationEvent
                                   @PreDestroy, then DisposableBean.destroy, in reverse creation order
```

Spring follows the same order, and most of the steps have the same names in Spring
(`AbstractAutowireCapableBeanFactory.doCreateBean`, `DefaultSingletonBeanRegistry.getSingleton`).

### How a circular dependency is resolved

`Chicken` has an `@Autowired Egg` field and `Egg` has an `@Autowired Chicken` field:

1. `getBean("chicken")` instantiates `Chicken` and registers a factory for its early reference.
2. Populating `Chicken` needs `egg`: `getBean("egg")` instantiates `Egg`.
3. Populating `Egg` needs `chicken`, which is still in creation: the factory hands out the early
   reference (moved to the 2nd-level cache), so `Egg` finishes.
4. `Chicken` gets the finished `Egg` and finishes too.

If a post-processor wraps `Chicken` in a proxy, `Egg` would hold the raw object. So a
`SmartInstantiationAwareBeanPostProcessor` gets to create the proxy already in step 3
(`getEarlyBeanReference`), which is what `TimedBeanPostProcessor` does. A post-processor that
replaces such a bean later is rejected with `BeanCurrentlyInCreationException`.

Through constructors the cycle can't be broken, because nothing exists yet to hand out early:

```
Circular dependency: alpha -> beta -> gamma -> alpha. 'gamma' needs 'alpha' before 'alpha' is even
constructed; inject 'beta' into 'alpha' through a field instead, so 'alpha' can be created first
```

## Usage

```java
public interface DiscountPolicy { int apply(int price); }

@Component @Primary
public class SeasonalDiscount implements DiscountPolicy { /* ... */ }

@Component @Qualifier("loyal")
public class LoyaltyDiscount implements DiscountPolicy { /* ... */ }

@Service
public class ProductCatalog implements Catalog {
    private final DiscountPolicy discount;
    private final int basePrice;

    // The only constructor: no @Autowired needed. Gets SeasonalDiscount (@Primary) and a property.
    public ProductCatalog(DiscountPolicy discount, @Value("${shop.base-price:100}") int basePrice) { /* ... */ }

    @Timed
    public int priceOf(String product) { /* ... */ }
}

@Component
public class Checkout {
    @Autowired private Catalog catalog;                                // the @Timed proxy
    @Autowired @Qualifier("loyal") private DiscountPolicy loyalty;
    @Autowired private List<DiscountPolicy> allDiscounts;
    @Autowired private Currency currency;                              // made by a @Bean method

    @PreDestroy
    public void shutdown() { /* ... */ }
}

@Component @Scope("prototype")
public class Cart { /* ... */ }

@Configuration
public class ShopConfig {
    @Bean
    public static TimedBeanPostProcessor timedBeanPostProcessor() {
        return new TimedBeanPostProcessor();
    }

    @Bean
    public Currency currency(@Value("${shop.currency:EUR}") String code) {
        return Currency.getInstance(code);
    }
}

try (ApplicationContext context = new ApplicationContext("com.zuk.demo.shop")) {
    Checkout checkout = context.getBean(Checkout.class);
}
```

## Running

Requires JDK 17+ and Maven.

```bash
mvn verify      # compile and run the tests
mvn exec:java   # run the demo (com.zuk.demo.Main)
```

The demo first drives a bare `BeanFactory` by hand, then starts an `ApplicationContext` for a small shop:

```
==== BeanFactory, step by step ====
---CustomPostProcessor Before promotionsService
PromotionsService: afterPropertiesSet called
---CustomPostProcessor After promotionsService
---CustomPostProcessor Before productService
---CustomPostProcessor After productService
Injected PromotionsService, bean name: promotionsService
ProductService: @PreDestroy called

==== ApplicationContext ====
[timed] productCatalog.priceOf() took 0.412 ms
Quote with the @Primary discount: book: 180 UAH
@Qualifier("loyal") discount on 200: 160
List<DiscountPolicy>: [LoyaltyDiscount, SeasonalDiscount]
Catalog injected into Checkout is a proxy: jdk.proxy2.$Proxy13
Prototype: two requests, two carts: true
Checkout: received ContextClosedEvent
```

`shop.currency` and `shop.base-price` come from `src/main/resources/application.properties`;
override them with `-Dshop.currency=USD`. The demo also runs from a packaged JAR:

```bash
mvn package
java -cp target/mini-spring-1.0-SNAPSHOT.jar com.zuk.demo.Main
```

## Project structure

```
src/main/java/com/zuk/
├── minispring/
│   ├── annotation/   @Component, @Service, @Configuration, @Bean, @Autowired, @Qualifier, @Primary, @Value, @Scope, @PreDestroy
│   ├── beans/        BeanFactory, BeanDefinition, ClassPathScanner, PropertyResolver, post-processor
│   │                 and lifecycle interfaces, exceptions
│   ├── context/      ApplicationContext, ApplicationListener, ContextClosedEvent
│   └── aop/          @Timed, TimedBeanPostProcessor
└── demo/
    ├── lifecycle/    beans for the step-by-step BeanFactory run
    └── shop/         a small shop using every feature
src/test/java/        JUnit 5 tests for each feature and lifecycle phase
```

## Differences from real Spring

Some parts are simplified on purpose. These are the main differences:

| mini-spring | Spring Framework |
|---|---|
| Loads every scanned class to check its annotations | Reads class files with ASM, so unannotated classes are never loaded |
| Only `@Component`, `@Service` and `@Configuration` themselves mark a bean | Any annotation meta-annotated with `@Component` does (`@Repository`, `@RestController`, your own) |
| Fields and constructors only | Also setter and arbitrary method injection, `Optional<T>`, `ObjectProvider<T>`, `@Lazy` |
| `List<T>` in registration order | Also arrays, `Set<T>`, `Map<String, T>`, ordered by `@Order` / `Ordered` |
| Field cycles are always resolved | Spring Framework resolves them the same way, but Spring Boot 2.6+ rejects them unless `spring.main.allow-circular-references=true` |
| `@Configuration` classes aren't subclassed, so a `@Bean` method calling another one gets a new object | CGLIB subclasses make such calls return the singleton |
| JDK proxies only: a `@Timed` bean needs an interface and is injected by it | Falls back to CGLIB subclasses for classes without interfaces |
| One built-in aspect, `@Timed` | Spring AOP with AspectJ pointcuts and any kind of advice |
| `@Value` supports `${...}` placeholders and basic types | SpEL (`#{...}`) and a full `ConversionService` |
| `singleton` and `prototype` scopes | Also `request`, `session`, `application` and custom scopes |
| Definitions can't be changed once registered | `BeanFactoryPostProcessor` can modify them before any bean is created |
| Supports only `ContextClosedEvent` | A general event system (`ApplicationEventPublisher`, `@EventListener`) |

## Roadmap

- [x] Constructor injection, with a dependency graph and circular-dependency detection
- [x] Injection by interface; clear `NoSuchBeanDefinitionException` / `NoUniqueBeanDefinitionException`
- [x] `@Qualifier` and `@Primary`, and `List<T>` injection
- [x] Put the bean returned by `BeanPostProcessor` back into the registry
- [x] A JDK dynamic-proxy based `@Timed` aspect, with dependents receiving the proxy
- [x] `@Configuration` + `@Bean`, `@Value("${...}")` from `application.properties`
- [x] `prototype` scope
- [x] Recursive package scanning that also works inside a JAR
- [ ] `@PostConstruct`
- [ ] General events: `publishEvent()` and `@EventListener`
- [ ] Meta-annotations, so `@Repository` or a custom stereotype can be defined with `@Component`
- [ ] `BeanFactoryPostProcessor`
