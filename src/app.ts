import express from 'express';
const app = express();

// todo - move to config
const port = 3000;

// todo - break into app and server files
// base on levine ts project
app.get('/', (req, res) => {
  res.send('Hello World!');
});

app.listen(port, () => {
  return console.log(`Express is listening at http://localhost:${port}`);
});