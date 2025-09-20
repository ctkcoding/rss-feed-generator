import * as path from 'path';
import * as fs from 'fs';

import config from '../../src/config/config';
import { testData, testTextFileName, testResourcesPath } from '../testConsts';
import { FileWriter } from '../../src/services/fileWriter';

// todo - test dir goes in as config, then to /resources
const filePath = path.join(config.pathRoot, testResourcesPath, testTextFileName); 


describe('fileWriter', () => {
    test("writes data to file", async () => {

        let fileWriter: FileWriter = new FileWriter();
        fileWriter.writeFile(filePath, testData)


        expect(fs.existsSync(filePath));
        // const showJson = fs.readFileSync(filePath, 'utf8');

        expect(fs.readFileSync(filePath).compare(Buffer.from(testData)) === 0)
    })
})
