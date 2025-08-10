import { Router } from 'express';
import { feed } from '../controllers/feedController';


const router = Router();
router.get('/rss', feed)

export default router;