#!/data/data/com.termux/files/usr/bin/bash
set -e

APP_NAME="FluxCore"
ANDROID_JAR="/data/data/com.termux/files/home/android-sdk/platforms/android-35/android.jar"
if [ -z "$ANDROID_JAR" ]; then
  echo "未找到 android.jar，请手动设置 ANDROID_JAR 路径"
  exit 1
fi

BUILD_DIR="build"
rm -rf "$BUILD_DIR"
mkdir -p "$BUILD_DIR/gen" "$BUILD_DIR/obj" "$BUILD_DIR/apk"

aapt2 compile --dir res -o "$BUILD_DIR/res.zip"
aapt2 link -o "$BUILD_DIR/unaligned.apk" \
  --manifest AndroidManifest.xml \
  -I "$ANDROID_JAR" \
  --java "$BUILD_DIR/gen" \
  "$BUILD_DIR/res.zip"

javac -source 1.8 -target 1.8 -encoding UTF-8 \
  -bootclasspath "$ANDROID_JAR" \
  -d "$BUILD_DIR/obj" \
  $(find src "$BUILD_DIR/gen" -name "*.java")

d8 --lib "$ANDROID_JAR" \
  --output "$BUILD_DIR/apk" \
  $(find "$BUILD_DIR/obj" -name "*.class")

cd "$BUILD_DIR/apk"
zip -u ../unaligned.apk classes.dex
cd ../..

cp "$BUILD_DIR/unaligned.apk" "$BUILD_DIR/aligned.apk"

if [ ! -f debug.keystore ]; then
  keytool -genkeypair -v -keystore debug.keystore \
    -storepass android -alias androiddebugkey -keypass android \
    -keyalg RSA -keysize 2048 -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"
fi

apksigner sign --ks debug.keystore \
  --ks-pass pass:android \
  --key-pass pass:android \
  --out "${APP_NAME}.apk" \
  "$BUILD_DIR/aligned.apk"

echo "打包成功: $(pwd)/${APP_NAME}.apk"

