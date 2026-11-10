//! UTF-16 streaming tag filter. Preserves Java code units, including lone surrogates.
use std::ffi::c_void;

const OPEN: &[u16] = &[60, 116, 104, 105, 110, 107, 62];
const CLOSE: &[u16] = &[60, 47, 116, 104, 105, 110, 107, 62];

#[derive(Debug, Default, PartialEq, Eq)]
pub struct Output {
    pub visible: Vec<u16>,
    pub reasoning: Vec<u16>,
    pub pending: Vec<u16>,
    pub in_reasoning: bool,
    pub signal: bool,
}

pub fn process(input: &[u16], mut in_reasoning: bool) -> Output {
    let mut output = Output::default();
    let mut rest = input;
    while !rest.is_empty() {
        let tag = if in_reasoning { CLOSE } else { OPEN };
        if let Some(index) = rest.windows(tag.len()).position(|window| window == tag) {
            let destination = if in_reasoning {
                &mut output.reasoning
            } else {
                &mut output.visible
            };
            destination.extend_from_slice(&rest[..index]);
            rest = &rest[index + tag.len()..];
            in_reasoning = !in_reasoning;
            output.signal |= in_reasoning;
        } else {
            let keep = (1..tag.len().min(rest.len() + 1))
                .rev()
                .find(|&size| rest.ends_with(&tag[..size]))
                .unwrap_or(0);
            let destination = if in_reasoning {
                &mut output.reasoning
            } else {
                &mut output.visible
            };
            destination.extend_from_slice(&rest[..rest.len() - keep]);
            output.pending.extend_from_slice(&rest[rest.len() - keep..]);
            break;
        }
    }
    output.in_reasoning = in_reasoning;
    output
}

/// Calls the sink synchronously; no Rust-owned allocation survives the call.
/// Returns false after a caught panic; caller must discard any partial result.
///
/// # Safety
/// input must address len valid u16 values (or may be null for len == 0).
/// sink/context must be valid for a synchronous call; sink must not unwind.
#[unsafe(no_mangle)]
pub unsafe extern "C" fn plusgram_text_process(
    input: *const u16,
    len: usize,
    in_reasoning: bool,
    context: *mut c_void,
    sink: unsafe extern "C" fn(*mut c_void, u32, *const u16, usize),
) -> bool {
    if input.is_null() && len != 0 {
        return false;
    }
    std::panic::catch_unwind(|| {
        let input = if len == 0 {
            &[]
        } else {
            // SAFETY: guaranteed by the FFI caller.
            unsafe { std::slice::from_raw_parts(input, len) }
        };
        let output = process(input, in_reasoning);
        let flags = [u16::from(output.in_reasoning), u16::from(output.signal)];
        for (index, value) in [
            output.visible.as_slice(),
            output.reasoning.as_slice(),
            output.pending.as_slice(),
            flags.as_slice(),
        ]
        .iter()
        .enumerate()
        {
            // SAFETY: the callback is synchronous and slices remain alive.
            unsafe { sink(context, index as u32, value.as_ptr(), value.len()) };
        }
    })
    .is_ok()
}

#[cfg(test)]
mod tests {
    use super::*;
    fn utf16(s: &str) -> Vec<u16> {
        s.encode_utf16().collect()
    }

    #[test]
    fn plain_and_multiple_blocks() {
        let result = process(&utf16("a<think>b</think>c<think>d</think>e"), false);
        assert_eq!(result.visible, utf16("ace"));
        assert_eq!(result.reasoning, utf16("bd"));
        assert!(result.signal);
        assert!(!result.in_reasoning);
        assert!(result.pending.is_empty());
    }

    #[test]
    fn every_chunk_boundary_including_utf16_surrogates() {
        let input = utf16("Привет 🦀<think>世界😀</think>done");
        let reference = process(&input, false);
        for boundary in 0..=input.len() {
            let first = process(&input[..boundary], false);
            let mut next = first.pending.clone();
            next.extend_from_slice(&input[boundary..]);
            let second = process(&next, first.in_reasoning);
            let mut visible = first.visible;
            visible.extend(second.visible);
            let mut reasoning = first.reasoning;
            reasoning.extend(second.reasoning);
            assert_eq!(visible, reference.visible, "boundary {boundary}");
            assert_eq!(reasoning, reference.reasoning, "boundary {boundary}");
            assert_eq!(second.pending, reference.pending);
            assert_eq!(second.in_reasoning, reference.in_reasoning);
            assert_eq!(first.signal || second.signal, reference.signal);
        }
    }

    #[test]
    fn incomplete_and_nested_tags_match_java_contract() {
        let result = process(&utf16("abc<thi"), false);
        assert_eq!(result.visible, utf16("abc"));
        assert_eq!(result.pending, utf16("<thi"));
        let result = process(&utf16("<think>x</thi"), false);
        assert_eq!(result.reasoning, utf16("x"));
        assert_eq!(result.pending, utf16("</thi"));
        assert!(result.in_reasoning);
        let result = process(&utf16("<think>a<think>b</think>c"), false);
        assert_eq!(result.reasoning, utf16("a<think>b"));
        assert_eq!(result.visible, utf16("c"));
    }

    #[test]
    fn preserves_unpaired_surrogates_and_empty_mode() {
        assert_eq!(
            process(&[0xd800, 0, 0xdc00], false).visible,
            [0xd800, 0, 0xdc00]
        );
        assert!(process(&[], true).in_reasoning);
        assert!(!process(&[], true).signal);
    }
}
