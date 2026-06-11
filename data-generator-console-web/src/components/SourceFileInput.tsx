import { UploadOutlined } from '@ant-design/icons';
import { Button, Input, Radio, Space, Upload, message } from 'antd';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { uploadInlineSource, uploadSourceFile } from '../api/uploads';

type Mode = 'path' | 'upload' | 'paste';

type Props = {
  path: string;
  readOnly: boolean;
  allowPaste?: boolean;
  accept?: string;
  defaultPasteName?: string;
  onPathChange: (path: string) => void;
};

/**
 * Path field with optional file upload and inline paste (writes server-side file).
 */
export function SourceFileInput({
  path,
  readOnly,
  allowPaste = false,
  accept,
  defaultPasteName = 'data.txt',
  onPathChange,
}: Props) {
  const { t } = useTranslation();
  const [mode, setMode] = useState<Mode>('path');
  const [paste, setPaste] = useState('');
  const [uploading, setUploading] = useState(false);

  const handleUpload = async (file: File) => {
    setUploading(true);
    try {
      const saved = await uploadSourceFile(file);
      onPathChange(saved);
      message.success(t('source.file.uploaded'));
    } catch (err) {
      message.error((err as Error).message);
    } finally {
      setUploading(false);
    }
    return false;
  };

  const applyPaste = async () => {
    if (!paste.trim()) {
      return;
    }
    setUploading(true);
    try {
      const saved = await uploadInlineSource(defaultPasteName, paste);
      onPathChange(saved);
      message.success(t('source.file.pasted'));
    } catch (err) {
      message.error((err as Error).message);
    } finally {
      setUploading(false);
    }
  };

  return (
    <Space direction="vertical" style={{ width: '100%' }} size="small">
      {allowPaste ? (
        <Radio.Group
          disabled={readOnly}
          value={mode}
          onChange={(e) => setMode(e.target.value as Mode)}
          optionType="button"
          buttonStyle="solid"
          options={[
            { label: t('source.file.mode.path'), value: 'path' },
            { label: t('source.file.mode.upload'), value: 'upload' },
            { label: t('source.file.mode.paste'), value: 'paste' },
          ]}
        />
      ) : null}
      {(mode === 'path' || !allowPaste) && (
        <Input
          readOnly={readOnly}
          value={path}
          placeholder={t('source.path.placeholder')}
          onChange={(e) => onPathChange(e.target.value)}
        />
      )}
      {allowPaste && mode === 'upload' && (
        <Upload accept={accept} maxCount={1} showUploadList={false} beforeUpload={handleUpload}>
          <Button icon={<UploadOutlined />} loading={uploading} disabled={readOnly}>
            {t('source.file.upload')}
          </Button>
        </Upload>
      )}
      {allowPaste && mode === 'paste' && (
        <>
          <Input.TextArea
            rows={6}
            readOnly={readOnly}
            value={paste}
            placeholder={t('source.file.pastePlaceholder')}
            onChange={(e) => setPaste(e.target.value)}
          />
          <Button disabled={readOnly} loading={uploading} onClick={applyPaste}>
            {t('source.file.applyPaste')}
          </Button>
        </>
      )}
      {!allowPaste && (
        <Upload accept={accept} maxCount={1} showUploadList={false} beforeUpload={handleUpload}>
          <Button icon={<UploadOutlined />} loading={uploading} disabled={readOnly}>
            {t('source.file.upload')}
          </Button>
        </Upload>
      )}
    </Space>
  );
}
