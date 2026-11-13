# Scripting with ModJS

BarelyPowered adds a global `BarelyPowered` to every ModJS script when ModJS is installed. ModJS is optional: without it nothing is loaded and nothing changes. A script that throws is logged as one line and the game carries on.

## Events

`BarelyPowered.on(name, fn)` listens to an event on the server and returns `false` for an unknown name. `BarelyPowered.events()` lists the names.

| Event | When |
|---|---|
| `generatorStarted` | A generator begins producing. |
| `generatorStopped` | A generator stops producing. |
| `fuelBurned` | A combustion generator takes an item to burn, or a lava generator takes a bucket. |

The listener gets one object with `name`, `kind` (such as `combustion_generator`), `tier` (1 to 3), `id` (the block id), `dimension`, `x`, `y`, `z`, `output` (FE per tick), `note` (why the output is what it is, such as `burning`, `no_fuel`, `night`) and `fuel` (`coal`, `charcoal`, `wood`, `planks` or `lava` for `fuelBurned`, otherwise empty).

## Reading state (server scripts)

| Call | Returns |
|---|---|
| `generatorAt(dimension, x, y, z)` | `null` or an object with `kind`, `tier`, `id`, `output`, `stored`, `capacity`, `lit`, `note`, `fuel` (item id), `fuelCount`, `lavaMb`, `lavaCapacityMb`, `burnTicksLeft`. |
| `batteryAt(dimension, x, y, z)` | `null` or an object with `tier`, `id`, `stored`, `capacity`, `rate`. |
| `kinds()` | The generator kinds. |

Reads return `null` for an unloaded position, a block of another kind, or before any generator has ticked in the world. They never load chunks. Dimensions are ids such as `minecraft:overworld`.

## Settings

| Call | Does |
|---|---|
| `settingKeys()` | Every key of `config/barelypowered.properties`. |
| `setting(key)` | The current value as text, or `null`. |
| `settingNumber(key)` | The current value as a number. |
| `setSetting(key, value)` | Changes a value for this session (the file is not written). Returns `false` for an unknown key. Use it from a startup script. |

## Example

Server script:

```js
BarelyPowered.on('generatorStopped', e => {
  if (e.note == 'no_fuel') console.log('Out of fuel at ' + e.dimension + ' ' + e.x + ' ' + e.y + ' ' + e.z)
})

BarelyPowered.on('fuelBurned', e => console.log(e.fuel + ' lit in a tier ' + e.tier + ' ' + e.kind))
```

Startup script:

```js
BarelyPowered.setSetting('combustion.planks.burn_ticks', 100)
BarelyPowered.setSetting('battery.iron.capacity', 600000)
```
