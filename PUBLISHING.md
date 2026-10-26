# Publishing a release

The repository is ready. The `v1.0.0` tag and release notes are prepared locally.

1. Authenticate GitHub CLI with an account that can write to `MohammedAlamkhan/HzLab`. A classic token with `repo` scope works; with a fine-grained token, grant **Contents: Read and write** and **Workflows: Read and write** for the `HzLab` repository.
2. From the repository root:

```bash
export PATH="$PWD/../tools/gh/bin:$PATH"   # optional local GH CLI
gh auth login --hostname github.com --git-protocol https --web
./publish.sh
```

`publish.sh` rebuilds and verifies the APK, updates `dist/SHA256SUMS.txt`, pushes `main` and the `v1.0.0` tag, then creates or updates the GitHub release with the APK and checksum file.

If the release already exists, the script uploads the new APK/checksum with `--clobber` instead of failing.

The signing key lives in `signing/hzlab-release.jks` and is git-ignored. Back it up; updates must be signed with the same key or Android will reject them.
