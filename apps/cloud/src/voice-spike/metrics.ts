export type VoiceTransportCandidate =
  | 'direct-openai'
  | 'livekit-rtc'
  | 'vercel-gateway';

export type VoiceNetworkScenario =
  | 'wifi'
  | 'mobile'
  | 'wifi-to-mobile'
  | 'mobile-to-wifi';

export type VoiceAudioRoute = 'speaker' | 'wired-headset' | 'bluetooth';

export type VoiceSpikeOutcome = 'success' | 'failure';

export type VoiceSpikeFailureStage =
  | 'connect'
  | 'first-speech'
  | 'barge-in'
  | 'reconnect'
  | 'network-transition'
  | 'audio-route'
  | 'unknown';

export type VoicePrivacyEligibility =
  | 'private-data-eligible'
  | 'synthetic-only'
  | 'ineligible'
  | 'unknown';

export interface VoiceOperationalFootprint {
  androidClientLibraries: number;
  backendComponents: number;
  credentialExchangeHops: number;
  statefulRelayRequired: boolean;
  managedRtcControlPlane: boolean;
}

/**
 * Identifies one comparable transport/provider/model/privacy configuration.
 * Measurements from different configurations are never silently mixed.
 */
export interface VoiceSpikeConfiguration {
  candidate: VoiceTransportCandidate;
  configurationId: string;
  model: string;
  privacyEligibility: VoicePrivacyEligibility;
  privacyEvidenceRef: string | null;
  operationalFootprint: VoiceOperationalFootprint | null;
}

export interface VoiceSpikeTimings {
  coldConnectionMs: number | null;
  firstSpeechMs: number | null;
  bargeInMs: number | null;
  reconnectMs: number | null;
}

/**
 * Intentionally metadata-only. The M8 harness has no fields for transcript,
 * prompt, microphone audio, generated audio, or private wake context.
 */
export interface VoiceSpikeSample {
  sampleId: string;
  candidate: VoiceTransportCandidate;
  configurationId: string;
  networkScenario: VoiceNetworkScenario;
  audioRoute: VoiceAudioRoute;
  outcome: VoiceSpikeOutcome;
  failureStage: VoiceSpikeFailureStage | null;
  timings: VoiceSpikeTimings;
  estimatedCostUsd: number | null;
}

export interface VoiceMetricDistribution {
  count: number;
  min: number;
  p50: number;
  p95: number;
  max: number;
  mean: number;
}

export type VoiceMissingEvidenceCode =
  | 'successful-runs'
  | 'wifi-success'
  | 'mobile-success'
  | 'network-transition-success'
  | 'bluetooth-success'
  | 'barge-in-measurements'
  | 'reconnect-measurements'
  | 'cost-measurements'
  | 'private-data-eligibility'
  | 'privacy-evidence'
  | 'operational-footprint';

export interface VoiceSpikeEvidenceRequirements {
  minimumSuccessfulRuns: number;
  minimumBargeInMeasurements: number;
  minimumReconnectMeasurements: number;
  minimumCostMeasurements: number;
  requireWifiSuccess: boolean;
  requireMobileSuccess: boolean;
  requireNetworkTransitionSuccess: boolean;
  requireBluetoothSuccess: boolean;
  requirePrivateDataEligibility: boolean;
  requirePrivacyEvidence: boolean;
  requireOperationalFootprint: boolean;
}

/**
 * Smoke-level M8 evidence floor, not a production SLA or statistical claim.
 * The values are centralized so real spike experience can tighten them.
 */
export const DEFAULT_VOICE_SPIKE_REQUIREMENTS: VoiceSpikeEvidenceRequirements = {
  minimumSuccessfulRuns: 5,
  minimumBargeInMeasurements: 3,
  minimumReconnectMeasurements: 3,
  minimumCostMeasurements: 3,
  requireWifiSuccess: true,
  requireMobileSuccess: true,
  requireNetworkTransitionSuccess: true,
  requireBluetoothSuccess: true,
  requirePrivateDataEligibility: true,
  requirePrivacyEvidence: true,
  requireOperationalFootprint: true,
};

export interface VoiceSpikeEvidenceReadiness {
  eligibleForArchitectureDecision: boolean;
  missingEvidence: VoiceMissingEvidenceCode[];
}

export interface VoiceSpikeSummary {
  candidate: VoiceTransportCandidate;
  configurationId: string;
  model: string;
  totalRuns: number;
  successfulRuns: number;
  failedRuns: number;
  successRate: number;
  coldConnectionMs: VoiceMetricDistribution | null;
  firstSpeechMs: VoiceMetricDistribution | null;
  bargeInMs: VoiceMetricDistribution | null;
  reconnectMs: VoiceMetricDistribution | null;
  estimatedCostUsd: VoiceMetricDistribution | null;
  successfulNetworkScenarios: Record<VoiceNetworkScenario, number>;
  successfulAudioRoutes: Record<VoiceAudioRoute, number>;
  failuresByStage: Record<VoiceSpikeFailureStage, number>;
  privacyEligibility: VoicePrivacyEligibility;
  privacyEvidenceRef: string | null;
  operationalFootprint: VoiceOperationalFootprint | null;
  readiness: VoiceSpikeEvidenceReadiness;
}

export interface VoiceSpikeEvidenceSet {
  configuration: VoiceSpikeConfiguration;
  samples: VoiceSpikeSample[];
}

export interface VoiceArchitectureEvidenceMatrix {
  comparableForDecision: boolean;
  comparisonMissingEvidence: string[];
  summaries: VoiceSpikeSummary[];
}

const NETWORK_SCENARIOS: VoiceNetworkScenario[] = [
  'wifi',
  'mobile',
  'wifi-to-mobile',
  'mobile-to-wifi',
];

const AUDIO_ROUTES: VoiceAudioRoute[] = ['speaker', 'wired-headset', 'bluetooth'];

const FAILURE_STAGES: VoiceSpikeFailureStage[] = [
  'connect',
  'first-speech',
  'barge-in',
  'reconnect',
  'network-transition',
  'audio-route',
  'unknown',
];

function assertNonBlank(value: string, label: string): void {
  if (value.trim().length === 0) throw new Error(`${label} must not be blank`);
}

function assertCount(value: number, label: string): void {
  if (!Number.isInteger(value) || value < 0) {
    throw new Error(`${label} must be a non-negative integer`);
  }
}

function assertMeasurement(value: number | null, label: string): void {
  if (value === null) return;
  if (!Number.isFinite(value) || value < 0) {
    throw new Error(`${label} must be a finite non-negative number`);
  }
}

export function validateVoiceSpikeConfiguration(configuration: VoiceSpikeConfiguration): void {
  assertNonBlank(configuration.configurationId, 'Voice configuration id');
  assertNonBlank(configuration.model, 'Voice model');
  if (configuration.privacyEvidenceRef !== null) {
    assertNonBlank(configuration.privacyEvidenceRef, 'Privacy evidence reference');
  }

  const footprint = configuration.operationalFootprint;
  if (footprint !== null) {
    assertCount(footprint.androidClientLibraries, 'Android client library count');
    assertCount(footprint.backendComponents, 'Backend component count');
    assertCount(footprint.credentialExchangeHops, 'Credential exchange hop count');
  }
}

export function validateVoiceSpikeRequirements(
  requirements: VoiceSpikeEvidenceRequirements,
): void {
  for (const [label, value] of [
    ['Minimum successful runs', requirements.minimumSuccessfulRuns],
    ['Minimum barge-in measurements', requirements.minimumBargeInMeasurements],
    ['Minimum reconnect measurements', requirements.minimumReconnectMeasurements],
    ['Minimum cost measurements', requirements.minimumCostMeasurements],
  ] as const) {
    if (!Number.isInteger(value) || value < 1) {
      throw new Error(`${label} must be a positive integer`);
    }
  }
}

export function validateVoiceSpikeSample(sample: VoiceSpikeSample): void {
  assertNonBlank(sample.sampleId, 'Voice spike sample id');
  assertNonBlank(sample.configurationId, 'Voice spike sample configuration id');

  assertMeasurement(sample.timings.coldConnectionMs, 'Cold connection latency');
  assertMeasurement(sample.timings.firstSpeechMs, 'First speech latency');
  assertMeasurement(sample.timings.bargeInMs, 'Barge-in latency');
  assertMeasurement(sample.timings.reconnectMs, 'Reconnect latency');
  assertMeasurement(sample.estimatedCostUsd, 'Estimated cost');

  if (sample.outcome === 'success') {
    if (sample.failureStage !== null) {
      throw new Error('Successful voice spike samples cannot include a failure stage');
    }
    if (sample.timings.coldConnectionMs === null || sample.timings.firstSpeechMs === null) {
      throw new Error(
        'Successful voice spike samples require cold connection and first speech measurements',
      );
    }
  } else if (sample.failureStage === null) {
    throw new Error('Failed voice spike samples require a failure stage');
  }
}

/** Nearest-rank percentile: deterministic and intentionally dependency-free. */
export function nearestRankPercentile(values: readonly number[], percentile: number): number {
  if (values.length === 0) throw new Error('Cannot calculate a percentile from zero values');
  if (!Number.isFinite(percentile) || percentile <= 0 || percentile > 1) {
    throw new Error('Percentile must be in the range (0, 1]');
  }

  const sorted = [...values].sort((left, right) => left - right);
  const rank = Math.ceil(percentile * sorted.length);
  const value = sorted[Math.max(0, rank - 1)];
  if (value === undefined) throw new Error('Percentile calculation produced no value');
  return value;
}

export function summarizeDistribution(values: readonly number[]): VoiceMetricDistribution | null {
  if (values.length === 0) return null;

  const sorted = [...values].sort((left, right) => left - right);
  const min = sorted[0];
  const max = sorted.at(-1);
  if (min === undefined || max === undefined) return null;

  return {
    count: sorted.length,
    min,
    p50: nearestRankPercentile(sorted, 0.5),
    p95: nearestRankPercentile(sorted, 0.95),
    max,
    mean: sorted.reduce((sum, value) => sum + value, 0) / sorted.length,
  };
}

function zeroRecord<T extends string>(keys: readonly T[]): Record<T, number> {
  return Object.fromEntries(keys.map(key => [key, 0])) as Record<T, number>;
}

function measured(
  samples: readonly VoiceSpikeSample[],
  select: (sample: VoiceSpikeSample) => number | null,
): number[] {
  const result: number[] = [];
  for (const sample of samples) {
    const value = select(sample);
    if (value !== null) result.push(value);
  }
  return result;
}

function evidenceReadiness(
  configuration: VoiceSpikeConfiguration,
  samples: readonly VoiceSpikeSample[],
  requirements: VoiceSpikeEvidenceRequirements,
  successfulNetworkScenarios: Record<VoiceNetworkScenario, number>,
  successfulAudioRoutes: Record<VoiceAudioRoute, number>,
): VoiceSpikeEvidenceReadiness {
  const successful = samples.filter(sample => sample.outcome === 'success');
  const missing = new Set<VoiceMissingEvidenceCode>();

  if (successful.length < requirements.minimumSuccessfulRuns) missing.add('successful-runs');
  if (requirements.requireWifiSuccess && successfulNetworkScenarios.wifi === 0) {
    missing.add('wifi-success');
  }
  if (requirements.requireMobileSuccess && successfulNetworkScenarios.mobile === 0) {
    missing.add('mobile-success');
  }
  if (
    requirements.requireNetworkTransitionSuccess &&
    successfulNetworkScenarios['wifi-to-mobile'] === 0 &&
    successfulNetworkScenarios['mobile-to-wifi'] === 0
  ) {
    missing.add('network-transition-success');
  }
  if (requirements.requireBluetoothSuccess && successfulAudioRoutes.bluetooth === 0) {
    missing.add('bluetooth-success');
  }
  if (
    measured(samples, sample => sample.timings.bargeInMs).length <
    requirements.minimumBargeInMeasurements
  ) {
    missing.add('barge-in-measurements');
  }
  if (
    measured(samples, sample => sample.timings.reconnectMs).length <
    requirements.minimumReconnectMeasurements
  ) {
    missing.add('reconnect-measurements');
  }
  if (
    measured(samples, sample => sample.estimatedCostUsd).length <
    requirements.minimumCostMeasurements
  ) {
    missing.add('cost-measurements');
  }
  if (
    requirements.requirePrivateDataEligibility &&
    configuration.privacyEligibility !== 'private-data-eligible'
  ) {
    missing.add('private-data-eligibility');
  }
  if (requirements.requirePrivacyEvidence && configuration.privacyEvidenceRef === null) {
    missing.add('privacy-evidence');
  }
  if (requirements.requireOperationalFootprint && configuration.operationalFootprint === null) {
    missing.add('operational-footprint');
  }

  return {
    eligibleForArchitectureDecision: missing.size === 0,
    missingEvidence: [...missing].sort(),
  };
}

export function summarizeVoiceSpikeEvidence(
  evidence: VoiceSpikeEvidenceSet,
  requirements: VoiceSpikeEvidenceRequirements = DEFAULT_VOICE_SPIKE_REQUIREMENTS,
): VoiceSpikeSummary {
  validateVoiceSpikeConfiguration(evidence.configuration);
  validateVoiceSpikeRequirements(requirements);

  const sampleIds = new Set<string>();
  for (const sample of evidence.samples) {
    validateVoiceSpikeSample(sample);
    if (
      sample.candidate !== evidence.configuration.candidate ||
      sample.configurationId !== evidence.configuration.configurationId
    ) {
      throw new Error('Voice spike evidence cannot mix candidate/configuration identities');
    }
    if (sampleIds.has(sample.sampleId)) {
      throw new Error(`Duplicate voice spike sample id: ${sample.sampleId}`);
    }
    sampleIds.add(sample.sampleId);
  }

  const successfulNetworkScenarios = zeroRecord(NETWORK_SCENARIOS);
  const successfulAudioRoutes = zeroRecord(AUDIO_ROUTES);
  const failuresByStage = zeroRecord(FAILURE_STAGES);

  let successfulRuns = 0;
  let failedRuns = 0;
  for (const sample of evidence.samples) {
    if (sample.outcome === 'success') {
      successfulRuns += 1;
      successfulNetworkScenarios[sample.networkScenario] += 1;
      successfulAudioRoutes[sample.audioRoute] += 1;
    } else {
      failedRuns += 1;
      const stage = sample.failureStage;
      if (stage === null) throw new Error('Validated failed sample lost its failure stage');
      failuresByStage[stage] += 1;
    }
  }

  const totalRuns = evidence.samples.length;
  return {
    candidate: evidence.configuration.candidate,
    configurationId: evidence.configuration.configurationId,
    model: evidence.configuration.model,
    totalRuns,
    successfulRuns,
    failedRuns,
    successRate: totalRuns === 0 ? 0 : successfulRuns / totalRuns,
    coldConnectionMs: summarizeDistribution(
      measured(evidence.samples, sample => sample.timings.coldConnectionMs),
    ),
    firstSpeechMs: summarizeDistribution(
      measured(evidence.samples, sample => sample.timings.firstSpeechMs),
    ),
    bargeInMs: summarizeDistribution(measured(evidence.samples, sample => sample.timings.bargeInMs)),
    reconnectMs: summarizeDistribution(
      measured(evidence.samples, sample => sample.timings.reconnectMs),
    ),
    estimatedCostUsd: summarizeDistribution(
      measured(evidence.samples, sample => sample.estimatedCostUsd),
    ),
    successfulNetworkScenarios,
    successfulAudioRoutes,
    failuresByStage,
    privacyEligibility: evidence.configuration.privacyEligibility,
    privacyEvidenceRef: evidence.configuration.privacyEvidenceRef,
    operationalFootprint: evidence.configuration.operationalFootprint,
    readiness: evidenceReadiness(
      evidence.configuration,
      evidence.samples,
      requirements,
      successfulNetworkScenarios,
      successfulAudioRoutes,
    ),
  };
}

/**
 * Builds comparable evidence only. It deliberately has no winner/ranking field.
 * Provider selection remains an explicit ADR decision after evidence is complete.
 */
export function buildVoiceArchitectureEvidenceMatrix(
  evidenceSets: readonly VoiceSpikeEvidenceSet[],
  requirements: VoiceSpikeEvidenceRequirements = DEFAULT_VOICE_SPIKE_REQUIREMENTS,
  minimumComparableConfigurations = 2,
): VoiceArchitectureEvidenceMatrix {
  if (!Number.isInteger(minimumComparableConfigurations) || minimumComparableConfigurations < 2) {
    throw new Error('Architecture comparison requires at least two configurations');
  }

  const identities = new Set<string>();
  const summaries = evidenceSets.map(evidence => {
    const identity = `${evidence.configuration.candidate}:${evidence.configuration.configurationId}`;
    if (identities.has(identity)) {
      throw new Error(`Duplicate voice architecture evidence configuration: ${identity}`);
    }
    identities.add(identity);
    return summarizeVoiceSpikeEvidence(evidence, requirements);
  });

  summaries.sort((left, right) => {
    const candidateOrder = left.candidate.localeCompare(right.candidate);
    return candidateOrder !== 0
      ? candidateOrder
      : left.configurationId.localeCompare(right.configurationId);
  });

  const comparisonMissingEvidence: string[] = [];
  if (summaries.length < minimumComparableConfigurations) {
    comparisonMissingEvidence.push(
      `comparison:requires-${minimumComparableConfigurations}-configurations`,
    );
  }
  for (const summary of summaries) {
    for (const missing of summary.readiness.missingEvidence) {
      comparisonMissingEvidence.push(
        `${summary.candidate}:${summary.configurationId}:${missing}`,
      );
    }
  }

  comparisonMissingEvidence.sort();
  return {
    comparableForDecision: comparisonMissingEvidence.length === 0,
    comparisonMissingEvidence,
    summaries,
  };
}
