import { Router } from 'express';
import { feed } from '../controllers/feedController';


const router = Router();
router.get('/', feed)

export default router;