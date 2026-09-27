# 3dsrecomp

This is a static recompiler for Nintendo 3DS games, made to run with [Zakuro](https://github.com/fearkov/zakuro). It's a WIP.

It reads a game's code, finds the functions in it and turns them into C, which compiles into a library the emulator loads. Anything it can't handle or didn't find still runs in Zakuro's interpreter, so a game doesn't have to be fully recompiled to work.

So far it has been tested with a few games like Pokémon Alpha Sapphire and Pokémon Y, with more than 99% of the instructions it runs coming from the library. With Zakuro now drawing on the GPU, the game runs about twice as fast as it did on the interpreter.

## Features

- finding the code in the main executable and in the CRO modules, from the entry point, calls, exports, relocations, pointers in data and what the modules import from each other, then looking for where functions begin in whatever is left
- generating C for ARM, Thumb and VFP code, with the less common instructions going through the interpreter
- modules get code that works wherever the game loads them
- checking every recompiled function against Zakuro's interpreter, running both from the same state and comparing registers, flags and memory
- running it in Zakuro, which finds the library build installs on its own
- replacing any function with one written by hand in C, for fixes, mods or faster versions, see [docs/overrides.md](docs/overrides.md)
- making a game into a program of its own, with the code linked into Zakuro instead of loaded by it, see [docs/port.md](docs/port.md)

## How to use

You need Rust and a C compiler. I've only tested on Linux so far.

```
cargo build --release
./target/release/3dsrecomp analyze game.3ds
./target/release/3dsrecomp build game.3ds
```

analyze - shows how much of the code was found; 
build - writes the C, compiles it and installs the library where Zakuro finds it. Give it a folder (build game.3ds out) to keep everything there instead. 

verify runs the recompiled functions against Zakuro's interpreter, which cargo fetches when it's built with the verify feature:

```
cargo build --release --features verify
./target/release/3dsrecomp verify game.3ds ~/.local/share/3dsrecomp/<title id>.so
```

The build takes a while, so don't worry. After that, Zakuro runs the game on the recompiled code on its own:

```
zakuro game.3ds
```

Or make the game a program of its own, which still needs the game to run:

```
./target/release/3dsrecomp port game.3ds mygame
cd mygame && cargo build --release
./target/release/<name> game.3ds
```

build and port both take --overrides with a C file or a folder of them, for functions written by hand.

## Notes

This repository doesn't contain any game code. You need your own dump of a game you own, and since the generated C, libraries and programs come from the game, don't share them.

I do not condone piracy, and I will not help you with that. So, don't ask me about that.

Contributions are welcome. Using AI is fine sometimes, but the code must always be reviewed by a human. Code that is entirely vibecoded will be discarded.


