import * as fs from 'fs';
import * as path from 'path';
import { rssFilePath } from '../utils/consts';


export class FileWriter {

    // takes content
    // writes it to file
    // write(filename: string, content: string (?))
    public writeFile(data: string) {
        // todo - directories saved in constants file
        fs.writeFile(rssFilePath, data, () => {
            console.log("RSS written successfully!");
        })
    }


}




