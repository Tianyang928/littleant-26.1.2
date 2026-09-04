<p align="center">
  <img src="docs/img/programming_gui.png" alt="title" width="300" />
</p>
<h1>LittleAnt 26.1.2</h1>
<hr>
<p align="center">
    <a href="https://github.com/Tianyang928/littleant-26.1.2/issues">Report Bug</a> 
    ·
    <a href="https://github.com/Tianyang928/littleant-26.1.2/releases">View Release</a>
</p>

This is a Minecraft 26.1.2 Neoforge mod.

**Ant** is the new entity added in this mod. It's able to break blocks, set blocks, craft things, operate inventories, just like players. And the most important thing is that its AI can be programmed in-game by players.

I tried to make the programming GUI as close as possible to [Scratch](https://scratch.mit.edu/).

Press `right-click` to open the Ant Entity's inventory.

Press `Shift + right-click` to open the Ant Entity's programming GUI.

## LittleAnt DSL

The `/antrunjson` command and the in-game program editor also accept a small,
Python-like DSL. It is compiled into the same Scratch module graph; it is not
arbitrary Python and cannot import libraries or access the filesystem.

```python
@tick_start
def main():
    if food_level() < 8:
        say("I need food")
    step_forward(1)
```

The complete, versioned module reference is generated from `ModuleRegistry`:
see [`docs/ai-api.md`](docs/ai-api.md) or [`docs/ai-api.json`](docs/ai-api.json).
Run `./gradlew generateAiApi` after adding or changing a module.


## Future Features

- Add preset of Ant AI using those scratch-like modules.
- Add more containers that the Ant can operate (anvil, enchanting table, smoker, blast furnace, for example).
- Improve the programming GUI's appearance.
- Add carrier that the Ant can use.
- Add evolution system, so that the Ant population can evolve over time.
