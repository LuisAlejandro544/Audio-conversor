# CMake configuration for AudioStudio Native Engine
cmake_minimum_required(VERSION 3.22.1)
project("audiostudio_native" LANGUAGES C CXX)

# Habilitar el estándar moderno C++20 de forma estricta
set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)

# Habilitar estándar C11 para el núcleo puro de FFmpeg
set(CMAKE_C_STANDARD 11)
set(CMAKE_C_STANDARD_REQUIRED ON)

# Opciones de compilación para C++20 y C (optimización y advertencias limpias)
add_compile_options(
    -Wall
    -Wextra
    -Wno-unused-parameter
    -O3
)

# Incluir cabeceras nativas puras de FFmpeg
include_directories(
    ${CMAKE_CURRENT_SOURCE_DIR}
    ${CMAKE_CURRENT_SOURCE_DIR}/include
)

# Definir la biblioteca compartida nativa de la app con el núcleo puro de FFmpeg y el puente JNI
add_library(
    audiostudio_native
    SHARED
    ffmpeg_pure_core.c
    native-lib.cpp
)

# Enlazar con las bibliotecas del sistema Android (logging de NDK)
find_library(
    log-lib
    log
)

target_link_libraries(
    audiostudio_native
    PRIVATE
    ${log-lib}
)
