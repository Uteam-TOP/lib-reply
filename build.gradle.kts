plugins {
    `java-library`
    `maven-publish`
}

group = "ru.fvds.cdss13.libreply"
version = "0.0.7"
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21

    withSourcesJar()
    withJavadocJar()
}

repositories {
    mavenCentral()
}

dependencies {
    // Spring
    api("org.springframework.boot:spring-boot-starter:3.2.0")
    api("org.springframework.kafka:spring-kafka:3.1.0")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor:3.2.0")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor:3.2.0")

    // Resilience4j
    api("io.github.resilience4j:resilience4j-retry:2.0.2")
    api("io.github.resilience4j:resilience4j-core:2.0.2")

    // Jackson
    api("com.fasterxml.jackson.core:jackson-databind:2.15.0")
    api("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.0")

    implementation("org.springframework.kafka:spring-kafka:3.3.8")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.0")
    implementation("io.github.resilience4j:resilience4j-retry:2.0.2")
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.1")
    implementation("org.springframework:spring-messaging:6.0.0")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.20.0-rc1")
    implementation("javax.validation:validation-api:2.0.1.Final")

    compileOnly("org.springframework.boot:spring-boot-starter:3.4.2")

    testImplementation("org.springframework.boot:spring-boot-starter-test:3.4.2")
    testImplementation("org.springframework.kafka:spring-kafka-test:3.3.8")
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")


}

tasks.withType<Test> {
    useJUnitPlatform()
}



publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            groupId = "ru.fvds.cdss13.libreply"
            artifactId = "lib-reply"
            version = version.toString()
        }
    }

    repositories {
        maven {
                    name = "GitHubPackages"
                    url = uri("https://maven.pkg.github.com/Uteam-TOP/lib-reply")
                    credentials {
                        username = System.getenv("GITHUB_ACTOR") ?: "Uteam-TOP" // Ваш GitHub username или org name
                        password = System.getenv("GITHUB_TOKEN") ?: ""
                    }
                }
    }
}






/*publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            artifactId = "lib-reply"
            versionMapping {
                usage("java-api") {
                    fromResolutionOf("runtimeClasspath")
                }
                usage("java-runtime") {
                    fromResolutionResult()
                }
            }
            pom {
                name.set("Lib-Reply")
                description.set("Kafka Request-Reply Library")
                url.set("https://github.com/fvds/cdss13-lib-reply")
                /*licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("fvds")
                        name.set("FVDS Team")
                        email.set("dev@fvds.ru")
                    }
                }*/
            }
        }
    }
    repositories {
        maven {
            val releasesRepoUrl = uri("https://oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsRepoUrl = uri("https://oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
            credentials {
                username = project.findProperty("sonatypeUsername") as String? ?: ""
                password = project.findProperty("sonatypePassword") as String? ?: ""
            }
        }
    }
}

// Task для создания ZIP архива
tasks.register<Zip>("createZip") {
    from(".") {
        include("src/**")
        include("build.gradle.kts")
        include("settings.gradle.kts")
        include("README.md")
        include("gradle.properties")
        exclude("build/**")
        exclude(".gradle/**")
        exclude("*.iml")
    }
    archiveFileName.set("lib-reply.zip")
    destinationDirectory.set(file("build/distributions"))
}*/

// Task для сборки библиотеки и создания ZIP
/*tasks.register("buildLibrary") {
    dependsOn("build", "createZip")
    doLast {
        println("ZIP архив создан: build/distributions/lib-reply.zip")
        println("Библиотека собрана: build/libs/lib-reply-${version}.jar")
    }
}

// Конфигурация JavaDoc
tasks.withType<Javadoc> {
    options {
        encoding = "UTF-8"
        (this as StandardJavadocDocletOptions).addStringOption("Xdoclint:none", "-quiet")
    }
}

// Конфигурация компиляции
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.compilerArgs.add("-parameters")
}*/