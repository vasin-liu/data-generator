import { yaml } from '@codemirror/lang-yaml';
import CodeMirror from '@uiw/react-codemirror';
import { useMemo } from 'react';

type Props = {
  value: string;
  readOnly?: boolean;
  onChange?: (value: string) => void;
};

/**
 * YAML editor with syntax highlighting.
 */
export function YamlEditor({ value, readOnly, onChange }: Props) {
  const extensions = useMemo(() => [yaml()], []);

  return (
    <CodeMirror
      value={value}
      height="420px"
      extensions={extensions}
      editable={!readOnly}
      onChange={(v) => onChange?.(v)}
      basicSetup={{
        lineNumbers: true,
        foldGutter: true,
      }}
    />
  );
}
