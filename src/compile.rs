//! compiling the generated C, one compiler per core, into a shared library
//! a host loads or a static one a program links in.

use std::path::{Path, PathBuf};
use std::process::{Command, Stdio};
use std::sync::Mutex;
use std::sync::atomic::{AtomicBool, AtomicUsize, Ordering};

/// floating point has to round exactly the way the interpreter does, so
/// nothing may be fused into a multiply-add.
const FLAGS: &[&str] = &["-O2", "-fPIC", "-fvisibility=hidden", "-ffp-contract=off", "-fno-math-errno", "-w"];

/// what progress hears after each file, how many are done and of how
/// many, answering whether to go on.
pub type Progress<'a> = &'a (dyn Fn(usize, usize) -> bool + Sync);

/// the C compiler, from CC or else cc.
fn compiler() -> String {
    std::env::var("CC").unwrap_or_else(|_| "cc".to_owned())
}

/// whether there is a C compiler to build with.
pub fn check() -> Result<(), String> {
    let compiler = compiler();
    match Command::new(&compiler).arg("--version").stdout(Stdio::null()).stderr(Stdio::null()).status() {
        Ok(status) if status.success() => Ok(()),
        _ => Err(format!("there is no C compiler ({compiler}), install one such as gcc or clang, or name it in CC")),
    }
}

/// compiles sources, file names inside dir, and links them into library,
/// telling progress how many of them are done after each one.
pub fn compile(dir: &Path, sources: &[String], library: &Path, progress: Progress) -> Result<(), String> {
    shared(&objects(dir, sources, progress)?, library)
}

/// compiles sources, file names inside dir, each into an object beside it,
/// stopping when progress says so.
pub fn objects(dir: &Path, sources: &[String], progress: Progress) -> Result<Vec<PathBuf>, String> {
    let compiler = compiler();
    let jobs = std::thread::available_parallelism().map_or(4, |n| n.get());
    let next = AtomicUsize::new(0);
    let done = AtomicUsize::new(0);
    let stopped = AtomicBool::new(false);
    let failures = Mutex::new(Vec::new());
    std::thread::scope(|scope| {
        for _ in 0..jobs {
            scope.spawn(|| {
                while let Some(source) = sources.get(next.fetch_add(1, Ordering::Relaxed)) {
                    if stopped.load(Ordering::Relaxed) {
                        break;
                    }
                    let path = dir.join(source);
                    let status = Command::new(&compiler)
                        .args(FLAGS)
                        .arg("-I")
                        .arg(dir)
                        .arg("-c")
                        .arg(&path)
                        .arg("-o")
                        .arg(path.with_extension("o"))
                        .status();
                    if !status.is_ok_and(|s| s.success()) {
                        failures.lock().unwrap().push(source.clone());
                    }
                    if !progress(done.fetch_add(1, Ordering::Relaxed) + 1, sources.len()) {
                        stopped.store(true, Ordering::Relaxed);
                    }
                }
            });
        }
    });
    if stopped.into_inner() {
        return Err("stopped".to_owned());
    }
    let failures = failures.into_inner().unwrap();
    if !failures.is_empty() {
        return Err(format!("{} failed to compile, {}", failures.len(), failures.join(" ")));
    }
    Ok(sources.iter().map(|source| dir.join(source).with_extension("o")).collect())
}

/// links objects into a shared library.
pub fn shared(objects: &[PathBuf], library: &Path) -> Result<(), String> {
    let status = Command::new(compiler()).arg("-shared").arg("-o").arg(library).args(objects).status();
    match status {
        Ok(status) if status.success() => Ok(()),
        _ => Err("linking failed".to_owned()),
    }
}

/// puts objects into a static library.
pub fn archive(objects: &[PathBuf], library: &Path) -> Result<(), String> {
    // ar adds to what is there, which could hold objects no longer built
    if library.exists() {
        std::fs::remove_file(library).map_err(|e| format!("could not replace {}, {e}", library.display()))?;
    }
    let ar = std::env::var("AR").unwrap_or_else(|_| "ar".to_owned());
    let status = Command::new(&ar).arg("rcs").arg(library).args(objects).status();
    match status {
        Ok(status) if status.success() => Ok(()),
        _ => Err("archiving failed".to_owned()),
    }
}
