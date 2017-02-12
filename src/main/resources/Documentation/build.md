Build
=====

This plugin is built with Bazel.

Clone (or link) this plugin to the `plugins` directory of Gerrit's source tree.

Then issue

```
  bazel build plugins/ref-protection
```

in the root of Gerrit's source tree to build

The output is created in

```
  bazel-genfiles/plugins/ref-protection/ref-protection.jar
```

This project can be imported into the Eclipse IDE.
Add the plugin name to the `CUSTOM_PLUGINS` set in
Gerrit core in `tools/bzl/plugins.bzl`, and execute:

```
  ./tools/eclipse/project.py
```

To execute the tests run:

```
  bazel test plugins/ref-protection:ref-protection_tests
```

How to build the Gerrit Plugin API is described in the [Gerrit
documentation](../../../Documentation/dev-bazel.html#_extension_and_plugin_api_jar_files).
