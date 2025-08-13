plugins {
    java
}

group = "org.dc"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
        implementation.set(JvmImplementation.VENDOR_SPECIFIC)
    }
}

gradle.taskGraph.whenReady {
    println("==> Gradle is running with: ${System.getProperty("java.home")}")
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(21)
    options.encoding = "UTF-8"

    doFirst {
        println("===== JavaCompile Info =====")
        println("Compiler path            : ${javaCompiler.get().metadata.installationPath}")
        println("Source compatibility     : $sourceCompatibility")
        println("Target compatibility     : $targetCompatibility")
        println("Release option           : ${options.release.orNull ?: "N/A"}")
        println("Boot classpath           : ${options.bootstrapClasspath?.files ?: "N/A"}")
        println("Source path              : ${options.sourcepath?.files ?: "N/A"}")
        println("Annotation processor path: ${options.annotationProcessorPath?.files ?: "N/A"}")
        println("============================")
    }
}


// 將依賴複製到指定目錄
tasks.register<Copy>("copyDependencies") {
    from(configurations.runtimeClasspath)
    into(layout.buildDirectory.dir("libs/lib"))
}

// Dry JAR（不含依賴）
tasks.register<Jar>("jar_DryJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "${project.group}.RunEncryptor"
    }
    from(sourceSets.main.get().output)
    //archiveFileName.set("${project.name}-dry.jar")
}

// Thin JAR（只含主程式與外部依賴路徑）
tasks.register<Jar>("jar_ThinJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(
            "Main-Class" to "${project.group}.RunEncryptor",
            "Class-Path" to configurations.runtimeClasspath.get().files.joinToString(" ") { "lib/${it.name}" }
        )
    }
    dependsOn("copyDependencies")
    from(sourceSets.main.get().output)
    //archiveFileName.set("${project.name}-thin.jar")
}

// Fat JAR（含所有依賴）
tasks.register<Jar>("jar_FatJar") {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "${project.group}.Main"
        //attributes["Premain-Class"] = "${project.group}.util.component.LogInitializer"
    }
    from({
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        }
    })
    //archiveFileName.set("${project.name}-fat.jar")
}

// TODO 解決 build.gradle 內建的 jar 會繼續執行或循環引用的問題
// val jarType = project.findProperty("jarType")?.toString() ?: "fatJar"
//
// if (jarType == "fatJar") {
//     println("📦 Building FAT JAR")
//     dependsOn("jar_FatJar")
//     finalizedBy("jar_FatJar")
// } else if (jarType == "thinJar") {
//     println("📦 Building THIN JAR")
//     dependsOn("jar_ThinJar")
//     finalizedBy("jar_ThinJar")
// }

// 若是能修復 build.gradle 內建的 jar 會繼續執行或循環引用的問題，移除以下 task 代碼
tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "${project.group}.RunEncryptor"
    }
    from(sourceSets.main.get().output)
    //archiveFileName.set("${project.name}-dry.jar")
}

// 印出完整依賴樹
tasks.register("printDependencyTree") {
    doLast {
        val config = configurations.runtimeClasspath.get()
        val resolved = config.resolvedConfiguration
        println("Dependency tree for configuration: ${config.name}")
        val visited = mutableSetOf<String>()

        fun printDependency(dep: ResolvedDependency, indent: String = "") {
            val depId = "${dep.moduleGroup}:${dep.moduleName}:${dep.moduleVersion}"
            println("$indent+-- $depId")

            if (!visited.add(depId)) {
                println("$indent    (already printed)")
                return
            }

            dep.children.forEach { child ->
                printDependency(child, "$indent    ")
            }
        }

        resolved.firstLevelModuleDependencies.forEach {
            printDependency(it)
        }
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
    google()
    gradlePluginPortal()
}



val springVersion = "6.2.9"
val lombokVersion = "1.18.38"
val mapstructVersion = "1.6.3"
val lombokMapstructBindingVersion = "0.2.0"



dependencies {
    implementation("org.springframework:spring-context:$springVersion")
    implementation("org.springframework:spring-jdbc:$springVersion")
    implementation("org.springframework:spring-tx:$springVersion")
    implementation("org.springframework:spring-web:$springVersion")
    implementation("jakarta.validation:jakarta.validation-api:3.0.2")
    implementation("org.hibernate.validator:hibernate-validator:9.0.1.Final")

    //gson
    implementation("com.google.code.gson:gson:2.13.1")

    //jdbc
    runtimeOnly("com.mysql:mysql-connector-j:9.3.0")

    //mapStruct
    compileOnly("org.mapstruct:mapstruct:$mapstructVersion")
    annotationProcessor("org.mapstruct:mapstruct-processor:$mapstructVersion")

    //lombok
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")

    //lombok-mapStruct-binding
    implementation("org.projectlombok:lombok-mapstruct-binding:$lombokMapstructBindingVersion")

    implementation("com.google.guava:guava:33.4.8-jre")

    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.13.4")

    //log
    testImplementation("org.apache.logging.log4j:log4j-core:2.24.3")
    testImplementation("org.apache.logging.log4j:log4j-api:2.24.3")
    testImplementation("org.apache.logging.log4j:log4j-slf4j2-impl:2.24.3")
    implementation("org.slf4j:slf4j-api:2.0.17")
    testImplementation("org.slf4j:slf4j-api:2.0.17")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
