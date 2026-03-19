# mrgreen

It's a discord bot

## Requirements

- Java 25
- A Discord bot token and client ID
- A Google API key (YouTube Data API v3 enabled)

## Environment variables

| Variable                 | Required | Description                                    |
|--------------------------|----------|------------------------------------------------|
| `DISCORD_TOKEN`          | yes      | Bot token                                      |
| `DISCORD_CLIENT_ID`      | yes      | Application client ID                          |
| `DISCORD_BRIDGE_BOT_IDS` | no       | Comma-separated bot IDs for IRC bridge support |
| `GOOGLE_API_KEY`         | yes      | Google API key for YouTube lookups             |

## Local development

```
export DISCORD_TOKEN=...
export DISCORD_CLIENT_ID=...
export GOOGLE_API_KEY=...
./gradlew bootRun
```

## Deployment

```
cd infra/terraform && terraform apply
cd infra/ansible && ansible-playbook -i inventory.yml playbook.yml --ask-vault-pass
```

## Adding commands

Implement `SlashCommand` and/or `PrefixCommand`, annotate with `@Component`. They get picked up automatically.

## Adding message handlers

Implement `MessageHandler` with a regex pattern, annotate with `@Component`. Matched messages get dispatched on virtual
threads.
