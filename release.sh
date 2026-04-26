#!/usr/bin/env bash
set -e

FILE="app/build.gradle.kts"
STAMP=".last_build_tree"

# Read current version
current=$(grep 'versionName =' "$FILE" | sed -E 's/.*versionName = "(.*)".*/\1/')

# Get the current source tree hash (tracked files only, excluding the build file itself to avoid loops)
tree_hash=$(git ls-files | grep -v "$FILE" | git hash-object --stdin-paths | sha256sum | cut -c1-16)

last_hash=""
[ -f "$STAMP" ] && last_hash=$(cat "$STAMP")

if [ "$tree_hash" != "$last_hash" ]; then
    # Bump patch version: split on '.', increment last segment
    IFS='.' read -ra parts <<< "$current"
    if [ ${#parts[@]} -eq 3 ]; then
        parts[2]=$(( ${parts[2]} + 1 ))
        new_version=$(IFS='.'; echo "${parts[*]}")
    else
        # Fallback if version format is different
        new_version="$current.1"
    fi

    # Update versionName
    sed -i "s/versionName = \".*\"/versionName = \"$new_version\"/" "$FILE"
    
    # Update versionCode
    vcode=$(grep 'versionCode =' "$FILE" | sed -E 's/.*versionCode = ([0-9]+).*/\1/')
    new_vcode=$((vcode + 1))
    sed -i "s/versionCode = [0-9]*/versionCode = $new_vcode/" "$FILE"

    echo "$tree_hash" > "$STAMP"
    echo "Version bumped: $current (code $vcode) → $new_version (code $new_vcode)"
else
    echo "No source changes, keeping version $current"
fi

./gradlew assembleRelease "$@"

# Rename output APK
version=$(grep 'versionName =' "$FILE" | sed -E 's/.*versionName = "(.*)".*/\1/')
APK_PATH="app/build/outputs/apk/release/app-release.apk"
NEW_APK_PATH="app/build/outputs/apk/release/f1-android-$version.apk"

if [ -f "$APK_PATH" ]; then
    mv "$APK_PATH" "$NEW_APK_PATH"
    echo "APK renamed to: $NEW_APK_PATH"
else
    # Check if it was already renamed or has a different default name
    ACTUAL_APK=$(ls app/build/outputs/apk/release/*.apk | head -n1)
    if [ -n "$ACTUAL_APK" ] && [ "$ACTUAL_APK" != "$NEW_APK_PATH" ]; then
        mv "$ACTUAL_APK" "$NEW_APK_PATH"
        echo "APK renamed from $(basename "$ACTUAL_APK") to: $(basename "$NEW_APK_PATH")"
    fi
fi
