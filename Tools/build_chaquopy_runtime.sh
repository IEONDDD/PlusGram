#!/bin/bash
set -euo pipefail

to_mixed() {
    if command -v cygpath >/dev/null 2>&1; then
        cygpath -m "$1"
    else
        echo "$1"
    fi
}

HERE="$(cd "$(dirname "$0")" && pwd)"
HERE="$(to_mixed "$HERE")"

WORKSPACE="${WORKSPACE:-$HERE/../../chaquopy-build}"
WORKSPACE="$(to_mixed "$WORKSPACE")"
mkdir -p "$WORKSPACE"
WORKSPACE="$(to_mixed "$(cd "$WORKSPACE" && pwd -P)")"

SRC="${SRC:-$HERE/../product/runtime}"
SRC="$(to_mixed "$SRC")"

ABI="${ABI:-arm64-v8a}"
case "$ABI" in arm64-v8a|x86_64) ;; *) echo "Unsupported ABI" >&2; exit 1 ;; esac
PYTHON="${PYTHON:-python3}"
PYTHON="$(to_mixed "$PYTHON")"
PYTHON_VERSION="${PYTHON_VERSION:-3.11}"
PYTHON_TARGET="${PYTHON_TARGET:-3.11.14-0}"

# Auto-detect Android SDK directory
SDK_DIR="${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}"
if [ -z "$SDK_DIR" ] && [ -n "${LOCALAPPDATA:-}" ]; then
    SDK_DIR="$(to_mixed "$LOCALAPPDATA")/Android/Sdk"
fi
if [ -z "$SDK_DIR" ]; then
    SDK_DIR="$HOME/AppData/Local/Android/Sdk"
fi
[ -d "$SDK_DIR" ] || SDK_DIR="$HOME/Android/Sdk"
SDK_DIR="$(to_mixed "$SDK_DIR")"

if [ -n "${NDK:-}" ]; then
    NDK="$(to_mixed "$NDK")"
else
    NDK="$SDK_DIR/ndk/27.2.12479018"
fi

if [ -n "${CMAKE:-}" ]; then
    CMAKE="$(to_mixed "$CMAKE")"
else
    CMAKE="$(command -v cmake || ls -d "$SDK_DIR"/cmake/*/bin/cmake.exe "$SDK_DIR"/cmake/*/bin/cmake 2>/dev/null | sort -V | tail -1 || true)"
fi

OUT="${OUT:-$WORKSPACE/out/$ABI}"
OUT="$(to_mixed "$OUT")"

VENV="$WORKSPACE/venv"
CY="$WORKSPACE/build/cython-$ABI"
WORK="$WORKSPACE/build/cmake-$ABI"
PREFIX="$WORKSPACE/prefix/$ABI"

remove_build_dir() {
    local resolved
    resolved="$(to_mixed "$(realpath -m "$1")")"
    case "$resolved" in
        "$WORKSPACE"/*) rm -rf -- "$resolved" ;;
        *) echo "Refusing removal outside the build workspace" >&2; exit 1 ;;
    esac
}

if [ -n "${PYTHON_TARGET_ZIP:-}" ]; then
    PYTHON_TARGET_ZIP="$(to_mixed "$PYTHON_TARGET_ZIP")"
else
    GRADLE_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
    GRADLE_HOME="$(to_mixed "$GRADLE_HOME")"
    PYTHON_TARGET_ZIP=$(find "$GRADLE_HOME/caches/modules-2/files-2.1/com.chaquo.python/target/$PYTHON_TARGET" -name "target-$PYTHON_TARGET-$ABI.zip" 2>/dev/null | head -1)
fi

echo "Using PYTHON=$PYTHON"
echo "Using NDK=$NDK"
echo "Using CMAKE=$CMAKE"
echo "Using PYTHON_TARGET_ZIP=$PYTHON_TARGET_ZIP"

if [ -f "$VENV/Scripts/cython.exe" ]; then
    CYTHON_BIN="$VENV/Scripts/cython.exe"
    PIP_BIN="$VENV/Scripts/pip.exe"
elif [ -f "$VENV/bin/cython" ]; then
    CYTHON_BIN="$VENV/bin/cython"
    PIP_BIN="$VENV/bin/pip"
else
    "$PYTHON" -m venv "$VENV"
    if [ -f "$VENV/Scripts/pip.exe" ]; then
        PIP_BIN="$VENV/Scripts/pip.exe"
        CYTHON_BIN="$VENV/Scripts/cython.exe"
    else
        PIP_BIN="$VENV/bin/pip"
        CYTHON_BIN="$VENV/bin/cython"
    fi
    "$PIP_BIN" install -q Cython==3.0.11
fi

if [ "$(cat "$PREFIX/source" 2>/dev/null || true)" != "$PYTHON_TARGET_ZIP" ]; then
    remove_build_dir "$PREFIX"
    mkdir -p "$PREFIX/lib"
    unzip -q -o "$PYTHON_TARGET_ZIP" -d "$PREFIX"
    unzip -q -o -j "$PYTHON_TARGET_ZIP" "jniLibs/$ABI/libpython$PYTHON_VERSION.so" -d "$PREFIX/lib"
    echo "$PYTHON_TARGET_ZIP" > "$PREFIX/source"
fi

remove_build_dir "$CY"
remove_build_dir "$WORK"
mkdir -p "$CY" "$WORK" "$OUT"

cythonize() {
    local dir="$1" pyx="$2" stem="${2%.pyx}"
    (cd "$dir" && "$CYTHON_BIN" -Wextra -Werror "$pyx" -I "$CY" -o "$CY/$stem.c")
    sed -i -E \
        -e 's|^__PYX_EXTERN_C (\w+)|/* cythonTask altered */ __PYX_EXTERN_C JNIEXPORT \1 JNICALL|' \
        -e 's|(__pyx_insert_code_object\(c_line)|// cythonTask disabled: \1|' \
        "$CY/$stem.c"
}

echo "Running cythonize..."
cythonize "$SRC/src/main/python/java" chaquopy.pyx
cythonize "$SRC/src/main/python" chaquopy_java.pyx

NINJA="$(dirname "$CMAKE")/ninja.exe"
[ -f "$NINJA" ] || NINJA="$(dirname "$CMAKE")/ninja"
[ -f "$NINJA" ] || NINJA=""
GENERATOR=()
[ -n "$NINJA" ] && GENERATOR=(-GNinja -DCMAKE_MAKE_PROGRAM="$NINJA")

echo "add_compile_options(\"-ffile-prefix-map=$CY/=\")" > "$WORK/prefix-map.cmake"

echo "Configuring CMake..."
"$CMAKE" -S "$SRC" -B "$WORK" "${GENERATOR[@]}" \
    -DCMAKE_PROJECT_INCLUDE="$WORK/prefix-map.cmake" \
    -DCMAKE_BUILD_TYPE=Release \
    -DCHAQUOPY_SRC_DIR="$CY" \
    -DCMAKE_TOOLCHAIN_FILE="$NDK/build/cmake/android.toolchain.cmake" \
    -DANDROID_ABI="$ABI" \
    -DANDROID_NATIVE_API_LEVEL=24 \
    -DANDROID_SUPPORT_FLEXIBLE_PAGE_SIZES=ON \
    -DCHAQUOPY_PYTHON_VERSION="$PYTHON_VERSION" \
    -DCHAQUOPY_INCLUDE_PYTHON="$PREFIX/include" \
    -DCHAQUOPY_LIB_DIRS="$PREFIX/lib" \
    -DCHAQUOPY_PY_SUFFIX=.so

echo "Building CMake..."
"$CMAKE" --build "$WORK"

cp "$WORK/chaquopy.so" "$OUT/chaquopy.so"
cp "$WORK/libchaquopy_java.so" "$OUT/libchaquopy_java.so"
ls -la "$OUT"
