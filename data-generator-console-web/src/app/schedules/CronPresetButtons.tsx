import { Button, Space } from 'antd';
import type { FormInstance } from 'antd/es/form';
import { useTranslation } from 'react-i18next';
import { SCHEDULE_CRON_PRESETS } from './cronPresets';

type Props = {
  form: FormInstance;
};

/**
 * One-click cron presets for the schedule create/edit dialog.
 */
export function CronPresetButtons({ form }: Props) {
  const { t } = useTranslation();

  return (
    <Space wrap size={[4, 4]} style={{ marginTop: 4, marginBottom: 8 }}>
      {SCHEDULE_CRON_PRESETS.map((preset) => (
        <Button
          key={preset.key}
          size="small"
          type="link"
          onClick={() => form.setFieldValue('cronExpression', preset.cron)}
        >
          {t(preset.labelKey)}
        </Button>
      ))}
    </Space>
  );
}
