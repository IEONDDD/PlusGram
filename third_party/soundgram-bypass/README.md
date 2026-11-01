# SoundGram Bypass engine

Imported from https://github.com/soundgram-project/SoundGram at
`b489ce80fdc5390844902582d50b0476a6278d78` (GPL-2.0).
The Go engine is copied unchanged from
`TMessagesProj/src/main/java/soundgram/wsproxy/tg-ws-proxy.go`.
Android service, detector, settings and JNI bridge were adapted to the Plusgram
package and the immutable ProxySettings API. Upstream networking, Cloudflare,
FakeTLS, DNS-over-HTTPS, connection pooling and recovery behavior are retained.

The engine is built from source by Gradle using Go and the Android NDK.
`-trimpath` and stripped symbols prevent local source paths entering the binary.
No prebuilt upstream native library is shipped.

## Android engine tests

Cross-compile the Go tests with `GOOS=android`, `GOARCH=amd64`, `CGO_ENABLED=1`
and `CC` pointing to the NDK `x86_64-linux-android24-clang` compiler:

```sh
go test -c -trimpath -o bypass-tests
adb push bypass-tests /data/local/tmp/bypass-tests
adb shell chmod 755 /data/local/tmp/bypass-tests
adb shell '/data/local/tmp/bypass-tests -test.v=true'
adb shell 'PLUSGRAM_NETWORK_TEST=1 /data/local/tmp/bypass-tests -test.v=true'
```

The opt-in network test verifies a real Telegram MTProto `req_pq_multi`/`resPQ`
exchange and requires that it used Cloudflare. It uses no Telegram account,
phone number, application credentials or user session. Run it on an Android
device or emulator with the matching ABI; the release is built for arm64-v8a.
