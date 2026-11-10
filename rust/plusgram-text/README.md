# PlusGramRUST text pilot

The streaming tag parser in ReasoningFilter now has a dependency-free Rust backend
with a C++ JNI bridge. Java retains the per-request state, accumulated reasoning,
flush behavior and signals. Rust processes UTF-16 code units unchanged, including
split or unpaired surrogates. No native handle or Rust allocation survives a call.

## Scope and audit

This is one isolated pilot, based on commit 3c4a33aae4fc87b247ce34383387e114dbd5e73d.
ReasoningFilter has no Android, database or account dependencies and is called from
three AI Client paths. This makes it suitable for differential testing.

The project already contains Rust: gram-wallet and the wallet-engine submodule use
UniFFI/C++/JNI; tlottie also includes a prebuilt Rust runtime. The new module is
therefore isolated in libplusgram_text.so instead of linking another Rust standard
library into tmessages. Only its JNI entry point is exported; other symbols are local.
MTProto/tgnet, authorization, sessions, messaging, calls, media, Python plugins and
the main Telegram interface were not migrated.

The app uses Gradle 9.7.1, AGP 9.3.1, Java/Kotlin target 21, NDK 27.2.12479018,
CMake 3.31.6+, Chaquopy 17.0.0/Python 3.11 and Media3 submodules. The normal Gradle
ABI set is arm64-v8a + x86_64; NATIVE_TARGET selects a single ABI. The pilot supports
those two 64-bit ABIs. ABI splits remain disabled due to the existing Chaquopy
abiFilters constraint. No dependencies were updated in the source configuration.

## Runtime selection and rollback

The pilot is opt-in: -PrustTextEnabled=true sets BuildConfig.RUST_TEXT_ENABLED.
A lazy loader loads plusgram_text once. If loading or symbol resolution fails, or
Rust catches a panic, processing falls back to Java before parser state is committed.

JNI costs more than Java for normal short chunks. Production therefore uses Java
below 4096 UTF-16 units and for sparse blocks. Rust is selected only when both the
first and last 2048-unit windows contain at least 32 opening tags. The heuristic is
conservative, based on desktop measurements; Android tuning remains future work.
The original Java implementation is retained. Build without the flag to disable the
pilot and exclude its library entirely.

## Build

Install Rust stable and the matching targets:

```powershell
rustup target add aarch64-linux-android x86_64-linux-android
cargo install cargo-ndk --version 4.1.2 --locked
```

With the existing Android SDK/NDK/CMake, complete submodules, valid local.properties
and requested JDK available, build from the repository root:

```powershell
$env:NATIVE_TARGET = "arm64-v8a"
.\gradlew.bat --no-daemon --max-workers=4 "-Dorg.gradle.jvmargs=-Xmx6g -XX:+UseG1GC -Dfile.encoding=UTF-8" "-PrustTextEnabled=true" "-PAPP_VERSION_NAME=5.0.0-rust" "-PAPP_VERSION_CODE=7" :TMessagesProj:assembleRelease
```

Use NATIVE_TARGET=x86_64 for an emulator build, or universal for both ABIs.
SKIP is for source/resource checks, not a functional Rust APK.

CMake calls cargo-ndk with --platform 27 inside the crate working directory. Its
archive and JNI shim are linked into a separate shared library, automatically
packaged by AGP. Outputs stay under the CMake build directory. No compiled binaries
or credentials are committed. Both LOAD page alignment and the app's existing
flexible-page-size configuration use 16 KiB. The optional library follows the app's
existing native API 27 setting; API 24-26 behavior is not established by this build.

On the validation machine, the repository daemon criteria requested JetBrains JDK 21,
but only Adoptium 21 and JetBrains 25 were installed. The vendor constraint was
temporarily relaxed for the local build, then the original criteria file restored.
That local workaround is not a dependency/configuration change in this commit.

## Validation

- Rust stable 1.99.1, cargo-ndk 4.1.2.
- cargo fmt --check, cargo clippy --all-targets -- -D warnings, cargo test: passed.
- Four Rust tests cover multiple blocks, every chunk boundary, Unicode/surrogates,
  incomplete/nested tags and empty state.
- Actual desktop JVM/JNI parity: 623,760 comparisons passed, including Java/Rust
  transitions in one stream, null/empty chunks, randomized streams, flush and consumes.
- Separate missing-library fallback and production lazy-loading tests passed.
- Android Rust core and JNI shared library built for arm64-v8a and x86_64.
- The JNI export and 16 KiB LOAD alignment were checked in the ARM64 ELF.
- APK validation: assembleRelease passed (7m 23s); version 5.0.0-rust/code 7, ARM64 APK. apksigner and zipalign -c -P 16 4 passed. The packaged 310,880-byte Rust/JNI library contains the expected export and 16 KiB LOAD alignment. Signing certificate SHA-256 matches v5.0.0: 1837e4fedc2035971feba8a3ee04a744d894732522e8523745c4016dee64ddfa.
- No device or AVD was connected/configured. Android runtime smoke tests and
  end-to-end Android performance/memory measurements were not performed.

Run the host checks on Windows with MSVC 2022, a JDK and CMake:

```powershell
.\tests\rust-text\run-host-tests.ps1 -CMake "path\to\cmake.exe" -JdkHome "path\to\jdk"
```

Add -Benchmark for an illustrative desktop microbenchmark. Under concurrent Android
compilation, dense ~65K-unit input measured about 4363 us/op in Java versus 305 us/op
with adaptive Rust. Sparse inputs favored Java, motivating the gate. This is not an
Android speed claim or a statistical benchmark. For that dense case, measured Java
allocation was ~105K bytes/op for Java versus ~150K for Rust/JNI; native allocations
are not included. There is no claim of lower memory usage.

Before expanding the migration: validate on ARM64 hardware and an x86_64 emulator,
measure p50/p95 latency and both Java/native memory on real workloads, test cancellation
and concurrent requests, and run Telegram smoke checks. Retain Java if Android
measurements do not justify the native path.

The request file FORGPT.txt is not tracked by the repository.
The existing dirty Chaquopy and jlatexmath submodules were not included in the commit.

References: [cargo-ndk](https://github.com/bbqsrc/cargo-ndk),
[JNI specification](https://docs.oracle.com/en/java/javase/21/docs/specs/jni/index.html).
