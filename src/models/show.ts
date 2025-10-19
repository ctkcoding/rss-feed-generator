import { Episode } from "./episode";

// stores all the fields at show level
// and has a list of episodes

// check what metadata to include
// https://validator.w3.org/feed/


export class Show {
    title: string;
    // todo - feed vs site links??
    description: string;
    
    site: string;
    link: string;
    image: string;
    language: string;
    ttl: number;
    episodes: Episode[];

    constructor(title: string, description: string, site: string, link: string, image: string, language: string, ttl: number, episodes: Episode[]) {
        this.title = title;
        this.description = description;

        this.site = site;
        this.link = link;
        this.image = image;
        this.language = language;
        this.ttl = ttl;

        this.episodes = episodes;
      }
  }
