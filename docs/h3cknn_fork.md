# h3cknn's Magisk fork

![h3cknn's Magisk icon](images/h3cknn_magisk_icon.png)

This is a GitHub fork of `topjohnwu/Magisk`, not a standalone copy. The upstream remote and fork
relationship are retained so upstream security and compatibility work can be merged deliberately.

## Fork additions

- App branding: **h3cknn's Magisk**.
- Material 3-inspired shield/spark launcher and splash icon.
- A root-independent Modules center backed by the public
  `Magisk-Modules-Alt-Repo/json` index.
- Search and source links for catalog entries.
- Root-gated module installation. Browsing is available without root, while selecting Install on
  an unrooted device shows: `u need root for installing modules`.

## Compatibility target

The fork keeps the upstream native patching and root implementation and inherits its device support.
The Android app build targets API 37 and retains the upstream minimum SDK 23, so Android 13 through
Android 17 are within the declared build target. Device-specific boot image support still depends on
the device's boot layout and upstream Magisk compatibility.

## Licensing and attribution

All upstream files and the GPL-3.0 license remain in this repository. Fork-specific files and
changes are identifiable in the commit history and this document.
