// import * as path from 'path';
// import * as fs from 'fs';

// import { DirectoryWatcher } from '../../src/services/directoryWatcher';

// // todo - 
// import { episodesPath } from '../../src/utils/consts';
// import config from '../../src/config/config';


// describe('directoryWatcher', () => {
//     test("watches as expected", done => {

//         config.disableWatch = false;

//         console.log("watch enabled: " + config);
//         console.log("dir const: " + episodesPath)

//         // todo - pass in dir?
//         let directoryWatcher: DirectoryWatcher = new DirectoryWatcher();
//         let tempFile:string = path.join(episodesPath + "test.txt")


//         // write a file in test dir
//         fs.writeFile(tempFile, "", () => {
//             console.log("test file to: " + tempFile);
//             expect(directoryWatcher.changesSinceXmlGenerated).toBe(true);

//             config.disableWatch = true;
//             console.log("watch re-disabled: " + config);
//             directoryWatcher.closeWatcher();

//             // delete file
//             fs.unlink(tempFile, done)    
//         })
//     })
// })