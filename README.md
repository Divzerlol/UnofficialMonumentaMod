This is an unofficial client-side mod for the Monumenta MMO server (www.playmonumenta.com).

## Installation Notes

This mod is currently only available for Fabric. It furthermore
requires [Fabric API](https://www.curseforge.com/minecraft/mc-mods/fabric-api) to run.

To be able to change this mod's settings in game, [Mod Menu](https://www.curseforge.com/minecraft/mc-mods/modmenu)
and [Cloth Config API](https://www.curseforge.com/minecraft/mc-mods/cloth-config) need to be installed. The config
screen can then be accessed as usual via Mod Menu.

## Feature List

### Abilities Display

![Abilties Display](img/abilities-display.png)

Shows current class abilities and your mom, with current stacks/charges if applicable and remaining cooldown. Also works in Darkest
Depths!

To reduce clutter, completely passive abilities are not shown. Most passive abilities with stacks, charges, or a
cooldown are shown however.

The display is highly configurable. Make sure to install Mod Menu and Cloth Config API as noted above to be able to edit
all options. To reorder abilities, open the chat and then drag the abilities around with the mouse. The entire display
can be dragged around by holding ctrl and then dragging.

Big thanks to Randy (mimi_29), Noelle (kindabland), Alyssa (@Alychemist_),
Grape (aGrxpe), Kiocifer, nyarrgh, and Papaya (@Papayaaaaa) for the textures!

Textures are licensed under [CC BY-SA 3.0](https://creativecommons.org/licenses/by-sa/3.0) by their respective authors.

### Effects Display

Shows the active effects from the tab list as a HUD overlay while playing.

Feature made by Ascynx - many thanks!

### Rich Discord Presence

Adds some extra information about Monumenta in Discord's activity status, e.g. which shard the player is on right now.

Feature made by Ascynx.

### Trident Texture Fix

![Tridents](img/tridents.png)

Shows custom trident textures of the resource pack when tridents are held instead of only when in the inventory. Works
for both your own and other players' held tridents. Compatible with OptiFine and CIT Resewn.

Thrown trident projectiles still look like the vanilla trident however, as changing that would require a server-side
mod.

### Custom Helmet Models

![Spinning Helmet](img/hats.png)

Allows resource packs to define models for helmets. Compatible with OptiFine, in particular compatible with its CIT.

To use this feature, download and enable
the [Unofficial Monumenta 3D Hats](https://www.curseforge.com/minecraft/texture-packs/unofficial-monumenta-3d-hats)
resource pack.

### Firmament ping fix

Makes Firmament and Doorway from Eternity usable even at high ping (and may also be an improvement even with low ping):
Using Firmament will place a Prismarine/Blackstone block instead of the Firmament Shulker box itself, and the Firmament
is kept in the hotbar for immediate re-use.

### Minor Features

There's some more minor features, like moving player heads worn by NPCs down to fit with the NPC model of the resource
pack, and location notifications (by Ascynx). All features can be configured via the options menu.

#### Chest Count Overlay

Shows number of collected chests in strikes. Feature made by Ascynx.

The chest counts were "borrowed" from [Vladomeme's Lootcounter mod](https://github.com/Vladomeme/lootcounter/releases).

#### Calculator

Adds a small calculator for easier trading to opened containers in the player market. Feature made by Ascynx.

Visibility of the calculator can be toggled by pressing 'K' (key can be rebound in the options menu).

#### Optionally disable ChestSort

There's options to disable sorting inventories on double right-click. The sorting can be selectively disabled for just
the player inventory and/or the ender chest, or for all inventories as well.

## Technical Documentation

### Creating a helmet model as a resource pack artist

1. Create your 3D model and put it anywhere in your resource pack. Make sure it has a proper `head` transformation
   defined.
2. Create a json file for the helmet you want the model to apply to, looking like this:
   ```json
   {
     "parent": "item/generated",
     "textures": {"layer0": "<icon texture path>"},
     "overrides": [{
        "predicate": {"on_head": 1},
        "model": "<absolute path to 3D model>"
      }]
   }
   ```
3. In the properties file of your helmet that has `type=item`, use `model=` instead of `texture=` and reference the json
   file created in step 2.

## Contribute

[The code is open source](https://github.com/Njol/UnofficialMonumentaMod) and pull requests for new features or fixes
are always welcome!
