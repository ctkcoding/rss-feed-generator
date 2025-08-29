# rss-feed-generator
generate an rss feed from local or remote sources and feed a generated xml document to podcast players

## todos/app parts
✅ generate an rss document

✅ write a rss file to dir (file writer and xml generator)

✅ serve the rss file to api endpoint

✅ cron job/dir watch for checking new documents

✅ parsing service (abstract interface)

✅ move js to class paradigm

### parsing services
✅ review a local dir and parse. serve local files

- hit url with dir and parse. link to url's files

## mvp plus
🚨 add tests in pipeline for all features (next)

- review + add metadata for apple podcasts (ep photo, link to show, etc)

- containerize it

- how do I serve mp3s/photos? does container handle?

## future improvements
- config for required metadata

- log missing metadata



## acknowledgements
shoutout to @JustinLaoshi for bailign me out on when I hit js jank that I'm not familiar with