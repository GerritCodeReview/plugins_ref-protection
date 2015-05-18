include_defs('//bucklets/gerrit_plugin.bucklet')

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

java_test(
  name = 'ref-protection_tests',
  srcs = glob(['src/test/java/**/*.java']),
  deps = [
    ':ref-protection__plugin',
    '//gerrit-common:server',
    '//gerrit-reviewdb:server',
    '//gerrit-server:server',
    '//lib:guava',
    '//lib:gwtorm',
    '//lib:junit',
    '//lib:truth',
    '//lib/jgit:jgit',
  ],
)
