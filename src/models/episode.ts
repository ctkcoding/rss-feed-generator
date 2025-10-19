// constructor takes required fields
// chain builder for non required

import { EnclosureObject } from "rss";

// check what metadata to include
// https://validator.w3.org/feed/

export class Episode {
    title: string;
    description: string;
    url: string;
    pubdate: Date;
    image: string;
    enclosure: EnclosureObject;

    // duration etc
    

    constructor(title: string, description: string, url: string, pubdate: Date, image: string, enclosure: string) {
        this.title = title;
        this.description = description;
        this.url = url;
        this.pubdate = pubdate;
        this.image = image;
        this.enclosure = {url: enclosure, type: "audio/mpeg"};
      }
  }