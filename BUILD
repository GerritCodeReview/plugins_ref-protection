load(
    "@com_googlesource_gerrit_bazlets//:gerrit_plugin.bzl",
    "gerrit_plugin",
    "gerrit_plugin_tests",
)

gerrit_plugin(
    srcs = glob(["src/main/java/**/*.java"]),
    manifest_entries = [
        "Implementation-Title: Ref Protection plugin",
        "Implementation-URL: http://gerrit.googlesource.com/plugins/ref-protection",
        "Gerrit-PluginName: ref-protection",
        "Gerrit-Module: com.googlesource.gerrit.plugins.refprotection.RefProtectionModule",
    ],
    plugin = "ref-protection",
    resources = glob(["src/main/resources/**/*"]),
)

gerrit_plugin_tests(
    name = "ref_protection_tests",
    srcs = glob(["src/test/java/**/*.java"]),
    plugin = "ref-protection",
)
