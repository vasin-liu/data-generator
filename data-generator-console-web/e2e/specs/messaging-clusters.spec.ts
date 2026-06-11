import { expect, test } from '@playwright/test';
import {
  apiPostWithRole,
  expectApiSuccess,
  fetchDatasourcesOverview,
  unwrapApiData,
} from '../helpers/api';

type DataSourcesOverview = {
  kafkaPersisted?: Array<{ name: string }>;
  elasticsearchPersisted?: Array<{ name: string }>;
  kafkaClusters?: string[];
  elasticsearchClusters?: string[];
};

test.describe('Messaging cluster registration', () => {
  test('API upserts Kafka and Elasticsearch clusters', async ({ request }) => {
    const suffix = Date.now();
    const kafkaName = `e2e-kafka-${suffix}`;
    const esName = `e2e-es-${suffix}`;

    const kafkaSave = await apiPostWithRole(request, '/api/datasources/kafka-clusters', {
      name: kafkaName,
      bootstrapServers: ['127.0.0.1:9092'],
      clientId: 'e2e-kafka-client',
      acks: 'all',
    });
    expect(kafkaSave.res.ok()).toBeTruthy();
    expectApiSuccess(kafkaSave.body);

    const esSave = await apiPostWithRole(request, '/api/datasources/elasticsearch-clusters', {
      name: esName,
      uris: ['http://127.0.0.1:9200'],
      connectionTimeoutMs: 1000,
      socketTimeoutMs: 1000,
    });
    expect(esSave.res.ok()).toBeTruthy();
    expectApiSuccess(esSave.body);

    const { res, body } = await fetchDatasourcesOverview(request);
    expect(res.ok()).toBeTruthy();
    expectApiSuccess(body);
    const overview = unwrapApiData<DataSourcesOverview>(body);
    expect(overview?.kafkaPersisted?.map((row) => row.name)).toContain(kafkaName);
    expect(overview?.elasticsearchPersisted?.map((row) => row.name)).toContain(esName);
    expect(overview?.kafkaClusters).toContain(kafkaName);
    expect(overview?.elasticsearchClusters).toContain(esName);
  });
});
