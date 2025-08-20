import { Router } from 'express';
import { parseLocal } from '../controllers/parseController';


const router = Router();
router.get('/local', parseLocal)

export default router;