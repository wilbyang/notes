### get started
```
implementation 'org.springframework.boot:spring-boot-starter-web'
```

```java
@RestController
class Foobar {
	@GetMapping(value = "/students/{id}")
    public Student getStudentById(@PathVariable Long id) {
    }
}
```

### spring ioc/bean

```java
@PostConstruct
```

### springboot misc

@EnableScheduling
@Scheduled(cron = " 0/50 * * * * ?")

### mysql+jpa

```
implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
implementation 'com.mysql:mysql-connector-j:8.1.0'
```

```yaml
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/sbjpa
    username: root
    password: yangbo
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

```java
@Entity
@Getter
@Setter
public class Student {
    @Id
    @GeneratedValue(strategy=GenerationType.AUTO)
    private Long id;
    private String name;
    private int age;
    @Column(name = "visit", columnDefinition = "json")
    @Convert(converter = VisitConverter.class)
    private Visit visit;

    @Column(name = "created_time", updatable = false)
    private Instant createdTime;

    @Column(unique = true)
    private String userName;
}
```

### jackson

```
spring:
  jackson:
    default-property-inclusion: non_null
```

### swagger

```
implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.1.0'
```
### aop

```java
@EnableAspectJAutoProxy

@Aspect
@Component
public class LoggingAspect {
    
    @Pointcut("execution(* com.example.demo.service.MyService.*(..))")
    public void serviceMethods() {}

    @Before("serviceMethods()")
    public void beforeServiceMethod() {
        System.out.println("Before executing a service method.");
    }
}
```

### lombok
```
implementation 'org.projectlombok:lombok:1.18.28'
annotationProcessor 'org.projectlombok:lombok:1.18.28'
```

```java
@Slf4j
@Data
@Builder
@NoArgsConstructor
```

### kafka

```
@KafkaListener(topics = "tracardi1", groupId = "springcss")
```


### cache

```
implementation 'org.springframework.boot:spring-boot-starter-cache'
implementation 'com.github.ben-manes.caffeine:caffeine:3.0.4'
```

```java
@Configuration
@EnableCaching
public class CachConfig {
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("books");
        cacheManager.setCaffeine(Caffeine.newBuilder().maximumSize(100));
        return cacheManager;
    }

}



@Cacheable(cacheNames = "books")
public List<Student> GetAll() {
    return studentRepository.findAll();
}
```

### docker

```
plugin {
 	id 'com.google.cloud.tools.jib' 
}

jib {
    from {
        image = 'openjdk:11-jre-slim' // 指定基础镜像
    }
    to {
        image = 'my-container-image:latest' // 指定目标镜像名称和标签
    }
    container {
        jvmFlags = ['-Xmx512m', '-Xms256m'] // 配置 JVM 参数
        ports = ['8080'] // 暴露端口
    }
}
```

```
./gradlew jibDockerBuild -Djib.dockerClient.executable=/Users/boya/.rd/bin/docker
```

### http interaction

```
implementation 'org.springframework.cloud:spring-cloud-starter-openfeign'
```

```
@EnableFeignClients

@FeignClient(name = "httpbin", url = "https://httpbin.org")
public interface IHttpBin {
    @GetMapping("/get")
    String get();
}



String s = iHttpBin.get();
```

### test


#### unit-test

```java
	private TodoItemService service;
    private TodoItemRepository repository;

    @BeforeEach
    public void setUp() {
        this.repository = mock(TodoItemRepository.class);
        this.service = new TodoItemService(this.repository);
    }
    @Test
    public void should_add_todo_item() {
        when(repository.save(any())).then(returnsFirstArg());
```
#### jpa-test

```java
@ExtendWith(SpringExtension.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource("classpath:test.properties")
public class StudentRepositoryTest {

    private final StudentRepository repository;

    @Autowired
    public StudentRepositoryTest(StudentRepository repository) {
        this.repository = repository;
    }

    @Test
    public void save_and_get_shouldWork() {
        Student bo1 = Student.builder().age(12).email("yang.wilby@gmail.com").name("bo1").build();
        Student bo2 = Student.builder().age(12).email("yang.wilby@gmail.com").name("bo2").build();

        repository.save(bo1);
        repository.save(bo2);

        List<Student> all = repository.findAll();

        assertThat(all).hasSize(2);
    }


}
```
### rest


