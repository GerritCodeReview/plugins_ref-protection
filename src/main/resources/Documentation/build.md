Build
=====

This plugin is built with Buck.

Two build modes are supported: Standalone and in Gerrit tree. Standalone
build mode is recommended, as this mode doesn't require local Gerrit
tree to exist.

Build standalone
----------------

Clone bucklets library:

```
  git clone https://gerrit.googlesource.com/bucklets

```
and link it to ref-protection directory:

```
  cd ref-protection && ln -s ../bucklets .
```

Add link to the .buckversion file:

```
  cd ref-protection && ln -s bucklets/buckversion .buckversion
```

Add link to the .watchmanconfig file:

```
  cd ref-protection && ln -s bucklets/watchmanconfig .watchmanconfig
```

To build the plugin, issue the following command:

```
  buck build plugin
```

The output is created in

```
  buck-out/gen/ref-protection.jar
```

To run unit tests, issue the following command:

```
  buck test
```

Build in Gerrit tree
--------------------

Clone or link this plugin to the plugins directory of Gerrit's source
tree, and issue the command:

```
  buck build plugins/ref-protection
```

The output is created in

```
  buck-out/gen/plugins/ref-protection/ref-protection.jar
```

This project can be imported into the Eclipse IDE:

```
  ./tools/eclipse/project.py
```

To run the unit tests and restrict to run only the tests from this plugin:

```
  buck test --include ref-protection
```

How to build the Gerrit Plugin API is described in the [Gerrit
documentation](../../../Documentation/dev-buck.html#_extension_and_plugin_api_jar_files).
