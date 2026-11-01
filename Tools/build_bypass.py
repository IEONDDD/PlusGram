"""Build the vendored SoundGram Go proxy for Android, without local source paths."""
import argparse
import os
from pathlib import Path
import shutil
import subprocess

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--ndk', required=True)
    parser.add_argument('--abi', choices=['arm64-v8a', 'x86_64'], required=True)
    parser.add_argument('--out', required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    arch, triple = {'arm64-v8a': ('arm64', 'aarch64-linux-android'),
                    'x86_64': ('amd64', 'x86_64-linux-android')}[args.abi]
    host = 'windows-x86_64' if os.name == 'nt' else 'linux-x86_64'
    toolchain = Path(args.ndk) / 'toolchains/llvm/prebuilt' / host / 'bin'
    compiler = toolchain / (triple + '24-clang' + ('.cmd' if os.name == 'nt' else ''))
    if not compiler.exists():
        raise SystemExit('Android NDK clang not found')
    go = shutil.which('go')
    if go is None:
        raise SystemExit('Go is required to build the SoundGram Bypass engine')
    output = Path(args.out).resolve()
    output.mkdir(parents=True, exist_ok=True)
    env = os.environ.copy()
    env.update(GOOS='android', GOARCH=arch, CGO_ENABLED='1', CC=str(compiler),
               CGO_CFLAGS='-O2 -g0', CGO_LDFLAGS='-Wl,-z,max-page-size=16384', GOTOOLCHAIN='local')
    subprocess.run([go, 'build', '-trimpath', '-buildvcs=false', '-buildmode=c-shared',
                    '-ldflags=-s -w -buildid=', '-o', str(output/'libtgwsproxy.so'),
                    'tg-ws-proxy.go'], cwd=root/'third_party/soundgram-bypass', env=env, check=True)

if __name__ == '__main__':
    main()
