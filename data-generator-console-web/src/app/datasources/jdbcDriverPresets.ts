import type { JdbcDriverPresetDto } from '../../api/types';

/**
 * JDBC driver preset for the datasource form (aligned with server catalog).
 */
export type JdbcDriverPreset = {
  id: string;
  labelKey: string;
  driverClassName: string;
  alternateDriverClassNames: string[];
  urlTemplate: string;
  groupKey: string;
  bundleKey: string;
  bundled: boolean;
};

export const JDBC_DRIVER_GROUP_KEYS = [
  'dm',
  'kingbase',
  'highgo',
  'clickhouse',
  'postgresql',
  'mysql',
] as const;

/** Client fallback when API catalog is unavailable (mirrors JdbcDriverPresetCatalog). */
export const JDBC_DRIVER_PRESETS_FALLBACK: JdbcDriverPreset[] = [
  {
    id: 'dm8',
    labelKey: 'datasources.driver.dm8',
    groupKey: 'dm',
    bundleKey: 'dm',
    driverClassName: 'dm.jdbc.driver.DmDriver',
    alternateDriverClassNames: [],
    urlTemplate: 'jdbc:dm://localhost:5236?schema=YOUR_SCHEMA',
    bundled: true,
  },
  {
    id: 'kingbase8',
    labelKey: 'datasources.driver.kingbase8',
    groupKey: 'kingbase',
    bundleKey: 'kingbase8',
    driverClassName: 'com.kingbase8.Driver',
    alternateDriverClassNames: ['com.kingbase.Driver'],
    urlTemplate: 'jdbc:kingbase8://localhost:54321/YOUR_DATABASE',
    bundled: true,
  },
  {
    id: 'kingbase9',
    labelKey: 'datasources.driver.kingbase9',
    groupKey: 'kingbase',
    bundleKey: 'kingbase9',
    driverClassName: 'com.kingbase9.Driver',
    alternateDriverClassNames: ['com.kingbase8.Driver', 'com.kingbase.Driver'],
    urlTemplate: 'jdbc:kingbase9://localhost:54321/YOUR_DATABASE',
    bundled: true,
  },
  {
    id: 'highgo',
    labelKey: 'datasources.driver.highgo',
    groupKey: 'highgo',
    bundleKey: 'highgo',
    driverClassName: 'com.highgo.jdbc.Driver',
    alternateDriverClassNames: [],
    urlTemplate: 'jdbc:highgo://localhost:5866/highgo',
    bundled: true,
  },
  {
    id: 'clickhouse24',
    labelKey: 'datasources.driver.clickhouse24',
    groupKey: 'clickhouse',
    bundleKey: 'clickhouse',
    driverClassName: 'com.clickhouse.jdbc.ClickHouseDriver',
    alternateDriverClassNames: ['ru.yandex.clickhouse.ClickHouseDriver'],
    urlTemplate: 'jdbc:clickhouse://localhost:8123/default',
    bundled: false,
  },
  {
    id: 'postgresql16',
    labelKey: 'datasources.driver.postgresql16',
    groupKey: 'postgresql',
    bundleKey: 'postgresql',
    driverClassName: 'org.postgresql.Driver',
    alternateDriverClassNames: [],
    urlTemplate: 'jdbc:postgresql://localhost:5432/postgres',
    bundled: false,
  },
  {
    id: 'mysql57',
    labelKey: 'datasources.driver.mysql57',
    groupKey: 'mysql',
    bundleKey: 'mysql',
    driverClassName: 'com.mysql.cj.jdbc.Driver',
    alternateDriverClassNames: ['com.mysql.jdbc.Driver'],
    urlTemplate:
      'jdbc:mysql://localhost:3306/YOUR_DATABASE?useUnicode=true&characterEncoding=utf8&useSSL=false&serverTimezone=GMT%2B8',
    bundled: false,
  },
];

/**
 * @param presets active catalog
 * @param presetId selected preset id
 */
export function isBundledPreset(
  presets: JdbcDriverPreset[],
  presetId: string | undefined,
): boolean {
  if (!presetId) {
    return false;
  }
  return findJdbcDriverPreset(presets, presetId)?.bundled ?? false;
}

/**
 * @param dtos presets from GET /api/datasources
 */
export function mapApiDriverPresets(dtos: JdbcDriverPresetDto[] | undefined): JdbcDriverPreset[] {
  if (!dtos?.length) {
    return JDBC_DRIVER_PRESETS_FALLBACK;
  }
  return dtos.map((dto) => ({
    id: dto.id,
    labelKey: dto.labelKey,
    groupKey: dto.groupKey,
    bundleKey: dto.bundleKey,
    driverClassName: dto.driverClassName,
    alternateDriverClassNames: dto.alternateDriverClassNames ?? [],
    urlTemplate: dto.urlTemplate,
    bundled: dto.bundled,
  }));
}

/**
 * @param dtos optional API catalog
 */
export function resolveDriverPresets(dtos?: JdbcDriverPresetDto[]): JdbcDriverPreset[] {
  return mapApiDriverPresets(dtos);
}

const presetById = new Map(JDBC_DRIVER_PRESETS_FALLBACK.map((p) => [p.id, p]));

/**
 * @param presets active catalog
 * @param id preset id
 */
export function findJdbcDriverPreset(
  presets: JdbcDriverPreset[],
  id: string | undefined,
): JdbcDriverPreset | undefined {
  if (!id) {
    return undefined;
  }
  return presets.find((p) => p.id === id) ?? presetById.get(id);
}

/**
 * @param presets active catalog
 * @param driverClassName JDBC driver class
 */
export function guessPresetId(
  presets: JdbcDriverPreset[],
  driverClassName: string | undefined,
): string | undefined {
  if (!driverClassName) {
    return undefined;
  }
  return presets.find(
    (p) =>
      p.driverClassName === driverClassName
      || p.alternateDriverClassNames.includes(driverClassName),
  )?.id;
}
