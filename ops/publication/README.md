# Publication operations

Publication is part of the ToolNeuron workload.

## Discord
Use an already-configured cloud/repository secret or connected operator tool. Never commit the bot token. Publish meaningful milestones only: beta builds, releases, major accepted features, known P0/P1 issues, qualification results, testing calls.

If direct Discord access is unavailable, write the exact ready-to-send message under ops/outbox/discord/ and continue engineering.

## Website
Preferred target is AnonymousNomad/anonymousnomad.github.io. Keep claims evidence-backed and synchronize release/version/download information with GitHub.

If cross-repository writes are unavailable, maintain site/ in this repository and place the exact publication patch/copy under ops/outbox/site/.

## GitHub releases
Development APKs must be labeled testing/development. A release record states source SHA, versionCode/versionName, artifact hash, verification actually run, and device status. Never claim physical device acceptance from Cloud without hardware.

## Credentials
Secrets stay in the platform secret store/environment. Do not rotate, print, log, or commit them.