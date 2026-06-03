import { Breadcrumb, Space, Typography } from 'antd';
import type { ReactNode } from 'react';
import { Link } from 'react-router-dom';

export type BreadcrumbItem = {
  label: string;
  path?: string;
};

type Props = {
  title: ReactNode;
  subtitle?: ReactNode;
  crumbs?: BreadcrumbItem[];
  extra?: ReactNode;
};

/**
 * Shared page title row with optional breadcrumb trail and action buttons.
 */
export function ConsolePageHeader({ title, subtitle, crumbs, extra }: Props) {
  return (
    <header className="console-page-header">
      {crumbs && crumbs.length > 0 && (
        <Breadcrumb
          style={{ marginBottom: 8 }}
          items={crumbs.map((crumb, index) => {
            const isLast = index === crumbs.length - 1;
            return {
              title:
                crumb.path && !isLast ? (
                  <Link to={crumb.path}>{crumb.label}</Link>
                ) : (
                  crumb.label
                ),
            };
          })}
        />
      )}
      <div className="console-page-header-row">
        <div>
          <Typography.Title level={3} style={{ margin: 0 }}>
            {title}
          </Typography.Title>
          {subtitle ? (
            <Typography.Paragraph type="secondary" style={{ marginBottom: 0, marginTop: 4 }}>
              {subtitle}
            </Typography.Paragraph>
          ) : null}
        </div>
        {extra ? <Space wrap>{extra}</Space> : null}
      </div>
    </header>
  );
}
