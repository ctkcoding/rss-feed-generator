import { Episode } from "./episode";

// stores all the fields at show level
// and has a list of episodes

// check what metadata to include
// https://validator.w3.org/feed/


export class Show {
    title: string;
    link: string;
    // todo - feed vs site links??
    description: string;
    episodes: Episode[];

    constructor(title: string, link: string, description: string, episodes: Episode[]) {
        this.title = title;
        this.link = link;
        this.description = description;
        this.episodes = episodes;
      }

      // "toRss" calls xml service
  }

  