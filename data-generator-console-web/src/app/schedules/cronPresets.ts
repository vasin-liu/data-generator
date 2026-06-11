/** Spring six-field cron presets for the schedule editor. */
export const SCHEDULE_CRON_PRESETS = [
  { key: 'hourly', cron: '0 0 * * * *', labelKey: 'schedules.form.cronPresets.hourly' },
  { key: 'daily2am', cron: '0 0 2 * * *', labelKey: 'schedules.form.cronPresets.daily2am' },
  { key: 'weeklyMon3am', cron: '0 0 3 * * MON', labelKey: 'schedules.form.cronPresets.weeklyMon3am' },
  { key: 'monthly1st4am', cron: '0 0 4 1 * *', labelKey: 'schedules.form.cronPresets.monthly1st4am' },
] as const;
