import { Show } from "../../models/show";

// parse function takes an input (url, directory etc)
// what else? do I need to enable logs on this directly?


export interface Parser {
    parse(source: string): Show;
  }