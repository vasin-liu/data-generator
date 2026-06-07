/** Mirrors {@code JsTransformVO.MAX_SCRIPT_BYTES}. */
export const JS_TRANSFORM_MAX_SCRIPT_BYTES = 65_536;

/** Mirrors {@code JsTransformVO.DEFAULT_TIMEOUT_MS}. */
export const JS_TRANSFORM_DEFAULT_TIMEOUT_MS = 5_000;

/**
 * @param script JavaScript source text
 */
export function jsScriptByteLength(script: string): number {
  return new TextEncoder().encode(script).length;
}
