#!/usr/bin/env bash
# ==============================================================================
# Script: generate_debug_keystore.sh
# Misión: Obligar a generar desde CERO, sí o sí y sin esperar (non-interactive),
#         una firma debug (debug.keystore) para Android.
# Compatible: GitHub Actions, Linux, macOS y terminales móviles (Termux).
# ==============================================================================

set -euo pipefail

# 1. Definir la ruta de destino del almacén de claves (keystore)
KEYSTORE_PATH="${1:-debug.keystore}"

echo "=========================================================="
echo "🔐 Generador Automático de Firma Debug para Android"
echo "📁 Destino: $KEYSTORE_PATH"
echo "=========================================================="

# 2. Si ya existe un keystore previo en la ruta indicada, se elimina forzosamente
#    para cumplir con la directiva de regenerarlo desde 0 sí o sí.
if [ -f "$KEYSTORE_PATH" ]; then
    echo "⚠️  Keystore previo detectado. Eliminándolo para generar uno nuevo desde 0..."
    rm -f "$KEYSTORE_PATH"
fi

# 3. Comprobar que la herramienta keytool (incluida en el JDK de Java) esté instalada
if ! command -v keytool &> /dev/null; then
    echo "❌ ERROR: 'keytool' no se encuentra disponible en el PATH."
    echo "   Asegúrate de tener un Java JDK instalado (Java 17 o superior)."
    exit 1
fi

# 4. Generación no interactiva e inmediata de la firma debug con los parámetros estándar de Android
echo "⚙️  Generando firma RSA de 2048 bits para depuración (sin esperas)..."
keytool -genkeypair \
    -v \
    -keystore "$KEYSTORE_PATH" \
    -storepass android \
    -alias androiddebugkey \
    -keypass android \
    -keyalg RSA \
    -keysize 2048 \
    -validity 10000 \
    -dname "CN=Android Debug,O=Android,C=US"

# 5. Verificación final de la existencia del archivo
if [ -f "$KEYSTORE_PATH" ]; then
    echo "✅ Keystore debug generado exitosamente en: $KEYSTORE_PATH"
    echo "🔑 Alias: androiddebugkey | Password: android"
    echo "=========================================================="
    exit 0
else
    echo "❌ ERROR: No se pudo generar el archivo $KEYSTORE_PATH"
    exit 1
fi
