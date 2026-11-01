package main

import (
    "bytes"
    "context"
    "crypto/rand"
    "crypto/sha256"
    "encoding/binary"
    "encoding/hex"
    "io"
    "net"
    "os"
    "testing"
    "time"
)

// These tests run the real Android-compiled engine, not a model of its behavior.
func TestBindFailureIsReported(t *testing.T) {
    listener, err := net.Listen("tcp", "127.0.0.1:0")
    if err != nil { t.Fatal(err) }
    defer listener.Close()
    started := make(chan error, 1)
    err = runProxy(context.Background(), "127.0.0.1", listener.Addr().(*net.TCPAddr).Port, nil, started)
    if err == nil { t.Fatal("occupied port accepted") }
    if <-started == nil { t.Fatal("startup reported success after bind failure") }
}

func TestLifecycleRejectsUnauthenticatedClientsAndReleasesPort(t *testing.T) {
    initLogging(false)
    cfproxyEnabled = false
    poolSize.Store(0)
    free, err := net.Listen("tcp", "127.0.0.1:0")
    if err != nil { t.Fatal(err) }
    port := free.Addr().(*net.TCPAddr).Port
    address := free.Addr().String()
    free.Close()
    for cycle := 0; cycle < 2; cycle++ {
        ctx, cancel := context.WithCancel(context.Background())
        started, done := make(chan error, 1), make(chan error, 1)
        go func() { done <- runProxy(ctx, "127.0.0.1", port, nil, started) }()
        select {
        case err = <-started:
            if err != nil { cancel(); t.Fatal(err) }
        case <-time.After(3*time.Second): cancel(); t.Fatal("startup timeout")
        }
        conn, err := net.DialTimeout("tcp", address, time.Second)
        if err != nil { cancel(); t.Fatal(err) }
        conn.SetDeadline(time.Now().Add(3*time.Second))
        conn.Write(make([]byte, 64))
        n, err := conn.Read(make([]byte, 1))
        conn.Close()
        if n != 0 || err == nil { cancel(); t.Fatal("unauthenticated data accepted") }
        if e, ok := err.(net.Error); ok && e.Timeout() { cancel(); t.Fatal("bad handshake was not rejected") }
        cancel()
        select {
        case err = <-done: if err != nil { t.Fatal(err) }
        case <-time.After(3*time.Second): t.Fatal("shutdown did not release port")
        }
    }
}

func TestDomainFallbacksRemainAvailableOffline(t *testing.T) {
    domains := defaultCfproxyDomains()
    if len(domains) == 0 { t.Fatal("no bundled Cloudflare domains: cold start would require GitHub") }
    merged := mergeCfproxyDomains(domains, domains)
    if len(merged) != len(domains) { t.Fatal("duplicate fallback domains are retained") }
}

// Opt-in live test: a real req_pq_multi exchange through the local proxy.
// No Telegram account, phone number, API credentials or user session is used.
func TestTelegramHandshakeThroughBypass(t *testing.T) {
    if os.Getenv("PLUSGRAM_NETWORK_TEST") != "1" { t.Skip("set PLUSGRAM_NETWORK_TEST=1 for live network test") }
    initLogging(false)
    cfproxyEnabled = true
    cfproxyDomains = defaultCfproxyDomains()
    cfBefore := stats.connectionsCfproxy.Load()
    poolSize.Store(0)
    secret := make([]byte, 16)
    rand.Read(secret)
    proxySecret = hex.EncodeToString(secret)
    free, err := net.Listen("tcp", "127.0.0.1:0")
    if err != nil { t.Fatal(err) }
    port, address := free.Addr().(*net.TCPAddr).Port, free.Addr().String()
    free.Close()
    ctx, cancel := context.WithCancel(context.Background())
    defer cancel()
    started := make(chan error, 1)
    go runProxy(ctx, "127.0.0.1", port, nil, started)
    if err := <-started; err != nil { t.Fatal(err) }
    conn, err := net.DialTimeout("tcp", address, time.Second)
    if err != nil { t.Fatal(err) }
    defer conn.Close()
    conn.SetDeadline(time.Now().Add(50*time.Second))
    header := make([]byte, 64)
    rand.Read(header)
    header[0] = 0x7e
    binary.LittleEndian.PutUint32(header[56:60], 0xeeeeeeee)
    binary.LittleEndian.PutUint16(header[60:62], 2)
    outKey := sha256.Sum256(append(append([]byte{}, header[8:40]...), secret...))
    outgoing, _ := newAESCTR(outKey[:], header[40:56])
    reversed := make([]byte, 48)
    for i := range reversed { reversed[i] = header[55-i] }
    inKey := sha256.Sum256(append(append([]byte{}, reversed[:32]...), secret...))
    incoming, _ := newAESCTR(inKey[:], reversed[32:])
    encrypted := make([]byte, 64)
    outgoing.XORKeyStream(encrypted, header)
    copy(header[56:], encrypted[56:])
    if _, err := conn.Write(header); err != nil { t.Fatal(err) }
    request := make([]byte, 44)
    binary.LittleEndian.PutUint32(request[:4], 40)
    binary.LittleEndian.PutUint64(request[12:20], uint64(time.Now().Unix())<<32)
    binary.LittleEndian.PutUint32(request[20:24], 20)
    binary.LittleEndian.PutUint32(request[24:28], 0xbe7e8ef1)
    nonce := request[28:44]
    rand.Read(nonce)
    expectedNonce := append([]byte{}, nonce...)
    outgoing.XORKeyStream(request, request)
    if _, err := conn.Write(request); err != nil { t.Fatal(err) }
    size := make([]byte, 4)
    if _, err := io.ReadFull(conn, size); err != nil { t.Fatal("no Telegram response through Bypass:", err) }
    incoming.XORKeyStream(size, size)
    length := binary.LittleEndian.Uint32(size)
    if length < 40 || length > 4096 { t.Fatalf("invalid response length: %d", length) }
    reply := make([]byte, length)
    if _, err := io.ReadFull(conn, reply); err != nil { t.Fatal(err) }
    incoming.XORKeyStream(reply, reply)
    if binary.LittleEndian.Uint32(reply[20:24]) != 0x05162463 || !bytes.Equal(reply[24:40], expectedNonce) {
        t.Fatal("response is not resPQ for our nonce")
    }
    if stats.connectionsCfproxy.Load() <= cfBefore { t.Fatal("response used direct TCP instead of Cloudflare") }
    t.Log("Verified Telegram resPQ through the native Cloudflare Bypass engine")
}
