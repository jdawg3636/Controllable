# SDL2 native provenance

`controllable-sdl-2.32.10-1.1.0.jar` is the unmodified upstream release:
https://github.com/MrCrayfish/ControllableSDL/releases/tag/v2.32.10-1.1.0

SHA-256: `127afb5612470e035b2b61ccb4c7119e7c446500a3036d0365118bbb25717bdc`

Only its eight platform native libraries are packaged. Its Java 17 classes are
excluded: the backport uses a small Java 7 JNA binding to the same SDL2 ABI.
The upstream workflow records SDL source revisions and cross-platform builds:
https://github.com/MrCrayfish/ControllableSDL/blob/v2.32.10-1.1.0/.github/workflows/build.yml

This file is vendored so users can build without GitHub Packages credentials.
`verifyVendor` checks the archive before resource extraction. SDL2 and the
controller mapping database retain their zlib licenses. JNA 5.13.0 is bundled
under its Apache 2.0 license, with its original package names/native resources.
