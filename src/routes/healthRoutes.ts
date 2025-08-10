import { Router } from 'express';
import { health } from '../controllers/healthController';


const router = Router();
router.get('/basic', health)

export default router;