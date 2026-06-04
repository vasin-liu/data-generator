import type { TFunction } from 'i18next';

/**
 * Builds Select options with human-readable labels while keeping enum values as values.
 */
export function labeledOptions(
  t: TFunction,
  prefix: string,
  values: readonly string[],
): { value: string; label: string }[] {
  return values.map((value) => ({
    value,
    label: t(`${prefix}.${value}`, { defaultValue: value }),
  }));
}
