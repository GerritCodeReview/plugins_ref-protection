load("//tools/bzl:junit.bzl", "junit_tests")
load(
    "//tools/bzl:plugin.bzl",
    "gerrit_plugin",
    "PLUGIN_DEPS",
    "PLUGIN_TEST_DEPS",
)

gerrit_plugin(
    name = "ref-protection",
    srcs = glob(["src/main/java/**/*.java"]),
    resources = glob(["src/main/resources/**/*"]),
    manifest_entries = [
        "Implementation-Title: Ref Protection plugin",
        "Implementation-URL: http://gerrit.googlesource.com/plugins/ref-protection",
        "Gerrit-PluginName: ref-protection",
        "Gerrit-Module: com.googlesource.gerrit.plugins.refprotection.RefProtectionModule",
    ],
)

junit_tests(
    name = "ref-protection_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    tags = ["ref-protection"],
    deps = PLUGIN_DEPS + PLUGIN_TEST_DEPS + [
        ":ref-protection__plugin",
    ],
)
