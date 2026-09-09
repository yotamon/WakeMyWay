# Wake psychology research notes

This is a product research note, not medical guidance.

## Sleep inertia

Sleep inertia describes the temporary period of impaired alertness and cognition following awakening. This matters because conventional UI usability assumptions are weaker during the first wake minutes.

Product consequence:

- fewer choices
- larger targets
- audio-led guidance
- shorter language
- progressive information density

References:

- https://pubmed.ncbi.nlm.nih.gov/10389091/
- https://pmc.ncbi.nlm.nih.gov/articles/PMC3832615/

## Alarm sound

Research reviewed during product discovery suggests melodic alarm qualities may be associated with better perceived or measured wake outcomes than some non-melodic alarm sounds. Evidence is not strong enough to prescribe a universal sound.

Product consequence:

Create a recognizable melodic Wake Motif and test it empirically rather than claiming scientific optimization.

Reference:

- https://pmc.ncbi.nlm.nih.gov/articles/PMC7445849/

## Snoozing

Research is mixed. Repeated snoozing can fragment sleep and extend sleep inertia, while limited snooze among habitual snoozers may not always produce worse immediate performance.

Product consequence:

Treat snooze as an individual strategy variable, not a moral failure.

Reference:

- https://link.springer.com/article/10.1186/s40101-022-00317-w

## Implementation intentions

Planning a cue-linked future action ("when X occurs, I will do Y") has evidence for improving goal attainment across behaviors.

Product consequence:

Tomorrow Contract can encode a simple first action:

```text
When Wake My Way starts -> sit up
Then -> shower
Because -> interview at 10:00
```

Reference:

- https://doi.org/10.1016/S0065-2601(06)38002-1

## Research principles for WMW

- Do not overclaim scientific certainty in marketing.
- Measure wake outcomes inside the product.
- Separate subjective annoyance from objective activation.
- Expect individual variation.
- Run real overnight tests. Laboratory assumptions are not sufficient.
