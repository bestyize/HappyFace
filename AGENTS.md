# Project guidance

- HappyFace is an Android-first reusable expression system. Preserve the shared Kotlin core when adding new platform entry points.
- Ears, tails and robot bodies are external attachments. Never draw them as part of the reusable full-screen face.
- Keep idle animation and touch response local and functional without cloud access. Model output is a validated high-level intent, never per-frame pixels.
- Check 800×480, 400×320 and 320×240 on every layout change; phone portrait needs its own composition.
- Treat this repository as an early P0 prototype. Document actual test results rather than claiming a platform or build has been verified when it has not.
- Never commit API keys, recorded private conversations or personal images.

