module.exports = {
    preset: 'ts-jest',
    testEnvironment: 'node',
    moduleFileExtensions: ['ts', 'js'],
    testMatch: ['**/test/**/*.test.(ts|js)'],
    globals: {
      'ts-jest': {
        tsconfig: 'tsconfig.json',
      },
    },
    // "collectCoverage": true,
    // "collectCoverageFrom": ["./src/**"],
    // "coverageThreshold": {
    //   "global": {
    //     "branches": 80,
    //     "functions": 80,
    //     "lines": 80,
    //     "statements": -10
    //   }
    // }
  };