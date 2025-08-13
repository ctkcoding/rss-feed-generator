import { Show } from "../../models/show";
import { Parser } from "./parser";


// implementation of parser for local dir
class FileSystemParser implements Parser
{
    // takes a directory to watch
    // dir structure:
    // - metadata.txt           contains show details
    // - episodes               contains episode files
    //  L episode1.mp3          actual episode contents
    //  L episode1.txt          contains episode details


    parse(source: string): Show {
        // look for metadata.txt
        // pulls show metadata from file
        let title: string = "";
        let link: string = "";
        let description: string = "";

        let show: Show = new Show(title, link, description);

        // pulls episodes from /episodes dir
        // saves into episode.ts type (parallel if possible a la java stream)

        // todo - save a log of episodes or metadata missing or without match

        // throw new Error("Method not implemented.");

        return show;
    }
}