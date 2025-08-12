# rss-feed-generator
generate an rss feed from local or remote sources and feed a generated xml document to podcast players

## todos/app parts
- write a rss file to dir (file writer and xml generator)
- serve the rss file to api endpoint
- cron job for checking new documents
- parsing service (abstract interface)

### parsing services
- review a local dir and parse. serve local files
- hit url with dir and parse. link to url's files
