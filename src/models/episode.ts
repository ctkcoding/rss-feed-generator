// constructor takes required fields
// chain builder for non required

// check what metadata to include
// https://validator.w3.org/feed/

export class Episode {
    title: string;
    description: string;
    url: string;
    date: Date;
    author: string;

    // duration etc

    constructor() {
        this.title = "todo";
        this.description = "desc";
        this.url = "url";
        this.date = new Date();
        this.author = "Chris";
      }
  }