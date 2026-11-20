## Summary
<!-- Describe what this PR changes and why. -->

## Checklist
- [ ] Code respects client/server isolation (`common/` never imports `client/`).
- [ ] Ran `.\gradlew.bat runData --no-daemon` if blocks, items, recipes, tags, or models changed.
- [ ] Ran `.\gradlew.bat build --no-daemon` and `.\gradlew.bat runGameTestServer --no-daemon` with all tests passing.
- [ ] Added/updated translations in both `en_us.json` and `pt_br.json` with identical key sets.
