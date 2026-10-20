#!/usr/bin/env node
/**
 * Deliberate ONE-VOICE-AT-A-TIME operator tool. No account endpoint for voice
 * creation: users cannot spend our OpenAI quota by generating voice IDs.
 *
 * Usage: OPENAI_API_KEY=... node scripts/create-wake-voice.mjs alfred --create
 * Copy the returned id into WMW_GPT_LIVE_VOICE_ALFRED only after listening tests.
 */
const profiles = {
  alfred: {
    name: 'WakeMyWay Alfred — candidate',
    prompt: 'A naturally British adult male voice. Quiet confidence, subtle warm resonance, understated dry wit, relaxed slightly textured timbre and believable casual phrasing. Very human, close to the microphone, never polished like a narrator, never a theatrical butler. No exaggerated emotion. Conversational, gentle in a bedroom, distinct pauses without slow-motion delivery.',
  },
  sam: {
    name: 'WakeMyWay Sam — candidate',
    prompt: 'A warm, grounded, friendly adult voice. Unforced, relaxed and direct, with natural small inflection changes, modest breathiness, comfortable silences and a slightly amused conversational rhythm. Sounds like a trusted friend, not a wellness coach or a commercial announcer. Avoid artificial enthusiasm and motivational catchphrases.',
  },
  luna: {
    name: 'WakeMyWay Luna — candidate',
    prompt: 'An intimate and softly spoken adult feminine voice. Warm, calm, gentle but clearly audible, with organic timing, grounded emotions and natural short pauses. Comfortable in a quiet early morning room. Neither sleepy caricature nor meditation app narrator. Never theatrical, whisper-only, whisper breath noise, or exaggerated sweetness.',
  },
  rocco: {
    name: 'WakeMyWay Rocco — candidate',
    prompt: 'A playful adult masculine voice with an easy, slightly raspy everyday tone, quick dry humour, confident conversational rhythm and a natural smile in the voice. Can become crisp and direct without shouting. Never cartoonish, insulting, aggressively loud or a motivational announcer.',
  },
};
const scriptHint = 'Morning. It is that time again. I was going to make a grand speech, but honestly, we can keep this simple. Take a moment. No need to rush into a conversation. We have a morning to start, and we can start small.';
const character = process.argv[2];
if (!Object.hasOwn(profiles, character ?? '')) {
  console.error('Select one: alfred | sam | luna | rocco');
  process.exitCode = 2;
} else if (process.argv[3] !== '--create') {
  console.log(JSON.stringify({ ...profiles[character], script_hint: scriptHint }, null, 2));
  console.log('\nPREVIEW ONLY. Add --create to generate ONE voice using your own OpenAI API key.');
} else if (!process.env.OPENAI_API_KEY) {
  console.error('OPENAI_API_KEY is required for --create.');
  process.exitCode = 2;
} else {
  const controller = new AbortController();
  const timeout = setTimeout(() => controller.abort(), 20_000);
  try {
    const response = await fetch('https://api.openai.com/v1/audio/voices', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${process.env.OPENAI_API_KEY}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify({
        type: 'prompt',
        name: profiles[character].name,
        prompt: profiles[character].prompt,
        script_hint: scriptHint,
        model: 'auto',
      }),
      signal: controller.signal,
    });
    if (!response.ok) {
      console.error(`Voice creation failed: HTTP ${response.status}. No provider body logged.`);
      process.exitCode = 1;
    } else {
      const data = await response.json();
      if (!data || typeof data.id !== 'string' || !/^[A-Za-z0-9_-]{1,128}$/.test(data.id)) {
        console.error('Unexpected OpenAI voice ID shape.');
        process.exitCode = 1;
      } else {
        console.log('Created one voice candidate (not automatically promoted).');
        console.log(`voice_id: ${data.id}`);
        console.log(`After blind testing, set WMW_GPT_LIVE_VOICE_${character.toUpperCase()}=${data.id}`);
      }
    }
  } catch {
    console.error('Voice creation failed or timed out. Check provider dashboard before retrying.');
    process.exitCode = 1;
  } finally {
    clearTimeout(timeout);
  }
}
