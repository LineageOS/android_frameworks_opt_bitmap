package(
    default_visibility = [
        "//java/com/google/android/apps/bigtop:__subpackages__",
    ],
)

licenses(["notice"])  # Apache License 2.0

exports_files(["LICENSE"])

android_manifest_merge(
    name = "merged_manifest",
    srcs = ["AndroidManifest.xml"],
    deps = [
        "//third_party/java_src/android_libs/bitmap_library:AndroidManifest.xml",
    ],
)

filegroup(
    name = "resource_files",
    srcs = glob(
        ["res/**"],
        exclude_directories = 1,
    ),
)

android_resources(
    name = "resources",
    custom_package = "com.example.bitmapsample",
    inline_constants = 0,
    manifest = ":merged_manifest",
    resources = [
        ":resource_files",
        "//third_party/java_src/android_libs/bitmap_library:resource_files",
    ],
    resources_dir = "res",
    visibility = ["//visibility:private"],
)

exports_files([
    "proguard-config.pro",
])

android_binary(
    name = "bitmap_sample",
    srcs = glob(["src/**/*.java"]) + [":resources"],
    proguard_specs = [
        ":proguard-config.pro",
    ],
    resources = ":resources",
    deps = [
        "//third_party/java_src/android_libs/bitmap_library",
    ],
)
