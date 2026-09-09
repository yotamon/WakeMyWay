import { describe, expect, it } from 'vitest';

import {
  buildVoiceArchitectureEvidenceMatrix,
  nearestRankPercentile,
  summarizeDistribution,
  summarizeVoiceSpikeEvidence,
  type VoiceOperationalFootprint,
  type VoiceSpikeConfiguration,
  type VoiceSpikeEvidenceSet,
  type VoiceSpikeSample,
  type VoiceTransportCandidate,
} from '../src/voice-spike/metrics';

const footprint: VoiceOperationalFootprint = {
  androidClientLibraries: 1,
  backendComponents: 1,
  credentialExchangeHops: 1,
  statefulRelayRequired: false,
  managedRtcControlPlane: false,
};

function configuration(
  candidate: VoiceTransportCandidate = 'direct-openai',
  configurationId = 'direct-openai:v1',
): VoiceSpikeConfiguration {
  return {
    candidate,
    configurationId,
    model: 'openai/gpt-realtime-test',
    privacyEligibility: 'private-data-eligible',
    privacyEvidenceRef: 'ADR-008/test-fixture',
    operationalFootprint: footprint,
  };
}

function sample(
  sampleId: string,
  overrides: Partial<VoiceSpikeSample> = {},
): VoiceSpikeSample {
  return {
    sampleId,
    candidate: 'direct-openai',
    configurationId: 'direct-openai:v1',
    networkScenario: 'wifi',
    audioRoute: 'speaker',
    outcome: 'success',
    failureStage: null,
    timings: {
      coldConnectionMs: 300,
      firstSpeechMs: 650,
      bargeInMs: 120,
      reconnectMs: 450,
    },
    estimatedCostUsd: 0.04,
    ...overrides,
  };
}

function completeEvidence(
  candidate: VoiceTransportCandidate = 'direct-openai',
  configurationId = 'direct-openai:v1',
): VoiceSpikeEvidenceSet {
  const base = [
    sample('run-1', { networkScenario: 'wifi', audioRoute: 'speaker' }),
    sample('run-2', {
      networkScenario: 'mobile',
      audioRoute: 'speaker',
      timings: {
        coldConnectionMs: 340,
        firstSpeechMs: 700,
        bargeInMs: 135,
        reconnectMs: 480,
      },
      estimatedCostUsd: 0.041,
    }),
    sample('run-3', {
      networkScenario: 'wifi-to-mobile',
      audioRoute: 'bluetooth',
      timings: {
        coldConnectionMs: 360,
        firstSpeechMs: 720,
        bargeInMs: 145,
        reconnectMs: 510,
      },
      estimatedCostUsd: 0.043,
    }),
    sample('run-4', {
      networkScenario: 'mobile-to-wifi',
      audioRoute: 'bluetooth',
      timings: {
        coldConnectionMs: 320,
        firstSpeechMs: 680,
        bargeInMs: null,
        reconnectMs: null,
      },
      estimatedCostUsd: 0.039,
    }),
    sample('run-5', {
      networkScenario: 'wifi',
      audioRoute: 'bluetooth',
      timings: {
        coldConnectionMs: 310,
        firstSpeechMs: 660,
        bargeInMs: null,
        reconnectMs: null,
      },
      estimatedCostUsd: 0.04,
    }),
  ];

  return {
    configuration: configuration(candidate, configurationId),
    samples: base.map(value => ({
      ...value,
      candidate,
      configurationId,
    })),
  };
}

describe('voice spike distributions', () => {
  it('uses deterministic nearest-rank percentiles', () => {
    const values = [500, 100, 400, 300, 200];

    expect(nearestRankPercentile(values, 0.5)).toBe(300);
    expect(nearestRankPercentile(values, 0.95)).toBe(500);
    expect(summarizeDistribution(values)).toEqual({
      count: 5,
      min: 100,
      p50: 300,
      p95: 500,
      max: 500,
      mean: 300,
    });
  });

  it('rejects invalid percentile requests', () => {
    expect(() => nearestRankPercentile([], 0.5)).toThrow(/zero values/i);
    expect(() => nearestRankPercentile([1], 0)).toThrow(/range/i);
    expect(() => nearestRankPercentile([1], 1.01)).toThrow(/range/i);
  });
});

describe('voice spike evidence summary', () => {
  it('marks a complete comparable configuration ready without selecting a winner', () => {
    const summary = summarizeVoiceSpikeEvidence(completeEvidence());

    expect(summary.totalRuns).toBe(5);
    expect(summary.successfulRuns).toBe(5);
    expect(summary.failedRuns).toBe(0);
    expect(summary.successRate).toBe(1);
    expect(summary.coldConnectionMs?.p50).toBe(320);
    expect(summary.coldConnectionMs?.p95).toBe(360);
    expect(summary.firstSpeechMs?.p50).toBe(680);
    expect(summary.bargeInMs?.count).toBe(3);
    expect(summary.reconnectMs?.count).toBe(3);
    expect(summary.estimatedCostUsd?.count).toBe(5);
    expect(summary.successfulNetworkScenarios.mobile).toBe(1);
    expect(summary.successfulNetworkScenarios['wifi-to-mobile']).toBe(1);
    expect(summary.successfulAudioRoutes.bluetooth).toBe(3);
    expect(summary.readiness).toEqual({
      eligibleForArchitectureDecision: true,
      missingEvidence: [],
    });
  });

  it('keeps synthetic-only gateway evidence out of a private-data architecture decision', () => {
    const evidence = completeEvidence('vercel-gateway', 'vercel-gateway:v1');
    evidence.configuration = {
      ...evidence.configuration,
      privacyEligibility: 'synthetic-only',
      privacyEvidenceRef: 'current-gateway-audio-route-is-non-zdr',
    };

    const summary = summarizeVoiceSpikeEvidence(evidence);

    expect(summary.readiness.eligibleForArchitectureDecision).toBe(false);
    expect(summary.readiness.missingEvidence).toContain('private-data-eligibility');
  });

  it('does not let failed mobile or Bluetooth attempts satisfy successful coverage', () => {
    const evidence: VoiceSpikeEvidenceSet = {
      configuration: configuration(),
      samples: [
        sample('wifi-success'),
        sample('mobile-bt-failure', {
          networkScenario: 'mobile',
          audioRoute: 'bluetooth',
          outcome: 'failure',
          failureStage: 'audio-route',
          timings: {
            coldConnectionMs: 340,
            firstSpeechMs: null,
            bargeInMs: null,
            reconnectMs: null,
          },
        }),
      ],
    };

    const summary = summarizeVoiceSpikeEvidence(evidence);

    expect(summary.successfulNetworkScenarios.mobile).toBe(0);
    expect(summary.successfulAudioRoutes.bluetooth).toBe(0);
    expect(summary.failuresByStage['audio-route']).toBe(1);
    expect(summary.successRate).toBe(0.5);
    expect(summary.readiness.missingEvidence).toContain('mobile-success');
    expect(summary.readiness.missingEvidence).toContain('bluetooth-success');
  });

  it('rejects mixed configurations, duplicate sample ids and malformed success samples', () => {
    const mixed = completeEvidence();
    mixed.samples[0] = {
      ...mixed.samples[0]!,
      configurationId: 'different:v1',
    };
    expect(() => summarizeVoiceSpikeEvidence(mixed)).toThrow(/cannot mix/i);

    const duplicate = completeEvidence();
    duplicate.samples[1] = {
      ...duplicate.samples[1]!,
      sampleId: duplicate.samples[0]!.sampleId,
    };
    expect(() => summarizeVoiceSpikeEvidence(duplicate)).toThrow(/duplicate/i);

    const malformed: VoiceSpikeEvidenceSet = {
      configuration: configuration(),
      samples: [
        sample('malformed', {
          timings: {
            coldConnectionMs: null,
            firstSpeechMs: 600,
            bargeInMs: null,
            reconnectMs: null,
          },
        }),
      ],
    };
    expect(() => summarizeVoiceSpikeEvidence(malformed)).toThrow(/require cold connection/i);
  });

  it('is deterministic regardless of sample ordering', () => {
    const evidence = completeEvidence();
    const forward = summarizeVoiceSpikeEvidence(evidence);
    const reverse = summarizeVoiceSpikeEvidence({
      configuration: evidence.configuration,
      samples: [...evidence.samples].reverse(),
    });

    expect(reverse).toEqual(forward);
  });
});

describe('voice architecture evidence matrix', () => {
  it('requires multiple complete configurations and deliberately does not rank them', () => {
    const direct = completeEvidence('direct-openai', 'direct:v1');
    const livekit = completeEvidence('livekit-rtc', 'livekit:v1');

    const matrix = buildVoiceArchitectureEvidenceMatrix([livekit, direct]);

    expect(matrix.comparableForDecision).toBe(true);
    expect(matrix.comparisonMissingEvidence).toEqual([]);
    expect(matrix.summaries.map(summary => summary.candidate)).toEqual([
      'direct-openai',
      'livekit-rtc',
    ]);
    expect(Object.hasOwn(matrix, 'winner')).toBe(false);
    expect(Object.hasOwn(matrix, 'score')).toBe(false);
  });

  it('reports why comparison evidence is incomplete instead of guessing a provider', () => {
    const directOnly = completeEvidence('direct-openai', 'direct:v1');

    const matrix = buildVoiceArchitectureEvidenceMatrix([directOnly]);

    expect(matrix.comparableForDecision).toBe(false);
    expect(matrix.comparisonMissingEvidence).toEqual([
      'comparison:requires-2-configurations',
    ]);
  });
});
