import { QuestionCircleOutlined } from '@ant-design/icons';
import { Tooltip } from 'antd';
import type { ReactNode } from 'react';

type Props = {
  label: ReactNode;
  help: ReactNode;
  required?: boolean;
};

/**
 * Form label with a ? tooltip for field-level guidance.
 */
export function FieldHelp({ label, help, required }: Props) {
  return (
    <span>
      {label}
      {required ? <span style={{ color: '#ff4d4f', marginLeft: 4 }}>*</span> : null}
      <Tooltip title={help}>
        <QuestionCircleOutlined style={{ marginLeft: 6, color: 'rgba(0,0,0,0.45)', cursor: 'help' }} />
      </Tooltip>
    </span>
  );
}
