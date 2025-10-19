import RSS from 'rss';
import { Show } from '../models/show';

// https://validator.w3.org/feed/

// todo - provide as function or service? whats better in js

// generateRssXml(episodes: episode[])
// builds xml doc for a pod feed given config + episode list
// take feed-level details from app config
// take per-episode details from parser's data

export class RssBuilder {

    public generateRss(show: Show) {

        const feed = new RSS({
            title: show.title,
            description: show.description,
            feed_url: show.link,
            site_url: show.site,
            // todo - itunes image
            image_url: show.image,
            language: show.language,
            ttl: show.ttl,
            custom_namespaces: {
                'itunes': 'http://www.itunes.com/dtds/podcast-1.0.dtd'
              },
            custom_elements: [
                {"itunes:image": show.image}
            ]

        });
    
        show.episodes.forEach(episode => {
            feed.item({
                title: episode.title,
                description: episode.description,
                
                // todo - custom elements
                custom_elements: [
                    {"itunes:image": episode.image},
                ],
                // itunes:duration
                url: episode.url,
                date: episode.pubdate,
                enclosure: episode.enclosure,                
            });
        });
    
    
        // add more rss tags
    
        return feed.xml({ indent: true });
    }
}



/*
<rss xmlns:atom="http://www.w3.org/2005/Atom" xmlns:itunes="http://www.itunes.com/dtds/podcast-1.0.dtd" version="2.0">
<channel>

<title>TC</title>
<link>https://google.com</link>
<description>this is my show</description>

</channel>
</rss>
*/


