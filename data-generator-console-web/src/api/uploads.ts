import { apiFormRequest, apiRequest } from './client';

/**
 * @param file source file bytes
 * @returns absolute path for template source {@code path}
 */
export function uploadSourceFile(file: File): Promise<string> {
  const form = new FormData();
  form.set('file', file);
  return apiFormRequest<string>('/console/uploads/file', form);
}

/**
 * @param filename suggested name (e.g. data.json)
 * @param content pasted body
 */
export function uploadInlineSource(filename: string, content: string): Promise<string> {
  return apiRequest<string>('/console/uploads/inline', {
    method: 'POST',
    body: JSON.stringify({ filename, content }),
  });
}
