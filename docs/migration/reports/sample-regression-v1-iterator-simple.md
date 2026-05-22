# Migration compare report

## Summary

- Template id: regression-v1-iterator-simple
- Classification: ADAPTED
- Recommendation: accept_with_review

## Counts

| Pipeline | Row count |
|----------|----------:|
| V1 | 3 |
| V2 | 3 |

## Sample match

- Sample size: 3
- Sample match rate: 1.0000

## Warnings

- V2 draft includes SpEL transform for field `label` (`'row'` literal); compare uses SQL+SpEL on iterator and JDBC-shaped paths when SCRIPT fields exist — see `docs/migration/script-spel-draft-migration.md`.

## Classification

Final class **ADAPTED** — promote guidance: `accept_with_review`.
