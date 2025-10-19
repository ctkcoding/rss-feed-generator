import express from 'express';
import healthRoutes from './routes/healthRoutes';
import feedRoutes from './routes/feedRoutes';
import artworkRoutes from './routes/artworkRoutes'
import episodeRoutes from './routes/episodeRoutes'
// import { DirectoryWatcher } from './services/directoryWatcher';
import { Scheduler } from './services/scheduler';


const app = express();
app.use(express.json());

// let fileWriter: FileWriter = new FileWriter;

export const scheduler:Scheduler = new Scheduler;

// Routes
app.use('/api/health', healthRoutes);
// app.use('/api/parse', parseRoutes);
app.use('/api/feed', feedRoutes);
app.use('/episodes', episodeRoutes);
app.use('/artwork', artworkRoutes);

// look at levine poc if i want to copy over
// Global error handler (should be after routes)
// app.use(errorHandler);

export default app;