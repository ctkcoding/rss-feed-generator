import * as fs from 'fs';


export class FileWriter {

    // takes content
    // writes it to file
    // write(filename: string, content: string (?))
    public writeFile(path: string, data: string) {
        // todo - directories saved in constants file
        fs.writeFile(path, data, () => {
            console.log("RSS written successfully!");
        })
    }
}
