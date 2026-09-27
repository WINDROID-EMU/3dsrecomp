//! recompiling a title from end to end, writing the C, compiling it and
//! installing the library, for the command line and for programs that run
//! it themselves and follow it through its events.

use std::path::{Path, PathBuf};
use std::sync::atomic::{AtomicBool, Ordering};
use std::time::{Duration, Instant};

use crate::discover::{self, Analysis, Mode, Source};
use crate::overrides::{self, Override};
use crate::rom::Title;
use crate::{abi, codegen, compile};

/// what happens along the way.
#[derive(Debug, Clone, PartialEq)]
pub enum Event {
    /// the C is written.
    Generated { files: usize, bytes: usize, overrides: usize },
    /// something worth knowing that does not stop it.
    Note(String),
    Compiled { done: usize, total: usize },
    /// the library is linked.
    Built { library: PathBuf, took: Duration },
    Installed(PathBuf),
}

/// what a build is to do.
#[derive(Default)]
pub struct Options<'a> {
    /// where to work, the cache when none, and then the library gets
    /// installed where hosts find it.
    pub dir: Option<&'a Path>,
    /// C files of functions written by hand, see docs/overrides.md.
    pub overrides: Option<&'a Path>,
    /// set from elsewhere to stop it.
    pub cancel: Option<&'a AtomicBool>,
}

/// the title's code written as C.
pub struct Generated {
    pub title: Title,
    pub dir: PathBuf,
    /// the files to compile, names inside dir.
    pub sources: Vec<String>,
}

/// writes the title at rom as C in the dir pick chooses, the overrides with
/// it.
pub fn generate(
    rom: &Path,
    pick: impl FnOnce(&Title) -> PathBuf,
    overrides: Option<&Path>,
    events: &(dyn Fn(Event) + Sync),
) -> Result<Generated, String> {
    let title = Title::load(rom).map_err(|error| format!("could not load {}, {error}", rom.display()))?;
    let mut programs = crate::programs(&title).map_err(|error| format!("could not read the code, {error}"))?;
    let dir = pick(&title);
    let files = match overrides {
        Some(path) => overrides::load(path)?,
        None => Vec::new(),
    };
    let replaced: Vec<Override> = files.iter().flat_map(|file| file.overrides.iter().cloned()).collect();
    // whatever an override replaces has to be generated too, for it to call
    for item in &replaced {
        let name = item.module.as_deref().unwrap_or("executable");
        let Some((_, program)) = programs.iter_mut().find(|(program, _)| program == name) else {
            return Err(format!("the title has no module called {name}"));
        };
        program.seeds.insert(0, (item.address, Source::Override));
    }

    let analyses: Vec<Analysis> = programs.iter().map(|(_, program)| discover::analyze(program)).collect();
    for item in &replaced {
        let index = programs.iter().position(|(name, _)| Some(name.as_str()) == item.module.as_deref()).unwrap_or(0);
        let thumb = item.address & 1 != 0;
        let found = analyses[index]
            .functions
            .values()
            .any(|f| (f.mode == Mode::Thumb) == thumb && f.labels.contains(&(item.address & !1)));
        if !found {
            events(Event::Note(format!(
                "no code at 0x{:08X} for {} to replace, so it cannot call the original",
                item.address, item.name
            )));
        }
    }
    let units: Vec<codegen::Unit> = programs
        .iter()
        .zip(&analyses)
        .enumerate()
        .map(|(i, ((name, program), analysis))| codegen::Unit { module: (i > 0).then_some(name.as_str()), program, analysis })
        .collect();
    let generated = codegen::generate(&units, &replaced);

    let write = |path: &Path, contents: &[u8]| {
        std::fs::write(path, contents).map_err(|error| format!("could not write {}, {error}", path.display()))
    };
    let folder = if files.is_empty() { dir.clone() } else { dir.join("hand") };
    std::fs::create_dir_all(&folder).map_err(|error| format!("could not create {}, {error}", folder.display()))?;
    for (name, contents) in &generated {
        write(&dir.join(name), contents.as_bytes())?;
    }
    let mut sources: Vec<String> = generated.iter().map(|(name, _)| name.clone()).filter(|name| name.ends_with(".c")).collect();
    for file in &files {
        let name = format!("hand/{}", file.path.file_name().unwrap_or_default().to_string_lossy());
        write(&dir.join(&name), file.source.as_bytes())?;
        sources.push(name);
    }
    let bytes = generated.iter().map(|(_, contents)| contents.len()).sum();
    events(Event::Generated { files: generated.len(), bytes, overrides: replaced.len() });
    Ok(Generated { title, dir, sources })
}

/// recompiles the title at rom into a library and returns where it ended
/// up, installed when the options name no dir.
pub fn build(rom: &Path, options: &Options, events: &(dyn Fn(Event) + Sync)) -> Result<PathBuf, String> {
    compile::check()?;
    let stopped = || options.cancel.is_some_and(|cancel| cancel.load(Ordering::Relaxed));
    let pick = |title: &Title| options.dir.map(Path::to_owned).unwrap_or_else(|| cache_dir(title.program_id()));
    let generated = generate(rom, pick, options.overrides, events)?;
    if stopped() {
        return Err("stopped".to_owned());
    }
    let program_id = generated.title.program_id();
    let library = generated.dir.join(abi::library_name(program_id));
    let start = Instant::now();
    let progress = |done, total| {
        events(Event::Compiled { done, total });
        !stopped()
    };
    compile::compile(&generated.dir, &generated.sources, &library, &progress)?;
    events(Event::Built { library: library.clone(), took: start.elapsed() });
    if options.dir.is_some() {
        return Ok(library);
    }
    let installed = install(&library, program_id)?;
    events(Event::Installed(installed.clone()));
    Ok(installed)
}

/// where build works on a title whose library it installs.
pub fn cache_dir(program_id: u64) -> PathBuf {
    let var = |name: &str| std::env::var_os(name).filter(|value| !value.is_empty()).map(PathBuf::from);
    let base = if cfg!(windows) {
        var("LOCALAPPDATA")
    } else if cfg!(target_os = "macos") {
        var("HOME").map(|home| home.join("Library/Caches"))
    } else {
        var("XDG_CACHE_HOME").or_else(|| var("HOME").map(|home| home.join(".cache")))
    };
    base.unwrap_or_else(std::env::temp_dir).join("3dsrecomp").join(format!("{program_id:016X}"))
}

/// copies the library to where hosts look for it, under another name first
/// so that a host running the old one keeps it.
pub fn install(library: &Path, program_id: u64) -> Result<PathBuf, String> {
    let dir = abi::library_dir().ok_or("there is no place to install the library")?;
    let target = dir.join(abi::library_name(program_id));
    let partial = target.with_extension(format!("{}.new", std::env::consts::DLL_EXTENSION));
    std::fs::create_dir_all(&dir)
        .and_then(|()| std::fs::copy(library, &partial))
        .and_then(|_| std::fs::rename(&partial, &target))
        .map_err(|error| format!("could not install {}, {error}", target.display()))?;
    Ok(target)
}
