plugins { kotlin("jvm").version("1.7.20"); java; `maven-publish` }
group = "moe.nea"
version = "1.0.0"
repositories { mavenCentral() }
java.toolchain { languageVersion.set(JavaLanguageVersion.of(8)) }
java.withSourcesJar()
sourceSets.main { java.setSrcDirs(listOf("src/")); resources.setSrcDirs(listOf("res")) }
sourceSets.test { java.setSrcDirs(listOf("test/src")); resources.setSrcDirs(listOf("test/res")) }

publishing {
	publications {
		create<MavenPublication>("maven") {
			from(components["java"])
		}
	}
}

val testReportFile = layout.buildDirectory.file("test-results/nealisp/results.xml")

tasks.create("testLisps", JavaExec::class) {
	javaLauncher.set(javaToolchains.launcherFor(java.toolchain))
	classpath(sourceSets.test.get().runtimeClasspath)
	mainClass.set("TestMain")
	dependsOn(tasks.testClasses)
	dependsOn(tasks.processTestResources)
	outputs.file(testReportFile)
	systemProperty("test.report", testReportFile.map { it.asFile.absolutePath }.get())
	systemProperty("test.suites", "test")
	systemProperty("test.imports", "secondary")
	group = "verification"
}


tasks.create("testLispsHtml", Exec::class) {
	dependsOn("testLisps")
	executable("xunit-viewer")
	inputs.file(testReportFile)
	val testReportHtmlFile = layout.buildDirectory.file("reports/nealisp/tests/index.html")
	outputs.file(testReportHtmlFile)
	args(
		"-r", testReportFile.map { it.asFile.absolutePath }.get(),
		"-o", testReportHtmlFile.map { it.asFile.absolutePath }.get()
	)
	group = "verification"
}
