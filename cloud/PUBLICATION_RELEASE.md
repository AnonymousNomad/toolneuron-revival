# Publication and release operations

Public surfaces are part of the workload but never justify leaking credentials.

## GitHub
Primary repo: AnonymousNomad/toolneuron-revival.
Push every verified slice to the cloud working branch.
Use Releases for APKs that meet the stated gate. Development APKs are labeled DEVELOPMENT/TESTING.

## Discord
The owner already operates a ToolNeuron Revival / Covert Edge community. Earlier work established a bot/application, but its token is intentionally not in Git.

Rules:
- credentials only from configured environment/repository secret slots;
- never print, rotate or commit credentials;
- do not change server ownership/security;
- publish only evidence-backed updates;
- if credentials are unavailable, write ready-to-send Markdown to ops/outbox/discord/ and continue.

## Website
Preferred site repo: AnonymousNomad/anonymousnomad.github.io.
Add/maintain a ToolNeuron page when cross-repo write access is available.
Otherwise keep deployable fallback content under site/ in this repo and an exact cross-repo publication note under ops/outbox/site/.

Website content:
- what ToolNeuron Revival is;
- current version/status;
- download/release link;
- verified capabilities;
- known limitations;
- screenshots/evidence where available;
- GitHub/community links.
No unverified benchmark or compatibility claims.

## APK identity
Keep application id com.anonymousnomad.toolneuron.covert unless a newer owner directive changes it.
Never commit signing material.
Release signing uses secure environment/secret store.
Record versionCode/versionName, source SHA, APK SHA-256, signer identity if available and verification commands.

## Publish cadence
Meaningful milestones only:
- feature slice accepted;
- beta build;
- release candidate;
- known P0/P1;
- fixed user-visible defect;
- model/device qualification result.

Missing publication credentials block only publication. Prepare the artifact and continue engineering. Destructive, paid, credential-rotating or ownership-changing external actions require the owner.