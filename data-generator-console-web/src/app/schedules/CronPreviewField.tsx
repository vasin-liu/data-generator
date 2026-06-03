import { Form, Typography } from 'antd';
import type { FormInstance } from 'antd/es/form';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { previewScheduleCron } from '../../api/schedules';

type Props = {
  /** Parent schedule form; only `cronExpression` is watched. */
  form: FormInstance;
};

/**
 * Live next-run preview for the schedule cron field (debounced server parse).
 */
export function CronPreviewField({ form }: Props) {
  const { t } = useTranslation();
  const cronExpression = Form.useWatch('cronExpression', form);
  const [preview, setPreview] = useState<{ ok: true; next: string } | { ok: false; error: string } | null>(
    null,
  );
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const trimmed = cronExpression?.trim() ?? '';
    if (!trimmed) {
      setPreview(null);
      return;
    }
    setLoading(true);
    const handle = window.setTimeout(() => {
      previewScheduleCron(trimmed)
        .then((next) => setPreview({ ok: true, next }))
        .catch((err: Error) => setPreview({ ok: false, error: err.message }))
        .finally(() => setLoading(false));
    }, 400);
    return () => window.clearTimeout(handle);
  }, [cronExpression]);

  if (!cronExpression?.trim()) {
    return null;
  }

  if (loading && !preview) {
    return (
      <Typography.Text type="secondary" style={{ display: 'block', marginTop: 4 }}>
        {t('schedules.form.cronPreview.loading')}
      </Typography.Text>
    );
  }

  if (!preview) {
    return null;
  }

  if (!preview.ok) {
    return (
      <Typography.Text type="danger" style={{ display: 'block', marginTop: 4 }}>
        {t('schedules.form.cronPreview.invalid', { message: preview.error })}
      </Typography.Text>
    );
  }

  return (
    <Typography.Text type="success" style={{ display: 'block', marginTop: 4 }}>
      {t('schedules.form.cronPreview.next', { time: new Date(preview.next).toLocaleString() })}
    </Typography.Text>
  );
}
