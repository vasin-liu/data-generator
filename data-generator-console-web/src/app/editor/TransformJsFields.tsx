import { Alert, Form, Input, InputNumber, Typography } from 'antd';
import { useTranslation } from 'react-i18next';
import { FieldHelp } from '../../components/FieldHelp';
import {
  JS_TRANSFORM_DEFAULT_TIMEOUT_MS,
  JS_TRANSFORM_MAX_SCRIPT_BYTES,
  jsScriptByteLength,
} from './transformLimits';

type Props = {
  script: string;
  timeoutMs?: number;
  readOnly: boolean;
  onScriptChange: (script: string) => void;
  onTimeoutChange: (timeoutMs: number | undefined) => void;
};

/**
 * Sandboxed JavaScript transform fields (script body, optional timeout, limit hints).
 */
export function TransformJsFields({
  script,
  timeoutMs,
  readOnly,
  onScriptChange,
  onTimeoutChange,
}: Props) {
  const { t } = useTranslation();
  const scriptBytes = jsScriptByteLength(script);
  const overLimit = scriptBytes > JS_TRANSFORM_MAX_SCRIPT_BYTES;

  return (
    <>
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 12 }}
        message={t('transform.js.limits.title')}
        description={t('transform.js.limits.body', {
          maxBytes: JS_TRANSFORM_MAX_SCRIPT_BYTES,
          defaultTimeoutMs: JS_TRANSFORM_DEFAULT_TIMEOUT_MS,
        })}
      />
      <Form.Item label={<FieldHelp label={t('transform.js.script')} help={t('transform.js.script.help')} />}>
        <Input.TextArea
          data-testid="transform-js-script"
          rows={6}
          readOnly={readOnly}
          value={script}
          placeholder={t('transform.js.scriptPlaceholder')}
          onChange={(e) => onScriptChange(e.target.value)}
        />
        <Typography.Text type={overLimit ? 'danger' : 'secondary'} style={{ fontSize: 12 }}>
          {t('transform.js.scriptBytes', { bytes: scriptBytes, maxBytes: JS_TRANSFORM_MAX_SCRIPT_BYTES })}
        </Typography.Text>
      </Form.Item>
      <Form.Item label={<FieldHelp label={t('transform.js.timeoutMs')} help={t('transform.js.timeoutMs.help')} />}>
        <InputNumber
          data-testid="transform-js-timeout"
          disabled={readOnly}
          min={1}
          style={{ width: 160 }}
          placeholder={String(JS_TRANSFORM_DEFAULT_TIMEOUT_MS)}
          value={timeoutMs}
          onChange={(value) => onTimeoutChange(typeof value === 'number' ? value : undefined)}
        />
      </Form.Item>
    </>
  );
}
