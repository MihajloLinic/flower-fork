plugins {
    kotlin("jvm")
    id("com.google.protobuf")
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.8.20"
}

kotlin {
    jvmToolchain(17)
}

// Include protobuf sources from the repository
sourceSets {
    main {
        proto {
            srcDir("../../proto")
        }
        java {
            srcDir("$buildDir/generated/source/proto/main/java")
            srcDir("$buildDir/generated/source/proto/main/grpc")
        }
    }
}

val grpcVersion = "1.56.1"

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:3.23.4"
    }
    plugins {
        create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcVersion"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                named("java") {
                    option("lite")
                }
            }
            task.plugins {
                create("grpc") {
                    option("lite")
                }
            }
        }
    }
}

dependencies {
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.8.0"))

    // Coroutines for JVM
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // gRPC + Protobuf (lite works on JVM as well)
    implementation("io.grpc:grpc-okhttp:$grpcVersion")
    implementation("io.grpc:grpc-protobuf-lite:$grpcVersion")
    implementation("io.grpc:grpc-stub:$grpcVersion")

    implementation("javax.annotation:javax.annotation-api:1.3.2")


    testImplementation("junit:junit:4.13.2")
}
