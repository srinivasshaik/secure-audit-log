# Milestone 4 - Scenario B Retention

## Implemented scope

- Configurable soft-archive window and daily scheduler
- `archivedAt` lifecycle metadata
- Default exclusion of archived events from query results
- `includeArchived=true` query option
- Full-chain verification across archived rows
- Integration coverage for legitimate archived-row verification

## Configuration

```yaml
audit:
  retention:
    archive-after: 365d
    archive-cron: "0 0 2 * * *"
```

The default is intentionally conservative and must be replaced by an approved retention policy before production use.
