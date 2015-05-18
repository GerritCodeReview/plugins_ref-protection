include_defs('//bucklets/gerrit_plugin.bucklet')

if STANDALONE_MODE:
  TEST_DEPS = GERRIT_PLUGIN_API
else:
  TEST_DEPS = [
    '//gerrit-common:server',
    '//gerrit-reviewdb:server',
    '//gerrit-server:server',
    '//lib/jgit:jgit',
    '//lib:guava',
    '//lib:gwtorm',
   ]

gerrit_plugin(
  name = 'ref-protection',
  srcs = glob(['src/main/java/**/*.java']),
  resources = glob(['src/main/resources/**/*']),
  manifest_entries = [
    'Implementation-Title: Ref Protection plugin',
    'Implementation-URL: http://review-plus.sonyericsson.net/#/admin/projects/gerrit/plugins/ref-protection',
    'Gerrit-PluginName: ref-protection',
    'Gerrit-Module: com.googlesource.gerrit.plugins.refprotection.RefProtectionModule'
  ],
)

java_library(
  name = 'classpath',
  deps = [':ref-protection__plugin'],
)

java_library(
  name = 'classpath',
  deps = [':ref-protection__plugin'],
)

java_test(
  name = 'ref-protection_tests',
  srcs = glob(['src/test/java/**/*.java']),
  labels = ['ref-protection'],
  deps = TEST_DEPS + [
    ':ref-protection__plugin',
    '//lib:junit',
    '//lib:truth',
  ],
)
