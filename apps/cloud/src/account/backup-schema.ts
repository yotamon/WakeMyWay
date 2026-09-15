import { z } from 'zod';

const MAX_ALARMS_PER_BACKUP = 64;
const MAX_SAFE_REVISION = Number.MAX_SAFE_INTEGER;

const isoInstant = z.string().refine(value => {
  const parsed = Date.parse(value);
  return Number.isFinite(parsed) && /T/.test(value);
}, 'Expected an ISO-8601 timestamp.');

const localDate = z.string().regex(/^\d{4}-\d{2}-\d{2}$/);
const localTime = z.string().regex(/^(?:[01]\d|2[0-3]):[0-5]\d(?::[0-5]\d(?:\.\d{1,9})?)?$/);
const zoneId = z.string().min(1).max(80).refine(value => {
  try {
    new Intl.DateTimeFormat('en-US', { timeZone: value });
    return true;
  } catch {
    return false;
  }
}, 'Expected an IANA time-zone id.');

const wakeSoundId = z.enum(['morning-light', 'soft-start', 'morning-pulse']);
const voiceStyle = z.enum(['DEFAULT', 'MOTIVATIONAL', 'MINIMAL']);
const appearance = z.enum(['DAYLIGHT', 'WARM_SUNRISE', 'SOFT_DAWN']);
const tomorrowContractMode = z.enum(['OPTIONAL', 'ALWAYS_PROMPT', 'DISABLED']);
const dayOfWeek = z.enum([
  'MONDAY',
  'TUESDAY',
  'WEDNESDAY',
  'THURSDAY',
  'FRIDAY',
  'SATURDAY',
  'SUNDAY',
]);

const weeklySchedule = z.object({
  type: z.literal('weekly'),
  days: z.array(dayOfWeek).min(1).max(7).refine(days => new Set(days).size === days.length),
  time: localTime,
}).strict();

const oneShotSchedule = z.object({
  type: z.literal('oneShot'),
  date: localDate,
  time: localTime,
}).strict();

const snoozePolicy = z.object({
  enabled: z.boolean(),
  durationSeconds: z.number().int().min(1).max(30 * 60),
  maxCount: z.number().int().positive().nullable(),
}).strict();

export const consumerBackupAlarmSchema = z.object({
  id: z.string().trim().min(1).max(128),
  label: z.string().max(80),
  enabled: z.boolean(),
  zoneId,
  schedule: z.discriminatedUnion('type', [weeklySchedule, oneShotSchedule]),
  soundId: wakeSoundId,
  voiceCheckInEnabled: z.boolean(),
  characterId: z.literal('alfred'),
  voiceStyle,
  snoozePolicy,
  tomorrowContractMode,
  firstMoveDefault: z.string().max(120).nullable(),
  revision: z.number().int().positive().max(MAX_SAFE_REVISION),
  createdAt: isoInstant,
  updatedAt: isoInstant,
}).strict().superRefine((alarm, context) => {
  if (Date.parse(alarm.updatedAt) < Date.parse(alarm.createdAt)) {
    context.addIssue({
      code: 'custom',
      path: ['updatedAt'],
      message: 'updatedAt must not be before createdAt.',
    });
  }
});

export const consumerBackupPreferencesSchema = z.object({
  displayName: z.string().max(48).nullable(),
  defaultSoundId: wakeSoundId,
  defaultVoiceCheckInEnabled: z.boolean(),
  defaultVoiceStyle: voiceStyle,
  defaultSnoozeMinutes: z.union([z.literal(5), z.literal(10), z.literal(15)]),
  defaultFirstMove: z.string().max(120).nullable(),
  appearance,
}).strict();

export const consumerBackupSnapshotSchema = z.object({
  schemaVersion: z.literal(1),
  generatedAt: isoInstant,
  preferences: consumerBackupPreferencesSchema,
  alarms: z.array(consumerBackupAlarmSchema).max(MAX_ALARMS_PER_BACKUP),
}).strict().superRefine((snapshot, context) => {
  const ids = snapshot.alarms.map(alarm => alarm.id);
  if (new Set(ids).size !== ids.length) {
    context.addIssue({
      code: 'custom',
      path: ['alarms'],
      message: 'Alarm ids must be unique.',
    });
  }
});

export type ConsumerBackupSnapshot = z.infer<typeof consumerBackupSnapshotSchema>;
