import { Show } from "../../models/show";

// parse function takes an input (url, directory etc)
// what else? do I need to enable logs on this directly?

// todo - url or non-standard dir taken from env

export interface Parser {
    parse(episodesPath: string): Promise<Show>;
  }