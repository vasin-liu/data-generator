import { Form, Input, Select, Typography } from 'antd';
import type { FormInstance } from 'antd/es/form';
import { useEffect, useMemo, useState } from 'react';
import { useTranslation } from 'react-i18next';
import {
  JDBC_DRIVER_GROUP_KEYS,
  findJdbcDriverPreset,
  guessPresetId,
  type JdbcDriverPreset,
} from './jdbcDriverPresets';

type FormValues = {
  name: string;
  url: string;
  username: string;
  password: string;
  driverClassName: string;
};

type Props = {
  form: FormInstance<FormValues>;
  /** Resets preset picker when dialog opens for another row */
  dialogKey: string;
  /** Server-provided JDBC driver catalog */
  presets: JdbcDriverPreset[];
  /** Notifies parent when preset selection changes (for upload visibility) */
  onPresetIdChange?: (presetId: string | undefined) => void;
};

/**
 * Preset picker + editable driver class field.
 */
export function DriverPresetFields({ form, dialogKey, presets, onPresetIdChange }: Props) {
  const { t } = useTranslation();
  const [presetId, setPresetId] = useState<string | undefined>();
  const driverClassName = Form.useWatch('driverClassName', form);

  useEffect(() => {
    const guessed = guessPresetId(presets, form.getFieldValue('driverClassName'));
    setPresetId(guessed);
    onPresetIdChange?.(guessed);
  }, [dialogKey, form, presets, onPresetIdChange]);

  useEffect(() => {
    if (!presetId) {
      return;
    }
    const preset = findJdbcDriverPreset(presets, presetId);
    if (preset && preset.driverClassName !== driverClassName) {
      const stillMatches = preset.alternateDriverClassNames.includes(driverClassName ?? '');
      if (!stillMatches) {
        setPresetId(guessPresetId(presets, driverClassName));
      }
    }
  }, [driverClassName, presetId, presets]);

  const groupedOptions = useMemo(() => {
    const known = JDBC_DRIVER_GROUP_KEYS.filter((gk) => presets.some((p) => p.groupKey === gk));
    const knownSet = new Set<string>(known);
    const extra = [...new Set(presets.map((p) => p.groupKey))].filter((gk) => !knownSet.has(gk));
    const groupKeys = [...known, ...extra];
    return groupKeys.map((groupKey) => ({
      label: t(`datasources.driver.group.${groupKey}`, groupKey),
      options: presets
        .filter((p) => p.groupKey === groupKey)
        .map((p) => ({
          value: p.id,
          label: t(p.labelKey),
        })),
    }));
  }, [presets, t]);

  const applyPreset = (preset: JdbcDriverPreset) => {
    const currentUrl = form.getFieldValue('url');
    form.setFieldsValue({
      driverClassName: preset.driverClassName,
      url: currentUrl?.trim() ? currentUrl : preset.urlTemplate,
    });
  };

  const onPresetChange = (id: string) => {
    setPresetId(id);
    onPresetIdChange?.(id);
    const preset = findJdbcDriverPreset(presets, id);
    if (preset) {
      applyPreset(preset);
    }
  };

  const selectedPreset = presetId ? findJdbcDriverPreset(presets, presetId) : undefined;
  const bundledSelected = selectedPreset?.bundled ?? false;

  return (
    <>
      <Form.Item label={t('datasources.dialog.driverPreset')}>
        <Select
          allowClear
          showSearch
          optionFilterProp="label"
          placeholder={t('datasources.dialog.driverPresetPlaceholder')}
          value={presetId}
          options={groupedOptions}
          onChange={onPresetChange}
          onClear={() => {
            setPresetId(undefined);
            onPresetIdChange?.(undefined);
            form.setFieldValue('driverClassName', '');
          }}
        />
        <Typography.Text type="secondary" style={{ display: 'block', marginTop: 4 }}>
          {t('datasources.dialog.driverPresetHint')}
        </Typography.Text>
      </Form.Item>
      <Form.Item
        name="driverClassName"
        label={t('datasources.dialog.driver')}
        rules={[{ required: true }]}
        extra={
          bundledSelected
            ? t('datasources.dialog.driverBundledHint')
            : t('datasources.dialog.driverAlternateHint')
        }
      >
        <Input placeholder={t('datasources.dialog.driverCustomPlaceholder')} />
      </Form.Item>
      {bundledSelected ? (
        <Typography.Text type="success">{t('datasources.dialog.bundledDriverNote')}</Typography.Text>
      ) : null}
    </>
  );
}
