import { apiRequest } from './client';
import type { TaskScheduleUpsertRequest, TaskScheduleView } from './types';

/**
 * @param templateId optional filter
 */
export function fetchSchedules(templateId?: string): Promise<TaskScheduleView[]> {
  const suffix = templateId != null ? `?templateId=${encodeURIComponent(templateId)}` : '';
  return apiRequest<TaskScheduleView[]>(`/console/schedules${suffix}`);
}

/**
 * @param id schedule row id
 */
export function fetchSchedule(id: string): Promise<TaskScheduleView> {
  return apiRequest<TaskScheduleView>(`/console/schedules/${id}`);
}

/**
 * @param body create payload
 */
export function createSchedule(body: TaskScheduleUpsertRequest): Promise<TaskScheduleView> {
  return apiRequest<TaskScheduleView>('/console/schedules', {
    method: 'POST',
    body: JSON.stringify(body),
  });
}

/**
 * @param id schedule row id
 * @param body update payload
 */
export function updateSchedule(id: string, body: TaskScheduleUpsertRequest): Promise<TaskScheduleView> {
  return apiRequest<TaskScheduleView>(`/console/schedules/${id}`, {
    method: 'PUT',
    body: JSON.stringify(body),
  });
}

/**
 * @param id schedule row id
 */
export function deleteSchedule(id: string): Promise<string> {
  return apiRequest<string>(`/console/schedules/${id}`, { method: 'DELETE' });
}

/**
 * @param cron Spring six-field cron expression
 * @returns ISO-8601 instant for next fire after now
 */
export function previewScheduleCron(cron: string): Promise<string> {
  const params = new URLSearchParams({ cron: cron.trim() });
  return apiRequest<string>(`/console/schedules/preview?${params}`);
}
