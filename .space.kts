/**
* JetBrains Space Automation
* This Kotlin script file lets you automate build activities
* For more info, see https://www.jetbrains.com/help/space/automation.html
*/

job("Build") {
  container(displayName = "Run gradle build", image = "openjdk:17") {
    kotlinScript { api ->
      env["CI"] = "true"
      api.gradlew("clean downloadWaterfall reCompressWaterfall compileJava jar shadowJar reobfJar")
  	}
  }
}
