import * as path from 'path';
import * as fs from 'fs';

import { FileWriter } from '../../src/services/fileWriter';

// inject the fake rss feed somehow

const filePath = path.join(__dirname, '..', "/resources/", "test.txt"); 


describe('fileWriter', () => {
    test("writes data to file", () => {

        let fileWriter: FileWriter = new FileWriter();
        fileWriter.writeFile(filePath, "test data")


        expect(fs.existsSync(filePath));
        // const showJson = fs.readFileSync(filePath, 'utf8');

        expect(fs.readFileSync(filePath).compare(Buffer.from('test data')) === 0)
    })
})
