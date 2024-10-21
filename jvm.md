```
public class HelloWorld {
    public static void main(String[] args) {
        System.out.println("Hello World!");
    }
}
```

```
javac HelloWorld.java
```

```
native-image HelloWorld
```

```
<project>
    <dependencies>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.graalvm.buildtools</groupId>
                <artifactId>native-maven-plugin</artifactId>
                <version>0.9.25</version>
            </plugin>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

```
mvn -Pnative clean native:compile -f pom.xml
```

```
java -jar arthas-boot.jar
profiler start
profiler stop --format flamegraph
```
其中绿色部分代表Java代码，黄色部分代表JVM C++代码，橙色部分代表内核态C语言代码，红色代表用户态C语言代码。