# RSS Feed Generator

A podcast RSS feed generator backed by a Spring Boot 4.0 application. It reads MP3 files from a local directory, extracts metadata from their ID3 tags, and produces an RSS 2.0 feed with iTunes/Apple Podcasts modules — auto-regenerated when files change.

## Quick Start

### Docker Compose (recommended)

```bash
docker compose up
```

This builds the image, starts the service on port 8080, and binds the `artwork/`, `episodes/`, and `info/` directories.

### Dockerfile only

```bash
docker build -t rss-feed-generator .
docker run -p 8080:8080 \
  -v $(pwd)/artwork:/app/artwork \
  -v $(pwd)/episodes:/app/episodes \
  -v $(pwd)/info:/app/info \
  -e RSS_ARTWORK_DIR=/app/artwork \
  -e RSS_EPISODES_DIR=/app/episodes \
  -e RSS_INFO_DIR=/app/info \
  rss-feed-generator
```

### Local development

```bash
./gradlew bootRun
```

## Required Directory Structure

At runtime, the app reads from three directories relative to the working directory:

```
.
├── episodes/          # MP3 episode files (.mp3 by default)
├── artwork/           # Cover art files (.jpeg by default)
│   └── cover.jpeg     # Used as the podcast show art
└── info/
    └── show.json      # Podcast metadata (see below)
```

### show.json format

```json
{
  "title": "My Podcast",
  "description": "A description of your show",
  "site": "https://example.com",
  "link": "http://localhost:8080",
  "image": "cover.jpeg",
  "language": "en-us"
}
```

| Field | Required | Description |
| --- | --- | --- |
| `title` | Yes | Podcast name |
| `description` | Yes | Show description |
| `link` | Yes | Base URL for serving RSS, episodes, and artwork |
| `image` | No | Artwork filename in the `artwork/` directory |
| `site` | No | External website URL |
| `language` | Yes | RSS feed language code (e.g. `en-us`) |

## Configuration

All settings can be configured via Spring properties in `application.properties` or via environment-prefixed variables (`RSS_`).

| Property | Env Var | Default | Description |
| --- | --- | --- | --- |
| `rss.artwork-dir` | `RSS_ARTWORK_DIR` | `artwork` | Path to artwork directory |
| `rss.episodes-dir` | `RSS_EPISODES_DIR` | `episodes` | Path to episodes directory |
| `rss.info-dir` | `RSS_INFO_DIR` | `info` | Path to info/ directory containing `show.json` |
| `rss.rss-file-name` | `RSS_RSS_FILE_NAME` | `rss.xml` | Name of the generated RSS XML file |
| `rss.show-file-name` | `RSS_SHOW_FILE_NAME` | `show.json` | Podcast metadata file name |
| `rss.episode-file-extension` | `RSS_EPISODE_FILE_EXTENSION` | `.mp3` | File extension to recognize as episodes |
| `rss.artwork-file-extension` | `RSS_ARTWORK_FILE_EXTENSION` | `.jpeg` | File extension for artwork files |
| `rss.extract-artwork` | `RSS_EXTRACT_ARTWORK` | `false` | Extract embedded cover art from MP3 ID3 tags |
| `rss.file-watch` | `RSS_FILE_WATCH` | `true` | Watch episodes directory for file changes and rebuild RSS automatically |
| `rss.run-on-startup` | `RSS_RUN_ON_STARTUP` | `false` | Generate RSS feed on application startup |
| `rss.language` | `RSS_LANGUAGE` | `en-us` | Default language code |
| `rss.error-log-file-prefix` | `RSS_ERROR_LOG_FILE` | `parse-errors-` | Prefix for error log files |
| `rss.failure-limit` | `RSS_FAILURE_LIMIT` | `5` | Max RSS generation failures before stopping retries |

### Container overrides

With Docker Compose, override the defaults in `compose.yaml`:

```yaml
environment:
  RSS_ARTWORK_DIR: /app/artwork
  RSS_EPISODES_DIR: /app/episodes
  RSS_INFO_DIR: /app/info
  RSS_FILE_WATCH: "true"
  RSS_RUN_ON_STARTUP: "true"
```

Or when running with `docker run`, pass `--env RSS_RUN_ON_STARTUP=true`.

## API Endpoints

| Endpoint | Method | Description |
| --- | --- | --- |
| `/health` | GET | Health check — returns `"Up and running"` |
| `/parse` | GET | Trigger a full RSS feed regeneration |
| `/rss` | GET | Serve the generated RSS feed |
| `/episodes/{file}` | GET | Serve an episode MP3 file |
| `/artwork/{file}` | GET | Serve an artwork image file |

### Examples

```bash
# Check health
curl http://localhost:8080/health

# Trigger a re-parse and RSS rebuild
curl http://localhost:8080/parse

# Download the feed
curl http://localhost:8080/rss

# Serve an episode
curl http://localhost:8080/episodes/01%20-%20Ep.%201.mp3

# Serve artwork
curl http://localhost:8080/artwork/cover.jpeg
```

## Development

### Build and run

```bash
./gradlew bootRun
```

### Build JAR

```bash
./gradlew bootJar
java -jar build/libs/rss-generator-0.0.1-SNAPSHOT.jar
```

### Run tests

Tests automatically run `spotlessCheck` first:

```bash
./gradlew test
```

### Code formatting

This project uses [Spotless](https://github.com/diffplug/spotless) with Google Java Format. Run lints independently:

```bash
./gradlew spotlessCheck
./gradlew spotlessApply
```

### File watching

The app has two mechanisms for RSS regeneration:

1. **File watcher** (default: enabled) — monitors the episodes directory using Java's `WatchService` and rebuilds the RSS feed when files are created, deleted, or modified.
2. **Scheduled poll** (cron: every minute) — checks for new file changes and rebuilds if detected. Also triggers a one-time parse on startup if `rss.run-on-startup` is `true`.

Failed RSS generations are retried up to `rss.failure-limit` times before being abandoned. Errors are logged to files with the prefix `rss.error-log-file-prefix`.
