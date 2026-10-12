plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.protobuf)
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
                maybeCreate("java").option("lite")
            }
        }
    }
}

dependencies {
    api(libs.protobuf.javalite)
    api(libs.kotlinx.coroutines.core)
    implementation(libs.bouncycastle.prov)
    implementation(libs.bouncycastle.pkix)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.withType<Test>().configureEach {
    useJUnit()
}
