# Optional pilot: -PrustTextEnabled=true enables this from Gradle.
find_program(PLUSGRAM_CARGO cargo REQUIRED)
find_program(PLUSGRAM_CARGO_NDK cargo-ndk REQUIRED)
if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(RUST_TARGET aarch64-linux-android)
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(RUST_TARGET x86_64-linux-android)
else()
    message(FATAL_ERROR "Rust text pilot supports arm64-v8a and x86_64 only")
endif()
get_filename_component(RUST_ROOT "${CMAKE_CURRENT_LIST_DIR}/../../../rust/plusgram-text" ABSOLUTE)
set(RUST_BUILD "${CMAKE_CURRENT_BINARY_DIR}/cargo")
set(RUST_ARCHIVE "${RUST_BUILD}/${RUST_TARGET}/release/libplusgram_text.a")
add_custom_command(
    OUTPUT "${RUST_ARCHIVE}"
    COMMAND "${CMAKE_COMMAND}" -E env "ANDROID_NDK_HOME=${ANDROID_NDK}" "CARGO_TARGET_DIR=${RUST_BUILD}"
        "${PLUSGRAM_CARGO}" ndk -t "${ANDROID_ABI}" --platform 27 build --locked --release --manifest-path "${RUST_ROOT}/Cargo.toml"
    DEPENDS "${RUST_ROOT}/Cargo.toml" "${RUST_ROOT}/Cargo.lock" "${RUST_ROOT}/src/lib.rs"
    WORKING_DIRECTORY "${RUST_ROOT}"
    VERBATIM)
add_custom_target(plusgram_text_build DEPENDS "${RUST_ARCHIVE}")
add_library(plusgram_text_core STATIC IMPORTED)
set_target_properties(plusgram_text_core PROPERTIES IMPORTED_LOCATION "${RUST_ARCHIVE}")
add_dependencies(plusgram_text_core plusgram_text_build)
# Keep this Rust runtime separate from tlottie and the wallet's prebuilt Rust archives.
add_library(plusgram_text SHARED "${CMAKE_CURRENT_LIST_DIR}/bridge.cpp")
add_dependencies(plusgram_text plusgram_text_build)
target_link_libraries(plusgram_text PRIVATE plusgram_text_core dl m)
target_link_options(plusgram_text PRIVATE
    "-Wl,-z,max-page-size=16384"
    "-Wl,-z,common-page-size=16384"
    "-Wl,--version-script=${CMAKE_CURRENT_LIST_DIR}/exports.map")
set_property(TARGET plusgram_text APPEND PROPERTY LINK_DEPENDS "${CMAKE_CURRENT_LIST_DIR}/exports.map")
