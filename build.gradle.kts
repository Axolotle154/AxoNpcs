plugins {
    java
    kotlin("jvm") version "2.3.0"
}

group = "org.axostudio"
version = "4.1.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:[26.1.2.build,)")
    compileOnly("io.netty:netty-transport:4.2.7.Final")

    // Paper plugin libraries are not available early enough to construct a Kotlin
    // JavaPlugin main class, so the Kotlin runtime is bundled into the plugin JAR.
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.0")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.3.0")
    testImplementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.0")
    testImplementation("io.papermc.paper:paper-api:[26.1.2.build,)")
    testImplementation("org.mockito:mockito-core:5.14.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    val props = mapOf("version" to project.version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("paper-plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get().map { dependency ->
            if (dependency.isDirectory) dependency else zipTree(dependency)
        }
    })
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")
}

tasks.test {
    useJUnitPlatform()
    // Mockito's bundled Byte Buddy version can instrument Java 25 test classes
    // when its forward-compatibility mode is enabled.
    systemProperty("net.bytebuddy.experimental", "true")
}
