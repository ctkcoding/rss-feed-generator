import * as path from 'path';
import * as fs from 'fs';

import { RssBuilder } from '../../src/services/rssBuilder';
import { Show } from '../../src/models/show';
import { Episode } from '../../src/models/episode';

// inject the fake rss feed somehow

const filePath = path.join(__dirname, '..', "/assets/", "test.txt"); 


describe('rssBuilder', () => {
    test("builds RSS as expected", () => {
        const rssBuilder: RssBuilder = new RssBuilder();

        // create two episodes
        let e1: Episode = new Episode();
        e1.title = "episode 1";
        e1.date = new Date();
        e1.description = "joe biden";

        let e2: Episode = new Episode();
        e2.title = "episode 2";
        e1.date = new Date();
        e1.description = "them ribs";

        let episodes: Episode[] = [e1, e2];


        // title, link, description, eipisodes[]
        let title: string = "test title";
        let link: string = "test.com/rss";
        let description: string = "test description";
        let actual = rssBuilder.generateRss(new Show(title, link, description, episodes))

        // todo - test that it generates correctly
        expect(actual.includes('<title><![CDATA[test title]]></title>'));
        expect(actual.includes('<description><![CDATA[test description]]></description>'));
        expect(actual.includes('<atom:link href="test.com/rss" rel="self" type="application/rss+xml"/>'));

        expect(actual.includes('<title><![CDATA[episode 1]]></title>'));
        expect(actual.includes('<description><![CDATA[joe biden]]></description>'));

        expect(actual.includes('<title><![CDATA[episode 2]]></title>'));
        expect(actual.includes('<description><![CDATA[description]]></description>'));
    })
})
