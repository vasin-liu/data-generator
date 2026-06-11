import { useQuery } from '@tanstack/react-query';
import { Alert, Button, Modal, Table } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { useTranslation } from 'react-i18next';
import { fetchScenarioCatalog } from '../../api/scenarios';
import type { ScenarioCatalogEntry } from '../../api/types';

type Props = {
  open: boolean;
  onClose: () => void;
  onSelect: (scenarioId: string) => void;
};

/**
 * Modal listing official V2 scenarios for the create-from-scenario wizard.
 */
export function ScenarioCatalogModal({ open, onClose, onSelect }: Props) {
  const { t } = useTranslation();
  const catalogQuery = useQuery({
    queryKey: ['scenario-catalog'],
    queryFn: fetchScenarioCatalog,
    enabled: open,
  });

  const columns: ColumnsType<ScenarioCatalogEntry> = [
    {
      title: t('scenarios.col.family'),
      dataIndex: 'family',
      width: 72,
      render: (family: string) => t(`scenarios.family.${family}`, family),
    },
    {
      title: t('scenarios.col.id'),
      dataIndex: 'scenarioId',
      width: 96,
    },
    {
      title: t('scenarios.col.name'),
      dataIndex: 'name',
    },
    {
      title: t('scenarios.col.catalogRef'),
      dataIndex: 'catalogRef',
      ellipsis: true,
    },
    {
      title: t('scenarios.col.actions'),
      key: 'actions',
      width: 120,
      render: (_value, row) => (
        <Button
          type="link"
          data-testid={`scenario-use-${row.scenarioId}`}
          onClick={() => onSelect(row.scenarioId)}
        >
          {t('scenarios.use')}
        </Button>
      ),
    },
  ];

  return (
    <Modal
      title={t('scenarios.title')}
      open={open}
      onCancel={onClose}
      footer={null}
      width={880}
      destroyOnClose
    >
      <div data-testid="scenario-catalog-modal">
      <Alert
        type="info"
        showIcon
        style={{ marginBottom: 16 }}
        message={t('scenarios.hint.title')}
        description={t('scenarios.hint.body')}
      />
      <Table<ScenarioCatalogEntry>
        rowKey="scenarioId"
        size="small"
        loading={catalogQuery.isLoading}
        dataSource={catalogQuery.data ?? []}
        columns={columns}
        pagination={false}
        locale={{ emptyText: catalogQuery.isError ? t('scenarios.loadError') : t('scenarios.empty') }}
      />
      </div>
    </Modal>
  );
}
