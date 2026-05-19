#!/usr/bin/env bash
# setup_tdlib.sh — Build TDLib for Android using Docker and install artifacts
# into the expected app/ directories.
#
# Outputs:
#   app/libs/tdlib.jar                       — Java bindings JAR
#   app/src/main/jniLibs/<ABI>/libtdjni.so  — Native libs (arm64-v8a, armeabi-v7a, x86_64, x86)
#
# Prerequisites: docker, git, java (javac + jar)

set -euo pipefail

REPO_URL="https://github.com/tdlib/td.git"
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
WORK_DIR="$ROOT_DIR/.tdlib_build"
OUT_DIR="$WORK_DIR/output"
CLASSES_DIR="$WORK_DIR/classes"

ABIS=(arm64-v8a armeabi-v7a x86_64 x86)

# ── Colour helpers ────────────────────────────────────────────────────────────
red()   { echo -e "\033[0;31m$*\033[0m"; }
green() { echo -e "\033[0;32m$*\033[0m"; }
blue()  { echo -e "\033[0;34m$*\033[0m"; }

# ── Prerequisite check ────────────────────────────────────────────────────────
check_prereqs() {
    local missing=0
    for cmd in docker git javac jar; do
        if ! command -v "$cmd" &>/dev/null; then
            red "Missing prerequisite: $cmd"
            missing=1
        fi
    done
    if [[ $missing -eq 1 ]]; then
        echo "Install the missing tools and re-run."
        exit 1
    fi
    if ! docker info &>/dev/null; then
        red "Docker daemon is not running. Start Docker and re-run."
        exit 1
    fi
}

# ── Sparse-clone just the Android example directory ───────────────────────────
clone_or_update() {
    local td_dir="$WORK_DIR/td"
    if [[ -d "$td_dir/.git" ]]; then
        blue "Updating existing td clone…"
        git -C "$td_dir" fetch --depth=1 origin master
        git -C "$td_dir" checkout FETCH_HEAD
    else
        blue "Sparse-cloning tdlib/td (example/android only)…"
        mkdir -p "$td_dir"
        git -C "$td_dir" init
        git -C "$td_dir" remote add origin "$REPO_URL"
        git -C "$td_dir" config core.sparseCheckout true
        echo "example/android/" > "$td_dir/.git/info/sparse-checkout"
        git -C "$td_dir" fetch --depth=1 origin master
        git -C "$td_dir" checkout FETCH_HEAD
    fi
}

# ── Docker build ──────────────────────────────────────────────────────────────
build_tdlib() {
    local android_dir="$WORK_DIR/td/example/android"
    mkdir -p "$OUT_DIR"

    blue "Running Docker build (this takes 20-40 min on first run)…"
    docker build \
        -f "$SCRIPT_DIR/Dockerfile.tdlib" \
        --output "$OUT_DIR" \
        "$android_dir"
}

# ── Extract archive ───────────────────────────────────────────────────────────
extract_artifacts() {
    local zip="$OUT_DIR/tdlib.zip"
    if [[ ! -f "$zip" ]]; then
        red "Expected $zip was not produced by Docker build."
        exit 1
    fi
    blue "Extracting tdlib.zip…"
    local extract_dir="$WORK_DIR/extracted"
    rm -rf "$extract_dir"
    mkdir -p "$extract_dir"
    unzip -q "$zip" -d "$extract_dir"
    echo "$extract_dir"
}

# ── Copy .so files ────────────────────────────────────────────────────────────
install_natives() {
    local extract_dir="$1"
    blue "Installing native libraries…"
    for abi in "${ABIS[@]}"; do
        local src="$extract_dir/tdlib/libs/$abi/libtdjni.so"
        local dst_dir="$ROOT_DIR/app/src/main/jniLibs/$abi"
        if [[ -f "$src" ]]; then
            mkdir -p "$dst_dir"
            cp "$src" "$dst_dir/libtdjni.so"
            green "  Installed $abi/libtdjni.so"
        else
            echo "  Skipping $abi (not present in build output)"
        fi
    done
}

# ── Compile Java sources → JAR ────────────────────────────────────────────────
build_jar() {
    local extract_dir="$1"
    local java_src="$extract_dir/tdlib/java"

    if [[ ! -d "$java_src" ]]; then
        red "Java source directory not found at $java_src"
        exit 1
    fi

    blue "Compiling TDLib Java sources…"
    rm -rf "$CLASSES_DIR"
    mkdir -p "$CLASSES_DIR"

    # Collect all .java files
    mapfile -t java_files < <(find "$java_src" -name "*.java")
    if [[ ${#java_files[@]} -eq 0 ]]; then
        red "No .java files found under $java_src"
        exit 1
    fi

    javac --release 8 -d "$CLASSES_DIR" "${java_files[@]}"

    local jar_dir="$ROOT_DIR/app/libs"
    mkdir -p "$jar_dir"
    jar cf "$jar_dir/tdlib.jar" -C "$CLASSES_DIR" .
    green "  Created app/libs/tdlib.jar (${#java_files[@]} source files)"
}

# ── Main ──────────────────────────────────────────────────────────────────────
main() {
    echo ""
    blue "=== SmartTelegramFilter — TDLib setup ==="
    echo ""

    check_prereqs
    mkdir -p "$WORK_DIR"
    clone_or_update
    build_tdlib
    local extract_dir
    extract_dir="$(extract_artifacts)"
    install_natives "$extract_dir"
    build_jar "$extract_dir"

    echo ""
    green "=== Done! TDLib artifacts installed successfully ==="
    echo ""
    echo "Next steps:"
    echo "  1. Add TELEGRAM_API_ID and TELEGRAM_API_HASH to local.properties"
    echo "  2. Open the project in Android Studio and build"
    echo ""
}

main "$@"
