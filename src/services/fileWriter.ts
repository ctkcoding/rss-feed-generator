import * as fs from 'fs';
import * as path from 'path';


class fileWriter {

    // takes content
    // writes it to file
    writeFile() {
        let data = "test"
        fs.readFile(path.join(__dirname, "filename.txt"), (err, data) => {
            if (err) throw err;
            console.log(data);
        })

    }

    // write(filename: string, content: string (?))
    // todo - what type to hold document in

}




