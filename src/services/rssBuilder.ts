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

        // todo - should I create just one builder and re use?
        const feed = new RSS({
            title: show.title,
            description: show.description,
            feed_url: show.link,
            // todo - site url vs feed url?
            site_url: 'https://music.apple.com/us/curator/time-crisis/993269786',
            // Optional: language, image_url, etc.

            // todo - include author?
        });
    
        show.episodes.forEach(episode => {
            feed.item({
                title: episode.title,
                description: episode.description,
                url: episode.url,
                date: episode.date,
                author: episode.author,
                // update episodes using show details? ie for author
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


