plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.protobuf)
    application
}

kotlin {
    jvmToolchain(17)
}

protobuf {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                // protobuf-javalite: the generated code must be the "lite" runtime,
                // which is what Android needs and what this module links against.
                maybeCreate("java").option("lite")
            }
        }
    }
}

dependencies {
    // `api` so :app inherits the generated protobuf classes and the flow types.
    api(libs.protobuf.javalite)
    api(libs.kotlinx.coroutines.core)

    implementation(libs.bouncycastle.prov)
    implementation(libs.bouncycastle.pkix)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

application {
    // Headless harness used to validate the protocol against a real TV.
    mainClass.set("dev.haris.atvremote.protocol.cli.MainKt")
}

tasks.withType<JavaExec>().configureEach {
    standardInput = System.`in`
}

tasks.withType<Test>().configureEach {
    useJUnit()
}
