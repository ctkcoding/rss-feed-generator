import * as fs from 'fs';
import * as path from 'path';


export class FileWriter {

    // takes content
    // writes it to file
    // write(filename: string, content: string (?))
    public writeFile(data: string) {
        // todo - directories saved in constants file
        let filePath = path.join(path.join(__dirname, '..'), '..', "rss.xml");
        fs.writeFile(filePath, data, () => {
            console.log(data);
        })
    }


}




