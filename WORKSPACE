workspace(name = "platform_java")

load("@bazel_tools//tools/build_defs/repo:http.bzl", "http_archive")

# Rules Shell (for commit script)
http_archive(
    name = "rules_shell",
    sha256 = "66f4810757d975607e4e16e45ca46495db95c808fdd9ba0ce6471c6670e17637",
    strip_prefix = "rules_shell-0.1.0",
    url = "https://github.com/bazelbuild/rules_shell/archive/refs/tags/0.1.0.tar.gz",
)

# Rules Java
http_archive(
    name = "rules_java",
    sha256 = "685c3384f932ed057997d9197c3ce1329a28892cc9827471953259648602b662",
    strip_prefix = "rules_java-7.2.0",
    url = "https://github.com/bazelbuild/rules_java/archive/refs/tags/7.2.0.tar.gz",
)

# Rules Docker
http_archive(
    name = "io_bazel_rules_docker",
    sha256 = "b1e09c4889f5bc36c641f37e46634b07f2b1d7f3c4676136bb87d00f6c2453e9",
    strip_prefix = "rules_docker-0.25.0",
    url = "https://github.com/bazelbuild/rules_docker/releases/download/v0.25.0/rules_docker-v0.25.0.tar.gz",
)

load("@io_bazel_rules_docker//java:image.bzl", _java_image_repos = "repositories")
_java_image_repos()
