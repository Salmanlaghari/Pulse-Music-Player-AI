# Security Notes

## Keystore exposure (found 2026-08-24)
Commit `d425931` historically committed `app/release.keystore` together with its
passwords (`storePassword = "pulsepassword"`, alias `pulseKey`). Although the
files are removed from the current tree and `.gitignore`d, they remain in git
history and CAN be extracted by anyone who clones the repo.

### Required action (owner decision)
1. Generate a NEW keystore, update the `RELEASE_KEYSTORE_BASE64`,
   `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`
   GitHub secrets.
2. Note: rotating the key changes the APK signature — existing users must
   uninstall/reinstall once.
3. Optionally rewrite history (`git filter-repo --path app/release.keystore
   --invert-paths` + force-push) to purge the blob.

Current state: HEAD contains no keystores or passwords; CI signs using GitHub
secrets only.

## API keys scan
No hardcoded third-party API keys found in source. All music endpoints used are
public/no-key (JioSaavn web API, WordPress REST, Internet Archive).
