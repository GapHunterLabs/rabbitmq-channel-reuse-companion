# RabbitMQ Channel Reuse Companion

Warning icon on a RabbitMQ `Channel` created with
`....createChannel()` and used for `basicPublish(...)` in the same
method — RabbitMQ's own Java Client API Guide calls this a "classic
anti-pattern to be avoided": "opening a channel for each published
message... Channels are supposed to be reasonably long-lived and
opening a new one is a network round-trip which makes this pattern
extremely inefficient".

## Why it exists

`Channel channel = connection.createChannel(); channel.basicPublish(...)`
compiles fine and publishes the message — call it once per outgoing
message and every single publish quietly costs a full network
round-trip to open a channel, instead of reusing one long-lived
channel the application already has.

## Why built this way

- **100% static text/PSI analysis** — matches method/variable names by
  simple text, so it works whether the real RabbitMQ client jar is on
  the classpath or not. Java and Kotlin.

## v0.1 scope — stated honestly, not exhaustively

Matches by simple name, not real type resolution — an unrelated
`createChannel()`/`basicPublish()` pair on some other type is a
possible (rare) false positive. Only a channel assigned to a **local**
variable and published on in the same method is flagged — a channel
stored as an instance field (the correct long-lived pattern) is never
flagged.

## Usage

Open any Java/Kotlin file using the RabbitMQ client. A channel created
and published on in the same method shows a warning icon.

## Enterprise / Team Licensing

Need enterprise features, custom rules, or team licensing? Contact us at
**gaphunterlabs@gmail.com**.

## Development

```
./gradlew test           # unit tests
./gradlew buildPlugin    # generates build/distributions/*.zip
./gradlew verifyPlugin   # checks compatibility against real IDEs
```

## License

Apache-2.0. See `LICENSE`.
