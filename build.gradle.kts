plugins { kotlin("jvm").version("1.7.20"); java; `maven-publish` }
group = "moe.nea"
version = "1.0.0"
repositories { mavenCentral() }
java.toolchain { languageVersion.set(JavaLanguageVersion.of(8)) }
sourceSets.main { java.setSrcDirs(listOf("src/")); resources.setSrcDirs(listOf("res")) }
sourceSets.test { java.setSrcDirs(listOf("test/src")); resources.setSrcDirs(listOf("test/res")) }

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
