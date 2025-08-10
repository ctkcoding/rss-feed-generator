import express from 'express';
import healthRoutes from './routes/healthRoutes';
import parseRoutes from './routes/parseRoutes';
import feedRoutes from './routes/feedRoutes';


const app = express();
app.use(express.json());

// Routes
app.use('/api/health', healthRoutes);
app.use('/api/parse', parseRoutes);
app.use('/api/feed', feedRoutes);

// look at levine poc if i want to copy over
// Global error handler (should be after routes)
// app.use(errorHandler);

export default app;