# guest loader for host apk

it provides git clone and pull via git2, to overcome jgit 3.x problem (not support modelscope.cn)

in gradle debug build, it will be compiled and embedded.

in gradle release build, it will be downloaded from github release.

to build and upload to github release

```sh
./0 release
```
