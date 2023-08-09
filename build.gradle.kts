plugins { kotlin("jvm").version("1.7.20"); java; `maven-publish` }
repositories { mavenCentral() }
java.toolchain { languageVersion.set(JavaLanguageVersion.of(8)) }
sourceSets.main { java.setSrcDirs(listOf("src/")); resources.setSrcDirs(listOf("res")) }
sourceSets.test { java.setSrcDirs(listOf("test/src")); resources.setSrcDirs(listOf("test/res")) }
