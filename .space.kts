/**
* JetBrains Space Automation
* This Kotlin script file lets you automate build activities
* For more info, see https://www.jetbrains.com/help/space/automation.html
*/

job("Build") {
  startOn {
    gitPush {
      enabled = true
    }
  }

  // Use Amazon Corretto 17 to avoids the "xargs not present" error.
  gradlew("amazoncorretto:17", "clean downloadWaterfall reCompressWaterfall compileJava jar shadowJar reobfJar", init = {
    env["CI"] = "true"

    shellScript {
        content = """
            #!/bin/bash
            uname -a;
            cat /etc/os-release;
        """.trimIndent()
    }
  })
}
