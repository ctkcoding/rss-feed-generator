import { Router } from 'express';
import { episodes } from '../controllers/episodeController';


const router = Router();
router.get('/:filename', episodes)

export default router;